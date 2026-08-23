# GroupingMessages Revitalization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Modernize the GroupingMessages Android SMS auto-categorizer (pure Java) to MVVM architecture, current SDK/toolchain, secure defaults, real test coverage, modern CI, and accurate docs — with zero feature regressions.

**Architecture:** Hand-rolled MVVM: immutable model POJOs → typed DAOs over SQLite via a thread-safe `AppDatabase` singleton → `LiveData`-exposing ViewModels executed via `AppExecutors` → Activities that only observe and render. No Room, no Hilt, no Kotlin.

**Tech Stack:** Java 17, Android SDK (minSdk 24 → targetSdk/compileSdk 35), AGP latest stable pair w/ Gradle wrapper, androidx (lifecycle-livedata, appcompat, recyclerview, preference, material), Robolectric 4.x, JUnit 4, GitHub Actions.

## Global Constraints

- Feature parity except explicitly listed bugfixes. No new user-visible features.
- Java only. No Kotlin, no Room, no Hilt, no Dagger, no RxJava.
- minSdk stays 24. Final state: compileSdk 35, targetSdk 35.
- Every task ends green: `./gradlew lint testDebugUnitTest assembleDebug` exits 0.
- One conventional commit per task (`fix:`, `refactor:`, `feat:`, `chore:`, `docs:`, `ci:`). Never commit keystore/secrets.
- Work happens on branch `revitalize/mvvm`.
- Pin exact dependency versions at execution time (query Maven Central for latest stable when a task says so).
- Preserve user-visible behavior: same strings shown to users (unless extracting hardcoded ones verbatim), same swipe semantics, same training pipeline order, same toast counts.
- Existing flaky timing-based tests may be converted to functional asserts only when a task touches them; otherwise leave alone until Phase 6.

---

### Task 1: Baseline record + branch setup

**Files:**
- Create: `BASELINE.md`
- Create branch: `revitalize/mvvm`

**Interfaces:**
- Produces: `BASELINE.md` documenting pre-change state; branch `revitalize/mvvm` all later tasks commit to.

- [ ] **Step 1:** `git checkout -b revitalize/mvvm` from `master`.
- [ ] **Step 2:** Run and capture: `./gradlew clean lintDebug testDebugUnitTest assembleDebug --stacktrace`. Record exit code, unit-test count, lint issue counts (errors/warnings) in `BASELINE.md`.
- [ ] **Step 3:** Write `BASELINE.md`: date, base commit SHA, Gradle/AGP versions, compileSdk/targetSdk/minSdk, dependency list w/ versions, test count, lint error+warning counts, known-bug list (FK comma fresh-install crash, phantom WRITE_EXTERNAL_STORAGE request, GridLayoutManager field-init context bug, TrainSms reflection metric, duplicated stopwords, WAL-less backup copy, unbounded threads/no timeouts, allowBackup=true, missing DB indexes).
- [ ] **Step 4:** Commit: `git add BASELINE.md && git commit -m "chore: record pre-revitalization baseline"`

### Task 2: Fix fresh-install crash (missing comma in Sms CREATE_TABLE)

**Files:**
- Modify: `app/src/main/java/in/rahulja/groupingmessages/DatabaseContract.java` (~lines 182-185)
- Test: `app/src/test/java/in/rahulja/groupingmessages/SchemaTest.java` (new)

**Interfaces:**
- Consumes: existing `DatabaseContract.Sms.CREATE_TABLE` constant.
- Produces: valid schema on fresh install; `DatabaseHelper.DATABASE_VERSION = 3` is set later (Task 6) — do NOT bump version here.

- [ ] **Step 1:** Read `DatabaseContract.java`; locate the two adjacent `FOREIGN KEY(...)` clauses joined without `COMMA_SEP` between them inside `CREATE_TABLE + ...` concatenation.
- [ ] **Step 2:** Write failing Robolectric test `SchemaTest`:

```java
@RunWith(RobolectricTestRunner.class)
@Config(sdk = {24})
public class SchemaTest {
    @Test
    public void freshInstallCreatesValidSchema() {
        Context ctx = ApplicationProvider.getApplicationContext();
        SQLiteDatabase db = new DatabaseHelper(ctx).getWritableDatabase();
        Cursor c = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null);
        List<String> tables = new ArrayList<>();
        while (c.moveToNext()) tables.add(c.getString(0));
        c.close();
        assertThat(tables).contains("sms", "category", "config", "android_metadata");
    }
}
```

