package app.serene.launcher.ui.screens

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.serene.launcher.R
import app.serene.launcher.model.AppItem
import app.serene.launcher.model.SwipeSide
import app.serene.launcher.model.SwipeTarget
import app.serene.launcher.model.ThemeMode

@Composable
fun SettingsScreen(
    themeMode: ThemeMode,
    showAppIcons: Boolean,
    autoLaunchSingleMatch: Boolean,
    openKeyboardOnDrawer: Boolean,
    onThemeModeChange: (ThemeMode) -> Unit,
    onShowAppIconsChange: (Boolean) -> Unit,
    onAutoLaunchSingleMatchChange: (Boolean) -> Unit,
    onOpenKeyboardOnDrawerChange: (Boolean) -> Unit,
    onClose: () -> Unit,
    hiddenApps: List<AppItem> = emptyList(),
    onUnhideApp: (AppItem) -> Unit = {},
    swipeDownForNotifications: Boolean = true,
    onSwipeDownForNotificationsChange: (Boolean) -> Unit = {},
    doubleTapToLock: Boolean = false,
    onDoubleTapToLockChange: (Boolean) -> Unit = {},
    isLockServiceOn: Boolean = false,
    isLockSupported: Boolean = true,
    onOpenAccessibilitySettings: () -> Unit = {},
    swipeLeft: SwipeTarget? = null,
    swipeRight: SwipeTarget? = null,
    /** The apps a swipe can open, for the picker. */
    pickableApps: List<AppItem> = emptyList(),
    onSwipeAppChange: (SwipeSide, AppItem?) -> Unit = { _, _ -> },
    showWallpaper: Boolean = false,
    onShowWallpaperChange: (Boolean) -> Unit = {},
    useWallpaperColours: Boolean = false,
    onUseWallpaperColoursChange: (Boolean) -> Unit = {},
    /** Wallpaper colours need Android 12; the switch is hidden on older phones. */
    supportsWallpaperColours: Boolean = true,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onClose)
    var explainLockService by rememberSaveable { mutableStateOf(false) }
    var pickingSwipeSide by rememberSaveable { mutableStateOf<SwipeSide?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            // Over the wallpaper, the scrim behind every screen is enough; a second one would hide it.
            .background(MaterialTheme.colorScheme.background.takeIf { it.alpha == 1f } ?: Color.Transparent)
            .testTag("settings_screen"),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 580.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("settings_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = stringResource(R.string.settings_back_cd),
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Text(
                    text = stringResource(R.string.settings_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.semantics { heading() }
                )
            }

            SectionHeader(R.string.settings_section_appearance)
            Text(
                text = stringResource(R.string.settings_theme),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp)
            )
            Column(modifier = Modifier.selectableGroup()) {
                ThemeOption(ThemeMode.SYSTEM, R.string.theme_system, themeMode, onThemeModeChange)
                ThemeOption(ThemeMode.LIGHT, R.string.theme_light, themeMode, onThemeModeChange)
                ThemeOption(ThemeMode.DARK, R.string.theme_dark, themeMode, onThemeModeChange)
            }
            SwitchRow(
                titleRes = R.string.settings_show_icons,
                summaryRes = R.string.settings_show_icons_summary,
                checked = showAppIcons,
                onCheckedChange = onShowAppIconsChange,
                testTag = "settings_show_icons"
            )
            SwitchRow(
                titleRes = R.string.settings_show_wallpaper,
                summaryRes = R.string.settings_show_wallpaper_summary,
                checked = showWallpaper,
                onCheckedChange = onShowWallpaperChange,
                testTag = "settings_show_wallpaper"
            )
            if (supportsWallpaperColours) {
                SwitchRow(
                    titleRes = R.string.settings_wallpaper_colours,
                    summaryRes = R.string.settings_wallpaper_colours_summary,
                    checked = useWallpaperColours,
                    onCheckedChange = onUseWallpaperColoursChange,
                    testTag = "settings_wallpaper_colours"
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            SectionHeader(R.string.settings_section_drawer)
            SwitchRow(
                titleRes = R.string.settings_auto_open,
                summaryRes = R.string.settings_auto_open_summary,
                checked = autoLaunchSingleMatch,
                onCheckedChange = onAutoLaunchSingleMatchChange,
                testTag = "settings_auto_open"
            )
            SwitchRow(
                titleRes = R.string.settings_open_keyboard,
                summaryRes = null,
                checked = openKeyboardOnDrawer,
                onCheckedChange = onOpenKeyboardOnDrawerChange,
                testTag = "settings_open_keyboard"
            )

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            SectionHeader(R.string.settings_section_gestures)
            SwitchRow(
                titleRes = R.string.settings_swipe_down,
                summaryRes = null,
                checked = swipeDownForNotifications,
                onCheckedChange = onSwipeDownForNotificationsChange,
                testTag = "settings_swipe_down"
            )
            SwitchRow(
                titleRes = R.string.settings_double_tap_lock,
                summaryRes = when {
                    !isLockSupported -> R.string.settings_double_tap_lock_old_android
                    doubleTapToLock && !isLockServiceOn -> R.string.settings_double_tap_lock_needs_service
                    else -> R.string.settings_double_tap_lock_summary
                },
                checked = doubleTapToLock,
                onCheckedChange = { enabled ->
                    onDoubleTapToLockChange(enabled)
                    // Explain the accessibility service before sending anyone to turn it on.
                    if (enabled && isLockSupported && !isLockServiceOn) explainLockService = true
                },
                testTag = "settings_double_tap_lock"
            )
            SwipeAppRow(R.string.settings_swipe_left, swipeLeft, "settings_swipe_left") {
                pickingSwipeSide = SwipeSide.LEFT
            }
            SwipeAppRow(R.string.settings_swipe_right, swipeRight, "settings_swipe_right") {
                pickingSwipeSide = SwipeSide.RIGHT
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            SectionHeader(R.string.settings_section_hidden)
            if (hiddenApps.isEmpty()) {
                Text(
                    text = stringResource(R.string.settings_hidden_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }
            hiddenApps.forEach { app -> HiddenAppRow(app, onUnhide = { onUnhideApp(app) }) }
        }
    }

    pickingSwipeSide?.let { side ->
        SwipeAppPicker(
            titleRes = if (side == SwipeSide.LEFT) R.string.settings_swipe_left else R.string.settings_swipe_right,
            apps = pickableApps,
            onPick = { app ->
                pickingSwipeSide = null
                onSwipeAppChange(side, app)
            },
            onDismiss = { pickingSwipeSide = null }
        )
    }

    if (explainLockService) {
        AlertDialog(
            onDismissRequest = { explainLockService = false },
            title = { Text(stringResource(R.string.lock_service_title)) },
            text = { Text(stringResource(R.string.lock_service_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        explainLockService = false
                        onOpenAccessibilitySettings()
                    },
                    modifier = Modifier.testTag("lock_service_open_settings")
                ) { Text(stringResource(R.string.lock_service_open_settings)) }
            },
            dismissButton = {
                TextButton(onClick = { explainLockService = false }) {
                    Text(stringResource(R.string.lock_service_not_now))
                }
            }
        )
    }
}

@Composable
private fun HiddenAppRow(app: AppItem, onUnhide: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 4.dp)
            .testTag("settings_hidden_${app.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = app.label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f)
        )
        val unhideDescription = stringResource(R.string.settings_unhide_cd, app.label)
        TextButton(
            onClick = onUnhide,
            modifier = Modifier.semantics { contentDescription = unhideDescription }
        ) {
            Text(stringResource(R.string.settings_unhide))
        }
    }
}

@Composable
private fun SectionHeader(@StringRes titleRes: Int) {
    Text(
        text = stringResource(titleRes),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .padding(start = 16.dp, top = 20.dp, bottom = 4.dp)
            .semantics { heading() }
    )
}

@Composable
private fun ThemeOption(
    mode: ThemeMode,
    @StringRes labelRes: Int,
    selected: ThemeMode,
    onSelect: (ThemeMode) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = mode == selected, role = Role.RadioButton, onClick = { onSelect(mode) })
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .testTag("settings_theme_${mode.name.lowercase()}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = mode == selected, onClick = null, modifier = Modifier.padding(8.dp))
        Text(
            text = stringResource(labelRes),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
private fun SwitchRow(
    @StringRes titleRes: Int,
    @StringRes summaryRes: Int?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(titleRes),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            if (summaryRes != null) {
                Text(
                    text = stringResource(summaryRes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}

/** A Settings row naming the app a swipe opens, or "Nothing". Tapping it opens the picker. */
@Composable
private fun SwipeAppRow(@StringRes titleRes: Int, target: SwipeTarget?, testTag: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag(testTag)
    ) {
        Text(
            text = stringResource(titleRes),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = when {
                target == null -> stringResource(R.string.settings_swipe_nothing)
                target.app == null -> stringResource(R.string.settings_swipe_app_missing)
                else -> target.app.label
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Lets the user choose the app a swipe opens, with "Nothing" first to turn the swipe off. */
@Composable
private fun SwipeAppPicker(
    @StringRes titleRes: Int,
    apps: List<AppItem>,
    onPick: (AppItem?) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(titleRes)) },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
                item(key = "none") {
                    PickerRow(stringResource(R.string.settings_swipe_nothing), "swipe_pick_none") { onPick(null) }
                }
                items(apps, key = { it.id }) { app ->
                    PickerRow(app.label, "swipe_pick_${app.id}") { onPick(app) }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.rename_cancel)) }
        }
    )
}

@Composable
private fun PickerRow(text: String, testTag: String, onClick: () -> Unit) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp)
            .testTag(testTag)
    )
}
