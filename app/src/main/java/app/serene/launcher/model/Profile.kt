package app.serene.launcher.model

/** The kind of Android user profile an app lives in. */
enum class ProfileKind {
    PERSONAL,
    WORK,
    /** Android 15+ private space. Its apps are kept out of sight while it is locked. */
    PRIVATE,
    /** A profile some phones create to run a second copy of an app. */
    CLONE,
    /** Any other profile Android reports, shown with a neutral tag. */
    OTHER
}

/** Whether a profile's apps could be read on the last scan. */
enum class ProfileState {
    /** Running and unlocked: the scan is the full, current list. */
    AVAILABLE,
    /** Turned off by the user ("Pause work apps"). The scan may be partial or empty. */
    PAUSED,
    /** Running but not unlocked yet. The scan may be partial or empty. */
    LOCKED,
    /** Android refused the scan, so the list says nothing about what is installed. */
    UNREADABLE
}

/** One profile's result from a scan of installed apps. */
data class ProfileScan(
    val serial: Long,
    val kind: ProfileKind,
    val state: ProfileState,
    val apps: List<AppItem>
)
