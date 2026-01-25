## 2026-01-25 - [Data Leakage in Logs]
**Vulnerability:** The application was logging the full `toString()` representation of SMS objects, including the message body and sender address, at INFO level in `ExternalContentBridge`.
**Learning:** Developers might inadvertently log sensitive data when logging whole objects for debugging purposes. Even "harmless" objects can contain PII.
**Prevention:** Avoid logging entire objects containing sensitive data. Override `toString()` to exclude sensitive fields or log only specific non-sensitive fields. Always review logging statements in code that handles PII.
