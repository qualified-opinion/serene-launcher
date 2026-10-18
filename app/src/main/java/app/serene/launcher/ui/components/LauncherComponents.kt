package app.serene.launcher.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Launch
import androidx.compose.material.icons.automirrored.outlined.Message
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.CloudQueue
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.serene.launcher.R
import app.serene.launcher.model.AppItem
import app.serene.launcher.model.FallbackIconGlyph
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MinimalClockDateHeader(
    isDarkTheme: Boolean,
    showAppIcons: Boolean,
    onToggleTheme: () -> Unit,
    onToggleIcons: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentTimeMillis by remember { mutableStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            currentTimeMillis = System.currentTimeMillis()
            delay(15_000L)
        }
    }

    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val dateFormat = remember { SimpleDateFormat("EEE, MMM d", Locale.getDefault()) }

    val formattedTime = remember(currentTimeMillis) { timeFormat.format(Date(currentTimeMillis)) }
    val formattedDate = remember(currentTimeMillis) {
        dateFormat.format(Date(currentTimeMillis)).uppercase(Locale.getDefault())
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column(
            modifier = Modifier.testTag("clock_date_header")
        ) {
            Text(
                text = formattedTime,
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.testTag("home_clock_text")
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = formattedDate,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag("home_date_text")
            )
        }

        // Minimalist utility toggles (Warm Light / Dark Theme & Pure Text / Small Monochrome Icons)
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onToggleIcons,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("icons_toggle_button")
            ) {
                Icon(
                    imageVector = if (showAppIcons) Icons.Outlined.GridView else Icons.Outlined.TextFields,
                    contentDescription = stringResource(R.string.toggle_icons_cd),
                    tint = if (showAppIcons) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(20.dp)
                )
            }

            IconButton(
                onClick = onToggleTheme,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("theme_toggle_button")
            ) {
                Icon(
                    imageVector = if (isDarkTheme) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                    contentDescription = stringResource(R.string.toggle_theme_cd),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun MonochromeAppIcon(
    app: AppItem,
    modifier: Modifier = Modifier
) {
    val grayscaleMatrix = remember {
        ColorMatrix().apply { setToSaturation(0f) }
    }

    if (app.iconBitmap != null) {
        Image(
            bitmap = app.iconBitmap,
            contentDescription = null,
            colorFilter = ColorFilter.colorMatrix(grayscaleMatrix),
            modifier = modifier
                .size(20.dp)
                .clip(RoundedCornerShape(5.dp))
                .alpha(0.82f)
        )
    } else {
        Icon(
            imageVector = app.fallbackGlyph.toImageVector(),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier.size(19.dp)
        )
    }
}

private fun FallbackIconGlyph.toImageVector(): ImageVector = when (this) {
    FallbackIconGlyph.PHONE -> Icons.Outlined.Call
    FallbackIconGlyph.MESSAGES -> Icons.AutoMirrored.Outlined.Message
    FallbackIconGlyph.BROWSER -> Icons.Outlined.Language
    FallbackIconGlyph.CAMERA -> Icons.Outlined.CameraAlt
    FallbackIconGlyph.CLOCK -> Icons.Outlined.Schedule
    FallbackIconGlyph.SETTINGS -> Icons.Outlined.Settings
    FallbackIconGlyph.CALENDAR -> Icons.Outlined.CalendarToday
    FallbackIconGlyph.CALCULATOR -> Icons.Outlined.Calculate
    FallbackIconGlyph.CONTACTS -> Icons.Outlined.PersonOutline
    FallbackIconGlyph.FILES -> Icons.Outlined.FolderOpen
    FallbackIconGlyph.GALLERY -> Icons.Outlined.PhotoLibrary
    FallbackIconGlyph.MAIL -> Icons.Outlined.MailOutline
    FallbackIconGlyph.MAPS -> Icons.Outlined.Map
    FallbackIconGlyph.MUSIC -> Icons.Outlined.MusicNote
    FallbackIconGlyph.NOTES -> Icons.Outlined.Description
    FallbackIconGlyph.WEATHER -> Icons.Outlined.CloudQueue
    FallbackIconGlyph.GENERIC -> Icons.Outlined.Apps
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PinnedAppRow(
    app: AppItem,
    showIcon: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current
    val tagSafeName = app.label.lowercase(Locale.getDefault()).replace(" ", "_")

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                }
            )
            .padding(vertical = 13.dp, horizontal = 8.dp)
            .testTag("pinned_app_row_$tagSafeName"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AnimatedVisibility(visible = showIcon) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MonochromeAppIcon(app = app)
                Spacer(modifier = Modifier.width(14.dp))
            }
        }

        Text(
            text = app.label,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )

        if (app.isWorkProfile) {
            WorkProfileLabel(modifier = Modifier.padding(start = 10.dp))
        }
    }
}

