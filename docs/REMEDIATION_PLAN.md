# Remediation and release plan — Grouping Messages

**Status:** implemented for the P0/P1 scope in the working tree; see §12 for the
verification record. Device-only checks remain open.

**Companion audit:** `docs/AUDIT-2026-11-20.md`  
**Target release:** 1.7.0 (`versionCode` 10700)  
**Scope:** fix P0/P1 first; add only the features that pay for themselves; get a clean
F-Droid/IzzyOnDroid submission without rewriting the app.

This plan is deliberately ordered. Each item names the files, the invariant being
restored, the test that proves it, and the acceptance condition. Sizes are rough:
S = under half a day, M = one to two days, L = three or more.

---

## 1. Anti-slop rules for this work

1. **No rewrite.** Keep Java, XML views, single `app` module, existing `Activity` ->
   `ViewModel` -> DAO layering. No Compose, Room, Hilt, coroutines, or Kotlin migration.
2. **Reuse the existing abstraction.** If a DAO, ViewModel, executor, or resource
   already owns the behavior, extend it. Do not add a parallel "manager".
3. **Smallest complete diff.** A fix reaches its callers, tests, strings, and docs; it
   does not add options nobody asked for.
4. **Delete before adding.** Unused methods, resources, and dependencies go first.
5. **No new dependency for a few lines.** Reimplement a 20-line Levenshtein before
   keeping an abandoned library; do not add SQLCipher for a first release.
6. **Comment only the why.** No narration of obvious code.
7. **Every non-trivial behavior gets one small test.** Data loss, dedupe, backup swap,
   permission denial, and release versioning are non-trivial.
8. **One release path.** No second "auto-patch" path, no mutable release tag, no manual
   version string outside the tag.

---

## 2. Phase 0 — Baseline and safety net (already done for this audit)

| Item | Evidence |
|---|---|
| Unit tests | 102/102 pass |
| Lint | 50 warnings recorded; report path documented |
| Unsigned release | deterministic SHA-256 across clean builds |
| Manifest | package, target 36, permissions, exported components inspected |
| Device | no device/AVD available; instrumented is a known gap |

Before touching app code, add the two checks that make later changes safe:

- **0.1 CI runs `testDebugUnitTest` + `lintDebug` on every PR** (already present; keep).
- **0.2 Release refuses to build from anything except `vX.Y.Z`** (implemented in the
  updated `.github/workflows/release.yml`).
- **0.3 Store metadata validation is a gate** (implemented in
  `.github/workflows/store.yml`; it will fail until the recipe and Fastlane files are
  committed).

---

## 3. Phase 1 — P0 release blockers

### R-01 — Tag-driven release, correct version code

**Problem:** A-01/A-02/A-06.

**Change:**

- `release.yml` triggers on `push: tags: ['v[0-9]+.[0-9]+.[0-9]+']` and on
  `workflow_dispatch` with an explicit existing tag.
- The tag is the single source of truth. Validate `^v[0-9]+\.[0-9]+\.[0-9]+$`.
- `versionName = tag without v`; `versionCode = major*10000 + minor*100 + patch`.
- Refuse to run if the tag does not exist or is not annotated/created as expected.
- The release job creates the GitHub release for that exact tag; it does not invent a
  tag and does not move one.

**Files:** `.github/workflows/release.yml`, `.github/workflows/master_debug_release.yml`.

**Test / proof:**

```bash
# in a throwaway clone
git tag v9.9.9
# workflow output must be versionName=9.9.9 versionCode=90909
# http status of the release after the run must reference v9.9.9
```

**Acceptance:** no path in the repository produces a `latest`-derived version; a second
release from the same tag is rejected or produces the same artifact, not a new code.

**Rollback:** revert the workflow file; no app data changes.

### R-02 — Signing key never touches the checkout

**Problem:** A-03.

**Change:**

