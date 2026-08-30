package io.github.alexstraw.tminus

import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class UpdateSchedulerTest {
    private val london = ZoneId.of("Europe/London")

    @Test
    fun `next run is five minutes after next local midnight`() {
        val now = ZonedDateTime.of(2027, 8, 8, 15, 30, 0, 0, london)

        assertEquals(
            ZonedDateTime.of(2027, 8, 9, 0, 5, 0, 0, london),
            UpdateScheduler.nextRunAfter(now),
        )
    }

    @Test
    fun `spring DST uses the next local date rather than adding 24 hours`() {
        val now = ZonedDateTime.of(2027, 3, 28, 2, 0, 0, 0, london)

        assertEquals(
            ZonedDateTime.of(2027, 3, 29, 0, 5, 0, 0, london),
            UpdateScheduler.nextRunAfter(now),
        )
    }

    @Test
    fun `autumn DST uses the next local date rather than adding 24 hours`() {
        val now = ZonedDateTime.of(2027, 10, 31, 0, 30, 0, 0, london)

        assertEquals(
            ZonedDateTime.of(2027, 11, 1, 0, 5, 0, 0, london),
            UpdateScheduler.nextRunAfter(now),
        )
    }
}