(Use `assertj-core` or plain JUnit asserts if assertj absent — plain `assertTrue(tables.contains("sms"))` etc.)
- [ ] **Step 3:** Run `./gradlew testDebugUnitTest --tests "*SchemaTest*"` — expect FAIL (SQLiteException near FOREIGN KEY).
- [ ] **Step 4:** Insert the missing `COMMA_SEP` between the two FOREIGN KEY clauses. Run again — PASS.
- [ ] **Step 5:** Full gate: `./gradlew lint testDebugUnitTest assembleDebug`.
- [ ] **Step 6:** Commit `fix: add missing comma between foreign keys in sms table creation`.

### Task 3: Remove phantom storage permission request

**Files:**
- Modify: `app/src/main/java/in/rahulja/groupingmessages/MainActivity.java` (`checkAndGetPermissions()`)

**Interfaces:**
- Produces: permission flow requesting only READ_SMS + READ_CONTACTS (INTERNET needs no runtime grant).

- [ ] **Step 1:** In `checkAndGetPermissions()`, remove `Manifest.permission.WRITE_EXTERNAL_STORAGE` from the requested-permission list and its branch in the result handling.
- [ ] **Step 2:** Verify `AndroidManifest.xml` contains no WRITE_EXTERNAL_STORAGE entry (it shouldn't; do not re-add).
- [ ] **Step 3:** Gate + commit `fix: drop obsolete WRITE_EXTERNAL_STORAGE runtime request`.

### Task 4: TrainSms determinism fixes (metric selection + stopwords dedupe)

**Files:**
- Modify: `app/src/main/java/in/rahulja/groupingmessages/TrainSms.java`
- Modify: `app/src/main/res/values/arrays.xml` (stopwords array)
- Test: `app/src/test/java/in/rahulja/groupingmessages/TrainSmsTest.java` (new)

**Interfaces:**
- Produces: `static StringMetric getMetric(String name)` returning one of levenshtein / normalizedLevenshtein / jaroWinkler, default levenshtein; single deduped `STOP_WORDS` source.

- [ ] **Step 1:** Replace reflection-based `getMetric` with explicit switch:

```java
private static StringMetric getMetric(String prefName) {
    if (prefName == null) return new Levenshtein();
    switch (prefName) {
        case "normalizedLevenshtein": return new NormalizedLevenshtein();
        case "jaroWinkler":           return new JaroWinklerSimilarity();
        case "levenshtein":
        default:                      return new Levenshtein();
    }
}
```

Keep any caller behavior identical otherwise (same pipeline order, same thresholds).
- [ ] **Step 2:** Deduplicate the stopword array declared twice in TrainSms into ONE array; remove junk tokens `"an "`, `"i "`, `" i"`. Keep the surviving tokens exactly matching the values in `arrays.xml` string-array used by preferences (sync both if names differ — single source in arrays.xml preferred if TrainSms can read resources via context; otherwise keep the Java array and note the sync requirement in a comment-free way by making arrays.xml authoritative if already wired).
- [ ] **Step 3:** Test `TrainSmsTest` (Robolectric): assert `getMetric("levenshtein")`, `getMetric("normalizedLevenshtein")`, `getMetric("jaroWinkler")` return distinct correct classes; `getMetric("garbage")` and `getMetric(null)` return Levenshtein, never null.
- [ ] **Step 4:** Failing→passing cycle for Step 3, full gate, commit `fix: replace reflection metric lookup and dedupe stopwords`.

### Task 5: Immutable model layer

**Files:**
- Create: `app/src/main/java/in/rahulja/groupingmessages/model/Sms.java`
- Create: `app/src/main/java/in/rahulja/groupingmessages/model/Category.java`
- Create: `app/src/main/java/in/rahulja/groupingmessages/model/ConfigEntry.java`

**Interfaces:**
- Produces (later tasks consume):
  - `Category(long id, String name, int color)` + getters `getId()/getName()/getColor()`; `static Category fromCursor(Cursor)` reading columns via `DatabaseContract.Category.*`; `ContentValues toContentValues()`.
  - `Sms(long id, long categoryId, long date, int visibility, String address, String body)` + getters; `fromCursor(Cursor)`; `toContentValues()`.
  - `ConfigEntry(String key, String value)` + getters; `fromCursor(Cursor)`.
  - All fields `private final`, classes effectively immutable, no setters.

- [ ] **Step 1:** Read `DatabaseContract.java` column names; write the three POJOs exactly as specified above. `fromCursor` must NOT close the cursor (caller owns it) and must use `cursor.getColumnIndexOrThrow(DatabaseContract.X.COL)`.
- [ ] **Step 2:** Unit-test constructors/getters/fromCursor with a mocked Cursor (Mockito): correct column mapping, ContentValues round-trip equality for Category and Sms.
- [ ] **Step 3:** Gate + commit `feat: add immutable model layer`.

### Task 6: Thread-safe AppDatabase + v3 index migration

**Files:**
- Create: `app/src/main/java/in/rahulja/groupingmessages/db/AppDatabase.java`
- Modify: `app/src/main/java/in/rahulja/groupingmessages/DatabaseHelper.java` (onUpgrade → v3)
- Test: `app/src/test/java/in/rahulja/groupingmessages/MigrationTest.java` (new)

**Interfaces:**
- Produces:
```java
public final class AppDatabase {
    public static synchronized SQLiteDatabase get(Context ctx); // lazy init, never closed mid-app
    public static synchronized void close(Context ctx);          // only for backup/import flows
}
```
Backed by the existing `DatabaseHelper` singleton; replaces all `DatabaseBridge.init/unInitialize` churn.

- [ ] **Step 1:** Refactor `DatabaseHelper` to strict singleton (`getInstance(Context)`); keep schema SQL identical except adding v3.
- [ ] **Step 2:** `onUpgrade` handles v1→v3 and v2→v3 by running through intermediate steps then executing:
```sql
CREATE INDEX IF NOT EXISTS idx_sms_category ON sms(category_id);
CREATE INDEX IF NOT EXISTS idx_sms_date ON sms(date);
CREATE INDEX IF NOT EXISTS idx_sms_visibility ON sms(visibility);
```
Bump `DATABASE_VERSION` to 3. Add these same indexes to `onCreate` path (they come free via onCreate re-run on fresh installs after contract update — verify).
- [ ] **Step 3:** Write `AppDatabase` exactly per interface above.
- [ ] **Step 4:** MigrationTest (Robolectric, FrameworkSQLiteOpenHelperFactory pattern or direct helper use with an old-version DB file created by raw SQL): create DB at version 1 with minimal old schema rows → open via helper → assert version 3, indexes exist (`sqlite_master` query `type='index' AND name LIKE 'idx_sms_%'`), prior rows intact. Repeat starting at version 2.
- [ ] **Step 5:** Gate + commit `feat: thread-safe database singleton with v3 index migration`.

### Task 7: Typed DAOs, retire DatabaseBridge

**Files:**
- Create: `app/src/main/java/in/rahulja/groupingmessages/db/SmsDao.java`
- Create: `app/src/main/java/in/rahulja/groupingmessages/db/CategoryDao.java`
- Create: `app/src/main/java/in/rahulja/groupingmessages/db/ConfigDao.java`
- Delete: `app/src/main/java/in/rahulja/groupingmessages/DatabaseBridge.java`
- Modify: all former `DatabaseBridge` call sites (MainActivity, SmsActivity, TrainSms, ExternalContentBridge, SwipeUtil, adapters, ChangeCategoryActivity, SettingsFragment reset/export/import helpers, AddCategoryFragment)
- Test: `app/src/test/java/in/rahulja/groupingmessages/DaoTest.java` (new)

**Interfaces:**
- Produces (signatures the ViewModels rely on — adapt names minimally if reality differs, but keep semantics):
  - `CategoryDao`: `List<Category> getAllOrderedByPipelineOrder()`; `Category getById(long)`; `long insert(String name)`; `int rename(long, String)`; `int deleteById(long)`; `int getCountPerCategory(long categoryId)`; `void ensureDefaultsExist()` (creates Unknown etc. exactly like today's bootstrap).
  - `SmsDao`: `List<Sms> getVisibleByCategory(long categoryId)` (same ordering as today's queries); `int markRead(List<Long> ids)`; `int setVisibility(List<Long> ids, int visibility)`; `int changeCategory(List<Long> ids, long newCategoryId)`; `int deleteByIds(List<Long> ids)`; `int countAll()`; `int countUnreadInCategory(long)`.
  - `ConfigDao`: `String getValue(String key)`; `void put(String key, String value)`.
  - All DAO methods take `Context` (or are constructed with one) and obtain the DB via `AppDatabase.get(ctx)`. Cursors consumed with try-with-resources. Batched operations use transactions (`beginTransaction/setTransactionSuccessful/endTransaction`) preserving today's batching perf work.

- [ ] **Step 1:** Inventory every static method on `DatabaseBridge` and map each to a DAO method or inline deletion (write mapping in report).
- [ ] **Step 2:** Implement the three DAOs.
- [ ] **Step 3:** Migrate ALL call sites mechanically (same logic, DAO calls). Delete `DatabaseBridge.java`.
- [ ] **Step 4:** DaoTest (Robolectric, real in-memory DB): CRUD round-trips for each DAO incl. batch ops in transaction, float/color precision round-trip, cursor cleanup (no leaks), ordering matches legacy queries.
- [ ] **Step 5:** Gate + commit `refactor: replace DatabaseBridge god-class with typed DAOs`.

### Task 8: Safe backup/export-import

**Files:**
- Modify: export/import implementation (wherever importDB/exportDB live — likely SettingsFragment/ExternalContentBridge; locate via grep)
- Test: `app/src/test/java/in/rahulja/groupingmessages/BackupRestoreTest.java` (new)

**Interfaces:**
- Produces: export = WAL checkpoint TRUNCATE → copy to destination atomically; import = close DB → restore → reopen; no partial-state window on failure.

- [ ] **Step 1:** Before copying on export: `db.rawQuery("PRAGMA wal_checkpoint(TRUNCATE)", null)` and fully drain the cursor. Copy to `<dest>.tmp` then atomic rename.
- [ ] **Step 2:** Import: close via `AppDatabase.close(ctx)`, copy file over DB path (+ `-wal`/`-shm` removal), reopen lazily.
- [ ] **Step 3:** BackupRestoreTest (Robolectric): seed DB → export to temp dir → mutate DB → import → assert original data back; assert no `.tmp` residue; corrupt-file import surfaces error without crashing app process (exception propagates to caller which shows existing error toast).
- [ ] **Step 4:** Gate + commit `fix: checkpoint WAL and swap atomically during backup/restore`.

### Task 9: AppExecutors

**Files:**
- Create: `app/src/main/java/in/rahulja/groupingmessages/vm/AppExecutors.java`

**Interfaces:**
- Produces:
```java
public class AppExecutors {
    private static final ExecutorService DISK_IO = Executors.newSingleThreadExecutor();
    public static void disk(Runnable r);
    public static void main(Runnable r);   // Handler(Looper.getMainLooper())
}
```

- [ ] **Step 1:** Implement exactly above. No shutdown hooks, no config.
- [ ] **Step 2:** Trivial unit test: `disk()` executes on non-main thread, `main()` posts to main looper (Robolectric `shadowOf(getMainLooper()).idle()`).
- [ ] **Step 3:** Gate + commit `feat: shared app executors`.

### Task 10: CategoriesViewModel + MainActivity migration

**Files:**
- Create: `app/src/main/java/in/rahulja/groupingmessages/vm/CategoriesViewModel.java`
- Modify: `app/src/main/java/in/rahulja/groupingmessages/MainActivity.java`
- Test: `app/src/test/java/in/rahulja/groupingmessages/CategoriesViewModelTest.java` (new)

**Interfaces:**
- Produces:
```java
public class CategoriesViewModel extends ViewModel {
    public LiveData<List<Category>> getCategories();   // ordered as today's pipeline display
    public void refresh();                              // disk executor → setValue on main
    public void addCategory(String name);               // preserves today's toast feedback flow
    public void deleteCategory(long id);                // preserves cascade/count-check behavior
}
```

- [ ] **Step 1:** Implement VM using `MutableLiveData`, `AppExecutors.disk/main`, CategoryDao. Preserve EXACT pipeline processing order currently produced by MainActivity background work (note the order in code comments-free form: replicate the same sort/query).
- [ ] **Step 2:** MainActivity: instantiate GridLayoutManager in `onCreate` (kills field-init context bug), observe VM, move ALL raw Threads/runOnUiThread bodies into VM + observer callbacks. Keep scroll-position preservation behavior identical (same `scrollToPositionWithOffset` logic if present). Keep permission flow from Task 3 intact. Toast text/count identical.
- [ ] **Step 3:** CategoriesViewModelTest (Robolectric): refresh emits list ordered like legacy; add/delete delegate to DAO (mockito-verify or real DB).
- [ ] **Step 4:** Manual-parity checklist in report: spinner contents/order, empty-state text, category-count badges/toasts unchanged.
- [ ] **Step 5:** Gate + commit `refactor: MainActivity onto CategoriesViewModel`.

### Task 11: SmsListViewModel + SmsActivity migration

**Files:**
- Create: `app/src/main/java/in/rahulja/groupingmessages/vm/SmsListViewModel.java`
- Modify: `app/src/main/java/in/rahulja/groupingmessages/SmsActivity.java`
- Test: `app/src/test/java/in/rahulja/groupingmessages/SmsListViewModelTest.java` (new)

**Interfaces:**
- Produces:
```java
public class SmsListViewModel extends ViewModel {
    public LiveData<List<Sms>> getSms(long categoryId);
    public void refresh(long categoryId);
    public void swipeDelete(Sms sms, long categoryId);   // untrained→delete row; trained→visibility=0 (EXACT legacy semantics)
    public void markRead(List<Long> ids);
    public void moveToCategory(List<Long> ids, long targetId);
}
```

- [ ] **Step 1:** Implement VM; preserve legacy swipe branching byte-for-byte in semantics (read current SwipeUtil/SmsActivity logic first; if the trained/untrained decision lives outside the activity, keep that call path, just relocate threading).
- [ ] **Step 2:** SmsActivity: observe VM; remove raw threads/runOnUiThread; guard UI updates with `isFinishing()/isDestroyed()` checks where legacy did not (bugfix allowed: prevents NPEs on rotated/finished activities).
- [ ] **Step 3:** SmsListViewModelTest: swipeDelete on trained sms sets visibility=0 not delete; on untrained deletes row; markRead updates rows; refresh ordering equals legacy query ordering.
- [ ] **Step 4:** Gate + commit `refactor: SmsActivity onto SmsListViewModel`.

### Task 12: SettingsViewModel + hardened version check

**Files:**
- Create: `app/src/main/java/in/rahulja/groupingmessages/vm/SettingsViewModel.java`
- Modify: `SettingsFragment.java` (version-check portion; leave PreferenceFragment wiring else intact)
- Test: `app/src/test/java/in/rahulja/groupingmessages/VersionCheckTest.java` (new)

**Interfaces:**
- Produces:
```java
public class SettingsViewModel extends ViewModel {
    public LiveData<OptionalBoolean> checkLatestVersion();  // tri-state: upToDate/outdated/error — map to existing UI outcomes
}
```
(If project avoids java.util.Optional, use a small enum `VersionState {UP_TO_DATE, OUTDATED, ERROR}`.)

- [ ] **Step 1:** Move HTTP version check out of Fragment thread into VM. On the connection: `setConnectTimeout(5000)`, `setReadTimeout(5000)`, wrap whole body in try/catch(Throwable) → ERROR state. Same URL parsing, same semver comparison outcome mapping to existing UI (dialog/toast text identical).
- [ ] **Step 2:** Fragment observes result; UI calls guarded by `isAdded()`.
- [ ] **Step 3:** VersionCheckTest: mock HttpURLConnection (Mockito) — success/outdated/malformed-json/timeout-throw paths each produce expected VersionState; assert timeouts were set.
- [ ] **Step 4:** Gate + commit `refactor: settings version check onto viewmodel with network timeouts`.

### Task 13: Platform upgrade (SDK 35, deps, edge-to-edge)

**Files:**
- Modify: `app/build.gradle`, `build.gradle`, `gradle/wrapper/gradle-wrapper.properties`
- Modify: layout/activity files needing insets (determine by build+visual diff)

**Interfaces:**
- Produces: compileSdk=35, targetSdk=35; latest stable AGP+Gradle pair; zero unused deps; insets handled.

- [ ] **Step 1:** Upgrade Gradle wrapper + AGP to latest stable compatible pair (check `./gradlew wrapper --gradle-version X` works; AGP per compatibility matrix). Update `compileSdk 35`, `targetSdk 35`.
- [ ] **Step 2:** Grep for Lombok imports/usages — expect none → remove dependency. Grep `legacy-support-v4`, `vectordrawable` usages — remove if unreferenced. Do NOT remove anything referenced.
- [ ] **Step 3:** Edge-to-edge on 35: for each Activity root, apply:
```java
ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.root), (v, insets) -> {
    Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
    v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
    return WindowInsetsCompat.CONSUMED;
});
```
Adjust per-layout (toolbars/FABs must not sit under status/navigation bars).
- [ ] **Step 4:** Build + lint; fix deprecations surfaced by SDK 35 (e.g., registerReceiver flags if lint errors).
- [ ] **Step 5:** Gate + commit `feat: target android 15 with updated toolchain and inset handling`.

### Task 14: Manifest hardening

**Files:**
- Modify: `app/src/main/AndroidManifest.xml`

**Interfaces:**
- Produces: `android:allowBackup="false"` on `<application>`; launchMode `singleTask` on MainActivity + SmsActivity (replacing singleInstance).

- [ ] **Step 1:** Apply both changes. Note in report why allowBackup=false (SMS content privacy).
- [ ] **Step 2:** Gate + commit `fix: disable backups and normalize launch modes`.

### Task 15: UI polish (strings, DiffUtil, responsive grid, a11y)

**Files:**
- Modify: `app/src/main/res/values/strings.xml` + every hardcoded-string site (MainActivity dialogs, SmsActivity swipe label 'Delete', OK/Cancel, toasts)
- Modify: category/sms RecyclerView adapters → `ListAdapter<T, VH>` with `DiffUtil.ItemCallback`
- Modify: grid span calculation (responsive columns)
- Test: `app/src/test/java/in/rahulja/groupingmessages/DiffCallbackTest.java` (new)

**Interfaces:**
- Produces: `CategoryDiffCallback extends DiffUtil.ItemCallback<Category>`, `SmsDiffCallback extends DiffUtil.ItemCallback<Sms>` (areItemsTheSame=id; areContentsTheSame=field equality).

- [ ] **Step 1:** Extract every hardcoded user-visible string to strings.xml (values VERBATIM — no rewording).
- [ ] **Step 2:** Convert adapters to ListAdapter+DiffUtil; submitList instead of rebuild; preserve click/swipe wiring exactly. Scroll position: rely on DiffUtil (removes drawUi scroll hack if it existed solely for rebuild flicker — verify visual parity note in report).
- [ ] **Step 3:** Responsive span: `int spans = max(2, screenWidthDp / 320)` style calculation in onCreate (or keep fixed 2 if that IS current product behavior — default to preserving current fixed behavior unless trivially better; report choice).
- [ ] **Step 4:** A11y pass: touch targets ≥48dp, contentDescriptions on icon-only views (reuse existing #98 accessibility work; fill gaps only).
- [ ] **Step 5:** DiffCallbackTest: items-same/contents-same/false-positive-content cases.
- [ ] **Step 6:** Gate + commit `refactor: diffutil adapters extracted strings and responsive grid`.

### Task 16: CI overhaul

**Files:**
- Modify: `.github/workflows/ci.yml`
- Create: `.github/workflows/instrumented.yml`
- Create: `.github/workflows/dependencies.yml`
- Modify: `.github/workflows/release.yml` (inject versionCode/versionName from tag)
- Delete: `.travis.yml`
- Modify: `renovate.json` → `"extends": ["config:recommended"]`
- Resolve sonar duplication: pick ONE mechanism — enable `org.sonarsource.scanner.gradle:sonarqube-gradle-plugin` in build.gradle gated on CI `SONAR_TOKEN` secret presence, DELETE orphaned `sonar-project.properties` (preferred since gradle-centric).

**Interfaces:**
- Produces: ci.yml jobs = lint + unit tests (with `--stacktrace`, uploads test-report artifacts always); instrumented.yml = emulator matrix API 24 + 35 running `connectedDebugAndroidTest` (reactivecircus/android-emulator-runner@v2); dependencies.yml = `actions/dependency-review-action@v4` on PRs; release.yml derives versionCode/versionName from pushed tag (`vX.Y.Z` → versionName=X.Y.Z, versionCode computed Y*1000+Z*10+X or documented scheme) passed via `-PversionCode= -PversionName=` (add corresponding reads in app/build.gradle with defaults 16/1.6 preserved locally).

- [ ] **Step 1:** Apply ci.yml changes (keep existing job shape; append test task + artifact upload).
- [ ] **Step 2:** Create instrumented.yml + dependencies.yml per interfaces.
- [ ] **Step 3:** release.yml: compute vars, pass to gradle; app/build.gradle reads `-P` overrides. Bump local defaults remain 16/1.6.
- [ ] **Step 4:** Sonar: add plugin + `sonar { properties }` block (organization/project/key via env `SONAR_TOKEN`, `SONAR_HOST_URL`), delete sonar-project.properties.
- [ ] **Step 5:** Delete .travis.yml; update renovate.json preset.
- [ ] **Step 6:** Validate workflow YAML syntactically (actionlint if available, else `python3 -c yaml.safe_load`). Gate + commit `ci: modernize workflows with emulator tests dependency review and tag-driven versions`.

### Task 17: Documentation rewrite

**Files:**
- Modify: `README.md` (full rewrite)
- Create: `docs/ARCHITECTURE.md`

**Interfaces:**
- Produces: README reflecting truth (min 24/target 35, GH Actions badges, privacy section: all SMS processing on-device; only outbound network = GitHub releases version check; permissions rationale table). ARCHITECTURE.md: layer diagram (models → DAOs → AppDatabase → ViewModels → Activities), training-pipeline explanation (categorization engine incl. TF-IDF + char-trigram blend per research-categorization notes), schema v3 w/ indexes, backup format notes.

- [ ] **Step 1:** Rewrite README (no Travis references anywhere in repo afterwards: grep -ri travis must return nothing).
- [ ] **Step 2:** Write ARCHITECTURE.md from ACTUAL final code (read the migrated files; describe what exists). Document the categorization engine (Task 20): word TF-IDF cosine + char-trigram Dice blend, mode setting, threshold semantics, Unknown fallback.
- [ ] **Step 3:** Gate + commit `docs: rewrite readme and add architecture guide`.

### Task 18: QA gate — full verification

**Files:**
- Modify: `BASELINE.md` (append final report section)

**Interfaces:**
- Produces: verification evidence + tag candidate recommendation.

- [ ] **Step 1:** `./gradlew clean lint testDebugUnitTest assembleDebug assembleRelease --stacktrace` — all green. Release build requires signing config; if signing secrets absent locally, assembleRelease may produce unsigned APK — acceptable, note it (CI does signed builds).
- [ ] **Step 2:** Anti-flake: run `testDebugUnitTest` 3 consecutive times — identical pass count each time.
- [ ] **Step 3:** Emulator smoke checklist (instrumented run or manual adb): fresh install → grant READ_SMS/READ_CONTACTS → Unknown category appears → receive/train an SMS → category assigned → swipe behaviors → add/rename/delete category → settings reset/export/import round-trip → airplane-mode ON → settings version check shows graceful error, no crash. Categorization accuracy smoke: train category A on "Your order has shipped and will arrive Tuesday" + category B on "Meeting confirmed for tomorrow at 3pm", then verify near-copy variants (typo'd, reordered) land in the right categories, unrelated text stays Unknown.
- [ ] **Step 4:** Upgrade smoke: install release v1.6 (or construct v1/v2 DB via MigrationTest harness), install new build over it → data survives, version=3, indexes present; legacy stored similarity-algorithm pref value maps to a working mode without crash (Task 20 mapping).
- [ ] **Step 5:** Security review pass: `lint` security severities, manifest permissions minimal (READ_SMS, READ_CONTACTS, INTERNET only), no secrets in tree (`git log -p | grep -i keystore` spot check on branch diff).
- [ ] **Step 6:** Append QA results to BASELINE.md; commit `test: record revitalization qa evidence`; recommend tag `v1.7.0` candidate in report.

---

### Task 19: Categorization engine core (research-driven)

> Executes AFTER Task 15, BEFORE Task 16. Research basis: `.superpowers/sdd/2026-08-23-revitalization-mvvm/research-categorization.md` (read it first).

**Files:**
- Create: `app/src/main/java/in/rahulja/groupingmessages/classify/TextVectorizer.java`
- Create: `app/src/main/java/in/rahulja/groupingmessages/classify/CharTrigramProfile.java`
- Test: `app/src/test/java/in/rahulja/groupingmessages/classify/EngineCoreTest.java` (new)

**Interfaces:**
- Produces:
```java
public final class TextVectorizer {
    public TextVectorizer(List<String> stopwords);            // reuse TrainSms stopword list contents
    public Map<String, Double> tfIdfVector(String text, Map<String, Integer> documentFrequency, long corpusSize);
    // tf = count/tokenCount after normalize->lowercase->[^a-z0-9]+ split->stopword drop
    // idf = ln((corpusSize+1)/(df+1)); unseen term df=0
    public static double cosine(Map<String, Double> a, Map<String, Double> b); // 0 if either empty
    public static String normalize(String raw);               // lowercase + non-alphanumeric collapse to space
}
public final class CharTrigramProfile {
    public static Map<String, Integer> of(String normalizedText); // pad with '_' both ends, all trigrams w/ counts
    public static double dice(Map<String, Integer> a, Map<String, Integer> b); // 2*sum(min)/ (sumA+sumB); 0 if either empty
}
```

- [ ] **Step 1:** Implement both classes exactly per signatures. No deps beyond java.util/lang.
- [ ] **Step 2:** EngineCoreTest properties: cosine identical vectors = 1, disjoint = 0, empty = 0; dice identical profiles = 1, disjoint = 0; typo robustness: dice(normalize("meetng tommrw ok"), profile-of("meeting tomorrow ok")) > 0.5 while word-cosine of same pair < dice (documents the blend rationale); tf-idf weights rare terms higher within corpus fixture; stopword dropped from vector; deterministic across runs.
- [ ] **Step 3:** Failing→passing cycle, gate `./gradlew lint testDebugUnitTest assembleDebug`, commit `feat: categorization engine core vectorizer and trigram profile`.

### Task 20: Wire engine into training pipeline + settings modes

> Executes immediately after Task 19. Depends on Task 7 DAOs (exemplar corpus loading) and Task 10 VM structure.

**Files:**
- Modify: `app/src/main/java/in/rahulja/groupingmessages/TrainSms.java` (scoring internals only)
- Modify: `app/src/main/res/values/arrays.xml` (SimilarityAlgorithms + SimilarityAlgorithmsAlias)
- Modify: SettingsFragment preference handling only where algorithm pref is consumed
- Delete reflection usage in: `app/src/test/java/in/rahulja/groupingmessages/PerformanceBenchmarkTest.java`
- Test: `app/src/test/java/in/rahulja/groupingmessages/classify/CategorizationParityTest.java` (new)

**Interfaces:**
- Produces:
```java
// inside TrainSms (or classify/SmsCategorizer.java preferred):
// mode strings: balanced(0.6/0.4), wordsOnly(1.0/0), charactersOnly(0/1), legacyLevenshtein
// legacy pref value mapping: levenshtein|jaroWinkler -> legacyLevenshtein; any other stale value -> balanced
```

- [ ] **Step 1:** BEFORE touching scoring: write CategorizationParityTest recording CURRENT behavior — fixed corpus of ≥6 exemplar SMS across 2 categories + threshold, assert current getMetric-based pipeline's chosen category per probe message (golden master). Keep this test as behavioral contract harness; update goldens ONLY in the dedicated parity-evaluation step below with justification table old-vs-new per probe.
- [ ] **Step 2:** Build corpus loader via DAOs (trained exemplars = sms rows with visibility trained state / similar_to linkage as found in code). IDF over exemplar corpus computed once per classification batch.
- [ ] **Step 3:** Replace pairwise scoring with blend per research file; keep 1-NN decision rule, threshold pref semantics (0-100→0-1), Unknown fallback, pipeline order, `similar_to` writes unchanged. Mode selection reads pref; stale values mapped deterministically.
- [ ] **Step 4:** arrays.xml: replace 21 alias entries with the 4 modes above (display names verbatim-new; this prunes dead options that never functioned — documented bugfix). Ensure existing preference key unchanged.
- [ ] **Step 5:** Remove PerformanceBenchmarkTest reflection; benchmark new engine timing instead (functional assert: classify 100 msgs < 2s on Robolectric, no sleep-based timing asserts elsewhere).
- [ ] **Step 6:** Parity evaluation step: run CategorizationParityTest probes under BOTH engines; produce comparison table in report; goldens updated to NEW engine outcomes with one-line justification each (expected: typo/reorder probes improve, exact-match probes unchanged).
- [ ] **Step 7:** Gate, commit `feat: science-backed categorization engine with tfidf and char ngram blend`.
