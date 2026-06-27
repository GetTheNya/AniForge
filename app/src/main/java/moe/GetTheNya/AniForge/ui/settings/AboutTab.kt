package moe.GetTheNya.AniForge.ui.settings

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import moe.GetTheNya.AniForge.BuildConfig
import moe.GetTheNya.AniForge.ui.localization.LocalLocaleStrings
import moe.GetTheNya.AniForge.ui.theme.*
import moe.GetTheNya.AniForge.ui.update.UpdateViewModel
import moe.GetTheNya.AniForge.ui.update.UpdateManager
import kotlin.math.roundToInt

@Composable
fun AboutTab(
    updateViewModel: UpdateViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val strings = LocalLocaleStrings.current
    val updateState by updateViewModel.updateState.collectAsState()

    var wasChecking by remember { mutableStateOf(false) }

    LaunchedEffect(updateState) {
        if (updateState is UpdateManager.UpdateState.Checking) {
            wasChecking = true
        } else if (wasChecking && updateState is UpdateManager.UpdateState.Idle) {
            wasChecking = false
            android.widget.Toast.makeText(
                context,
                strings.settingsScreen.updateUpToDate,
                android.widget.Toast.LENGTH_SHORT
            ).show()
        } else if (updateState is UpdateManager.UpdateState.ReadyToInstall) {
            updateViewModel.installUpdate((updateState as UpdateManager.UpdateState.ReadyToInstall).apkFile)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // About App Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(SurfaceDark)
                .border(1.dp, CardBorder, RoundedCornerShape(24.dp))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header: App Branding Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                androidx.compose.ui.viewinterop.AndroidView(
                    factory = { ctx ->
                        android.widget.ImageView(ctx).apply {
                            setImageDrawable(ctx.packageManager.getApplicationIcon(ctx.packageName))
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
                Column {
                    Text(
                        text = "AniForge",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                    Text(
                        text = strings.settingsScreen.aboutApp,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextSecondary
                        )
                    )
                }
            }

            HorizontalDivider(color = CardBorder, thickness = 1.dp)

            val appSynopsisText = strings.settingsScreen.appSynopsis
            val annotatedSynopsis = remember(appSynopsisText) {
                buildAnnotatedString {
                    append(appSynopsisText)

                    val aniListStart = appSynopsisText.indexOf("AniList API")
                    if (aniListStart != -1) {
                        addStyle(
                            style = SpanStyle(
                                color = NeonCoral,
                                fontWeight = FontWeight.Bold
                            ),
                            start = aniListStart,
                            end = aniListStart + "AniList API".length
                        )
                        addStringAnnotation(
                            tag = "URL",
                            annotation = "https://anilist.co/",
                            start = aniListStart,
                            end = aniListStart + "AniList API".length
                        )
                    }

                    val hikkaStart = appSynopsisText.indexOf("Hikka.io")
                    if (hikkaStart != -1) {
                        addStyle(
                            style = SpanStyle(
                                color = NeonCoral,
                                fontWeight = FontWeight.Bold
                            ),
                            start = hikkaStart,
                            end = hikkaStart + "Hikka.io".length
                        )
                        addStringAnnotation(
                            tag = "URL",
                            annotation = "https://hikka.io/",
                            start = hikkaStart,
                            end = hikkaStart + "Hikka.io".length
                        )
                    }

                    val tmdbStart = appSynopsisText.indexOf("TMDB")
                    if (tmdbStart != -1) {
                        addStyle(
                            style = SpanStyle(
                                color = NeonCoral,
                                fontWeight = FontWeight.Bold
                            ),
                            start = tmdbStart,
                            end = tmdbStart + "TMDB".length
                        )
                        addStringAnnotation(
                            tag = "URL",
                            annotation = "https://www.themoviedb.org/",
                            start = tmdbStart,
                            end = tmdbStart + "TMDB".length
                        )
                    }
                }
            }

            ClickableText(
                text = annotatedSynopsis,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = TextPrimary,
                    lineHeight = 20.sp
                ),
                modifier = Modifier.fillMaxWidth(),
                onClick = { offset ->
                    annotatedSynopsis.getStringAnnotations(tag = "URL", start = offset, end = offset)
                        .firstOrNull()?.let { annotation ->
                            val intent = android.content.Intent(
                                android.content.Intent.ACTION_VIEW,
                                android.net.Uri.parse(annotation.item)
                            )
                            context.startActivity(intent)
                        }
                }
            )

            HorizontalDivider(color = CardBorder, thickness = 1.dp)

            // Info rows
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.settingsScreen.appVersion,
                    color = TextSecondary,
                    fontSize = 13.sp
                )
                Text(
                    text = BuildConfig.VERSION_NAME,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.settingsScreen.developer,
                    color = TextSecondary,
                    fontSize = 13.sp
                )
                Text(
                    text = "GetTheNya",
                    color = NeonCoral,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable {
                        val intent = android.content.Intent(
                            android.content.Intent.ACTION_VIEW,
                            android.net.Uri.parse("https://github.com/GetTheNya")
                        )
                        context.startActivity(intent)
                    }
                )
            }

            HorizontalDivider(color = CardBorder, thickness = 1.dp)

            AnimatedContent(
                targetState = updateState,
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                },
                label = "updateSectionContent"
            ) { state ->
                when (state) {
                    is UpdateManager.UpdateState.Idle -> {
                        Button(
                            onClick = { updateViewModel.checkForUpdates() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeonCoral,
                                contentColor = BackgroundDark
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = strings.settingsScreen.checkForUpdates,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    is UpdateManager.UpdateState.Checking -> {
                        Button(
                            onClick = {},
                            enabled = false,
                            colors = ButtonDefaults.buttonColors(
                                disabledContainerColor = Color(0x33FFFFFF),
                                disabledContentColor = TextSecondary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            CircularProgressIndicator(
                                color = TextSecondary,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    is UpdateManager.UpdateState.UpdateAvailable -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF0F0F13))
                                .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = strings.settingsScreen.updateAvailable,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = state.versionName,
                                        color = TextSecondary,
                                        fontSize = 12.sp
                                    )
                                }
                                TextButton(
                                    onClick = { updateViewModel.reset() }
                                ) {
                                    Text(
                                        text = strings.libraryScreen.cancel,
                                        color = TextSecondary,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                            Button(
                                onClick = { updateViewModel.downloadUpdate(state.downloadUrl) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = NeonCoral,
                                    contentColor = BackgroundDark
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = strings.settingsScreen.download,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    is UpdateManager.UpdateState.Downloading -> {
                        val percent = (state.progress * 100).roundToInt()
                        Column(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = strings.settingsScreen.downloading,
                                    color = TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "$percent%",
                                    color = NeonCoral,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFF1E1E24))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(state.progress.coerceIn(0f, 1f))
                                        .fillMaxHeight()
                                        .background(
                                            Brush.horizontalGradient(
                                                colors = listOf(NeonCoral, ElectricViolet)
                                            )
                                        )
                                )
                            }
                        }
                    }
                    is UpdateManager.UpdateState.ReadyToInstall -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { updateViewModel.installUpdate(state.apkFile) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = NeonCoral,
                                    contentColor = BackgroundDark
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = strings.settingsScreen.readyToInstall,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            TextButton(
                                onClick = { updateViewModel.reset() },
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            ) {
                                Text(
                                    text = strings.libraryScreen.cancel,
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                    is UpdateManager.UpdateState.Error -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0x11EF4444))
                                .border(1.dp, Color(0x33EF4444), RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "${strings.settingsScreen.errorPrefix}${state.message}",
                                color = Color(0xFFEF4444),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                TextButton(
                                    onClick = { updateViewModel.reset() }
                                ) {
                                    Text(
                                        text = strings.libraryScreen.cancel,
                                        color = TextSecondary,
                                        fontSize = 12.sp
                                    )
                                }
                                TextButton(
                                    onClick = { updateViewModel.checkForUpdates() }
                                ) {
                                    Text(
                                        text = strings.misc.retry,
                                        color = NeonCoral,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
