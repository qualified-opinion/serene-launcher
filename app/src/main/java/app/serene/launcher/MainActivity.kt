package app.serene.launcher

import android.content.Intent
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.serene.launcher.data.AppIconLoader
import app.serene.launcher.ui.components.AppContextActionSheet
import app.serene.launcher.ui.components.LocalAppIconLoader
import app.serene.launcher.ui.components.TransientStatusPill
import app.serene.launcher.ui.screens.AppDrawerScreen
import app.serene.launcher.ui.screens.HomeScreen
import app.serene.launcher.ui.theme.SereneLauncherTheme
import app.serene.launcher.viewmodel.LauncherViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: LauncherViewModel by viewModels {
        LauncherViewModel.Factory(application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            SereneLauncherTheme(darkTheme = uiState.isDarkTheme) {
                CompositionLocalProvider(LocalAppIconLoader provides AppIconLoader.get(this)) {
                    SereneLauncherRoot(
                        viewModel = viewModel
                    )
                }
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
