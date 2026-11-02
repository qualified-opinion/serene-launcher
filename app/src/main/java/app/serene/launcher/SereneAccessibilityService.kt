package app.serene.launcher

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import java.lang.ref.WeakReference

/**
 * The opt-in service that lets a Home gesture lock the screen or open notifications. The user turns
 * it on in the phone's Accessibility settings. It asks for no events and can't read what is on the
 * screen (see res/xml/accessibility_service_config.xml): it only presses those two system buttons.
 */
class SereneAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        current = WeakReference(this)
    }

    override fun onUnbind(intent: Intent?): Boolean {
        current = null
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        current = null
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    companion object {
        @Volatile private var current: WeakReference<SereneAccessibilityService>? = null

        val isRunning: Boolean get() = current?.get() != null

        /** Runs one of AccessibilityService's GLOBAL_ACTION_* actions. False if the service is off. */
        fun perform(action: Int): Boolean = current?.get()?.performGlobalAction(action) ?: false
    }
}
