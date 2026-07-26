package moe.GetTheNya.AniForge.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import moe.GetTheNya.AniForge.core.model.Anime
import moe.GetTheNya.AniForge.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContinuationHandoverBottomSheet(
    prequelAnime: Anime,
    completedAnime: Anime,
    onWaitNextSeason: (Long, Long) -> Unit,
    onStopWaiting: (Long, Long) -> Unit,
    onDismiss: () -> Unit,
    preferUk: Boolean = true
) {
    val strings = moe.GetTheNya.AniForge.ui.localization.LocalLocaleStrings.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SurfaceDark,
        contentColor = TextPrimary,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = { BottomSheetDefaults.DragHandle(color = TextSecondary.copy(alpha = 0.5f)) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Visual Banner: Prequel -> Completed transition artwork
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Prequel Cover (dimmed / previous)
                Box(
                    modifier = Modifier
                        .size(64.dp, 90.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(CardBorder)
                ) {
                    if (!prequelAnime.coverLarge.isNullOrEmpty()) {
                        AsyncImage(
                            model = prequelAnime.coverLarge,
                            contentDescription = prequelAnime.getDisplayTitle(preferUk),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                            alpha = 0.6f
                        )
                    }
                }

                // Transfer Icon
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(NeonCoral.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalFireDepartment,
                        contentDescription = null,
                        tint = NeonCoral,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Completed Anime Cover (bright / active)
                Box(
                    modifier = Modifier
                        .size(64.dp, 90.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(CardBorder)
                ) {
                    if (!completedAnime.coverLarge.isNullOrEmpty()) {
                        AsyncImage(
                            model = completedAnime.coverLarge,
                            contentDescription = completedAnime.getDisplayTitle(preferUk),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            // Title & Content Text
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = strings.libraryScreen.continuationHandoverTitle,
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = strings.libraryScreen.continuationHandoverContent.replace(
                        "{title}",
                        completedAnime.getDisplayTitle(preferUk)
                    ),
                    color = TextSecondary,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
            }

            // Action Handlers
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Option 1: Wait for Next Season
                Button(
                    onClick = {
                        onWaitNextSeason(prequelAnime.anilistId, completedAnime.anilistId)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCoral),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text(
                        text = strings.libraryScreen.waitForNextSeason,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // Option 2: Stop Waiting
                OutlinedButton(
                    onClick = {
                        onStopWaiting(prequelAnime.anilistId, completedAnime.anilistId)
                    },
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, CardBorder),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text(
                        text = strings.libraryScreen.stopWaiting,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}