- Decode `KEY_STORE_BASE64` into `$RUNNER_TEMP/keystore/release.jks` with `umask 077`.
- Sign with the Android SDK `apksigner` (or the Gradle signing config with
  `STORE_FILE` pointing at `$RUNNER_TEMP`), never `app/`.
- Remove the key in an `if: always()` step.
- Add a workflow assertion that no `*.jks` exists under `$GITHUB_WORKSPACE`.

**Files:** `.github/workflows/release.yml`.

**Test / proof:** `find "$GITHUB_WORKSPACE" -name '*.jks' -print -quit` is empty in the
post-step; `apksigner verify --print-certs` shows the expected certificate.

**Acceptance:** a failed build still leaves no key behind.

**Rollback:** workflow only.

### R-03 — Release artifact verification

**Problem:** A-04 and the absence of a machine check.

**Change:** after signing:

```bash
aapt2 dump badging  app-release.apk   # package, versionCode, targetSdk, no debuggable
apksigner verify --verbose --print-certs app-release.apk
sha256sum app-release.apk > app-release.apk.sha256
```

Fail on wrong package, wrong `versionCode`, `application-debuggable`, `testOnly`, or a
failed signature. Publish both APK and `.sha256`.

**Files:** `release.yml`.

**Acceptance:** a deliberately wrong `-PversionCode` fails the job (test once in a
branch, then remove the test commit).

### R-04 — Stop logging SMS content

**Problem:** A-07.

**Change:** in `SmsDao.insertInTransaction`, log only `_ID`/date or a row count; never
`trainedSmsMap.toString()`. Sweep every `Log.*` in `app/src/main` for `body`, `address`,
`trainedSmsMap`, `smsMap`, `strBody`, `strAddress`. Add a CI grep gate for those terms
on the same line as a `Log.` call.

**Files:** `app/src/main/java/in/rahulja/groupingmessages/db/SmsDao.java`, CI workflow.

**Test:** unit test that triggers a failed insert and asserts no body/address appears in
the captured log is awkward; the CI grep plus code review is the acceptance gate. Keep
one Robolectric test that a failed insert returns `-1` and leaves the transaction usable.

**Acceptance:** `grep -rnE 'Log\.[a-z]+\(.*(KEY_BODY|KEY_ADDRESS|trainedSmsMap|strBody|strAddress)' app/src/main` is empty.

### R-05 — F-Droid and Izzy metadata

**Problem:** A-05.

**Change:**

- Add `metadata/in.rahulja.groupingmessages.yml` for the first `v1.7.0` tag:
  `License: Apache-2.0`, `Categories: Phone & SMS`, `RepoType: git`, `Repo` GitHub,
  `Builds` with `commit: v1.7.0`, no `subdir`, `gradle: [yes, -PversionCode=10700,
  -PversionName=1.7.0]`, `AutoUpdateMode: Version v%v`, `UpdateCheckMode: Tags`.
- Add `fastlane/metadata/android/en-US/`: `title.txt`, `short_description.txt` (≤80),
  `full_description.txt`, `changelogs/10700.txt`, `images/icon.png` (512×512),
  `phoneScreenshots/1.jpg` and `2.jpg` from the current UI.
- Add `store.yml` validation: required files, short-description length, icon dimensions,
  at least two screenshots, recipe schema, tag/version match, APK signature/debuggable
  check, optional `AllowedAPKSigningKeys` fingerprint match.

**Files:** `metadata/`, `fastlane/`, `.github/workflows/store.yml`.

**Test:** `python3` validation and `fdroid lint` both pass locally; `store.yml` fails on
a deliberately wrong `CurrentVersionCode`.

**Acceptance:** a dry-run of the store workflow on a `v1.7.0` release is green.

### R-06 — Version defaults and docs

**Problem:** A-08.

**Change:** bump root `versionCode`/`versionName` to the next release when tagging
(10700 / 1.7.0), or document that the tag is authoritative and keep the defaults at the
last released version. Update `README.md` target/compile to 36, JDK 21, and the build
commands. Update `docs/LLM_MEMORY.md`, `docs/PRD.md`, and `docs/TD.md` so they describe
the tag-driven release, not the old auto-patch flow.

