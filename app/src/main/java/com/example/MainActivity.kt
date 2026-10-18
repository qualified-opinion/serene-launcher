package com.example

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.AppContextActionSheet
import com.example.ui.components.TransientStatusPill
import com.example.ui.screens.AppDrawerScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.theme.SereneLauncherTheme
import com.example.viewmodel.LauncherViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: LauncherViewModel by viewModels {
        LauncherViewModel.Factory(application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            // Listen for package additions/removals offline to keep the app drawer fresh
            val context = LocalContext.current
            DisposableEffect(context) {
                val receiver = object : BroadcastReceiver() {
                    override fun onReceive(ctx: Context?, intent: Intent?) {
                        viewModel.refreshInstalledApps()
                    }
                }
                val filter = IntentFilter().apply {
                    addAction(Intent.ACTION_PACKAGE_ADDED)
                    addAction(Intent.ACTION_PACKAGE_REMOVED)
                    addAction(Intent.ACTION_PACKAGE_CHANGED)
                    addDataScheme("package")
                }
                context.registerReceiver(receiver, filter)
                onDispose {
                    try {
                        context.unregisterReceiver(receiver)
                    } catch (_: Exception) {
                    }
                }
            }

            SereneLauncherTheme(darkTheme = uiState.isDarkTheme) {
                SereneLauncherRoot(
                    viewModel = viewModel
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Pressing the system Home button while already in the launcher returns cleanly to Home
        if (intent.action == Intent.ACTION_MAIN && intent.hasCategory(Intent.CATEGORY_HOME)) {
            viewModel.closeDrawer()
            viewModel.dismissAppMenu()
        }
    }
}

@Composable
fun SereneLauncherRoot(
    viewModel: LauncherViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = uiState.isDrawerOpen,
                transitionSpec = {
                    if (targetState) {
                        // Opening App Drawer: slide up gently + fade in
                        (slideInVertically(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            ),
                            initialOffsetY = { fullHeight -> fullHeight / 6 }
                        ) + fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMedium)))
                            .togetherWith(fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMedium)))
                    } else {
                        // Returning to Home Screen: fade in + slide drawer down gently
                        fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMedium))
                            .togetherWith(
                                slideOutVertically(
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioNoBouncy,
                                        stiffness = Spring.StiffnessMediumLow
                                    ),
                                    targetOffsetY = { fullHeight -> fullHeight / 6 }
                                ) + fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMedium))
                            )
                    }
                },
                label = "LauncherScreenTransition"
            ) { isDrawerOpen ->
                if (isDrawerOpen) {
                    AppDrawerScreen(
                        apps = uiState.filteredDrawerApps,
                        alphabetIndexMap = uiState.alphabetIndexMap,
                        availableLetters = uiState.availableLetters,
                        searchQuery = uiState.searchQuery,
                        showAppIcons = uiState.showAppIcons,
                        autoLaunchSingleMatch = uiState.autoLaunchSingleMatch,
                        onSearchQueryChange = viewModel::onSearchQueryChanged,
                        onClearSearch = viewModel::clearSearch,
                        onToggleAutoLaunch = viewModel::toggleAutoLaunchSingleMatch,
                        onAppClick = viewModel::launchApp,
                        onAppLongPress = viewModel::onAppLongPress,
                        onQuickTogglePin = viewModel::togglePin,
                        onCloseDrawer = viewModel::closeDrawer
                    )
                } else {
                    HomeScreen(
                        pinnedApps = uiState.pinnedApps,
                        totalInstalledCount = uiState.allApps.size,
                        isDarkTheme = uiState.isDarkTheme,
                        showAppIcons = uiState.showAppIcons,
                        onAppClick = viewModel::launchApp,
                        onAppLongPress = viewModel::onAppLongPress,
                        onOpenDrawer = viewModel::openDrawer,
                        onToggleTheme = viewModel::toggleTheme,
                        onToggleIcons = viewModel::toggleShowAppIcons
                    )
                }
            }

            // Transient confirmation pill (e.g. "Pinned Calculator to Home")
            TransientStatusPill(
                message = uiState.statusMessage,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 84.dp)
            )

            // Long-press contextual sheet for Pin / Unpin / Open / App Info
            val selectedApp = uiState.selectedAppForMenu
            if (selectedApp != null) {
                AppContextActionSheet(
                    app = selectedApp,
                    onDismiss = viewModel::dismissAppMenu,
                    onTogglePin = { viewModel.togglePin(selectedApp) },
                    onOpenApp = { viewModel.launchApp(selectedApp) },
                    onAppInfo = { viewModel.openAppInfo(selectedApp) }
                )
            }
        }
    }
}
