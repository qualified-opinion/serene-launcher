package app.serene.launcher.data

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.LauncherApps
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import androidx.core.content.ContextCompat
import app.serene.launcher.domain.ProfileApps
import app.serene.launcher.model.AppItem
import app.serene.launcher.model.FallbackIconGlyph
import app.serene.launcher.model.ProfileKind
import app.serene.launcher.model.ProfileScan
import app.serene.launcher.model.ProfileState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * Lists launchable apps through [LauncherApps], which covers every profile on the device
 * (personal, work, private space), and launches them in the right profile.
 */
class InstalledAppsRepository(
    private val context: Context
) : AppsRepository {

    private val launcherApps = context.getSystemService(LauncherApps::class.java)
    private val userManager = context.getSystemService(UserManager::class.java)

    override suspend fun scanProfiles(): List<ProfileScan> = withContext(Dispatchers.IO) {
        val ownPackage = context.packageName
        val myUser = Process.myUserHandle()

        profiles().map { user ->
            val serial = userManager.getSerialNumberForUser(user)
            val isPersonal = user == myUser
            val kind = if (isPersonal) ProfileKind.PERSONAL else kindOf(user)
            // A refused scan says nothing about what is installed, so it isn't treated as "no apps".
            val activities = try {
                launcherApps.getActivityList(null, user)
            } catch (_: RuntimeException) {
                null
            }
            val state = when {
                activities == null -> ProfileState.UNREADABLE
                !isPersonal && isPaused(user) -> ProfileState.PAUSED
                !isUnlocked(user) -> ProfileState.LOCKED
                else -> ProfileState.AVAILABLE
            }
            val apps = activities.orEmpty()
                .filterNot { it.componentName.packageName == ownPackage }
                .map { info ->
                    val component = info.componentName
                    val label = info.label?.toString()?.trim().takeUnless { it.isNullOrBlank() }
                        ?: component.packageName.substringAfterLast('.')
                    AppItem(
                        id = appId(component, serial, isWork = !isPersonal),
                        label = label,
                        packageName = component.packageName,
                        activityName = component.className,
                        fallbackGlyph = inferFallbackGlyph(label),
                        profileSerial = serial,
                        profileKind = kind
                    )
                }
                .distinctBy { it.id }
            ProfileScan(serial = serial, kind = kind, state = state, apps = apps)
        }
    }

    override fun profileExists(serial: Long): Boolean =
        runCatching { userManager.getUserForSerialNumber(serial) != null }.getOrDefault(true)

    /**
     * The phone's profiles. From Android 15, LauncherApps also lists private space, which
     * UserManager leaves out; it needs the ACCESS_HIDDEN_PROFILES permission and Serene as the
     * default home app.
     */
    private fun profiles(): List<UserHandle> {
        val profiles = userManager.userProfiles
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) return profiles
        val launcherProfiles = runCatching { launcherApps.profiles }.getOrNull().orEmpty()
        return (profiles + launcherProfiles).distinct()
    }

    @SuppressLint("FlaggedApi")
    private fun kindOf(user: UserHandle): ProfileKind {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) return ProfileKind.WORK
        val userType = runCatching { launcherApps.getLauncherUserInfo(user)?.userType }.getOrNull()
        return ProfileApps.kindForUserType(userType)
    }

    private fun isPaused(user: UserHandle): Boolean =
        runCatching { userManager.isQuietModeEnabled(user) }.getOrDefault(false)

    private fun isUnlocked(user: UserHandle): Boolean =
        runCatching { userManager.isUserUnlocked(user) }.getOrDefault(true)

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
        // Profile added, removed, paused, resumed or unlocked: none of these reach LauncherApps.Callback.
        val profileReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                trySend(AppChange.Refresh)
            }
        }
        launcherApps.registerCallback(callback, Handler(Looper.getMainLooper()))
        // Only the system sends these broadcasts, so the receiver needn't be open to other apps.
        ContextCompat.registerReceiver(context, profileReceiver, profileChangeFilter(), ContextCompat.RECEIVER_NOT_EXPORTED)
        awaitClose {
            launcherApps.unregisterCallback(callback)
            context.unregisterReceiver(profileReceiver)
        }
    }

    private fun profileChangeFilter() = IntentFilter().apply {
        addAction(Intent.ACTION_MANAGED_PROFILE_ADDED)
        addAction(Intent.ACTION_MANAGED_PROFILE_REMOVED)
        addAction(Intent.ACTION_MANAGED_PROFILE_AVAILABLE)
        addAction(Intent.ACTION_MANAGED_PROFILE_UNAVAILABLE)
        addAction(Intent.ACTION_MANAGED_PROFILE_UNLOCKED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // The same events for every kind of profile, including private space.
            addAction(Intent.ACTION_PROFILE_ADDED)
            addAction(Intent.ACTION_PROFILE_REMOVED)
            addAction(Intent.ACTION_PROFILE_AVAILABLE)
            addAction(Intent.ACTION_PROFILE_UNAVAILABLE)
        }
    }

    /**
     * Selects a sensible initial set of up to 5 essential app IDs to pin on first run.
     */
    override fun selectInitialPinnedIds(allApps: List<AppItem>): Set<String> {
        val preferredNames = listOf("Phone", "Messages", "Camera", "Settings", "Clock", "Notes", "Calendar")
        val personalApps = allApps.filter { it.isPersonalProfile }
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
        launcherApps.startMainActivity(app.componentName(), app.userHandle(), null, null)
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
