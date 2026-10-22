package app.serene.launcher.testing

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import app.serene.launcher.data.LauncherPreferencesRepository
import app.serene.launcher.data.settingsCorruptionHandler
import kotlinx.coroutines.test.TestScope
import java.io.File

/** The test scope closes the store before JUnit removes its temporary file. */
fun TestScope.temporaryPreferences(file: File) = LauncherPreferencesRepository(temporaryPreferencesStore(file))

fun TestScope.temporaryPreferencesStore(file: File) =
    PreferenceDataStoreFactory.create(
        corruptionHandler = settingsCorruptionHandler,
        scope = backgroundScope,
        produceFile = { file }
    )
