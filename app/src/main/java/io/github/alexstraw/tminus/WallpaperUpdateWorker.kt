package io.github.alexstraw.tminus

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.time.LocalDate

class WallpaperUpdateWorker(
    appContext: Context,
    workerParameters: WorkerParameters,
) : Worker(appContext, workerParameters) {
    override fun doWork(): Result {
        val preferences = TPreferences(applicationContext)
        val initialState = preferences.state
        if (!initialState.active) return Result.success()

        val controller = WallpaperController(applicationContext)
        if (!controller.isWallpaperChangeAllowed()) {
            preferences.deactivate()
            return Result.success()
        }

        val currentWallpaperId = controller.currentLockWallpaperId()
        if (automaticUpdateDecision(initialState, currentWallpaperId) == AutomaticUpdateDecision.STOP) {
            preferences.deactivate()
            return Result.success()
        }

        // Re-read after validation so a concurrent Stop action wins before wallpaper I/O begins.
        if (!preferences.state.active) return Result.success()

        val target = LocalDate.ofEpochDay(initialState.targetEpochDay)
        val today = LocalDate.now()
        return when (val applyResult = controller.apply(target, today)) {
            is WallpaperApplyResult.Success -> {
                val latestState = preferences.state
                if (latestState.active) {
                    preferences.state = stateAfterSuccessfulApply(
                        latestState,
                        target,
                        applyResult.wallpaperId,
                        today,
                    )
                    UpdateScheduler.scheduleNext(applicationContext)
                }
                Result.success()
            }

            WallpaperApplyResult.NotAllowed -> {
                preferences.deactivate()
                Result.success()
            }

            is WallpaperApplyResult.Error -> Result.retry()
        }
    }
}
