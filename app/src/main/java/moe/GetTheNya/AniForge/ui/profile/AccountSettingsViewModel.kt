package moe.GetTheNya.AniForge.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import moe.GetTheNya.AniForge.core.network.AuthRepository
import moe.GetTheNya.AniForge.core.network.UserInfo
import moe.GetTheNya.AniForge.core.network.UsernameTakenException
import moe.GetTheNya.AniForge.core.network.UsernameFormatException
import javax.inject.Inject

sealed interface SaveError {
    object None : SaveError
    object UsernameTaken : SaveError
    object ServerRejected : SaveError
    object NetworkError : SaveError
}

@HiltViewModel
class AccountSettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _username = MutableStateFlow("")
    val username: StateFlow<String> = _username.asStateFlow()

    private val _validationError = MutableStateFlow<UsernameValidator.ErrorType?>(null)
    val validationError: StateFlow<UsernameValidator.ErrorType?> = _validationError.asStateFlow()

    private val _saveError = MutableStateFlow<SaveError>(SaveError.None)
    val saveError: StateFlow<SaveError> = _saveError.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _saveSuccess = MutableStateFlow(false)
    val saveSuccess: StateFlow<Boolean> = _saveSuccess.asStateFlow()

    val currentUser: StateFlow<UserInfo?> = authRepository.currentUser
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = authRepository.currentSessionUser
        )

    init {
        // Initialize username from current authenticated user session if available
        authRepository.currentSessionUser?.let { user ->
            _username.value = user.username
            performValidation(user.username)
        }
        
        // Keep updated if auth session updates
        viewModelScope.launch {
            authRepository.currentUser.collect { user ->
                if (user != null && _username.value.isEmpty()) {
                    _username.value = user.username
                    performValidation(user.username)
                }
            }
        }
    }

    fun onUsernameChanged(newVal: String) {
        // Block spaces dynamically
        val filtered = UsernameValidator.filterInput(newVal)
        _username.value = filtered
        _saveError.value = SaveError.None
        _saveSuccess.value = false
        performValidation(filtered)
    }

    private fun performValidation(name: String) {
        when (val res = UsernameValidator.validate(name)) {
            is UsernameValidator.ValidationResult.Valid -> {
                _validationError.value = null
            }
            is UsernameValidator.ValidationResult.Invalid -> {
                _validationError.value = res.errorType
            }
        }
    }

    fun saveUsername() {
        val currentName = _username.value
        if (UsernameValidator.validate(currentName) !is UsernameValidator.ValidationResult.Valid) return

        val user = authRepository.currentSessionUser ?: return
        
        viewModelScope.launch {
            _isSaving.value = true
            _saveError.value = SaveError.None
            _saveSuccess.value = false
            
            val res = authRepository.updateUsername(user.id, currentName)
            res.fold(
                onSuccess = {
                    _saveSuccess.value = true
                },
                onFailure = { error ->
                    _saveSuccess.value = false
                    when (error) {
                        is UsernameTakenException -> {
                            _saveError.value = SaveError.UsernameTaken
                        }
                        is UsernameFormatException -> {
                            _saveError.value = SaveError.ServerRejected
                        }
                        else -> {
                            _saveError.value = SaveError.NetworkError
                        }
                    }
                }
            )
            _isSaving.value = false
        }
    }

    fun clearSaveSuccess() {
        _saveSuccess.value = false
    }

    fun signOut(onResult: (Result<Unit>) -> Unit = {}) {
        viewModelScope.launch {
            val result = authRepository.signOut()
            onResult(result)
        }
    }
}
