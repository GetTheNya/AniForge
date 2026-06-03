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
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val localizationService: LocalizationService
) : ViewModel() {

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
}
