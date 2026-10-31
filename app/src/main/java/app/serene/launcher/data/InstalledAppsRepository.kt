package app.serene.launcher.data

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import android.util.Log
import androidx.core.content.ContextCompat
import app.serene.launcher.domain.AppSearch
import app.serene.launcher.model.AppItem
import app.serene.launcher.model.FallbackIconGlyph
import app.serene.launcher.model.ProfileKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * Lists launchable apps through [LauncherApps], which covers every profile on the device
 * (personal, work, private space), and launches them in the right profile.
 */
class InstalledAppsRepository(
    private val context: Context
) : AppsRepository {

    private val launcherApps = context.getSystemService(LauncherApps::class.java)
    private val userManager = context.getSystemService(UserManager::class.java)

    /** The last apps read from each profile, by serial, for when Android refuses to list one. */
    private val lastAppsByProfile = ConcurrentHashMap<Long, List<AppItem>>()

    override suspend fun loadInstalledApps(): List<AppItem> = withContext(Dispatchers.IO) {
        val ownPackage = context.packageName
        val myUser = Process.myUserHandle()

        userManager.userProfiles.flatMap { user ->
            val serial = userManager.getSerialNumberForUser(user)
            val isPersonal = user == myUser
            val kind = if (isPersonal) ProfileKind.PERSONAL else profileKind(user)
            val isPaused = !isPersonal && isQuietModeEnabled(user)
            // A locked private space must not reveal its apps anywhere.
            if (kind == ProfileKind.PRIVATE && isPaused) return@flatMap emptyList()

            val activities = try {
                launcherApps.getActivityList(null, user)
            } catch (_: SecurityException) {
                // Don't let a refused read look like an empty profile: keep showing what was there.
                Log.w(TAG, "Not allowed to list apps in profile $serial; keeping the last list")
                return@flatMap lastAppsByProfile[serial].orEmpty().map { it.copy(isProfilePaused = isPaused) }
            }
            activities
                .filterNot { it.componentName.packageName == ownPackage }
                .map { info ->
                    val component = info.componentName
                    val label = info.label?.toString()?.trim().takeUnless { it.isNullOrBlank() }
                        ?: component.packageName.substringAfterLast('.')
                    AppItem(
                        id = appId(component, serial, !isPersonal),
                        label = label,
                        packageName = component.packageName,
                        activityName = component.className,
                        fallbackGlyph = inferFallbackGlyph(label),
                        profileSerial = serial,
                        isWorkProfile = !isPersonal,
                        profileKind = kind,
                        isProfilePaused = isPaused
                    )
                }
                .also { lastAppsByProfile[serial] = it }
        }
            .distinctBy { it.id }
            .let { AppSearch.sort(it) }
    }

    override fun profileSerials(): Set<Long>? {
        val hiddenProfilesVisible = Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM ||
            context.checkSelfPermission(ACCESS_HIDDEN_PROFILES) == PackageManager.PERMISSION_GRANTED
        if (!hiddenProfilesVisible) return null
        return userManager.userProfiles.map { userManager.getSerialNumberForUser(it) }.toSet()
    }

    /** Android 15 says which kind a profile is; before that, every extra profile is treated as work. */
    private fun profileKind(user: UserHandle): ProfileKind {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) return ProfileKind.WORK
        val userType = try {
            launcherApps.getLauncherUserInfo(user)?.userType
        } catch (_: SecurityException) {
            null
        }
        return when (userType) {
            USER_TYPE_MANAGED -> ProfileKind.WORK
            USER_TYPE_PRIVATE -> ProfileKind.PRIVATE
            USER_TYPE_CLONE -> ProfileKind.CLONE
            null -> ProfileKind.WORK
            else -> ProfileKind.OTHER
        }
    }

    private fun isQuietModeEnabled(user: UserHandle): Boolean =
        try {
            userManager.isQuietModeEnabled(user)
        } catch (_: SecurityException) {
            false
        }

    override fun appChanges(): Flow<AppChange> = callbackFlow {
        val callback = object : LauncherApps.Callback() {
            override fun onPackageRemoved(packageName: String, user: UserHandle) {
                val serial = if (user == Process.myUserHandle()) null else userManager.getSerialNumberForUser(user)
                trySend(AppChange.Removed(packageName, serial))
            }

            override fun onPackageAdded(packageName: String, user: UserHandle) {
                trySend(AppChange.Refresh)
            }

            override fun onPackageChanged(packageName: String, user: UserHandle) {
                trySend(AppChange.Refresh)
            }

            override fun onPackagesAvailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) {
                trySend(AppChange.Refresh)
            }

            override fun onPackagesUnavailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) {
                trySend(AppChange.Refresh)
            }
        }
        launcherApps.registerCallback(callback, Handler(Looper.getMainLooper()))

        // Profiles being added, removed, paused or resumed don't reach LauncherApps.Callback.
        val profileReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                trySend(AppChange.Refresh)
            }
        }
        val profileFilter = IntentFilter().apply { PROFILE_ACTIONS.forEach(::addAction) }
        ContextCompat.registerReceiver(context, profileReceiver, profileFilter, ContextCompat.RECEIVER_NOT_EXPORTED)

        awaitClose {
            launcherApps.unregisterCallback(callback)
            context.unregisterReceiver(profileReceiver)
        }
    }

    /**
     * Selects a sensible initial set of up to 5 essential app IDs to pin on first run.
     */
    override fun selectInitialPinnedIds(allApps: List<AppItem>): Set<String> {
        val preferredNames = listOf("Phone", "Messages", "Camera", "Settings", "Clock", "Notes", "Calendar")
        val personalApps = allApps.filterNot { it.isWorkProfile }
        val selected = mutableSetOf<String>()
        for (preferred in preferredNames) {
            val match = personalApps.firstOrNull { it.label.equals(preferred, ignoreCase = true) }
                ?: personalApps.firstOrNull { it.label.contains(preferred, ignoreCase = true) }
            if (match != null) {
                selected.add(match.id)
            }
            if (selected.size >= 5) break
        }
        if (selected.isEmpty()) {
            personalApps.take(4).forEach { selected.add(it.id) }
        }
        return selected
    }

    override fun launchApp(app: AppItem): Result<Unit> = runCatching {
        val user = app.userHandle()
        if (app.isWorkProfile && isQuietModeEnabled(user)) {
            // The default launcher may ask Android to resume the profile; Android shows any unlock screen.
            val resumeRequested = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
                runCatching { userManager.requestQuietModeEnabled(false, user) }.isSuccess
            throw ProfilePausedException(resumeRequested)
        }
        launcherApps.startMainActivity(app.componentName(), user, null, null)
    }

    override fun openAppInfo(app: AppItem): Result<Unit> = runCatching {
        launcherApps.startAppDetailsActivity(app.componentName(), app.userHandle(), null, null)
    }

    private fun AppItem.componentName(): ComponentName =
        ComponentName(packageName, requireNotNull(activityName) { "No activity for $label" })

    private fun AppItem.userHandle(): UserHandle =
        requireNotNull(userManager.getUserForSerialNumber(profileSerial)) { "Profile for $label is gone" }

    /**
     * Personal apps keep the plain `package/activity` id used before work profiles were supported,
     * so existing pins still match. Apps in other profiles get the profile serial appended.
     */
    private fun appId(component: ComponentName, serial: Long, isWork: Boolean): String {
        val base = "${component.packageName}/${component.className}"
        return if (isWork) "$base@$serial" else base
    }

    private companion object {
        const val TAG = "InstalledApps"
        const val ACCESS_HIDDEN_PROFILES = "android.permission.ACCESS_HIDDEN_PROFILES"
        const val USER_TYPE_MANAGED = "android.os.usertype.profile.MANAGED"
        const val USER_TYPE_PRIVATE = "android.os.usertype.profile.PRIVATE"
        const val USER_TYPE_CLONE = "android.os.usertype.profile.CLONE"

        val PROFILE_ACTIONS = listOf(
            Intent.ACTION_MANAGED_PROFILE_ADDED,
            Intent.ACTION_MANAGED_PROFILE_REMOVED,
            Intent.ACTION_MANAGED_PROFILE_AVAILABLE,
            Intent.ACTION_MANAGED_PROFILE_UNAVAILABLE,
            Intent.ACTION_MANAGED_PROFILE_UNLOCKED,
            // Android 15+ sends these for every kind of profile, including private space.
            "android.intent.action.PROFILE_ADDED",
            "android.intent.action.PROFILE_REMOVED",
            "android.intent.action.PROFILE_AVAILABLE",
            "android.intent.action.PROFILE_UNAVAILABLE"
        )
    }

    private fun inferFallbackGlyph(label: String): FallbackIconGlyph {
        val lower = label.lowercase(Locale.getDefault())
        return when {
            lower.contains("phone") || lower.contains("dial") || lower.contains("call") -> FallbackIconGlyph.PHONE
            lower.contains("message") || lower.contains("sms") || lower.contains("chat") -> FallbackIconGlyph.MESSAGES
            lower.contains("chrome") || lower.contains("browser") || lower.contains("web") -> FallbackIconGlyph.BROWSER
            lower.contains("camera") || lower.contains("photo") -> FallbackIconGlyph.CAMERA
            lower.contains("clock") || lower.contains("alarm") || lower.contains("timer") -> FallbackIconGlyph.CLOCK
            lower.contains("setting") -> FallbackIconGlyph.SETTINGS
            lower.contains("calendar") -> FallbackIconGlyph.CALENDAR
            lower.contains("calc") -> FallbackIconGlyph.CALCULATOR
            lower.contains("contact") || lower.contains("people") -> FallbackIconGlyph.CONTACTS
            lower.contains("file") || lower.contains("document") -> FallbackIconGlyph.FILES
            lower.contains("gallery") || lower.contains("image") -> FallbackIconGlyph.GALLERY
            lower.contains("mail") || lower.contains("gmail") -> FallbackIconGlyph.MAIL
            lower.contains("map") || lower.contains("navigation") -> FallbackIconGlyph.MAPS
            lower.contains("music") || lower.contains("audio") || lower.contains("spotify") -> FallbackIconGlyph.MUSIC
            lower.contains("note") || lower.contains("keep") -> FallbackIconGlyph.NOTES
            lower.contains("weather") -> FallbackIconGlyph.WEATHER
            else -> FallbackIconGlyph.GENERIC
        }
    }
}
