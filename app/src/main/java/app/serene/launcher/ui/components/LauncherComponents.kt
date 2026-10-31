package app.serene.launcher.ui.components

import androidx.annotation.StringRes
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.serene.launcher.R
import app.serene.launcher.data.AppIconLoader
import app.serene.launcher.model.AppItem
import app.serene.launcher.model.FallbackIconGlyph
import app.serene.launcher.model.ProfileKind

@Composable
fun MinimalClockDateHeader(
    onOpenSettings: () -> Unit,
    onClockClick: () -> Unit,
    onDateClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val clock = rememberClockText()

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column(
            modifier = Modifier.testTag("clock_date_header")
        ) {
            Text(
                text = clock.time,
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .clickable(onClickLabel = stringResource(R.string.open_alarms_action), onClick = onClockClick)
                    .testTag("home_clock_text")
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = clock.date,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .clickable(onClickLabel = stringResource(R.string.open_calendar_action), onClick = onDateClick)
                    .testTag("home_date_text")
            )
        }

        IconButton(
            onClick = onOpenSettings,
            modifier = Modifier
                .size(48.dp)
                .testTag("open_settings_button")
        ) {
            Icon(
                imageVector = Icons.Outlined.Settings,
                contentDescription = stringResource(R.string.settings_open_cd),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/** Supplies app icons to [MonochromeAppIcon]; null (as in previews) shows the fallback glyphs. */
val LocalAppIconLoader = staticCompositionLocalOf<AppIconLoader?> { null }

private val AppIconSize = 20.dp

@Composable
fun MonochromeAppIcon(
    app: AppItem,
    modifier: Modifier = Modifier
) {
    val loader = LocalAppIconLoader.current
    val sizePx = with(LocalDensity.current) { AppIconSize.roundToPx() }
    val icon by produceState(initialValue = loader?.cached(app, sizePx), app.id, sizePx, loader) {
        if (value == null && loader != null) value = loader.load(app, sizePx)
    }
    val grayscaleMatrix = remember {
        ColorMatrix().apply { setToSaturation(0f) }
    }

    val loaded = icon
    if (loaded != null) {
        Image(
            bitmap = loaded.bitmap,
            contentDescription = null,
            colorFilter = if (loaded.isMonochrome) {
                ColorFilter.tint(MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                ColorFilter.colorMatrix(grayscaleMatrix)
            },
            modifier = modifier
                .size(AppIconSize)
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

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                role = Role.Button,
                onClickLabel = stringResource(R.string.open_app_action),
                onLongClickLabel = stringResource(R.string.app_options_action),
                onClick = onClick,
                onLongClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                }
            )
            .padding(vertical = 13.dp, horizontal = 8.dp)
            .testTag("pinned_app_row_${app.id}"),
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
            ProfileLabel(app, modifier = Modifier.padding(start = 10.dp))
        }
    }
}

/** Small "Work", "Private" or "Clone" tag so apps from other profiles can be told apart, with "paused" when off. */
@Composable
private fun ProfileLabel(app: AppItem, modifier: Modifier = Modifier) {
    val kind = stringResource(profileLabelRes(app.profileKind))
    Text(
        text = if (app.isProfilePaused) stringResource(R.string.paused_profile_label, kind) else kind,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
    )
}

@StringRes
fun profileLabelRes(kind: ProfileKind): Int = when (kind) {
    ProfileKind.PERSONAL, ProfileKind.WORK -> R.string.work_profile_label
    ProfileKind.PRIVATE -> R.string.private_profile_label
    ProfileKind.CLONE -> R.string.clone_profile_label
    ProfileKind.OTHER -> R.string.other_profile_label
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
    val pinnedState = stringResource(R.string.pinned_badge)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .combinedClickable(
                role = Role.Button,
                onClickLabel = stringResource(R.string.open_app_action),
                onLongClickLabel = stringResource(R.string.app_options_action),
                onClick = onClick,
                onLongClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                }
            )
            .semantics { if (app.isPinned) stateDescription = pinnedState }
            .padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp)
            .testTag("drawer_app_row_${app.id}"),
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
                ProfileLabel(app, modifier = Modifier.padding(start = 10.dp))
            }

            if (app.isPinned) {
                Spacer(modifier = Modifier.width(10.dp))
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .testTag("pinned_dot_${app.id}")
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
                .testTag("quick_pin_button_${app.id}")
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
                // A touch shortcut only: TalkBack users move through the list itself instead.
                .clearAndSetSemantics {}
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
    onAppInfo: () -> Unit,
    onReorderPins: () -> Unit = {},
    onRename: () -> Unit = {},
    onHide: () -> Unit = {}
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
                        text = stringResource(if (app.isPinned) R.string.sheet_state_pinned else R.string.sheet_state_in_drawer),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    app.originalLabel?.let { original ->
                        Text(
                            text = stringResource(R.string.sheet_original_name, original),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
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

            if (app.isPinned) {
                val reorder = stringResource(R.string.reorder_pins)
                TextButton(onClick = onReorderPins, modifier = Modifier.semantics { contentDescription = reorder }) {
                    Text(reorder)
                }
            }

            SheetActionRow(Icons.Outlined.Edit, stringResource(R.string.sheet_rename), onRename, "sheet_rename_button")
            SheetActionRow(Icons.Outlined.VisibilityOff, stringResource(R.string.sheet_hide), onHide, "sheet_hide_button")

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
                    text = stringResource(R.string.sheet_open_app, app.label),
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
private fun SheetActionRow(icon: ImageVector, text: String, onClick: () -> Unit, testTag: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 8.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/** Asks for a new name for [app]. Saving a blank name goes back to the app's own name. */
@Composable
fun RenameAppDialog(
    app: AppItem,
    onSave: (String) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit
) {
    var name by rememberSaveable(app.id) { mutableStateOf(app.label) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.rename_title, app.originalLabel ?: app.label)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(MAX_APP_NAME_LENGTH) },
                label = { Text(stringResource(R.string.rename_field_label)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onSave(name) }),
                modifier = Modifier.testTag("rename_field")
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(name) }, modifier = Modifier.testTag("rename_save_button")) {
                Text(stringResource(R.string.rename_save))
            }
        },
        dismissButton = {
            Row {
                if (app.originalLabel != null) {
                    TextButton(onClick = onReset, modifier = Modifier.testTag("rename_reset_button")) {
                        Text(stringResource(R.string.rename_reset))
                    }
                }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.rename_cancel)) }
            }
        }
    )
}

private const val MAX_APP_NAME_LENGTH = 40

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
                .semantics { liveRegion = LiveRegionMode.Polite }
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
