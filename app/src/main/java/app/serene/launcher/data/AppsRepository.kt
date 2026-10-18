package app.serene.launcher.data

import app.serene.launcher.model.AppItem

/** Source of launchable apps and the actions the launcher performs on them. */
interface AppsRepository {
    suspend fun loadInstalledApps(): List<AppItem>

    fun selectInitialPinnedIds(allApps: List<AppItem>): Set<String>

    fun launchApp(app: AppItem): Result<String>

    fun openAppInfo(app: AppItem): Result<String>
}
