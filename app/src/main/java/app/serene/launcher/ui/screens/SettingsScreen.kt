package app.serene.launcher.ui.screens

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.serene.launcher.R
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
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onClose)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
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
