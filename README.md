# Grouping Messages

[![CI](https://github.com/xRahul/GroupingMessages/actions/workflows/ci.yml/badge.svg)](https://github.com/xRahul/GroupingMessages/actions/workflows/ci.yml)
[![Instrumented Tests](https://github.com/xRahul/GroupingMessages/actions/workflows/instrumented.yml/badge.svg)](https://github.com/xRahul/GroupingMessages/actions/workflows/instrumented.yml)
[![GitHub license](https://img.shields.io/github/license/xRahul/GroupingMessages.svg)](https://github.com/xRahul/GroupingMessages/blob/master/LICENSE)
[![GitHub issues](https://img.shields.io/github/issues/xRahul/GroupingMessages.svg)](https://github.com/xRahul/GroupingMessages/issues)
[![Releases](https://img.shields.io/github/release/xRahul/GroupingMessages.svg)](https://github.com/xRahul/GroupingMessages/releases/latest)

An Android app that automatically categorizes your SMS into categories of your choice.

- **Minimum supported Android:** 7.0 Nougat (API 24)
- **Target/compile SDK:** Android 15 (API 35)

Every trained model is user specific: the app learns from the messages you move into
categories, so each user's categorization fits their own inbox. It needs some training at
the beginning, and it keeps learning every time you correct a message's category.

All SMS processing happens **on-device** — there are no external APIs involved in reading,
training, or categorizing messages (see [Privacy](#privacy)).

## Features

- Automatic categorization of incoming SMS using a hybrid on-device classifier
  (word TF-IDF cosine + character-trigram similarity, see
  [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md))
- User-defined categories with colors; Unknown fallback for unclassified messages
- Training: move a message to the right category and similar future messages follow;
  re-training can propagate a correction to already-stored similar messages
- Per-category read/unread counts and mark-all-read
- Configurable similarity mode (balanced / words only / characters only / legacy
  Levenshtein) and threshold in Settings
- Database backup/export and restore from device storage
- Version check against GitHub releases (the app's only network call)

## Screenshots

![Activities](https://github.com/xRahul/GroupingMessages/raw/master/Screenshots/Activities_View_1.2.jpg)

---

![Settings](https://github.com/xRahul/GroupingMessages/raw/master/Screenshots/Settings_View_1.2.jpg)

## Building

Requirements:

- JDK 21 (Temurin recommended)
- Android SDK with platform 35 (Gradle downloads the rest via the wrapper)

```bash
./gradlew assembleDebug        # debug APK
./gradlew lint                 # Android lint
./gradlew testDebugUnitTest    # JVM unit tests (Robolectric)
./gradlew connectedDebugAndroidTest  # instrumented tests (device/emulator required)
```

Releases are built by GitHub Actions from tags; the version code/name can be overridden
on the command line with `-PversionCode=` / `-PversionName=`.

## Permissions rationale

| Permission | Why it is needed |
|---|---|
| `READ_SMS` | Read inbox messages to import them into the local database and categorize them. Nothing is ever sent anywhere. |
| `READ_CONTACTS` | Distinguish senders saved in your contacts from companies and bare numbers when labeling messages. |
| `INTERNET` | One purpose only: checking for newer releases by resolving `github.com/xRahul/GroupingMessages/releases/latest`. |

## Privacy

- All SMS reading, cleaning, training, and categorization runs entirely on-device.
- Message content never leaves your phone. There is no analytics, no crash reporting,
  and no sync service.
- The **only** outbound network call the app makes is the optional version check in
  Settings, which fetches the redirect target of the GitHub releases URL to compare
  versions. No message data is included.
- OS-level auto-backup of app data is disabled (`allowBackup="false"`); use the in-app
  backup feature instead.

## Backup & export

Settings offers export/import of the full SMS database as a single SQLite file
(`GroupMessagingBackupV3`) in the app's external files directory. Export flushes the WAL
checkpoint first; import verifies the file header before atomically replacing the live
database. See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md#backup-format-notes) for details.

## License

[Apache License 2.0](LICENSE)
