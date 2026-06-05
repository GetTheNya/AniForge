package moe.GetTheNya.AniForge.core.database.settings

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import moe.GetTheNya.AniForge.core.database.repository.SettingsRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository
) {
    private val prefs = context.getSharedPreferences("aniforge_settings", Context.MODE_PRIVATE)
    private val scope = CoroutineScope(Dispatchers.IO)

    companion object {
        private const val KEY_ACTIVE_CATALOG = "active_catalog_slot"
        private const val KEY_CATALOG_VERSION = "catalog_version"
        private const val DEFAULT_CATALOG = "catalog_a.db"
    }

    val preferUkTitles: StateFlow<Boolean> = settingsRepository.getSettingFlow(
        SettingsKeys.PREFER_UKRAINIAN,
        "true"
    )
    .map { it.toBoolean() }
    .stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = true
    )

    val titleAnimStyleStr: StateFlow<String> = settingsRepository.getSettingFlow(
        SettingsKeys.TITLE_ANIM_STYLE,
        "DECODING"
    )
    .stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = "DECODING"
    )

    val subtitleAnimStyleStr: StateFlow<String> = settingsRepository.getSettingFlow(
        SettingsKeys.SUBTITLE_ANIM_STYLE,
        "BLUR_FADE"
    )
    .stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = "BLUR_FADE"
    )

    val contentAnimStyleStr: StateFlow<String> = settingsRepository.getSettingFlow(
        SettingsKeys.CONTENT_ANIM_STYLE,
        "POWER_UP"
    )
    .stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = "POWER_UP"
    )

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
        return preferUkTitles.value
    }

    fun setPreferUkTitles(value: Boolean) {
        scope.launch {
            settingsRepository.saveSetting(SettingsKeys.PREFER_UKRAINIAN, value.toString())
        }
    }

    fun setTitleAnimStyle(value: String) {
        scope.launch {
            settingsRepository.saveSetting(SettingsKeys.TITLE_ANIM_STYLE, value)
        }
    }

    fun setSubtitleAnimStyle(value: String) {
        scope.launch {
            settingsRepository.saveSetting(SettingsKeys.SUBTITLE_ANIM_STYLE, value)
        }
    }

    fun setContentAnimStyle(value: String) {
        scope.launch {
            settingsRepository.saveSetting(SettingsKeys.CONTENT_ANIM_STYLE, value)
        }
    }
}
