package moe.GetTheNya.AniForge.ui.settings

import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import moe.GetTheNya.AniForge.ui.localization.LocalLocaleStrings
import moe.GetTheNya.AniForge.ui.navigation.NavController
import moe.GetTheNya.AniForge.ui.navigation.Screen
import moe.GetTheNya.AniForge.ui.theme.*
import moe.GetTheNya.AniForge.ui.update.UpdateViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    navController: NavController,
    initialTab: Int = 0,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = { navController.popBackStack() }
) {
    val context = LocalContext.current
    val strings = LocalLocaleStrings.current
    val coroutineScope = rememberCoroutineScope()

    val storeOwner = LocalViewModelStoreOwner.current
    val importViewModel = remember(storeOwner) {
        if (storeOwner != null) {
            ViewModelProvider(storeOwner)[ImportViewModel::class.java]
        } else {
            ViewModelProvider(context as ComponentActivity)[ImportViewModel::class.java]
        }
    }
    val updateViewModel = remember(storeOwner) {
        if (storeOwner != null) {
            ViewModelProvider(storeOwner)[UpdateViewModel::class.java]
        } else {
            ViewModelProvider(context as ComponentActivity)[UpdateViewModel::class.java]
        }
    }

    val pagerState = rememberPagerState(initialPage = initialTab) {
        SettingsTab.entries.size
    }

    val currentEntry = remember(navController) {
        navController.backStack.lastOrNull { it.screen is Screen.Settings }
    }

    val density = LocalDensity.current
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val screenWidthPx = remember(density, screenWidth) { with(density) { screenWidth.toPx() } }

    var accumulatedDragX by remember { mutableStateOf(0f) }

    val nestedScrollConnection = remember(screenWidthPx, currentEntry) {
        object : NestedScrollConnection {
            override fun onPreScroll(
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                // If we are currently in the middle of a swipe-back drag (accumulatedDragX > 0f)
                if (pagerState.currentPage == 0 && accumulatedDragX > 0f) {
                    val dragChange = available.x
                    val newDrag = (accumulatedDragX + dragChange).coerceAtLeast(0f)
                    val consumed = newDrag - accumulatedDragX
                    accumulatedDragX = newDrag
                    currentEntry?.let { entry ->
                        if (accumulatedDragX > 0f) {
                            entry.isDragging = true
                            entry.dragOffset = accumulatedDragX
                        } else {
                            entry.isDragging = false
                            entry.dragOffset = 0f
                        }
                    }
                    return Offset(consumed, 0f)
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                // If we are at the first tab (index 0) and the user is swiping from left to right to start the drag
                if (pagerState.currentPage == 0 && available.x > 0f) {
                    accumulatedDragX = (accumulatedDragX + available.x).coerceAtLeast(0f)
                    currentEntry?.let { entry ->
                        if (accumulatedDragX > 0f) {
                            entry.isDragging = true
                            entry.dragOffset = accumulatedDragX
                        } else {
                            entry.isDragging = false
                            entry.dragOffset = 0f
                        }
                    }
                    return Offset(available.x, 0f) // Consume the horizontal scroll delta
                }
                return Offset.Zero
            }
        }
    }

    LaunchedEffect(pagerState.isScrollInProgress) {
        if (!pagerState.isScrollInProgress && accumulatedDragX > 0f) {
            val releaseOffset = accumulatedDragX
            accumulatedDragX = 0f
            currentEntry?.let { entry ->
                entry.isDragging = false
                coroutineScope.launch {
                    entry.animatableOffset.snapTo(releaseOffset)
                    if (releaseOffset > screenWidthPx * 0.4f) {
                        entry.animatableOffset.animateTo(
                            screenWidthPx,
                            tween(durationMillis = 300)
                        )
                        navController.popBackStack()
                    } else {
                        entry.animatableOffset.animateTo(0f, spring())
                    }
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Sticky Header with Back Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .padding(end = 12.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceDark)
                    .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = strings.misc.back,
                    tint = TextPrimary
                )
            }

            Text(
                text = strings.settingsScreen.title,
                color = TextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Sticky styled TabRow
        TabRow(
            selectedTabIndex = pagerState.currentPage,
            containerColor = SurfaceDark,
            contentColor = TextPrimary,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                    color = NeonCoral
                )
            },
            divider = {
                HorizontalDivider(color = CardBorder, thickness = 1.dp)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            SettingsTab.entries.forEachIndexed { index, tab ->
                val isSelected = pagerState.currentPage == index
                val titleText = when (tab) {
                    SettingsTab.APPEARANCE -> strings.settingsScreen.tabAppearance
                    SettingsTab.DATA_MANAGEMENT -> strings.settingsScreen.tabDataManagement
                    SettingsTab.ABOUT -> strings.settingsScreen.tabAbout
                }
                Tab(
                    selected = isSelected,
                    onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    },
                    text = {
                        Text(
                            text = titleText,
                            color = if (isSelected) TextPrimary else TextSecondary,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 14.sp
                        )
                    }
                )
            }
        }

        // Horizontal Pager wrapping tabs
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .nestedScroll(nestedScrollConnection)
        ) { page ->
            when (SettingsTab.entries[page]) {
                SettingsTab.APPEARANCE -> {
                    AppearanceTab(viewModel = viewModel)
                }
                SettingsTab.DATA_MANAGEMENT -> {
                    DataManagementTab(
                        viewModel = viewModel,
                        importViewModel = importViewModel
                    )
                }
                SettingsTab.ABOUT -> {
                    AboutTab(
                        updateViewModel = updateViewModel
                    )
                }
            }
        }
    }
}
