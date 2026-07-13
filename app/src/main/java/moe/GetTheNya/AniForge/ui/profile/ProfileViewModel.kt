package moe.GetTheNya.AniForge.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import moe.GetTheNya.AniForge.core.database.dao.UserTrackingDao
import moe.GetTheNya.AniForge.core.database.settings.SettingsProvider
import moe.GetTheNya.AniForge.core.database.repository.BentoWidgetRepository
import moe.GetTheNya.AniForge.core.database.util.AppLogger
import moe.GetTheNya.AniForge.core.database.util.LogEntry
import moe.GetTheNya.AniForge.core.network.AuthRepository
import moe.GetTheNya.AniForge.core.network.ProfileRepository
import moe.GetTheNya.AniForge.core.network.UserInfo
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val settingsProvider: SettingsProvider,
    private val userTrackingDao: UserTrackingDao,
    private val bentoWidgetRepository: BentoWidgetRepository,
    private val authRepository: AuthRepository,
    private val profileRepository: ProfileRepository
) : ViewModel() {

    fun uploadAvatar(byteArray: ByteArray, onResult: (Result<Unit>) -> Unit = {}) {
        viewModelScope.launch {
            val result = profileRepository.uploadUserAvatar(byteArray)
            if (result.isSuccess) {
                authRepository.fetchUserProfile()
            }
            onResult(result)
        }
    }

    init {
        viewModelScope.launch {
            var lastLoggedUserId: String? = null
            var lastLoggedUsername: String? = null
            authRepository.currentUser.collect { user ->
                if (user != null) {
                    if (user.id != lastLoggedUserId || user.username != lastLoggedUsername) {
                        AppLogger.i("Auth", "User logged in: ${user.username} (${user.id})")
                        lastLoggedUserId = user.id
                        lastLoggedUsername = user.username
                    }
                } else {
                    if (lastLoggedUserId != null) {
                        AppLogger.i("Auth", "No active user session (logged out)")
                        lastLoggedUserId = null
                        lastLoggedUsername = null
                    }
                }
            }
        }
    }

    val currentUser: StateFlow<UserInfo?> = authRepository.currentUser
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = authRepository.currentSessionUser
        )

    fun signInWithIdToken(idToken: String, onResult: (Result<Unit>) -> Unit = {}) {
        AppLogger.i("Auth", "Attempting Supabase sign-in with Google ID Token...")
        viewModelScope.launch {
            val result = authRepository.signInWithIdToken(idToken)
            result.fold(
                onSuccess = {
                    AppLogger.i("Auth", "Supabase sign-in completed successfully.")
                },
                onFailure = { throwable ->
                    AppLogger.e("Auth", "Supabase sign-in failed.", throwable)
                }
            )
            onResult(result)
        }
    }

    fun signOut(onResult: (Result<Unit>) -> Unit = {}) {
        AppLogger.i("Auth", "Attempting sign-out...")
        viewModelScope.launch {
            val result = authRepository.signOut()
            result.fold(
                onSuccess = {
                    AppLogger.i("Auth", "Sign-out completed successfully.")
                },
                onFailure = { throwable ->
                    AppLogger.e("Auth", "Sign-out failed.", throwable)
                }
            )
            onResult(result)
        }
    }

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
