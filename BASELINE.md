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
