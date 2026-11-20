# Releasing

## Preconditions

- `master` is green: unit tests, lint, debug build.
- The version has been smoke-tested on API 24 and API 36.
- The signing secrets exist in the repository:
  `KEY_STORE_BASE64`, `KEY_STORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`.

## Create the tag

The tag is the release. Do not edit a tag after publishing it.

```bash
git tag -a v1.7.0 -m "Grouping Messages 1.7.0"
git push origin v1.7.0
```

Or run **Release -> Run workflow** and choose `patch`, `minor`, or `major`. The
workflow bumps the version, updates `metadata/in.rahulja.groupingmessages.yml`, commits
and pushes that change, creates the tag, and then builds it.

`versionName` = `1.7.0`; `versionCode` = `10700` (`1*10000 + 7*100 + 0`).

## What the release workflow does

1. Validates the tag shape and that the tag exists.
2. Runs `testDebugUnitTest` and `lintDebug` on the tagged commit.
3. Builds the unsigned release twice and fails if the APKs differ.
4. Decodes the keystore into `$RUNNER_TEMP/keystore`, signs, verifies the signature,
   checks package/version/no-debuggable/no-testOnly, and deletes the keystore in an
   `if: always()` step.
5. Uploads the APK and `.sha256` to the GitHub release.

Nothing is built from the moving `latest` tag. Debug builds use `debug-latest`.

## Store checks

`store.yml` runs after the release is published and validates:

- Fastlane title, short description (≤ 80 characters), full description.
- 512×512 icon and at least two phone screenshots.
- F-Droid recipe keys, license, current version/code, build commit and Gradle args.
- The published APK: signed, not debuggable/testOnly, version matches the recipe.
- Optional `AllowedAPKSigningKeys` fingerprint match.

## F-Droid

1. Update `metadata/in.rahulja.groupingmessages.yml` for the new tag.
2. Run `fdroid lint`.
3. Submit a merge request to `fdroid/fdroiddata`.
4. After the first buildserver build, compare the F-Droid unsigned APK with the
   developer APK. Do not add `Binaries:` until reproducibility is confirmed.

## IzzyOnDroid

IzzyOnDroid uses the signed APK attached to the GitHub release. Confirm the release has
the APK and that the Fastlane metadata is current, then submit the inclusion request.

## Rollback

Do not move or delete a published tag. Ship a new patch release with a higher
`versionCode`. Rotating the signing key requires a documented migration and will break
in-place updates on Android.
