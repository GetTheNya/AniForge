package moe.GetTheNya.AniForge.core.database.util

import moe.GetTheNya.AniForge.core.model.AnimeSeasonInfo
import java.time.OffsetDateTime

object AnimeSeasonCalculator {
    fun calculateSeason(generatedAt: String): AnimeSeasonInfo? {
        return try {
            val odt = OffsetDateTime.parse(generatedAt)
            val year = odt.year
            val month = odt.monthValue
            val season = when (month) {
                1, 2, 3 -> "WINTER"
                4, 5, 6 -> "SPRING"
                7, 8, 9 -> "SUMMER"
                10, 11, 12 -> "FALL"
                else -> throw IllegalArgumentException("Invalid month: $month")
            }
            AnimeSeasonInfo(season, year)
        } catch (e: Exception) {
            null
        }
    }
}
