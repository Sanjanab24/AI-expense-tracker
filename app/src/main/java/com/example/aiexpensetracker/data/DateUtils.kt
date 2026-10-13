package com.example.aiexpensetracker.data

import java.util.Calendar

object DateUtils {
    /**
     * Returns a pair of [startOfMonth, nextMonthStart] timestamps in milliseconds
     * for the given 1-based month (1-12) and 4-digit year.
     *
     * The range is start-inclusive and end-exclusive:
     * date >= startOfMonth && date < nextMonthStart
     */
    fun getMonthRange(month: Int, year: Int): Pair<Long, Long> {
        val cal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfMonth = cal.timeInMillis
        cal.add(Calendar.MONTH, 1)
        val nextMonthStart = cal.timeInMillis
        return Pair(startOfMonth, nextMonthStart)
    }

    /**
     * Returns the [startOfMonth, nextMonthStart] timestamp pair for the current month and year.
     */
    fun getCurrentMonthRange(): Pair<Long, Long> {
        val now = Calendar.getInstance()
        return getMonthRange(now.get(Calendar.MONTH) + 1, now.get(Calendar.YEAR))
    }
}
