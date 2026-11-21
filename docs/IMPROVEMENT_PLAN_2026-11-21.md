# Improvement plan — Grouping Messages

**Date:** 2026-11-21
**Baseline:** v1.7.3 (`versionCode` 10703), database schema v4, commit `96b54b7`
**Status:** Phase 0 plus selected Phase 1/2 slices implemented (docs, changelog,
cursor/chunk, classifier parity, async/global search, confidence/review,
encrypted backup, dependency refresh, batch actions, dark theme, onboarding).
Baseline evidence and line references are tied to commit `96b54b7` before those
edits.

---

## 1. How this plan was produced

1. Read the current production source, manifest, layouts, menus, preferences,
   build files, workflows, tests, and the existing `docs/`, `README.md`,
   `BASELINE.md`, and `improvements.md`.
2. Ran the current verification gates locally:
   - `./gradlew testDebugUnitTest lintDebug`
   - parsed the JUnit XML and lint report
3. Checked live repository state:
   - GitHub Actions runs, tags, releases, release assets
   - F-Droid package page and RFP search
4. Ran a scoped external evidence review (five parallel web scouts plus direct
   official-document checks) for the decisions that can change the roadmap:
   - Android 17 / API 37 behaviour
   - Google Play target-API deadlines
   - `SMS_RECEIVED` feasibility and background limits
   - F-Droid / IzzyOnDroid readiness
   - current dependency versions
5. Benchmarked the two paths the plan proposes to change: in-memory search and
   the classifier.

This is intentionally **not** a 30–100-source deep-research report. It is a
decision-oriented evidence review: every external claim below is tied to a
source, and every code claim is tied to a file and line. Unverified points are
marked as such.

---

## 2. Verified baseline

| Check | Result | Evidence |
|---|---|---|
| Unit tests | **108 tests, 17 suites, 0 failures, 0 errors** | `app/build/test-results/testDebugUnitTest/*.xml` |
| Lint | **0 errors, 7 warnings**, all dependency-version notices | `app/build/reports/lint-results-debug.txt` |
| Debug build | PASS | `./gradlew testDebugUnitTest lintDebug` → `BUILD SUCCESSFUL` |
| CI | Green | latest `CI` run `44798162606`, 2026-11-20 |
| Instrumented tests | Green on API 24 and API 36 | latest `Instrumented Tests` run `44798163091` |
| Release | Green | latest `Release` run `44798180908`, `v1.7.3` |
| Release assets | APK + debug APK + SHA-256 files | `gh release view v1.7.3` |
| Released APK (live check) | Signature verifies (`CN=Rahul`, SHA-256 `6a81…06a`); `versionCode` 10703 / 1.7.3; target 36; no `debuggable`/`testOnly` | `apksigner verify`, `aapt2 dump badging` on the downloaded asset |
| SDK levels | min 24, compile 36, target 36 | `build.gradle:14-16` |
| Build toolchain | AGP 9.3.1, Gradle 9.7.1, JDK 21 build, Java 17 source/target | `gradle/libs.versions.toml`, wrapper, `app/build.gradle` |
| Database | Schema v4, foreign keys enabled in `onOpen` | `DatabaseContract.java:7`, `DatabaseHelper.java` |
| F-Droid listing | **Not listed**: package page returns HTTP 404; a sampled RFP search returned no match | `https://f-droid.org/en/packages/in.rahulja.groupingmessages/` |
| IzzyOnDroid listing | No listing found in sampled checks; not used as evidence for P-14 | old package URL 404; site search |
| Play distribution | Not targeted, by design | `README.md`, `docs/PRD.md` |

The release pipeline is now materially better than the 2026-08 audit baseline:
tag-driven versions, tests and lint before signing, two-build reproducibility
check, signing key under `$RUNNER_TEMP`, APK signature/manifest verification,
and store-metadata validation.

### Implementation progress (2026-11-21)

| Ticket | Status | Notes |
|---|---|---|
| IP-01 docs truth | **Done** | README no longer claims incoming-message sync; ARCHITECTURE now says schema v4 / FK on / disk-thread queries |
| IP-02 changelogs | **Done** | `10701.txt`–`10703.txt` added |
| IP-05 API 37 decision | **Done** | `docs/PLATFORM.md` added; target stays 36 |
| IP-11 global search | **Done** | All-categories mode with the existing message screen; search action on the home screen |
| IP-12 async search | **Done** | 250 ms debounce, disk executor, generation guard |
| IP-13 classifier parity | **Done** | Explicit IDF corpus; retrain now uses the exemplar corpus |
| IP-15 cursor/chunk | **Done** | Try-with-resources inbox cursor; 900-ID chunks |
| IP-21 encrypted backup | **Done** | Optional AES-256-GCM / PBKDF2 container, passphrase prompt on import, plaintext compatibility; crypto round-trip/tamper tests |
| IP-22 batch actions | **Done** | Long-press ActionMode: move to category, mark read, delete with a consequence-confirming dialog; chunked DAO batch delete; Snackbar undo restores the pre-delete snapshot |
| IP-23 dark theme | **Partial** | DayNight theme; row read/unread colours now resolve theme surface tokens; category text colour follows background luminance. TalkBack/contrast audit still pending |
| IP-24 onboarding | **Partial** | First-run explainer plus a recoverable “SMS access required” path with an Open settings action; empty-state polish remains |
| IP-03 store submission | **Blocked** | Needs maintainer Izzy/F-Droid accounts and a buildserver result |
| IP-04 dependency refresh | **Done** | AGP 9.5.2, Gradle 9.9.2, Lifecycle 2.11.1, Mockito 5.24.0, Robolectric 4.18.1, SonarQube 7.6.0.18749; `resConfigs` migrated to `androidResources.localeFilters`; lint now clean |
| IP-06 archive backlog | **Blocked** | Needs confirmation before moving/deleting untracked `improvements.md` |
| IP-10 instant ingest | **Declined** | Decision 2026-11-21: keep on-open sync; no `RECEIVE_SMS` |
| IP-14 confidence/review | **Done (MVP)** | Score exposed in `Sms` and shown per row; “Review unclassified” lists Unknown rows with best-category suggestions; tapping a row accepts the suggestion or opens the picker when none exists |

Current local gate after these edits: **120 tests, 18 suites, 0 failures; lint
0 errors, 0 warnings**. No instrumented run was done in this session. Dark-mode
screenshots, TalkBack checks, and the first-run flow still need a device.

---

## 3. Feature inventory — what the app does today

