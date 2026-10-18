package app.serene.launcher.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.UserHandle
import android.os.UserManager
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import app.serene.launcher.model.AppItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * A rendered app icon. [isMonochrome] is true when it came from the app's own monochrome layer
 * (Android 13+), which should be tinted; otherwise it is the colour icon, to be desaturated.
 */
class AppIcon(val bitmap: ImageBitmap, val isMonochrome: Boolean)

/**
 * Loads app icons on demand and keeps recently shown ones in a memory cache, so the app list
 * itself carries no bitmaps. Cached icons for a package are dropped when that package changes.
 */
class AppIconLoader private constructor(context: Context) {

    private val launcherApps = context.getSystemService(LauncherApps::class.java)
    private val userManager = context.getSystemService(UserManager::class.java)
    private val densityDpi = context.resources.displayMetrics.densityDpi

    private val cache = object : LruCache<String, AppIcon>(cacheSizeKb()) {
        override fun sizeOf(key: String, value: AppIcon): Int =
            value.bitmap.asAndroidBitmap().byteCount / 1024
    }

    init {
        launcherApps.registerCallback(object : LauncherApps.Callback() {
            override fun onPackageRemoved(packageName: String, user: UserHandle) = evictPackage(packageName)
            override fun onPackageAdded(packageName: String, user: UserHandle) = evictPackage(packageName)
            override fun onPackageChanged(packageName: String, user: UserHandle) = evictPackage(packageName)
            override fun onPackagesAvailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) =
                packageNames.forEach(::evictPackage)
            override fun onPackagesUnavailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) =
                packageNames.forEach(::evictPackage)
        }, Handler(Looper.getMainLooper()))
    }

    fun cached(app: AppItem, sizePx: Int): AppIcon? = cache.get(key(app, sizePx))

    /** Returns the icon for [app] at [sizePx], or null if the app or its icon can't be found. */
    suspend fun load(app: AppItem, sizePx: Int): AppIcon? {
        cached(app, sizePx)?.let { return it }
        return withContext(Dispatchers.IO) {
            runCatching { render(app, sizePx) }.getOrNull()
        }?.also { cache.put(key(app, sizePx), it) }
    }

    private fun render(app: AppItem, sizePx: Int): AppIcon? {
        val activity = app.activityName ?: return null
        val user = userManager.getUserForSerialNumber(app.profileSerial) ?: return null
        val intent = Intent(Intent.ACTION_MAIN).setComponent(ComponentName(app.packageName, activity))
        val info = launcherApps.resolveActivity(intent, user) ?: return null

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val monochrome = (info.getIcon(densityDpi) as? AdaptiveIconDrawable)?.monochrome
            if (monochrome != null) {
                return AppIcon(drawAdaptiveLayer(monochrome, sizePx), isMonochrome = true)
            }
        }
        // The badged icon carries the briefcase overlay for work apps.
        return AppIcon(draw(info.getBadgedIcon(densityDpi), sizePx), isMonochrome = false)
    }

    private fun evictPackage(packageName: String) {
        cache.snapshot().keys
            .filter { it.startsWith("$packageName/") }
            .forEach { cache.remove(it) }
    }

    private fun key(app: AppItem, sizePx: Int) = "${app.id}#$sizePx"

    private fun draw(drawable: Drawable, sizePx: Int): ImageBitmap {
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        drawable.setBounds(0, 0, sizePx, sizePx)
        drawable.draw(Canvas(bitmap))
        return bitmap.asImageBitmap()
    }

    /**
     * Adaptive icon layers are 108dp with the visible content in the middle 72dp, so the layer is
     * drawn larger than the bitmap to make that safe zone fill it.
     */
    private fun drawAdaptiveLayer(layer: Drawable, sizePx: Int): ImageBitmap {
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val inset = sizePx / 4
        layer.setBounds(-inset, -inset, sizePx + inset, sizePx + inset)
        layer.draw(Canvas(bitmap))
        return bitmap.asImageBitmap()
    }

    companion object {
        @Volatile
        private var instance: AppIconLoader? = null

        fun get(context: Context): AppIconLoader =
            instance ?: synchronized(this) {
                instance ?: AppIconLoader(context.applicationContext).also { instance = it }
            }

        /** An eighth of the app's heap, which holds several hundred small icons. */
        private fun cacheSizeKb(): Int = (Runtime.getRuntime().maxMemory() / 1024 / 8).toInt()
    }
}
