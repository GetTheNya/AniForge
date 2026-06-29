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
import moe.GetTheNya.AniForge.core.database.util.BackupManager
import moe.GetTheNya.AniForge.core.database.util.BackupMetadata
import moe.GetTheNya.AniForge.ui.dashboard.UserTrackingRepository
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import moe.GetTheNya.AniForge.core.network.api.GitHubApiService
import moe.GetTheNya.AniForge.BuildConfig
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val localizationService: LocalizationService,
    private val databaseManager: DatabaseManager,
    private val animeRepository: AnimeRepository,
    private val settingsProvider: SettingsProvider,
    private val gitHubApiService: GitHubApiService,
    private val backupManager: BackupManager,
    private val userTrackingRepository: UserTrackingRepository
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

    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting.asStateFlow()

    private val _isRestoring = MutableStateFlow(false)
    val isRestoring: StateFlow<Boolean> = _isRestoring.asStateFlow()

    private val _isPreparingImport = MutableStateFlow(false)
    val isPreparingImport: StateFlow<Boolean> = _isPreparingImport.asStateFlow()

    private val _backupMetadata = MutableStateFlow<BackupMetadata?>(null)
    val backupMetadata: StateFlow<BackupMetadata?> = _backupMetadata.asStateFlow()

    private val _backupError = MutableStateFlow<String?>(null)
    val backupError: StateFlow<String?> = _backupError.asStateFlow()

    fun parseBackupFile(uri: android.net.Uri) {
        _isPreparingImport.value = true
        _backupError.value = null
        _backupMetadata.value = null
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val result = backupManager.parseBackupMetadata(uri)
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                _isPreparingImport.value = false
                result.onSuccess { metadata ->
                    _backupMetadata.value = metadata
                }
                .onFailure { exception ->
                    AppLogger.e("SettingsViewModel", "Failed to parse backup metadata", exception)
                    _backupError.value = exception.message ?: "Failed to parse backup file"
                }
            }
        }
    }

    @OptIn(coil.annotation.ExperimentalCoilApi::class)
    fun performRestore(
        uri: android.net.Uri,
        restoreSettings: Boolean,
        restoreTracking: Boolean,
        restoreCollections: Boolean,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _isRestoring.value = true
            _backupError.value = null
            
            val result = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                backupManager.restoreBackup(uri, restoreSettings, restoreTracking, restoreCollections)
            }
            if (result.isSuccess) {
                // 1. Recalculate watch time stats if tracking lists were restored
                if (restoreTracking) {
                    try {
                        userTrackingRepository.recalculateTotalWatchTime()
                        AppLogger.i("SettingsViewModel", "Watch time stats recalculated successfully during restore.")
                    } catch (e: Exception) {
                        AppLogger.e("SettingsViewModel", "Failed to recalculate watch time stats", e)
                    }
                }

                // 2. Clear Coil Image Cache
                try {
                    val imageLoader = coil.Coil.imageLoader(context)
                    imageLoader.memoryCache?.clear()
                    imageLoader.diskCache?.clear()
                    AppLogger.i("SettingsViewModel", "Coil image cache cleared.")
                } catch (e: Exception) {
                    AppLogger.e("SettingsViewModel", "Failed to clear Coil cache", e)
                }

                // 3. Show Toast and trigger UI success
                android.widget.Toast.makeText(
                    context,
                    "Import successful. Restarting AniForge to apply data...",
                    android.widget.Toast.LENGTH_SHORT
                ).show()

                _isRestoring.value = false
                onSuccess()

                try {
                    val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
                        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    }
                    context.startActivity(intent)
                    Runtime.getRuntime().exit(0)
                } catch (e: Exception) {
                    AppLogger.e("SettingsViewModel", "Failed to restart application", e)
                }
            } else {
                _isRestoring.value = false
                val exception = result.exceptionOrNull()
                AppLogger.e("SettingsViewModel", "Failed to restore backup", exception)
                _backupError.value = exception?.message ?: "Failed to restore database tables"
            }
        }
    }

    fun performExport(
        uri: android.net.Uri,
        includeSettings: Boolean,
        includeTracking: Boolean,
        includeCollections: Boolean,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _isExporting.value = true
            _backupError.value = null
            val result = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                backupManager.exportBackup(uri, includeSettings, includeTracking, includeCollections)
            }
            _isExporting.value = false
            result.onSuccess {
                onSuccess()
            }
            .onFailure { exception ->
                AppLogger.e("SettingsViewModel", "Failed to export backup", exception)
                _backupError.value = exception.message ?: "Failed to write backup package"
            }
        }
    }

    fun clearBackupError() {
        _backupError.value = null
    }

    fun clearParsedMetadata() {
        _backupMetadata.value = null
    }
}
