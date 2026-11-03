package app.serene.launcher.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/** Writes [text] to the file the user picked, replacing what was in it. */
internal suspend fun Context.writeTextTo(uri: Uri, text: String) = withContext(Dispatchers.IO) {
    requireNotNull(contentResolver.openOutputStream(uri, "wt")) { "Can't write $uri" }.use { output ->
        output.write(text.toByteArray(Charsets.UTF_8))
    }
}

/**
 * Reads the file the user picked as text, or returns null when it can't be opened or is bigger
 * than a settings backup could be (a photo or video picked by mistake).
 */
internal suspend fun Context.readSmallTextFrom(uri: Uri): String? = withContext(Dispatchers.IO) {
    contentResolver.openInputStream(uri)?.use { input ->
        val bytes = ByteArrayOutputStream()
        val chunk = ByteArray(8 * 1024)
        while (true) {
            val read = input.read(chunk)
            if (read < 0) break
            if (bytes.size() + read > SettingsBackup.MAX_BYTES) return@use null
            bytes.write(chunk, 0, read)
        }
        bytes.toString(Charsets.UTF_8.name())
    }
}