| Area | Current behaviour | Main code |
|---|---|---|
| Inbox ingest | Reads inbox rows newer than `lastSmsTime` when `MainActivity` resumes; deduplicates by provider `_ID` | `MainActivity.java:170` → `CategoriesViewModel.java:64` → `ExternalContentBridge.java:28-110` → `SmsDao.storeTrainedInboxSms` |
| Classification | 1-nearest-exemplar over the user's trained exemplars; word TF-IDF cosine + character-trigram Dice; four modes; configurable threshold | `classify/SmsCategorizer.java`, `classify/TextVectorizer.java`, `classify/CharTrigramProfile.java` |
| Training | Moving a message to a category makes it an exemplar (`similar_to = _id`, score 1.0), then re-scores existing similar messages | `SmsActivity.java:280-360` → `TrainSms.retrainExistingSms` → `SmsDao.storeReTrainedSms` |
| Categories | Create, rename/recolour, soft-delete; built-in Unknown (id 1); per-category unread/read counts; mark-all-read; delete-all-messages-in-category | `MainActivity`, `AddCategoryFragment`, `CategoryListItemHolder`, `CategoryDao`, `SmsDao` |
| Message list | Per-category list, newest first; tap marks read; long-press absent; swipe-left delete with undo; “Change Category” picker | `SmsActivity`, `SmsListArrayAdapter`, `SmsListItemHolder`, `SwipeUtil`, `ChangeCategoryActivity` |
| Search | Text/sender/contact-name search **inside one category only**; filters the already-loaded list on the UI thread per keystroke | `SmsActivity.java:130-151`, `SmsFilter.java` |
| Read state | `read` and `seen` columns; counts computed per category; no explicit “mark unread” | `SmsDao.setSmsAsRead`, `CategoriesViewModel` |
| Backup / restore | User-picked SAF file; plaintext SQLite; warning dialog; header, integrity, schema and table validation on import; atomic replace | `SettingsFragment.java:163-260`, `DatabaseBackup.java` |
| Settings | Similarity mode, threshold, update-check opt-in, reset model, reset model + delete categories, export/import, developer/version links | `preferences.xml`, `SettingsFragment.java`, `SettingsViewModel.java` |
| Update check | Opt-in HTTPS HEAD to the GitHub `releases/latest` redirect; validates repository URL; normalizes `v` prefix | `SettingsViewModel.java` |
| Privacy boundary | No analytics, ads, accounts, cloud sync, WebView, dynamic code; SMS content never intentionally logged; `FLAG_SECURE` on the message screen; OS backup disabled | `AndroidManifest.xml`, `SmsActivity.java:58-62` |
| Permissions | `READ_SMS` required; `READ_CONTACTS` requested but optional; `INTERNET` only for the opt-in update check | `AndroidManifest.xml:7-9`, `MainActivity.java:176-233` |
| Distribution | GitHub releases; F-Droid recipe and Fastlane metadata committed; store validation workflow; no active store listing yet | `metadata/`, `fastlane/`, `.github/workflows/store.yml` |

### Product boundaries that should stay

- Read-only with respect to the system SMS database: no sending, modifying,
  blocking, MMS, or RCS.
- No default-SMS-handler role.
- No Google Play target; Play’s SMS-permission policy would require a role or
  exception this product does not have.
- No cloud, accounts, analytics, crash reporting, ads, or dynamic code loading.
- No Compose/Room/Hilt rewrite for its own sake. The current Java/XML layering
  is small, testable, and already passes 108 tests.

---

## 4. What is genuinely strong and must be preserved

1. **The privacy boundary is real.** The only network call is opt-in, and SMS
   text never leaves the device.
2. **The classifier is deterministic and unit-testable.** It has no Android
   dependency and has parity/regression tests.
3. **The data layer is now sane.** Provider-ID deduplication, foreign keys,
   v4 migration, WAL-safe export/import, and integrity validation.
4. **Release engineering is no longer the weak point.** Tag-driven versions,
   reproducible unsigned builds, signature verification, SHA-256 publication.
5. **The app is small.** 33 production Java files and one module; changes remain
   reviewable.

---

## 5. Findings, ranked

Severity: **P0** blocks the next release promise · **P1** high user value or
correctness · **P2** material quality · **P3** polish.

| ID | Severity | Finding | Evidence | Impact |
|---|---|---|---|---|
| P-01 | **P2 (accepted)** | Messages are categorized only when the app opens. There is no `BroadcastReceiver`, `Service`, or scheduler. | `MainActivity.java:170`; `CategoriesViewModel.java:64`; no `RECEIVE_SMS` or receiver in `AndroidManifest.xml` | A message received while the app is closed sits uncategorized until the user opens the app. Decision 2026-11-21: keep on-open sync; the README now says so. |
| P-02 | **P0** | No global search. Search exists only inside an already-open category. | `res/menu/menu_main.xml` has only Settings; `res/menu/menu_sms.xml` has search; `SmsActivity.java:211-245` | Users must guess the category before searching. Highest-value missing utility. |
| P-03 | **P1** | In-category search filters the full list synchronously on the UI thread on every keystroke; no debounce or stale-result guard. | `SmsActivity.java:130-151`; `SmsFilter.java:17-58` | Jank on large inboxes; worst on slow devices. Desktop benchmark: 10k rows × 20 queries = 65 ms, ~3.25 ms/query; Android + DiffUtil is worse. |
| P-04 | **P1** | First-pass classification and retraining score the same pair against different IDF corpora, so the same pair can cross the threshold differently. | `TrainSms.java:60` builds a batch over exemplars; `TrainSms.java:221-223` builds a batch over **all** stored SMS | Corrections can propagate inconsistently; “similar” means different things in the two paths. |
| P-05 | **P1 (fixed)** | Classification confidence was stored (`similarity_score`, Java constant `KEY_SIM_SCORE`) but invisible; Unknown was a dead end with no suggested category. | Score now exposed by `Sms.fromCursor`, shown in `SmsListItemHolder`, and the Review-unclassified screen suggests a best category | Users can judge auto-categorisation and accept a suggestion in one tap. |
| P-06 | **P1** | `sender_type` is computed and stored but no rule path reads it; there are no sender rules. | `CategoriesViewModel.addSenderTypeToListOfSms`; the only consumers are the schema and the ingest classifier | Bank/OTP/transaction senders that are obvious from the address still depend on text similarity. Deterministic rules would improve accuracy and explainability. |
| P-07 | **P1 (fixed)** | Backups were plaintext SQLite only. | `DatabaseBackup.java` now supports an optional AES-256-GCM container via `BackupCrypto`; plaintext import remains for compatibility | Anyone who obtains a plaintext export still gets every stored message and sender address, but users can now set a passphrase. Contact names are resolved at runtime and are not stored in the database. |
| P-08 | **P2 (mostly fixed)** | Light-only theme and hardcoded colours; category colours are used as backgrounds without contrast handling. | `styles.xml` now uses DayNight; row colours resolve Material surface tokens; category text colour follows background luminance | Dark mode is implemented; a device visual/TalkBack pass remains. |
| P-09 | **P2** | Large `IN (...)` mutations are not chunked. | `SmsDao.updateIds` (`SmsDao.java:402-425`) builds one placeholder per ID | Selections above SQLite’s bind-variable limit fail. Blocks safe batch actions. |
| P-10 | **P2** | Inbox cursor is closed manually, not with try-with-resources. | `ExternalContentBridge.java:48-100` | A cursor leak if an exception occurs between query and close. |
| P-11 | **P2** | Documentation drift. | `README.md:24`; `docs/ARCHITECTURE.md:48` (claims remaining UI-thread DB queries), `:117` (“Schema v3”), `:131` (“foreign keys OFF”) | Reviewers and future agents get a false model of the app. |
| P-12 | **P2** | Fastlane changelog only covers 1.7.0; 1.7.1–1.7.3 have no changelog entries. | `fastlane/metadata/android/en-US/changelogs/10700.txt` is the only file | Store/update surfaces can show stale or missing release notes. |
| P-13 | **P3 (fixed)** | Dependency lag. | Upgraded to AGP 9.5.2, Gradle 9.9.2, Lifecycle 2.11.1, Mockito 5.24.0, Robolectric 4.18.1, SonarQube 7.6.0.18749; `resConfigs` migrated to `androidResources.localeFilters` | Local unit, lint, and debug builds are green; CI and instrumented runs still need a push. |
| P-14 | **P1** | No active store listing. | F-Droid package page 404; sampled RFP search found no request | The app is only reachable through GitHub releases, which limits the audience for a privacy-first utility. |
| P-15 | **P1** | Android 17 changes OTP SMS availability for non-default SMS apps. | See §6.1 | At `targetSdk 37`, standard OTP messages are withheld for three hours; at `targetSdk 36`, standard OTP is unaffected but WebOTP-format messages are filtered for all apps. This constrains the “instant” promise and must be documented. |
| P-16 | **P2** | Category deletion has no UI path to undo, restore, or merge. | `CategoriesViewModel.deleteCategory` → `CategoryDao.deleteCategory` sets `visibility=0` and `SmsDao.deleteModelForCategory` moves messages to Unknown and clears their training | An accidental long-press delete loses the category and its training; the only recovery is manual re-creation and re-training. |

