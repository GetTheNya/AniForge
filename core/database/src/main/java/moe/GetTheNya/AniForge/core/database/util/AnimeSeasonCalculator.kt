package moe.GetTheNya.AniForge.core.database.util

import moe.GetTheNya.AniForge.core.model.TransitionState
import java.time.OffsetDateTime

object AnimeSeasonCalculator {
    fun calculateSeason(generatedAt: String): TransitionState? {
        return try {
            val odt = OffsetDateTime.parse(generatedAt)
            val year = odt.year
            val month = odt.monthValue
            val day = odt.dayOfMonth

            val isTransitionMonth = month == 1 || month == 4 || month == 7 || month == 10
            val isFallback = isTransitionMonth && day in 1..7

            val (season, seasonYear) = if (isFallback) {
                when (month) {
                    1 -> "FALL" to (year - 1)
                    4 -> "WINTER" to year
                    7 -> "SPRING" to year
                    10 -> "SUMMER" to year
                    else -> throw IllegalArgumentException("Invalid transition month: $month")
                }
            } else {
                val currentSeason = when (month) {
                    1, 2, 3 -> "WINTER"
                    4, 5, 6 -> "SPRING"
                    7, 8, 9 -> "SUMMER"
                    10, 11, 12 -> "FALL"
                    else -> throw IllegalArgumentException("Invalid month: $month")
                }
                currentSeason to year
            }

            TransitionState(
                season = season,
                seasonYear = seasonYear,
                isFallbackToPrevious = isFallback
            )
        } catch (e: Exception) {
            null
        }
    }
}
