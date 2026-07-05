package moe.GetTheNya.AniForge.ui.profile

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
}