---

## 6. External constraints that shape the plan

### 6.1 Android 17 / API 37 and OTP messages

Two separate official statements matter:

1. **WebOTP format, all apps.** Beginning with Android 17, if an app has
   permission to read SMS but is not the intended recipient of a WebOTP message
   (domain verification), the message is not accessible for three hours. During
   the delay, `SMS_RECEIVED_ACTION` is withheld and SMS provider queries are
   filtered. This applies to **all apps, regardless of target API level**.
   Source: `https://developer.android.com/about/versions/17/behavior-changes-all`
2. **Standard SMS OTP, apps targeting 37+.** Android is extending the same
   three-hour protection to standard SMS messages containing an OTP, for most
   apps **targeting Android 17 (API level 37) or higher**. Default SMS handlers,
   connected-device companions, and similar apps are exempt; this app is not.
   Source: `https://developer.android.com/about/versions/17/behavior-changes-17`

**Planning consequence:** stay at `targetSdk 36` while that is acceptable.
Do not bump to 37 without documenting that OTP messages may arrive up to three
hours late. The app is a categorizer, not an OTP consumer, so SMS Retriever /
SMS User Consent are not drop-in fixes. A future migration to 37 needs a product
notice and tests, not a silent version bump.

### 6.2 Google Play target API

- From **31 August 2026**, new apps and updates must target **API 36**.
  Source: `https://developer.android.com/google/play/requirements/target-sdk`
- A 2027 deadline for API 37 was **not published on that page as of
  2026-11-21**. Do not invent a date. Re-check before planning the 37 migration.
- This app does not target Play because of the SMS-permission policy, but the
  deadline still sets the ecosystem baseline for F-Droid/Izzy builds.

### 6.3 Instant ingest is still technically feasible

- `SMS_RECEIVED` is on the official exempted list for implicit-broadcast
  restrictions, so a manifest receiver still receives it on modern Android.
  Source: `https://developer.android.com/develop/background-work/background-tasks/broadcasts/broadcast-exceptions`
- A broadcast receiver has roughly 10 seconds before the system considers it
  unresponsive; `goAsync()` extends the work and must be finished explicitly.
  Source: `https://developer.android.com/reference/android/content/BroadcastReceiver#goAsync()`
- Android 16 changed JobScheduler/WorkManager quotas. A WorkManager-based
  fallback must be tested against those quotas, not assumed.
  Source: `https://developer.android.com/about/versions/16/behavior-changes-all`
- The classifier benchmark is fast enough for a single incoming message:
  100 queries against 100 exemplars = 71 ms; against 500 exemplars = 94 ms
  (desktop JVM, this repository’s compiled classes). A receiver would still do
  only classification + insert, not full retraining.
- **Decision 2026-11-21:** the receiver is not being implemented. Keep the
  on-open sync and keep the README honest. See IP-10.

### 6.4 F-Droid and IzzyOnDroid

- **F-Droid:** the package page returns 404 and a sampled RFP search returned
  no request. The app is not listed. F-Droid builds from source and requires a
  FLOSS licence, a build recipe, and a successful buildserver build.
  Sources: `https://f-droid.org/en/packages/in.rahulja.groupingmessages/`,
  `https://f-droid.org/docs/Inclusion_Policy/`
- **F-Droid buildserver risk:** there is public evidence that the buildserver
  lags modern AGP versions (community issue: AGP 8.12.0 did not work;
  `https://github.com/CompassMB/MBCompass/issues/88`). The project uses AGP
  9.3.1 / compileSdk 36. This is **unverified for this app**; test the recipe
  before promising F-Droid availability. Do not pin an old AGP blindly.
- **IzzyOnDroid:** accepts developer-signed release APKs attached to the
  project, requires Fastlane (or equivalent) metadata, and scans the APK.
  Sources: `https://izzyondroid.org/docs/general/AppInclusionPolicy/`,
  `https://izzyondroid.org/contributing/NewAppInclusions/`
  This is the fastest distribution win because the release APK already exists.

### 6.5 Dependency baseline

Current stable versions verified on 2026-11-21:

| Component | Project | Latest verified | Notes |
|---|---|---|---|
| AGP | **9.5.2** | 9.5.2 | AGP 9.5 supports max API 37; minimum Gradle 9.6.0 |
| Gradle | **9.9.2** | 9.9.2 | local gate green |
| AppCompat | 1.8.0 | 1.8.0 | current |
| Material Components | 1.14.0 | 1.14.0 | current |
| RecyclerView | 1.4.0 | 1.4.0 | current |
| Lifecycle | **2.11.1** | 2.11.1 | current |
| Preference | 1.2.1 | 1.2.1 | current |
| Mockito | **5.24.0** | 5.24.0 | current |
| Robolectric | **4.18.1** | 4.18.1 | current |
| SonarQube plugin | **7.6.0.18749** | 7.6.0.18749 | current |

Sources: AGP 9.5 release notes, `services.gradle.org/versions/current`, Google
Maven metadata, Maven Central metadata.

### 6.6 Feature landscape (sampled, not exhaustive)

- QKSMS is abandoned: F-Droid package page 404; the upstream issue tracker has
  an “abandoned?” report.
