package app.serene.launcher.data

import app.serene.launcher.model.AppItem
import kotlinx.coroutines.flow.Flow

/** Source of launchable apps and the actions the launcher performs on them. */
interface AppsRepository {
    suspend fun loadInstalledApps(): List<AppItem>

    /** Emits whenever apps are installed, removed, updated, enabled or disabled in any profile. */
    fun appChanges(): Flow<Unit>

    fun selectInitialPinnedIds(allApps: List<AppItem>): Set<String>

    /** Starts the app. Fails if it is no longer installed, is disabled, or its profile is paused. */
    fun launchApp(app: AppItem): Result<Unit>

    fun openAppInfo(app: AppItem): Result<Unit>
}
