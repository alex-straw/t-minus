package io.github.alexstraw.tminus

import android.annotation.SuppressLint
import android.content.Context
import java.time.LocalDate

data class PersistedTState(
    val targetEpochDay: Long,
    val active: Boolean,
    val lastWallpaperId: Int,
    val lastRenderedEpochDay: Long,
)

@SuppressLint("ApplySharedPref")
class TPreferences(context: Context) {
    private val preferences = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    var state: PersistedTState
        get() = PersistedTState(
            targetEpochDay = preferences.getLong(KEY_TARGET_EPOCH_DAY, DEFAULT_TARGET.toEpochDay()),
            active = preferences.getBoolean(KEY_ACTIVE, false),
            lastWallpaperId = preferences.getInt(KEY_LAST_WALLPAPER_ID, NO_WALLPAPER_ID),
            lastRenderedEpochDay = preferences.getLong(KEY_LAST_RENDERED_EPOCH_DAY, NEVER_RENDERED),
        )
        set(value) {
            preferences.edit()
                .putLong(KEY_TARGET_EPOCH_DAY, value.targetEpochDay)
                .putBoolean(KEY_ACTIVE, value.active)
                .putInt(KEY_LAST_WALLPAPER_ID, value.lastWallpaperId)
                .putLong(KEY_LAST_RENDERED_EPOCH_DAY, value.lastRenderedEpochDay)
                .commit()
        }

    fun deactivate() {
        val current = state
        state = current.copy(active = false, lastWallpaperId = NO_WALLPAPER_ID)
    }

    companion object {
        val DEFAULT_TARGET: LocalDate = LocalDate.of(2027, 8, 8)
        const val NO_WALLPAPER_ID = -1
        const val NEVER_RENDERED = Long.MIN_VALUE

        private const val FILE_NAME = "t_preferences"
        private const val KEY_TARGET_EPOCH_DAY = "targetEpochDay"
        private const val KEY_ACTIVE = "active"
        private const val KEY_LAST_WALLPAPER_ID = "lastWallpaperId"
        private const val KEY_LAST_RENDERED_EPOCH_DAY = "lastRenderedEpochDay"
    }
}