- Fossify Messages is the maintained Simple SMS successor; version 1.10.2 was
  added to F-Droid on 2026-10-27.
- SMS Import/Export remains active; version 2.13.0 was added on 2026-11-17.
- Recurring complaint clusters in those projects: performance at scale, backup
  reliability, and spam/blocking demand.

**Product reading:** there is no evidence in this sample of an active,
privacy-first **SMS categorizer** with on-device training. That is the
differentiator to deepen. Do not chase MMS/RCS/default-handler scope.

---

## 7. Strategy

**Thesis:** the app already has the hard part — a private, deterministic,
on-device classifier. The next gains come from making that promise true all the
time, making it trustworthy and controllable, and making the app reachable.

Three rules for the work:

1. **Do not rewrite the stack.** Extend the current Activity → ViewModel → DAO
   layering. Compose/Room/Hilt are not justified by the current size or tests.
2. **Prefer features that compound the classifier.** Instant ingest, confidence,
   review, and sender rules all improve the training loop. Notifications,
   widgets, and stats do not.
3. **Every claim gets a test.** A feature without a test or a documented manual
   device check is not done. The release gate stays: unit tests + lint +
   instrumented matrix + two-build reproducibility.

### Relation to the 2026-08 backlog

`improvements.md` remains useful as history, but it is not the current plan.
The live items map as follows:

| Old ticket | This plan |
|---|---|
| GM-047 background classification via `SMS_RECEIVED` | IP-10 (declined 2026-11-21) |
| GM-048 per-category notifications | IP-30 (deferred) |
| GM-049 global cross-category search | IP-11 |
| GM-050 surface classification confidence | IP-14 |
| GM-051 backup automation / readable export | IP-21 (encryption first; automation later) |
| GM-052 onboarding and training kick-start | IP-24 |
| GM-054 multi-select batch actions | IP-22 |
| GM-058 sender-based rules engine | IP-20 |
| GM-029 DayNight foundation | IP-23 |
| GM-016 main-thread database calls | Fixed; keep the disk-executor rule |
| GM-017/018 typed DAO and repository | Not done; deliberately deferred, not a 1.8 blocker |

Everything in the old backlog that is already done should be marked done or
archived, not carried forward as if open.

---

## 8. Roadmap

| Release | Theme | Headline work | Exit criteria |
|---|---|---|---|
| **1.7.4** | Truth and reach | Fix docs/changelog, dependency refresh, submit to IzzyOnDroid, open F-Droid RFP, record the API 37 decision | No false feature claims; store submissions live; CI green after dependency bumps |
| **1.8.0** | Trustworthy search and confidence | Global search, async search, classifier parity, confidence/review; keep on-open ingest | Search spans all categories; no UI-thread filtering; score visible; README truthful about when sync runs |
| **1.9.0** | Control and privacy | Sender rules, encrypted backup, batch actions, dark theme, onboarding/permission recovery | Rules beat classifier deterministically; encrypted round-trip; multi-select with undo; dark mode + contrast checked |
| **2.0** | Reach and scale | Notifications (opt-in), widget, stats, localization, FTS/Room spike, API 37 migration when required | Each feature is separately gated; no scope creep into MMS/RCS/default handler |

Estimated effort: 1.7.4 ≈ 1 week; 1.8.0 ≈ 2–4 weeks; 1.9.0 ≈ 4–6 weeks.
The estimates assume one developer and the existing test discipline.

---

## 9. Tickets

### Phase 0 — 1.7.4 Truth and reach

#### IP-01 · Make the documentation true again

| | |
|---|---|
| Type / Priority | Task / **P0** |
| Estimate | S |
| Depends on | Nothing |

**Problem.** `README.md:24` says “Automatic categorization of incoming SMS”,
but there is no receiver. `docs/ARCHITECTURE.md:48` still claims some DB queries
run on the UI thread, `:117` says schema v3, and `:131` says foreign keys are
off. The code is better than the document.

**Change.**
1. Reword the README bullet to “Categorizes SMS from your inbox when the app
   opens”. This is the shipped behaviour; IP-10 is declined, so do not change it
   back unless the product decision changes.
2. Update `docs/ARCHITECTURE.md` to schema v4, foreign keys enabled, and the
   actual threading model.
3. Add a short “last verified against version” line to `ARCHITECTURE.md` so the
   next drift is obvious.

**Files.** `README.md`, `docs/ARCHITECTURE.md`.

**Acceptance.**
- Every feature bullet can be demonstrated by current code.
- Schema version, FK state, and threading statements match `DatabaseHelper`
  and the current adapters/holders.
- `grep` for “Schema v3” and “foreign-key enforcement is OFF” returns nothing.

**Verification.** `grep -n` the claims against `DatabaseContract.java:7`,
`DatabaseHelper.onOpen`, and the list of `AppExecutors.disk` call sites.

---

#### IP-02 · Complete the changelog and store metadata

| | |
|---|---|
| Type / Priority | Task / **P0** |
| Estimate | S |
| Depends on | Nothing |

**Problem.** `fastlane/metadata/android/en-US/changelogs/` contains only
`10700.txt`. Releases 1.7.1–1.7.3 have no user-facing notes, while
`metadata/in.rahulja.groupingmessages.yml` already points at 1.7.3.

**Change.** Add `10701.txt`, `10702.txt`, `10703.txt` with the actual fixes
(search/filtering, SMS hardening, release-pipeline fixes). Keep entries short
and user-facing, not commit messages.

**Files.** `fastlane/metadata/android/en-US/changelogs/`.

**Acceptance.**
- One changelog per released version code from 10700 to 10703.
- `store.yml` metadata validation passes.
- The latest changelog does not claim a feature that is not shipped.
- Fastlane screenshots and the 512 px icon still match the current UI; replace
  them if the review screen or dark mode changes the visible surface.

**Verification.** Run the store workflow’s local validation command (or the
`store.yml` job via `workflow_dispatch`) and inspect the Fastlane directory.

---

#### IP-03 · Submit to IzzyOnDroid and start the F-Droid process

| | |
|---|---|
| Type / Priority | Task / **P0** |
| Estimate | M |
| Depends on | IP-02; signed release asset |

**Problem.** The app is not listed on either store. The signed APK and Fastlane
metadata already exist, so the blocker is process, not code.

**Change.**
1. **IzzyOnDroid:** open a new-app inclusion request with the package name,
   repository, release asset URL, and the Fastlane metadata. Confirm that the
   APK is developer-signed, not debuggable/testOnly, and that the release tag
   matches `versionName`/`versionCode`.
2. **F-Droid:** open an RFP issue and prepare an `fdroiddata` merge request
   using the existing `metadata/in.rahulja.groupingmessages.yml`.
3. Test the recipe against the current F-Droid buildserver. If AGP 9.3.1 or
   SDK 36 is rejected, record the exact error and decide between waiting,
   pinning the newest supported AGP, or distributing only via Izzy + GitHub.
   Do not silently downgrade the app just to satisfy a stale builder.

