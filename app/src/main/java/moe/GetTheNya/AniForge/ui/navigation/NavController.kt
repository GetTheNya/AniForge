package moe.GetTheNya.AniForge.ui.navigation

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.util.UUID
import android.content.Context
import android.view.inputmethod.InputMethodManager

sealed interface Screen {
    data object Tabs : Screen
    data class Detail(
        val anilistId: Long,
        val sourceStatusId: String? = null,
        val sourceCollectionId: String? = null,
        val rouletteCount: Int = 0,
        val visitedIds: String = ""
    ) : Screen
    data object LogViewer : Screen
    data class Settings(val initialTab: Int = 0) : Screen
    data object DevSettings : Screen
    data object AccountSettings : Screen
    data class ImageViewer(val urls: List<String>, val initialIndex: Int) : Screen
    data object Library : Screen
    data class FranchiseTree(val franchiseId: Long) : Screen
    data class CollectionDetail(val collectionId: String) : Screen
    data object Social : Screen
    data class SharedProfile(val userId: String, val username: String) : Screen
    data class SharedCollectionDetail(val targetUserId: String, val collectionId: String) : Screen
}

class BackStackEntry(
    val id: String = UUID.randomUUID().toString(),
    val screen: Screen,
    private val activity: ComponentActivity
) : ViewModelStoreOwner, HasDefaultViewModelProviderFactory {
    private val store = ViewModelStore()
    override val viewModelStore: ViewModelStore = store

    val animatableOffset = Animatable(0f)
    var dragOffset by mutableFloatStateOf(0f)
    var isDragging by mutableStateOf(false)
    var isEntranceStarted by mutableStateOf(false)
    var swipeDismissProgress by mutableFloatStateOf(0f)

    var savedStateHandle = SavedStateHandle(
        buildMap {
            when (val s = screen) {
                is Screen.Detail -> {
                    put("anilistId", s.anilistId)
                    s.sourceStatusId?.let { put("sourceStatusId", it) }
                    s.sourceCollectionId?.let { put("sourceCollectionId", it) }
                    put("rouletteCount", s.rouletteCount)
                    put("visitedIds", s.visitedIds)
                }
                is Screen.FranchiseTree -> {
                    put("franchiseId", s.franchiseId)
                }
                is Screen.CollectionDetail -> {
                    put("collectionId", s.collectionId)
                }
                is Screen.SharedProfile -> {
                    put("userId", s.userId)
                    put("username", s.username)
                }
                is Screen.SharedCollectionDetail -> {
                    put("targetUserId", s.targetUserId)
                    put("collectionId", s.collectionId)
                }
                else -> {}
            }
        }
    )

    override val defaultViewModelProviderFactory: ViewModelProvider.Factory
        get() = activity.defaultViewModelProviderFactory

    override val defaultViewModelCreationExtras: CreationExtras
        get() {
            val extras = androidx.lifecycle.viewmodel.MutableCreationExtras(activity.defaultViewModelCreationExtras)
            val bundle = android.os.Bundle()
            when (val s = screen) {
                is Screen.Detail -> {
                    bundle.putLong("anilistId", s.anilistId)
                    s.sourceStatusId?.let { bundle.putString("sourceStatusId", it) }
                    s.sourceCollectionId?.let { bundle.putString("sourceCollectionId", it) }
                    bundle.putInt("rouletteCount", s.rouletteCount)
                    bundle.putString("visitedIds", s.visitedIds)
                }
                is Screen.FranchiseTree -> {
                    bundle.putLong("franchiseId", s.franchiseId)
                }
                is Screen.CollectionDetail -> {
                    bundle.putString("collectionId", s.collectionId)
                }
                is Screen.SharedProfile -> {
                    bundle.putString("userId", s.userId)
                    bundle.putString("username", s.username)
                }
                is Screen.SharedCollectionDetail -> {
                    bundle.putString("targetUserId", s.targetUserId)
                    bundle.putString("collectionId", s.collectionId)
                }
                else -> {}
            }
            extras[androidx.lifecycle.DEFAULT_ARGS_KEY] = bundle
            return extras
        }

    fun clear() {
        store.clear()
    }
}

