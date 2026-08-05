package org.carlospinan.bloqueador.app.rules

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime

/**
 * Time boundaries for the blocked-call statistics, in the user's local calendar.
 *
 * These used to be computed in SQL as `strftime('%s', 'now', 'start of day')`, which is midnight
 * **UTC**. "Blocked today" therefore reset at 19:00 for a reader in New York and at 09:00 the
 * next morning in Tokyo, and the chart's "Today" bucket held calls from two different local
 * days. Computing them here with kotlinx-datetime makes them land on local midnight and stay
 * correct across a DST change, where a day is 23 or 25 hours long rather than exactly 86 400 000
 * milliseconds — the arithmetic the old bucket code did.
 *
 * Every value is an epoch-milliseconds instant, matching what `CallLogEntry.timestamp` stores.
 */
object StatsWindows {
    /** Local midnight at the start of the day containing [nowMillis]. */
    fun startOfToday(
        nowMillis: Long,
        timeZone: TimeZone = TimeZone.currentSystemDefault(),
    ): Long = localDate(nowMillis, timeZone).atStartOfDayIn(timeZone).toEpochMilliseconds()

    /** Local midnight [days] calendar days before the day containing [nowMillis]. */
    fun startOfDaysAgo(
        nowMillis: Long,
        days: Int,
        timeZone: TimeZone = TimeZone.currentSystemDefault(),
    ): Long =
        localDate(nowMillis, timeZone)
            .minus(days, DateTimeUnit.DAY)
            .atStartOfDayIn(timeZone)
            .toEpochMilliseconds()

    /**
     * Local midnight on the Monday of the ISO week containing [nowMillis]. Matches what the old
     * `'start of day', 'weekday 0', '-6 days'` SQL was reaching for, minus the UTC skew.
     */
    fun startOfWeek(
        nowMillis: Long,
        timeZone: TimeZone = TimeZone.currentSystemDefault(),
    ): Long {
        val today = localDate(nowMillis, timeZone)
        return today
            .minus(today.dayOfWeek.isoDayNumber - 1, DateTimeUnit.DAY)
            .atStartOfDayIn(timeZone)
            .toEpochMilliseconds()
    }

    /** Local midnight on the 1st of the month containing [nowMillis]. */
    fun startOfMonth(
        nowMillis: Long,
        timeZone: TimeZone = TimeZone.currentSystemDefault(),
    ): Long {
        val today = localDate(nowMillis, timeZone)
        return LocalDate(today.year, today.month, 1).atStartOfDayIn(timeZone).toEpochMilliseconds()
    }

    /**
     * Half-open `[start, end)` bounds for the last [daysBack] local days, newest bucket first.
     *
     * Derived from consecutive local midnights rather than by subtracting a fixed 86 400 000 ms,
     * so the 23-hour and 25-hour days around a DST transition each get exactly the calls that
     * happened on them.
     */
    fun dayBuckets(
        nowMillis: Long,
        daysBack: Int,
        timeZone: TimeZone = TimeZone.currentSystemDefault(),
    ): List<DayBucket> =
        (0 until daysBack).map { dayOffset ->
            DayBucket(
                daysAgo = dayOffset,
                startMillis = startOfDaysAgo(nowMillis, dayOffset, timeZone),
                endMillis = startOfDaysAgo(nowMillis, dayOffset - 1, timeZone),
            )
        }

    private fun localDate(
        epochMillis: Long,
        timeZone: TimeZone,
    ): LocalDate = Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(timeZone).date
}

/** One local calendar day, as a half-open `[startMillis, endMillis)` range. */
data class DayBucket(
    /** 0 = the day containing "now", 1 = the day before it, and so on. */
    val daysAgo: Int,
    val startMillis: Long,
    val endMillis: Long,
)
