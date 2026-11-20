# Technical Design (TD)

## Architecture

- Single-module Java Android app, XML views, `Activity` -> `ViewModel` -> DAO.
- `ExternalContentBridge` reads the SMS and contacts providers.
- `DatabaseHelper`/`AppDatabase` own one SQLite database; all callers use `AppDatabase.get`.
- `SmsDao`, `CategoryDao`, `ConfigDao` own SQL. User input is passed as selection args.
- `SmsCategorizer` performs on-device scoring; `TrainSms` applies corrections.
- `AppExecutors` provides one disk executor and a separate network executor.

## Database

- Schema version 4.
- `sms` has a category foreign key and a nullable `provider_id` with a unique partial
  index so repeated provider reads are ignored.
- Foreign keys are enabled in `onOpen`, after any upgrade. The v4 upgrade rebuilds the
  `sms` table, removes the historical self-referential `similar_to` constraint (the app
  uses `0` as "no exemplar"), copies all rows, repairs orphan categories and similar-to
  references, and recreates indexes and the update trigger.
- Migration v1 -> v4 and v2 -> v4 are covered by `MigrationTest`.

## Backup

- Export: checkpoint the WAL, close the live database under `synchronized
  (AppDatabase.class)`, stream the file to a user-selected SAF `OutputStream`.
- Import: copy to a scratch file, verify the SQLite header, `PRAGMA integrity_check`,
  `user_version <= DATABASE_VERSION`, and required tables, then close and atomically
  replace the live file.
- The legacy fixed-path export remains only for existing tests/backward compatibility.
- The backup is plaintext. The UI warns before export.

## Network

- The update check is off by default (`key_check_updates`).
- When enabled, it runs on the network executor, uses HEAD with 5 s timeouts, follows no
  redirects itself, accepts only HTTPS GitHub release URLs for this repository, and
  normalizes a leading `v` before comparing versions.
- No other network calls exist.

## CI/CD

1. `ci.yml`: wrapper validation, lint, unit tests, debug build.
2. `instrumented.yml`: emulator matrix API 24 and 36.
3. `release.yml`: tag validation, tests, lint, reproducible unsigned builds, signing from
   `$RUNNER_TEMP`, APK verification, GitHub release.
4. `store.yml`: Fastlane/F-Droid metadata and released APK validation.
5. `dependencies.yml`: dependency review on pull requests.

Secrets: `KEY_STORE_BASE64`, `KEY_STORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`. The
keystore is decoded outside the checkout and deleted in an `if: always()` step.

## Versioning

`versionName` and `versionCode` come from the `vX.Y.Z` tag. Local defaults in
`build.gradle` are only for developer builds; CI passes `-PversionCode` and
`-PversionName`.
