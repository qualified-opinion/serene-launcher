package app.serene.launcher.domain

/**
 * Whether this app is the home screen, and what a tap on "Make Serene my home screen" should do.
 *
 * The phone reads the home role and the resolved home package. These functions only compare those
 * facts, so the choice can be tested without Android.
 */
object DefaultHome {

    /** Android 10 (API 29), the first version with the home role. */
    const val ROLE_SDK = 29

    /** The next step after the user asks to make Serene the home screen. */
    enum class Action {
        /** Show the system dialog that asks for the home role. */
        REQUEST_ROLE,

        /** Open the phone's home-app settings. */
        OPEN_HOME_SETTINGS,

        /** Open the main Settings app, because the home-app screen isn't there. */
        OPEN_SETTINGS
    }

    /**
     * True when this app is the default home app.
     *
     * From [ROLE_SDK] up, [roleHeld] is the answer. Before that, the package that resolves the
     * home intent has to be [ownPackageName].
     */
    fun isDefault(
        sdkInt: Int,
        roleHeld: Boolean,
        resolvedHomePackage: String?,
        ownPackageName: String
    ): Boolean = if (sdkInt >= ROLE_SDK) {
        roleHeld
    } else {
        resolvedHomePackage == ownPackageName
    }

    /**
     * What a tap should do, or what to do after the role dialog closes without granting the role.
     *
     * The role dialog is only for Android 10 and later, and only when the home role exists, this
     * app doesn't hold it, and [roleRequestNotGranted] is false. Otherwise open home-app settings,
     * or the main Settings app when [homeSettingsAvailable] is false.
     */
    fun chooseAction(
        sdkInt: Int,
        roleAvailable: Boolean,
        roleHeld: Boolean,
        roleRequestNotGranted: Boolean = false,
        homeSettingsAvailable: Boolean = true
    ): Action {
        val askWithRole = sdkInt >= ROLE_SDK && roleAvailable && !roleHeld && !roleRequestNotGranted
        if (askWithRole) return Action.REQUEST_ROLE
        return if (homeSettingsAvailable) Action.OPEN_HOME_SETTINGS else Action.OPEN_SETTINGS
    }
}
