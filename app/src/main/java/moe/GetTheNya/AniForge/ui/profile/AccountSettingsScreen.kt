package moe.GetTheNya.AniForge.ui.profile

import android.widget.Toast
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt
import moe.GetTheNya.AniForge.ui.navigation.NavController
import moe.GetTheNya.AniForge.ui.navigation.Screen
import moe.GetTheNya.AniForge.ui.theme.*
import moe.GetTheNya.AniForge.ui.localization.LocalLocaleStrings

@Composable
fun AccountSettingsScreen(
    viewModel: AccountSettingsViewModel,
    navController: NavController,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalLocaleStrings.current
    val username by viewModel.username.collectAsState()
    val validationError by viewModel.validationError.collectAsState()
    val saveError by viewModel.saveError.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val saveSuccess by viewModel.saveSuccess.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    val context = LocalContext.current

    LaunchedEffect(currentUser) {
        if (currentUser == null) {
            onBack()
        }
    }

    var showSignOutDialog by remember { mutableStateOf(false) }

    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var cropBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoadingBitmap by remember { mutableStateOf(false) }

    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            selectedImageUri = uri
        }
    }

    LaunchedEffect(selectedImageUri) {
        val uri = selectedImageUri
        if (uri != null) {
            isLoadingBitmap = true
            cropBitmap = withContext(Dispatchers.IO) {
                try {
                    val contentResolver = context.contentResolver
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        val source = ImageDecoder.createSource(contentResolver, uri)
                        ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                            decoder.isMutableRequired = true
                            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                        }
                    } else {
                        @Suppress("DEPRECATION")
                        android.provider.MediaStore.Images.Media.getBitmap(contentResolver, uri)
                            .copy(Bitmap.Config.ARGB_8888, true)
                    }
                } catch (e: Exception) {
                    Log.e("AccountSettings", "Failed to decode bitmap: ${e.message}", e)
                    null
                }
            }
            isLoadingBitmap = false
        } else {
            cropBitmap = null
        }
    }

    LaunchedEffect(cropBitmap) {
        scale = 1f
        offset = Offset.Zero
    }

    LaunchedEffect(saveSuccess) {
        if (saveSuccess) {
            Toast.makeText(context, strings.accountSettings.success, Toast.LENGTH_SHORT).show()
            viewModel.clearSaveSuccess()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .statusBarsPadding()
            .padding(horizontal = 24.dp)
            .navigationBarsPadding()
    ) {
        // Top Navigation Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(40.dp)
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
                text = strings.accountSettings.title,
                color = TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Card Container
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CardBorder, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = SurfaceDark
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Avatar Option
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(TransparentAccent)
                            .border(2.dp, if (!currentUser?.avatarUrl.isNullOrBlank()) CardBorder else NeonCoral, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        val avatarUrl = currentUser?.avatarUrl
                        if (!avatarUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = avatarUrl,
                                contentDescription = "User Avatar",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text(
                                text = currentUser?.username?.firstOrNull()?.uppercase() ?: "?",
                                color = NeonCoral,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = strings.accountSettings.avatarLabel,
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            pickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCoral),
                        border = BorderStroke(1.dp, NeonCoral),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = strings.accountSettings.avatarChangeBtn,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                HorizontalDivider(color = CardBorder, thickness = 1.dp)

                // Username Field
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = username,
                        onValueChange = { viewModel.onUsernameChanged(it) },
                        enabled = !isSaving,
                        label = { Text(strings.accountSettings.usernameLabel) },
                        placeholder = { Text(strings.accountSettings.usernamePlaceholder) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            disabledTextColor = TextSecondary,
                            focusedBorderColor = NeonCoral,
                            unfocusedBorderColor = CardBorder,
                            disabledBorderColor = CardBorder.copy(alpha = 0.5f),
                            focusedLabelColor = NeonCoral,
                            unfocusedLabelColor = TextSecondary,
                            disabledLabelColor = TextSecondary.copy(alpha = 0.5f),
                            cursorColor = NeonCoral,
                            focusedContainerColor = BackgroundDark,
                            unfocusedContainerColor = BackgroundDark,
                            disabledContainerColor = BackgroundDark.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Real-time Errors
                    val activeErrorMsg = when {
                        validationError != null -> when (validationError) {
                            UsernameValidator.ErrorType.TOO_SHORT -> strings.accountSettings.errorTooShort
                            UsernameValidator.ErrorType.TOO_LONG -> strings.accountSettings.errorTooLong
                            UsernameValidator.ErrorType.INVALID_CHARACTERS -> strings.accountSettings.errorInvalidChars
                            else -> null
                        }
                        saveError != SaveError.None -> when (saveError) {
                            SaveError.UsernameTaken -> strings.accountSettings.errorAlreadyTaken
                            SaveError.ServerRejected -> strings.accountSettings.errorServerRejected
                            SaveError.NetworkError -> strings.accountSettings.errorGeneric
                            else -> null
                        }
                        else -> null
                    }

                    if (activeErrorMsg != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = activeErrorMsg,
                            color = Color(0xFFE53935),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }

                // Save Button
                Button(
                    onClick = { viewModel.saveUsername() },
                    enabled = !isSaving && validationError == null && username.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonCoral,
                        contentColor = TextPrimary,
                        disabledContainerColor = NeonCoral.copy(alpha = 0.25f),
                        disabledContentColor = TextPrimary.copy(alpha = 0.4f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = TextPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = strings.accountSettings.save,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Destructive Sign Out Button at the bottom
        Button(
            onClick = {
                showSignOutDialog = true
            },
            enabled = !isSaving,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0x1AE53935),
                contentColor = Color(0xFFE53935),
                disabledContainerColor = Color(0x0DE53935),
                disabledContentColor = Color(0x66E53935)
            ),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color(0x33E53935)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
                .height(48.dp)
        ) {
            Text(
                text = strings.accountSettings.signOut,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }

    if (showSignOutDialog) {
        AlertDialog(
            modifier = Modifier.border(
                width = 1.dp,
                color = CardBorder,
                shape = RoundedCornerShape(24.dp)
            ),
            onDismissRequest = { showSignOutDialog = false },
            title = {
                Text(
                    text = strings.accountSettings.signOutConfirmTitle,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = strings.accountSettings.signOutConfirmText,
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSignOutDialog = false
                        viewModel.signOut()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE53935),
                        contentColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = strings.accountSettings.signOutConfirmOk,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showSignOutDialog = false
                    }
                ) {
                    Text(
                        text = strings.accountSettings.signOutConfirmCancel,
                        color = TextSecondary
                    )
                }
            },
            containerColor = AlertBackground,
            shape = RoundedCornerShape(24.dp)
        )
    }

    if (selectedImageUri != null) {
        Dialog(
            onDismissRequest = {
                selectedImageUri = null
                cropBitmap = null
            },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = true,
                dismissOnClickOutside = false
            )
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color.Black
            ) {
                BoxWithConstraints(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val parentWidthPx = constraints.maxWidth.toFloat()
                    val parentHeightPx = constraints.maxHeight.toFloat()
                    
                    val cropBoxSizePx = parentWidthPx
                    val cropBoxSizeDp = maxWidth
                    
                    val bitmap = cropBitmap
                    if (bitmap == null || isLoadingBitmap) {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center),
                            color = NeonCoral
                        )
                    } else {
                        val w = bitmap.width.toFloat()
                        val h = bitmap.height.toFloat()
                        val aspectRatio = w / h
                        
                        val displayHeightPx = if (w >= h) cropBoxSizePx else cropBoxSizePx / aspectRatio
                        val displayWidthPx = if (w >= h) cropBoxSizePx * aspectRatio else cropBoxSizePx
                        
                        val displayWidthDp = with(density) { displayWidthPx.toDp() }
                        val displayHeightDp = with(density) { displayHeightPx.toDp() }
                        
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(Unit) {
                                    detectTransformGestures { _, pan, zoom, _ ->
                                        scale = (scale * zoom).coerceIn(1f, 5f)
                                        
                                        val maxOffsetX = (displayWidthPx * scale) / 2f - parentWidthPx / 2f
                                        val minOffsetX = -maxOffsetX
                                        val maxOffsetY = (displayHeightPx * scale) / 2f - cropBoxSizePx / 2f
                                        val minOffsetY = -maxOffsetY
                                        
                                        val targetX = offset.x + pan.x
                                        val targetY = offset.y + pan.y
                                        
                                        offset = Offset(
                                            x = targetX.coerceIn(minOffsetX, maxOffsetX),
                                            y = targetY.coerceIn(minOffsetY, maxOffsetY)
                                        )
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(displayWidthDp, displayHeightDp)
                                    .graphicsLayer {
                                        scaleX = scale
                                        scaleY = scale
                                        translationX = offset.x
                                        translationY = offset.y
                                    },
                                contentScale = ContentScale.FillBounds
                            )

                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val circleRadius = size.width / 2f
                                val path = Path().apply {
                                    addOval(Rect(center, circleRadius))
                                }
                                clipPath(path, clipOp = ClipOp.Difference) {
                                    drawRect(
                                        color = Color.Black.copy(alpha = 0.7f)
                                    )
                                }
                                drawCircle(
                                    center = center,
                                    radius = circleRadius,
                                    color = Color.White,
                                    style = Stroke(width = 2.dp.toPx())
                                )
                                drawCircle(
                                    center = center,
                                    radius = circleRadius,
                                    color = Color.White.copy(alpha = 0.5f),
                                    style = Stroke(
                                        width = 1.dp.toPx(),
                                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                                    )
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.TopCenter)
                                    .background(Color.Black.copy(alpha = 0.5f))
                                    .statusBarsPadding()
                                    .padding(horizontal = 20.dp, vertical = 16.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = strings.accountSettings.avatarEditDialogTitle,
                                    color = TextPrimary,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.BottomCenter)
                                    .background(Color.Black.copy(alpha = 0.5f))
                                    .navigationBarsPadding()
                                    .padding(horizontal = 24.dp, vertical = 24.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        selectedImageUri = null
                                        cropBitmap = null
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                                    border = BorderStroke(1.dp, CardBorder)
                                ) {
                                    Text(strings.accountSettings.avatarCancel)
                                }

                                Button(
                                    onClick = {
                                        coroutineScope.launch {
                                            val cropped = withContext(Dispatchers.IO) {
                                                try {
                                                    val cropLeftDisp = displayWidthPx / 2f - (parentWidthPx / 2f + offset.x) / scale
                                                    val cropTopDisp = displayHeightPx / 2f - (cropBoxSizePx / 2f + offset.y) / scale
                                                    val cropWidthDisp = cropBoxSizePx / scale
                                                    val cropHeightDisp = cropBoxSizePx / scale
                                                    
                                                    val left = (cropLeftDisp / displayWidthPx) * bitmap.width
                                                    val top = (cropTopDisp / displayHeightPx) * bitmap.height
                                                    val width = (cropWidthDisp / displayWidthPx) * bitmap.width
                                                    val height = (cropHeightDisp / displayHeightPx) * bitmap.height
                                                    
                                                    val finalLeft = left.roundToInt().coerceIn(0, bitmap.width - 1)
                                                    val finalTop = top.roundToInt().coerceIn(0, bitmap.height - 1)
                                                    val finalWidth = width.roundToInt().coerceAtMost(bitmap.width - finalLeft)
                                                    val finalHeight = height.roundToInt().coerceAtMost(bitmap.height - finalTop)
                                                    
                                                    Bitmap.createBitmap(bitmap, finalLeft, finalTop, finalWidth, finalHeight)
                                                } catch (e: Exception) {
                                                    Log.e("AccountSettings", "Error cropping bitmap: ${e.message}", e)
                                                    null
                                                }
                                            }

                                            if (cropped != null) {
                                                val optimizedBytes = AvatarCompressionEngine.optimizeAvatar(cropped)
                                                cropped.recycle()

                                                viewModel.uploadAvatar(optimizedBytes) { uploadResult ->
                                                    if (uploadResult.isSuccess) {
                                                        Toast.makeText(context, strings.accountSettings.avatarUpdateSuccess, Toast.LENGTH_SHORT).show()
                                                    } else {
                                                        Toast.makeText(context, String.format(strings.accountSettings.avatarUploadFailed, uploadResult.exceptionOrNull()?.message ?: ""), Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            }
                                            selectedImageUri = null
                                            cropBitmap = null
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = NeonCoral)
                                ) {
                                    Text(strings.accountSettings.avatarSave, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
