package app.serene.launcher.testing

import app.serene.launcher.data.LauncherPreferencesRepository
import app.serene.launcher.data.createLauncherPreferencesStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.TestScope
import java.io.File

/** The test scope closes the store before JUnit removes its temporary file. */
fun TestScope.temporaryPreferences(file: File) = LauncherPreferencesRepository(temporaryPreferencesStore(file))

fun TestScope.temporaryPreferencesStore(file: File, scope: CoroutineScope = backgroundScope) =
    createLauncherPreferencesStore(produceFile = { file }, scope = scope)
