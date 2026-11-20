# Security policy

## Reporting a vulnerability

Do not open a public issue for a security problem. Use GitHub's private
vulnerability reporting on `xRahul/GroupingMessages`:

<https://github.com/xRahul/GroupingMessages/security/advisories/new>

Include the app version, Android version, device, reproduction steps, and the
smallest message/DB fixture that shows the problem. Do not attach a real inbox
backup.

## Supported versions

The latest published release receives security fixes. Older releases are not
maintained.

## Data and threat model

Grouping Messages processes SMS locally. It does not send, modify, block, or
upload messages.

**Stored data:** message body and sender address, contact display names, category
names and colors, read state, the classifier's exemplar rows, and the local
SQLite database.

**Where it lives:** the app's private internal storage, protected by Android's
app sandbox and file-based encryption. OS backup is disabled
(`android:allowBackup="false"`).

**What leaves the device:** nothing by default. The update check against GitHub is
**off by default**; when the user enables it in Settings, the app sends a HEAD request
to the GitHub releases page. It sends no message data. There is no analytics, crash
reporting, advertising, account, or sync service.

**Backups:** the export/import feature copies the SQLite database through the system
file picker. The file contains every stored message in plain text. Treat it as
sensitive: do not put it in cloud storage or send it to anyone you do not trust. An
encrypted-backup option is planned but is not part of this release.

## Security properties

- All SMS screens are `exported="false"`; only the launcher activity is exported.
- The `FileProvider` is `exported="false"` and grants read access per share.
- No `WebView`, JavaScript bridge, dynamic code loading, or reflection-driven
  plugin surface.
- No analytics or third-party tracker SDKs.
- No SMS content is intentionally logged. If you find a log line containing a
  message body or sender, report it as a vulnerability.
- Database queries use parameterized selections for user-controlled values.

## Known limitations

- The database is not encrypted at rest beyond Android's device encryption. A
  rooted device or an ADB backup can read it.
- The backup file is plaintext. The user chooses its destination and must protect it.
- The update check is opt-in and validates the redirect host, but it still reveals app
  use to GitHub when enabled.
- The app is not a default SMS handler and does not claim Play Store SMS
  eligibility.
- F-Droid builds are produced from the same source tag but signed with F-Droid's
  key unless reproducible builds and developer-signed publishing are enabled.
  Verify the signer before installing a build from a third party.
