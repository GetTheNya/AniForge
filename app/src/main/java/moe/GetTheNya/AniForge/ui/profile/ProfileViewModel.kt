package moe.GetTheNya.AniForge.ui.profile

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import moe.GetTheNya.AniForge.core.database.settings.SettingsProvider
import moe.GetTheNya.AniForge.core.database.util.AppLogger
import moe.GetTheNya.AniForge.core.database.util.LogEntry
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val settingsProvider: SettingsProvider
) : ViewModel() {

    val logs: StateFlow<List<LogEntry>> = AppLogger.logs

    fun clearLogs() {
        AppLogger.clear()
        AppLogger.i("ProfileScreen", "System logs cleared by user.")
    }
}
