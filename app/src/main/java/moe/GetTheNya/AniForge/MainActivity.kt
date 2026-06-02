package moe.GetTheNya.AniForge

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import dagger.hilt.android.AndroidEntryPoint
import moe.GetTheNya.AniForge.core.database.sync.DatabaseManager
import moe.GetTheNya.AniForge.ui.dashboard.DashboardScreen
import moe.GetTheNya.AniForge.ui.dashboard.DashboardViewModel
import moe.GetTheNya.AniForge.ui.detail.DetailScreen
import moe.GetTheNya.AniForge.ui.detail.DetailViewModel
import moe.GetTheNya.AniForge.ui.theme.AniForgeTheme
import javax.inject.Inject

sealed interface NavigationScreen {
    data object Dashboard : NavigationScreen
    data class Detail(val anilistId: Long) : NavigationScreen
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var databaseManager: DatabaseManager

    private val dashboardViewModel: DashboardViewModel by viewModels()
    private val detailViewModel: DetailViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var currentScreen by remember { mutableStateOf<NavigationScreen>(NavigationScreen.Dashboard) }

            // Check for catalog database updates asynchronously on application startup
            LaunchedEffect(Unit) {
                databaseManager.updateCatalogIfAvailable()
            }

            AniForgeTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    when (val screen = currentScreen) {
                        is NavigationScreen.Dashboard -> {
                            DashboardScreen(
                                viewModel = dashboardViewModel,
                                onAnimeClick = { id ->
                                    currentScreen = NavigationScreen.Detail(id)
                                },
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                        is NavigationScreen.Detail -> {
                            DetailScreen(
                                anilistId = screen.anilistId,
                                viewModel = detailViewModel,
                                onBackClick = {
                                    currentScreen = NavigationScreen.Dashboard
                                },
                                onAnimeClick = { newId ->
                                    currentScreen = NavigationScreen.Detail(newId)
                                },
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                    }
                }
            }
        }
    }
}