package moe.GetTheNya.AniForge.core.model

import androidx.compose.runtime.Immutable

@Immutable
enum class RelationType(val rawValue: String) {
    ADAPTATION("ADAPTATION"),
    PREQUEL("PREQUEL"),
    SEQUEL("SEQUEL"),
    PARENT("PARENT"),
    SIDE_STORY("SIDE_STORY"),
    CHARACTER("CHARACTER"),
    SUMMARY("SUMMARY"),
    ALTERNATIVE("ALTERNATIVE"),
    SPIN_OFF("SPIN_OFF"),
    OTHER("OTHER"),
    SOURCE("SOURCE"),
    COMPILATION("COMPILATION"),
    CONTAINS("CONTAINS");

    companion object {
        fun fromString(value: String): RelationType {
            return entries.firstOrNull { it.rawValue.equals(value, ignoreCase = true) } ?: OTHER
        }
    }
}
