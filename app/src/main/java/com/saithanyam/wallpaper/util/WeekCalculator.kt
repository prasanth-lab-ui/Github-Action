package com.saithanyam.wallpaper.util

import java.time.LocalDate
import java.time.temporal.ChronoUnit

object WeekCalculator {

    const val TOTAL_WEEKS = 4000

    /**
     * Returns the number of full weeks lived since [birthday] up to today.
     * Clamped to [0, TOTAL_WEEKS].
     */
    fun weeksLived(birthday: LocalDate): Int {
        val today = LocalDate.now()
        if (today.isBefore(birthday)) return 0
        val days = ChronoUnit.DAYS.between(birthday, today)
        return (days / 7).toInt().coerceIn(0, TOTAL_WEEKS)
    }

    fun weeksRemaining(birthday: LocalDate): Int {
        return (TOTAL_WEEKS - weeksLived(birthday)).coerceAtLeast(0)
    }
}
