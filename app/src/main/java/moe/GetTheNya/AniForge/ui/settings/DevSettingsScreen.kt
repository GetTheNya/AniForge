package moe.GetTheNya.AniForge.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import moe.GetTheNya.AniForge.core.database.settings.SettingsProvider
import moe.GetTheNya.AniForge.ui.home.TitleAnimStyle
import moe.GetTheNya.AniForge.ui.home.SubtitleAnimStyle
import moe.GetTheNya.AniForge.ui.home.ContentAnimStyle
import moe.GetTheNya.AniForge.ui.localization.LocalLocaleStrings
import moe.GetTheNya.AniForge.ui.localization.getAnimationLabel
import moe.GetTheNya.AniForge.ui.navigation.NavController
import moe.GetTheNya.AniForge.ui.navigation.Screen
import moe.GetTheNya.AniForge.ui.theme.*
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.platform.LocalContext
import moe.GetTheNya.AniForge.ui.dashboard.UserTrackingRepository

@Composable
fun DevSettingsScreen(
    navController: NavController,
    settingsProvider: SettingsProvider,
    userTrackingRepository: UserTrackingRepository,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = { navController.popBackStack() }
) {
    val strings = LocalLocaleStrings.current
    val scrollState = rememberScrollState()

    val currentTitleStyle by settingsProvider.titleAnimStyleStr.collectAsState()
    val currentSubtitleStyle by settingsProvider.subtitleAnimStyleStr.collectAsState()
    val currentContentStyle by settingsProvider.contentAnimStyleStr.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .statusBarsPadding()
            .padding(horizontal = 24.dp)
            .verticalScroll(scrollState)
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
                text = strings.devSettings.devSettingsHeader,
                color = TextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Developer Settings Options Container
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(SurfaceDark)
                .border(1.dp, CardBorder, RoundedCornerShape(24.dp))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 1. View System Logs row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { navController.navigate(Screen.LogViewer) }
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                    Text(
                        text = strings.devSettings.viewLogs,
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = strings.devSettings.diagnosticsEngineStatus,
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
                Button(
                    onClick = { navController.navigate(Screen.LogViewer) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyberTeal,
                        contentColor = BackgroundDark
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = strings.devSettings.viewLogsButton,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            HorizontalDivider(color = CardBorder, thickness = 1.dp)

            // 2. Recalculate Watch Time Stats row
            var isRecalculating by remember { mutableStateOf(false) }
            val context = LocalContext.current
            val coroutineScope = rememberCoroutineScope()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                    Text(
                        text = strings.devSettings.recalculateWatchTime,
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = strings.devSettings.recalculateWatchTimeDesc,
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
                Button(
                    onClick = {
                        isRecalculating = true
                        coroutineScope.launch(Dispatchers.IO) {
                            try {
                                userTrackingRepository.recalculateTotalWatchTime()
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(
                                        context,
                                        strings.devSettings.watchTimeCacheRebuilt,
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(
                                        context,
                                        "Error: ${e.localizedMessage}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            } finally {
                                withContext(Dispatchers.Main) {
                                    isRecalculating = false
                                }
                            }
                        }
                    },
                    enabled = !isRecalculating,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyberTeal,
                        contentColor = BackgroundDark,
                        disabledContainerColor = CyberTeal.copy(alpha = 0.5f),
                        disabledContentColor = BackgroundDark.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isRecalculating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = BackgroundDark,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = strings.devSettings.recalculateButton,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = strings.devSettings.animationSandbox,
            color = TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Sandbox Styles Container
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(SurfaceDark)
                .border(1.dp, CardBorder, RoundedCornerShape(24.dp))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val titleLabel = strings.devSettings.devSettingsTitleAnim
            val titleCurrent = try { TitleAnimStyle.valueOf(currentTitleStyle) } catch (e: Exception) { TitleAnimStyle.DECODING }
            AnimationSettingDropdown(
                label = titleLabel,
                currentValue = titleCurrent,
                options = TitleAnimStyle.entries.toTypedArray(),
                getLabel = { strings.devSettings.getAnimationLabel(it.labelKey) },
                onValueSelected = { settingsProvider.setTitleAnimStyle(it.name) }
            )

            HorizontalDivider(color = CardBorder, thickness = 1.dp)

            val subtitleLabel = strings.devSettings.devSettingsSubtitleAnim
            val subtitleCurrent = try { SubtitleAnimStyle.valueOf(currentSubtitleStyle) } catch (e: Exception) { SubtitleAnimStyle.BLUR_FADE }
            AnimationSettingDropdown(
                label = subtitleLabel,
                currentValue = subtitleCurrent,
                options = SubtitleAnimStyle.entries.toTypedArray(),
                getLabel = { strings.devSettings.getAnimationLabel(it.labelKey) },
                onValueSelected = { settingsProvider.setSubtitleAnimStyle(it.name) }
            )

            HorizontalDivider(color = CardBorder, thickness = 1.dp)

            val contentLabel = strings.devSettings.devSettingsContentAnim
            val contentCurrent = try { ContentAnimStyle.valueOf(currentContentStyle) } catch (e: Exception) { ContentAnimStyle.POWER_UP }
            AnimationSettingDropdown(
                label = contentLabel,
                currentValue = contentCurrent,
                options = ContentAnimStyle.entries.toTypedArray(),
                getLabel = { strings.devSettings.getAnimationLabel(it.labelKey) },
                onValueSelected = { settingsProvider.setContentAnimStyle(it.name) }
            )
        }

        Spacer(modifier = Modifier.height(48.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T : Enum<T>> AnimationSettingDropdown(
    label: String,
    currentValue: T,
    options: Array<T>,
    getLabel: (T) -> String,
    onValueSelected: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(6.dp))
        
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it }
        ) {
            OutlinedTextField(
                value = getLabel(currentValue),
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedBorderColor = NeonCoral,
                    unfocusedBorderColor = CardBorder,
                    focusedContainerColor = Color(0xFF0F0F13),
                    unfocusedContainerColor = Color(0xFF0F0F13)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                shape = RoundedCornerShape(12.dp)
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
                                text = getLabel(option),
                                color = TextPrimary,
                                fontWeight = if (option == currentValue) FontWeight.Bold else FontWeight.Normal
                            ) 
                        },
                        onClick = {
                            onValueSelected(option)
                            expanded = false
                        },
                        modifier = Modifier.background(SurfaceDark),
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                    )
                }
            }
        }
    }
}
