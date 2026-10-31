package app.serene.launcher.data

import app.serene.launcher.model.AppItem
import kotlinx.coroutines.flow.Flow

sealed interface AppChange {
    data object Refresh : AppChange
    /** A null serial denotes the personal profile, whose ids have no suffix. */
    data class Removed(val packageName: String, val profileSerial: Long?) : AppChange
}

/**
 * Launching failed because the app's profile (usually work apps) is paused. When [resumeRequested]
 * is true, Android was asked to turn the profile back on and may show its own confirmation.
 */
class ProfilePausedException(val resumeRequested: Boolean) : IllegalStateException("Profile is paused")

/** Source of launchable apps and the actions the launcher performs on them. */
interface AppsRepository {
    suspend fun loadInstalledApps(): List<AppItem>

    /**
     * Emits whenever apps are installed, removed, updated, enabled or disabled in any profile, and
     * when a profile is added, removed, paused or resumed.
     */
    fun appChanges(): Flow<AppChange>

    fun selectInitialPinnedIds(allApps: List<AppItem>): Set<String>

    /**
     * Serial numbers of every profile on the phone, including paused ones, or null when some may be
     * invisible to the launcher (a private space it isn't allowed to see).
     */
    fun profileSerials(): Set<Long>? = null

    /**
     * Starts the app. Fails if it is no longer installed or is disabled, and with
     * [ProfilePausedException] if its profile is paused.
     */
    fun launchApp(app: AppItem): Result<Unit>

    fun openAppInfo(app: AppItem): Result<Unit>
}
