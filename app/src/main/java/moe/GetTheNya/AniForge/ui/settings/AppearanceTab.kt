package moe.GetTheNya.AniForge.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import moe.GetTheNya.AniForge.ui.localization.LocalLocaleStrings
import moe.GetTheNya.AniForge.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceTab(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val strings = LocalLocaleStrings.current
    val preferUk by viewModel.preferUkTitles.collectAsState()
    val show18Plus by viewModel.show18Plus.collectAsState()
    val hideNavigationBar by viewModel.hideNavigationBar.collectAsState()
    val currentLangCode by viewModel.currentLanguage.collectAsState()
    val availableLangs by viewModel.availableLanguages.collectAsState()

    val gestureCenter by viewModel.gestureCenter.collectAsState()
    val gestureUp by viewModel.gestureUp.collectAsState()
    val gestureDown by viewModel.gestureDown.collectAsState()
    val gestureLeft by viewModel.gestureLeft.collectAsState()
    val gestureRight by viewModel.gestureRight.collectAsState()

    var showWarningDialog by remember { mutableStateOf(false) }
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
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Settings Form Card
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

            HorizontalDivider(Modifier, thickness = 1.dp, color = CardBorder)

            // 4. Hide Navigation Bar Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                    Text(
                        text = strings.settingsScreen.hideNavigationBarTitle,
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = strings.settingsScreen.hideNavigationBarDesc,
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
                Switch(
                    checked = hideNavigationBar,
                    onCheckedChange = viewModel::setHideNavigationBar,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = BackgroundDark,
                        checkedTrackColor = NeonCoral,
                        uncheckedThumbColor = TextSecondary,
                        uncheckedTrackColor = Color(0x33FFFFFF)
                    )
                )
            }
        }

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

        Spacer(modifier = Modifier.height(16.dp))
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
