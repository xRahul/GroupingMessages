# Architecture

This document describes the actual code as of schema v4 / version 1.7.3.

## Layers

```
┌─────────────────────────────────────────────────────────────────────┐
│ UI: Activities + ListAdapter/DiffUtil                               │
│     MainActivity, SmsActivity, ChangeCategoryActivity,              │
│     SettingsActivity (+ SettingsFragment, fragments, adapters)      │
├─────────────────────────────────────────────────────────────────────┤
│ ViewModel (vm/)                                                     │
│     CategoriesViewModel   SmsListViewModel   SettingsViewModel      │
│     expose LiveData; bulk/pipeline work via AppExecutors            │
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

- `disk(Runnable)` — a single-threaded executor serializing bulk/pipeline work
  (inbox sync, training, backup/import). Serial execution means that pipelined
  work never overlaps. The previously audited micro-queries (rename dialog,
  tap-to-read, bulk mark-read, category load) now run here too; do not add new
  database work on the UI thread.
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

## Schema v4

SQLite database managed by `DatabaseHelper` (singleton). Version history:
v2 added training columns (`cleaned_sms`, `sender_type`, `category_id`, `similar_to`,
`sim_score` changes); v3 added the sms indexes; v4 added `provider_id` with a
unique partial index, enabled foreign-key enforcement, rebuilt the `sms` table,
repaired orphan categories and `similar_to` references, and recreated the indexes
and update trigger.

- **config** — key/value store (`name` unique, `value`); seeded with `lastSmsTime = 0`.
- **category** — `_id`, `name`, `color`, `visibility` (default 1), timestamps.
  Seeded with the white **Unknown** category (id 1).
- **sms** — `_id`, `date`, `person`, `read`, `seen`, `subject`, `address`, `body`,
  `cleaned_sms`, `visibility`, `sender_type`, `category_id` → category(`_id`)
  (enforced foreign key), `provider_id` (nullable, unique partial index so
  repeated provider reads are ignored), `similar_to` (logical reference to
  sms(`_id`) marking the exemplar a message was trained against; `0` means none;
  no foreign-key constraint), `sim_score` (default 0.0), timestamps.

> SQLite foreign-key enforcement is **ON** — `DatabaseHelper.onOpen` calls
> `setForeignKeyConstraintsEnabled(true)` after any upgrade, and the v4 migration
> repairs orphan category and `similar_to` references before the indexes are
> recreated.

All three tables carry `created_at`/`updated_at` defaults plus `updated_at` triggers.

Indexes (v3 + v4):

```sql
CREATE INDEX IF NOT EXISTS idx_sms_category   ON sms(category_id);
CREATE INDEX IF NOT EXISTS idx_sms_date       ON sms(date);
CREATE INDEX IF NOT EXISTS idx_sms_visibility ON sms(visibility);
CREATE UNIQUE INDEX IF NOT EXISTS idx_sms_provider_id
    ON sms(provider_id) WHERE provider_id IS NOT NULL;
```

These back the hot paths: per-category visible listings, `lastSmsTime` date scans,
and visibility-filtered count queries.

## Backup format notes

`db/DatabaseBackup` copies the raw SQLite database file — not SQL dumps.

- **Export (current UI):** `PRAGMA wal_checkpoint(TRUNCATE)` drains the WAL, DB closed,
  file streamed to a Storage Access Framework `OutputStream` the user picked. No fixed
  external path is written by default. If the user sets a passphrase, `BackupCrypto`
  writes a versioned AES-256-GCM container (PBKDF2-HMAC-SHA256, per-file salt and IV)
  instead of plaintext SQLite.
- **Import (current UI):** selected document copied to `<live>.tmp`; an encrypted
  container is detected by its `GMBK1` header, decrypted to a scratch file with the
  supplied passphrase, and then header, `PRAGMA integrity_check`,
  `user_version <= DATABASE_VERSION` and required tables are checked. Only then is the
  live DB closed, stale `-wal`/`-shm` sidecars deleted, and the scratch file atomically
  renamed over the live DB.
- **Legacy path:** `exportDb`/`importDb` keep the old fixed
  `GroupMessagingBackupV<schemaVersion>` file in `getExternalFilesDir(null)` for
  backward compatibility and tests; the Settings UI no longer uses it.
- Both operations hold `synchronized (AppDatabase.class)` for the swap; failure paths
  clean up `.tmp` files and leave the previous database intact.
- Backups are plaintext SQLite by default or an encrypted container when the user sets a
  passphrase. The UI warns before export and the passphrase is never stored. Restoring
  across schema versions relies on `SQLiteOpenHelper`'s upgrade path after import.

## ViewModel inventory

| ViewModel | LiveData exposed | Commands |
|---|---|---|
| `CategoriesViewModel` | categories list, unread/read counts per category, newly-added-SMS count, added-category name | `refresh`, `syncLatestSms`, `addCategory`, `deleteCategory` (guarded: Unknown id 1 never deleted), one-shot events consumed via `consume...()` |
| `SmsListViewModel` | sms list for the open category | `refresh(categoryId)`, `swipeDelete` (hides self-trained, deletes untrained — decision lives in `SmsDao.deleteSmsByMap`), `markRead(ids)`, `moveToCategory(ids, targetId)` |
| `SettingsViewModel` | `VersionState` (`UP_TO_DATE`/`OUTDATED`/`ERROR`/`DISABLED`) | `checkLatestVersion()` — reads the opt-in preference, runs on the network executor, resolves the GitHub releases redirect without following it, accepts only HTTPS GitHub release URLs for this repository, normalizes a leading `v`, 5 s connect/read timeouts |

## Network surface

The only outbound request in the app is `SettingsViewModel.fetchLatestVersion()`:
an HTTP HEAD of `https://github.com/xRahul/GroupingMessages/releases/latest` with
redirects disabled, reading the `Location` header to compare versions. It runs only
when the user enables "Check for updates". All SMS processing stays on-device.
