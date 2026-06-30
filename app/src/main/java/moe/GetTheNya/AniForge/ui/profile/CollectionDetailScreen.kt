package moe.GetTheNya.AniForge.ui.profile

import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import moe.GetTheNya.AniForge.core.model.Anime
import moe.GetTheNya.AniForge.ui.navigation.NavController
import moe.GetTheNya.AniForge.ui.navigation.Screen
import moe.GetTheNya.AniForge.ui.theme.*
import moe.GetTheNya.AniForge.ui.franchises.CollectionFormDialog
import moe.GetTheNya.AniForge.ui.utils.statusConfigs
import moe.GetTheNya.AniForge.ui.localization.getPlural
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class DragDropListState(
    val lazyListState: LazyListState,
    private val getItems: () -> List<Anime>,
    val onMove: (Int, Int) -> Unit,
    val onDragEnd: () -> Unit,
    private val coroutineScope: CoroutineScope
) {
    var initiallyDraggedElement by mutableStateOf<LazyListItemInfo?>(null)
    var currentDraggedIndex by mutableStateOf<Int?>(null)
    var autoScrollSpeed by mutableFloatStateOf(0f)
    
    var dragOffset by mutableFloatStateOf(0f)

    val elementDisplacement: Float?
        get() = initiallyDraggedElement?.let { init ->
            val current = lazyListState.layoutInfo.visibleItemsInfo
                .firstOrNull { it.key == init.key } ?: return@let null
            val initialDragOffset = init.offset.toFloat()
            dragOffset + initialDragOffset - current.offset
        }

    fun onDragStart(key: Any) {
        lazyListState.layoutInfo.visibleItemsInfo
            .firstOrNull { it.key == key }
            ?.also { item ->
                initiallyDraggedElement = item
                currentDraggedIndex = item.index
                dragOffset = 0f
                autoScrollSpeed = 0f
            }
    }

    fun onDrag(dragAmountY: Float) {
        dragOffset += dragAmountY
        
        val initiallyDragged = initiallyDraggedElement ?: run {
            autoScrollSpeed = 0f
            return
        }
        val items = getItems()
        val currentDraggedIdx = items.indexOfFirst { it.anilistId == initiallyDragged.key }
        if (currentDraggedIdx == -1) {
            autoScrollSpeed = 0f
            return
        }
            
        val currentDragged = lazyListState.layoutInfo.visibleItemsInfo
            .firstOrNull { it.key == initiallyDragged.key } ?: run {
                autoScrollSpeed = 0f
                return
            }

        if (currentDraggedIndex != null && currentDragged.index != currentDraggedIndex) {
            return
        }
            
        val initialDragOffset = initiallyDragged.offset.toFloat()
        val startOffset = dragOffset + initialDragOffset
        val endOffset = startOffset + initiallyDragged.size
        
        val targetItem = lazyListState.layoutInfo.visibleItemsInfo
            .firstOrNull { item ->
                if (item.key == initiallyDragged.key) return@firstOrNull false
                val itemStart = item.offset
                val itemEnd = itemStart + item.size
                
                if (item.index > currentDragged.index) {
                    endOffset > itemStart + item.size / 2
                } else {
                    startOffset < itemEnd - item.size / 2
                }
            }
            
        if (targetItem != null) {
            val targetIdx = items.indexOfFirst { it.anilistId == targetItem.key }
            if (targetIdx != -1 && targetIdx != currentDraggedIdx) {
                val firstVisibleIdx = lazyListState.firstVisibleItemIndex
                val firstVisibleScrollOffset = lazyListState.firstVisibleItemScrollOffset
                
                onMove(currentDraggedIdx, targetIdx)
                currentDraggedIndex = targetIdx

                if (currentDraggedIdx == firstVisibleIdx || targetIdx == firstVisibleIdx) {
                    coroutineScope.launch {
                        lazyListState.scrollToItem(firstVisibleIdx, firstVisibleScrollOffset)
                    }
                }
            }
        }

        val viewportHeight = lazyListState.layoutInfo.viewportSize.height.toFloat()
        val threshold = 100f
        
        autoScrollSpeed = if (viewportHeight > 0f) {
            when {
                startOffset < threshold -> {
                    val ratio = ((threshold - startOffset) / threshold).coerceIn(0f, 1f)
                    -30f * ratio
                }
                endOffset > viewportHeight - threshold -> {
                    val ratio = ((endOffset - (viewportHeight - threshold)) / threshold).coerceIn(0f, 1f)
                    30f * ratio
                }
                else -> 0f
            }
        } else {
            0f
        }
    }

    fun onDragInterrupted() {
        initiallyDraggedElement = null
        currentDraggedIndex = null
        dragOffset = 0f
        autoScrollSpeed = 0f
        onDragEnd()
    }
}

