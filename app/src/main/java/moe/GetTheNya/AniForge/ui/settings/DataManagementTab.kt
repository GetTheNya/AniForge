package moe.GetTheNya.AniForge.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import moe.GetTheNya.AniForge.ui.localization.LocalLocaleStrings
import moe.GetTheNya.AniForge.ui.theme.*
import coil.compose.AsyncImage
import moe.GetTheNya.AniForge.core.database.entity.ImportStatus
import moe.GetTheNya.AniForge.core.database.entity.TargetStatus
import moe.GetTheNya.AniForge.ui.settings.PendingImportItem
import moe.GetTheNya.AniForge.ui.utils.AnimeStatusColors
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import moe.GetTheNya.AniForge.core.model.SearchFilterQuery
import moe.GetTheNya.AniForge.core.model.SortOption
import moe.GetTheNya.AniForge.core.model.Anime
import kotlinx.coroutines.launch

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
    val preFlightCollisions by importViewModel.preFlightCollisions.collectAsState()


    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            selectedFileUri = uri
        }
    }

    val isExporting by viewModel.isExporting.collectAsState()
    val isRestoring by viewModel.isRestoring.collectAsState()
    val isPreparingImport by viewModel.isPreparingImport.collectAsState()
    val backupMetadata by viewModel.backupMetadata.collectAsState()
    val backupError by viewModel.backupError.collectAsState()

    var selectedRestoreUri by remember { mutableStateOf<Uri?>(null) }

    var showExportDialog by remember { mutableStateOf(false) }

    // Module checkboxes for export
    var includeSettings by remember { mutableStateOf(true) }
    var includeTracking by remember { mutableStateOf(true) }
    var includeCollections by remember { mutableStateOf(true) }

    // Module checkboxes for restore
    var restoreSettings by remember { mutableStateOf(true) }
    var restoreTracking by remember { mutableStateOf(true) }
    var restoreCollections by remember { mutableStateOf(true) }

    val dateFormat = remember { java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US) }

    val backupCreateLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        if (uri != null) {
            viewModel.performExport(
                uri = uri,
                includeSettings = includeSettings,
                includeTracking = includeTracking,
                includeCollections = includeCollections,
                onSuccess = {
                    android.widget.Toast.makeText(context, strings.settingsScreen.backupExportSuccess, android.widget.Toast.LENGTH_LONG).show()
                }
            )
        }
    }

    val backupOpenLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            selectedRestoreUri = uri
            viewModel.parseBackupFile(uri)
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

        // Group 1: Create Backup (Export)
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
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Storage,
                    contentDescription = null,
                    tint = NeonCoral
                )
                Text(
                    text = strings.settingsScreen.exportBackupTitle,
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = strings.settingsScreen.exportBackupDesc,
                color = TextSecondary,
                fontSize = 12.sp
            )

            Button(
                onClick = { showExportDialog = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonCoral,
                    contentColor = BackgroundDark
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text(
                    text = strings.settingsScreen.exportBackupTitle,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }

        // Group 2: Restore Data (Import)
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
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CloudUpload,
                    contentDescription = null,
                    tint = CyberTeal
                )
                Text(
                    text = strings.settingsScreen.importBackupTitle,
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = strings.settingsScreen.importBackupDesc,
                color = TextSecondary,
                fontSize = 12.sp
            )

            Button(
                onClick = { backupOpenLauncher.launch(arrayOf("*/*")) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyberTeal,
                    contentColor = BackgroundDark
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text(
                    text = strings.settingsScreen.importBackupTitle,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            HorizontalDivider(color = CardBorder, thickness = 1.dp)

            // Import from Anixart Row
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

            if (pendingCount > 0) {
                HorizontalDivider(color = CardBorder, thickness = 1.dp)

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
        val pendingItems by importViewModel.pendingImportItems.collectAsState()
        
        val readyItems = remember(pendingItems) { pendingItems.filter { it.entity.importStatus == ImportStatus.SUCCESS || it.entity.importStatus == ImportStatus.RESOLVED } }
        val ambiguousItems = remember(pendingItems) { pendingItems.filter { it.entity.importStatus == ImportStatus.AMBIGUOUS } }
        val notFoundItems = remember(pendingItems) { pendingItems.filter { it.entity.importStatus == ImportStatus.NOT_FOUND } }

        var selectedTabIndex by remember { mutableStateOf(0) }
        var skipUnresolved by remember { mutableStateOf(false) }

        var selectedAmbiguousItem by remember { mutableStateOf<PendingImportItem?>(null) }
        var showAmbiguousDialog by remember { mutableStateOf(false) }

        var showManualSearchByRemember by remember(showAmbiguousDialog) {
            mutableStateOf(selectedAmbiguousItem?.entity?.importStatus == ImportStatus.NOT_FOUND)
        }
        var manualSearchQuery by remember(showAmbiguousDialog) { mutableStateOf("") }
        var manualSearchResults by remember(showAmbiguousDialog) { mutableStateOf<List<Anime>>(emptyList()) }
        var isManualSearching by remember(showAmbiguousDialog) { mutableStateOf(false) }
        val dialogCoroutineScope = rememberCoroutineScope()

        val candidates by importViewModel.ambiguousCandidates.collectAsState()
        val isFetchingCandidates by importViewModel.isFetchingCandidates.collectAsState()
        val isCommitting by importViewModel.isCommitting.collectAsState()

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



                    val tabs = listOf(
                        strings.settingsScreen.importTabAmbiguous to ambiguousItems.size,
                        strings.settingsScreen.importTabNotFound to notFoundItems.size,
                        strings.settingsScreen.importTabReady to readyItems.size
                    )

                    TabRow(
                        selectedTabIndex = selectedTabIndex,
                        containerColor = BackgroundDark,
                        contentColor = CyberTeal,
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                                color = CyberTeal
                            )
                        }
                    ) {
                        tabs.forEachIndexed { index, (title, count) ->
                            Tab(
                                selected = selectedTabIndex == index,
                                onClick = { selectedTabIndex = index },
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(text = title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Badge(
                                            containerColor = if (selectedTabIndex == index) CyberTeal else CardBorder,
                                            contentColor = if (selectedTabIndex == index) BackgroundDark else TextSecondary
                                        ) {
                                            Text(text = count.toString(), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                },
                                selectedContentColor = CyberTeal,
                                unselectedContentColor = TextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        when (selectedTabIndex) {
                            0 -> { // Ambiguous list
                                if (ambiguousItems.isEmpty()) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text(text = strings.settingsScreen.anixartImportAllMapped, color = TextSecondary, fontSize = 14.sp)
                                    }
                                } else {
                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        items(items = ambiguousItems, key = { it.entity.id }) { item ->
                                            Column(
                                                modifier = Modifier
                                                    .animateItem()
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(SurfaceCardDark)
                                                    .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                                                    .padding(12.dp)
                                            ) {
                                                // Top row: full width title
                                                Text(
                                                    text = item.entity.russianTitle.ifEmpty { item.entity.originalTitle },
                                                    color = TextPrimary,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                                if (item.entity.russianTitle.isNotEmpty() && item.entity.originalTitle.isNotEmpty()) {
                                                    Text(
                                                        text = item.entity.originalTitle,
                                                        color = TextSecondary,
                                                        fontSize = 11.sp,
                                                        modifier = Modifier.fillMaxWidth()
                                                    )
                                                }

                                                Spacer(modifier = Modifier.height(8.dp))

                                                // Bottom row: target status list badge + action buttons
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                        modifier = Modifier.weight(1f)
                                                    ) {
                                                        val statusLabel = when (item.entity.targetStatus) {
                                                            TargetStatus.CURRENT -> strings.misc.watching
                                                            TargetStatus.PLANNING -> strings.misc.planning
                                                            TargetStatus.COMPLETED -> strings.misc.completed
                                                            TargetStatus.PAUSED -> strings.misc.paused
                                                            TargetStatus.DROPPED -> strings.misc.dropped
                                                            else -> ""
                                                        }
                                                        if (statusLabel.isNotEmpty()) {
                                                            val statusColor = AnimeStatusColors[item.entity.targetStatus.name] ?: Color.Gray
                                                            Box(
                                                                modifier = Modifier
                                                                    .clip(RoundedCornerShape(4.dp))
                                                                    .background(statusColor.copy(alpha = 0.15f))
                                                                    .border(0.5.dp, statusColor.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                                            ) {
                                                                Text(
                                                                    text = "${strings.settingsScreen.targetListPrefix} $statusLabel",
                                                                    color = statusColor,
                                                                    fontSize = 10.sp,
                                                                    fontWeight = FontWeight.Bold
                                                                )
                                                            }
                                                        }

                                                        if (item.entity.isFavorite) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .clip(RoundedCornerShape(4.dp))
                                                                    .background(Color(0xFFFFD700).copy(alpha = 0.15f))
                                                                    .border(0.5.dp, Color(0xFFFFD700).copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                                            ) {
                                                                Row(
                                                                    verticalAlignment = Alignment.CenterVertically,
                                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                                ) {
                                                                    Icon(
                                                                        imageVector = Icons.Default.Star,
                                                                        contentDescription = null,
                                                                        tint = Color(0xFFFFD700),
                                                                        modifier = Modifier.size(10.dp)
                                                                    )
                                                                    Text(
                                                                        text = strings.settingsScreen.anixartImportCollectionTitle,
                                                                        color = Color(0xFFFFD700),
                                                                        fontSize = 10.sp,
                                                                        fontWeight = FontWeight.Bold
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }

                                                    Row(
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(32.dp)
                                                                .clip(RoundedCornerShape(8.dp))
                                                                .border(1.dp, CardBorder, RoundedCornerShape(8.dp))
                                                                .clickable { importViewModel.deletePendingImport(item.entity.id) },
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                                                        }
                                                        Button(
                                                            onClick = {
                                                                selectedAmbiguousItem = item
                                                                importViewModel.fetchCandidatesForAmbiguous(item.entity)
                                                                showAmbiguousDialog = true
                                                            },
                                                            colors = ButtonDefaults.buttonColors(containerColor = NeonCoral, contentColor = BackgroundDark),
                                                            shape = RoundedCornerShape(8.dp),
                                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                                            modifier = Modifier.height(32.dp)
                                                        ) {
                                                            Text(text = strings.settingsScreen.anixartImportResolveManually, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            1 -> { // Not Found list
                                if (notFoundItems.isEmpty()) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text(text = strings.settingsScreen.anixartImportAllMapped, color = TextSecondary, fontSize = 14.sp)
                                    }
                                } else {
                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        items(items = notFoundItems, key = { it.entity.id }) { item ->
                                            Column(
                                                modifier = Modifier
                                                    .animateItem()
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(SurfaceCardDark)
                                                    .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                                                    .padding(12.dp)
                                            ) {
                                                // Top row: full width title
                                                Text(
                                                    text = item.entity.russianTitle.ifEmpty { item.entity.originalTitle },
                                                    color = TextPrimary,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                                if (item.entity.russianTitle.isNotEmpty() && item.entity.originalTitle.isNotEmpty()) {
                                                    Text(
                                                        text = item.entity.originalTitle,
                                                        color = TextSecondary,
                                                        fontSize = 11.sp,
                                                        modifier = Modifier.fillMaxWidth()
                                                    )
                                                }

                                                Spacer(modifier = Modifier.height(8.dp))

                                                // Bottom row: target status list badge + action buttons
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                        modifier = Modifier.weight(1f)
                                                    ) {
                                                        val statusLabel = when (item.entity.targetStatus) {
                                                            TargetStatus.CURRENT -> strings.misc.watching
                                                            TargetStatus.PLANNING -> strings.misc.planning
                                                            TargetStatus.COMPLETED -> strings.misc.completed
                                                            TargetStatus.PAUSED -> strings.misc.paused
                                                            TargetStatus.DROPPED -> strings.misc.dropped
                                                            else -> ""
                                                        }
                                                        if (statusLabel.isNotEmpty()) {
                                                            val statusColor = AnimeStatusColors[item.entity.targetStatus.name] ?: Color.Gray
                                                            Box(
                                                                modifier = Modifier
                                                                    .clip(RoundedCornerShape(4.dp))
                                                                    .background(statusColor.copy(alpha = 0.15f))
                                                                    .border(0.5.dp, statusColor.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                                            ) {
                                                                Text(
                                                                    text = "${strings.settingsScreen.targetListPrefix} $statusLabel",
                                                                    color = statusColor,
                                                                    fontSize = 10.sp,
                                                                    fontWeight = FontWeight.Bold
                                                                )
                                                            }
                                                        }

                                                        if (item.entity.isFavorite) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .clip(RoundedCornerShape(4.dp))
                                                                    .background(Color(0xFFFFD700).copy(alpha = 0.15f))
                                                                    .border(0.5.dp, Color(0xFFFFD700).copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                                            ) {
                                                                Row(
                                                                    verticalAlignment = Alignment.CenterVertically,
                                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                                ) {
                                                                    Icon(
                                                                        imageVector = Icons.Default.Star,
                                                                        contentDescription = null,
                                                                        tint = Color(0xFFFFD700),
                                                                        modifier = Modifier.size(10.dp)
                                                                    )
                                                                    Text(
                                                                        text = strings.settingsScreen.anixartImportCollectionTitle,
                                                                        color = Color(0xFFFFD700),
                                                                        fontSize = 10.sp,
                                                                        fontWeight = FontWeight.Bold
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }

                                                    Row(
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(32.dp)
                                                                .clip(RoundedCornerShape(8.dp))
                                                                .border(1.dp, CardBorder, RoundedCornerShape(8.dp))
                                                                .clickable { importViewModel.deletePendingImport(item.entity.id) },
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                                                        }
                                                        Button(
                                                            onClick = {
                                                                selectedAmbiguousItem = item
                                                                showAmbiguousDialog = true
                                                            },
                                                            colors = ButtonDefaults.buttonColors(containerColor = NeonCoral, contentColor = BackgroundDark),
                                                            shape = RoundedCornerShape(8.dp),
                                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                                            modifier = Modifier.height(32.dp)
                                                        ) {
                                                            Text(
                                                                text = strings.settingsScreen.anixartImportResolveManually,
                                                                fontSize = 10.sp,
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
                            2 -> { // Ready list
                                if (readyItems.isEmpty()) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text(text = "No records ready to import", color = TextSecondary, fontSize = 14.sp)
                                    }
                                } else {
                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        items(items = readyItems, key = { it.entity.id }) { item ->
                                            Column(
                                                modifier = Modifier
                                                    .animateItem()
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(SurfaceCardDark)
                                                    .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                                                    .padding(12.dp)
                                            ) {
                                                // Top row: full width title
                                                Text(
                                                    text = item.matchedAnime?.getDisplayTitle(preferUk = preferUk)
                                                        ?: (item.entity.russianTitle.ifEmpty { item.entity.originalTitle }),
                                                    color = TextPrimary,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.fillMaxWidth()
                                                )

                                                Spacer(modifier = Modifier.height(8.dp))

                                                // Bottom row: status badges on the left, delete button on the right
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                        modifier = Modifier.weight(1f)
                                                    ) {
                                                        val statusLabel = when (item.entity.targetStatus) {
                                                            TargetStatus.CURRENT -> strings.misc.watching
                                                            TargetStatus.PLANNING -> strings.misc.planning
                                                            TargetStatus.COMPLETED -> strings.misc.completed
                                                            TargetStatus.PAUSED -> strings.misc.paused
                                                            TargetStatus.DROPPED -> strings.misc.dropped
                                                            else -> ""
                                                        }
                                                        if (statusLabel.isNotEmpty()) {
                                                            val statusColor = AnimeStatusColors[item.entity.targetStatus.name] ?: Color.Gray
                                                            Box(
                                                                modifier = Modifier
                                                                    .clip(RoundedCornerShape(4.dp))
                                                                    .background(statusColor.copy(alpha = 0.15f))
                                                                    .border(0.5.dp, statusColor.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                                            ) {
                                                                Text(
                                                                    text = "${strings.settingsScreen.targetListPrefix} $statusLabel",
                                                                    color = statusColor,
                                                                    fontSize = 10.sp,
                                                                    fontWeight = FontWeight.Bold
                                                                )
                                                            }
                                                        }

                                                        val importLabel = if (item.entity.importStatus == ImportStatus.RESOLVED) {
                                                            strings.settingsScreen.importResolvedLabel
                                                        } else {
                                                            strings.settingsScreen.importSuccessLabel
                                                        }
                                                        val badgeColor = if (item.entity.importStatus == ImportStatus.RESOLVED) NeonCoral else CyberTeal
                                                        Box(
                                                            modifier = Modifier
                                                                .clip(RoundedCornerShape(4.dp))
                                                                .background(badgeColor.copy(alpha = 0.15f))
                                                                .border(0.5.dp, badgeColor.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                                        ) {
                                                            Text(
                                                                text = importLabel,
                                                                color = badgeColor,
                                                                fontSize = 10.sp,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        }

                                                        if (item.entity.isFavorite) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .clip(RoundedCornerShape(4.dp))
                                                                    .background(Color(0xFFFFD700).copy(alpha = 0.15f))
                                                                    .border(0.5.dp, Color(0xFFFFD700).copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                                            ) {
                                                                Row(
                                                                    verticalAlignment = Alignment.CenterVertically,
                                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                                ) {
                                                                    Icon(
                                                                        imageVector = Icons.Default.Star,
                                                                        contentDescription = null,
                                                                        tint = Color(0xFFFFD700),
                                                                        modifier = Modifier.size(10.dp)
                                                                    )
                                                                    Text(
                                                                        text = strings.settingsScreen.anixartImportCollectionTitle,
                                                                        color = Color(0xFFFFD700),
                                                                        fontSize = 10.sp,
                                                                        fontWeight = FontWeight.Bold
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }

                                                    Box(
                                                        modifier = Modifier
                                                            .size(32.dp)
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .border(1.dp, CardBorder, RoundedCornerShape(8.dp))
                                                            .clickable { importViewModel.deletePendingImport(item.entity.id) },
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { skipUnresolved = !skipUnresolved }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = skipUnresolved,
                            onCheckedChange = { skipUnresolved = it },
                            colors = CheckboxDefaults.colors(checkedColor = NeonCoral, uncheckedColor = TextSecondary)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = strings.settingsScreen.importSkipUnresolved, color = TextPrimary, fontSize = 13.sp)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { importViewModel.clearSummary() },
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, CardBorder)
                        ) {
                            Text(text = strings.libraryScreen.cancel, color = TextPrimary, fontWeight = FontWeight.Bold)
                        }

                        val isImportEnabled = readyItems.isNotEmpty() && ((ambiguousItems.isEmpty() && notFoundItems.isEmpty()) || skipUnresolved)

                        Button(
                            onClick = {
                                importViewModel.runPreFlightCheck {
                                    importViewModel.commitImport(skipUnresolved = skipUnresolved) {
                                        android.widget.Toast.makeText(context, strings.settingsScreen.anixartImportAllMapped, android.widget.Toast.LENGTH_LONG).show()
                                    }
                                }
                            },
                            enabled = isImportEnabled,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyberTeal,
                                contentColor = BackgroundDark,
                                disabledContainerColor = Color(0x33FFFFFF),
                                disabledContentColor = TextSecondary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Text(text = strings.settingsScreen.importAllAction, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }
        }

        // Resolution Dialog for Ambiguous items
        if (showAmbiguousDialog && selectedAmbiguousItem != null) {
            val item = selectedAmbiguousItem!!
            AlertDialog(
                modifier = Modifier.border(1.dp, CardBorder, RoundedCornerShape(24.dp)),
                onDismissRequest = {
                    showAmbiguousDialog = false
                    selectedAmbiguousItem = null
                    importViewModel.clearCandidates()
                },
                title = {
                    Text(
                        text = strings.settingsScreen.importAmbiguousSelectionHeader,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (!showManualSearchByRemember) {
                            Text(
                                text = strings.settingsScreen.importAmbiguousSelectionDesc,
                                color = TextSecondary,
                                fontSize = 13.sp
                            )

                            Text(
                                text = item.entity.russianTitle.ifEmpty { item.entity.originalTitle },
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )

                            HorizontalDivider(color = CardBorder, thickness = 1.dp)

                            if (isFetchingCandidates) {
                                Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(color = CyberTeal)
                                }
                            } else if (candidates.isEmpty()) {
                                Text(
                                    text = strings.settingsScreen.anixartImportNoResults,
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                            } else {
                                LazyColumn(
                                    modifier = Modifier.heightIn(max = 240.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(candidates) { candidate ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(SurfaceCardDark)
                                                .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                                                .clickable {
                                                    importViewModel.resolveAmbiguous(item.entity.id, candidate.anilistId)
                                                    showAmbiguousDialog = false
                                                    selectedAmbiguousItem = null
                                                    importViewModel.clearCandidates()
                                                }
                                                .padding(8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            AsyncImage(
                                                model = candidate.coverLarge,
                                                contentDescription = candidate.getDisplayTitle(preferUk),
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .size(50.dp, 70.dp)
                                                    .clip(RoundedCornerShape(6.dp))
                                            )
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = candidate.getDisplayTitle(preferUk),
                                                    color = TextPrimary,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                val metaStr = listOfNotNull(
                                                    candidate.format,
                                                    candidate.seasonYear?.toString()
                                                ).joinToString(" • ")
                                                Text(
                                                    text = metaStr,
                                                    color = TextSecondary,
                                                    fontSize = 11.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            Text(
                                text = strings.settingsScreen.importSearchManually,
                                color = TextSecondary,
                                fontSize = 13.sp
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = item.entity.originalTitle.ifEmpty { item.entity.russianTitle },
                                    color = TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )

                                var isNetworkConnected by remember { mutableStateOf(true) }
                                DisposableEffect(context) {
                                    val connectivityManager = context.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
                                    val callback = object : android.net.ConnectivityManager.NetworkCallback() {
                                        override fun onAvailable(network: android.net.Network) {
                                            isNetworkConnected = true
                                        }
                                        override fun onLost(network: android.net.Network) {
                                            isNetworkConnected = isNetworkAvailable(context)
                                        }
                                    }
                                    connectivityManager?.registerDefaultNetworkCallback(callback)
                                    isNetworkConnected = isNetworkAvailable(context)
                                    onDispose {
                                        connectivityManager?.unregisterNetworkCallback(callback)
                                    }
                                }

                                var isAniListSearching by remember { mutableStateOf(false) }

                                Button(
                                    onClick = {
                                        isAniListSearching = true
                                        dialogCoroutineScope.launch {
                                            val queryTitle = item.entity.originalTitle.ifEmpty { item.entity.russianTitle }
                                            val resolvedId = importViewModel.resolveWithAniList(queryTitle)
                                            if (resolvedId != null) {
                                                val idStr = resolvedId.toString()
                                                manualSearchQuery = idStr
                                                isManualSearching = true
                                                val results = importViewModel.searchAnimeCatalog(idStr)
                                                manualSearchResults = results
                                                isManualSearching = false
                                            } else {
                                                android.widget.Toast.makeText(context, strings.settingsScreen.anixartImportNotFoundOnAniList, android.widget.Toast.LENGTH_SHORT).show()
                                            }
                                            isAniListSearching = false
                                        }
                                    },
                                    enabled = !isAniListSearching && isNetworkConnected,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = CyberTeal,
                                        contentColor = BackgroundDark,
                                        disabledContainerColor = Color(0x33FFFFFF),
                                        disabledContentColor = TextSecondary
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        if (isAniListSearching) {
                                            CircularProgressIndicator(
                                                color = BackgroundDark,
                                                strokeWidth = 2.dp,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.Search,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                        Text(
                                            text = strings.settingsScreen.anixartImportSearchAniList,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            HorizontalDivider(color = CardBorder, thickness = 1.dp)

                            OutlinedTextField(
                                value = manualSearchQuery,
                                onValueChange = { query ->
                                    manualSearchQuery = query
                                    if (query.isNotBlank()) {
                                        isManualSearching = true
                                        dialogCoroutineScope.launch {
                                            val results = importViewModel.searchAnimeCatalog(query)
                                            manualSearchResults = results
                                            isManualSearching = false
                                        }
                                    } else {
                                        manualSearchResults = emptyList()
                                    }
                                },
                                placeholder = { Text(text = strings.settingsScreen.anixartImportSearchAnime, fontSize = 11.sp, color = TextSecondary) },
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = LocalTextStyle.current.copy(fontSize = 12.sp, color = TextPrimary),
                                singleLine = true,
                                trailingIcon = {
                                    if (isManualSearching) {
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

                            if (manualSearchResults.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                LazyColumn(
                                    modifier = Modifier.heightIn(max = 200.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(manualSearchResults) { candidate ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(SurfaceCardDark)
                                                .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                                                .clickable {
                                                    importViewModel.resolveAmbiguous(item.entity.id, candidate.anilistId)
                                                    showAmbiguousDialog = false
                                                    selectedAmbiguousItem = null
                                                    importViewModel.clearCandidates()
                                                }
                                                .padding(8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            AsyncImage(
                                                model = candidate.coverLarge,
                                                contentDescription = candidate.getDisplayTitle(preferUk),
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .size(40.dp, 56.dp)
                                                    .clip(RoundedCornerShape(6.dp))
                                            )
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = candidate.getDisplayTitle(preferUk),
                                                    color = TextPrimary,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                val metaStr = listOfNotNull(
                                                    candidate.format,
                                                    candidate.seasonYear?.toString()
                                                ).joinToString(" • ")
                                                if (metaStr.isNotEmpty()) {
                                                    Text(
                                                        text = metaStr,
                                                        color = TextSecondary,
                                                        fontSize = 10.sp
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            } else if (manualSearchQuery.isNotBlank() && !isManualSearching) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = strings.settingsScreen.anixartImportNoResults,
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    if (!showManualSearchByRemember) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = {
                                showManualSearchByRemember = true
                            }) {
                                Text(
                                    text = strings.settingsScreen.importNotTheseAnime,
                                    color = NeonCoral,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            TextButton(onClick = {
                                showAmbiguousDialog = false
                                selectedAmbiguousItem = null
                                importViewModel.clearCandidates()
                            }) {
                                Text(text = strings.libraryScreen.cancel, color = TextSecondary)
                            }
                        }
                    } else {
                        if (selectedAmbiguousItem?.entity?.importStatus == ImportStatus.NOT_FOUND) {
                            TextButton(onClick = {
                                showAmbiguousDialog = false
                                selectedAmbiguousItem = null
                                importViewModel.clearCandidates()
                            }) {
                                Text(text = strings.libraryScreen.cancel, color = TextSecondary)
                            }
                        } else {
                            TextButton(onClick = {
                                showManualSearchByRemember = false
                            }) {
                                Text(text = strings.misc.back, color = TextPrimary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                },
                containerColor = AlertBackground,
                shape = RoundedCornerShape(24.dp)
            )
        }

        // Pre-Flight Collision Resolution Dialog
        if (preFlightCollisions.isNotEmpty()) {
            AlertDialog(
                modifier = Modifier.border(1.dp, CardBorder, RoundedCornerShape(24.dp)),
                onDismissRequest = {
                    importViewModel.clearPreFlightCollisions()
                },
                title = {
                    Text(
                        text = strings.settingsScreen.preFlightCollisionTitle,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                },
                text = {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 350.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(preFlightCollisions) { group ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(SurfaceCardDark)
                                    .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                                    .padding(12.dp)
                            ) {
                                val desc = String.format(strings.settingsScreen.preFlightCollisionDesc, group.targetAnimeTitle)
                                Text(
                                    text = desc,
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                group.conflictingRows.forEachIndexed { index, row ->
                                    val statusLabel = when (row.targetStatus) {
                                        TargetStatus.CURRENT -> strings.misc.watching
                                        TargetStatus.PLANNING -> strings.misc.planning
                                        TargetStatus.COMPLETED -> strings.misc.completed
                                        TargetStatus.PAUSED -> strings.misc.paused
                                        TargetStatus.DROPPED -> strings.misc.dropped
                                        else -> "Unknown"
                                    }
                                    val statusColor = AnimeStatusColors[row.targetStatus.name] ?: Color.Gray
                                    
                                    val formattedRow = String.format(strings.settingsScreen.preFlightCollisionRowLabel, index + 1, row.russianTitle.ifEmpty { row.originalTitle })

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = formattedRow,
                                            color = TextSecondary,
                                            fontSize = 12.sp,
                                            modifier = Modifier.weight(1f).padding(end = 8.dp)
                                        )
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(statusColor.copy(alpha = 0.15f))
                                                .border(0.5.dp, statusColor.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = statusLabel,
                                                color = statusColor,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            dialogCoroutineScope.launch {
                                importViewModel.demoteCollisionsToConflicts()
                                selectedTabIndex = 0
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCoral, contentColor = BackgroundDark),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(text = strings.settingsScreen.preFlightCollisionCancel, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = {
                            importViewModel.clearPreFlightCollisions()
                            importViewModel.commitImport(skipUnresolved = skipUnresolved) {
                                android.widget.Toast.makeText(context, strings.settingsScreen.anixartImportAllMapped, android.widget.Toast.LENGTH_LONG).show()
                            }
                        },
                        border = BorderStroke(1.dp, CardBorder),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(text = strings.settingsScreen.preFlightCollisionMerge, color = TextPrimary, fontWeight = FontWeight.Bold)
                    }
                },
                containerColor = AlertBackground,
                shape = RoundedCornerShape(24.dp)
            )
        }

        // Committing Overlay
        if (isCommitting) {
            Dialog(
                onDismissRequest = {},
                properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
            ) {
                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .background(SurfaceDark, shape = RoundedCornerShape(16.dp))
                        .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(color = CyberTeal)
                        Text(
                            text = strings.settingsScreen.preparingImport,
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }

    // Export Choice Dialog
    if (showExportDialog) {
        AlertDialog(
            modifier = Modifier.border(1.dp, CardBorder, RoundedCornerShape(24.dp)),
            onDismissRequest = { showExportDialog = false },
            title = {
                Text(
                    text = strings.settingsScreen.exportBackupTitle,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = strings.settingsScreen.exportBackupDesc,
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clickable { includeSettings = !includeSettings }.padding(vertical = 4.dp)
                    ) {
                        Checkbox(
                            checked = includeSettings,
                            onCheckedChange = { includeSettings = it },
                            colors = CheckboxDefaults.colors(checkedColor = NeonCoral, uncheckedColor = TextSecondary)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = strings.settingsScreen.moduleSettings, color = TextPrimary, fontSize = 14.sp)
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clickable { includeTracking = !includeTracking }.padding(vertical = 4.dp)
                    ) {
                        Checkbox(
                            checked = includeTracking,
                            onCheckedChange = { includeTracking = it },
                            colors = CheckboxDefaults.colors(checkedColor = NeonCoral, uncheckedColor = TextSecondary)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = strings.settingsScreen.moduleTracking, color = TextPrimary, fontSize = 14.sp)
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clickable { includeCollections = !includeCollections }.padding(vertical = 4.dp)
                    ) {
                        Checkbox(
                            checked = includeCollections,
                            onCheckedChange = { includeCollections = it },
                            colors = CheckboxDefaults.colors(checkedColor = NeonCoral, uncheckedColor = TextSecondary)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = strings.settingsScreen.moduleCollections, color = TextPrimary, fontSize = 14.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showExportDialog = false
                        val dateStr = dateFormat.format(java.util.Date())
                        backupCreateLauncher.launch("aniforge_backup_$dateStr.forge")
                    },
                    enabled = includeSettings || includeTracking || includeCollections,
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCoral, contentColor = BackgroundDark),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(text = strings.misc.save, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text(text = strings.misc.back, color = TextSecondary)
                }
            },
            containerColor = AlertBackground,
            shape = RoundedCornerShape(24.dp)
        )
    }

    // Restore Choice Dialog
    val parsedBackupMetadata = backupMetadata
    if (parsedBackupMetadata != null && selectedRestoreUri != null) {
        LaunchedEffect(parsedBackupMetadata) {
            restoreSettings = parsedBackupMetadata.includeSettings
            restoreTracking = parsedBackupMetadata.includeTracking
            restoreCollections = parsedBackupMetadata.includeCollections
        }

        AlertDialog(
            modifier = Modifier.border(1.dp, CardBorder, RoundedCornerShape(24.dp)),
            onDismissRequest = {
                viewModel.clearParsedMetadata()
                selectedRestoreUri = null
            },
            title = {
                Text(
                    text = strings.settingsScreen.importBackupTitle,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = strings.settingsScreen.importBackupDesc,
                        color = TextSecondary,
                        fontSize = 13.sp
                    )

                    if (parsedBackupMetadata.includeSettings) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().clickable { restoreSettings = !restoreSettings }.padding(vertical = 4.dp)
                        ) {
                            Checkbox(
                                checked = restoreSettings,
                                onCheckedChange = { restoreSettings = it },
                                colors = CheckboxDefaults.colors(checkedColor = CyberTeal, uncheckedColor = TextSecondary)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = strings.settingsScreen.moduleSettings, color = TextPrimary, fontSize = 14.sp)
                        }
                    }

                    if (parsedBackupMetadata.includeTracking) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().clickable { restoreTracking = !restoreTracking }.padding(vertical = 4.dp)
                        ) {
                            Checkbox(
                                checked = restoreTracking,
                                onCheckedChange = { restoreTracking = it },
                                colors = CheckboxDefaults.colors(checkedColor = CyberTeal, uncheckedColor = TextSecondary)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = strings.settingsScreen.moduleTracking, color = TextPrimary, fontSize = 14.sp)
                        }
                    }

                    if (parsedBackupMetadata.includeCollections) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().clickable { restoreCollections = !restoreCollections }.padding(vertical = 4.dp)
                        ) {
                            Checkbox(
                                checked = restoreCollections,
                                onCheckedChange = { restoreCollections = it },
                                colors = CheckboxDefaults.colors(checkedColor = CyberTeal, uncheckedColor = TextSecondary)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = strings.settingsScreen.moduleCollections, color = TextPrimary, fontSize = 14.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val uri = selectedRestoreUri!!
                        viewModel.performRestore(
                            uri = uri,
                            restoreSettings = restoreSettings && parsedBackupMetadata.includeSettings,
                            restoreTracking = restoreTracking && parsedBackupMetadata.includeTracking,
                            restoreCollections = restoreCollections && parsedBackupMetadata.includeCollections,
                            onSuccess = {
                                viewModel.clearParsedMetadata()
                                selectedRestoreUri = null
                            }
                        )
                    },
                    enabled = (restoreSettings && parsedBackupMetadata.includeSettings) || 
                              (restoreTracking && parsedBackupMetadata.includeTracking) || 
                              (restoreCollections && parsedBackupMetadata.includeCollections),
                    colors = ButtonDefaults.buttonColors(containerColor = CyberTeal, contentColor = BackgroundDark),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(text = strings.misc.save, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.clearParsedMetadata()
                    selectedRestoreUri = null
                }) {
                    Text(text = strings.misc.back, color = TextSecondary)
                }
            },
            containerColor = AlertBackground,
            shape = RoundedCornerShape(24.dp)
        )
    }

    // Incompatible Backup or Error Dialog
    val errorMsg = backupError
    if (errorMsg != null) {
        val isIncompatible = errorMsg.contains("Incompatible Backup", ignoreCase = true)
        AlertDialog(
            modifier = Modifier.border(1.dp, CardBorder, RoundedCornerShape(24.dp)),
            onDismissRequest = {
                viewModel.clearBackupError()
                selectedRestoreUri = null
            },
            title = {
                Text(
                    text = if (isIncompatible) strings.settingsScreen.incompatibleBackupHeader else strings.misc.error,
                    color = NeonCoral,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = errorMsg,
                    color = TextPrimary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearBackupError()
                        selectedRestoreUri = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCoral, contentColor = BackgroundDark),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(text = "OK", fontWeight = FontWeight.Bold)
                }
            },
            containerColor = AlertBackground,
            shape = RoundedCornerShape(24.dp)
        )
    }

    // Preparing Import loading overlay
    if (isPreparingImport) {
        Dialog(
            onDismissRequest = {},
            properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
        ) {
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .background(SurfaceDark, shape = RoundedCornerShape(16.dp))
                    .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(color = CyberTeal)
                    Text(
                        text = strings.settingsScreen.preparingImport,
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }

    // Exporting/Restoring progress overlay
    if (isExporting || isRestoring) {
        Dialog(
            onDismissRequest = {},
            properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
        ) {
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .background(SurfaceDark, shape = RoundedCornerShape(16.dp))
                    .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(color = if (isExporting) NeonCoral else CyberTeal)
                    Text(
                        text = if (isExporting) strings.settingsScreen.exportingBackup else strings.settingsScreen.restoringBackup,
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

private fun isNetworkAvailable(context: android.content.Context): Boolean {
    val connectivityManager = context.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
    val activeNetwork = connectivityManager?.activeNetwork ?: return false
    val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
    return capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) ||
            capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR) ||
            capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_ETHERNET)
}