**Acceptance:** `grep -rn "API 35\|Gradle 8.5\|source compatibility Java 1.8" README.md docs/` returns only historical notes, if any.

---

## 4. Phase 2 — P1 data integrity and privacy

### S-01 — Ingest dedupe and watermark

**Problem:** B-01/B-02.

**Design (smallest correct shape):**

- Projection adds `_ID`.
- The map carries `_ID` into the `sms` row.
- `storeTrainedInboxSms` uses `insertWithOnConflict(..., CONFLICT_IGNORE)`; only count
  rows actually inserted.
- Query changes from `date > last` to `date >= last` (or `(date = last AND _id > lastId)`
  if an ID watermark is added). Duplicate provider IDs are ignored, so the boundary
  message is harmless.
- Replace `Long.parseLong(null)` with a helper `lastSmsTime(context)` returning `0L` on
  absent/invalid values.

**Files:** `ExternalContentBridge.java`, `SmsDao.java`, `ConfigDao.java`,
`DatabaseContract.java` if an ID watermark column is added.

**Tests (`ExternalContentBridgeTest`, `DaoTest`):**

- two syncs over the same provider fixture insert one row;
- a message dated exactly at the watermark is inserted once;
- missing `lastSmsTime` does not throw;
- a failed insert does not advance the watermark.

**Acceptance:** a 1,000-message fixture produces exactly 1,000 rows after three syncs.

### S-02 — Foreign keys and v4 cleanup

**Problem:** B-03.

**Change:**

- Bump `DATABASE_VERSION` to 4.
- `onUpgrade(old < 4)`: reassign orphan `category_id` to the Unknown category (1); set
  `similar_to = 0` where the referenced row is gone; then create any missing indexes.
- `onOpen`: execute `PRAGMA foreign_keys = ON` (after upgrade, so the cleanup is not
  blocked); verify with `PRAGMA foreign_key_check`.
- `SchemaTest`/`MigrationTest`: inserting a row with a non-existent `category_id` raises
  `SQLiteConstraintException`; a v3 fixture with orphans migrates cleanly.

**Files:** `DatabaseContract.java`, `DatabaseHelper.java`, tests.

**Acceptance:** `PRAGMA foreign_key_check` returns no rows on a migrated fixture.

### S-03 — Backup through the Storage Access Framework

**Problem:** B-04/B-05/B-06/B-07/B-13.

**Change:**

- `DatabaseBackup` exposes stream-based methods:
  `exportTo(Context, OutputStream)` and `importFrom(Context, InputStream)`, keeping the
  current checkpoint/close/`.tmp`/rename sequence internally.
- Settings uses `ActivityResultContracts.CreateDocument` / `OpenDocument` (Java callbacks
  are fine; no new framework).
- Import validation runs on a scratch file **before** the live swap:
  - `SQLite format 3\0` header;
  - open read-only, `PRAGMA integrity_check` returns `ok`;
  - `PRAGMA user_version` is `<= DATABASE_VERSION` (reject a future schema);
  - required tables/columns exist.
- Export shows a one-line warning: "The backup contains every SMS and contact name in
  plain text." Share only after that confirmation.
- Narrow `file_paths.xml` to the backup file name; use a specific MIME type
  (`application/vnd.sqlite3` or `application/octet-stream`) instead of `*/*`.
- Serialize the swap with the same executor/lock that owns all DB access; do not merely
  document "no readers".

**Files:** `db/DatabaseBackup.java`, `SettingsFragment.java`,
`res/xml/file_paths.xml`, `res/values/strings.xml`, tests.

**Tests (`BackupRestoreTest`):**

- round trip through byte streams;
- corrupt header rejected with the live DB unchanged;
- future `user_version` rejected;
- missing table rejected;
- a concurrent read either completes before the swap or fails cleanly, never sees a
  half-installed file.