**Files.** `metadata/`, `fastlane/`, possibly a `docs/DISTRIBUTION.md` runbook.

**Acceptance.**
- IzzyOnDroid request URL recorded.
- F-Droid RFP URL recorded.
- A buildserver result is recorded: pass, or a specific blocker with a chosen
  fallback.
- No store submission claims “available” before the listing is live.

**Verification.** Live package-page checks, RFP/MR links, and `fdroid lint`
output where available.

---

#### IP-04 · Dependency refresh

| | |
|---|---|
| Type / Priority | Task / **P1** |
| Estimate | S–M |
| Depends on | Nothing; do after IP-01/02 so CI failures are isolated |

**Problem.** AGP, Gradle, Lifecycle, Mockito, and Robolectric are behind
current stable. Lint’s only warnings are dependency notices.

**Change.**
1. Bump AGP 9.3.1 → 9.5.2 first; keep Gradle 9.7.1 for the first run.
2. After AGP is green, test Gradle 9.7.1 → 9.9.2. If AGP/Gradle compatibility
   is unverified, stay on 9.7.1 and record why.
3. Bump Lifecycle 2.11.0 → 2.11.1, Mockito 5.18.0 → 5.24.0, Robolectric
   4.16.1 → 4.18.1, and the SonarQube plugin 7.4.0.8496 → 7.6.0.18749.
4. Do not bump compileSdk/targetSdk as part of this ticket.

**Files.** `gradle/libs.versions.toml`, `gradle/wrapper/gradle-wrapper.properties`.

**Acceptance.**
- `./gradlew testDebugUnitTest lintDebug assembleDebug` passes.
- Lint has zero dependency-version warnings.
- Instrumented matrix API 24/36 is green.
- `docs/LLM_MEMORY.md` and `README.md` version claims still match.

**Verification.** Full local gate + CI/instrumented runs.

---

#### IP-05 · Record the API 37 decision before it becomes urgent

| | |
|---|---|
| Type / Priority | Task / **P1** |
| Estimate | S |
| Depends on | Nothing |

**Problem.** Android 17 (API 37) is shipped. Targeting 37 triggers the
three-hour OTP withholding for this non-default SMS app. The Play 2027 deadline
is not yet published.

**Change.** Add `docs/PLATFORM.md` with:
- current target 36 and why;
- the exact Android 17 OTP rules and the app’s non-exempt status;
- a checklist for an API 37 smoke test (edge-to-edge, predictive back,
  large-screen resizability, notification permission if notifications ship);
- the trigger for revisiting: Google publishes the 2027 deadline, or a
  dependency forces API 37.

**Files.** `docs/PLATFORM.md` (new), `README.md` link.

**Acceptance.**
- A reader can explain why the app is not on 37 yet.
- The document does not claim OTP messages are instant on 37.
- API 37 emulator smoke results are recorded when run.

**Verification.** Doc review + `sdkmanager`/emulator run; no target bump.

---

#### IP-06 · Retire the stale 2026-08 backlog safely

| | |
|---|---|
| Type / Priority | Chore / **P3** |
| Estimate | S |
| Depends on | User confirmation |

**Problem.** `improvements.md` is an untracked 86 KB, 59-ticket backlog from
2026-08-26. Some items are done; some are stale; it competes with this plan as a
source of truth.

**Change.** Either leave it untouched and mark it “historical” in this plan, or
move it to `docs/archive/improvements-2026-08.md` with a status header. Do not
delete user work without explicit confirmation.

**Acceptance.** One current plan (this document) and, at most, one clearly
labelled historical backlog.

---

### Phase 1 — 1.8.0 Trustworthy search and confidence

#### IP-10 · Keep on-open ingest (declined receiver)

| | |
|---|---|
| Type / Priority | Decision / **Accepted** |
| Estimate | None |
| Depends on | Nothing |

**Decision (2026-11-21).** Keep the existing on-open `syncLatestSms` path. Do
not add `RECEIVE_SMS` or a manifest receiver. The README must say when sync
runs, and the product must not promise instant categorization.

**Why.** The receiver is technically feasible, but it adds a second dangerous
permission and a background execution surface for a categorizer. The product
owner chose the smaller permission surface. This is an accepted limitation, not
an open bug.

**Consequences.**
- Messages received while the app is closed are categorized at the next app
  open/resume.
- Android 17 OTP delays remain a separate constraint (see `docs/PLATFORM.md`).
- Do not reopen this ticket without an explicit product decision; if revisited,
  the original receiver design and tests are still valid (manifest receiver,
  `BROADCAST_SMS` guard, `goAsync()`, multipart join, provider-ID dedupe).

**Acceptance.**
- No `RECEIVE_SMS` permission and no receiver in the manifest.
- README and store text say “when you open the app”.
- On-open sync still deduplicates by provider `_ID` and does not block the UI.

**Verification.** Existing ingest tests plus a manual fresh-install/open check.

---

#### IP-11 · Global search across categories

| | |
|---|---|
| Type / Priority | Story / **P0** |
| Estimate | M |
| Depends on | IP-12 (shared async filter path) |

**Problem.** P-02: search is category-scoped.

**Change.**
1. Add a search action to `menu_main.xml` (and a visible entry point on the
   category screen).
2. Load all visible SMS once on the disk executor, then filter with
   `SmsFilter`; include category name in each result row.
3. Tapping a result opens the message’s category (and ideally scrolls to the
   hit).
4. Empty state: “No messages matching …” with a clear action.
5. Do not add FTS or a schema migration for this version; measure first.

**Files.** `MainActivity`, `res/menu/menu_main.xml`, new search
activity/fragment or a `SmsActivity` all-categories mode, `SmsFilter`,
`SmsListArrayAdapter`, `strings.xml`, tests.

**Acceptance.**
- Search is reachable from the home screen.
- Results span all visible categories and show the category name.
- Tapping a result lands on the right category.
- The search query does not scan on the UI thread.
- Search still works when `READ_CONTACTS` is denied (body/address only).
- 10k-message benchmark stays within the agreed budget.

**Verification.** New DAO/filter tests, an Espresso smoke test if the UI path
is stable, and the 10k benchmark.

---

#### IP-12 · Debounced, off-main-thread filtering

| | |
|---|---|
| Type / Priority | Task / **P0** |
| Estimate | M |
| Depends on | Nothing |

**Problem.** P-03: every keystroke runs `SmsFilter.filter` on the UI thread.

**Change.**
1. Debounce input by ~250 ms.
2. Run the filter on `AppExecutors.disk()`.
3. Keep a generation counter so a stale result cannot overwrite a newer query.
4. Apply results on the main thread with the existing `submitSms` path.

**Files.** `SmsActivity`, `SmsListViewModel` or a small search controller,
`SmsFilter`, tests.

**Acceptance.**
- Typing fast produces no UI-thread full-list scans.
- The final result matches the last query.
- Rotation/back does not leave a stale result.
- 10k-message regression test documents the budget.

