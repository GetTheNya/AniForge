package moe.GetTheNya.AniForge.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import moe.GetTheNya.AniForge.ui.localization.LocalLocaleStrings
import moe.GetTheNya.AniForge.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataManagementTab(
    viewModel: SettingsViewModel,
    importViewModel: ImportViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val strings = LocalLocaleStrings.current

    val preferUk by viewModel.preferUkTitles.collectAsState()
    val isUpdating by viewModel.isUpdating.collectAsState()
    val metadata by viewModel.catalogMetadata.collectAsState()

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

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Database Management Card
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

        Spacer(modifier = Modifier.height(16.dp))
    }

    // Confirmation dialog for import parameters
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

    // Importing processing dialog
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

    // Summary and Resolution fullscreen dialog
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
