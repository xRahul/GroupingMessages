# Pre-Revitalization Baseline

Recorded: 2026-08-23
Branch: `revitalize/mvvm` (created from `master`)
Base commit SHA: `b6e66bcff00bd632fb222d6872f23527fc21b3c1`

## Build Toolchain

| Component | Version |
|---|---|
| Gradle | 8.5 (`gradle-wrapper.properties`) |
| Android Gradle Plugin | 8.2.0 |
| Java source/target compatibility | 1.8 |
| applicationId | `in.rahulja.groupingmessages` |
| versionCode / versionName | 16 / 1.6 |

## SDK Levels

| Level | Value |
|---|---|
| compileSdk | 34 |
| targetSdk | 34 |
| minSdk | 24 |

## Dependencies

**Implementation**

- `androidx.appcompat:appcompat:1.6.1`
- `androidx.preference:preference:1.2.1`
- `com.google.android.material:material:1.11.0`
- `androidx.cardview:cardview:1.0.0`
- `androidx.recyclerview:recyclerview:1.3.2`
- `androidx.legacy:legacy-support-v4:1.0.0`
- `androidx.vectordrawable:vectordrawable:1.1.0`
- `com.github.QuadFlask:colorpicker:0.0.13` (jitpack)
- `com.github.mpkorstanje:simmetrics-core:4.1.1` (jitpack)
- `org.apache.commons:commons-collections4:4.4`

**Compile-only / annotation processor**

- `org.projectlombok:lombok:1.18.30`

**Android test**

- `androidx.test.espresso:espresso-core:3.5.1`
- `androidx.test:runner:1.5.2`
- `androidx.test:rules:1.5.0`

Note: guava's `listenablefuture` module is excluded globally; a local `libs/` fileTree is also on the implementation classpath.

## Baseline Verification Run

Command: `./gradlew clean lintDebug testDebugUnitTest assembleDebug --stacktrace`

- Result: `BUILD SUCCESSFUL`, exit code 0 (2m 19s; 45 actionable tasks)
- Unit tests (`testDebugUnitTest`): **4 tests, 0 failures, 0 errors, 0 skipped**
  - `DatabaseBridgeTest` (1), `ExampleUnitTest` (1), `ExternalContentBridgeTest` (1), `PerformanceBenchmarkTest` (1)
- Lint (`lintDebug`): **0 errors, 44 warnings**
  - Notable warning clusters: `RedundantLabel` (manifest), `ObsoleteSdkInt` (incl. unnecessary `values-v21`), deprecated API usage
  - Compile warnings: obsolete Java source/target 8; deprecated API usage in main and unit-test sources

## Known Bugs (to fix during revitalization)

1. **FK comma fresh-install crash** — foreign-key/category-list handling splits on commas and crashes on fresh installs with no stored data.
2. **Phantom WRITE_EXTERNAL_STORAGE request** — runtime permission requested at `MainActivity.java:153` but not declared/needed by the manifest.
3. **GridLayoutManager field-init context bug** — `MainActivity.java:46` calls `new GridLayoutManager(getBaseContext(), 2)` at field initialization, before `onCreate()`.
4. **TrainSms reflection metric** — training similarity metric relies on reflection-heavy simmetrics path.
5. **Duplicated stopwords** — stopword list duplicated between sources (comma-separated inline list).
6. **WAL-less backup copy** — DB backup copies the main database file only, ignoring the `-wal` file (data loss on restore).
7. **Unbounded threads / no timeouts** — ad-hoc `new Thread(runnable).start()` per action (e.g. `MainActivity.java:139,238`, `CategoryListItemHolder.java:155`, `SettingsFragment.java:71`); no executor bounds or timeouts.
8. **allowBackup=true** — `AndroidManifest.xml:12`; SMS-derived DB included in device backups.
9. **Missing DB indexes** — queries filter/join columns without supporting indexes.

## Architecture Snapshot

- Pure Java Android app (no Kotlin), classic Activity/Fragment + static bridge classes (`DatabaseBridge`, `ExternalContentBridge`) rather than MVVM; no ViewModel/LiveData/DataBinding usage.
- UI built around `MainActivity` + `SmsActivity` + `SettingsActivity` with RecyclerView list adapters/holders performing direct DB access from background threads.

---

## Final QA Report (Revitalization Gate)

Recorded: 2026-08-23 · Head SHA: `d8d02882a8835d5bc8246ea5326ed0db94a49b8e` (`revitalize/mvvm`, 24 commits ahead of `master`)

### Build Matrix

