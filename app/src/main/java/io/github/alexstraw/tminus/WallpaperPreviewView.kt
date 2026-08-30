package io.github.alexstraw.tminus

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.View

class WallpaperPreviewView @JvmOverloads constructor(
    context: Context,
    attributes: AttributeSet? = null,
    defaultStyleAttribute: Int = 0,
) : View(context, attributes, defaultStyleAttribute) {
    var countdownText: String = "T"
        set(value) {
            if (field == value) return
            field = value
            invalidate()
        }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        WallpaperRenderer.drawCountdown(canvas, width, height, countdownText)
    }
}