**Acceptance:** the initial backup file in the old external path is no longer created by
default; a user must choose a destination.

### S-04 — Permissions: SMS required, contacts optional

**Problem:** B-08.

**Change:**

- Request `READ_SMS` as the only hard requirement.
- If `READ_CONTACTS` is denied, show bare numbers and a one-line settings hint; do not
  block ingest or categorization.
- Update `permission_rationale_*` strings and `onRequestPermissionsResult`.
- Add a `SmsListViewModel`/adapter path for an empty contact map; no null keys.

**Tests:** Robolectric activity test with `READ_SMS` granted, `READ_CONTACTS` denied:
list renders, no crash, no contacts query.

**Acceptance:** the app is usable end-to-end with only SMS permission.

### S-05 — Opt-in update check and safe URL handling

**Problem:** B-09/B-10/B-16.

**Change:**

- `key_check_updates` preference, default `false`. No network call until enabled.
- When enabled, use a dedicated `AppExecutors.network()` so a timeout cannot block DB.
- Request with `HEAD`, `setInstanceFollowRedirects(false)`, 5 s timeouts, a `User-Agent`.
- Accept only `https` URLs whose host is `github.com` (or the exact configured host);
  normalize the tag by stripping a leading `v` before comparing.
- If the check is disabled, show "Update check disabled" in the version summary and no
  tap-to-open.
- `disconnect()` the connection in a `finally`.

**Files:** `SettingsViewModel.java`, `SettingsFragment.java`, `preferences.xml`,
`AppExecutors.java`, `strings.xml`, `VersionCheckTest.java`.

**Tests:**

- disabled preference performs zero network calls;
- `v1.7.0` redirect matches `1.7.0`;
- non-HTTPS/non-GitHub redirect is rejected;
- a slow connection does not block a DB task (executor identity test);
- timeout still returns `ERROR`.

**Acceptance:** a default install makes no outbound request.

### S-06 — Manifest hardening

**Problem:** B-12/B-14.

**Change:**

- `android:dataExtractionRules="@xml/data_extraction_rules"` with cloud backup and
  device transfer disabled; `android:fullBackupContent="false"`.
- `android:usesCleartextTraffic="false"`.
- `SmsActivity`: `getWindow().setFlags(FLAG_SECURE, FLAG_SECURE)`.
- Remove the redundant `android:label` from `MainActivity`/`SmsActivity` if lint still
  reports `RedundantLabel`.

**Files:** `AndroidManifest.xml`, `res/xml/data_extraction_rules.xml`, `SmsActivity.java`.

**Tests:** manifest assertion in `SchemaTest`-style Robolectric or a simple `aapt2` check
in CI; manual screenshot attempt on device shows a black frame.

### S-07 — Main-thread database calls

**Problem:** B-11/C-20.

**Change (no new layer):**

- `CategoryListItemHolder` mark-all-read -> `CategoriesViewModel.markAllRead(id)`.
- `SmsListItemHolder` tap-to-read -> `SmsListViewModel.markRead(ids)`; update the row
  from the observed list rather than only changing the card color.
- `ChangeCategoryActivity` load -> a small `CategoriesViewModel` call or a
  `AppExecutors.disk` load with a main-thread submit.
- `MainActivity` add/edit -> `CategoriesViewModel.addCategory/updateCategory` (already
  exists for add; add update).
- Delete `SmsListViewModel.moveToCategory` if it stays unused; otherwise wire it.

**Tests:** `SmsListViewModelTest`/`CategoriesViewModelTest` assert the DAO is invoked on
the disk executor; no direct DAO call remains in an `onClick`.

**Acceptance:** `grep` shows no `SmsDao.`/`CategoryDao.` call directly inside an
`Activity`/`ViewHolder` method that runs on the main thread.

---

## 5. Phase 3 — Correctness and UX (P2)

