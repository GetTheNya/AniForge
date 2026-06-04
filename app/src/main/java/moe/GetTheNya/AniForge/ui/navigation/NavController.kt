package moe.GetTheNya.AniForge.ui.navigation

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
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
import java.util.UUID

sealed interface Screen {
    data object Tabs : Screen
    data class Detail(val animeId: Long) : Screen
    data object LogViewer : Screen
    data object Settings : Screen
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

    override val defaultViewModelProviderFactory: ViewModelProvider.Factory
        get() = activity.defaultViewModelProviderFactory

    override val defaultViewModelCreationExtras: CreationExtras
        get() = activity.defaultViewModelCreationExtras

    fun clear() {
        store.clear()
    }
}

class NavController(
    initialScreen: Screen = Screen.Tabs,
    private val activity: ComponentActivity
) {
    val backStack: SnapshotStateList<BackStackEntry> = mutableStateListOf(
        BackStackEntry(screen = initialScreen, activity = activity)
    )

    fun navigate(screen: Screen) {
        val isDuplicateActive = backStack.any { it.screen == screen }

        val isMultiTouchSpam = backStack.lastOrNull()?.screen != Screen.Tabs

        val isAnyTransitionRunning = backStack.any { entry ->
            entry.screen != Screen.Tabs && (entry.animatableOffset.isRunning || entry.isDragging)
        }

        if (isDuplicateActive || isMultiTouchSpam || isAnyTransitionRunning) {
            return
        }

        backStack.add(BackStackEntry(screen = screen, activity = activity))
    }

    fun popBackStack(): Boolean {
        if (backStack.size > 1) {
            val removed = backStack.removeAt(backStack.lastIndex)
            removed.clear()
            return true
        }
        return false
    }
}

@Composable
fun rememberNavController(initialScreen: Screen = Screen.Tabs): NavController {
    val context = LocalContext.current
    val activity = remember(context) { context as ComponentActivity }
    return remember(activity) { NavController(initialScreen, activity) }
}
