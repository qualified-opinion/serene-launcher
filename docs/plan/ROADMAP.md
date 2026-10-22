# Serene Launcher roadmap

Updated 2026-10-22 against `main` `82082df`. This file replaces [issue #27](https://github.com/qualified-opinion/serene-launcher/issues/27) as the source of truth. Issue #27 stays open as a pointer.

## What this is
A minimalist, fully offline Android home screen. It has pinned apps on Home and a searchable A–Z drawer, including work apps. It's distributed as sideloaded APKs on GitHub Releases only.

## Status table
| Batch | Branch | Scope | Status | Pack |
|---|---|---|---|---|
| Phase 0–1 | various | B1–B10, B12–B16 (PR 1–9 of issue #27) | Merged #1, #2, #6, #12–#17 | - |
| #18 Settings | `feat/settings-screen` | Settings screen, follow-system theme | Merged #29, `82082df` | - |
| B0 Docs | `docs/plan-roadmap` | This roadmap, review, packs, AGENTS.md, CLAUDE.md | In review | - |
| B1 Hardening | `fix/settings-hardening` | N-01, N-02, N-03, N-07, B15 | Pack ready | [batch1](handoffs/batch1-settings-hardening.md) |
| B2 Ordered pins | `feat/ordered-pins` | #19 part 1 (B11, N-15) | Outline | [batch2](handoffs/batch2-ordered-pins.md) |
| B3 Backup policy | `fix/backup-policy` | N-04, #23 shrunk | Outline | - |
| B4 Hide apps | `feat/hide-apps` | #19 part 2, hide only | Outline | - |
| B5 Release | `ci/release-workflow` | #24 | Outline | - |
| B6 Publish | `docs/publish` | #25, needs the licence decision | Outline | - |

## Item index
Old ids B1–B16 come from issue #27. New ids N-01 onward come from [Kiln review 1](reviews/kiln-review-1.md).

| Id | Sev | Problem | Where it goes |
|---|---|---|---|
| B6 | Med | Partly fixed: work-profile pause and resume aren't tracked (see N-05) | Later |
| B8 | Low | Possibly only partly fixed: font weights may look the same on Android 7 (API 24–25). Unverified | Later |
| B11 | Med | Pins are always alphabetical and never cleaned up | B2 |
| B15 | Low | Still partly broken: the start-up window is light in dark mode | B1 |
| N-01 | High | A damaged settings file makes the launcher crash on every start | B1 |
| N-02 | Med | An empty first app scan marks default pins as done with no pins | B1 |
| N-03 | Med | Home flashes "no pinned apps" and the drawer shows "No apps found" while loading | B1 |
| N-04 | Med | Android cloud backup is on with unedited sample rules | B3 |
| N-05 | Med | Work-profile add, remove, pause and resume aren't tracked. A permission error looks like an empty profile | Later |
| N-06 | Low | Every non-personal profile is labelled "Work". Private space likely isn't listed | Later |
| N-07 | Low | The keyboard Search key with an empty search box opens the first app | B1 |
| N-09 | Low | After a read error, settings stay at defaults until restart | B1 (with N-01) |
| N-10 | Low | Typing never auto-opens an app installed in both profiles | Later |
| N-11 | Low | Many installs in a row each trigger a full reload | Later |
| N-12 | Low | The release build isn't shrunk and ships the full extended icon library | B5 |
| N-13 | Low | A unit test writes the real settings file and never cleans up | B1 |
| N-15 | Low | A pin disappears from Home when an app renames its activity, but stays stored | B2 (skip, don't delete) |

## Decisions Jan still owes
1. **Licence, MIT or GPL-3.0.** It's needed for B6. Default: none is added until Jan picks.
2. **Android cloud backup, on or off.** Default for B3: off (`allowBackup="false"`), which matches the offline promise. The in-app backup file in #23 stays later.
3. **Order: hardening first, then pins.** Kiln recommends it. Jan's 2026-10-22 go was given for "ordered pins first", so confirm.

## Cut for the first public release (later ideas)
#20 gestures, #21 wallpaper and Material You, #22 app shortcuts, #26 emulator UI tests, renaming apps (part of #19 part 2), a full in-app backup file (#23), and N-05, N-06, N-10, N-11. Revisit after v1.

## How work is run
1. Kiln writes the handoff pack, and Jan says go.
2. Nightshift builds on a feature branch (Codex by default) and opens one PR that updates this status table.
3. Kiln reviews with a different engine (Claude Code for Codex-written PRs).
4. The PR is squash-merged, pinned to the approved head, after green CI and Jan's OK.
5. Jan does the phone checks and publishes releases himself. Agents never tag or release.

## Open verification (phone checks not yet done)
- Nothing from PRs #1–#29 has been run on a physical device by a reviewer. All were checked by CI only.

## First message for an agent starting alone
"Read AGENTS.md and docs/plan/ROADMAP.md in qualified-opinion/serene-launcher. Take the first batch whose status isn't Merged, follow its pack in docs/plan/handoffs/, open one PR that updates the status table, and don't merge."

## Facts for a new owner
- `review-and-pr-plan.md`, which issue #27 mentions, is not in the repo. Issue #27 and `reviews/` are the record.
- Three Dependabot PRs are open (#10 AGP 9.4.1, #11 Robolectric 4.17, #28 Gradle wrapper 9.8.1). They're untouched on purpose. Handle them in one dependency batch after B2.
- The code map: `data/` (apps via LauncherApps, icons, DataStore prefs), `domain/AppSearch.kt`, `viewmodel/LauncherViewModel.kt`, `ui/screens/`, `ui/components/`. Tests are in `app/src/test`.
