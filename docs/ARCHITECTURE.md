# Architecture

This document describes the actual code as of schema v3 / version 1.6.

## Layers

```
┌─────────────────────────────────────────────────────────────────────┐
│ UI: Activities + ListAdapter/DiffUtil                               │
│     MainActivity, SmsActivity, ChangeCategoryActivity,              │
│     SettingsActivity (+ SettingsFragment, fragments, adapters)      │
├─────────────────────────────────────────────────────────────────────┤
│ ViewModel (vm/)                                                     │
│     CategoriesViewModel   SmsListViewModel   SettingsViewModel      │
│     expose LiveData; all DB/provider work via AppExecutors          │
├─────────────────────────────────────────────────────────────────────┤
│ DAO (db/)                    Engine (classify/)                     │
│     CategoryDao  SmsDao      TextVectorizer                         │
│     ConfigDao                CharTrigramProfile                     │
│                              SmsCategorizer                         │
├─────────────────────────────────────────────────────────────────────┤
│ Database: AppDatabase singleton → DatabaseHelper (SQLiteOpenHelper) │
│     DatabaseBackup (export/import)                                  │
├─────────────────────────────────────────────────────────────────────┤
│ Sources/bridges: ExternalContentBridge (inbox SMS, contacts),       │
│     TrainSms (training pipeline), model/ POJOs (Sms, Category)      │
└─────────────────────────────────────────────────────────────────────┘
```

