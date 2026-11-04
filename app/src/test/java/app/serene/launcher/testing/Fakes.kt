package app.serene.launcher.testing

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import app.serene.launcher.data.AppChange
import app.serene.launcher.data.AppsRepository
import app.serene.launcher.data.LockResult
import app.serene.launcher.data.SystemActions
import app.serene.launcher.model.AppItem
import app.serene.launcher.model.AppShortcut
import app.serene.launcher.model.ProfileKind
import app.serene.launcher.model.ProfileScan
import app.serene.launcher.model.ProfileState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

fun app(label: String, id: String = "pkg.${label.lowercase()}/Main") =
    AppItem(id = id, label = label, packageName = id.substringBefore('/'), activityName = id.substringAfter('/').substringBefore('@'))

/** The same app as [personal] in the profile with [serial], with the `@serial` id work apps use. */
fun inProfile(personal: AppItem, serial: Long, kind: ProfileKind = ProfileKind.WORK) =
    personal.copy(id = "${personal.id}@$serial", profileSerial = serial, profileKind = kind)

/** In-memory [AppsRepository] that records which apps were launched. */
class FakeAppsRepository(
    var apps: List<AppItem>,
    private val initialPins: Set<String> = emptySet()
) : AppsRepository {
    val launched = mutableListOf<AppItem>()

    /** When true, launching and opening app info fail, as they would for an app that was just removed. */
    var failLaunches = false

    /**
     * Profile states by serial. Profiles not listed here are available. A listed profile is
     * scanned even with no apps in [apps], as a work profile unless its apps say otherwise.
     */
    val profileStates = mutableMapOf<Long, ProfileState>()

    /** Serials Android still knows besides those scanned, for [profileExists]. */
    val otherKnownProfiles = mutableSetOf<Long>()

    /** Each profile in [apps] or [profileStates] becomes one scan. */
    override suspend fun scanProfiles(): List<ProfileScan> =
        (apps.map { it.profileSerial } + profileStates.keys).distinct().map { serial ->
            val profileApps = apps.filter { it.profileSerial == serial }
            ProfileScan(
                serial = serial,
                kind = profileApps.firstOrNull()?.profileKind ?: ProfileKind.WORK,
                state = profileStates[serial] ?: ProfileState.AVAILABLE,
                apps = profileApps
            )
        }

    override fun profileExists(serial: Long): Boolean =
        serial in otherKnownProfiles || serial in profileStates || apps.any { it.profileSerial == serial }

    val changes = MutableSharedFlow<AppChange>()
    override fun appChanges(): Flow<AppChange> = changes

    override fun selectInitialPinnedIds(allApps: List<AppItem>): Set<String> = initialPins

    override fun launchApp(app: AppItem): Result<Unit> {
        if (failLaunches) return Result.failure(IllegalStateException("launch failed"))
        launched += app
        return Result.success(Unit)
    }

    /** Whether [requestProfileResume] succeeds, as it does for the default home app on Android 9+. */
    var resumeAllowed = false
    val resumeRequests = mutableListOf<AppItem>()

    override fun requestProfileResume(app: AppItem): Boolean {
        resumeRequests += app
        return resumeAllowed
    }

    override fun openAppInfo(app: AppItem): Result<Unit> =
        if (failLaunches) Result.failure(IllegalStateException("app info failed")) else Result.success(Unit)

    /** Shortcuts by app id, as the long-press menu would get them. */
    val shortcuts = mutableMapOf<String, List<AppShortcut>>()
    val startedShortcuts = mutableListOf<AppShortcut>()

    override suspend fun shortcutsFor(app: AppItem): List<AppShortcut> = shortcuts[app.id].orEmpty()

    override fun startShortcut(shortcut: AppShortcut): Result<Unit> {
        if (failLaunches) return Result.failure(IllegalStateException("shortcut failed"))
        startedShortcuts += shortcut
        return Result.success(Unit)
    }
}

/** DataStore that keeps preferences in memory, so tests need no files or Android runtime. */
class InMemoryPreferencesDataStore : DataStore<Preferences> {
    private val state = MutableStateFlow(emptyPreferences())
    private val mutex = Mutex()

    override val data: Flow<Preferences> = state

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences =
        mutex.withLock { transform(state.value).also { state.value = it } }
}

/** Records gesture actions instead of performing them. */
class FakeSystemActions(
    var notificationsOpen: Boolean = true,
    var lockResult: LockResult = LockResult.LOCKED
) : SystemActions {
    var notificationRequests = 0
    var lockRequests = 0
    override val isLockServiceOn: Boolean get() = lockResult == LockResult.LOCKED

    override fun openNotifications(): Boolean {
        notificationRequests++
        return notificationsOpen
    }

    override fun lockScreen(): LockResult {
        lockRequests++
        return lockResult
    }
}
