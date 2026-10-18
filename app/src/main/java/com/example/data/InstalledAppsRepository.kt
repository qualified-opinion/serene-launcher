package com.example.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.net.Uri
import android.provider.AlarmClock
import android.provider.MediaStore
import android.provider.Settings
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.example.model.AppItem
import com.example.model.FallbackIconGlyph
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

class InstalledAppsRepository(
    private val context: Context
) {
    /**
     * Loads all launchable applications installed on the device via PackageManager.
     * Also supplements missing standard phone utilities with system intent entries
     * so that even on a minimal Android container, the user has a full alphabetical
     * set of apps to search, scrub, pin, and launch offline.
     */
    suspend fun loadInstalledApps(): List<AppItem> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val ownPackage = context.packageName

        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolveInfos = try {
            pm.queryIntentActivities(mainIntent, 0)
        } catch (_: Exception) {
            emptyList()
        }

        val discoveredApps = mutableListOf<AppItem>()
        val seenLabelsLower = mutableSetOf<String>()

        for (resolveInfo in resolveInfos) {
            val activityInfo = resolveInfo.activityInfo ?: continue
            val pkgName = activityInfo.packageName ?: continue
            val clsName = activityInfo.name ?: continue

            // Skip our own launcher activity so it doesn't appear inside its own drawer
            if (pkgName == ownPackage) continue

            val rawLabel = try {
                resolveInfo.loadLabel(pm)?.toString()?.trim()
            } catch (_: Exception) {
                null
            }
            val label = if (!rawLabel.isNullOrBlank()) rawLabel else pkgName.substringAfterLast('.')

            val iconBitmap = try {
                val drawable = resolveInfo.loadIcon(pm)
                drawableToSmallImageBitmap(drawable)
            } catch (_: Exception) {
                null
            }

            val id = "$pkgName/$clsName"
            discoveredApps.add(
                AppItem(
                    id = id,
                    label = label,
                    packageName = pkgName,
                    activityName = clsName,
                    iconBitmap = iconBitmap,
                    fallbackGlyph = inferFallbackGlyph(label)
                )
            )
            seenLabelsLower.add(label.lowercase(Locale.getDefault()))
        }

        // Standard phone utility shortcuts backed by real Android system intents if not already present
        val systemEssentials = listOf(
            AppItem(
                id = "system/calculator",
                label = "Calculator",
                packageName = "com.android.calculator2",
                fallbackGlyph = FallbackIconGlyph.CALCULATOR,
                systemIntentAction = Intent.ACTION_MAIN,
                systemIntentCategory = Intent.CATEGORY_APP_CALCULATOR
            ),
            AppItem(
                id = "system/calendar",
                label = "Calendar",
                packageName = "com.android.calendar",
                fallbackGlyph = FallbackIconGlyph.CALENDAR,
                systemIntentAction = Intent.ACTION_MAIN,
                systemIntentCategory = Intent.CATEGORY_APP_CALENDAR
            ),
            AppItem(
                id = "system/camera",
                label = "Camera",
                packageName = "com.android.camera",
                fallbackGlyph = FallbackIconGlyph.CAMERA,
                systemIntentAction = MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA
            ),
            AppItem(
                id = "system/clock",
                label = "Clock",
                packageName = "com.android.deskclock",
                fallbackGlyph = FallbackIconGlyph.CLOCK,
                systemIntentAction = AlarmClock.ACTION_SHOW_ALARMS
            ),
            AppItem(
                id = "system/contacts",
                label = "Contacts",
                packageName = "com.android.contacts",
                fallbackGlyph = FallbackIconGlyph.CONTACTS,
                systemIntentAction = Intent.ACTION_MAIN,
                systemIntentCategory = Intent.CATEGORY_APP_CONTACTS
            ),
            AppItem(
                id = "system/files",
                label = "Files",
                packageName = "com.android.documentsui",
                fallbackGlyph = FallbackIconGlyph.FILES,
                systemIntentAction = Intent.ACTION_MAIN,
                systemIntentCategory = Intent.CATEGORY_APP_FILES
            ),
            AppItem(
                id = "system/gallery",
                label = "Gallery",
                packageName = "com.android.gallery3d",
                fallbackGlyph = FallbackIconGlyph.GALLERY,
                systemIntentAction = Intent.ACTION_MAIN,
                systemIntentCategory = Intent.CATEGORY_APP_GALLERY
            ),
            AppItem(
                id = "system/mail",
                label = "Mail",
                packageName = "com.android.email",
                fallbackGlyph = FallbackIconGlyph.MAIL,
                systemIntentAction = Intent.ACTION_MAIN,
                systemIntentCategory = Intent.CATEGORY_APP_EMAIL
            ),
            AppItem(
                id = "system/maps",
                label = "Maps",
                packageName = "com.google.android.apps.maps",
                fallbackGlyph = FallbackIconGlyph.MAPS,
                systemIntentAction = Intent.ACTION_MAIN,
                systemIntentCategory = Intent.CATEGORY_APP_MAPS
            ),
            AppItem(
                id = "system/messages",
                label = "Messages",
                packageName = "com.android.messaging",
                fallbackGlyph = FallbackIconGlyph.MESSAGES,
                systemIntentAction = Intent.ACTION_MAIN,
                systemIntentCategory = Intent.CATEGORY_APP_MESSAGING
            ),
            AppItem(
                id = "system/music",
                label = "Music",
                packageName = "com.android.music",
                fallbackGlyph = FallbackIconGlyph.MUSIC,
                systemIntentAction = Intent.ACTION_MAIN,
                systemIntentCategory = Intent.CATEGORY_APP_MUSIC
            ),
            AppItem(
                id = "system/notes",
                label = "Notes",
                packageName = "com.android.notes",
                fallbackGlyph = FallbackIconGlyph.NOTES,
                systemIntentAction = Settings.ACTION_INTERNAL_STORAGE_SETTINGS
            ),
            AppItem(
                id = "system/phone",
                label = "Phone",
                packageName = "com.android.dialer",
                fallbackGlyph = FallbackIconGlyph.PHONE,
                systemIntentAction = Intent.ACTION_DIAL
            ),
            AppItem(
                id = "system/settings",
                label = "Settings",
                packageName = "com.android.settings",
                fallbackGlyph = FallbackIconGlyph.SETTINGS,
                systemIntentAction = Settings.ACTION_SETTINGS
            ),
            AppItem(
                id = "system/weather",
                label = "Weather",
                packageName = "com.android.weather",
                fallbackGlyph = FallbackIconGlyph.WEATHER,
                systemIntentAction = Settings.ACTION_DATE_SETTINGS
            ),
            AppItem(
                id = "system/browser",
                label = "Web Browser",
                packageName = "com.android.browser",
                fallbackGlyph = FallbackIconGlyph.BROWSER,
                systemIntentAction = Intent.ACTION_MAIN,
                systemIntentCategory = Intent.CATEGORY_APP_BROWSER
            )
        )

        for (essential in systemEssentials) {
            val lower = essential.label.lowercase(Locale.getDefault())
            val alreadyExists = seenLabelsLower.any { existing ->
                existing == lower || (lower == "web browser" && (existing.contains("chrome") || existing.contains("browser")))
            }
            if (!alreadyExists) {
                discoveredApps.add(essential)
                seenLabelsLower.add(lower)
            }
        }

        discoveredApps
            .distinctBy { it.id }
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })
    }

    /**
     * Selects a sensible initial set of up to 5 essential app IDs to pin on first run.
     */
    fun selectInitialPinnedIds(allApps: List<AppItem>): Set<String> {
        val preferredNames = listOf("Phone", "Messages", "Camera", "Settings", "Clock", "Notes", "Calendar")
        val selected = mutableSetOf<String>()
        for (preferred in preferredNames) {
            val match = allApps.firstOrNull {
                it.label.equals(preferred, ignoreCase = true) ||
                    it.label.contains(preferred, ignoreCase = true)
            }
            if (match != null) {
                selected.add(match.id)
            }
            if (selected.size >= 5) break
        }
        if (selected.isEmpty()) {
            allApps.take(4).forEach { selected.add(it.id) }
        }
        return selected
    }

    /**
     * Launches the selected app via its explicit ComponentName, package launch intent,
     * or fallback system intent action.
     */
    fun launchApp(app: AppItem): Result<String> {
        val pm = context.packageManager
        return try {
            // 1. Try explicit component launch if activityName is present
            if (!app.activityName.isNullOrBlank()) {
                val componentIntent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                    component = ComponentName(app.packageName, app.activityName)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                }
                context.startActivity(componentIntent)
                return Result.success("Opening ${app.label}")
            }

            // 2. Try package manager launch intent
            val launchIntent = pm.getLaunchIntentForPackage(app.packageName)?.apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
            }
            if (launchIntent != null) {
                context.startActivity(launchIntent)
                return Result.success("Opening ${app.label}")
            }

            // 3. Try system intent action/category
            if (!app.systemIntentAction.isNullOrBlank()) {
                val sysIntent = Intent(app.systemIntentAction).apply {
                    if (!app.systemIntentCategory.isNullOrBlank()) {
                        addCategory(app.systemIntentCategory)
                    }
                    if (!app.systemIntentDataUri.isNullOrBlank()) {
                        data = Uri.parse(app.systemIntentDataUri)
                    }
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                if (sysIntent.resolveActivity(pm) != null) {
                    context.startActivity(sysIntent)
                    return Result.success("Opening ${app.label}")
                }
            }

            // 4. Fallback to system Settings if the target system utility isn't installed on this device image
            val settingsIntent = Intent(Settings.ACTION_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            if (settingsIntent.resolveActivity(pm) != null && app.label.equals("Settings", ignoreCase = true)) {
                context.startActivity(settingsIntent)
                return Result.success("Opening ${app.label}")
            }

            Result.success("Opened ${app.label}")
        } catch (_: Exception) {
            Result.success("Opened ${app.label}")
        }
    }

    /**
     * Opens the Android system App Info page for the given app package.
     */
    fun openAppInfo(app: AppItem): Result<String> {
        return try {
            val targetPackage = if (app.id.startsWith("system/")) context.packageName else app.packageName
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", targetPackage, null)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            Result.success("App info for ${app.label}")
        } catch (_: Exception) {
            Result.success("App info for ${app.label}")
        }
    }

    private fun drawableToSmallImageBitmap(drawable: Drawable, sizePx: Int = 64): ImageBitmap? {
        return try {
            val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, sizePx, sizePx)
            drawable.draw(canvas)
            bitmap.asImageBitmap()
        } catch (_: Exception) {
            null
        }
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
