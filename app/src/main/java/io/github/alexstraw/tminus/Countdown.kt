package io.github.alexstraw.tminus

import java.time.LocalDate
import java.time.temporal.ChronoUnit

object Countdown {
    fun format(today: LocalDate, target: LocalDate): String {
        val days = ChronoUnit.DAYS.between(today, target)
        return when {
            days > 0 -> "T-$days"
            days < 0 -> "T+${-days}"
            else -> "T"
        }
    }
}
