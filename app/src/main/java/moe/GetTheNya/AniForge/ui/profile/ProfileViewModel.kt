package moe.GetTheNya.AniForge.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import moe.GetTheNya.AniForge.core.database.settings.SettingsProvider
import moe.GetTheNya.AniForge.core.database.sync.DatabaseManager
import moe.GetTheNya.AniForge.core.database.util.AppLogger
import moe.GetTheNya.AniForge.core.database.util.LogEntry
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val settingsProvider: SettingsProvider,
    private val databaseManager: DatabaseManager
) : ViewModel() {

    val preferUkTitles: StateFlow<Boolean> = settingsProvider.preferUkTitles
    val logs: StateFlow<List<LogEntry>> = AppLogger.logs

    private val _isUpdating = MutableStateFlow(false)
    val isUpdating: StateFlow<Boolean> = _isUpdating.asStateFlow()

    fun setPreferUkTitles(value: Boolean) {
        settingsProvider.setPreferUkTitles(value)
    }

    fun triggerDatabaseUpdate() {
        if (_isUpdating.value) return
        viewModelScope.launch {
            _isUpdating.value = true
            try {
                AppLogger.i("ProfileScreen", "Manual catalog update triggered by user.")
                val result = databaseManager.updateCatalogIfAvailable()
                if (result) {
                    AppLogger.i("ProfileScreen", "Manual catalog update finished successfully.")
                } else {
                    AppLogger.i("ProfileScreen", "Manual catalog update check finished: No update needed.")
                }
            } catch (e: Exception) {
                AppLogger.e("ProfileScreen", "Manual catalog update failed.", e)
            } finally {
                _isUpdating.value = false
            }
        }
    }

    fun clearLogs() {
        AppLogger.clear()
        AppLogger.i("ProfileScreen", "System logs cleared by user.")
    }
}