Command: `./gradlew clean lint testDebugUnitTest assembleDebug assembleRelease --stacktrace`

- Result: `BUILD SUCCESSFUL`, exit code 0 (1m 39s; 95 actionable tasks)
- Artifacts: `app-debug.apk` (≈7.5 MB, 7,477,822 bytes), `app-release.apk` (~1.3 MB, `minifyEnabled` + `shrinkResources`)
- Signing note: local signing secrets absent — first attempt failed at `validateSigningRelease` (no local `keystore.jks`; config is env-driven for CI). Adapted by generating a throwaway local keystore so the release path ran end-to-end (V2 signature verified via apksigner); throwaway keystore deleted after QA. Production builds are signed in CI from secrets.

### Unit Tests (anti-flake ×3)

| Run | tests | failures | errors | skipped |
|---|---|---|---|---|
| 1 (in matrix) | 95 | 0 | 0 | 0 |
| 2 | 95 | 0 | 0 | 0 |
| 3 | 95 | 0 | 0 | 0 |

16 suites (baseline had 4). Test classpath additions recorded here per Task 1 deferred note: junit 4.13.2, mockito-core/mockito-inline 4.11.0, robolectric 4.11.1.

### Lint vs Baseline

Baseline 0 errors / 44 warnings → **now 0 errors / 50 warnings (+6)**.

Clusters: UnusedResources 29, GradleDependency 6, ObsoleteSdkInt 5, NewerVersionAvailable 3, RedundantLabel 2, single hits: Autofill, DataExtractionRules, HardcodedText (`choose_category_list_item.xml:11`), Overdraw (`tool_bar.xml:8`), PluralsCandidate. No security-severity findings.

### Security Review

- Manifest requests exactly `READ_SMS`, `READ_CONTACTS`, `INTERNET`; `android:allowBackup="false"` confirmed. Stale `tools:ignore="AllowBackup"` token remains (cosmetic, deferred minor).
- Branch diff secret scan (`git diff master...HEAD` vs keystore/password/token/private-key/JWT patterns): only GitHub Actions `${{ secrets.* }}` references and SMS test-fixture strings ("your one time password is 456789…"). No credentials in tree or branch diff.
- No `.jks`/`.keystore`/`.p12`/`.pem` files tracked, and this branch adds no APKs — but 7 legacy **signed release APKs are already tracked** under `app/apks/` on master (`GroupingMessages 1.0.apk` … `1.6.apk` incl `[AVOID]GroupingMessages 1.4.apk`, master-era commits ecf9a93/d2e9743). Removal recommended (triage below).
- `.gitignore` covers `/build`, `.gradle`, `local.properties`, `/captures`. Local IDE/tooling dirs (`.codegraph/`, `.opencode/`, `.project`, `.settings/`, `docs/superpowers/`) are untracked but not ignored — hygiene candidate.
- Informational (pre-existing): the deleted `.travis.yml` contained a Travis token blob persisting in git history only.

### Emulator-Blocked Items (no emulator/device available in this CLI environment)

