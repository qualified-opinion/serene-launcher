# Kiln review 1 of main

- **Date:** 2026-10-22
- **Commit:** `main` `82082df` (after #29, Settings)
- **Engine:** Claude Code in read-only plan mode. Kiln read the findings and checked N-01, N-02 and N-07 in the code.
- **Previous review:** issue #27 (October 2026, findings B1–B16).
- **Changes from that review:** the batch order now puts hardening (B1) before ordered pins, because the pin migration writes to the same settings store as N-01. Pins are pruned only on uninstall events, so N-05 isn't a prerequisite. Renaming apps is cut from v1.

Nothing was run on a device. Line numbers refer to `82082df`.

---

The biggest risk is a damaged settings file: it would make the launcher crash on every start (N-01). Fix that before any pin migration. Nothing in the repo was edited, and nothing was built, run or tested on a device. Points marked *(inference)* are unchecked, and line numbers are at 82082df.

## 1. Correctness and data-loss risks
- **N-01 (High):** the settings store has no handler for a damaged file (`LauncherPreferencesRepository.kt:18-20`).
  - Reading catches the error and quietly shows defaults (`:47-54`), because a damaged-file error counts as an `IOException`.
  - The next save throws the same error, with nothing around it to catch it. One save runs on every start (`LauncherViewModel.kt:133`), another when pinning (`:200`).
  - Result: the home app crashes in a loop.
- **N-02 (Med):** if the first app scan returns nothing, the "default pins done" flag is still set, with no pins (`LauncherViewModel.kt:130-133` → `LauncherPreferencesRepository.kt:66-74`). Default pins are never tried again.
- **N-04 (Med):** Android's cloud backup is on, and the backup rules files are still unedited samples (`AndroidManifest.xml:14-16`, `res/xml/backup_rules.xml`, `res/xml/data_extraction_rules.xml`).
  - The settings file is uploaded to Google's cloud backup, which clashes with the "no network" claim.
  - Restoring to a new phone brings back the "defaults done" flag and work-app ids with the old phone's profile number.
- **N-05 (Med):** the launcher doesn't notice when the work profile is added, removed, paused or resumed. `appChanges()` (`InstalledAppsRepository.kt:65-89`) only listens for app install, update and removal events.
  - A permission error while listing a profile becomes an empty list (`:39-43`), so "couldn't read this profile" looks the same as "this profile has no apps".
  - *(Inference)* A paused work profile probably also returns an empty list.
- **N-15 (Low):** pins are stored by the app's activity name. If an app update renames it, the pin silently disappears from Home (`LauncherViewModel.kt:103`) but stays in storage.
- Launching: start and App-info failures are caught and reported. B4 holds.

## 2. Findings marked fixed that are still partly broken
- **B15, partly broken:** the window background is always the light colour (`values/themes.xml:7`, `values/colors.xml:4`), with no dark-mode version. The screen also starts on "follow system" until saved settings load (`MainActivity.kt:61`). Dark-mode users see a light flash, and #29 made follow-system the default.
- **B6, partly fixed:** see N-05.
- **B8, partly fixed** *(inference)*: the fonts use a weight setting (`Type.kt:30-33`) that Android only supports from version 8.0 (API 26), but the app supports 7.0 (API 24). On those phones every weight probably looks the same.
- B1–B5, B7, B9, B10, B12–B14 and B16 check out in the code. B11 is still open (#19).

## 3. New findings
| Id | Sev | Problem |
|---|---|---|
| N-01 | High | Damaged settings file → crash loop (above) |
| N-02 | Med | Empty first scan → no default pins, ever |
| N-03 | Med | On cold start, Home briefly shows the "no pinned apps" card (`HomeScreen.kt:120`), because the loading state isn't passed in (`MainActivity.kt:434`). The drawer shows "No apps found for ''" while loading (`AppDrawerScreen.kt:203`). |
| N-04 | Med | Cloud backup policy undecided |
| N-05 | Med | Work-profile changes not tracked |
| N-07 | Low/Med | Pressing the keyboard's Search key with an empty search box opens the first app in the list (`AppDrawerScreen.kt:166-170`) |
| N-06 | Low | Every non-personal profile is labelled "Work", including private space and app clones (`InstalledAppsRepository.kt:38`). *(Inference)* Private space likely isn't listed at all: the manifest lacks the needed permission, though the code comment claims it is supported. |
| N-09 | Low | After a read error the settings stream stops, so the screen keeps defaults until the app restarts (`LauncherPreferencesRepository.kt:47-54`) |
| N-10 | Low | Typing never auto-opens an app installed in both profiles (`LauncherViewModel.kt:158-159`) |
| N-11 | Low | Many app installs in a row (e.g. after a restore) each trigger a full reload; no batching (`LauncherViewModel.kt:48-50`, `:125-128`) |
| N-12 | Low | Release build isn't shrunk (`app/build.gradle.kts:49`) but includes the full extended icon library, so the APK is large |
| N-13 | Low | A unit test writes the real settings file and never cleans up (`ExampleRobolectricTest.kt:72-88`) |
| N-15 | Low | Pins orphaned when an app renames its activity |

## 4. Guidance for #19 part 1 (ordered pins)
1. **Don't prune by comparing app lists.** An app can be missing from a scan for harmless reasons:
   - it's disabled, being updated, or on an unmounted SD card;
   - its profile is paused or locked, or a permission error returned an empty list;
   - a restore is in progress (pins come back before the apps reinstall).

   Prune only when Android reports that specific package removed for that specific profile (`onPackageRemoved(pkg, user)`).
2. If list-based pruning stays anyway, it depends on N-05 being fixed first. `loadInstalledApps` must report, per profile, whether it is paused, whether it is unlocked, and whether the scan worked. Then prune only profiles that are available and actually returned results, after the first successful refresh.
3. **Storage format:** one string with ids joined by `\n`. Ids contain `/ . $ @`, but class names can't contain a newline. Keep the old `pinned_app_ids` key; write both keys during the transition, or derive the set from the list.
4. **Migration order:** Home is currently sorted by app name, but the settings-store migration can't see app names, so sorting there would visibly reorder Home. Either sort the migrated list by name once apps have loaded, or treat "no order saved" as "old set, alphabetical" and only save an order on the first reorder.
5. Keep the meaning of the "defaults done" flag, and fix N-01 and N-02 first, so a migration can't trigger the crash loop.
6. Build the Home list by looking each saved id up in the app list. Skip ids that aren't found, but don't delete them.
7. Tests to add: migrating from the old set, ids containing `@serial` and `$`, a paused profile keeping its pins, and a restore that happens before the apps are reinstalled.

## 5. Test gaps, CI gaps and the missing doc
- **Tests:** none cover the app-id format (the function is private), a damaged settings file, an empty first scan, or work and paused profiles. There are no screen (UI) tests; the on-device test folder only holds a placeholder.
- **CI** (`.github/workflows/build.yml:19`):
  - doesn't build or lint the release variant, and runs no emulator tests;
  - has no limit on duplicate runs and no restricted token permissions;
  - pins actions by tag only;
  - the offline check only looks for the INTERNET permission.
- **`review-and-pr-plan.md` doesn't exist** in the repo.

## 6. Daily-driver UX and what to cut
**Friction:**
- A permanent "Tip: Long-press any app…" line on Home (`HomeScreen.kt:198`).
- Long-pressing empty space opens Settings, and nothing hints at it.
- Every drawer row has a pin button; accidental pins are likely and there's no undo.
- Auto-open is on by default.
- The "Opening X" message is redundant.
- There's no way to pause work apps.
- The empty-state flash on start (N-03).

**Cut for the first public release:** #20, #21, #22, #26, and renaming (keep hide in #19 part 2). Shrink #23 to deciding the cloud backup setting and documenting it in the privacy note.

## 7. Recommended order
1. **Hardening:** N-01, N-02, N-03, N-07, and the dark-mode start background (B15), with tests.
2. **Work-profile tracking (N-05):** needed before #19 can prune safely.
3. **#19 part 1:** ordered pins.
4. **Backup policy (N-04):** #23 in its shrunk form.
5. **#19 part 2:** hide.
6. **#24:** release hardening.
7. **#25:** publish, once the licence is chosen.
8. **Optional:** #22, #20, #21, #26.
