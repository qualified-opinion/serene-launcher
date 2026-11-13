package app.serene.launcher.data

import android.annotation.SuppressLint
import android.app.role.RoleManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi
import app.serene.launcher.domain.DefaultHome

/**
 * Reads whether Serene is the home app, and opens the screen that lets the user choose one.
 * Reading the status never asks the user. The dialog or the settings screen opens only from
 * [request] and [openHomeSettings], after a tap.
 */
object DefaultLauncherStatus {

    fun isDefaultLauncher(context: Context): Boolean = DefaultHome.isDefault(
        sdkInt = Build.VERSION.SDK_INT,
        roleHeld = isHomeRoleHeld(context),
        resolvedHomePackage = resolveHomePackage(context),
        ownPackageName = context.packageName
    )

    /**
     * Starts the home-role dialog when Android offers one. Otherwise opens home-app settings.
     * [launchRole] must deliver the dialog's result so a refusal can fall back to settings.
     */
    fun request(context: Context, launchRole: (Intent) -> Unit) {
        // The row is hidden once Serene is the home app. Ignore a tap that races that update.
        if (isDefaultLauncher(context)) return
        val action = DefaultHome.chooseAction(
            sdkInt = Build.VERSION.SDK_INT,
            roleAvailable = isHomeRoleAvailable(context),
            roleHeld = isHomeRoleHeld(context),
            roleRequestNotGranted = false,
            homeSettingsAvailable = true
        )
        when (action) {
            DefaultHome.Action.REQUEST_ROLE -> {
                val intent = roleRequestIntent(context)
                if (intent != null) launchRole(intent) else openHomeSettings(context)
            }
            // Home settings is tried first. If that screen can't be started, open Settings.
            DefaultHome.Action.OPEN_HOME_SETTINGS, DefaultHome.Action.OPEN_SETTINGS -> openHomeSettings(context)
        }
    }

    /**
     * Home-app settings, then the main Settings app if the phone has no home-app screen.
     * Used when the role dialog isn't available, and when it closes without making Serene the home app.
     */
    fun openHomeSettings(context: Context) {
        if (tryStart(context, Intent(Settings.ACTION_HOME_SETTINGS))) return
        // chooseAction maps "home settings isn't there" to the main Settings app.
        val fallback = DefaultHome.chooseAction(
            sdkInt = Build.VERSION.SDK_INT,
            roleAvailable = false,
            roleHeld = false,
            roleRequestNotGranted = true,
            homeSettingsAvailable = false
        )
        if (fallback == DefaultHome.Action.OPEN_SETTINGS) {
            tryStart(context, Intent(Settings.ACTION_SETTINGS))
        }
    }

    private fun tryStart(context: Context, intent: Intent): Boolean = try {
        context.startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        false
    } catch (_: SecurityException) {
        false
    }

    /** The system dialog intent, or null when the home role can't be requested. */
    private fun roleRequestIntent(context: Context): Intent? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        return roleRequestIntentApi29(context)
    }

    private fun isHomeRoleHeld(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
        return isHomeRoleHeldApi29(context)
    }

    private fun isHomeRoleAvailable(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
        return isHomeRoleAvailableApi29(context)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun isHomeRoleHeldApi29(context: Context): Boolean {
        val roles = context.getSystemService(RoleManager::class.java) ?: return false
        return roles.isRoleHeld(RoleManager.ROLE_HOME)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun isHomeRoleAvailableApi29(context: Context): Boolean {
        val roles = context.getSystemService(RoleManager::class.java) ?: return false
        return roles.isRoleAvailable(RoleManager.ROLE_HOME)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun roleRequestIntentApi29(context: Context): Intent? {
        val roles = context.getSystemService(RoleManager::class.java) ?: return null
        if (!roles.isRoleAvailable(RoleManager.ROLE_HOME) || roles.isRoleHeld(RoleManager.ROLE_HOME)) return null
        return try {
            roles.createRequestRoleIntent(RoleManager.ROLE_HOME)
        } catch (_: IllegalArgumentException) {
            // The role disappeared between the check and the request.
            null
        }
    }

    /**
     * Package of the preferred home app on Android 9 and older. Null on Android 10+, which uses
     * the home role instead, and null when the user hasn't chosen a home app.
     *
     * MATCH_DEFAULT_ONLY is how those versions name the preferred home app. The int overload of
     * resolveActivity is the one that exists there.
     */
    @Suppress("DEPRECATION")
    @SuppressLint("Deprecated")
    private fun resolveHomePackage(context: Context): String? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) return null
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val resolved = context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        return resolved?.activityInfo?.packageName
    }
}