class NavController(
    initialScreen: Screen = Screen.Tabs,
    private val activity: ComponentActivity
) {
    var rouletteExitMaxCount by mutableStateOf<Int?>(null)
    var composeCoroutineScope: kotlinx.coroutines.CoroutineScope? = null
    var onSelectTab: ((moe.GetTheNya.AniForge.TabScreen) -> Unit)? = null
    var onLibraryClick: (() -> Unit)? = null

    val backStack: SnapshotStateList<BackStackEntry> = mutableStateListOf(
        BackStackEntry(screen = initialScreen, activity = activity)
    )

    private fun hideKeyboardAndClearFocus() {
        val currentFocus = activity.currentFocus
        val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        if (currentFocus != null) {
            imm?.hideSoftInputFromWindow(currentFocus.windowToken, 0)
            currentFocus.clearFocus()
        } else {
            activity.window?.decorView?.let { decorView ->
                imm?.hideSoftInputFromWindow(decorView.windowToken, 0)
            }
        }
    }

    fun navigate(screen: Screen) {
        if (screen is Screen.Library) {
            hideKeyboardAndClearFocus()
            while (backStack.size > 1) {
                backStack.removeAt(backStack.lastIndex).clear()
            }
            onSelectTab?.invoke(moe.GetTheNya.AniForge.TabScreen.Library)
            return
        }
        if (screen is Screen.FranchiseTree) {
            val existingIndex = backStack.indexOfFirst { entry ->
                val s = entry.screen
                s is Screen.FranchiseTree && s.franchiseId == screen.franchiseId
            }
            if (existingIndex != -1) {
                hideKeyboardAndClearFocus()
                val existingEntry = backStack[existingIndex]
                val currentAnimeId = (backStack.lastOrNull()?.screen as? Screen.Detail)?.anilistId
                if (currentAnimeId != null) {
                    existingEntry.savedStateHandle.set("activeAnimeId", currentAnimeId)
                } else {
                    existingEntry.savedStateHandle.set("activeAnimeId", null)
                }

                val lastEntry = backStack.lastOrNull()
                if (lastEntry != null && lastEntry != existingEntry) {
                    // Instantly remove and clear any intermediate screens below the top screen but above the target
                    val currentIndex = backStack.indexOf(existingEntry)
                    val lastIndex = backStack.indexOf(lastEntry)
                    if (currentIndex != -1 && lastIndex != -1 && lastIndex > currentIndex + 1) {
                        for (i in (lastIndex - 1) downTo (currentIndex + 1)) {
                            backStack.removeAt(i).clear()
                        }
                    }

                    // Now, only lastEntry is left above existingEntry in the backstack.
                    // Animate lastEntry sliding away to reveal existingEntry underneath.
                    val cleanup = {
                        if (backStack.remove(lastEntry)) {
                            lastEntry.clear()
                        }
                    }
                    val screenWidthPx = activity.resources.displayMetrics.widthPixels.toFloat()
                    val scope = composeCoroutineScope
                    if (scope != null) {
                        scope.launch {
                            try {
                                lastEntry.isDragging = false
                                lastEntry.animatableOffset.snapTo(0f)
                                lastEntry.animatableOffset.animateTo(
                                    targetValue = screenWidthPx,
                                    animationSpec = androidx.compose.animation.core.tween(durationMillis = 300)
                                )
                            } catch (e: Exception) {
                                // Fallback to instant transition if frame clock animation fails
                            } finally {
                                cleanup()
                            }
                        }
                    } else {
                        cleanup()
                    }
                } else {
                    while (backStack.lastIndex > existingIndex) {
                        backStack.removeAt(backStack.lastIndex).clear()
                    }
                }
                return
            } else {
                hideKeyboardAndClearFocus()
                // Forward navigation: inject activeAnimeId if navigating from Screen.Detail
                val currentAnimeId = (backStack.lastOrNull()?.screen as? Screen.Detail)?.anilistId
                val newEntry = BackStackEntry(screen = screen, activity = activity).apply {
                    if (currentAnimeId != null) {
                        savedStateHandle.set("activeAnimeId", currentAnimeId)
                    } else {
                        savedStateHandle.set("activeAnimeId", null)
                    }
                }
                backStack.add(newEntry)
                return
            }
        }

        val isDuplicateActive = !(backStack.lastOrNull()?.screen is Screen.FranchiseTree && screen is Screen.Detail) &&
                backStack.any { it.screen == screen }

        val lastEntry = backStack.lastOrNull()
        val isMultiTouchSpam = lastEntry != null &&
                lastEntry.screen != Screen.Tabs &&
                screen !is Screen.ImageViewer &&
                (!lastEntry.isEntranceStarted || lastEntry.animatableOffset.isRunning)

        val isAnyTransitionRunning = backStack.any { entry ->
            entry.screen != Screen.Tabs && (entry.animatableOffset.isRunning || entry.isDragging)
        }

        if (isDuplicateActive || isMultiTouchSpam || isAnyTransitionRunning) {
            return
        }

        hideKeyboardAndClearFocus()

        if (screen is Screen.Detail && screen.rouletteCount > 0) {
            val randomDetailEntries = backStack.filter {
                it.screen is Screen.Detail && (it.screen as Screen.Detail).rouletteCount > 0
            }
            if (randomDetailEntries.size >= 5) {
                val oldest = randomDetailEntries.first()
                if (backStack.remove(oldest)) {
                    oldest.clear()
                }
            }
        }

        backStack.add(BackStackEntry(screen = screen, activity = activity))
    }

    fun popBackStack(): Boolean {
        if (backStack.size > 1) {
            hideKeyboardAndClearFocus()
            val removed = backStack.removeAt(backStack.lastIndex)
            removed.clear()
            return true
        }
        return false
    }

    fun finalizeRouletteExit() {
        hideKeyboardAndClearFocus()
        val updatedStack = backStack.toMutableList()
        while (updatedStack.size > 1 && updatedStack.last().screen is Screen.Detail) {
            val removed = updatedStack.removeAt(updatedStack.lastIndex)
            removed.clear()
        }
        backStack.clear()
        backStack.addAll(updatedStack)
        rouletteExitMaxCount = null // Reset state
    }
}

@Composable
fun rememberNavController(initialScreen: Screen = Screen.Tabs): NavController {
    val context = LocalContext.current
    val activity = remember(context) { context as ComponentActivity }
    val coroutineScope = rememberCoroutineScope()
    return remember(activity) { NavController(initialScreen, activity) }.apply {
        this.composeCoroutineScope = coroutineScope
    }
}
