# B1: settings hardening

- **Repo:** qualified-opinion/serene-launcher
- **Branch:** `fix/settings-hardening` from `main` `82082df`
- **Engine:** Codex on high builds it. Claude Code reviews it.
- **Source items:** N-01, N-02, N-03, N-07, N-09, N-13, B15 ([Kiln review 1](../reviews/kiln-review-1.md))

## Goal
The launcher never crash-loops on a bad settings file, never ends up with no default pins, and doesn't flash wrong screens on start. This is the base the ordered-pins migration (B2) writes onto.

## Scope
1. **N-01 and N-09.** In `data/LauncherPreferencesRepository.kt`, add a `ReplaceFileCorruptionHandler` to `preferencesDataStore(...)` that replaces a damaged file with empty preferences. After a read error, the flow keeps listening instead of stopping. Any other unexpected exception from a save at start (`LauncherViewModel.refreshInstalledApps`) or from pinning is caught and logged without app data, and never crashes the app.
2. **N-02.** Only set `has_initialized_default_pins` when the scan returned at least one app. An empty scan leaves the flag unset, so the next refresh tries again.
3. **N-03.** Pass the loading state into Home and the drawer. While apps are loading, show neither the "no pinned apps" card nor "No apps found".
4. **N-07.** In `ui/screens/AppDrawerScreen.kt`, the keyboard Search key does nothing when the search box is empty or only spaces.
5. **B15.** Add `values-night` window-background colours, or a DayNight parent theme, so a dark-mode phone doesn't flash light on start. Keep the follow-system default.
6. **N-13.** Tests use a temporary DataStore file and clean it up.

## Out of scope
Pin order and storage format (B2), backup rules (B3), work-profile tracking (N-05), the Dependabot PRs, and any change to stored keys, the app id, signing or permissions.

## Acceptance criteria
- `./gradlew assembleDebug testDebugUnitTest lintDebug processReleaseManifest` passes on JDK 21, and CI is green, including the offline check.
- New unit tests fail on `82082df` and pass on the branch:
  - a damaged settings file loads as defaults, and the next save succeeds;
  - an empty first scan doesn't set the flag, and a later non-empty scan pins the defaults;
  - Search on an empty query launches nothing;
  - while loading, Home has no empty-state card.
- No stored preference key is added, renamed or removed. The manifest is unchanged.
- The change is about 300 lines or fewer.
- The PR description is in plain language and has phone steps.

## Jan's phone check
1. Install over your current build. Your pins and theme should still be there.
2. With the phone in dark mode, press Home a few times and open the launcher from cold. There should be no white flash.
3. Open the drawer, leave search empty and press the keyboard's Search key. Nothing should open.

## Risks
The corruption handler hides real data loss, so it should log the event without app names. A wrong flag change could re-add default pins for existing users, so test that an initialised user is never re-seeded.

## Process
Open one PR, update the B1 row of the ROADMAP status table, and don't merge.
