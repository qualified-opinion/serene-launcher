package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.AppItem
import com.example.ui.components.AlphabetIndexSidebar
import com.example.ui.components.DrawerAppRow
import kotlinx.coroutines.launch

@Composable
fun AppDrawerScreen(
    apps: List<AppItem>,
    alphabetIndexMap: Map<Char, Int>,
    availableLetters: List<Char>,
    searchQuery: String,
    showAppIcons: Boolean,
    autoLaunchSingleMatch: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    onToggleAutoLaunch: () -> Unit,
    onAppClick: (AppItem) -> Unit,
    onAppLongPress: (AppItem) -> Unit,
    onQuickTogglePin: (AppItem) -> Unit,
    onCloseDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onCloseDrawer)

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("app_drawer_screen"),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 580.dp)
                .padding(top = 12.dp)
        ) {
            // Top search and navigation bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = {
                        keyboardController?.hide()
                        onCloseDrawer()
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("close_drawer_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.KeyboardArrowDown,
                        contentDescription = stringResource(R.string.close_drawer_cd),
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }

                // Minimalist offline search bar
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))

                        Box(modifier = Modifier.weight(1f)) {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.search_apps_placeholder),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = onSearchQueryChange,
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyLarge.copy(
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(
                                    onSearch = {
                                        if (apps.isNotEmpty()) {
                                            keyboardController?.hide()
                                            onAppClick(apps.first())
                                        }
                                    }
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("drawer_search_input")
                            )
                        }

                        if (searchQuery.isNotEmpty()) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = stringResource(R.string.clear_search_cd),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .clickable(onClick = onClearSearch)
                                    .testTag("clear_search_button")
                            )
                        }
                    }
                }

                // Auto-launch single match toggle pill
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (autoLaunchSingleMatch) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
                    contentColor = if (autoLaunchSingleMatch) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .clickable(onClick = onToggleAutoLaunch)
                        .testTag("auto_launch_toggle")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Bolt,
                            contentDescription = stringResource(R.string.toggle_auto_launch_cd),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = stringResource(R.string.auto_launch_label),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            // Main content area: Either "No apps found" empty state or alphabetical list + A-Z rail
            if (apps.isEmpty()) {
                EmptySearchResultsView(
                    query = searchQuery,
                    onClearSearch = onClearSearch,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 28.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 16.dp, end = 8.dp)
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(end = 32.dp)
                            .testTag("drawer_app_list"),
                        contentPadding = PaddingValues(top = 10.dp, bottom = 36.dp)
                    ) {
                        itemsIndexed(
                            items = apps,
                            key = { _, app -> app.id }
                        ) { index, app ->
                            val currentSection = app.firstLetterSection
                            val previousSection = apps.getOrNull(index - 1)?.firstLetterSection
                            val showSectionHeader = searchQuery.isBlank() && currentSection != previousSection

                            if (showSectionHeader) {
                                Text(
                                    text = currentSection.toString(),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(
                                        start = 12.dp,
                                        top = if (index == 0) 4.dp else 14.dp,
                                        bottom = 2.dp
                                    )
                                )
                            }

                            DrawerAppRow(
                                app = app,
                                showIcon = showAppIcons,
                                onClick = {
                                    keyboardController?.hide()
                                    onAppClick(app)
                                },
                                onLongClick = {
                                    keyboardController?.hide()
                                    onAppLongPress(app)
                                },
                                onQuickTogglePin = {
                                    onQuickTogglePin(app)
                                }
                            )
                        }
                    }

                    // Alphabet Index Sidebar along the right edge
                    AlphabetIndexSidebar(
                        letters = availableLetters,
                        activeLetters = alphabetIndexMap.keys,
                        onLetterSelected = { letter ->
                            val targetIndex = alphabetIndexMap[letter]
                            if (targetIndex != null) {
                                coroutineScope.launch {
                                    listState.scrollToItem(targetIndex)
                                }
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(vertical = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptySearchResultsView(
    query: String,
    onClearSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.testTag("empty_search_state"),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Outlined.SearchOff,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(36.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.no_apps_found_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.no_apps_found_subtitle, query),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(18.dp))
        FilledTonalButton(
            onClick = onClearSearch,
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier.testTag("empty_search_clear_button")
        ) {
            Text(text = stringResource(R.string.clear_search))
        }
    }
}
