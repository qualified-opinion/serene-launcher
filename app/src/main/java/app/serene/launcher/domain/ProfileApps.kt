package app.serene.launcher.domain

import app.serene.launcher.model.AppItem
import app.serene.launcher.model.ProfileKind
import app.serene.launcher.model.ProfileScan
import app.serene.launcher.model.ProfileState

/** Pure rules for apps spread over several Android profiles, kept free of Android types for tests. */
object ProfileApps {

    /**
     * Builds the app list from a new scan. A profile that is paused, locked or couldn't be read
     * keeps the apps it had on the previous scan, so a paused work profile doesn't look empty.
     * Private space is the exception: its apps disappear while it is locked. Profiles missing
     * from [scans] are gone from the phone, and so are their apps.
     */
    fun merge(previous: List<AppItem>, scans: List<ProfileScan>): List<AppItem> =
        scans.flatMap { appsFor(it, previous) }
            .distinctBy { it.id }
            .let { AppSearch.sort(it) }

    private fun appsFor(scan: ProfileScan, previous: List<AppItem>): List<AppItem> {
        val paused = scan.state == ProfileState.PAUSED
        val scanned = scan.apps.map { it.copy(profileKind = scan.kind, isProfilePaused = paused) }
        if (scan.state == ProfileState.AVAILABLE) return scanned
        if (scan.kind == ProfileKind.PRIVATE) return emptyList()

        val scannedIds = scanned.mapTo(HashSet()) { it.id }
        val kept = previous
            .filter { it.profileSerial == scan.serial && it.id !in scannedIds }
            .map { it.copy(profileKind = scan.kind, isProfilePaused = paused) }
        return scanned + kept
    }

    /**
     * When every app in [matches] is the same app installed in more than one profile, returns its
     * personal copy so typing can still open it. Returns null otherwise.
     */
    fun personalCopyOfOneApp(matches: List<AppItem>): AppItem? {
        if (matches.size < 2) return null
        val first = matches.first()
        val sameApp = matches.all { it.packageName == first.packageName && it.activityName == first.activityName }
        return if (sameApp) matches.singleOrNull { it.isPersonalProfile } else null
    }

    /**
     * Maps saved ids of a work profile that no longer exists on this phone (for example, settings
     * brought over from an old phone, whose work profile had another serial number) to the same
     * app in the phone's current work profile.
     *
     * Nothing is moved unless there is exactly one work profile, it is available, its scan found
     * apps, and the old profile is neither in the scan nor known to Android. An id only moves when
     * the same app is installed in the current work profile.
     */
    fun staleWorkIdReplacements(
        storedIds: Set<String>,
        scans: List<ProfileScan>,
        profileExists: (Long) -> Boolean
    ): Map<String, String> {
        val work = scans.singleOrNull { it.kind == ProfileKind.WORK } ?: return emptyMap()
        if (work.state != ProfileState.AVAILABLE || work.apps.isEmpty()) return emptyMap()
        val workIds = work.apps.mapTo(HashSet()) { it.id }
        val scannedSerials = scans.mapTo(HashSet()) { it.serial }

        return storedIds.mapNotNull { id ->
            val at = id.lastIndexOf('@')
            if (at <= 0) return@mapNotNull null
            val serial = id.substring(at + 1).toLongOrNull() ?: return@mapNotNull null
            if (serial in scannedSerials || profileExists(serial)) return@mapNotNull null
            val target = "${id.substring(0, at)}@${work.serial}"
            if (target in workIds) id to target else null
        }.toMap()
    }

    /**
     * The profile kind for an Android user type string (Android 15+). Unknown or missing types on
     * older phones count as work, which is the only kind of extra profile they normally have.
     */
    fun kindForUserType(userType: String?): ProfileKind = when (userType) {
        null, USER_TYPE_MANAGED -> ProfileKind.WORK
        USER_TYPE_PRIVATE -> ProfileKind.PRIVATE
        USER_TYPE_CLONE -> ProfileKind.CLONE
        else -> ProfileKind.OTHER
    }

    // The values of UserManager.USER_TYPE_PROFILE_*, written out so they compile on every API level.
    const val USER_TYPE_MANAGED = "android.os.usertype.profile.MANAGED"
    const val USER_TYPE_PRIVATE = "android.os.usertype.profile.PRIVATE"
    const val USER_TYPE_CLONE = "android.os.usertype.profile.CLONE"
}
