package moe.GetTheNya.AniForge.ui.social

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import moe.GetTheNya.AniForge.core.network.AuthRepository
import moe.GetTheNya.AniForge.core.network.FriendshipDto
import moe.GetTheNya.AniForge.core.network.SocialRepository
import moe.GetTheNya.AniForge.core.network.UserProfileDto
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class SocialViewModel @Inject constructor(
    private val socialRepository: SocialRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val currentUserId: String?
        get() = authRepository.currentSessionUser?.id

    // Search query state
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    // Friends list state
    private val _friends = MutableStateFlow<List<UserProfileDto>>(emptyList())
    val friends = _friends.asStateFlow()

    // Search results state
    private val _searchResults = MutableStateFlow<List<UserProfileDto>>(emptyList())
    val searchResults = combine(_searchResults, friends) { results, friendsList ->
        val friendIds = friendsList.map { it.id }.toSet()
        results.filter { it.id !in friendIds }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Incoming requests state
    private val _incomingRequests = MutableStateFlow<List<UserProfileDto>>(emptyList())
    val incomingRequests = _incomingRequests.asStateFlow()

    // Outgoing requests state
    private val _sentRequests = MutableStateFlow<List<UserProfileDto>>(emptyList())
    val sentRequests = _sentRequests.asStateFlow()

    // Counter flow for unread incoming pending friend requests
    val incomingRequestsCount: StateFlow<Int> = _incomingRequests
        .map { it.size }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    // All my relationships/friendships mapping for search action buttons
    private val _myFriendships = MutableStateFlow<List<FriendshipDto>>(emptyList())
    val myFriendships = _myFriendships.asStateFlow()

    // Full page loading state (for initial loads)
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    // Localized action button loading states: maps user ID -> Boolean
    private val _actionLoadingStates = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val actionLoadingStates = _actionLoadingStates.asStateFlow()

    // Toast/Error message shared flow
    private val _toastEvents = MutableSharedFlow<ToastEvent>()
    val toastEvents = _toastEvents.asSharedFlow()

    init {
        // Setup search query debounce (300ms)
        _searchQuery
            .debounce(300)
            .distinctUntilChanged()
            .onEach { query ->
                val currentId = currentUserId
                if (query.isBlank() || currentId == null) {
                    _searchResults.value = emptyList()
                } else {
                    val results = socialRepository.searchUsers(query, currentId)
                    _searchResults.value = results
                }
            }
            .launchIn(viewModelScope)

        // Observe current user changes to trigger refreshes or clear states
        viewModelScope.launch {
            authRepository.currentUser.collect { user ->
                if (user != null) {
                    refreshAll()
                } else {
                    _friends.value = emptyList()
                    _incomingRequests.value = emptyList()
                    _sentRequests.value = emptyList()
                    _myFriendships.value = emptyList()
                }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun refreshAll() {
        val currentId = currentUserId ?: return
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                // Fetch friends, requests and all user's relationships in parallel
                val friendsList = socialRepository.getFriends(currentId)
                val incomingList = socialRepository.getIncomingRequests(currentId)
                val sentList = socialRepository.getOutgoingRequests(currentId)
                val friendshipsList = socialRepository.getMyFriendships(currentId)

                _friends.value = friendsList
                _incomingRequests.value = incomingList
                _sentRequests.value = sentList
                _myFriendships.value = friendshipsList
            } catch (e: Exception) {
                emitToast(ToastEvent.Error(e.message ?: "Unknown error"))
            } finally {
                _isRefreshing.value = false
            }
        }
    }


    private fun setActionLoading(userId: String, isLoading: Boolean) {
        _actionLoadingStates.update { map ->
            map.toMutableMap().apply {
                if (isLoading) {
                    put(userId, true)
                } else {
                    remove(userId)
                }
            }
        }
    }

    fun sendRequest(receiverId: String) {
        viewModelScope.launch {
            setActionLoading(receiverId, true)
            val result = socialRepository.sendFriendRequest(receiverId)
            result.fold(
                onSuccess = {
                    emitToast(ToastEvent.RequestSent)
                    // Refresh friendships to update search results button status
                    refreshFriendshipsOnly()
                },
                onFailure = { error ->
                    emitToast(ToastEvent.Error(error.message ?: "Failed to send request"))
                }
            )
            setActionLoading(receiverId, false)
        }
    }

    fun acceptRequest(senderId: String) {
        val currentId = currentUserId ?: return
        viewModelScope.launch {
            setActionLoading(senderId, true)
            val result = socialRepository.acceptFriendRequest(senderId, currentId)
            result.fold(
                onSuccess = {
                    emitToast(ToastEvent.RequestAccepted)
                    refreshAll()
                },
                onFailure = { error ->
                    emitToast(ToastEvent.Error(error.message ?: "Failed to accept request"))
                }
            )
            setActionLoading(senderId, false)
        }
    }

    fun declineOrRemoveRequest(otherUserId: String) {
        val currentId = currentUserId ?: return
        viewModelScope.launch {
            setActionLoading(otherUserId, true)
            val result = socialRepository.declineOrRemoveFriendship(currentId, otherUserId)
            result.fold(
                onSuccess = {
                    // Check if it was an incoming request or friend removal for toast description
                    val wasFriend = _friends.value.any { it.id == otherUserId }
                    if (wasFriend) {
                        emitToast(ToastEvent.FriendRemoved)
                    } else {
                        emitToast(ToastEvent.RequestDeclined)
                    }
                    refreshAll()
                },
                onFailure = { error ->
                    emitToast(ToastEvent.Error(error.message ?: "Operation failed"))
                }
            )
            setActionLoading(otherUserId, false)
        }
    }

    private suspend fun refreshFriendshipsOnly() {
        val currentId = currentUserId ?: return
        try {
            val friendshipsList = socialRepository.getMyFriendships(currentId)
            _myFriendships.value = friendshipsList
        } catch (e: Exception) {
            // Suppress background sync errors
        }
    }

    private fun emitToast(event: ToastEvent) {
        viewModelScope.launch {
            _toastEvents.emit(event)
        }
    }
}

sealed interface ToastEvent {
    data object RequestSent : ToastEvent
    data object RequestAccepted : ToastEvent
    data object RequestDeclined : ToastEvent
    data object FriendRemoved : ToastEvent
    data class Error(val message: String) : ToastEvent
}