| ID | Change | Files | Test |
|---|---|---|---|
| C-01 | `Locale.ROOT` in `SmsFilter` | `SmsFilter.java` | Turkish-locale test |
| C-02 | Debounce search (250 ms) and filter on the disk executor | `SmsActivity.java` | 10k-row timing smoke; no main-thread filter |
| C-03 | Guard null dialog args and default to CREATE | `AddCategoryFragment.java`, `MainActivity.java` | Robolectric null-args test |
| C-04 | Pass the category row ID, not the TextView text | `ChangeCategoryActivity.java`, `SmsListItemHolder.java` | result-intent test |
| C-05 | Guard missing/invalid `category_id` | `SmsActivity.java` | launch-without-extra test |
| C-06 | `getIntrinsicHeight()` in swipe drawing | `SwipeUtil.java` | visual check |
| C-07 | Clear pending removal callbacks on detach | `SmsListArrayAdapter.java` | unit test with a fake handler |
| C-08 | Exclude the trained row from its own retrain candidates; deterministic tie-break | `TrainSms.java` | new `CategorizationParityTest` case |
| C-09 | `boolean` returns; delete unused methods | `CategoryDao`, `SmsListViewModel`, `DatabaseHelper` | compile + Error Prone |
| C-10 | Remove 29 unused resources | `res/` | lint `UnusedResources` = 0 |
| C-11 | Extract settings strings; fix "Apllication" | `SettingsFragment`, `preferences.xml`, `strings.xml` | lint `HardcodedText` |
| C-12 | Chunk `IN (...)` at 900 | `SmsDao.java` | 2k-ID test |
| C-13 | Use `_ID = similar_to` for self-trained | `SmsDao.getSelfTrained` | DAO test |
| C-14 | try-with-resources + `getColumnIndexOrThrow` for the inbox cursor | `ExternalContentBridge.java` | provider with missing column -> controlled error |
| C-15 | Global search (highest-value feature) | `MainActivity`, `SmsActivity`/new query | search across categories, 10k rows |
| C-16 | Instant ingest decision | `RECEIVE_SMS` receiver or WorkManager | device test; permission rationale |
| C-17 | Tests for dedupe/import/main-thread | test tree | see S-01/S-03/S-07 |
| C-18 | Error Prone + JaCoCo + CodeQL | build + workflows | first run triaged |

**C-15 design guardrail:** do not add an FTS table for the first pass. Reuse
`SmsDao` with a parameterized `LIKE` query over visible rows, limited to a sensible
result count, and debounce. Add FTS only if the 10k-row benchmark shows it is needed.

**C-16 decision:** default to **no** `RECEIVE_SMS` for 1.7. It adds a restricted
permission and background work for a convenience users can get by opening the app.
If added later: `exported="false"`, `android:permission="android.permission.BROADCAST_SMS"`,
`goAsync()` with a short timeout, query by provider `_id`, then hand off to the same
ingest path. Do not add a notification unless there is a product decision for it.

---

## 6. Phase 4 — Quality, dependencies, docs (P2/P3)

### Q-01 — Static analysis and tests

- Add Error Prone (`net.ltgt.errorprone`) at WARNING, with a curated ERROR list:
  `UnusedVariable`, `UnusedParameter`, `BoxedPrimitiveEquality`, `Finally`,
  `WaitNotInLoop`, `EqualsIncompatibleType`.
- Add JaCoCo report; do not gate on a percentage until a baseline exists.
- Add CodeQL after the first green CI run; do not add a red security check on day one.
- Add `gradle/actions/wrapper-validation` to CI.
- Add `actionlint`/`yamllint` locally or in CI for workflow syntax.
- Add an instrumented matrix entry for API 36.

**Acceptance:** zero ERROR-severity Error Prone findings; lint warnings reduced to the
documented baseline; CI green on API 24 and 36.

### Q-02 — Dependencies

- Remove `commons-collections4` (unused).
- Decide on `simmetrics`: keep if the legacy mode is still supported; otherwise
  replace `Levenshtein` with a small tested implementation and delete the dependency.
