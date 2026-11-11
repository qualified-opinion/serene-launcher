package app.serene.launcher.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.serene.launcher.R
import app.serene.launcher.model.AppItem
import app.serene.launcher.model.SwipeSide
import app.serene.launcher.ui.components.MinimalClockDateHeader
import app.serene.launcher.ui.components.PinnedAppRow

/** How far a swipe up (app drawer) or down (notifications) on Home must travel to count. */
private val OpenDrawerSwipeThreshold = 48.dp

@Composable
fun HomeScreen(
    pinnedApps: List<AppItem>,
    isLoading: Boolean,
    totalInstalledCount: Int,
    showAppIcons: Boolean,
    onAppClick: (AppItem) -> Unit,
    onAppLongPress: (AppItem) -> Unit,
    onOpenDrawer: () -> Unit,
    onOpenSettings: () -> Unit,
    onClockClick: () -> Unit,
    onDateClick: () -> Unit,
    modifier: Modifier = Modifier,
    isReorderingPins: Boolean = false,
    onMovePin: (AppItem, Int) -> Unit = { _, _ -> },
    onDoneReordering: () -> Unit = {},
    onSwipeDown: () -> Unit = {},
    /** A swipe left or right; the app it opens is chosen in Settings. */
    onSwipeSideways: (SwipeSide) -> Unit = {},
    /** Null when double-tap is off, so single taps on Home aren't held back waiting for a second. */
    onDoubleTap: (() -> Unit)? = null
) {
    // Home is the bottom of the back stack: swallow Back so the system doesn't
    // finish the launcher and immediately restart it.
    BackHandler { onDoneReordering() }

    var cumulativeUpwardDrag by remember { mutableFloatStateOf(0f) }
    var cumulativeSidewaysDrag by remember { mutableFloatStateOf(0f) }
    val openDrawerThresholdPx = with(LocalDensity.current) { OpenDrawerSwipeThreshold.toPx() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = { cumulativeUpwardDrag = 0f },
                    onDragEnd = {
                        if (cumulativeUpwardDrag < -openDrawerThresholdPx) {
                            onOpenDrawer()
                        } else if (cumulativeUpwardDrag > openDrawerThresholdPx) {
                            onSwipeDown()
                        }
                        cumulativeUpwardDrag = 0f
                    },
                    onDragCancel = { cumulativeUpwardDrag = 0f },
                    onVerticalDrag = { _, dragAmount ->
                        cumulativeUpwardDrag += dragAmount
                    }
                )
            }
            // Sideways swipes; a drag only counts once it clearly goes left or right, not up or down.
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { cumulativeSidewaysDrag = 0f },
                    onDragEnd = {
                        when {
                            cumulativeSidewaysDrag < -openDrawerThresholdPx -> onSwipeSideways(SwipeSide.LEFT)
                            cumulativeSidewaysDrag > openDrawerThresholdPx -> onSwipeSideways(SwipeSide.RIGHT)
                        }
                        cumulativeSidewaysDrag = 0f
                    },
                    onDragCancel = { cumulativeSidewaysDrag = 0f },
                    onHorizontalDrag = { _, dragAmount -> cumulativeSidewaysDrag += dragAmount }
                )
            }
            // Long-pressing empty space opens Settings; app rows handle their own long-press.
            .pointerInput(onDoubleTap) {
                detectTapGestures(
                    onLongPress = { onOpenSettings() },
                    onDoubleTap = onDoubleTap?.let { action -> { _: Offset -> action() } }
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
                    onOpenSettings = onOpenSettings,
                    onClockClick = onClockClick,
                    onDateClick = onDateClick
                )

                Spacer(modifier = Modifier.height(36.dp))

                if (pinnedApps.isEmpty() && !isLoading) {
                    EmptyPinnedAppsCard(
                        onOpenDrawer = onOpenDrawer,
                        modifier = Modifier.padding(top = 24.dp)
                    )
                } else if (pinnedApps.isNotEmpty()) {
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                PinnedAppRow(
                                    app = app, showIcon = showAppIcons,
                                    onClick = { onAppClick(app) }, onLongClick = { onAppLongPress(app) },
                                    modifier = Modifier.weight(1f)
                                )
                                if (isReorderingPins) {
                                    listOf(-1, 1).forEach { offset ->
                                        IconButton(
                                            onClick = { onMovePin(app, offset) },
                                            enabled = if (offset < 0) app != pinnedApps.first() else app != pinnedApps.last()
                                        ) {
                                            Icon(
                                                if (offset < 0) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                                                stringResource(if (offset < 0) R.string.move_pin_up else R.string.move_pin_down, app.label)
                                            )
                                        }
                                    }
                                }
                            }
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
                if (isReorderingPins) {
                    val done = stringResource(R.string.reorder_done)
                    TextButton(onClick = onDoneReordering, modifier = Modifier.semantics { contentDescription = done }) {
                        Text(done)
                    }
                }
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
                                pluralStringResource(R.plurals.all_apps_count, totalInstalledCount, totalInstalledCount)
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
                    // Faded on plain colour; full strength over the wallpaper, where faded text is hard to read.
                    color = MaterialTheme.colorScheme.onSurfaceVariant.let { grey ->
                        if (MaterialTheme.colorScheme.background.alpha < 1f) grey else grey.copy(alpha = 0.65f)
                    }
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