**Verification.** Unit test for generation/stale-result logic; 10k benchmark;
manual typing on API 24 and 36.

---

#### IP-13 · Classifier parity between first pass and retraining

| | |
|---|---|
| Type / Priority | Bug / **P1** |
| Estimate | M |
| Depends on | Nothing |

**Problem.** P-04: `getTrainedListOfSms` scores against the exemplar corpus;
`retrainExistingSms` scores against all stored SMS. IDF is corpus-dependent, so
the same pair can score differently.

**Change.**
1. Extract a shared scoring context that both paths use.
2. Either score retraining against the exemplar corpus, or use a
   corpus-independent similarity for retraining and document why.
3. Add a regression test that scores a fixed pair through both paths and
   asserts equality within a small epsilon.

**Files.** `TrainSms.java`, `classify/SmsCategorizer.java`,
`app/src/test/.../CategorizationParityTest.java`.

**Acceptance.**
- The same pair gets the same score in both paths.
- Threshold tests cover below/at/above the boundary.
- Existing parity tests remain green.

**Verification.** Unit tests; no device needed.

---

#### IP-14 · Confidence display and a review queue

| | |
|---|---|
| Type / Priority | Story / **P1** |
| Estimate | M |
| Depends on | IP-13 |

**Problem.** P-05: the stored score (`similarity_score` / `KEY_SIM_SCORE`)
exists but is invisible; Unknown has no suggestions.

**Change.**
1. Add `simScore` to `Sms` (the DB projection already carries it).
2. Show a small label on each row: “Auto 62%” or “Unclassified”.
3. Add a “Review” filter for Unknown/low-confidence messages.
4. For Unknown, compute the best suggested category on demand with the
   existing `SmsCategorizer.Batch`; show Accept / Choose category.
5. Do not add new columns for this version; compute suggestions on demand and
   cap the queue (for example, first 200 rows) with a progress indicator.

**Files.** `model/Sms.java`, `SmsDao`, `SmsListArrayAdapter`,
`SmsListItemHolder`, `SmsActivity`, new review screen/fragment, `strings.xml`,
tests.

**Acceptance.**
- Every auto-categorized row shows a score.
- Unknown rows can show a suggested category with a confidence.
- Accepting a suggestion trains the message exactly like the existing picker.
- The UI remains usable with a large Unknown category (bounded work).

**Verification.** Unit tests for suggestion selection and threshold display;
Robolectric/Espresso for the review action; manual check with a seeded DB.

---

#### IP-15 · Close the cursor and chunk large mutations

| | |
|---|---|
| Type / Priority | Bug / **P2** |
| Estimate | S |
| Depends on | Nothing |

**Problem.** P-09 and P-10: one unclosed-cursor path; unbounded `IN (...)`
lists.

**Change.**
1. Wrap `ExternalContentBridge.getLatestSmsFromInbox`’s cursor in
   try-with-resources (keep the existing null handling).
2. Chunk `updateIds` at 900 IDs per statement, preserving parameterized args.
3. Add a test with 1,000+ IDs and a cursor-exception test if practical.

**Files.** `ExternalContentBridge.java`, `SmsDao.java`, tests.

**Acceptance.**
- No manual `cursor.close()` remains in the inbox query.
- A 1,000-ID update succeeds and returns the correct count.
- Existing DAO tests stay green.

**Verification.** Unit/Robolectric tests; lint.

---

### Phase 2 — 1.9.0 Control and privacy

#### IP-20 · Sender rules (“always use this category for this sender”)

| | |
|---|---|
| Type / Priority | Story / **P1** |
| Estimate | L |
| Depends on | Schema v5 migration; IP-13 |

**Problem.** P-06: obvious sender→category mappings still depend on text
similarity.

**Change.**
1. Add `sender_rule` (`_id`, normalized `address`, `category_id`, timestamps)
   with a unique address and a category FK.
2. Normalize addresses with `PhoneNumberUtils.normalizeNumber` and keep the
   original for display.
3. Check rules before the classifier; a rule wins deterministically.
4. UI: a checkbox in the category picker (“Always use this category for this
   sender”) plus a list to review/delete rules in Settings.
5. On category deletion, clear or reassign its rules; on import/restore, keep
   them consistent with the DB validation.

**Files.** `DatabaseContract`, `DatabaseHelper` (v5 migration), new DAO,
`TrainSms`/ingest path, `ChangeCategoryActivity`, Settings, tests.

**Acceptance.**
- A rule beats the classifier for the same sender.
- Address normalization handles spaces, `+91`, and punctuation consistently.
- Deleting a category does not leave orphan rules.
- Backup/restore preserves rules and rejects a DB with invalid rule FKs.

**Verification.** Migration tests v4→v5, precedence tests, backup round-trip.

---

#### IP-21 · Optional encrypted backup

| | |
|---|---|
| Type / Priority | Story / **P1** |
| Estimate | M |
| Depends on | Nothing; coordinate with IP-20's schema |

**Problem.** P-07: backups are plaintext SQLite.

**Change.**
1. Define a versioned container: magic/version header, PBKDF2-HMAC-SHA256 salt
   and iteration count, AES-256-GCM IV, ciphertext, auth tag.
2. Export: read the DB bytes and encrypt when the user opts in.
3. Import: detect the header; if encrypted, prompt for the passphrase and
   decrypt to a scratch file before the existing validation.
4. Keep plaintext import/export working for compatibility; warn clearly.
5. Do not encrypt the live database; that is a separate migration project.

**Files.** `DatabaseBackup`, `SettingsFragment`, `strings.xml`, new crypto
helper, tests.

**Acceptance.**
- Encrypted export/import round-trips on a seeded DB.
- Wrong passphrase, truncated file, and tampered ciphertext fail closed.
- Legacy plaintext backup still imports.
- No passphrase or key is logged or written outside the chosen SAF stream.

**Verification.** JVM crypto tests with fixed vectors, Robolectric import/export
tests, manual SAF flow on device.

---

#### IP-22 · Batch multi-select

| | |
|---|---|
| Type / Priority | Story / **P1** |
| Estimate | M |
| Depends on | IP-15 (chunked mutations) |

**Problem.** No way to move/delete/mark-read several messages at once.

**Change.**
1. Long-press enters a selection `ActionMode` in `SmsActivity`.
2. Actions: move to category, mark read, delete with a dialog that explains
   the self-trained-row hiding rule. A Snackbar offers undo from a pre-delete
   snapshot.
3. Reuse the existing picker and DAO operations; chunk large selections.
4. Keep the current swipe-delete behaviour for single rows.

**Files.** `SmsActivity`, `SmsListArrayAdapter`, `SmsListViewModel`,
`ChangeCategoryActivity`, `strings.xml`, tests.

**Acceptance.**
- Selection is deliberately cleared on rotation/query change (MVP trade-off).
- Move/delete/mark-read update counts and lists correctly.
- 1,000-item selection does not crash.
- Batch delete hides self-trained rows and removes untrained rows atomically.
- Undo restores both hidden trained rows and removed untrained rows.
- The confirmation dialog names the consequence.

