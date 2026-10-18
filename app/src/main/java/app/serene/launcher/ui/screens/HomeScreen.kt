package app.serene.launcher.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.serene.launcher.R
import app.serene.launcher.model.AppItem
import app.serene.launcher.ui.components.MinimalClockDateHeader
import app.serene.launcher.ui.components.PinnedAppRow

@Composable
fun HomeScreen(
    pinnedApps: List<AppItem>,
    totalInstalledCount: Int,
    isDarkTheme: Boolean,
    showAppIcons: Boolean,
    onAppClick: (AppItem) -> Unit,
    onAppLongPress: (AppItem) -> Unit,
    onOpenDrawer: () -> Unit,
    onToggleTheme: () -> Unit,
    onToggleIcons: () -> Unit,
    modifier: Modifier = Modifier
) {
    var cumulativeUpwardDrag by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = { cumulativeUpwardDrag = 0f },
                    onDragEnd = {
                        if (cumulativeUpwardDrag < -72f) {
                            onOpenDrawer()
                        }
                        cumulativeUpwardDrag = 0f
                    },
                    onDragCancel = { cumulativeUpwardDrag = 0f },
                    onVerticalDrag = { _, dragAmount ->
                        cumulativeUpwardDrag += dragAmount
                    }
                )
            }
            .testTag("home_screen"),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 560.dp)
                .padding(horizontal = 28.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top area: Unobtrusive clock/date + pinned apps list
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
            ) {
                MinimalClockDateHeader(
                    isDarkTheme = isDarkTheme,
                    showAppIcons = showAppIcons,
                    onToggleTheme = onToggleTheme,
                    onToggleIcons = onToggleIcons
                )

                Spacer(modifier = Modifier.height(36.dp))

                if (pinnedApps.isEmpty()) {
                    EmptyPinnedAppsCard(
                        onOpenDrawer = onOpenDrawer,
                        modifier = Modifier.padding(top = 24.dp)
                    )
                } else {
                    Text(
                        text = stringResource(R.string.pinned_section_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(start = 8.dp, bottom = 10.dp)
                            .testTag("pinned_section_header")
                    )

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("pinned_apps_list"),
                        contentPadding = PaddingValues(vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        items(
                            items = pinnedApps,
                            key = { it.id }
                        ) { app ->
                            PinnedAppRow(
                                app = app,
                                showIcon = showAppIcons,
                                onClick = { onAppClick(app) },
                                onLongClick = { onAppLongPress(app) }
                            )
                        }
                    }
                }
            }

            // Bottom subtle App Drawer trigger (tappable + swipe-up hint)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(onClick = onOpenDrawer)
                        .testTag("open_drawer_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.KeyboardArrowUp,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = if (totalInstalledCount > 0) {
                                "All apps ($totalInstalledCount)"
                            } else {
                                stringResource(R.string.open_app_drawer)
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = stringResource(R.string.long_press_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                )
            }
        }
    }
}

@Composable
private fun EmptyPinnedAppsCard(
    onOpenDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier
            .fillMaxWidth()
            .testTag("empty_pinned_state")
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.PushPin,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = stringResource(R.string.empty_pinned_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(R.string.empty_pinned_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            FilledTonalButton(
                onClick = onOpenDrawer,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                modifier = Modifier.testTag("empty_state_open_drawer_button")
            ) {
                Text(text = stringResource(R.string.open_app_drawer))
            }
        }
    }
}
