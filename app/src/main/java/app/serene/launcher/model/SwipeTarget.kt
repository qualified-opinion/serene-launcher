package app.serene.launcher.model

/** Which way a horizontal swipe on Home goes. */
enum class SwipeSide { LEFT, RIGHT }

/** The app a swipe opens: its saved id, and the app itself while it is installed. */
data class SwipeTarget(val appId: String, val app: AppItem?)
