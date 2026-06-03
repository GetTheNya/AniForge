package moe.GetTheNya.AniForge.core.database.settings

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("aniforge_settings", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_ACTIVE_CATALOG = "active_catalog_slot"
        private const val KEY_CATALOG_VERSION = "catalog_version"
        private const val KEY_PREFER_UKRAINIAN_TITLES = "prefer_ukrainian_titles"
        private const val DEFAULT_CATALOG = "catalog_a.db"
    }

    private val _preferUkTitles = MutableStateFlow(getPreferUkTitles())
    val preferUkTitles: StateFlow<Boolean> = _preferUkTitles.asStateFlow()

    /**
     * Returns the name of the currently active catalog file (e.g. "catalog_a.db").
     */
    fun getActiveCatalogFileName(): String {
        return prefs.getString(KEY_ACTIVE_CATALOG, DEFAULT_CATALOG) ?: DEFAULT_CATALOG
    }

    /**
     * Returns the name of the standby database file (e.g. "catalog_b.db" if A is active).
     */
    fun getStandbyCatalogFileName(): String {
        val current = getActiveCatalogFileName()
        return if (current == "catalog_a.db") "catalog_b.db" else "catalog_a.db"
    }

    /**
     * Gets the currently stored catalog database version/timestamp.
     */
    fun getCatalogVersion(): Long {
        return prefs.getLong(KEY_CATALOG_VERSION, 0L)
    }

    /**
     * Swaps the active slot to the specified file and updates the current version number.
     */
    fun setActiveCatalog(fileName: String, version: Long) {
        prefs.edit()
            .putString(KEY_ACTIVE_CATALOG, fileName)
            .putLong(KEY_CATALOG_VERSION, version)
            .apply()
    }

    fun getPreferUkTitles(): Boolean {
        return prefs.getBoolean(KEY_PREFER_UKRAINIAN_TITLES, true)
    }

    fun setPreferUkTitles(value: Boolean) {
        prefs.edit().putBoolean(KEY_PREFER_UKRAINIAN_TITLES, value).apply()
        _preferUkTitles.value = value
    }
}