- **model/** — plain data holders (`Sms`, `Category`) with cursor mapping.
- **db/** — static-style DAOs (`CategoryDao`, `SmsDao`, `ConfigDao`) operating on
  `SQLiteDatabase` obtained from the `AppDatabase` singleton, which delegates to the
  `DatabaseHelper` `SQLiteOpenHelper`. `DatabaseBackup` handles export/import.
- **vm/** — `AndroidViewModel`s exposing `MutableLiveData`; they push disk work onto
  `AppExecutors.disk()` and post results back with `AppExecutors.main()`.
- **Activities** — observe `LiveData` and render lists with `ListAdapter` +
  `DiffUtil` callbacks (`CategoryDiffCallback`, `SmsDiffCallback`). No business logic.
- **classify/** — pure-Java categorization engine, no Android dependencies
  (unit-testable without Robolectric).

## Threading model

`AppExecutors` (vm/AppExecutors.java) provides:

- `disk(Runnable)` — a single-threaded executor for all database, provider, and
  network work. Serial execution means no concurrent access from ViewModels.
- `main(Runnable)` — posts to the main-looper handler so ViewModels can call
  `setValue(...)` on `LiveData`.

Flow example (`CategoriesViewModel.syncLatestSms`): disk thread reads inbox → trains →
stores → main thread updates LiveData → Activity re-renders via DiffUtil.

Database backup/export additionally serialize against all other access by holding
`synchronized (AppDatabase.class)` for the whole file swap (see Backup format notes).

## Training & classification pipeline

Everything below runs locally. Entry points:

1. **Import new inbox messages** — `ExternalContentBridge.getLatestSmsFromInbox`
   fetches inbox rows newer than the stored `lastSmsTime` config value;
   `CategoriesViewModel.syncLatestSms` hands them plus the *exemplar* set to
   `TrainSms.getTrainedListOfSms`, which stores results via `SmsDao`.
2. **Exemplars** — self-trained messages: stored SMS where `_id == similar_to` and
   `sim_score = 1.0` (created when the user moves a message into a category).
3. **Retrain propagation** — when a message is trained,
   `TrainSms.retrainExistingSms` re-scores every stored SMS against the new exemplar
   and moves those scoring at/above threshold (and better than their current score)
   into the same category.

### Preprocessing

`TrainSms.cleanString`: lowercase, digits collapsed to `'1'`, punctuation stripped
(single-pass char loop), WordNet-derived stop words removed. Cleaning applies to
`address + " "` + body unless a cleaned text already exists.

### Categorization engine (classify/)

1-nearest-exemplar scoring over the user's exemplar corpus:

- **Word model** — `TextVectorizer`: TF-IDF vectors over normalized tokens.
  `tf = count(term) / tokens`, `idf = ln((N+1)/(df+1))` where `N` = corpus size and
  `df` = number of exemplars containing the term. Similarity = cosine between query
  and exemplar vectors.
- **Character model** — `CharTrigramProfile`: counts of character trigrams over the
  normalized text padded with `_` at both ends; similarity = Dice coefficient
  `2·|A∩B| / (|A|+|B|)` using multiset intersection minima.
- **Hybrid blend** — `SmsCategorizer.Batch` precomputes per-exemplar TF-IDF vectors
  and trigram profiles once per pass, then scores each query:

  | Mode | Score |
  |---|---|
  | `balanced` (default) | `0.6 · wordCosine + 0.4 · charDice` |
  | `wordsOnly` | word cosine only |
  | `charactersOnly` | char Dice only |
  | `legacyLevenshtein` | whole-string simmetrics Levenshtein (historical pipeline semantics) |

  Mode selection comes from the `key_similarity_algorithm` preference via
  `resolveMode`: legacy stored values `levenshtein`/`jaroWinkler` map to
  `legacyLevenshtein`; unset or unknown values map to `balanced`.
  The simmetrics library is retained only for `legacyLevenshtein`.

### Threshold & fallback

- The `key_similarity_score` preference stores a percent (0–100). Default depends on
  mode (`defaultThresholdPercent`): **25%** for balanced-family modes (hybrid scores
  live on a lower absolute scale than whole-string similarity), **80%** for
  `legacyLevenshtein` (the historical default).
- A new SMS takes its best-scoring exemplar's category if the score ≥ threshold;
  ties go to the first exemplar in pipeline order. Otherwise it falls back to the
  built-in **Unknown category (id 1)** with `similar_to = 0`, `sim_score = 0`.

## Schema v3

SQLite database managed by `DatabaseHelper` (singleton). Version history:
v2 added training columns (`cleaned_sms`, `sender_type`, `category_id`, `similar_to`,
`sim_score` changes); v3 added the sms indexes below.

- **config** — key/value store (`name` unique, `value`); seeded with `lastSmsTime = 0`.
- **category** — `_id`, `name`, `color`, `visibility` (default 1), timestamps.
  Seeded with the white **Unknown** category (id 1).
- **sms** — `_id`, `date`, `person`, `read`, `seen`, `subject`, `address`, `body`,
  `cleaned_sms`, `visibility`, `sender_type`, `category_id` → category(`_id`),
  `similar_to` → sms(`_id`) (self-reference marking the exemplar a message was
  trained against), `sim_score` (default 0.0), timestamps.

All three tables carry `created_at`/`updated_at` defaults plus `updated_at` triggers.

Indexes (v3):

```sql
CREATE INDEX IF NOT EXISTS idx_sms_category   ON sms(category_id);
CREATE INDEX IF NOT EXISTS idx_sms_date       ON sms(date);
CREATE INDEX IF NOT EXISTS idx_sms_visibility ON sms(visibility);
```

These back the hot paths: per-category visible listings, `lastSmsTime` date scans,
and visibility-filtered count queries.

## Backup format notes

`db/DatabaseBackup` copies the raw SQLite database file — not SQL dumps — to
`GroupMessagingBackupV<schemaVersion>` inside the app's external files dir
(`getExternalFilesDir(null)`).

- **Export:** `PRAGMA wal_checkpoint(TRUNCATE)` drains the WAL, DB closed, file copied
  to `<backup>.tmp` then atomically renamed onto the backup name.
- **Import:** header verified as `SQLite format 3\0` before anything touches live data;
  DB closed, stale `-wal`/`-shm` sidecars deleted, backup copied to `<live>.tmp`,
  header re-verified inside the lock (guards TOCTOU on the backup file), then atomic
  rename over the live DB. Next `AppDatabase.get()` reopens lazily.
- Both operations hold `synchronized (AppDatabase.class)` end-to-end; the contract on
  `AppDatabase.get(Context)` documents that no readers may be in flight during a swap.
- Failure paths clean up `.tmp` files and leave the previous database intact.
- Backups are version-tagged by filename; restoring across schema versions relies on
  SQLiteOpenHelper's upgrade path after import.

## ViewModel inventory

| ViewModel | LiveData exposed | Commands |
|---|---|---|
| `CategoriesViewModel` | categories list, unread/read counts per category, newly-added-SMS count, added-category name | `refresh`, `syncLatestSms`, `addCategory`, `deleteCategory` (guarded: Unknown id 1 never deleted), one-shot events consumed via `consume...()` |
| `SmsListViewModel` | sms list for the open category | `refresh(categoryId)`, `swipeDelete` (hides self-trained, deletes untrained — decision lives in `SmsDao.deleteSmsByMap`), `markRead(ids)`, `moveToCategory(ids, targetId)` |
| `SettingsViewModel` | `VersionState` (`UP_TO_DATE`/`OUTDATED`/`ERROR`) | `checkLatestVersion()` — resolves the GitHub releases redirect without following it and compares against the current `VERSION_NAME` tag URL; 5 s connect/read timeouts |

## Network surface

The only outbound request in the app is `SettingsViewModel.fetchLatestVersion()`:
an HTTP GET of `https://github.com/xRahul/GroupingMessages/releases/latest` with
redirects disabled, reading the `Location` header to compare versions. All SMS
processing stays on-device.