| Smoke checklist item | Partial static/Robolectric evidence |
|---|---|
| Fresh install → grant READ_SMS/READ_CONTACTS → Unknown category appears | DB bootstrap/default categories covered by Robolectric suite; runtime-permission dialog flow unverified |
| Receive/train an SMS → category assigned | Training pipeline + hybrid engine covered by `SmsCategorizerTest`/`EngineCoreTest` goldens; on-device receiver unverified |
| Swipe behaviors | ItemTouchHelper attach-once dedup verified in code review (T15); gesture UX unverified |
| Add/rename/delete category | DAO + ViewModel CRUD unit tests pass; dialogs unverified |
| Settings reset/export/import round-trip | WAL-aware backup/restore unit-tested (`DatabaseBackup`, T8); device file I/O unverified |
| Airplane mode ON → settings version check graceful error, no crash | `checkLatestVersion` error-path state machine unit-tested (T12); live network-off behavior unverified |
| Categorization accuracy smoke (shipped-order vs meeting texts + typo'd/reordered variants, unrelated stays Unknown) | Near-copy/unrelated variants asserted in classifier unit goldens; real-device accuracy smoke unverified |

**Upgrade smoke (Step 4):** over-install APK flow EMULATOR-BLOCKED. Covered instead by the `MigrationTest` harness (v1→v3 and v2→v3 paths): data survives, `DATABASE_VERSION=3`, indexes `idx_sms_category`/`idx_sms_date`/`idx_sms_visibility` present (`DatabaseHelper` applies `CREATE INDEX IF NOT EXISTS` on create and upgrade). Legacy stored similarity-algorithm prefs map to `legacyLevenshtein` without crash (historical threshold semantics preserved at default 80); unset/stale values fall back to `balanced`.

### Known Behavior Changes Ledger

1. **Default algorithm/threshold** — new installs default to `balanced` @ similarity score 25 (re-baselined for cosine+dice scale); legacy whole-string default was 80.
2. **Stale-pref mapping** — stored `levenshtein`/`jaroWinkler` aliases map to `legacyLevenshtein` (old semantics kept); unset/stale values fall back to `balanced`.
3. **Swipe-vanish timing shift** — sub-second window after swipe-commit refresh; adjudicated acceptable (T15).
4. Contact-name display resolves all addresses (legacy: `person != 0` only) — broader coverage, cosmetic divergence.
5. Category-picker empty edge case no longer renders a literal "- null" title suffix.

### Deferred-Minors Triage (input to F1–F3)

**MUST-FIX-BEFORE-MERGE**

- `SmsActivity.java:230` — `SmsDao.getById` may return null (row deleted while picker open) → NPE on disk thread; add null-check + bail (T11 final-review triage candidate).
- `release.yml:18` — JDK 17 pinned vs JDK 21 in ci/instrumented (T17 confirmed carry-forward; assigned to F2 wave).

**POST-MERGE**

- High priority: import-failure path deletes uncheckpointed `-wal`/`-shm` after close (pre-existing data-loss edge, T8).
- Docs: README's `GroupMessagingBackupV3` mention mirrors the version-derived filename (`DatabaseBackup.java:28-29` builds `"GroupMessagingBackupV" + DATABASE_VERSION`, so V3 is current truth) — revisit wording only at schema v4 (T17); contact-name display divergence disclosure (T11, also ledgered above).
- Repo hygiene / supply-chain: remove the 7 legacy signed APKs tracked at `app/apks/` (v1.0–v1.6, master-era commits ecf9a93/d2e9743; branch adds none) via `git rm` — POST-MERGE, can ride the F2 CI wave; optional history rewrite given signed artifacts embed build provenance.
- CI: `setup-gradle@v3` straggler in `release.yml:22` (T16); `-PversionCode=0` falsy fallback nit (T16).
- Classifier hardening: negative-IDF unguarded if corpusSize < maxDf; weak determinism test (insertion order only); tie-break favors later category; non-ASCII truncation ceiling (T19/T20).
- Test hygiene: `SchemaTest` closes singleton-held DB; `TrainSmsTest` reflection; `ModelLayerTest` tautological assertSame; per-column ContentValues rebuild; `CategoriesViewModelTest` reflection; `MigrationTest` reflection on private `sInstance`.
- Cleanup: `SmsDao.updateMapInTransaction` misleading name; dead `cursor != null` DAO guards; `markRead(List<Long>)` has no production caller; sticky-redelivery window (single-live-event wrapper candidate); MainActivity onDestroy count-map fields not nulled; stale-count window in count observers (pre-existing parity).
- UI polish: transient flash of in-flight-undo rows after swipe-commit refresh; `ChangeCategoryActivity` dialog-theme double-padding risk (verify on device/F3); redundant `setTitle` in `SettingsActivity`; holder getter placement style.
- Trivial: `DatabaseContract.java` missing EOF newline; double blank line `MainActivity.java:151`; stale `tools:ignore="AllowBackup"` token + manifest EOF newline; `ListPreference` blank summary for stale stored value; brittle full-precision goldens; dead LinkedHashMap block `SmsCategorizerTest.java:1260`.
- Resolved/closed en route: `BACKUP_DB_PATH` consolidation (done, `SettingsFragment` references `DatabaseBackup.BACKUP_DB_PATH`); `PerformanceBenchmarkTest` reflection (removed in T20); contract-constants carry-forward (done in T7); alias-training supersession (resolved by T20 mapping).
- Device-QA riders (emulator-blocked above): fresh-install permission flow, categorization accuracy smoke, dialog padding check — run before public rollout.

### Tag Recommendation

**Recommend `v1.7.0`** (current 1.6). Minor bump fits scope: major internal revitalization (MVVM layering, hybrid TF-IDF classifier, DB v3 + indexes, WAL-aware backup, modernized CI) with user-facing feature set preserved; migrations and pref mapping keep upgrades non-breaking; disclosed behavior changes are parameter-level, not feature removals. Conditions before tagging: land the two MUST-FIX-BEFORE-MERGE items (F-wave), and given local emulator-blocked smoke, run the device-QA rider list on hardware or CI instrumented before public rollout.
