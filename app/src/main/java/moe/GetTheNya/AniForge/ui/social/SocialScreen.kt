package moe.GetTheNya.AniForge.ui.social

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.collectLatest
import moe.GetTheNya.AniForge.core.network.FriendshipStatus
import moe.GetTheNya.AniForge.core.network.UserProfileDto
import moe.GetTheNya.AniForge.ui.localization.LocalLocaleStrings
import moe.GetTheNya.AniForge.ui.navigation.NavController
import moe.GetTheNya.AniForge.ui.navigation.Screen
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import moe.GetTheNya.AniForge.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SocialScreen(
    viewModel: SocialViewModel,
    navController: NavController,
    modifier: Modifier = Modifier,
    onBack: () -> Unit
) {
    val strings = LocalLocaleStrings.current
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val friends by viewModel.friends.collectAsState()
    val incomingRequests by viewModel.incomingRequests.collectAsState()
    val sentRequests by viewModel.sentRequests.collectAsState()
    val myFriendships by viewModel.myFriendships.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val actionLoadingStates by viewModel.actionLoadingStates.collectAsState()

    var activeTab by remember { mutableIntStateOf(0) }
    var requestSubTab by remember { mutableIntStateOf(0) }


    LaunchedEffect(Unit) {
        viewModel.toastEvents.collectLatest { event ->
            val message = when (event) {
                is ToastEvent.RequestSent -> strings.socialScreen.toastRequestSent
                is ToastEvent.RequestAccepted -> strings.socialScreen.toastRequestAccepted
                is ToastEvent.RequestDeclined -> strings.socialScreen.toastRequestDeclined
                is ToastEvent.FriendRemoved -> strings.socialScreen.toastFriendRemoved
                is ToastEvent.Error -> String.format(strings.socialScreen.toastError, event.message)
            }
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .statusBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceDark)
                        .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = strings.misc.back,
                        tint = TextPrimary
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = strings.socialScreen.name,
                    color = TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                placeholder = {
                    Text(
                        text = strings.socialScreen.searchPlaceholder,
                        color = TextSecondary,
                        fontSize = 14.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = strings.misc.clear,
                                tint = TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonCoral,
                    unfocusedBorderColor = CardBorder,
                    focusedContainerColor = SurfaceDark,
                    unfocusedContainerColor = SurfaceDark,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
            )

            // Search Results or Tabbed layout
            if (searchQuery.isNotBlank()) {
                // Render Search Results
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                ) {
                    if (searchResults.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = strings.socialScreen.noFriends,
                                color = TextSecondary,
                                fontSize = 15.sp
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(searchResults, key = { it.id }) { user ->
                                // Determine friendship status
                                val friendship = myFriendships.find {
                                    (it.senderId == user.id || it.receiverId == user.id)
                                }
                                val isLoading = actionLoadingStates[user.id] == true

                                UserSearchResultRow(
                                    user = user,
                                    status = friendship?.status,
                                    isSender = friendship?.senderId == user.id,
                                    isLoading = isLoading,
                                    onActionClick = {
                                        if (friendship == null) {
                                            viewModel.sendRequest(user.id)
                                        } else if (friendship.status == FriendshipStatus.PENDING) {
                                            if (friendship.senderId == user.id) {
                                                // We received it, accept it
                                                viewModel.acceptRequest(user.id)
                                            } else {
                                                // We sent it, we can cancel it
                                                viewModel.declineOrRemoveRequest(user.id)
                                            }
                                        } else if (friendship.status == FriendshipStatus.ACCEPTED) {
                                            viewModel.declineOrRemoveRequest(user.id)
                                        }
                                    },
                                    onSecondaryActionClick = {
                                        // Decline option when we received a request
                                        if (friendship?.status == FriendshipStatus.PENDING && friendship.senderId == user.id) {
                                            viewModel.declineOrRemoveRequest(user.id)
                                        }
                                    },
                                    onRowClick = {
                                        if (friendship?.status == FriendshipStatus.ACCEPTED) {
                                            navController.navigate(Screen.SharedProfile(user.id, user.username, user.avatarUrl))
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            } else {
                // Tab Header Navigation
                TabRow(
                    selectedTabIndex = activeTab,
                    containerColor = Color.Transparent,
                    contentColor = NeonCoral,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[activeTab]),
                            color = NeonCoral
                        )
                    },
                    divider = {
                        HorizontalDivider(color = CardBorder, thickness = 1.dp)
                    },
                    modifier = Modifier.padding(horizontal = 24.dp)
                ) {
                    Tab(
                        selected = activeTab == 0,
                        onClick = { activeTab = 0 },
                        text = {
                            Text(
                                text = strings.socialScreen.tabFriends,
                                fontSize = 15.sp,
                                fontWeight = if (activeTab == 0) FontWeight.Bold else FontWeight.Normal,
                                color = if (activeTab == 0) TextPrimary else TextSecondary
                            )
                        }
                    )
                    Tab(
                        selected = activeTab == 1,
                        onClick = { activeTab = 1 },
                        text = {
                            Box(modifier = Modifier.padding(vertical = 12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = strings.socialScreen.tabRequests,
                                        fontSize = 15.sp,
                                        fontWeight = if (activeTab == 1) FontWeight.Bold else FontWeight.Normal,
                                        color = if (activeTab == 1) TextPrimary else TextSecondary
                                    )
                                    if (incomingRequests.isNotEmpty()) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Badge(
                                            containerColor = NeonCoral,
                                            contentColor = Color.White
                                        ) {
                                            Text(text = incomingRequests.size.toString(), fontSize = 10.sp)
                                        }
                                    }
                                }
                            }
                        }
                    )
                }

                // Tab Content
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                ) {
                    if (isRefreshing) {
                        CircularProgressIndicator(
                            color = NeonCoral,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else {
                        if (activeTab == 0) {
                            // Friends list tab
                            if (friends.isEmpty()) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = strings.socialScreen.noFriends,
                                        color = TextSecondary,
                                        fontSize = 15.sp
                                    )
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(vertical = 12.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(friends, key = { it.id }) { friend ->
                                        FriendRow(
                                            friend = friend,
                                            onRemoveClick = {
                                                viewModel.declineOrRemoveRequest(friend.id)
                                            },
                                            isLoading = actionLoadingStates[friend.id] == true,
                                            onClick = {
                                                navController.navigate(Screen.SharedProfile(friend.id, friend.username, friend.avatarUrl))
                                            }
                                        )
                                    }
                                }
                            }
                        } else {
                            // Requests tab
                            Column(modifier = Modifier.fillMaxSize()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // Received Tab button
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (requestSubTab == 0) SurfaceDark else Color.Transparent)
                                            .border(1.dp, if (requestSubTab == 0) NeonCoral else CardBorder, RoundedCornerShape(12.dp))
                                            .clickable { requestSubTab = 0 }
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = strings.socialScreen.tabRequestsReceived,
                                                color = if (requestSubTab == 0) TextPrimary else TextSecondary,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            if (incomingRequests.isNotEmpty()) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .size(18.dp)
                                                        .clip(CircleShape)
                                                        .background(NeonCoral),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = incomingRequests.size.toString(),
                                                        color = Color.White,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // Sent Tab button
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (requestSubTab == 1) SurfaceDark else Color.Transparent)
                                            .border(1.dp, if (requestSubTab == 1) NeonCoral else CardBorder, RoundedCornerShape(12.dp))
                                            .clickable { requestSubTab = 1 }
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = strings.socialScreen.tabRequestsSent,
                                                color = if (requestSubTab == 1) TextPrimary else TextSecondary,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            if (sentRequests.isNotEmpty()) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .size(18.dp)
                                                        .clip(CircleShape)
                                                        .background(TransparentAccent)
                                                        .border(1.dp, NeonCoral, CircleShape),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = sentRequests.size.toString(),
                                                        color = NeonCoral,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Sub-tab Content
                                if (requestSubTab == 0) {
                                    // Received Section
                                    if (incomingRequests.isEmpty()) {
                                        Box(
                                            modifier = Modifier.weight(1f).fillMaxWidth(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = strings.socialScreen.noRequests,
                                                color = TextSecondary,
                                                fontSize = 15.sp
                                            )
                                        }
                                    } else {
                                        LazyColumn(
                                            modifier = Modifier.weight(1f).fillMaxWidth(),
                                            contentPadding = PaddingValues(vertical = 12.dp),
                                            verticalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            items(incomingRequests, key = { it.id }) { request ->
                                                val isLoading = actionLoadingStates[request.id] == true
                                                RequestRow(
                                                    request = request,
                                                    onAccept = { viewModel.acceptRequest(request.id) },
                                                    onDecline = { viewModel.declineOrRemoveRequest(request.id) },
                                                    isLoading = isLoading
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    // Sent Section
                                    if (sentRequests.isEmpty()) {
                                        Box(
                                            modifier = Modifier.weight(1f).fillMaxWidth(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = strings.socialScreen.noRequests,
                                                color = TextSecondary,
                                                fontSize = 15.sp
                                            )
                                        }
                                    } else {
                                        LazyColumn(
                                            modifier = Modifier.weight(1f).fillMaxWidth(),
                                            contentPadding = PaddingValues(vertical = 12.dp),
                                            verticalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            items(sentRequests, key = { it.id }) { request ->
                                                val isLoading = actionLoadingStates[request.id] == true
                                                SentRequestRow(
                                                    request = request,
                                                    onCancel = { viewModel.declineOrRemoveRequest(request.id) },
                                                    isLoading = isLoading
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UserSearchResultRow(
    user: UserProfileDto,
    status: FriendshipStatus?,
    isSender: Boolean,
    isLoading: Boolean,
    onActionClick: () -> Unit,
    onSecondaryActionClick: () -> Unit,
    onRowClick: () -> Unit
) {
    val strings = LocalLocaleStrings.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = status == FriendshipStatus.ACCEPTED, onClick = onRowClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar placeholder
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(TransparentAccent)
                    .border(1.5.dp, if (!user.avatarUrl.isNullOrBlank()) CardBorder else NeonCoral, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                val avatarUrl = user.avatarUrl
                if (!avatarUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = avatarUrl,
                        contentDescription = "User Avatar",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        text = user.username.firstOrNull()?.uppercase() ?: "?",
                        color = NeonCoral,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user.username,
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Context Action Button
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = NeonCoral,
                    strokeWidth = 2.dp
                )
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    when (status) {
                        null -> {
                            Button(
                                onClick = onActionClick,
                                colors = ButtonDefaults.buttonColors(containerColor = NeonCoral),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(text = strings.socialScreen.addFriend, fontSize = 13.sp, color = TextPrimary)
                            }
                        }
                        FriendshipStatus.PENDING -> {
                            if (isSender) {
                                // Request received: show Accept & Decline
                                IconButton(
                                    onClick = onActionClick,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF2E7D32)) // Dark Green
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = strings.socialScreen.accept,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                IconButton(
                                    onClick = onSecondaryActionClick,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFC62828)) // Dark Red
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = strings.socialScreen.decline,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            } else {
                                // Request sent by us: show pending button (can click to cancel)
                                Button(
                                    onClick = onActionClick,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = SurfaceCardDark,
                                        contentColor = TextSecondary
                                    ),
                                    border = BorderStroke(1.dp, CardBorder),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(text = strings.socialScreen.pending, fontSize = 13.sp)
                                }
                            }
                        }
                        FriendshipStatus.ACCEPTED -> {
                            Button(
                                onClick = onRowClick,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SurfaceCardDark,
                                    contentColor = TextSecondary
                                ),
                                border = BorderStroke(1.dp, CardBorder),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(text = strings.socialScreen.friends, fontSize = 13.sp)
                            }
                        }
                        FriendshipStatus.BLOCKED -> {
                            // Suppress block rendering for simplicity
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FriendRow(
    friend: UserProfileDto,
    onRemoveClick: () -> Unit,
    isLoading: Boolean,
    onClick: () -> Unit
) {
    val strings = LocalLocaleStrings.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(TransparentAccent)
                    .border(1.5.dp, if (!friend.avatarUrl.isNullOrBlank()) CardBorder else NeonCoral, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                val avatarUrl = friend.avatarUrl
                if (!avatarUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = avatarUrl,
                        contentDescription = "Friend Avatar",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        text = friend.username.firstOrNull()?.uppercase() ?: "?",
                        color = NeonCoral,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = friend.username,
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = NeonCoral,
                    strokeWidth = 2.dp
                )
            } else {
                Button(
                    onClick = onRemoveClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = TextSecondary
                    ),
                    border = BorderStroke(1.dp, CardBorder),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(text = strings.socialScreen.removeFriend, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun RequestRow(
    request: UserProfileDto,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    isLoading: Boolean
) {
    val strings = LocalLocaleStrings.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CardBorder, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(TransparentAccent)
                    .border(1.5.dp, if (!request.avatarUrl.isNullOrBlank()) CardBorder else NeonCoral, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                val avatarUrl = request.avatarUrl
                if (!avatarUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = avatarUrl,
                        contentDescription = "Request Avatar",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        text = request.username.firstOrNull()?.uppercase() ?: "?",
                        color = NeonCoral,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = request.username,
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = NeonCoral,
                    strokeWidth = 2.dp
                )
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = onAccept,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF2E7D32)) // Dark Green
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = strings.socialScreen.accept,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = onDecline,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFC62828)) // Dark Red
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = strings.socialScreen.decline,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SentRequestRow(
    request: UserProfileDto,
    onCancel: () -> Unit,
    isLoading: Boolean
) {
    val strings = LocalLocaleStrings.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CardBorder, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(TransparentAccent)
                    .border(1.5.dp, if (!request.avatarUrl.isNullOrBlank()) CardBorder else NeonCoral, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                val avatarUrl = request.avatarUrl
                if (!avatarUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = avatarUrl,
                        contentDescription = "Request Avatar",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        text = request.username.firstOrNull()?.uppercase() ?: "?",
                        color = NeonCoral,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = request.username,
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = NeonCoral,
                    strokeWidth = 2.dp
                )
            } else {
                Button(
                    onClick = onCancel,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = TextSecondary
                    ),
                    border = BorderStroke(1.dp, CardBorder),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(text = strings.socialScreen.cancelRequest, fontSize = 12.sp)
                }
            }
        }
    }
}

