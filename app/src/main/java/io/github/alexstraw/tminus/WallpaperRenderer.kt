package io.github.alexstraw.tminus

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import kotlin.math.max

class WallpaperRenderer(private val context: Context) {
    fun render(text: String): Bitmap {
        val wallpaperManager = WallpaperManager.getInstance(context)
        val displayMetrics = context.resources.displayMetrics
        val width = max(
            displayMetrics.widthPixels,
            wallpaperManager.desiredMinimumWidth.takeIf { it > 0 } ?: 0,
        )
        val height = max(
            displayMetrics.heightPixels,
            wallpaperManager.desiredMinimumHeight.takeIf { it > 0 } ?: 0,
        )

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawCountdown(canvas, width, height, text)

        return bitmap
    }

    companion object {
        const val TEXT_SIZE_WIDTH_RATIO = 0.03f
        const val TEXT_CENTER_HEIGHT_RATIO = 0.40f

        fun drawCountdown(canvas: Canvas, width: Int, height: Int, text: String) {
            canvas.drawColor(Color.BLACK)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(242, 242, 242)
                textAlign = Paint.Align.CENTER
                textSize = width * TEXT_SIZE_WIDTH_RATIO
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
            }
            val visualCenterY = height * TEXT_CENTER_HEIGHT_RATIO
            val baseline = visualCenterY - (paint.fontMetrics.ascent + paint.fontMetrics.descent) / 2f
            canvas.drawText(text, width / 2f, baseline, paint)
        }
    }
}
