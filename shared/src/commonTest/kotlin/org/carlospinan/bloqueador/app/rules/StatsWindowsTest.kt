package org.carlospinan.bloqueador.app.rules

import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Statistics windows land on *local* midnight and survive a DST change.
 *
 * The boundaries used to come from SQLite's `strftime('%s','now','start of day')`, which is
 * midnight UTC. That gave every user outside UTC a "blocked today" counter that reset at the
 * wrong time of day, and a chart whose "Today" row held calls from two different local dates.
 * The day buckets were then built by subtracting a fixed 86 400 000 ms per step, which is
 * simply the wrong length on the two days a year a DST transition makes a day 23 or 25 hours.
 */
class StatsWindowsTest {
    private val madrid = TimeZone.of("Europe/Madrid")
    private val newYork = TimeZone.of("America/New_York")
    private val kathmandu = TimeZone.of("Asia/Kathmandu") // UTC+05:45, a non-hour offset

    private fun localTimeOf(
        millis: Long,
        zone: TimeZone,
    ) = Instant.fromEpochMilliseconds(millis).toLocalDateTime(zone)

    @Test
    fun startOfTodayIsLocalMidnightNotUtcMidnight() {
        // 2026-08-05T02:30 in Madrid (UTC+2 in summer) — i.e. 00:30 UTC, still "yesterday" for
        // anyone reading a UTC boundary.
        val now = Instant.parse("2026-08-05T00:30:00Z").toEpochMilliseconds()

        val start = StatsWindows.startOfToday(now, madrid)

        val local = localTimeOf(start, madrid)
        assertEquals(0, local.hour)
        assertEquals(0, local.minute)
        assertEquals(5, local.dayOfMonth)
        assertTrue(start <= now, "the start of today can't be in the future")
    }

    @Test
    fun aWesternUserGetsTheirOwnDayNotTomorrowsUtcOne() {
        // 2026-08-05T22:00 in New York is already 2026-08-06T02:00 UTC. Under the old UTC
        // boundary this user's "today" had silently rolled over three hours before midnight.
        val now = Instant.parse("2026-08-06T02:00:00Z").toEpochMilliseconds()

        val local = localTimeOf(StatsWindows.startOfToday(now, newYork), newYork)

        assertEquals(5, local.dayOfMonth)
        assertEquals(0, local.hour)
    }

    @Test
    fun nonHourOffsetsStillLandExactlyOnMidnight() {
        val now = Instant.parse("2026-08-05T12:00:00Z").toEpochMilliseconds()

        val local = localTimeOf(StatsWindows.startOfToday(now, kathmandu), kathmandu)

        assertEquals(0, local.hour)
        assertEquals(0, local.minute)
    }

    @Test
    fun startOfWeekIsTheMondayOfTheCurrentIsoWeek() {
        // 2026-08-05 is a Wednesday.
        val now = Instant.parse("2026-08-05T12:00:00Z").toEpochMilliseconds()

        val local = localTimeOf(StatsWindows.startOfWeek(now, madrid), madrid)

        assertEquals(3, local.dayOfMonth, "Monday of that week is the 3rd")
        assertEquals(0, local.hour)
    }

    @Test
    fun startOfWeekOnAMondayIsThatSameMorning() {
        // 2026-08-03 is a Monday; the week must start today, not seven days earlier.
        val now = Instant.parse("2026-08-03T12:00:00Z").toEpochMilliseconds()

        assertEquals(
            StatsWindows.startOfToday(now, madrid),
            StatsWindows.startOfWeek(now, madrid),
        )
    }

    @Test
    fun startOfMonthIsTheFirstAtLocalMidnight() {
        val now = Instant.parse("2026-08-05T12:00:00Z").toEpochMilliseconds()

        val local = localTimeOf(StatsWindows.startOfMonth(now, madrid), madrid)

        assertEquals(1, local.dayOfMonth)
        assertEquals(kotlinx.datetime.Month.AUGUST, local.month)
        assertEquals(0, local.hour)
    }

    @Test
    fun theWeekAndMonthAlwaysContainTheDay() {
        val now = Instant.parse("2026-08-05T12:00:00Z").toEpochMilliseconds()

        val today = StatsWindows.startOfToday(now, madrid)
        assertTrue(StatsWindows.startOfWeek(now, madrid) <= today)
        assertTrue(StatsWindows.startOfMonth(now, madrid) <= today)
    }

    // ---- day buckets ----

    @Test
    fun bucketsAreContiguousAndNewestFirst() {
        val now = Instant.parse("2026-08-05T12:00:00Z").toEpochMilliseconds()

        val buckets = StatsWindows.dayBuckets(now, daysBack = 7, timeZone = madrid)

        assertEquals(7, buckets.size)
        assertEquals(List(7) { it }, buckets.map { it.daysAgo })
        buckets.zipWithNext { newer, older ->
            assertEquals(newer.startMillis, older.endMillis, "buckets must tile without gaps or overlap")
        }
    }

    @Test
    fun theNewestBucketStartsAtTheSameInstantAsStartOfToday() {
        val now = Instant.parse("2026-08-05T12:00:00Z").toEpochMilliseconds()

        val buckets = StatsWindows.dayBuckets(now, daysBack = 7, timeZone = madrid)

        // Home's "blocked today" and the chart's first bar must never disagree.
        assertEquals(StatsWindows.startOfToday(now, madrid), buckets.first().startMillis)
        assertTrue(now < buckets.first().endMillis)
    }

    @Test
    fun theSpringForwardDayIs23HoursLongNotExactly24() {
        // Europe/Madrid springs forward on 2026-03-29: 02:00 becomes 03:00, so that local day
        // is 23 hours. Fixed 86_400_000 ms arithmetic would slide every earlier bucket an hour
        // off local midnight and drop calls into the wrong day.
        val now = Instant.parse("2026-03-30T12:00:00Z").toEpochMilliseconds()

        val buckets = StatsWindows.dayBuckets(now, daysBack = 3, timeZone = madrid)
        val springForward = buckets.single { localTimeOf(it.startMillis, madrid).dayOfMonth == 29 }

        assertEquals(23 * 60 * 60 * 1000L, springForward.endMillis - springForward.startMillis)
        for (bucket in buckets) {
            assertEquals(0, localTimeOf(bucket.startMillis, madrid).hour, "every bucket still starts at local midnight")
        }
    }

    @Test
    fun theFallBackDayIs25HoursLong() {
        // Europe/Madrid falls back on 2026-10-25: 03:00 becomes 02:00, a 25-hour local day.
        val now = Instant.parse("2026-10-26T12:00:00Z").toEpochMilliseconds()

        val buckets = StatsWindows.dayBuckets(now, daysBack = 3, timeZone = madrid)
        val fallBack = buckets.single { localTimeOf(it.startMillis, madrid).dayOfMonth == 25 }

        assertEquals(25 * 60 * 60 * 1000L, fallBack.endMillis - fallBack.startMillis)
    }

    @Test
    fun zeroDaysBackProducesNoBuckets() {
        val now = Instant.parse("2026-08-05T12:00:00Z").toEpochMilliseconds()

        assertEquals(emptyList(), StatsWindows.dayBuckets(now, daysBack = 0, timeZone = madrid))
    }
}
