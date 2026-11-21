# Project Context & Memory

## Project structure

- Native Android app, Java, single `app` module.
- Build: Gradle wrapper 9.9.2, AGP 9.5.2, JDK 21, source/target Java 17.
- `compileSdk`/`targetSdk` 36, `minSdk` 24.
- Database schema version 4.

## Key commands

- Build debug: `./gradlew assembleDebug`
- Build release: `./gradlew assembleRelease`
- Unit tests: `./gradlew testDebugUnitTest`
- Lint: `./gradlew lintDebug`
- Instrumented: `./gradlew connectedDebugAndroidTest`

## Architecture decisions

- SMS and contact processing stays on-device. No analytics, crash reporting or sync.
- Contacts permission is optional; SMS is required.
- Update check is opt-in and limited to HTTPS GitHub release pages.
- Backup uses the Storage Access Framework and validates the imported database.
- Release is tag-driven: `vX.Y.Z`, `versionCode = X*10000 + Y*100 + Z`.
- Signing key is written under `$RUNNER_TEMP` only and removed after the build.
- F-Droid and Fastlane metadata are validated by `store.yml`.
