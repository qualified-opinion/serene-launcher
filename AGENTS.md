# AGENTS.md

Start with [docs/plan/ROADMAP.md](docs/plan/ROADMAP.md). It holds the status table, the next batch and its handoff pack.

## Project
Serene Launcher is a minimalist, fully offline Android home screen. It has pinned apps on Home and a searchable A–Z drawer, and it includes work-profile apps. The owner (Jan) is not a developer, so write every PR description in plain language with phone test steps.

## Stack
Kotlin 2.2 and Jetpack Compose, AGP 9.1.1, Gradle 9.3.1 wrapper, compileSdk and targetSdk 36, minSdk 24, DataStore Preferences. The application id is `app.serene.launcher`. Never change it.

## Hard rules
- Fully offline. No `INTERNET` permission and no network libraries. CI fails if the merged release manifest gains `INTERNET`.
- Distribution is sideloaded APKs on GitHub Releases only.
- App ids stay `package/activity` for personal apps and `package/activity@serial` for work apps, so existing pins keep working.
- Any preference change needs a migration and a unit test that starts from the old format. Never drop or rename a stored key without one.
- Never remove a pin because an app is missing from a scan. Remove one only when Android reports that app uninstalled for that profile.
- Never commit secrets, keystores, `local.properties` or build output.

## Build and test
Use JDK 21, because Robolectric needs it for API 36. CI (`.github/workflows/build.yml`) runs:

    ./gradlew assembleDebug testDebugUnitTest lintDebug processReleaseManifest

Add a unit test for every logic change.

## Process
1. Take the next batch in the ROADMAP status table and read its pack in `docs/plan/handoffs/`.
2. Work on a feature branch from the latest `main`, open one PR and keep CI green.
3. Every batch PR updates the ROADMAP status table.
4. Never push to `main`, force-push, tag or create releases. The owner publishes releases.
5. A different engine from the one that wrote the code reviews the PR. It is squash-merged, pinned to the approved head, only after green CI and the owner's OK.
