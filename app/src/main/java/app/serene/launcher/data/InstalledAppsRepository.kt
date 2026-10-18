package app.serene.launcher.data

import android.content.ComponentName
import android.content.Context
import android.content.pm.LauncherApps
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import app.serene.launcher.model.AppItem
import app.serene.launcher.model.FallbackIconGlyph
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

    override suspend fun loadInstalledApps(): List<AppItem> = withContext(Dispatchers.IO) {
        val ownPackage = context.packageName
        val myUser = Process.myUserHandle()

        userManager.userProfiles.flatMap { user ->
            val serial = userManager.getSerialNumberForUser(user)
            val isWork = user != myUser
            val activities = try {
                launcherApps.getActivityList(null, user)
            } catch (_: SecurityException) {
                emptyList()
            }
            activities
                .filterNot { it.componentName.packageName == ownPackage }
                .map { info ->
                    val component = info.componentName
                    val label = info.label?.toString()?.trim().takeUnless { it.isNullOrBlank() }
                        ?: component.packageName.substringAfterLast('.')
                    AppItem(
                        id = appId(component, serial, isWork),
                        label = label,
                        packageName = component.packageName,
                        activityName = component.className,
                        // The badged icon carries the briefcase overlay for work apps.
                        iconBitmap = runCatching { drawableToSmallImageBitmap(info.getBadgedIcon(0)) }.getOrNull(),
                        fallbackGlyph = inferFallbackGlyph(label),
                        profileSerial = serial,
                        isWorkProfile = isWork
                    )
                }
        }
            .distinctBy { it.id }
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })
    }

    override fun appChanges(): Flow<Unit> = callbackFlow {
        val callback = object : LauncherApps.Callback() {
            override fun onPackageRemoved(packageName: String, user: UserHandle) {
                trySend(Unit)
            }

            override fun onPackageAdded(packageName: String, user: UserHandle) {
                trySend(Unit)
            }

            override fun onPackageChanged(packageName: String, user: UserHandle) {
                trySend(Unit)
            }

            override fun onPackagesAvailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) {
                trySend(Unit)
            }

            override fun onPackagesUnavailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) {
                trySend(Unit)
            }
        }
        launcherApps.registerCallback(callback, Handler(Looper.getMainLooper()))
        awaitClose { launcherApps.unregisterCallback(callback) }
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

    private fun drawableToSmallImageBitmap(drawable: Drawable, sizePx: Int = 64): ImageBitmap {
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, sizePx, sizePx)
        drawable.draw(canvas)
        return bitmap.asImageBitmap()
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
