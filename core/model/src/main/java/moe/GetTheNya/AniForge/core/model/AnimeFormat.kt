package moe.GetTheNya.AniForge.core.model

import androidx.compose.runtime.Immutable

@Immutable
enum class AnimeFormat(val rawValue: String) {
    TV("TV"),
    TV_SHORT("TV_SHORT"),
    MOVIE("MOVIE"),
    SPECIAL("SPECIAL"),
    OVA("OVA"),
    ONA("ONA"),
    MUSIC("MUSIC"),
    MANGA("MANGA"),
    NOVEL("NOVEL"),
    ONE_SHOT("ONE_SHOT")
}
