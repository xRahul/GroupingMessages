# Master Debug APK Auto-Release Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** On every push to the main development branch (`main` or `master`), GitHub Actions builds the debug APK and publishes/updates a rolling pre-release named `latest` containing `GroupingMessages-debug.apk`.

**Architecture:** Existing workflow `.github/workflows/master_debug_release.yml` already implements build → artifact upload → `softprops/action-gh-release@v2` rolling release (tag `latest`). This plan closes the gaps: add `main` branch trigger, ignore local tooling caches, commit/push, and verify the full CI/CD chain end-to-end via `gh`.

**Tech Stack:** GitHub Actions, Gradle 9.7.1 wrapper, AGP 9.3.1, Java 21 (temurin), `softprops/action-gh-release@v2`.

## Global Constraints

- Default branch of repo `xRahul/GroupingMessages` is `master`; triggers must cover both `main` and `master` (matches existing ci.yml/instrumented.yml convention).
- Debug APK asset name must remain exactly `GroupingMessages-debug.apk`; release tag stays `latest`, prerelease: true.
- Do not touch other workflows (ci.yml, release.yml, instrumented.yml, manual_debug.yml, dependencies.yml) except where a task says so.
- No signed-release changes; keystore secrets untouched.
- Working tree must stay clean of unrelated files when committing.

---

### Task 1: Add `main` branch trigger to debug release workflow

**Files:**
- Modify: `.github/workflows/master_debug_release.yml` (the `on.push.branches` list, currently `[ "master" ]`)

**Interfaces:**
- Consumes: nothing
- Produces: workflow triggers on pushes to both `main` and `master`; all other behavior unchanged

- [ ] **Step 1: Edit the branches list**

Change:
```yaml
on:
  push:
    branches: [ "master" ]
```
to:
```yaml
on:
  push:
    branches: [ "main", "master" ]
```

- [ ] **Step 2: Validate YAML syntax**

Run: `python3 -c "import yaml,sys; yaml.safe_load(open('.github/workflows/master_debug_release.yml')); print('yaml ok')"`
Expected output: `yaml ok`

- [ ] **Step 3: Confirm trigger lines**

Run: `grep -n -A2 'push:' .github/workflows/master_debug_release.yml | head -5`
Expected: shows `branches: [ "main", "master" ]`

### Task 2: Ignore local tooling cache in .gitignore

**Files:**
- Modify: `.gitignore` (append one entry under the IDE/tooling artifacts block)

**Interfaces:**
- Consumes: nothing
- Produces: `.ruff_cache/` ignored by git

- [ ] **Step 1: Append entry**

Under the `# IDE / tooling artifacts` section add:
```
.ruff_cache/
```

- [ ] **Step 2: Verify ignore takes effect**

Run: `git check-ignore -v .ruff_cache && git status --porcelain`
Expected: `.ruff_cache` matched by `.gitignore:<n>:.ruff_cache/`; porcelain output empty before staging

### Task 3: Commit, push, verify CI/CD + release end-to-end

**Files:**
- Modify: none new (commits Task 1 + Task 2 outputs)

**Interfaces:**
- Consumes: edited `.github/workflows/master_debug_release.yml` and `.gitignore`
- Produces: commit on `master`, green `Master Debug Release` run, refreshed `latest` release asset

- [ ] **Step 1: Review diff**

Run: `git diff && git status --porcelain`
Expected: only the two intended files changed; no strays staged

- [ ] **Step 2: Stage exactly those files, commit, push**

```bash
git add .github/workflows/master_debug_release.yml .gitignore
git commit -m "ci: trigger debug apk autorelease on main branch too, ignore ruff cache"
git push origin master
```

- [ ] **Step 3: Watch Master Debug Release run until completion**

Run: `gh run list --workflow=master_debug_release.yml --limit 1` then `gh run watch <id> --exit-status`
Expected: conclusion `success`

- [ ] **Step 4: Verify release asset refreshed**

Run: `gh release view latest --json assets,updatedAt,isPrerelease --jq '{assets: [.assets[].name], updatedAt, isPrerelease}'`
Expected: assets includes `GroupingMessages-debug.apk`, updatedAt is post-push time, isPrerelease true

- [ ] **Step 5: Verify CI workflow also green**

Run: `gh run list --workflow=ci.yml --limit 1`
Expected: latest CI run on master concludes success (lint/test/build-check)
