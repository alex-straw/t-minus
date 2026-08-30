package io.github.alexstraw.tminus

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class CountdownTest {
    @Test
    fun `future target uses T minus days`() {
        assertEquals(
            "T-364",
            Countdown.format(LocalDate.of(2026, 8, 9), LocalDate.of(2027, 8, 8)),
        )
    }

    @Test
    fun `tomorrow uses T minus one`() {
        assertEquals(
            "T-1",
            Countdown.format(LocalDate.of(2027, 8, 7), LocalDate.of(2027, 8, 8)),
        )
    }

    @Test
    fun `target date uses T`() {
        val date = LocalDate.of(2027, 8, 8)
        assertEquals("T", Countdown.format(date, date))
    }

    @Test
    fun `yesterday uses T plus one`() {
        assertEquals(
            "T+1",
            Countdown.format(LocalDate.of(2027, 8, 9), LocalDate.of(2027, 8, 8)),
        )
    }

    @Test
    fun `leap day is counted as a calendar day`() {
        assertEquals(
            "T-2",
            Countdown.format(LocalDate.of(2028, 2, 28), LocalDate.of(2028, 3, 1)),
        )
    }

    @Test
    fun `year boundary is counted correctly`() {
        assertEquals(
            "T-1",
            Countdown.format(LocalDate.of(2027, 12, 31), LocalDate.of(2028, 1, 1)),
        )
    }

    @Test
    fun `historical target uses elapsed calendar days`() {
        assertEquals(
            "T+366",
            Countdown.format(LocalDate.of(2029, 1, 1), LocalDate.of(2028, 1, 1)),
        )
    }
}
