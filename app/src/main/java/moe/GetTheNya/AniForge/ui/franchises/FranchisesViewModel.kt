package moe.GetTheNya.AniForge.ui.franchises

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import moe.GetTheNya.AniForge.core.database.repository.AnimeRepository
import moe.GetTheNya.AniForge.core.model.Anime
import moe.GetTheNya.AniForge.core.model.Franchise
import javax.inject.Inject

data class FranchiseItem(
    val franchise: Franchise,
    val releases: List<Anime>
)

@HiltViewModel
class FranchisesViewModel @Inject constructor(
    private val animeRepository: AnimeRepository
) : ViewModel() {

    private val _franchises = MutableStateFlow<List<FranchiseItem>>(emptyList())
    val franchises: StateFlow<List<FranchiseItem>> = _franchises.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadFranchises()
    }

    fun loadFranchises() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                val allFranchises = animeRepository.getAllFranchises()
                val items = allFranchises.map { franchise ->
                    val releases = animeRepository.getFranchiseAnime(franchise.franchiseId)
                    FranchiseItem(franchise, releases)
                }
                _franchises.value = items
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }
}
