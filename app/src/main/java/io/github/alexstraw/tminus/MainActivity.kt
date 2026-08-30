package io.github.alexstraw.tminus

import android.app.Activity
import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.Executors

class MainActivity : Activity() {
    private lateinit var preferences: TPreferences
    private lateinit var targetDateView: TextView
    private lateinit var changeDateButton: Button
    private lateinit var previewView: WallpaperPreviewView
    private lateinit var progressView: ProgressBar
    private lateinit var applyButton: Button
    private lateinit var stopButton: Button

    private val executor = Executors.newSingleThreadExecutor()
    private var draftTarget: LocalDate = TPreferences.DEFAULT_TARGET
    private var busy = false
    private var automaticRefreshRunning = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        applySystemBarInsets()

        preferences = TPreferences(this)
        draftTarget = savedInstanceState
            ?.takeIf { it.containsKey(STATE_DRAFT_TARGET) }
            ?.getLong(STATE_DRAFT_TARGET)
            ?.let(LocalDate::ofEpochDay)
            ?: LocalDate.ofEpochDay(preferences.state.targetEpochDay)

        targetDateView = findViewById(R.id.target_date)
        changeDateButton = findViewById(R.id.change_date)
        previewView = findViewById(R.id.wallpaper_preview)
        progressView = findViewById(R.id.progress)
        applyButton = findViewById(R.id.apply)
        stopButton = findViewById(R.id.stop)

        changeDateButton.setOnClickListener { showDatePicker() }
        applyButton.setOnClickListener { applyDraftTarget() }
        stopButton.setOnClickListener { stopUpdating() }

