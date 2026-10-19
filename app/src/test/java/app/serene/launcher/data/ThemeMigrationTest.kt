package app.serene.launcher.data

import androidx.datastore.preferences.core.edit
import app.serene.launcher.model.ThemeMode
import app.serene.launcher.testing.InMemoryPreferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ThemeMigrationTest {

    @Test
    fun `someone who had dark theme on keeps it after updating`() = runTest {
        val dataStore = InMemoryPreferencesDataStore()
        dataStore.edit { it[LauncherPreferencesRepository.KEY_IS_DARK_THEME] = true }

        val prefs = LauncherPreferencesRepository(dataStore).preferencesFlow.first()

        assertEquals(ThemeMode.DARK, prefs.themeMode)
    }

    @Test
    fun `choosing a theme in Settings replaces the old switch`() = runTest {
        val dataStore = InMemoryPreferencesDataStore()
        dataStore.edit { it[LauncherPreferencesRepository.KEY_IS_DARK_THEME] = true }
        val repository = LauncherPreferencesRepository(dataStore)

        repository.setThemeMode(ThemeMode.SYSTEM)

        assertEquals(ThemeMode.SYSTEM, repository.preferencesFlow.first().themeMode)
    }
}
