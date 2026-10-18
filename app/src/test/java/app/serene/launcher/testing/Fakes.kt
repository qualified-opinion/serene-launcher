package app.serene.launcher.testing

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import app.serene.launcher.data.AppsRepository
import app.serene.launcher.model.AppItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

fun app(label: String, id: String = "pkg.${label.lowercase()}/Main") =
    AppItem(id = id, label = label, packageName = id.substringBefore('/'), activityName = id.substringAfter('/'))

/** In-memory [AppsRepository] that records which apps were launched. */
class FakeAppsRepository(
    var apps: List<AppItem>,
    private val initialPins: Set<String> = emptySet()
) : AppsRepository {
    val launched = mutableListOf<AppItem>()

    /** When true, launching and opening app info fail, as they would for an app that was just removed. */
    var failLaunches = false

    override suspend fun loadInstalledApps(): List<AppItem> = apps

    override fun appChanges(): Flow<Unit> = emptyFlow()

    override fun selectInitialPinnedIds(allApps: List<AppItem>): Set<String> = initialPins

    override fun launchApp(app: AppItem): Result<Unit> {
        if (failLaunches) return Result.failure(IllegalStateException("launch failed"))
        launched += app
        return Result.success(Unit)
    }

    override fun openAppInfo(app: AppItem): Result<Unit> =
        if (failLaunches) Result.failure(IllegalStateException("app info failed")) else Result.success(Unit)
}

/** DataStore that keeps preferences in memory, so tests need no files or Android runtime. */
class InMemoryPreferencesDataStore : DataStore<Preferences> {
    private val state = MutableStateFlow(emptyPreferences())
    private val mutex = Mutex()

    override val data: Flow<Preferences> = state

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences =
        mutex.withLock { transform(state.value).also { state.value = it } }
}
