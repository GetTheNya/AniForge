package moe.GetTheNya.AniForge.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import moe.GetTheNya.AniForge.core.database.repository.SettingsRepository
import moe.GetTheNya.AniForge.core.database.settings.SettingsKeys
import moe.GetTheNya.AniForge.ui.localization.LocalizationService
import moe.GetTheNya.AniForge.core.database.sync.DatabaseManager
import moe.GetTheNya.AniForge.core.database.repository.AnimeRepository
import moe.GetTheNya.AniForge.core.database.repository.CatalogMetadata
import moe.GetTheNya.AniForge.core.database.settings.SettingsProvider
import moe.GetTheNya.AniForge.core.database.util.AppLogger
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val localizationService: LocalizationService,
    private val databaseManager: DatabaseManager,
    private val animeRepository: AnimeRepository,
    private val settingsProvider: SettingsProvider
) : ViewModel() {

    val isUpdating: StateFlow<Boolean> = databaseManager.isUpdating

    val catalogMetadata: StateFlow<CatalogMetadata> = animeRepository.getCatalogMetadataFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = CatalogMetadata(
                version = settingsProvider.getCatalogVersion(),
                activeSlot = settingsProvider.getActiveCatalogFileName()
            )
        )

    fun triggerForceDbUpdate() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                AppLogger.i("SettingsScreen", "Forced catalog database update triggered by user.")
                databaseManager.updateCatalogIfAvailable()
            } catch (e: Exception) {
                AppLogger.e("SettingsScreen", "Forced catalog database update failed.", e)
            }
        }
    }

    val preferUkTitles: StateFlow<Boolean> = settingsRepository.getSettingFlow(
        SettingsKeys.PREFER_UKRAINIAN,
        "true"
    )
    .map { it.toBoolean() }
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )

    val currentLanguage: StateFlow<String> = settingsRepository.getSettingFlow(
        SettingsKeys.LANGUAGE,
        "en"
    )
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "en"
    )

    val availableLanguages: StateFlow<Map<String, String>> = localizationService.availableLanguages

    fun setPreferUkTitles(value: Boolean) {
        viewModelScope.launch {
            settingsRepository.saveSetting(SettingsKeys.PREFER_UKRAINIAN, value.toString())
        }
    }

    fun setSelectedLanguage(langCode: String) {
        localizationService.setSelectedLanguage(langCode)
    }

    val gestureCenter: StateFlow<String> = settingsProvider.gestureCenterStr
    val gestureUp: StateFlow<String> = settingsProvider.gestureUpStr
    val gestureDown: StateFlow<String> = settingsProvider.gestureDownStr
    val gestureLeft: StateFlow<String> = settingsProvider.gestureLeftStr
    val gestureRight: StateFlow<String> = settingsProvider.gestureRightStr

    fun setGestureCenter(value: String) {
        settingsProvider.setGestureCenter(value)
    }

    fun setGestureUp(value: String) {
        settingsProvider.setGestureUp(value)
    }

    fun setGestureDown(value: String) {
        settingsProvider.setGestureDown(value)
    }

    fun setGestureLeft(value: String) {
        settingsProvider.setGestureLeft(value)
    }

    fun setGestureRight(value: String) {
        settingsProvider.setGestureRight(value)
    }
}
