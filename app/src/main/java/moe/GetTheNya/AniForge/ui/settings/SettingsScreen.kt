package moe.GetTheNya.AniForge.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
    
    var dropdownExpanded by remember { mutableStateOf(false) }
    val currentLangName = availableLangs[currentLangCode] ?: currentLangCode

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .statusBarsPadding()
            .padding(horizontal = 24.dp)
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
                    imageVector = Icons.Default.ArrowBack,
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

            Divider(color = CardBorder, thickness = 1.dp)

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
                        text = strings.homeScreen.hotswappingEnabled,
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
                        text = strings.homeScreen.activeSlot,
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
                        text = strings.homeScreen.catalogStamp,
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
    }
}
