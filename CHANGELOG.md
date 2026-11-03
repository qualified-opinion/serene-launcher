# Changelog

All notable changes to Serene Launcher. Versions follow the release tags (`v1.2.3`).

When you release, rename **Unreleased** to the version and date, for example `## [0.2.0] - 2026-11-01`. The Release workflow uses that section as the release notes. If there's no section for the version, GitHub writes notes from the merged PRs instead.

## [Unreleased]

### Added
- Swipe left or right on Home to open an app you choose in Settings → Gestures (nothing by default).
- Swipe down on Home to open notifications (on by default; turn it off in Settings).
- Double-tap empty space on Home to lock the screen (Android 9+, off by default). It uses an optional accessibility service that Serene explains before you turn it on; the service only presses the lock button and can't read your screen.
- Press and hold an app to see its own shortcuts, such as "New message", and tap one to open it. This needs Serene to be your default home app and Android 7.1 or later.
- Hide apps from the app list, and unhide them in Settings.
- Rename apps; the new name is used for sorting and search, and you can go back to the original name.
- Reorder pinned apps on Home with up and down buttons.
- A Settings screen: follow the phone's light or dark mode, show app icons, auto-open a single match, and open the keyboard with the app list.
- Work-profile apps, marked "Work", alongside personal apps.
- Private space apps (Android 15+) when Serene is the default home app, marked "Private". They stay out of sight while private space is locked.
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
- Pausing work apps no longer makes work apps and their pins vanish. They stay, marked "Work, paused", and come back to normal when work apps are turned on.
- The launcher notices a work profile being added, removed, paused, resumed or unlocked, without a restart.
- If Android won't let the launcher read a profile, its apps are no longer treated as uninstalled.
- On Android 15 and later, clone profiles and other kinds of profile are no longer all marked "Work".
- Typing the name of an app installed in both personal and work now opens the personal copy by itself.
- Work-app pins, hidden work apps and work-app names brought over from an old phone now find the new phone's work profile.

### Privacy
- No internet permission, no accounts, no tracking. See [PRIVACY.md](PRIVACY.md).
