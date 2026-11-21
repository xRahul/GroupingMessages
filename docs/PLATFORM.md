# Platform policy and API 37 decision

**Last updated:** 2026-11-21
**Current:** `minSdk 24`, `compileSdk 36`, `targetSdk 36`
**Decision owner:** project maintainer

## Why this file exists

Android 17 (API 37) changes how SMS one-time passwords (OTP) are exposed to apps
that are not the default SMS handler. That directly affects a categorizer that
reads the inbox. This file records the decision, the constraints, and the checks
to run before changing `targetSdk`.

## Current decision

**Stay on `targetSdk 36` until Google publishes the next Play requirement, or a
dependency forces 37.** Test the app on an API 37 emulator now. Do not bump
`targetSdk` as a routine dependency upgrade.

This is a product decision, not a build limitation: AGP 9.5 supports API 37.

## Android 17 OTP rules

Two separate official statements apply.

### WebOTP format: all apps

Beginning with Android 17, if an app has permission to read SMS but is not the
intended recipient of a WebOTP message (as determined by domain verification),
the message is not accessible until three hours after receipt. During that time
the `SMS_RECEIVED_ACTION` broadcast is withheld and SMS provider queries are
filtered. This applies to **all apps, regardless of target API level**.

Source: <https://developer.android.com/about/versions/17/behavior-changes-all>

### Standard SMS OTP: apps targeting 37+

Android is extending the same protection to standard SMS messages containing an
OTP (messages that do not use the WebOTP or SMS Retriever formats). For most apps
**targeting Android 17 (API level 37) or higher**, those messages do not become
available until three hours after receipt. The `SMS_RECEIVED_ACTION` broadcast is
withheld and SMS provider queries are filtered during the delay.

Default SMS handlers, connected-device companion apps, and similar apps are
exempt. This app is not exempt.

Source: <https://developer.android.com/about/versions/17/behavior-changes-17>

### What this app must say

- It is a categorizer, not an OTP consumer. SMS Retriever and SMS User Consent
  are not drop-in fixes for reading and grouping the inbox.
- On API 36, standard bank/OTP SMS are not subject to the new standard-SMS
  delay, but WebOTP-format messages may be filtered for three hours.
- On API 37, standard OTP messages may arrive up to three hours late. Do not
  promise “instant OTP categorization” in the README, store listing, or UI.
- A user who needs instant OTP handling should use the default SMS app; this app
  deliberately does not become one.

## Google Play target API timeline

- From **31 August 2026**, new apps and updates must target **API 36**.
  Source: <https://developer.android.com/google/play/requirements/target-sdk>
- No 2027 deadline was published on that page as of **2026-11-21**. Do not
  invent a date; re-check before planning the 37 migration.
- The app does not target Google Play (SMS-permission policy). The deadline
  still sets the ecosystem baseline for F-Droid and Izzy builds.

## API 37 smoke checklist

Run these on an API 37 emulator or device before any `targetSdk 37` change:

- [ ] Fresh install, grant `READ_SMS`, import an inbox, open a category.
- [ ] Edge-to-edge: toolbars, lists, FAB, and dialogs do not overlap system bars.
- [ ] Predictive back: back gesture leaves the app or returns to the previous
      screen without a black frame or a crash.
- [ ] Large screen / free-form window: the app is usable at widths above 600 dp;
      orientation and resizability restrictions are not relied on.
- [ ] If notifications ship: `POST_NOTIFICATIONS` runtime request and channels
      work; denial does not break core flows.
- [ ] OTP behaviour: send a standard OTP-format SMS and a WebOTP-format SMS;
      record when each becomes visible. On target 37, expect the three-hour
      delay for standard OTP.
- [ ] Existing API 24 and API 36 instrumented tests still pass.

Suggested commands (adjust the AVD name):

```bash
sdkmanager "platforms;android-37" "system-images;android-37;google_apis;x86_64"
avdmanager create avd -n api37 -k "system-images;android-37;google_apis;x86_64"
emulator -avd api37
./gradlew connectedDebugAndroidTest
```

## Trigger to revisit

Revisit this decision when any of the following happens:

1. Google publishes a Play deadline that requires API 37.
2. A dependency or SDK upgrade stops working on `compileSdk 36`.
3. The project decides to accept the OTP delay and document it in the store
   listing and onboarding.

When that happens, update this file, `README.md`, the store description, and the
instrumented matrix in the same change.
