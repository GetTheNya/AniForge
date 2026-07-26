package moe.GetTheNya.AniForge.ui.bento

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import moe.GetTheNya.AniForge.core.model.CandidateStatus
import moe.GetTheNya.AniForge.core.model.WaitingItem
import moe.GetTheNya.AniForge.ui.theme.*
import moe.GetTheNya.AniForge.ui.utils.statusConfigs
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ContinuationUpdatesWidget(
    waitingItems: List<WaitingItem>,
    onAddClick: (Long, String) -> Unit,
    onDismissClick: (Long) -> Unit,
    onItemClick: (Long) -> Unit,
    onHeaderClick: () -> Unit = {},
    onLongClick: (() -> Unit)? = null,
    isEditMode: Boolean = false,
    preferUk: Boolean = true,
    modifier: Modifier = Modifier
) {
    val strings = moe.GetTheNya.AniForge.ui.localization.LocalLocaleStrings.current
    val activeUpdates = waitingItems.filter {
        it.candidateStatus == CandidateStatus.READY_TO_WATCH || it.candidateStatus == CandidateStatus.RELEASED || it.candidateStatus == CandidateStatus.ANNOUNCED
    }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = BorderStroke(1.dp, CardBorder),
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onLongClick != null) {
                    Modifier.combinedClickable(
                        onClick = { if (!isEditMode) onHeaderClick() },
                        onLongClick = onLongClick
                    )
                } else Modifier
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (!isEditMode) Modifier.clickable { onHeaderClick() } else Modifier
                    ),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(NeonCoral.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = NeonCoral,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Text(
                        text = strings.bentoWidgets.continuationUpdates,
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (activeUpdates.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(NeonCoral)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = activeUpdates.size.toString(),
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            if (activeUpdates.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.03f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = strings.libraryScreen.noAnnouncements,
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                }
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(
                        items = activeUpdates,
                        key = { it.baseAnime.anilistId }
                    ) { item ->
                        val candidate = item.candidateSequel ?: return@items
                        val isReadyToWatch = item.candidateStatus == CandidateStatus.READY_TO_WATCH
                        val isReleased = item.candidateStatus == CandidateStatus.RELEASED
                        val statusColor = when (item.candidateStatus) {
                            CandidateStatus.READY_TO_WATCH -> CyberTeal
                            CandidateStatus.RELEASED -> CyberTeal
                            else -> NeonCoral
                        }
                        val targetStatus = if (isReadyToWatch || isReleased) "CURRENT" else "PLANNING"
                        val actionButtonColor = statusConfigs.firstOrNull { it.id == targetStatus }?.color
                            ?: (if (isReadyToWatch || isReleased) Color(0xFF3B82F6) else Color(0xFF9067C6))
                        val badgeText = when (item.candidateStatus) {
                            CandidateStatus.READY_TO_WATCH -> strings.libraryScreen.readyToWatch
                            CandidateStatus.RELEASED -> strings.libraryScreen.sequelReleased
                            else -> strings.libraryScreen.sequelAnnounced
                        }
                        val buttonText = if (isReadyToWatch || isReleased) strings.libraryScreen.addToWatching else strings.libraryScreen.addToPlanned

                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f)),
                            border = BorderStroke(1.dp, CardBorder),
                            modifier = Modifier
                                .width(260.dp)
                                .clickable { onItemClick(candidate.anilistId) }
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(54.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(CardBorder)
                                    ) {
                                        val cover = candidate.coverLarge ?: item.baseAnime.coverLarge
                                        if (!cover.isNullOrEmpty()) {
                                            AsyncImage(
                                                model = cover,
                                                contentDescription = candidate.getDisplayTitle(preferUk),
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                    }

                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(statusColor.copy(alpha = 0.18f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = badgeText,
                                                color = statusColor,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        Text(
                                            text = candidate.getDisplayTitle(preferUk),
                                            color = TextPrimary,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        Text(
                                            text = "Base: ${item.baseAnime.getDisplayTitle(preferUk)}",
                                            color = TextSecondary,
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Button(
                                        onClick = { onAddClick(candidate.anilistId, targetStatus) },
                                        colors = ButtonDefaults.buttonColors(containerColor = actionButtonColor),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.weight(1f).height(32.dp)
                                    ) {
                                        Text(
                                            text = buttonText,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }

                                    IconButton(
                                        onClick = { onDismissClick(candidate.anilistId) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = strings.libraryScreen.hideSequel,
                                            tint = TextSecondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
