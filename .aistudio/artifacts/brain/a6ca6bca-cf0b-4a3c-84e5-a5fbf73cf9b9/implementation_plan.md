# Serene Launcher — Minimalist Offline Android Home Screen

A calm, distraction-free Android home screen launcher inspired by Olauncher and Niagara Launcher. Designed to work 100% offline with zero accounts, ads, or visual clutter, it centers your daily phone usage around a focused vertical list of pinned apps and an effortless, keyboard-and-thumb-friendly app drawer.

## User Review & Critical Decisions

> [!IMPORTANT]
> The following design and functional choices have been incorporated based on your selections:

- **Confirmed Decision 1 — Warm Paper Light Theme Default + Dark Toggle**: The launcher defaults to a warm, tactile paper-inspired light theme (`#FBF9F5` background, deep charcoal `#1C1B18` ink, and a soft terracotta/amber accent `#C86D51`) with an instant one-tap toggle to a deep OLED dark theme (`#0D0D0D` background, `#EAE6DF` warm off-white text).
- **Confirmed Decision 2 — Pure Text by Default + Small Icon Toggle**: Both the home screen and app drawer display clean typographic app names by default, with a quick header preference toggle to reveal subtle, desaturated 20dp monochrome-tinted app icons alongside app names.
- **Confirmed Decision 3 — Fast Drawer Navigation (Alphabet Scrubber & Auto-Launch)**: The app drawer includes a vertical A–Z index sidebar for instant thumb scrubbing across installed apps, plus an optional/enabled-by-default **Auto-Launch Single Match** behavior that automatically opens an app when your search query narrows the list down to a single result.

---

## 1. Overview & Core Concept

- **What It Does**: Provides a minimalist home screen displaying the user's pinned essential apps alongside an unobtrusive live clock and date header, paired with a full-screen offline app drawer featuring instant search, A–Z thumb scrubbing, and long-press pinning.
- **Target Audience / Persona**: Mindful smartphone users looking for a quiet, intentional home screen replacement without widgets, notification badges, news feeds, or network connections.
- **Key Value**: Eliminates visual noise and doomscrolling triggers while making deliberate app launching faster than traditional icon grids.

---

## 2. User Experience & Visual Design

### Key User Flows
1. **Home Screen (Daily Launchpad)**:
   - Displays a quiet top header with the live time (`HH:mm`), formatted date (`EEE, MMM d`), and subtle icon toggles for Theme (Warm Paper / Dark), Small Icons (On / Off), and App Drawer access.
   - Centers the user's **Pinned Apps** in a vertically aligned, generously spaced typographic list.
   - **Empty Pinned State**: If all apps are unpinned, displays a calm editorial prompt (*"No pinned apps yet"*) with a direct button to open the App Drawer and pin favorites.
   - **Opening the Drawer**: Users can swipe up anywhere on the home screen or tap the subtle bottom **"All apps"** pill indicator to transition smoothly into the App Drawer.
2. **Pinning & Unpinning Apps (Long-Press Interaction)**:
   - Long-pressing any app row (either on the Home Screen or inside the App Drawer) triggers haptic feedback and reveals a clean contextual action sheet/pill for that app: **Pin to Home** / **Unpin from Home** (with a clear pin indicator badge next to already-pinned apps in the drawer) and **App Info** (system settings intent).
3. **App Drawer (Browse, Scrub & Instant Search)**:
   - Opens with a clean search bar at the top, a toggle pill for **Auto-open single match**, and a full alphabetical list of all launchable apps on the device.
   - **Instant Search & Auto-Launch**: Typing filters installed apps in real time with zero latency. When `Auto-launch` is active and typing narrows the list to exactly 1 matching app, the launcher automatically opens that app and resets the drawer. If no apps match, a clear *"No apps found"* empty state is shown with a one-tap *"Clear search"* action.
   - **Alphabet Index Sidebar**: A vertical `A–Z` rail along the right edge lets users tap or drag their thumb to jump smoothly to any letter section, showing a subtle floating letter indicator while scrubbing.

