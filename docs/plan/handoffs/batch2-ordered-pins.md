# B2: ordered pins

- **Repo:** qualified-opinion/serene-launcher
- **Branch:** `feat/ordered-pins` from `main` after B1 (#31) merges
- **Engine:** Codex on high builds it. Claude Code reviews it.
- **Items:** #19 part 1 (B11), N-15 ([Kiln review 1](../reviews/kiln-review-1.md))

## Goal
Pins keep the order the user sets, across restarts and updates, and no pin is ever lost by mistake.

## Where things are today (main after B1)
- `LauncherPreferencesRepository` stores pins as the string set `pinned_app_ids`. Sets have no order.
- `LauncherViewModel.buildUiState` builds `pinnedApps` by filtering the scanned list, so Home shows pins in the scan's alphabetical order. Pins missing from a scan are already hidden, not deleted.
- `InstalledAppsRepository.appChanges()` turns every `LauncherApps.Callback` event into `Unit`, so the ViewModel can't tell a removal from an update.
- Ids: `package/activity` for personal apps, `package/activity@serial` for other profiles. Activity names can contain `$`.

## Scope
1. **Storage.** Add one new key, `pinned_app_order` (string), holding the ids in order joined by `\n`. It's the source of truth once present. Every pin write also rewrites `pinned_app_ids` to the same ids as a set, so a downgrade still finds its pins. No other key changes.
2. **Read and migration.** `LauncherUserPreferences` gets `pinnedAppOrder: List<String>`.
   - If `pinned_app_order` exists, use it. Ids in the set but missing from the order go at the end; ids in the order but not in the set are dropped (the set wins on a mismatch, since it's what older builds wrote).
   - If it doesn't exist, the order is the set sorted the way Home sorts today (by app label, same comparator as `AppSearch.sort`). Ids with no matching app sort last by id.
   - Don't write `pinned_app_order` on read or at start. Write it only on the first reorder or pin change, so Home never visibly reorders on update.
3. **Pin changes.** Pinning appends to the end. Unpinning removes that one id. `initializeDefaultPinsIfNeeded` writes the order in the order `selectInitialPinnedIds` picks.
4. **Home order.** `buildUiState` maps the order to apps by id and skips ids with no app in the current scan (they stay in storage).
5. **Reorder mode.** The long-press menu on a pinned app gets "Reorder pins". Home then shows up and down buttons beside each pin and a "Done" button. Moving the first up or the last down does nothing. Each move saves right away. Back or Home exits the mode. Drag-to-reorder is cut. Strings go in `strings.xml`; buttons need content descriptions.
6. **Pruning.** Change `appChanges()` to emit a small sealed event, so `onPackageRemoved(pkg, user)` carries the package name and the profile serial. On that event only, remove every pin whose package is `pkg` in that profile (personal: no `@`; other profile: `@serial`), and save. Every other event just refreshes. Never prune by comparing scans, on an empty scan, on `onPackagesUnavailable`, or when a profile is paused or locked.
7. Wrap the prune save in B1's `savePreferences` so a failed save can't crash.

## Out of scope
Rename and hide (B4), backup rules (B3), work-profile removal clean-up (N-05), the Dependabot PRs, the app id format, the manifest, signing and permissions.

## Acceptance criteria
- `./gradlew assembleDebug testDebugUnitTest lintDebug processReleaseManifest` passes on JDK 21, and CI is green, including the offline check.
- Unit tests, with the temp or in-memory store from B1:
  - an old-style set with no order key reads in label order, and reading writes nothing;
  - ids with `@serial` and `$` survive a save and reload unchanged;
  - after a move, a fresh store on the same file reads the same order;
  - pin appends at the end, unpin removes one, and both keep `pinned_app_ids` equal to the order as a set;
  - a set/order mismatch resolves as in scope item 2;
  - a removed event for `pkg` in the personal profile drops only that package's personal pins and leaves the same package's work pin;
  - an empty scan, and a scan missing a paused profile's apps, prune nothing and keep the order;
  - restoring settings before the apps are installed keeps all pins, and they show once the apps appear.
- A Compose or Robolectric test: in Reorder mode, tapping down on the first pin swaps it with the second.
- The manifest is unchanged and the only new key is `pinned_app_order`.
- The change is about 400 lines or fewer, tests included.
- The PR description is in plain language, has phone steps, and updates the ROADMAP B1 row to merged and the B2 row to "In review".

## Jan's phone check
1. Install over the B1 build. All your pins are there, in the same order as before.
2. Long-press a pin, choose "Reorder pins", move one up, press Done. Restart the phone, and the order stays.
3. Uninstall a pinned app. Its pin goes, and nothing else moves.
4. Update any pinned app from the Play Store. Its pin stays where it was.
5. If you have a work profile, pause and resume it. The work pins come back in their places.

## Risks
- `onPackageRemoved` firing during an app update would delete pins. Android reports updates as changed, not removed, but test step 4 on the phone confirms it.
- Writing the order on read would reorder Home on update, which is why it's write-on-change only.
- A wrong profile match in pruning could remove a personal pin when the work copy is uninstalled. The test above covers it.

## Process
Open one PR, update the ROADMAP status table, and don't merge.
