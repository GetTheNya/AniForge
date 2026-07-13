package moe.GetTheNya.AniForge.ui.profile

import android.util.Log
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch
import moe.GetTheNya.AniForge.BuildConfig
import moe.GetTheNya.AniForge.core.database.util.AppLogger
import moe.GetTheNya.AniForge.ui.navigation.NavController
import moe.GetTheNya.AniForge.ui.navigation.Screen
import moe.GetTheNya.AniForge.core.network.UserProfileDto
import moe.GetTheNya.AniForge.ui.theme.*
import moe.GetTheNya.AniForge.ui.utils.statusConfigs
import moe.GetTheNya.AniForge.ui.localization.getPlural

private val GoogleLogoVector: ImageVector
    @Composable
    get() = remember {
        ImageVector.Builder(
            name = "GoogleLogo",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Red Section
            path(fill = SolidColor(Color(0xFFEA4335))) {
                moveTo(12f, 5.38f)
                curveTo(13.62f, 5.38f, 15.06f, 5.94f, 16.21f, 7.02f)
                lineTo(19.36f, 3.87f)
                curveTo(17.45f, 2.09f, 14.97f, 1f, 12f, 1f)
                curveTo(7.7f, 1f, 3.99f, 3.47f, 2.18f, 7.06f)
                lineTo(5.84f, 9.9f)
                curveTo(6.71f, 7.3f, 9.14f, 5.38f, 12f, 5.38f)
                close()
            }
            // Blue Section
            path(fill = SolidColor(Color(0xFF4285F4))) {
                moveTo(22.56f, 12.25f)
                curveTo(22.56f, 11.47f, 22.49f, 10.72f, 22.36f, 10f)
                lineTo(12f, 10f)
                lineTo(12f, 14.26f)
                lineTo(17.92f, 14.26f)
                curveTo(17.66f, 15.63f, 16.88f, 16.79f, 15.71f, 17.57f)
                lineTo(19.28f, 20.34f)
                curveTo(21.36f, 18.42f, 22.56f, 15.6f, 22.56f, 12.25f)
                close()
            }
            // Yellow Section
            path(fill = SolidColor(Color(0xFFFBBC05))) {
                moveTo(5.84f, 14.09f)
                curveTo(5.62f, 13.43f, 5.49f, 12.73f, 5.49f, 12f)
                curveTo(5.49f, 11.27f, 5.62f, 10.57f, 5.84f, 9.9f)
                lineTo(2.18f, 7.06f)
                curveTo(1.43f, 8.55f, 1f, 10.22f, 1f, 12f)
                curveTo(1f, 13.78f, 1.43f, 15.45f, 2.18f, 16.94f)
                lineTo(5.84f, 14.09f)
                close()
            }
            // Green Section
            path(fill = SolidColor(Color(0xFF34A853))) {
                moveTo(12f, 23f)
                curveTo(14.97f, 23f, 17.46f, 22.02f, 19.28f, 20.34f)
                lineTo(15.71f, 17.57f)
                curveTo(14.73f, 18.23f, 13.48f, 18.63f, 12f, 18.63f)
                curveTo(9.14f, 18.63f, 6.71f, 16.7f, 5.84f, 14.1f)
                lineTo(2.18f, 16.94f)
                curveTo(3.99f, 20.53f, 7.7f, 23f, 12f, 23f)
                close()
            }
        }.build()
    }

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    navController: NavController,
    onStudioClick: (Long) -> Unit,
    onGenreClick: (String) -> Unit,
    onCollectionClick: () -> Unit,
    onStatusClick: (String) -> Unit,
    unreadRequestsCount: Int = 0,
    friends: List<UserProfileDto> = emptyList(),
    modifier: Modifier = Modifier
) {
    val strings = moe.GetTheNya.AniForge.ui.localization.LocalLocaleStrings.current
    val userStats by viewModel.userStats.collectAsState()
    val bentoStats by viewModel.bentoStats.collectAsState()
    val trackingStats by viewModel.stats.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    val scrollState = rememberScrollState()
    val scrollOffset = scrollState.value

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    val snackbarHostState = remember { SnackbarHostState() }
    val isLoggingIn = remember { mutableStateOf(false) }

    val density = LocalDensity.current
    val expandedHeight = 80.dp
    val collapsedHeight = 56.dp
    val maxScrollPx = remember { with(density) { (expandedHeight - collapsedHeight).toPx() } }
    val collapseFraction = (scrollOffset.toFloat() / maxScrollPx).coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .statusBarsPadding()
    ) {
        // Scrollable Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(96.dp))

            // User Info or Sign In button
            if (currentUser != null) {
                if (currentUser!!.isProfileLoading) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, CardBorder, RoundedCornerShape(16.dp)),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = SurfaceDark
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp)
                                .height(64.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = NeonCoral,
                                modifier = Modifier.size(32.dp),
                                strokeWidth = 3.dp
                            )
                        }
                    }
                } else {
                    // Authenticated Profile Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                navController.navigate(Screen.AccountSettings)
                            },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = SurfaceDark
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                        .background(TransparentAccent)
                                        .border(2.dp, NeonCoral, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val avatarUrl = currentUser!!.avatarUrl
                                    if (!avatarUrl.isNullOrBlank()) {
                                        AsyncImage(
                                            model = avatarUrl,
                                            contentDescription = "User Avatar",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Text(
                                            text = currentUser!!.username.firstOrNull()?.uppercase() ?: "?",
                                            color = NeonCoral,
                                            fontSize = 24.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.width(16.dp))
                                
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = currentUser!!.username,
                                        color = TextPrimary,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = strings.accountSettings.title,
                                    tint = TextSecondary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
            } else {
                // Google Login Button (Premium Dark Glassmorphic Design)
                Button(
                    onClick = {
                        coroutineScope.launch {
                            try {
                                isLoggingIn.value = true
                                AppLogger.i("Auth", "Initializing Google Sign-In using Credential Manager...")
                                val googleIdOption = GetGoogleIdOption.Builder()
                                    .setFilterByAuthorizedAccounts(false)
                                    .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
                                    .setAutoSelectEnabled(true)
                                    .build()

                                val request = GetCredentialRequest.Builder()
                                    .addCredentialOption(googleIdOption)
                                    .build()

                                val result = credentialManager.getCredential(
                                    context = context,
                                    request = request
                                )

                                val credential = result.credential
                                if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                                    AppLogger.i("Auth", "Google Credential retrieved successfully.")
                                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                                    val idToken = googleIdTokenCredential.idToken
                                    
                                    viewModel.signInWithIdToken(idToken) { res ->
                                        isLoggingIn.value = false
                                        if (res.isFailure) {
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar("Sign-in failed: ${res.exceptionOrNull()?.message}")
                                            }
                                        }
                                    }
                                } else {
                                    isLoggingIn.value = false
                                    AppLogger.w("Auth", "Unexpected credential type retrieved: ${credential.type}")
                                }
                            } catch (e: GetCredentialException) {
                                isLoggingIn.value = false
                                AppLogger.e("Auth", "Credential Manager failed: ${e.message}", e)
                            } catch (e: Exception) {
                                isLoggingIn.value = false
                                AppLogger.e("Auth", "Google Sign-in flow encountered an error: ${e.message}", e)
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Sign-in failed: ${e.message}")
                                }
                            }
                        }
                    },
                    enabled = !isLoggingIn.value,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SurfaceDark.copy(alpha = 0.85f),
                        contentColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, CardBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    if (isLoggingIn.value) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = NeonCoral,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = GoogleLogoVector,
                                contentDescription = null,
                                tint = Color.Unspecified,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = strings.profileScreen.signInWithGoogle,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                        }
                    }
                }
            }

            moe.GetTheNya.AniForge.ui.bento.BentoDashboardGrid(
                stats = userStats,
                bentoData = bentoStats,
                trackingStats = trackingStats,
                onStudioClick = onStudioClick,
                onGenreClick = onGenreClick,
                onCollectionClick = onCollectionClick,
                onStatusClick = onStatusClick,
                friends = friends,
                onSocialClick = { navController.navigate(Screen.Social) }
            )

            Spacer(modifier = Modifier.height(110.dp))
        }

        // Floating Header Overlay
        val headerHeight = lerp(expandedHeight, collapsedHeight, collapseFraction)
        val verticalPadding = lerp(16.dp, 0.dp, collapseFraction)
        val titleSize = (28 - (28 - 20) * collapseFraction).sp

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(headerHeight)
                .background(BackgroundDark)
                .padding(horizontal = 24.dp)
                .padding(vertical = verticalPadding)
                .zIndex(5f),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = strings.profileScreen.userProfile,
                color = TextPrimary,
                fontSize = titleSize,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (currentUser != null) {
                    Box {
                        IconButton(
                            onClick = { navController.navigate(Screen.Social) },
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(SurfaceDark)
                                .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Group,
                                contentDescription = strings.socialScreen.name,
                                tint = TextPrimary
                            )
                        }
                        if (unreadRequestsCount > 0) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .align(Alignment.TopEnd)
                                    .clip(CircleShape)
                                    .background(NeonCoral)
                            )
                        }
                    }
                }
                IconButton(
                    onClick = { navController.navigate(Screen.Settings()) },
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceDark)
                        .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = strings.misc.settings,
                        tint = TextPrimary
                    )
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 80.dp) // Offset slightly above bottom navigation if needed
        )
    }
}
