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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import moe.GetTheNya.AniForge.BuildConfig
import moe.GetTheNya.AniForge.R
import moe.GetTheNya.AniForge.ui.localization.LocalLocaleStrings
import moe.GetTheNya.AniForge.ui.navigation.NavController
import moe.GetTheNya.AniForge.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    navController: NavController,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = { navController.popBackStack() }
) {
    val strings = LocalLocaleStrings.current
    val preferUk by viewModel.preferUkTitles.collectAsState()
    val currentLangCode by viewModel.currentLanguage.collectAsState()
    val availableLangs by viewModel.availableLanguages.collectAsState()
    val isUpdating by viewModel.isUpdating.collectAsState()
    val metadata by viewModel.catalogMetadata.collectAsState()

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
                val context = LocalContext.current
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

            // Dynamic escape-parsed multi-paragraph app description with Credits
            Text(
                text = strings.settingsScreen.appSynopsis,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = TextPrimary,
                    lineHeight = 20.sp
                ),
                modifier = Modifier.fillMaxWidth()
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
                val context = LocalContext.current
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

            // Dynamic Update Banner / Button
            val isCheckingForUpdates by viewModel.isCheckingForUpdates.collectAsState()
            val updateCheckStatus by viewModel.updateCheckStatus.collectAsState()
            val updateHtmlUrl by viewModel.updateHtmlUrl.collectAsState()

            val context = LocalContext.current

            // State A (Up to Date): Trigger a low-emphasis Toast when active check determines we are up to date
            LaunchedEffect(updateCheckStatus) {
                if (updateCheckStatus == SettingsViewModel.UpdateStatus.UP_TO_DATE) {
                    android.widget.Toast.makeText(
                        context,
                        strings.settingsScreen.updateUpToDate,
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            }

            if (updateCheckStatus == SettingsViewModel.UpdateStatus.UPDATE_AVAILABLE && updateHtmlUrl != null) {
                // State B: Update Found Banner
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = strings.settingsScreen.updateAvailable,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Button(
                        onClick = {
                            val intent = android.content.Intent(
                                android.content.Intent.ACTION_VIEW,
                                android.net.Uri.parse(updateHtmlUrl)
                            )
                            context.startActivity(intent)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            contentColor = MaterialTheme.colorScheme.primaryContainer
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
            } else {
                if (updateCheckStatus == SettingsViewModel.UpdateStatus.UP_TO_DATE) {
                    Text(
                        text = strings.settingsScreen.updateUpToDate,
                        color = TextSecondary,
                        fontSize = 12.sp,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }

                Button(
                    onClick = { viewModel.checkForUpdates() },
                    enabled = !isCheckingForUpdates,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonCoral,
                        contentColor = BackgroundDark,
                        disabledContainerColor = Color(0x33FFFFFF),
                        disabledContentColor = TextSecondary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isCheckingForUpdates) {
                        CircularProgressIndicator(
                            color = TextSecondary,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(16.dp)
                        )
                    } else {
                        Text(
                            text = strings.settingsScreen.checkForUpdates,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
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
