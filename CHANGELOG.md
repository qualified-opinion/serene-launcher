# Changelog

All notable changes to Serene Launcher. Versions follow the release tags (`v1.2.3`).

When you release, rename **Unreleased** to the version and date, for example `## [0.2.0] - 2026-11-01`. The Release workflow uses that section as the release notes. If there's no section for the version, GitHub writes notes from the merged PRs instead.

## [Unreleased]

### Added
- Choose a background colour: Paper (as before), Sand, Sage, Mist, Lavender or Blush, each with a light and a dark version (Settings → Appearance).
- With the wallpaper shown, choose how much to soften it: a little, some or a lot. "A lot" is the look from 0.1.0; the new default, "some", shows more of the wallpaper, and text gets a soft glow so it stays readable.
- A "Change wallpaper" shortcut in Settings that opens the phone's own wallpaper picker.

### Fixed
- On a phone in a language other than English, the apps pinned to Home on first start are now the phone's own dialer, messages, camera, settings, clock and calendar, found by what they do, instead of four apps from the top of the list.
- Pressing Home while renaming an app now closes the Rename box.
- The confirmation pill (for example "Pinned Maps to Home") keeps its text while it fades out instead of shrinking to a dot.
- The window behind the app now matches the launcher's own light or dark choice and colour, so no strip of a different colour shows when the keyboard opens.
- The "Press and hold" hint on Home is no longer faded over the wallpaper, where it was hard to read.

## [0.1.0] - 2026-11-04

### Added
- Export your pins, names, hidden apps and options to a file, and import them again, for example on a new phone (Settings → Backup). No network involved.
- Show your wallpaper behind Home and the app list, with a soft layer that keeps text readable (Settings → Appearance, off by default).
- Use your wallpaper's colours (Material You) on Android 12 and later (Settings → Appearance, off by default).
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
- Tapping a paused work app asks Android to turn work apps back on (Android 9+, when Serene is the default home app), instead of only saying they're off.
- The launcher notices a work profile being added, removed, paused, resumed or unlocked, without a restart.
- If Android won't let the launcher read a profile, its apps are no longer treated as uninstalled.
- On Android 15 and later, clone profiles and other kinds of profile are no longer all marked "Work".
- Typing the name of an app installed in both personal and work now opens the personal copy by itself.
- Work-app pins, hidden work apps and work-app names brought over from an old phone now find the new phone's work profile.

### Privacy
- No internet permission, no accounts, no tracking. See [PRIVACY.md](PRIVACY.md).
