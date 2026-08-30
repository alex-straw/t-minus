package io.github.alexstraw.tminus

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

object UpdateScheduler {
    internal fun nextRunAfter(now: ZonedDateTime): ZonedDateTime =
        now.toLocalDate()
            .plusDays(1)
            .atStartOfDay(now.zone)
            .plusMinutes(5)

    fun scheduleNext(
        context: Context,
        now: ZonedDateTime = ZonedDateTime.now(),
        replace: Boolean = false,
    ) {
        val nextRun = nextRunAfter(now)
        val delayMillis = Duration.between(now, nextRun).toMillis().coerceAtLeast(0)
        val request = OneTimeWorkRequestBuilder<WallpaperUpdateWorker>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)
            .addTag(WORK_TAG)
            .build()
        val workName = "$WORK_NAME_PREFIX${nextRun.toLocalDate().toEpochDay()}"
        val policy = if (replace) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP

        WorkManager.getInstance(context).enqueueUniqueWork(workName, policy, request)
    }

    fun reset(context: Context) {
        WorkManager.getInstance(context).cancelAllWorkByTag(WORK_TAG)
        scheduleNext(context, replace = true)
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelAllWorkByTag(WORK_TAG)
    }

    private const val WORK_TAG = "t-wallpaper-update"
    private const val WORK_NAME_PREFIX = "t-wallpaper-update-"
}