        renderState()
    }

    override fun onResume() {
        super.onResume()
        if (::preferences.isInitialized && preferences.state.active && !automaticRefreshRunning) {
            refreshActiveWallpaper()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putLong(STATE_DRAFT_TARGET, draftTarget.toEpochDay())
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        executor.shutdownNow()
        super.onDestroy()
    }

    private fun applySystemBarInsets() {
        val root = findViewById<View>(R.id.root)
        root.setOnApplyWindowInsetsListener { view, insets ->
            @Suppress("DEPRECATION")
            view.setPadding(
                0,
                insets.systemWindowInsetTop,
                0,
                insets.systemWindowInsetBottom,
            )
            insets
        }
        root.requestApplyInsets()
    }

    private fun showDatePicker() {
        DatePickerDialog(
            this,
            { _, year, zeroBasedMonth, dayOfMonth ->
                draftTarget = LocalDate.of(year, zeroBasedMonth + 1, dayOfMonth)
                renderState()
            },
            draftTarget.year,
            draftTarget.monthValue - 1,
            draftTarget.dayOfMonth,
        ).show()
    }

    private fun renderState() {
        val state = preferences.state
        val draftChanged = draftTarget.toEpochDay() != state.targetEpochDay

        targetDateView.text = DATE_FORMATTER.format(draftTarget)
        previewView.countdownText = Countdown.format(LocalDate.now(), draftTarget)
        changeDateButton.setText(if (state.active) R.string.change_date else R.string.change)
        applyButton.visibility = if (!state.active || draftChanged) View.VISIBLE else View.GONE
        applyButton.setText(if (state.active) R.string.update_lock_screen else R.string.set_lock_screen)
        stopButton.visibility = if (state.active) View.VISIBLE else View.GONE

        changeDateButton.isEnabled = !busy
        applyButton.isEnabled = !busy
        stopButton.isEnabled = !busy
        progressView.visibility = if (busy) View.VISIBLE else View.GONE
    }

    private fun applyDraftTarget() {
        val target = draftTarget
        setBusy(true)
        executor.execute {
            val previousState = preferences.state
            val today = LocalDate.now()
            val result = WallpaperController(applicationContext).apply(target, today)
            if (result is WallpaperApplyResult.Success) {
                preferences.state = stateAfterSuccessfulApply(
                    previousState,
                    target,
                    result.wallpaperId,
                    today,
                )
                UpdateScheduler.reset(applicationContext)
            }

            runOnUiThread {
                if (isDestroyed) return@runOnUiThread
                setBusy(false)
                when (result) {
                    is WallpaperApplyResult.Success -> renderState()
                    WallpaperApplyResult.NotAllowed -> showMessage(R.string.wallpaper_not_allowed)
                    is WallpaperApplyResult.Error -> showMessage(R.string.update_failed)
                }
            }
        }
    }

    private fun stopUpdating() {
        preferences.deactivate()
        UpdateScheduler.cancel(applicationContext)
        draftTarget = LocalDate.ofEpochDay(preferences.state.targetEpochDay)
        renderState()
    }

    private fun refreshActiveWallpaper() {
        automaticRefreshRunning = true
        setBusy(true)
        executor.execute {
            val state = preferences.state
            val controller = WallpaperController(applicationContext)
            val today = LocalDate.now()
            val outcome = when {
                !state.active -> RefreshOutcome.NO_CHANGE
                !controller.isWallpaperChangeAllowed() -> {
                    preferences.deactivate()
                    UpdateScheduler.cancel(applicationContext)
                    RefreshOutcome.NOT_ALLOWED
                }
                automaticUpdateDecision(state, controller.currentLockWallpaperId()) ==
                    AutomaticUpdateDecision.STOP -> {
                    preferences.deactivate()
                    UpdateScheduler.cancel(applicationContext)
                    RefreshOutcome.WALLPAPER_CHANGED
                }
                !needsCalendarRefresh(state, today) -> {
                    RefreshOutcome.NO_CHANGE
                }
                else -> refreshMatchingWallpaper(state, controller, today)
            }

            runOnUiThread {
                if (isDestroyed) return@runOnUiThread
                automaticRefreshRunning = false
                setBusy(false)
                draftTarget = LocalDate.ofEpochDay(preferences.state.targetEpochDay)
                renderState()
                when (outcome) {
                    RefreshOutcome.NOT_ALLOWED -> showMessage(R.string.wallpaper_not_allowed)
                    RefreshOutcome.ERROR -> showMessage(R.string.update_failed)
                    RefreshOutcome.WALLPAPER_CHANGED -> showMessage(R.string.wallpaper_changed)
                    RefreshOutcome.NO_CHANGE,
                    RefreshOutcome.UPDATED
                    -> Unit
                }
            }
        }
    }

    private fun refreshMatchingWallpaper(
        state: PersistedTState,
        controller: WallpaperController,
        today: LocalDate,
    ): RefreshOutcome {
        if (!preferences.state.active) return RefreshOutcome.NO_CHANGE
        val target = LocalDate.ofEpochDay(state.targetEpochDay)
        return when (val result = controller.apply(target, today)) {
            is WallpaperApplyResult.Success -> {
                val latestState = preferences.state
                if (latestState.active) {
                    preferences.state = stateAfterSuccessfulApply(
                        latestState,
                        target,
                        result.wallpaperId,
                        today,
                    )
                    UpdateScheduler.reset(applicationContext)
                }
                RefreshOutcome.UPDATED
            }
            WallpaperApplyResult.NotAllowed -> {
                preferences.deactivate()
                UpdateScheduler.cancel(applicationContext)
                RefreshOutcome.NOT_ALLOWED
            }
            is WallpaperApplyResult.Error -> RefreshOutcome.ERROR
        }
    }

    private fun setBusy(value: Boolean) {
        busy = value
        renderState()
    }

    private fun showMessage(messageResource: Int) {
        Toast.makeText(this, messageResource, Toast.LENGTH_LONG).show()
    }

    private enum class RefreshOutcome {
        NO_CHANGE,
        UPDATED,
        NOT_ALLOWED,
        WALLPAPER_CHANGED,
        ERROR,
    }

    companion object {
        private const val STATE_DRAFT_TARGET = "draftTargetEpochDay"
        private val DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMMM uuuu", Locale.ENGLISH)
    }
}