### Visual Identity & Theme
- **Aesthetic Direction**: Editorial Utilitarian Minimalism — inspired by Japanese stationery, E-Ink readers, Olauncher's typographic purity, and Niagara's ergonomic thumb reach.
- **Color Palette & Mood**:
  - *Warm Paper Light Mode (Default)*:
    - `--background`: `#FBF9F5` (Warm Alabaster Paper)
    - `--surface`: `#F2EFE9` (Soft Stone Surface for search bar & pills)
    - `--primary` / Primary Ink: `#1C1B18` (Deep Warm Charcoal)
    - `--secondary` / Muted Ink: `#6E6A63` (Warm Slate for clock date, section headers, and secondary labels)
    - `--accent`: `#C4654B` (Soft Terracotta Accent for pin indicator, active scrubber letter, and toggles)
  - *Calm OLED Dark Mode (Toggle)*:
    - `--background`: `#0E0E0E` (True Matte Obsidian)
    - `--surface`: `#1A1918` (Elevated Warm Graphite)
    - `--primary`: `#ECE8E1` (Soft Parchment White)
    - `--secondary`: `#8E8980` (Muted Warm Gray)
    - `--accent`: `#D97F66` (Warm Coral Clay)
- **Typography & Hierarchy**:
  - Uses locally bundled Google Fonts (**Plus Jakarta Sans** for clean, modern geometric app titles and **DM Sans** for subtle metadata/index labels).
  - Home Screen Pinned Apps: `24sp` Medium weight with generous `14dp` vertical touch padding (`56dp+` total row height) for effortless thumb tapping.
  - Clock & Date: Unobtrusive `28sp` Light time display paired with `14sp` Medium uppercase letter-spaced date.
- **Interactive Feedback & Motion**:
  - Subtle vertical slide + fade transitions (`spring` physics under 250ms) between the Home Screen and App Drawer.
  - Tactile ripple and haptic confirmation (`HapticFeedbackType.LongPress`) when pinning or unpinning an app.
  - System Back button or downward swipe cleanly closes the App Drawer and returns to the Home Screen.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Real Device App Discovery with Graceful Fallback Catalog**
  - *Chosen Approach*: Query real installed launchable activities on the phone via Android's `PackageManager` (`ACTION_MAIN` + `CATEGORY_LAUNCHER` via `<queries>` in the manifest—requiring zero dangerous permissions). To ensure a rich, realistic experience even on a barebones emulator with very few system apps installed, if fewer than 8 launchable apps are detected, merge in a curated set of realistic system-style fallback entries that launch available system intents (Settings, Clock, Calculator, Contacts, Browser, Dialer, Messaging, etc.) or display a clean status message if unavailable on the container.
  - *Why*: Guarantees the alphabet scrubber, instant search, and pinning workflows feel responsive and realistic immediately upon launch while working as a genuine Android launcher for every installed app.
- **Decision 2: Strictly Offline Local Persistence via Jetpack DataStore**
  - *Chosen Approach*: Persist pinned package identifiers, theme selection (Warm Light vs. Dark), icon visibility toggle, and auto-launch search preference locally using Jetpack DataStore Preferences (`androidx.datastore.preferences`).
  - *Why*: Completely offline, asynchronous, corruption-resistant, and survives app restarts and phone reboots without overhead. Zero network libraries or internet permissions are included.
- **Decision 3: Optional Monochrome Icon Rendering**
  - *Chosen Approach*: When the user enables the "Small Icons" toggle, render each app's actual drawable icon at `20dp` with a grayscale/monochrome `ColorMatrix` filter and subtle opacity so loud third-party app icons never break the calm monochrome palette.

---

## 4. Technical Architecture & Data Strategy

### Architecture & Component Diagram

