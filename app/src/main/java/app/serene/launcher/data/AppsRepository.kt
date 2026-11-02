package app.serene.launcher.data

import app.serene.launcher.domain.ProfileApps
import app.serene.launcher.model.AppItem
import app.serene.launcher.model.AppShortcut
import app.serene.launcher.model.ProfileScan
import kotlinx.coroutines.flow.Flow

sealed interface AppChange {
    data object Refresh : AppChange
    /** A null serial denotes the personal profile, whose ids have no suffix. */
    data class Removed(val packageName: String, val profileSerial: Long?) : AppChange
}

/** Source of launchable apps and the actions the launcher performs on them. */
interface AppsRepository {
    /** Lists every profile on the phone with its apps and whether the list could be read. */
    suspend fun scanProfiles(): List<ProfileScan>

    /** All launchable apps from one scan, with no earlier scan to fill in paused profiles. */
    suspend fun loadInstalledApps(): List<AppItem> = ProfileApps.merge(emptyList(), scanProfiles())

    /** True if Android still knows a profile with this serial number, even one the scan skipped. */
    fun profileExists(serial: Long): Boolean

    /**
     * Emits whenever apps are installed, removed, updated, enabled or disabled in any profile,
     * and when a profile is added, removed, paused, resumed or unlocked.
     */
    fun appChanges(): Flow<AppChange>

    fun selectInitialPinnedIds(allApps: List<AppItem>): Set<String>

    /** Starts the app. Fails if it is no longer installed, is disabled, or its profile is paused. */
    fun launchApp(app: AppItem): Result<Unit>

    fun openAppInfo(app: AppItem): Result<Unit>

    /**
     * The app's own shortcuts for its long-press menu. Empty when it has none, when Serene isn't
     * the default home app (Android only shares shortcuts with that app) or on Android 7.0.
     */
    suspend fun shortcutsFor(app: AppItem): List<AppShortcut>

    /** Opens one of an app's shortcuts. Fails if the app removed it or its profile is paused. */
    fun startShortcut(shortcut: AppShortcut): Result<Unit>
}
