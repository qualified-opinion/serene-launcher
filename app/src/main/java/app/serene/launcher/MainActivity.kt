package app.serene.launcher

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.AlarmClock
import android.provider.CalendarContract
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
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.serene.launcher.data.AppIconLoader
import app.serene.launcher.ui.components.AppContextActionSheet
import app.serene.launcher.ui.components.LocalAppIconLoader
import app.serene.launcher.ui.components.TransientStatusPill
import app.serene.launcher.ui.screens.AppDrawerScreen
import app.serene.launcher.model.ThemeMode
import app.serene.launcher.ui.screens.HomeScreen
import app.serene.launcher.ui.screens.SettingsScreen
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

            val darkTheme = when (uiState.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            SereneLauncherTheme(darkTheme = darkTheme) {
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
            viewModel.finishReorderingPins()
            viewModel.closeDrawer()
            viewModel.closeSettings()
            viewModel.dismissAppMenu()
        }
    }
}

@Composable
fun SereneLauncherRoot(
    viewModel: LauncherViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

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
                targetState = when {
                    uiState.isSettingsOpen -> LauncherScreen.SETTINGS
                    uiState.isDrawerOpen -> LauncherScreen.DRAWER
                    else -> LauncherScreen.HOME
                },
                transitionSpec = {
                    if (targetState != LauncherScreen.HOME) {
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
            ) { screen ->
                when (screen) {
                    LauncherScreen.SETTINGS -> SettingsScreen(
                        themeMode = uiState.themeMode,
                        showAppIcons = uiState.showAppIcons,
                        autoLaunchSingleMatch = uiState.autoLaunchSingleMatch,
                        openKeyboardOnDrawer = uiState.openKeyboardOnDrawer,
                        onThemeModeChange = viewModel::setThemeMode,
                        onShowAppIconsChange = viewModel::setShowAppIcons,
                        onAutoLaunchSingleMatchChange = viewModel::setAutoLaunchSingleMatch,
                        onOpenKeyboardOnDrawerChange = viewModel::setOpenKeyboardOnDrawer,
                        onClose = viewModel::closeSettings
                    )
                    LauncherScreen.DRAWER -> AppDrawerScreen(
                        apps = uiState.filteredDrawerApps,
                        isLoading = uiState.isLoading,
                        alphabetIndexMap = uiState.alphabetIndexMap,
                        availableLetters = uiState.availableLetters,
                        searchQuery = uiState.searchQuery,
                        showAppIcons = uiState.showAppIcons,
                        openKeyboardOnOpen = uiState.openKeyboardOnDrawer,
                        onSearchQueryChange = viewModel::onSearchQueryChanged,
                        onClearSearch = viewModel::clearSearch,
                        onAppClick = viewModel::launchApp,
                        onAppLongPress = viewModel::onAppLongPress,
                        onQuickTogglePin = viewModel::togglePin,
                        onCloseDrawer = viewModel::closeDrawer
                    )
                    LauncherScreen.HOME -> HomeScreen(
                        pinnedApps = uiState.pinnedApps,
                        isReorderingPins = uiState.isReorderingPins,
                        onMovePin = viewModel::movePin,
                        onDoneReordering = viewModel::finishReorderingPins,
                        isLoading = uiState.isLoading,
                        totalInstalledCount = uiState.allApps.size,
                        showAppIcons = uiState.showAppIcons,
                        onAppClick = viewModel::launchApp,
                        onAppLongPress = viewModel::onAppLongPress,
                        onOpenDrawer = viewModel::openDrawer,
                        onOpenSettings = viewModel::openSettings,
                        onClockClick = {
                            context.openSystemScreen(Intent(AlarmClock.ACTION_SHOW_ALARMS)) {
                                viewModel.onSystemScreenUnavailable(R.string.status_cant_open_alarms)
                            }
                        },
                        onDateClick = {
                            val today = CalendarContract.CONTENT_URI.buildUpon()
                                .appendPath("time")
                                .appendPath(System.currentTimeMillis().toString())
                                .build()
                            context.openSystemScreen(Intent(Intent.ACTION_VIEW, today)) {
                                viewModel.onSystemScreenUnavailable(R.string.status_cant_open_calendar)
                            }
                        }
                    )
                }
            }

            // Transient confirmation pill (e.g. "Pinned Calculator to Home")
            TransientStatusPill(
                message = uiState.statusMessage?.let { stringResource(it.resId, *it.args.toTypedArray()) },
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
                    onAppInfo = { viewModel.openAppInfo(selectedApp) },
                    onReorderPins = viewModel::startReorderingPins
                )
            }
        }
    }
}

/** Which full screen the launcher is showing. */
private enum class LauncherScreen { HOME, DRAWER, SETTINGS }

/** Starts [intent], or calls [onUnavailable] if no app on the phone handles it. */
private fun Context.openSystemScreen(intent: Intent, onUnavailable: () -> Unit) {
    try {
        startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        onUnavailable()
    } catch (_: SecurityException) {
        onUnavailable()
    }
}
