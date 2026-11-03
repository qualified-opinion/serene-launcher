package app.serene.launcher.data

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import app.serene.launcher.SereneAccessibilityService

/** What happened when a Home gesture asked Android to lock the screen. */
enum class LockResult {
    LOCKED,
    /** The user hasn't turned on Serene's accessibility service, which does the locking. */
    NEEDS_SERVICE,
    /** Android 8.1 and older have no way for an app to lock the screen. */
    NOT_SUPPORTED
}

/** Phone-wide actions that Home gestures trigger, kept behind an interface so they can be faked. */
interface SystemActions {
    /** Pulls down the notification shade. False if Android didn't allow it. */
    fun openNotifications(): Boolean

    fun lockScreen(): LockResult

    /** True while Serene's accessibility service is turned on in the phone's settings. */
    val isLockServiceOn: Boolean
}

/** For tests and previews: nothing happens, as on a phone where Android refuses both actions. */
object NoSystemActions : SystemActions {
    override fun openNotifications() = false
    override fun lockScreen() = LockResult.NEEDS_SERVICE
    override val isLockServiceOn = false
}

class AndroidSystemActions(private val context: Context) : SystemActions {

    override val isLockServiceOn: Boolean
        get() = SereneAccessibilityService.isRunning

    override fun openNotifications(): Boolean {
        if (SereneAccessibilityService.perform(AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS)) return true
        return expandStatusBar()
    }

    /**
     * Without the accessibility service, use the status bar's own "expand" call, which launchers
     * have long used. It isn't public API, so any failure just reports that it didn't work.
     */
    @SuppressLint("WrongConstant")
    private fun expandStatusBar(): Boolean = runCatching {
        val statusBar = context.getSystemService("statusbar") ?: return false
        statusBar.javaClass.getMethod("expandNotificationsPanel").invoke(statusBar)
        true
    }.getOrDefault(false)

    override fun lockScreen(): LockResult = when {
        Build.VERSION.SDK_INT < Build.VERSION_CODES.P -> LockResult.NOT_SUPPORTED
        SereneAccessibilityService.perform(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN) -> LockResult.LOCKED
        else -> LockResult.NEEDS_SERVICE
    }
}