/** Small "Work" tag so work-profile apps can be told apart from personal ones in text mode. */
@Composable
private fun WorkProfileLabel(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.work_profile_label),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DrawerAppRow(
    app: AppItem,
    showIcon: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onQuickTogglePin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current
    val tagSafeName = app.label.lowercase(Locale.getDefault()).replace(" ", "_")

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                }
            )
            .padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp)
            .testTag("drawer_app_row_$tagSafeName"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showIcon) {
                MonochromeAppIcon(app = app)
                Spacer(modifier = Modifier.width(14.dp))
            }

            Text(
                text = app.label,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = if (app.isPinned) FontWeight.Medium else FontWeight.Normal
                ),
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )

            if (app.isWorkProfile) {
                WorkProfileLabel(modifier = Modifier.padding(start = 10.dp))
            }

            if (app.isPinned) {
                Spacer(modifier = Modifier.width(10.dp))
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .testTag("pinned_dot_$tagSafeName")
                )
            }
        }

        IconButton(
            onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onQuickTogglePin()
            },
            modifier = Modifier
                .size(48.dp)
                .testTag("quick_pin_button_$tagSafeName")
        ) {
            Icon(
                imageVector = if (app.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                contentDescription = if (app.isPinned) {
                    stringResource(R.string.unpin_from_home)
                } else {
                    stringResource(R.string.pin_to_home)
                },
                tint = if (app.isPinned) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.42f)
                },
                modifier = Modifier.size(17.dp)
            )
        }
    }
}

@Composable
fun AlphabetIndexSidebar(
    letters: List<Char>,
    activeLetters: Set<Char>,
    onLetterSelected: (Char) -> Unit,
    modifier: Modifier = Modifier
) {
    var columnHeightPx by remember { mutableIntStateOf(0) }
    var scrubbingLetter by remember { mutableStateOf<Char?>(null) }
    val haptics = LocalHapticFeedback.current

    fun letterAtOffset(y: Float): Char? {
        if (columnHeightPx <= 0 || letters.isEmpty()) return null
        val fraction = (y / columnHeightPx.toFloat()).coerceIn(0f, 0.999f)
        val index = (fraction * letters.size).toInt().coerceIn(0, letters.lastIndex)
        return letters[index]
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.CenterEnd
    ) {
        // Floating letter indicator bubble while actively scrubbing
        AnimatedVisibility(
            visible = scrubbingLetter != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.padding(end = 44.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shadowElevation = 2.dp,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = scrubbingLetter?.toString() ?: "",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .width(32.dp)
                .fillMaxHeight()
                .padding(vertical = 8.dp)
                .onSizeChanged { columnHeightPx = it.height }
                .pointerInput(letters) {
                    detectTapGestures(
                        onPress = { offset ->
                            val letter = letterAtOffset(offset.y)
                            if (letter != null) {
                                scrubbingLetter = letter
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onLetterSelected(letter)
                            }
                            tryAwaitRelease()
                            scrubbingLetter = null
                        }
                    )
                }
                .pointerInput(letters) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val letter = letterAtOffset(offset.y)
                            if (letter != null) {
                                scrubbingLetter = letter
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onLetterSelected(letter)
                            }
                        },
                        onDragEnd = { scrubbingLetter = null },
                        onDragCancel = { scrubbingLetter = null },
                        onDrag = { change, _ ->
                            change.consume()
                            val letter = letterAtOffset(change.position.y)
                            if (letter != null && letter != scrubbingLetter) {
                                scrubbingLetter = letter
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onLetterSelected(letter)
                            }
                        }
                    )
                }
                .testTag("alphabet_sidebar"),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            letters.forEach { letter ->
                val hasApps = activeLetters.contains(letter)
                val isCurrent = scrubbingLetter == letter
                Text(
                    text = letter.toString(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = if (isCurrent) 12.sp else 10.sp,
                        fontWeight = if (isCurrent || hasApps) FontWeight.SemiBold else FontWeight.Normal
                    ),
                    color = when {
                        isCurrent -> MaterialTheme.colorScheme.primary
                        hasApps -> MaterialTheme.colorScheme.onSurfaceVariant
                        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.28f)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppContextActionSheet(
    app: AppItem,
    onDismiss: () -> Unit,
    onTogglePin: () -> Unit,
    onOpenApp: () -> Unit,
    onAppInfo: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 24.dp)
                .testTag("app_action_sheet")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MonochromeAppIcon(app = app)
                Column {
                    Text(
                        text = app.label,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (app.isPinned) "Pinned to home screen" else "In app drawer",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(8.dp))

            // Primary action: Pin or Unpin
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onTogglePin)
                    .padding(vertical = 14.dp, horizontal = 8.dp)
                    .testTag("sheet_pin_toggle_button"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Icon(
                    imageVector = if (app.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = if (app.isPinned) {
                        stringResource(R.string.unpin_from_home)
                    } else {
                        stringResource(R.string.pin_to_home)
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Secondary action: Open App
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onOpenApp)
                    .padding(vertical = 14.dp, horizontal = 8.dp)
                    .testTag("sheet_open_app_button"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.Launch,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Open ${app.label}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Tertiary action: System App Info
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onAppInfo)
                    .padding(vertical = 14.dp, horizontal = 8.dp)
                    .testTag("sheet_app_info_button"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = stringResource(R.string.app_info),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun TransientStatusPill(
    message: String?,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = !message.isNullOrBlank(),
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = CircleShape
                )
                .testTag("status_feedback_pill")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
                Text(
                    text = message ?: "",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
