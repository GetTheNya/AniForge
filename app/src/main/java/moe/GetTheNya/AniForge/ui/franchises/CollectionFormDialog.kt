package moe.GetTheNya.AniForge.ui.franchises

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import moe.GetTheNya.AniForge.ui.localization.LocalLocaleStrings
import moe.GetTheNya.AniForge.ui.theme.BackgroundDark
import moe.GetTheNya.AniForge.ui.theme.CardBorder
import moe.GetTheNya.AniForge.ui.theme.ElectricViolet
import moe.GetTheNya.AniForge.ui.theme.AlertBackground
import moe.GetTheNya.AniForge.ui.theme.SurfaceDark
import moe.GetTheNya.AniForge.ui.theme.TextPrimary
import moe.GetTheNya.AniForge.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionFormDialog(
    initialTitle: String,
    initialDescription: String,
    dialogTitle: String,
    confirmButtonText: String,
    descriptionLabel: String,
    onDismissRequest: () -> Unit,
    onConfirm: (title: String, description: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var title by remember { mutableStateOf(initialTitle) }
    var description by remember { mutableStateOf(initialDescription) }
    val strings = LocalLocaleStrings.current

    AlertDialog(
        modifier = modifier
            .border(
                width = 1.dp,
                color = CardBorder,
                shape = RoundedCornerShape(24.dp)
            ),
        onDismissRequest = onDismissRequest,
        title = { Text(dialogTitle, color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(strings.libraryScreen.title, color = TextSecondary) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = ElectricViolet,
                        unfocusedBorderColor = CardBorder,
                        focusedContainerColor = SurfaceDark,
                        unfocusedContainerColor = SurfaceDark
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(descriptionLabel, color = TextSecondary) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = ElectricViolet,
                        unfocusedBorderColor = CardBorder,
                        focusedContainerColor = SurfaceDark,
                        unfocusedContainerColor = SurfaceDark
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onConfirm(title, description)
                    }
                },
                enabled = title.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ElectricViolet,
                    disabledContainerColor = ElectricViolet.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(confirmButtonText, color = BackgroundDark, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(strings.libraryScreen.cancel, color = TextSecondary)
            }
        },
        containerColor = AlertBackground,
        shape = RoundedCornerShape(24.dp),
    )
}