@Composable
fun rememberDragDropListState(
    lazyListState: LazyListState = rememberLazyListState(),
    getItems: () -> List<Anime>,
    onMove: (Int, Int) -> Unit,
    onDragEnd: () -> Unit,
    coroutineScope: CoroutineScope = rememberCoroutineScope()
): DragDropListState {
    val currentGetItems = rememberUpdatedState(getItems)
    val currentOnMove = rememberUpdatedState(onMove)
    val currentOnDragEnd = rememberUpdatedState(onDragEnd)

    return remember(lazyListState, coroutineScope) {
        DragDropListState(
            lazyListState = lazyListState,
            getItems = { currentGetItems.value() },
            onMove = { from, to -> currentOnMove.value(from, to) },
            onDragEnd = { currentOnDragEnd.value() },
            coroutineScope = coroutineScope
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CollectionDetailScreen(
    collectionId: Int,
    viewModel: CollectionDetailViewModel,
    navController: NavController,
    modifier: Modifier = Modifier,
    onBack: () -> Unit
) {
    LaunchedEffect(collectionId) {
        viewModel.loadCollection(collectionId)
    }

    key(collectionId) {
        val strings = moe.GetTheNya.AniForge.ui.localization.LocalLocaleStrings.current
        val collection by viewModel.collection.collectAsState()
        val animeList by viewModel.animeList.collectAsState()
        val trackingMap by viewModel.trackingMap.collectAsState()
        val searchResults by viewModel.searchResults.collectAsState()
        val addSearchQuery by viewModel.searchQuery.collectAsState()
        val preferUk by viewModel.preferUk.collectAsState()

        var showEditDialog by remember { mutableStateOf(false) }
        var showAddTitlesDialog by remember { mutableStateOf(false) }
        
        val context = LocalContext.current
        val dragDropState = rememberDragDropListState(
            getItems = { animeList },
            onMove = { from, to -> viewModel.moveItem(from, to) },
            onDragEnd = { viewModel.saveNewOrder() }
        )

        LaunchedEffect(dragDropState.initiallyDraggedElement) {
            if (dragDropState.initiallyDraggedElement != null) {
                while (true) {
                    val speed = dragDropState.autoScrollSpeed
                    if (speed != 0f) {
                        dragDropState.lazyListState.scrollBy(speed)
                    }
                    kotlinx.coroutines.delay(10)
                }
            }
        }

        LaunchedEffect(dragDropState.initiallyDraggedElement) {
            if (dragDropState.initiallyDraggedElement != null) {
                snapshotFlow { dragDropState.lazyListState.layoutInfo }
                    .collect {
                        dragDropState.onDrag(0f)
                    }
            }
        }

        if (collection == null) {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .background(BackgroundDark),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = ElectricViolet)
            }
        } else {
            val currentCollection = collection!!

    if (showEditDialog) {
        CollectionFormDialog(
            initialTitle = currentCollection.title,
            initialDescription = currentCollection.description,
            dialogTitle = strings.libraryScreen.editCollectionDetails,
            confirmButtonText = strings.libraryScreen.save,
            descriptionLabel = strings.libraryScreen.description,
            onDismissRequest = { showEditDialog = false },
            onConfirm = { title, description ->
                viewModel.updateCollectionDetails(title, description)
                showEditDialog = false
            }
        )
    }

    if (showAddTitlesDialog) {
        AlertDialog(
            modifier = Modifier
                .border(
                    width = 1.dp,
                    color = CardBorder,
                    shape = RoundedCornerShape(24.dp)
                ),
            onDismissRequest = { 
                showAddTitlesDialog = false 
                viewModel.setSearchQuery("")
            },
            title = { Text(strings.libraryScreen.manageCollectionTitles, color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(350.dp)
                ) {
                    OutlinedTextField(
                        value = addSearchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        placeholder = { Text(strings.libraryScreen.searchCatalogOrTracked, color = TextSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = ElectricViolet,
                            unfocusedBorderColor = CardBorder,
                            focusedContainerColor = SurfaceDark,
                            unfocusedContainerColor = SurfaceDark
                        ),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(searchResults, key = { _, anime -> anime.anilistId }) { _, anime ->
                            val isAdded = animeList.any { it.anilistId == anime.anilistId }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(SurfaceDark.copy(alpha = 0.5f))
                                    .clickable {
                                        if (isAdded) {
                                            viewModel.removeAnimeFromCollection(anime.anilistId)
                                        } else {
                                            viewModel.addAnimeToCollection(anime.anilistId)
                                        }
                                    }
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = anime.coverLarge,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(40.dp, 56.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .border(0.5.dp, CardBorder, RoundedCornerShape(6.dp))
                                )

                                Spacer(modifier = Modifier.width(12.dp))

                                Text(
                                    text = anime.getDisplayTitle(preferUk = preferUk),
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )

                                Checkbox(
                                    checked = isAdded,
                                    onCheckedChange = { checked ->
                                        if (checked) {
                                            viewModel.addAnimeToCollection(anime.anilistId)
                                        } else {
                                            viewModel.removeAnimeFromCollection(anime.anilistId)
                                        }
                                    },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = ElectricViolet,
                                        uncheckedColor = TextSecondary
                                    )
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { 
                        showAddTitlesDialog = false 
                        viewModel.setSearchQuery("")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(strings.libraryScreen.done, color = BackgroundDark, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = AlertBackground,
            shape = RoundedCornerShape(24.dp)
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .statusBarsPadding()
    ) {
        // Toolbar header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = strings.misc.back,
                    tint = TextPrimary
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = currentCollection.title,
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (currentCollection.description.isNotBlank()) {
                    Text(
                        text = currentCollection.description,
                        color = TextSecondary,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Edit Action
            IconButton(onClick = { showEditDialog = true }) {
                Icon(Icons.Default.Edit, contentDescription = strings.libraryScreen.editCollectionDetails, tint = ElectricViolet)
            }

            // Delete Action
            IconButton(
                onClick = {
                    viewModel.deleteCollection()
                    Toast.makeText(context, strings.libraryScreen.collectionDeleted, Toast.LENGTH_SHORT).show()
                    onBack()
                }
            ) {
                Icon(Icons.Default.Delete, contentDescription = strings.libraryScreen.deleteCollection, tint = NeonCoral)
            }
        }

        // Add Titles Button Slot
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val countText = strings.libraryScreen.itemsCount.getPlural(animeList.size)
            Text(
                text = countText,
                color = TextSecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceDark)
                        .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                        .clickable {
                            val randomAnime = animeList.randomOrNull()
                            if (randomAnime != null) {
                                viewModel.incrementChaosMeter()
                                navController.navigate(
                                    Screen.Detail(
                                        anilistId = randomAnime.anilistId,
                                        sourceCollectionId = collectionId,
                                        rouletteCount = 1,
                                        visitedIds = randomAnime.anilistId.toString()
                                    )
                                )
                            } else {
                                Toast.makeText(context, strings.libraryScreen.randomEmpty, Toast.LENGTH_SHORT).show()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Casino,
                        contentDescription = "Random Anime",
                        tint = TextPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Button(
                    onClick = { showAddTitlesDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceDark),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = strings.libraryScreen.addTitle, tint = ElectricViolet, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(strings.libraryScreen.addTitle, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Titles Drag and Drop list container
        if (animeList.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = strings.libraryScreen.collectionIsEmpty,
                    color = TextSecondary,
                    fontSize = 14.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                state = dragDropState.lazyListState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(animeList, key = { _, anime -> anime.anilistId }) { index, anime ->
                    val isDragged = dragDropState.initiallyDraggedElement?.key == anime.anilistId
                    
                    val translationYAnimatable = remember { Animatable(0f) }
                    var lastDisplacement by remember { mutableFloatStateOf(0f) }

                    LaunchedEffect(isDragged) {
                        if (isDragged) {
                            snapshotFlow { dragDropState.elementDisplacement }
                                .collect { disp ->
                                    if (disp != null) {
                                        lastDisplacement = disp
                                    }
                                }
                        } else {
                            if (lastDisplacement != 0f) {
                                translationYAnimatable.snapTo(lastDisplacement)
                                lastDisplacement = 0f
                                translationYAnimatable.animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioLowBouncy,
                                        stiffness = Spring.StiffnessMediumLow
                                    )
                                )
                            }
                        }
                    }

                    val currentTranslationY = if (isDragged) {
                        dragDropState.elementDisplacement ?: 0f
                    } else if (translationYAnimatable.isRunning || translationYAnimatable.value != 0f) {
                        translationYAnimatable.value
                    } else {
                        lastDisplacement
                    }

                    val isAnimatingBack = translationYAnimatable.isRunning || 
                                          translationYAnimatable.value != 0f || 
                                          lastDisplacement != 0f

                    val itemModifier = if (isDragged) {
                        Modifier
                            .graphicsLayer {
                                translationY = dragDropState.elementDisplacement ?: 0f
                            }
                            .zIndex(10f)
                    } else if (isAnimatingBack) {
                        Modifier
                            .animateItem(
                                fadeInSpec = null,
                                fadeOutSpec = null
                            )
                            .graphicsLayer {
                                translationY = currentTranslationY
                            }
                            .zIndex(10f)
                    } else {
                        Modifier
                            .animateItem(
                                fadeInSpec = null,
                                fadeOutSpec = null
                            )
                            .zIndex(1f)
                    }

                    var layoutCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
                    var previousScreenY by remember { mutableFloatStateOf(0f) }
                    val dragHandleModifier = Modifier
                        .onGloballyPositioned { layoutCoordinates = it }
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    dragDropState.onDragStart(anime.anilistId)
                                    val coords = layoutCoordinates
                                    if (coords != null && coords.isAttached) {
                                        previousScreenY = coords.localToWindow(offset).y
                                    }
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    val coords = layoutCoordinates
                                    if (coords != null && coords.isAttached) {
                                        val currentScreenY = coords.localToWindow(change.position).y
                                        val deltaY = currentScreenY - previousScreenY
                                        previousScreenY = currentScreenY
                                        dragDropState.onDrag(deltaY)
                                    }
                                },
                                onDragEnd = {
                                    dragDropState.onDragInterrupted()
                                },
                                onDragCancel = {
                                    dragDropState.onDragInterrupted()
                                }
                            )
                        }

                    Box(
                        modifier = itemModifier
                    ) {
                        CollectionAnimeItemCard(
                            anime = anime,
                            status = trackingMap[anime.anilistId],
                            preferUk = preferUk,
                            isDragged = isDragged,
                            dragHandleModifier = dragHandleModifier,
                            onRemove = { viewModel.removeAnimeFromCollection(anime.anilistId) },
                            onClick = { navController.navigate(Screen.Detail(anime.anilistId)) }
                        )
                    }
                }
            }
        }
    }
}
}
}

@Composable
fun CollectionAnimeItemCard(
    anime: Anime,
    status: String?,
    preferUk: Boolean,
    isDragged: Boolean = false,
    dragHandleModifier: Modifier = Modifier,
    onRemove: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = moe.GetTheNya.AniForge.ui.localization.LocalLocaleStrings.current
    val cardBgColor = if (isDragged) Color(0xFF20202A) else SurfaceCardDark
    val cardElevation = if (isDragged) 8.dp else 0.dp

    val scale by animateFloatAsState(
        targetValue = if (isDragged) 1.04f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "scale"
    )

    val rotation by animateFloatAsState(
        targetValue = if (isDragged) -2.5f else 0.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "rotation"
    )

    Row(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                rotationZ = rotation
            }
            .fillMaxWidth()
            .shadow(cardElevation, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(cardBgColor)
            .border(1.dp, if (isDragged) ElectricViolet else CardBorder, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = anime.coverLarge,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(48.dp, 68.dp)
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, CardBorder, RoundedCornerShape(8.dp))
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = anime.getDisplayTitle(preferUk = preferUk),
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val format = anime.format
                if (format != null) {
                    Text(
                        text = format.uppercase(),
                        color = NeonCoral,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0x33FF2E93))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                if (status != null && status != "NONE") {
                    val config = statusConfigs.find { it.id == status }
                    if (config != null) {
                        Text(
                            text = config.getLabel(strings).uppercase(),
                            color = config.color,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xE60C0C0E))
                                .border(0.5.dp, config.color.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Drag Handle icon
        Icon(
            imageVector = Icons.Default.Menu,
            contentDescription = strings.libraryScreen.dragHandle,
            tint = TextSecondary.copy(alpha = 0.6f),
            modifier = dragHandleModifier
                .padding(horizontal = 8.dp)
                .size(20.dp)
        )

        // Remove title from collection
        IconButton(onClick = onRemove) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = strings.libraryScreen.removeFromCollection,
                tint = NeonCoral.copy(alpha = 0.8f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
