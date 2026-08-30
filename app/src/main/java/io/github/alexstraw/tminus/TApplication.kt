package io.github.alexstraw.tminus

import android.app.Application
import androidx.work.Configuration

class TApplication : Application(), Configuration.Provider {
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().build()
}