**Verification.** Unit tests for selection state and DAO counts; Espresso smoke;
manual large-selection check.

---

#### IP-23 · Dark theme and accessibility pass

| | |
|---|---|
| Type / Priority | Task / **P2** |
| Estimate | M |
| Depends on | Nothing |

**Problem.** P-08: light-only theme and hardcoded colours.

**Change.**
1. Switch to `Theme.MaterialComponents.DayNight...`.
2. Replace `Color.LTGRAY`/`Color.WHITE` with theme attributes
   (`colorSurface`, `colorOnSurface`, etc.).
3. Compute readable text colour for category backgrounds, or use a coloured
   accent strip instead of a full background.
4. Check touch targets, TalkBack labels, and contrast in both themes.

**Files.** `styles.xml`, `colors.xml`/`values-night`, layouts,
`SmsListItemHolder`, `CategoryListItemHolder`, tests/manual.

**Acceptance.**
- App follows system dark mode without restart.
- No hardcoded text colour on a user-coloured background.
- Contrast passes a documented check for the built-in palette.
- TalkBack announces category name, unread count, sender, and score.

**Verification.** Manual screenshots in light/dark; Accessibility Scanner or
equivalent; lint.

---

#### IP-24 · Onboarding and permission recovery

| | |
|---|---|
| Type / Priority | Story / **P2** |
| Estimate | M |
| Depends on | IP-01 |

**Problem.** First-run users see a permission dialog and an inbox import with
no explanation of the classifier or what training means. A permanently denied
SMS permission leaves only a toast.

**Change.**
1. First-run screen/dialog: what the app does, that everything is on-device,
   how corrections train the model, and why SMS access is required.
2. Request `READ_SMS`; request `READ_CONTACTS` separately as optional.
3. If permanently denied, show a clear explanation and an “Open settings”
   action.
4. Empty states for no categories and no messages.
5. Store a `key_onboarding_done` preference.

**Files.** `MainActivity`, new onboarding activity/fragment, `strings.xml`,
`preferences`/config, tests.

**Acceptance.**
- Fresh install reaches a usable state without guessing.
- Denying contacts still allows SMS import.
- Denying SMS shows a recoverable path, not a dead toast.
- Onboarding does not reappear after completion.

**Verification.** Fresh-install manual test on API 24 and 36; Robolectric test
for the preference gate.

---

#### IP-25 · Category restore, merge, or reorder

| | |
|---|---|
| Type / Priority | Story / **P2** |
| Estimate | M |
| Depends on | Nothing; can slip to 2.0 |

**Problem.** P-16: category deletion is a soft delete in the database, but the UI
offers no restore, merge, or reorder. A mistaken delete moves every message to
Unknown and clears the category’s training.

**Change.** Pick the smallest useful slice:
1. Add an “Undo” step or confirmation that names the consequence.
2. Add a category-management screen that lists soft-deleted categories and can
   restore one (set `visibility=1`).
3. Merge is a later extension: move messages and rules to a target category,
   then hide the source.

Do not add drag-to-reorder until there is evidence users need it; the current
query orders by name.

**Files.** `CategoryDao`, `CategoriesViewModel`, `MainActivity`,
`CategoryListItemHolder`, `strings.xml`, tests.

**Acceptance.**
- A deleted category can be restored without recreating its training.
- Messages that were moved to Unknown are visibly recoverable or clearly
  documented as re-trainable.
- Merge, if implemented, reassigns messages and sender rules atomically.
- The Unknown category can never be deleted or hidden.

**Verification.** DAO tests for soft-delete/restore/merge; manual long-press
flow.

### Phase 3 — 2.0 candidates (not committed)

| ID | Candidate | Why later |
|---|---|---|
| IP-30 | Opt-in per-category notifications | Needs `POST_NOTIFICATIONS`, channel design, and a privacy decision; not core |
| IP-31 | Home-screen widget with unread counts | Useful but not differentiating; receiver/widget plumbing |
| IP-32 | Statistics surface (messages per category, unknown ratio, training progress) | Nice-to-have; low user value versus review queue |
| IP-33 | FTS/Room evaluation | Only if measurements show search/scale pain; current 10k filter is ~3.25 ms desktop |
| IP-34 | Localization | Needs clean strings and translators; do after IP-01/IP-23 |
| IP-35 | API 37 migration | Only when Play requires it or a dependency forces it; OTP delay is a product change |

---

### Suggested execution order

1. **IP-01 + IP-02** — make the current claims true and complete the store
   changelog. This is cheap and removes ambiguity from every later release.
2. **IP-03 (Izzy first)** — submit the already-signed APK while the code work
   continues. Open the F-Droid RFP in parallel; do not block on it.
3. **IP-04** — bump AGP first, keep Gradle stable for one green run, then test
   Gradle 9.9.2. Keep the dependency bump isolated from feature work.
4. **IP-13 + IP-15** — fix classifier parity and the cursor/chunk bugs before
   adding new paths that depend on them.
5. **IP-12 → IP-11** — build the async filter path first, then reuse it for
   global search.
6. **IP-14** — confidence and review queue. The receiver is declined (IP-10);
   keep the on-open sync and spend the effort on making its output trustworthy.
7. **IP-05** — API 37 smoke test and decision record, before any target bump.

### Definition of done for 1.8.0

- [ ] README and store text say categorization runs when the app opens; no
      receiver or `RECEIVE_SMS` permission is present.
- [ ] Search from the home screen returns hits across categories and opens the
      right category.
- [ ] Typing in search never runs the full-list filter on the UI thread.
- [ ] The same message/exemplar pair scores identically in first-pass
      classification and retraining.
- [x] Auto-categorized rows show a confidence; Unknown has a bounded review
      path with suggestions.
- [ ] 1,000-ID mutations and the inbox cursor are safe.
- [ ] README, ARCHITECTURE, changelog, and store metadata match the shipped
      behaviour.
- [ ] Unit tests, lint, instrumented API 24/36, and the two-build release check
      are green.
- [ ] The Android 17 OTP limitation is documented; the app is still target 36.

## 10. Decision gates

These are the choices that need an explicit answer before implementation. The
rest of the plan can proceed without them.

1. **`RECEIVE_SMS` permission.** **Resolved 2026-11-21: keep on-open sync.**
   No receiver, no `RECEIVE_SMS`. IP-10 records the decision; revisit only with
   an explicit product change.
2. **Target API 37.** Recommended: stay at 36 until Play publishes the 2027
   requirement; test API 37 now. **Decision needed before any 37 bump.**
3. **F-Droid buildserver fallback.** If AGP 9.3.1 / SDK 36 cannot build on the
   buildserver, choose: wait, pin the newest supported AGP, or ship only via
   Izzy/GitHub. Do not decide before the buildserver result is known.
4. **Encrypted backup default.** **Resolved 2026-11-21: optional passphrase.**
   Plaintext remains the default when the field is blank; the warning is explicit.
