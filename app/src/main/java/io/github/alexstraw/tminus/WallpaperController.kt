package io.github.alexstraw.tminus

import android.app.WallpaperManager
import android.content.Context
import java.time.LocalDate

sealed interface WallpaperApplyResult {
    data class Success(val wallpaperId: Int) : WallpaperApplyResult
    data object NotAllowed : WallpaperApplyResult
    data class Error(val cause: Exception) : WallpaperApplyResult
}

enum class AutomaticUpdateDecision {
    APPLY,
    STOP,
}

class WallpaperController(context: Context) {
    private val appContext = context.applicationContext
    private val wallpaperManager = WallpaperManager.getInstance(appContext)
    private val renderer = WallpaperRenderer(appContext)

    fun isWallpaperChangeAllowed(): Boolean = try {
        wallpaperManager.isWallpaperSupported && wallpaperManager.isSetWallpaperAllowed
    } catch (_: RuntimeException) {
        false
    }

    fun currentLockWallpaperId(): Int = try {
        wallpaperManager.getWallpaperId(WallpaperManager.FLAG_LOCK)
    } catch (_: RuntimeException) {
        TPreferences.NO_WALLPAPER_ID
    }

    fun apply(target: LocalDate, today: LocalDate = LocalDate.now()): WallpaperApplyResult {
        if (!isWallpaperChangeAllowed()) return WallpaperApplyResult.NotAllowed

        var bitmap: android.graphics.Bitmap? = null
        return try {
            bitmap = renderer.render(Countdown.format(today, target))
            val wallpaperId = wallpaperManager.setBitmap(
                bitmap,
                null,
                false,
                WallpaperManager.FLAG_LOCK,
            )
            if (wallpaperId > 0) {
                WallpaperApplyResult.Success(wallpaperId)
            } else {
                WallpaperApplyResult.Error(IllegalStateException("WallpaperManager returned no ID"))
            }
        } catch (exception: Exception) {
            WallpaperApplyResult.Error(exception)
        } finally {
            bitmap?.recycle()
        }
    }
}

internal fun automaticUpdateDecision(
    state: PersistedTState,
    currentWallpaperId: Int,
): AutomaticUpdateDecision = if (
    state.active &&
    state.lastWallpaperId > 0 &&
    state.lastWallpaperId == currentWallpaperId
) {
    AutomaticUpdateDecision.APPLY
} else {
    AutomaticUpdateDecision.STOP
}

internal fun needsCalendarRefresh(state: PersistedTState, today: LocalDate): Boolean =
    state.lastRenderedEpochDay != today.toEpochDay()

internal fun stateAfterSuccessfulApply(
    previous: PersistedTState,
    target: LocalDate,
    wallpaperId: Int,
    renderedOn: LocalDate,
): PersistedTState = if (wallpaperId > 0) {
    PersistedTState(
        targetEpochDay = target.toEpochDay(),
        active = true,
        lastWallpaperId = wallpaperId,
        lastRenderedEpochDay = renderedOn.toEpochDay(),
    )
} else {
    previous
}
