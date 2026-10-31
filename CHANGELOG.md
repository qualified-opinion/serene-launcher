# Changelog

All notable changes to Serene Launcher. Versions follow the release tags (`v1.2.3`).

When you release, rename **Unreleased** to the version and date, for example `## [0.2.0] - 2026-11-01`. The Release workflow uses that section as the release notes. If there's no section for the version, GitHub writes notes from the merged PRs instead.

## [Unreleased]

### Added
- Hide apps from the app list, and unhide them in Settings.
- Rename apps; the new name is used for sorting and search, and you can go back to the original name.
- Reorder pinned apps on Home with up and down buttons.
- A Settings screen: follow the phone's light or dark mode, show app icons, auto-open a single match, and open the keyboard with the app list.
- Work-profile apps, marked "Work", alongside personal apps.
- Real app icons, including Android 13+ monochrome icons, loaded as you scroll.
- Search that ignores accents, matches the start of words and initials, and only auto-opens on an exact start of a name.
- Tap the clock for alarms and the date for the calendar. The clock follows the phone's 12/24-hour setting, time zone and language.
- TalkBack labels, translatable text and proper font weights.

### Changed
- Smaller release download: unused code and icons are removed.
- Android cloud backup is off. Phone-to-phone transfer still brings your settings across.

### Fixed
- A damaged settings file no longer makes the launcher crash on every start.
- Home no longer flashes "no pinned apps", and the app list no longer says "No apps found", while apps are loading.
- Pressing the keyboard's Search key with an empty search box no longer opens an app.
- The start-up screen is no longer white in dark mode.
- Default pins are set up again if the first app scan found nothing.
- Pressing Home returns to the home screen from anywhere in the launcher.

### Privacy
- No internet permission, no accounts, no tracking. See [PRIVACY.md](PRIVACY.md).