5. **Notifications / widget.** Recommended: defer to 2.0 and require a design
   first. **Decision needed only if the user wants them in 1.8.**
6. **Stale `improvements.md`.** Recommended: archive, not delete. **User
   confirmation required before moving/deleting.**

---

## 11. Verification matrix

### Every release

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug
./gradlew connectedDebugAndroidTest        # API 24 and 36
./gradlew clean assembleRelease -PversionCode=<code> -PversionName=<name>
# repeat the release build and compare bytes
cmp build/outputs/apk/release/app-release-unsigned.apk <second-build>
aapt2 dump badging build/outputs/apk/release/app-release-unsigned.apk
```

Release gates that already exist and must stay:

- unit tests + lint before signing;
- two clean unsigned builds byte-identical;
- signing key only under `$RUNNER_TEMP`, removed in `if: always()`;
- APK signature and manifest check;
- SHA-256 files attached to the GitHub release;
- Fastlane/F-Droid metadata validation.

### New tests by ticket

| Ticket | Test |
|---|---|
| IP-10 | On-open sync still deduplicates by provider `_ID`; no receiver/permission regression |
| IP-11 | Global search returns cross-category hits with category labels |
| IP-12 | Debounce/generation: last query wins, no stale submit |
| IP-13 | Same pair scores equally in first pass and retraining |
| IP-14 | Suggestion selection, threshold display, accept-action trains |
| IP-15 | 1,000-ID chunked update; no manual cursor close |
| IP-20 | Rule precedence, address normalization, category-delete cleanup, v4→v5 migration |
| IP-21 | Encrypted round-trip, wrong passphrase, tamper/truncate, legacy plaintext |
| IP-22 | Selection state, bulk move/delete/read counts, 1,000-item selection; chunked batch delete; snapshot undo |
| IP-23 | Dark/light screenshots, contrast check, TalkBack labels (device pending) |
| IP-24 | First-run gate, contacts denied, SMS permanently denied (device pending) |
| IP-25 | Soft-delete/restore, merge reassignment, Unknown protection |

### Live checks

```bash
gh run list --limit 10
gh release view v1.7.3 --json assets
curl -sI https://f-droid.org/en/packages/in.rahulja.groupingmessages/
```

---

## 12. Non-goals / do-not-do

- **No Compose/Room/Hilt/Kotlin rewrite** for its own sake. The current stack
  is small, tested, and easy to reason about.
- **No Google Play target.** The SMS-permission policy requires a default
  handler role or an exception this product does not have.
- **No MMS/RCS/default-SMS-handler scope.** It changes the permission model and
  the product’s privacy story.
- **No cloud, accounts, analytics, crash reporting, ads, or sync.**
- **No SQLCipher for the live DB** in this plan. Encrypted backup is the
  proportionate step; live-DB encryption is a separate migration with a
  restore-path project.
- **No notification or widget work** until the core promise and review loop are
  solid.
- **No target 37 bump** before the OTP delay is accepted and documented.

---

## 13. Evidence appendix

Tier 1 = official platform/vendor documentation; Tier 2 = official project or
store pages; Tier 3 = community/market evidence used only as a risk signal.

| Tier | Source | Used for |
|---|---|---|
| 1 | `https://developer.android.com/about/versions/17/behavior-changes-17` | Standard SMS OTP 3-hour delay for target 37+ |
| 1 | `https://developer.android.com/about/versions/17/behavior-changes-all` | WebOTP delay applies to all apps; Android 17 privacy changes |
| 1 | `https://developer.android.com/blog/posts/android-17-is-here` | Android 17 / API 37 shipped; large-screen resizability change |
| 1 | `https://developer.android.com/google/play/requirements/target-sdk` | API 36 required from 2026-08-31; no 2027 date published |
| 1 | `https://developer.android.com/develop/background-work/background-tasks/broadcasts/broadcast-exceptions` | `SMS_RECEIVED` exempt from implicit-broadcast restrictions |
| 1 | `https://developer.android.com/reference/android/content/BroadcastReceiver#goAsync()` | 10-second receiver window and `goAsync()` contract |
| 1 | `https://developer.android.com/about/versions/16/behavior-changes-all` | Android 16 JobScheduler/WorkManager quota changes |
| 1 | `https://developer.android.com/build/releases/agp-9-5-0-release-notes` | AGP 9.5.x, max API 37, minimum Gradle 9.6.0 |
| 1 | `https://services.gradle.org/versions/current` | Gradle 9.9.2 current |
| 1 | `https://f-droid.org/docs/Inclusion_Policy/` | FLOSS, build-from-source, anti-feature rules |
| 2 | `https://f-droid.org/en/packages/in.rahulja.groupingmessages/` | Package not listed (HTTP 404, 2026-11-21) |
| 2 | `https://izzyondroid.org/docs/general/AppInclusionPolicy/` | Signed release APK, no debuggable/testOnly, metadata requirements |
| 2 | `https://izzyondroid.org/contributing/NewAppInclusions/` | Fastlane/metadata and release-tag expectations |
| 2 | `https://github.com/xRahul/GroupingMessages/releases/tag/v1.7.3` | Release assets and signed APK presence |
| 2 | `https://dl.google.com/dl/android/maven2/...` metadata | Current AndroidX/Material versions |
| 2 | `https://repo1.maven.org/maven2/...` metadata | Mockito/Robolectric versions |
| 3 | `https://github.com/CompassMB/MBCompass/issues/88` | F-Droid buildserver AGP lag risk (unverified for this app) |
| 3 | `https://github.com/moezbhatti/QKSMS/issues/1816` | QKSMS abandonment signal |
| 3 | `https://f-droid.org/en/packages/org.fossify.messages/` | Active competitor baseline |
| 3 | `https://f-droid.org/en/packages/com.github.tmo1.sms_ie/` | Active SMS import/export baseline |

**Local evidence**

- `app/build/test-results/testDebugUnitTest/*.xml`: 108 tests / 17 suites,
  0 failures.
- `app/build/reports/lint-results-debug.txt`: 0 errors, 7 warnings.
- `gh run list`: CI, instrumented, and release runs green on 2026-11-20.
- `gh release view v1.7.3`: APK, debug APK, and SHA-256 assets present.
- Benchmark harness (temporary, not committed): 10k `SmsFilter` rows × 20
  queries = 65 ms; classifier 100 exemplars = 71 ms per 100 queries, 500
  exemplars = 94 ms per 100 queries on the desktop JVM.

**Unverified / do not overclaim**

- The 2027 Play target-API deadline and its level.
- Whether the F-Droid buildserver can build this exact AGP 9.3.1 / SDK 36
  project.
- IzzyOnDroid absence via the repository index (only page/search checks were
  done).
- Accuracy of the classifier on real multilingual inboxes; only synthetic and
  unit-level benchmarks exist.
- Any claim that OTP messages are instant on API 37 for a non-default SMS app;
  the opposite is documented.