- Review the JitPack color picker. If it cannot be fetched reproducibly, replace it with
  a minimal in-app color dialog rather than adding another store dependency.
- Remove `dependencyLocking` until lockfiles are generated; or generate and commit them.
- Update lifecycle/mockito/robolectric/Sonar versions from lint's "newer version"
  notices one at a time, with tests after each.
- Keep AGP/Gradle/target 36 only if the F-Droid buildserver accepts them; otherwise pin
  the last supported pair and document it.

### Q-03 — Docs

- `README`: current SDK, JDK, build, test, privacy, permissions, F-Droid/Izzy badges,
  and the "no Play due to SMS policy" note.
- `SECURITY.md`: reporting, threat model, backup warning, supported versions.
- `docs/RELEASING.md`: tag, signing secrets, reproducibility, store submission,
  rollback.
- Update or delete stale `BASELINE.md`, `docs/LLM_MEMORY.md`, `docs/PRD.md`,
  `docs/TD.md`. Do not keep contradictory release instructions.
- Add `fastlane` changelog for each release; do not invent a changelog entry for a
  version that did not ship.

---

## 7. F-Droid / Izzy submission runbook

### Before the first tag

1. Land P0 + S-01..S-06.
2. Run locally:
   ```bash
   ./gradlew testDebugUnitTest lintDebug
   ./gradlew clean assembleRelease
   # repeat; compare sha256
   ```
3. On a device/emulator (API 24 and API 36): deny contacts, sync, categorize, search,
   export/import, share, rotate, kill process, reopen.
4. Create the `v1.7.0` tag **after** the code is final:
   ```bash
   git tag -a v1.7.0 -m "Grouping Messages 1.7.0"
   git push origin v1.7.0
   ```
5. The release workflow runs tests/lint, builds twice, signs, verifies, publishes APK +
   SHA-256. Confirm the release page has the signed APK for `v1.7.0`.
6. Record the signing certificate SHA-256:
   ```bash
   apksigner verify --print-certs app-release.apk | grep SHA-256
   ```
   Put it in the F-Droid recipe as `AllowedAPKSigningKeys` only after the key is final.

### F-Droid

1. Generate/update `metadata/in.rahulja.groupingmessages.yml` from the released tag.
2. Run `fdroid lint` and fix metadata errors.
3. Open a merge request against `fdroid/fdroiddata` with the recipe and the Fastlane
   tree. Do not claim reproducibility until the buildserver confirms it.
4. After the first build, compare the F-Droid unsigned APK with the developer APK using
   `apksigcopier`/`diffoscope`; if they differ, fix the cause before asking for
   `Binaries:`/developer-signed publishing.
5. Set `AutoUpdateMode: Version v%v` and `UpdateCheckMode: Tags` only after the tag
   convention is stable.

### IzzyOnDroid

1. Confirm the release APK is signed by the developer, not debuggable, and attached to
   the GitHub release.
2. Confirm Fastlane metadata is in the repository and uses current screenshots.
3. Open the inclusion request at the IzzyOnDroid repository with the package name,
   source URL, and release URL.
4. Keep the same `applicationId`; do not rename it for a store.

### After release

- Keep `debug-latest` for debug builds only.
- Never move a release tag.
- If a release is broken, publish a new patch version; do not rewrite the tag.
- Rotate the signing key only with a documented migration; Android will not update
  across keys.

---

## 8. Verification matrix

| Layer | Command | Gate |
|---|---|---|
| Unit | `./gradlew testDebugUnitTest` | 0 failures |
| Lint | `./gradlew lintDebug` | no new warnings; baseline documented |
| Build | `./gradlew assembleDebug assembleRelease` | success |
| Reproducibility | two clean unsigned builds + `cmp` | identical SHA-256 |
| Manifest | `aapt2 dump badging` + `xmltree` | package, code, target, no debuggable/testOnly |
| Signature | `apksigner verify --print-certs` | v2/v3 verified; expected cert |
| Store assets | `store.yml` | metadata, icon, screenshots, recipe |
| F-Droid | `fdroid lint` | no errors |
| Device | `connectedDebugAndroidTest` on API 24/36 | pass |
| Privacy | log grep + manual logcat | no body/address |

