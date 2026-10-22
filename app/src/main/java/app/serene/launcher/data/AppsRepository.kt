package app.serene.launcher.data

import app.serene.launcher.model.AppItem
import kotlinx.coroutines.flow.Flow

sealed interface AppChange {
    data object Refresh : AppChange
    /** A null serial denotes the personal profile, whose ids have no suffix. */
    data class Removed(val packageName: String, val profileSerial: Long?) : AppChange
}

/** Source of launchable apps and the actions the launcher performs on them. */
interface AppsRepository {
    suspend fun loadInstalledApps(): List<AppItem>

    /** Emits whenever apps are installed, removed, updated, enabled or disabled in any profile. */
    fun appChanges(): Flow<AppChange>

    fun selectInitialPinnedIds(allApps: List<AppItem>): Set<String>

    /** Starts the app. Fails if it is no longer installed, is disabled, or its profile is paused. */
    fun launchApp(app: AppItem): Result<Unit>

    fun openAppInfo(app: AppItem): Result<Unit>
}
