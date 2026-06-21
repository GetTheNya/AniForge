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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import moe.GetTheNya.AniForge.core.network.api.GitHubApiService
import moe.GetTheNya.AniForge.BuildConfig
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val localizationService: LocalizationService,
    private val databaseManager: DatabaseManager,
    private val animeRepository: AnimeRepository,
    private val settingsProvider: SettingsProvider,
    private val gitHubApiService: GitHubApiService
) : ViewModel() {

    enum class UpdateStatus {
        NOT_CHECKED,
        CHECKING,
        UP_TO_DATE,
        UPDATE_AVAILABLE,
        FAILED
    }

    private val _isCheckingForUpdates = MutableStateFlow(false)
    val isCheckingForUpdates: StateFlow<Boolean> = _isCheckingForUpdates.asStateFlow()

    private val _isUpdateAvailable = MutableStateFlow(false)
    val isUpdateAvailable: StateFlow<Boolean> = _isUpdateAvailable.asStateFlow()

    private val _updateHtmlUrl = MutableStateFlow<String?>(null)
    val updateHtmlUrl: StateFlow<String?> = _updateHtmlUrl.asStateFlow()

    private val _updateCheckStatus = MutableStateFlow(UpdateStatus.NOT_CHECKED)
    val updateCheckStatus: StateFlow<UpdateStatus> = _updateCheckStatus.asStateFlow()

    fun checkForUpdates() {
        viewModelScope.launch {
            _isCheckingForUpdates.value = true
            _updateCheckStatus.value = UpdateStatus.CHECKING
            try {
                val latestRelease = gitHubApiService.getLatestRelease()
                val remoteTag = latestRelease.tagName
                val remoteUrl = latestRelease.htmlUrl

                val cleanRemote = remoteTag.trim().removePrefix("v")
                val cleanLocal = BuildConfig.VERSION_NAME.trim().removePrefix("v")

                val isGreater = isVersionGreater(cleanRemote, cleanLocal)

                _updateHtmlUrl.value = remoteUrl
                _isUpdateAvailable.value = isGreater

                if (isGreater) {
                    _updateCheckStatus.value = UpdateStatus.UPDATE_AVAILABLE
                } else {
                    _updateCheckStatus.value = UpdateStatus.UP_TO_DATE
                }
            } catch (e: Exception) {
                AppLogger.e("SettingsViewModel", "Check for updates failed", e)
                _updateCheckStatus.value = UpdateStatus.FAILED
            } finally {
                _isCheckingForUpdates.value = false
            }
        }
    }

    private fun isVersionGreater(remote: String, local: String): Boolean {
        val remoteParts = remote.split(".").mapNotNull { it.toIntOrNull() }
        val localParts = local.split(".").mapNotNull { it.toIntOrNull() }
        val maxLength = maxOf(remoteParts.size, localParts.size)
        for (i in 0 until maxLength) {
            val r = remoteParts.getOrElse(i) { 0 }
            val l = localParts.getOrElse(i) { 0 }
            if (r > l) return true
            if (r < l) return false
        }
        return false
    }

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

    val preferUkTitles: StateFlow<Boolean> = settingsProvider.preferUkTitles

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
        settingsProvider.setPreferUkTitles(value)
    }

    val show18Plus: StateFlow<Boolean> = settingsProvider.show18Plus

    fun setShow18Plus(value: Boolean) {
        settingsProvider.setShow18Plus(value)
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
