# B2: ordered pins (outline, full pack after B1 merges)

- **Branch:** `feat/ordered-pins` from `main` after B1
- **Engine:** Codex on high builds it. Claude Code reviews it.
- **Items:** #19 part 1 (B11), N-15

## Goal
Pins keep the order the user sets, across restarts and updates, and no pin is ever lost by mistake.

## Scope
1. **Storage.** Add a new string key holding the ids joined by `\n` in order. Keep the old `pinned_app_ids` set in sync while it's still read.
2. **Migration.** If no order is saved, treat the old set as alphabetical by app name. Save an order only on the first reorder or the first pin change, so Home never visibly reorders on update.
3. **Reorder.** Add a "Reorder" mode on Home with up and down buttons. Drag-to-reorder is cut.
4. **Pruning.** Remove a pin only when Android reports that package removed for that profile (`LauncherApps.Callback.onPackageRemoved(pkg, user)`). Never prune by comparing scans. Ids missing from the app list are skipped on Home but kept in storage.
5. App id formats are unchanged.

## Acceptance criteria
- Tests cover:
  - migrating from the old set;
  - ids containing `@serial` and `$`;
  - order surviving a restart;
  - a removed event pruning exactly one pin;
  - an empty or paused-profile scan pruning nothing;
  - a restore before the apps reinstall keeping the pins.
- Jan's phone check: install over the B1 build and confirm all pins are there. Reorder two pins, restart the phone, and the order stays. Uninstall a pinned app and its pin goes. If you have a work profile, pause and resume it, and the work pins come back.