```
┌───────────────────────────────────────────────────────────────────────────┐
│                         MainActivity (Edge-to-Edge)                       │
│                  LauncherRoot (Theme + BackHandler + Gesture)             │
└─────────────────────────────────────┬─────────────────────────────────────┘
                                      │ observes StateFlow<LauncherUiState>
                                      ▼
┌───────────────────────────────────────────────────────────────────────────┐
│                             LauncherViewModel                             │
│  • Combines installed apps + persisted preferences                        │
│  • Filters search query & triggers single-match auto-launch               │
│  • Handles pin/unpin toggling, theme switching, & icon visibility         │
└──────────────────┬─────────────────────────────────────┬──────────────────┘
                   │                                     │
                   ▼                                     ▼
┌─────────────────────────────────────┐ ┌───────────────────────────────────┐
│       InstalledAppsRepository       │ │    LauncherPreferencesRepository  │
│  • Queries PackageManager for       │ │  • Jetpack DataStore (Offline)    │
│    MAIN/LAUNCHER activities         │ │  • Persists:                      │
│  • Loads app labels & monochrome    │ │    - pinnedAppIds: Set<String>    │
│    drawable icons                   │ │    - isDarkTheme: Boolean         │
│  • Launches intents safely          │ │    - showAppIcons: Boolean        │
│                                     │ │    - autoLaunchSingle: Boolean    │
└─────────────────────────────────────┘ └───────────────────────────────────┘
                   │
                   ▼
┌───────────────────────────────────────────────────────────────────────────┐
│                        Jetpack Compose UI Screens                         │
├─────────────────────────────────────┬─────────────────────────────────────┤
│             HomeScreen              │            AppDrawerScreen          │
│  • MinimalClockDateHeader           │  • SearchBar + Auto-Launch Toggle   │
│  • QuickSettingsRow (Theme / Icons) │  • LazyColumn of Installed Apps     │
│  • PinnedAppsList (Tap / LongPress) │  • AlphabetIndexSidebar (A–Z Scrub) │
│  • EmptyPinnedState                 │  • EmptySearchState ("No apps...")  │
│  • Swipe-Up / "All apps" Trigger    │  • Pin/Unpin Contextual Action Bar  │
└─────────────────────────────────────┴─────────────────────────────────────┘
```

### Data Model & State
- **`AppItem`**:
  - `id`: Unique identifier (`packageName/activityName`)
  - `label`: User-facing app name (sorted alphabetically, case-insensitive)
  - `packageName`: Android package name for intent launching
  - `icon`: Optional `Drawable?` loaded from `PackageManager` (rendered with a monochrome saturation `0f` color filter when `showAppIcons` is enabled)
  - `isPinned`: Derived boolean indicating whether `id` is in `pinnedAppIds`
- **`LauncherUiState`**:
  - `pinnedApps`: `List<AppItem>` (displayed on the Home Screen)
  - `allApps`: `List<AppItem>` (full sorted catalog)
  - `filteredDrawerApps`: `List<AppItem>` (filtered by `searchQuery`)
  - `alphabetIndexMap`: `Map<Char, Int>` (maps `A`–`Z` and `#` to the first matching index in `filteredDrawerApps`)
  - `searchQuery`: `String`
  - `isDrawerOpen`: `Boolean`
  - `isDarkTheme`: `Boolean` (defaults to `false` for Warm Paper Light Mode)
  - `showAppIcons`: `Boolean` (defaults to `false` for Pure Text Mode)
  - `autoLaunchSingleMatch`: `Boolean` (defaults to `true`)
  - `selectedAppForMenu`: `AppItem?` (controls the clean long-press Pin/Unpin bottom sheet or inline action dialog)
  - `statusMessage`: `String?` (subtle transient feedback for pin/unpin or launch events)

### Interactive Component & State Mapping
- **Tap on App Row (`HomeScreen` or `AppDrawerScreen`)**: Invokes `viewModel.launchApp(app)` which starts the package's launch intent (or fallback system intent) and closes the drawer if open.
- **Long-Press on App Row (`combinedClickable`)**: Triggers haptic feedback and opens a minimal confirmation/action sheet (`AppActionSheet`) allowing immediate **Pin to Home** / **Unpin from Home** toggle, or direct one-step pin toggle with an undo/status pill.
- **Swipe-Up Gesture / Bottom Pill on Home Screen**: Sets `isDrawerOpen = true` with smooth vertical transition.
- **Search Input Change**: Updates `searchQuery`; if `autoLaunchSingleMatch` is enabled, `searchQuery` is non-empty, and `filteredDrawerApps.size == 1` (while user is typing forward), automatically launches that single matching app and clears the search query.
- **Alphabet Sidebar Drag/Tap**: Detects vertical pointer position across letters `A–Z`, highlights the active letter in a subtle floating badge, and scrolls the `LazyListState` directly to the corresponding app index.
