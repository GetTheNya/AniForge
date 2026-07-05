package moe.GetTheNya.AniForge.ui.social

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import moe.GetTheNya.AniForge.core.database.repository.AnimeRepository
import moe.GetTheNya.AniForge.core.model.Anime
import moe.GetTheNya.AniForge.core.network.SocialRepository
import moe.GetTheNya.AniForge.core.network.SupabaseUserTrackingDto
import javax.inject.Inject

data class FriendTrackingItem(
    val tracking: SupabaseUserTrackingDto,
    val anime: Anime?
)

@HiltViewModel
class SharedProfileViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val socialRepository: SocialRepository,
    private val animeRepository: AnimeRepository
) : ViewModel() {

    val userId: String = savedStateHandle.get<String>("userId") ?: ""
    val username: String = savedStateHandle.get<String>("username") ?: ""

    private val _trackingList = MutableStateFlow<List<FriendTrackingItem>>(emptyList())
    val trackingList = _trackingList.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    init {
        loadProfileData()
    }

    fun loadProfileData() {
        if (userId.isBlank()) return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Fetch remote user tracking list
                val trackingRecords = socialRepository.getFriendTrackingList(userId)
                
                // Fetch local anime details in one batch
                val animeIds = trackingRecords.map { it.anilistId }
                val animeDetailsList = animeRepository.getAnimeByIds(animeIds)
                val animeMap = animeDetailsList.associateBy { it.anilistId }

                // Map to presentation objects
                val pairedItems = trackingRecords.map { record ->
                    FriendTrackingItem(
                        tracking = record,
                        anime = animeMap[record.anilistId]
                    )
                }
                _trackingList.value = pairedItems
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }
}
