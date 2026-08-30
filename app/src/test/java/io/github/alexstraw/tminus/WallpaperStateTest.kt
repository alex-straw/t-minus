package io.github.alexstraw.tminus

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WallpaperStateTest {
    private val activeState = PersistedTState(
        targetEpochDay = LocalDate.of(2027, 8, 8).toEpochDay(),
        active = true,
        lastWallpaperId = 42,
        lastRenderedEpochDay = LocalDate.of(2026, 8, 9).toEpochDay(),
    )

    @Test
    fun `matching wallpaper ID permits automatic update`() {
        assertEquals(
            AutomaticUpdateDecision.APPLY,
            automaticUpdateDecision(activeState, currentWallpaperId = 42),
        )
    }

    @Test
    fun `changed wallpaper ID stops automatic update`() {
        assertEquals(
            AutomaticUpdateDecision.STOP,
            automaticUpdateDecision(activeState, currentWallpaperId = 99),
        )
    }

    @Test
    fun `inactive state never permits automatic update`() {
        assertEquals(
            AutomaticUpdateDecision.STOP,
            automaticUpdateDecision(activeState.copy(active = false), currentWallpaperId = 42),
        )
    }

    @Test
    fun `successful apply activates and stores confirmed target`() {
        val target = LocalDate.of(2030, 1, 2)
        val renderedOn = LocalDate.of(2029, 12, 31)
        val result = stateAfterSuccessfulApply(
            activeState.copy(active = false),
            target,
            77,
            renderedOn,
        )

        assertTrue(result.active)
        assertEquals(target.toEpochDay(), result.targetEpochDay)
        assertEquals(77, result.lastWallpaperId)
        assertEquals(renderedOn.toEpochDay(), result.lastRenderedEpochDay)
    }

    @Test
    fun `failed apply leaves active state and saved target unchanged`() {
        val previous = activeState.copy(active = false)
        val result = stateAfterSuccessfulApply(
            previous,
            LocalDate.of(2030, 1, 2),
            0,
            LocalDate.of(2029, 12, 31),
        )

        assertFalse(result.active)
        assertEquals(previous, result)
    }

    @Test
    fun `current render day skips expensive calendar refresh`() {
        assertFalse(needsCalendarRefresh(activeState, LocalDate.of(2026, 8, 9)))
    }

    @Test
    fun `stale render day requires calendar refresh`() {
        assertTrue(needsCalendarRefresh(activeState, LocalDate.of(2026, 8, 10)))
    }
}
