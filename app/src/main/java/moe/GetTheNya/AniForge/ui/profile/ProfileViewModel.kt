package moe.GetTheNya.AniForge.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import moe.GetTheNya.AniForge.core.database.dao.UserTrackingDao
import moe.GetTheNya.AniForge.core.database.settings.SettingsProvider
import moe.GetTheNya.AniForge.core.database.repository.BentoWidgetRepository
import moe.GetTheNya.AniForge.core.database.util.AppLogger
import moe.GetTheNya.AniForge.core.database.util.LogEntry
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val settingsProvider: SettingsProvider,
    private val userTrackingDao: UserTrackingDao,
    private val bentoWidgetRepository: BentoWidgetRepository
) : ViewModel() {

    val userStats: StateFlow<moe.GetTheNya.AniForge.core.database.entity.UserStatsEntity> = bentoWidgetRepository.observeUserStats
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), moe.GetTheNya.AniForge.core.database.entity.UserStatsEntity())

    val bentoStats: StateFlow<moe.GetTheNya.AniForge.core.model.BentoStatsData> = bentoWidgetRepository.bentoStatsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), moe.GetTheNya.AniForge.core.model.BentoStatsData())

    val logs: StateFlow<List<LogEntry>> = AppLogger.logs

    val stats: StateFlow<Map<String, Int>> = userTrackingDao.observeAllTracking()
        .map { list ->
            list.groupBy { it.watchStatus }
                .mapValues { it.value.size }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyMap()
        )

    fun clearLogs() {
        AppLogger.clear()
        AppLogger.i("ProfileScreen", "System logs cleared by user.")
    }
}