---

## 9. Risk register

| Risk | Likelihood | Mitigation |
|---|---|---|
| F-Droid buildserver rejects AGP 9.3.1 / SDK 36 | Medium | Test recipe early; fallback to last supported AGP/SDK with a documented pin. Do not ship an untested recipe. |
| Backup SAF change breaks users' old fixed-path backup | Medium | Keep a one-time "import legacy file" path for one release, or document the migration; never delete the old file automatically. |
| FKs surface existing orphan rows | Medium | v4 cleanup before enabling; test on a copy of a real DB. |
| Making contacts optional changes display names | Low | Expected; show bare numbers and a hint. |
| Removing `latest` debug tag disrupts existing debug users | Low | `debug-latest` with a clear body; debug builds are not store artifacts. |
| Enabling `FLAG_SECURE` blocks user screenshots | Low | Intentional for SMS privacy; document. |
| Error Prone/CodeQL adds CI noise | Medium | Start at WARNING/optional; promote only after triage. |
| Encrypted backup scope creep | Medium | Defer to 1.8; do not block 1.7 on SQLCipher. |

---

## 10. Definition of done for 1.7.0

- [x] P0 items closed and verified by the release workflow definition.
- [x] No SMS body/address in logs (grep + code changes; CI gate added).
- [x] Ingest is idempotent by provider `_ID`; watermark handles missing config.
- [x] Foreign keys enforced; v4 migration tested for v1/v2/v3 shapes.
- [x] Backup uses SAF, validates integrity/schema, warns about plaintext.
- [x] App runs with SMS permission only; contacts optional.
- [x] Update check is opt-in and URL-safe.
- [x] Main-thread DB calls removed from the listed paths.
- [x] F-Droid recipe + Fastlane assets committed; store metadata validation script added.
- [ ] Signed release APK + SHA-256 published from the real `v1.7.0` tag (requires the
      tag and repository secrets; not done locally).
- [x] Reproducible unsigned build confirmed locally on two clean builds.
- [ ] Device smoke on API 24 and 36, including permission denial and import/export.
- [x] README/SECURITY/RELEASING/docs match the shipped behavior.

---

## 11. Local verification record

```text
./gradlew testDebugUnitTest          -> 108 tests, 0 failures, 0 errors
./gradlew lintDebug                  -> BUILD SUCCESSFUL, 7 informational dependency notices
./gradlew clean assembleRelease -PversionCode=10700 -PversionName=1.7.0
  build 1 SHA-256 997e123dd1b97fe32164f96d2389b0ca1c27bea4253cfebe36e8573d1084f073
  build 2                   same bytes (cmp)
aapt2 dump badging                   -> in.rahulja.groupingmessages 10700 / 1.7.0, target 36
aapt2 dump xmltree                   -> allowBackup=false, fullBackupContent=false,
                                        usesCleartextTraffic=false, no debuggable/testOnly
```

Not run: instrumented tests (no device/AVD), F-Droid buildserver, signing with real
secrets. Those are release-time gates, not local passes.

## 12. Remaining P2/P3 work

These are intentionally not in the first store release:

- Global search across categories and debounced filtering (C-15/C-02).
- Instant ingest via `RECEIVE_SMS` or WorkManager (C-16); restricted permission decision.
- Optional encrypted backup (B-13); its own migration project.
- Chunked `IN (...)` mutations for very large selections (C-12).
- Error Prone/CodeQL/JaCoCo and dependency upgrades (Q-01/Q-02).
- Replace the JitPack color picker if it cannot be fetched reproducibly.
- Current store screenshots are from 1.2; replace with 1.7 screenshots on a device.

