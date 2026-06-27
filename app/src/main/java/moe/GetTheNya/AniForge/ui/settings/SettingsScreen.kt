package moe.GetTheNya.AniForge.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Delete
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import android.net.Uri
import moe.GetTheNya.AniForge.ui.settings.ImportViewModel
import moe.GetTheNya.AniForge.ui.settings.MatchPriority
import moe.GetTheNya.AniForge.BuildConfig
import moe.GetTheNya.AniForge.ui.update.UpdateViewModel
import moe.GetTheNya.AniForge.ui.update.UpdateManager
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.Brush
import kotlin.math.roundToInt
import androidx.compose.foundation.text.ClickableText
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import moe.GetTheNya.AniForge.R
import moe.GetTheNya.AniForge.ui.localization.LocalLocaleStrings
import moe.GetTheNya.AniForge.ui.navigation.NavController
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import moe.GetTheNya.AniForge.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    navController: NavController,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = { navController.popBackStack() }
) {
    val context = LocalContext.current
    val strings = LocalLocaleStrings.current
    val preferUk by viewModel.preferUkTitles.collectAsState()
    val show18Plus by viewModel.show18Plus.collectAsState()
    var showWarningDialog by remember { mutableStateOf(false) }
    val currentLangCode by viewModel.currentLanguage.collectAsState()
    val availableLangs by viewModel.availableLanguages.collectAsState()
    val isUpdating by viewModel.isUpdating.collectAsState()
    val metadata by viewModel.catalogMetadata.collectAsState()

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
    val updateState by updateViewModel.updateState.collectAsState()
    val importState by importViewModel.state.collectAsState()
    val matchPriority by importViewModel.matchPriority.collectAsState()
    val importSyncStatus by importViewModel.syncStatus.collectAsState()
    val importSyncRating by importViewModel.syncRating.collectAsState()
    val pendingCount by importViewModel.pendingImportsCount.collectAsState()
    val failedImports by importViewModel.failedImports.collectAsState()

    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            selectedFileUri = uri
        }
    }

    val gestureCenter by viewModel.gestureCenter.collectAsState()
    val gestureUp by viewModel.gestureUp.collectAsState()
    val gestureDown by viewModel.gestureDown.collectAsState()
    val gestureLeft by viewModel.gestureLeft.collectAsState()
    val gestureRight by viewModel.gestureRight.collectAsState()

    val scrollState = rememberScrollState()

    var dropdownExpanded by remember { mutableStateOf(false) }
    val currentLangName = availableLangs[currentLangCode] ?: currentLangCode

    val actionLabels = remember(strings) {
        mapOf(
            "OpenDetails" to strings.settingsScreen.actionOpenDetails,
            "OpenWatchStatusPicker" to strings.settingsScreen.actionOpenWatchStatusPicker,
            "ShareLink" to strings.settingsScreen.actionShareLink,
            "ScoreSlider" to strings.settingsScreen.actionScoreSlider,
            "EpisodeSlider" to strings.settingsScreen.actionEpisodeSlider,
            "None" to strings.settingsScreen.actionNone
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .statusBarsPadding()
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp)
            .padding(bottom = 36.dp)
    ) {
        // Sticky Header with Back Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
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

        // Settings Form Container
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(SurfaceDark)
                .border(1.dp, CardBorder, RoundedCornerShape(24.dp))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 1. Language Dropdown
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = strings.settingsScreen.language,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                
                ExposedDropdownMenuBox(
                    expanded = dropdownExpanded,
                    onExpandedChange = { dropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = currentLangName,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded)
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = NeonCoral,
                            unfocusedBorderColor = CardBorder,
                            focusedContainerColor = Color(0xFF0F0F13),
                            unfocusedContainerColor = Color(0xFF0F0F13)
                        ),
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    
                    ExposedDropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false },
                        modifier = Modifier.background(SurfaceDark)
                    ) {
                        availableLangs.forEach { (code, name) ->
                            DropdownMenuItem(
                                text = { 
                                    Text(
                                        text = name,
                                        color = TextPrimary,
                                        fontWeight = if (code == currentLangCode) FontWeight.Bold else FontWeight.Normal
                                    ) 
                                },
                                onClick = {
                                    viewModel.setSelectedLanguage(code)
                                    dropdownExpanded = false
                                },
                                modifier = Modifier.background(SurfaceDark)
                            )
                        }
                    }
                }
            }

            HorizontalDivider(Modifier, thickness = 1.dp, color = CardBorder)

            // 2. Ukrainian Titles Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                    Text(
                        text = strings.settingsScreen.preferUkTitle,
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = strings.settingsScreen.preferUkDesc,
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
                Switch(
                    checked = preferUk,
                    onCheckedChange = viewModel::setPreferUkTitles,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = BackgroundDark,
                        checkedTrackColor = NeonCoral,
                        uncheckedThumbColor = TextSecondary,
                        uncheckedTrackColor = Color(0x33FFFFFF)
                    )
                )
            }

            HorizontalDivider(Modifier, thickness = 1.dp, color = CardBorder)

            // 3. 18+ (NSFW) Mode Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                    Text(
                        text = strings.settingsScreen.show18PlusTitle,
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = strings.settingsScreen.show18PlusDesc,
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
                Switch(
                    checked = show18Plus,
                    onCheckedChange = { checked ->
                        if (checked) {
                            showWarningDialog = true
                        } else {
                            viewModel.setShow18Plus(false)
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = BackgroundDark,
                        checkedTrackColor = NeonCoral,
                        uncheckedThumbColor = TextSecondary,
                        uncheckedTrackColor = Color(0x33FFFFFF)
                    )
                )
            }

        }

        Spacer(modifier = Modifier.height(24.dp))

        // 4. Quick Action Gestures Configuration Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(SurfaceDark)
                .border(1.dp, CardBorder, RoundedCornerShape(24.dp))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column {
                Text(
                    text = strings.settingsScreen.gesturesHeader,
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = strings.settingsScreen.gesturesDesc,
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }

            HorizontalDivider(color = CardBorder, thickness = 1.dp)

            GestureConfigRow(
                label = strings.settingsScreen.gestureCenter,
                selectedAction = gestureCenter,
                options = listOf("OpenDetails", "OpenWatchStatusPicker", "ShareLink", "None"),
                actionLabels = actionLabels,
                onActionSelected = viewModel::setGestureCenter
            )

            GestureConfigRow(
                label = strings.settingsScreen.gestureUp,
                selectedAction = gestureUp,
                options = listOf("OpenDetails", "OpenWatchStatusPicker", "ShareLink", "None", "ScoreSlider", "EpisodeSlider"),
                actionLabels = actionLabels,
                onActionSelected = viewModel::setGestureUp
            )

            GestureConfigRow(
                label = strings.settingsScreen.gestureDown,
                selectedAction = gestureDown,
                options = listOf("OpenDetails", "OpenWatchStatusPicker", "ShareLink", "None", "ScoreSlider", "EpisodeSlider"),
                actionLabels = actionLabels,
                onActionSelected = viewModel::setGestureDown
            )

            GestureConfigRow(
                label = strings.settingsScreen.gestureLeft,
                selectedAction = gestureLeft,
                options = listOf("OpenDetails", "OpenWatchStatusPicker", "ShareLink", "None", "ScoreSlider", "EpisodeSlider"),
                actionLabels = actionLabels,
                onActionSelected = viewModel::setGestureLeft
            )

            GestureConfigRow(
                label = strings.settingsScreen.gestureRight,
                selectedAction = gestureRight,
                options = listOf("OpenDetails", "OpenWatchStatusPicker", "ShareLink", "None", "ScoreSlider", "EpisodeSlider"),
                actionLabels = actionLabels,
                onActionSelected = viewModel::setGestureRight
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 3. Database Management Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(SurfaceDark)
                .border(1.dp, CardBorder, RoundedCornerShape(24.dp))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = strings.settingsScreen.databaseHeader,
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = strings.settingsScreen.hotswappingEnabled,
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0x1F00F5D4))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = strings.misc.ready,
                        color = CyberTeal,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            HorizontalDivider(color = CardBorder, thickness = 1.dp)

            // Active Slot and Catalog Stamp row (spaced between)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = strings.settingsScreen.activeSlot,
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                    Text(
                        text = metadata.activeSlot.uppercase(),
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = strings.settingsScreen.catalogStamp,
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                    Text(
                        text = "v${metadata.version}",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            HorizontalDivider(color = CardBorder, thickness = 1.dp)

            // Force Update Button row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.settingsScreen.forceUpdateDesc,
                    color = TextSecondary,
                    fontSize = 11.sp,
                    modifier = Modifier.weight(1f).padding(end = 16.dp)
                )
                Button(
                    onClick = viewModel::triggerForceDbUpdate,
                    enabled = !isUpdating,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonCoral,
                        contentColor = BackgroundDark,
                        disabledContainerColor = Color(0x33FFFFFF),
                        disabledContentColor = TextSecondary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isUpdating) {
                        CircularProgressIndicator(
                            color = TextSecondary,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(16.dp)
                        )
                    } else {
                        Text(
                            text = strings.settingsScreen.forceUpdate,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Data Management Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(SurfaceDark)
                .border(1.dp, CardBorder, RoundedCornerShape(24.dp))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = strings.settingsScreen.dataManagementHeader,
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            HorizontalDivider(color = CardBorder, thickness = 1.dp)

            // Export Data Row (Disabled placeholder)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer(alpha = 0.5f)
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Storage,
                        contentDescription = null,
                        tint = TextSecondary
                    )
                    Text(
                        text = strings.settingsScreen.exportDataTitle,
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Text(
                    text = strings.settingsScreen.exportDataComingSoon,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            HorizontalDivider(color = CardBorder, thickness = 1.dp)

            if (pendingCount > 0) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFF2E1A47), Color(0xFF1F1035))
                            )
                        )
                        .border(1.dp, Color(0xFF9067C6).copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text(
                                text = strings.settingsScreen.incompleteImportTitle,
                                color = TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = strings.settingsScreen.incompleteImportSubtitle.replace("{count}", pendingCount.toString()),
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                        
                        Button(
                            onClick = { importViewModel.resumeImportResolution() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeonCoral,
                                contentColor = BackgroundDark
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text(
                                text = strings.settingsScreen.incompleteImportResumeBtn,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                
                HorizontalDivider(color = CardBorder, thickness = 1.dp)
            }

            // Import from Anixart Row (Active Trigger)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { filePickerLauncher.launch("text/*") }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudUpload,
                        contentDescription = null,
                        tint = NeonCoral
                    )
                    Text(
                        text = strings.settingsScreen.importAnixartTitle,
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 5. About App Card
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
                    
                    // Style and annotate "AniList API"
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

                    // Style and annotate "Hikka.io"
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

                    // Style and annotate "TMDB"
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
            // Row 1: Localized "Version" / "Версія"
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

            // Row 2: Localized "Developer" / "Розробник"
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

            // State A (Up to Date): Trigger a low-emphasis Toast when active check determines we are up to date
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
                    // Trigger package installer immediately when ready
                    updateViewModel.installUpdate((updateState as UpdateManager.UpdateState.ReadyToInstall).apkFile)
                }
            }

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
    }

    if (showWarningDialog) {
        AlertDialog(
            modifier = Modifier
                .border(
                    width = 1.dp,
                    color = CardBorder,
                    shape = RoundedCornerShape(24.dp)
                ),
            onDismissRequest = { showWarningDialog = false },
            title = {
                Text(
                    text = strings.settingsScreen.confirm18PlusTitle,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = strings.settingsScreen.confirm18PlusText,
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.setShow18Plus(true)
                        showWarningDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonCoral,
                        contentColor = BackgroundDark
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = strings.settingsScreen.confirm18PlusOk,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showWarningDialog = false
                    }
                ) {
                    Text(
                        text = strings.settingsScreen.confirm18PlusCancel,
                        color = TextSecondary
                    )
                }
            },
            containerColor = AlertBackground,
            shape = RoundedCornerShape(24.dp)
        )
    }

    // --- Anixart Import Wizard Dialogs ---

    if (selectedFileUri != null) {
        AlertDialog(
            modifier = Modifier
                .border(
                    width = 1.dp,
                    color = CardBorder,
                    shape = RoundedCornerShape(24.dp)
                ),
            onDismissRequest = { selectedFileUri = null },
            title = {
                Text(
                    text = strings.settingsScreen.anixartImportHeader,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = strings.settingsScreen.anixartImportDesc,
                        color = TextSecondary,
                        fontSize = 13.sp
                    )

                    HorizontalDivider(color = CardBorder, thickness = 1.dp)

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = strings.settingsScreen.anixartImportMatchPriority,
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (matchPriority == MatchPriority.ORIGINAL_TITLE) Color(0x33FF4081) else SurfaceCardDark)
                                .border(1.dp, if (matchPriority == MatchPriority.ORIGINAL_TITLE) NeonCoral else CardBorder, RoundedCornerShape(8.dp))
                                .clickable { importViewModel.setMatchPriority(MatchPriority.ORIGINAL_TITLE) }
                                .padding(8.dp)
                        ) {
                            RadioButton(
                                selected = matchPriority == MatchPriority.ORIGINAL_TITLE,
                                onClick = { importViewModel.setMatchPriority(MatchPriority.ORIGINAL_TITLE) },
                                colors = RadioButtonDefaults.colors(selectedColor = NeonCoral, unselectedColor = TextSecondary)
                            )
                            Text(
                                text = strings.settingsScreen.anixartImportPriorityOriginal,
                                color = TextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (matchPriority == MatchPriority.ALTERNATIVE_TITLE) Color(0x33FF4081) else SurfaceCardDark)
                                .border(1.dp, if (matchPriority == MatchPriority.ALTERNATIVE_TITLE) NeonCoral else CardBorder, RoundedCornerShape(8.dp))
                                .clickable { importViewModel.setMatchPriority(MatchPriority.ALTERNATIVE_TITLE) }
                                .padding(8.dp)
                        ) {
                            RadioButton(
                                selected = matchPriority == MatchPriority.ALTERNATIVE_TITLE,
                                onClick = { importViewModel.setMatchPriority(MatchPriority.ALTERNATIVE_TITLE) },
                                colors = RadioButtonDefaults.colors(selectedColor = NeonCoral, unselectedColor = TextSecondary)
                            )
                            Text(
                                text = strings.settingsScreen.anixartImportPriorityAlternative,
                                color = TextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    HorizontalDivider(color = CardBorder, thickness = 1.dp)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = strings.settingsScreen.anixartImportSyncStatus,
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Switch(
                            checked = importSyncStatus,
                            onCheckedChange = { importViewModel.setSyncStatus(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = BackgroundDark,
                                checkedTrackColor = NeonCoral,
                                uncheckedThumbColor = TextSecondary,
                                uncheckedTrackColor = Color(0x33FFFFFF)
                            )
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = strings.settingsScreen.anixartImportSyncRating,
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Switch(
                            checked = importSyncRating,
                            onCheckedChange = { importViewModel.setSyncRating(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = BackgroundDark,
                                checkedTrackColor = NeonCoral,
                                uncheckedThumbColor = TextSecondary,
                                uncheckedTrackColor = Color(0x33FFFFFF)
                            )
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        selectedFileUri?.let { uri ->
                            importViewModel.startImport(context, uri)
                        }
                        selectedFileUri = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCoral, contentColor = BackgroundDark),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(text = strings.settingsScreen.importAction, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedFileUri = null }) {
                    Text(text = strings.libraryScreen.cancel, color = TextSecondary)
                }
            },
            containerColor = AlertBackground,
            shape = RoundedCornerShape(24.dp)
        )
    }

    if (importState.isProcessing) {
        AlertDialog(
            modifier = Modifier
                .border(
                    width = 1.dp,
                    color = CardBorder,
                    shape = RoundedCornerShape(24.dp)
                ),
            onDismissRequest = {},
            title = {
                Text(
                    text = strings.settingsScreen.anixartImportProcessing,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        color = NeonCoral,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = strings.settingsScreen.anixartImportProgressDesc,
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = strings.settingsScreen.anixartImportSuccessText, color = CyberTeal, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text(text = importState.processedSuccessCount.toString(), color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = strings.settingsScreen.anixartImportFailedText, color = Color(0xFFEF4444), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text(text = importState.failedCount.toString(), color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = strings.settingsScreen.anixartImportTotalText, color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text(text = importState.totalRecords.toString(), color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = {},
            containerColor = AlertBackground,
            shape = RoundedCornerShape(24.dp)
        )
    }

    if (importState.showSummary) {
        Dialog(
            onDismissRequest = { importViewModel.clearSummary() },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = BackgroundDark
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                ) {
                    // Header Title & Close Button
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = strings.settingsScreen.anixartImportHeader,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                        IconButton(onClick = { importViewModel.clearSummary() }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                        }
                    }

                    // Success & Failed stats card
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF0F0F13))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = strings.settingsScreen.anixartImportSuccessCount
                                    .replace("{success}", importState.processedSuccessCount.toString())
                                    .replace("{total}", importState.totalRecords.toString()),
                                color = CyberTeal,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = strings.settingsScreen.anixartImportFailedCount
                                    .replace("{failed}", importState.failedCount.toString()),
                                color = Color(0xFFEF4444),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Scrollable failed items LazyColumn area
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        if (failedImports.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    text = strings.settingsScreen.anixartImportAllMapped,
                                    color = TextSecondary,
                                    fontSize = 14.sp
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(
                                    items = failedImports,
                                    key = { it.id }
                                ) { item ->
                                    Column(
                                        modifier = Modifier
                                            .animateItem()
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(SurfaceCardDark)
                                            .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                                            .padding(12.dp)
                                    ) {
                                        Text(
                                            text = item.russianTitle.ifEmpty { item.originalTitle },
                                            color = TextPrimary,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        if (item.russianTitle.isNotEmpty() && item.originalTitle.isNotEmpty()) {
                                            Text(
                                                text = item.originalTitle,
                                                color = TextSecondary,
                                                fontSize = 11.sp,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            val statusLabel = when (item.targetStatus) {
                                                moe.GetTheNya.AniForge.core.database.entity.TargetStatus.CURRENT -> strings.misc.watching
                                                moe.GetTheNya.AniForge.core.database.entity.TargetStatus.PLANNING -> strings.misc.planning
                                                moe.GetTheNya.AniForge.core.database.entity.TargetStatus.COMPLETED -> strings.misc.completed
                                                moe.GetTheNya.AniForge.core.database.entity.TargetStatus.PAUSED -> strings.misc.paused
                                                moe.GetTheNya.AniForge.core.database.entity.TargetStatus.DROPPED -> strings.misc.dropped
                                                else -> ""
                                            }
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                if (item.isFavorite) {
                                                    Icon(
                                                        imageVector = Icons.Default.Star,
                                                        contentDescription = "Favorite",
                                                        tint = Color(0xFFFFD700),
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                }
                                                if (statusLabel.isNotEmpty()) {
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(4.dp))
                                                            .background(Color(0x1Fffffff))
                                                            .border(0.5.dp, Color(0x33ffffff), RoundedCornerShape(4.dp))
                                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                                    ) {
                                                        Text(
                                                            text = "${strings.settingsScreen.targetListPrefix} $statusLabel",
                                                            color = TextSecondary,
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Medium
                                                        )
                                                    }
                                                }
                                            }

                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(32.dp)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .border(1.dp, CardBorder, RoundedCornerShape(8.dp))
                                                        .clickable { importViewModel.deletePendingImport(item.id) },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Delete,
                                                        contentDescription = strings.settingsScreen.removeFromImportTooltip,
                                                        tint = Color(0xFFEF4444),
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }

                                                Button(
                                                    onClick = { importViewModel.toggleResolving(item.id) },
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = if (item.isResolving) Color(0x33FFFFFF) else NeonCoral,
                                                        contentColor = if (item.isResolving) TextPrimary else BackgroundDark
                                                    ),
                                                    shape = RoundedCornerShape(8.dp),
                                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                                    modifier = Modifier.height(32.dp)
                                                ) {
                                                    Text(
                                                        text = if (item.isResolving) strings.libraryScreen.cancel else strings.settingsScreen.anixartImportResolveManually,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }

                                        if (item.isResolving) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            OutlinedTextField(
                                                value = item.searchQuery,
                                                onValueChange = { importViewModel.updateSearchQuery(item.id, it) },
                                                placeholder = { Text(text = strings.settingsScreen.anixartImportSearchAnime, fontSize = 11.sp, color = TextSecondary) },
                                                modifier = Modifier.fillMaxWidth(),
                                                textStyle = LocalTextStyle.current.copy(fontSize = 12.sp, color = TextPrimary),
                                                singleLine = true,
                                                trailingIcon = {
                                                    if (item.isSearching) {
                                                        CircularProgressIndicator(color = NeonCoral, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                                                    } else {
                                                        Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                                                    }
                                                },
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedBorderColor = NeonCoral,
                                                    unfocusedBorderColor = CardBorder,
                                                    focusedContainerColor = Color(0xFF0F0F13),
                                                    unfocusedContainerColor = Color(0xFF0F0F13)
                                                ),
                                                shape = RoundedCornerShape(8.dp)
                                            )

                                            if (item.searchResults.isNotEmpty()) {
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(Color(0xFF0F0F13))
                                                        .padding(4.dp),
                                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    item.searchResults.forEach { anime ->
                                                        Row(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .clickable { importViewModel.resolveManualBind(item.id, anime) }
                                                                .padding(8.dp),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Text(
                                                                text = anime.getDisplayTitle(preferUk = preferUk),
                                                                color = TextPrimary,
                                                                fontSize = 12.sp,
                                                                fontWeight = FontWeight.Medium
                                                            )
                                                        }
                                                    }
                                                }
                                            } else if (item.searchQuery.isNotBlank() && !item.isSearching) {
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text(
                                                    text = strings.settingsScreen.anixartImportNoResults,
                                                    color = TextSecondary,
                                                    fontSize = 11.sp,
                                                    modifier = Modifier.padding(start = 4.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // DONE Action Button at the bottom
                    Button(
                        onClick = { importViewModel.clearSummary() },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCoral, contentColor = BackgroundDark),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text(text = strings.libraryScreen.done, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GestureConfigRow(
    label: String,
    selectedAction: String,
    options: List<String>,
    actionLabels: Map<String, String>,
    onActionSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val displayLabel = actionLabels[selectedAction] ?: selectedAction

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )

        Box(modifier = Modifier.width(180.dp)) {
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it }
            ) {
                OutlinedTextField(
                    value = displayLabel,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = NeonCoral,
                        unfocusedBorderColor = CardBorder,
                        focusedContainerColor = Color(0xFF0F0F13),
                        unfocusedContainerColor = Color(0xFF0F0F13)
                    ),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.background(SurfaceDark)
                ) {
                    options.forEach { option ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = actionLabels[option] ?: option,
                                    color = TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = if (option == selectedAction) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            onClick = {
                                onActionSelected(option)
                                expanded = false
                            },
                            modifier = Modifier.background(SurfaceDark)
                        )
                    }
                }
            }
        }
    }
}
