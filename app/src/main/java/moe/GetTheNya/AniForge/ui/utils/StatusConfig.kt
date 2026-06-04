package moe.GetTheNya.AniForge.ui.utils

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import moe.GetTheNya.AniForge.ui.localization.LocaleStrings

data class StatusItemConfig(
    val id: String,
    val color: Color,
    val activeIcon: ImageVector,
    val inactiveIcon: ImageVector,
    val getLabel: (strings: Any) -> String // Lambda to dynamically fetch localized strings
)

val statusConfigs: List<StatusItemConfig> = listOf(
    StatusItemConfig(
        id = "PLANNING",
        color = Color(0xFF9067C6),
        activeIcon = Icons.Default.Bookmark,
        inactiveIcon = Icons.Default.BookmarkBorder,
        getLabel = { strings -> (strings as LocaleStrings).misc.planning }
    ),
    StatusItemConfig(
        id = "CURRENT",
        color = Color(0xFF3B82F6),
        activeIcon = Icons.Default.PlayArrow,
        inactiveIcon = Icons.Default.PlayArrow,
        getLabel = { strings -> (strings as LocaleStrings).misc.watching }
    ),
    StatusItemConfig(
        id = "COMPLETED",
        color = Color(0xFF10B981),
        activeIcon = Icons.Default.CheckCircle,
        inactiveIcon = Icons.Default.CheckCircle,
        getLabel = { strings -> (strings as LocaleStrings).misc.completed }
    ),
    StatusItemConfig(
        id = "PAUSED",
        color = Color(0xFFF59E0B),
        activeIcon = Icons.Default.Pause,
        inactiveIcon = Icons.Default.Pause,
        getLabel = { strings -> (strings as LocaleStrings).misc.paused }
    ),
    StatusItemConfig(
        id = "DROPPED",
        color = Color(0xFFEF4444),
        activeIcon = Icons.Default.Cancel,
        inactiveIcon = Icons.Default.Cancel,
        getLabel = { strings -> (strings as LocaleStrings).misc.dropped }
    )
)

val AnimeStatusColors: Map<String, Color> = statusConfigs.associate { it.id to it.color }
