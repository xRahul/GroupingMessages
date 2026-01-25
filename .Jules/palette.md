## 2024-05-22 - Build Environment Mismatch & Support Libs
**Learning:** The project uses legacy `android.support` libraries and older Gradle/AGP versions that are incompatible with the provided Java 21 environment.
**Action:** Rely on static analysis and XML validation for verification when the build environment is broken due to version mismatches.
