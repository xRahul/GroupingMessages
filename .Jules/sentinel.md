## 2025-10-21 - Insecure Database Backup Storage
**Vulnerability:** The application was exporting the SMS database (containing sensitive user messages) to the root of the external storage (`/sdcard/GroupMessagingBackupV2`).
**Learning:** This exposed the user's private messages to any application with `READ_EXTERNAL_STORAGE` permission.
**Prevention:** Use `Context.getExternalFilesDir(null)` (Scoped Storage) to store app-specific sensitive files, ensuring they are private to the application on Android 10+ and reducing the attack surface on older versions. This also allows removing the `WRITE_EXTERNAL_STORAGE` permission.
