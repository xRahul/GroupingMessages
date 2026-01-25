# Project Context & Memory

## Project Structure
*   **Type:** Native Android Application
*   **Language:** Java (compatible with Java 17/21 for build environment, source compatibility Java 1.8)
*   **Build System:** Gradle 8.5 (Wrapper configured)
*   **Main Module:** `app/`

## Key Commands
*   **Build Debug:** `./gradlew assembleDebug`
*   **Build Release:** `./gradlew assembleRelease`
*   **Run Tests:** `./gradlew test`
*   **Lint:** `./gradlew lint`

## Architecture Decisions
*   **CI/CD:** GitHub Actions.
*   **Signing:** Keystore injected via Base64 secret `KEY_STORE_BASE64` and decoded at runtime. Signing config uses environment variables.
*   **Versioning:** Automated patch increment based on Git tags.
*   **Artifacts:** Debug APKs retained for 7 days; Release APKs attached to GitHub Releases.

## Configuration Details
*   `app/build.gradle` is modified to include a `signingConfigs` block for release builds, reading from `System.getenv()`.
*   Project uses `androidx` libraries and `simmetrics-core`.
