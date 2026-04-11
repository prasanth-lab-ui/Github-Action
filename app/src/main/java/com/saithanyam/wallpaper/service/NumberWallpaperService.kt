package com.saithanyam.wallpaper.service

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import com.saithanyam.wallpaper.data.BirthdayRepository
import com.saithanyam.wallpaper.util.WeekCalculator

/**
 * Minimalist "Number Style" live wallpaper.
 *
 * Renders the remaining-weeks count as a single, large, white number centered
 * on a pure black background. No labels, no effects, no shadows.
 *
 * Font size is scaled dynamically to roughly 25% of the surface width so the
 * number feels dominant but never cramped — recalculated in onSurfaceChanged().
 */
class NumberWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine = NumberEngine()

    inner class NumberEngine : Engine() {

        private val handler = Handler(Looper.getMainLooper())
        private val drawRunnable = Runnable { drawFrame() }

        private var visible = false
        private var surfaceWidth = 0
        private var surfaceHeight = 0

        // Paint recalculated on surface change
        private val numberPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }

        override fun onVisibilityChanged(isVisible: Boolean) {
            visible = isVisible
            if (visible) {
                drawFrame()
            } else {
                handler.removeCallbacks(drawRunnable)
            }
        }

        override fun onSurfaceChanged(
            holder: SurfaceHolder,
            format: Int,
            width: Int,
            height: Int
        ) {
            super.onSurfaceChanged(holder, format, width, height)
            surfaceWidth = width
            surfaceHeight = height
            recalculateFont()
            drawFrame()
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            super.onSurfaceDestroyed(holder)
            visible = false
            handler.removeCallbacks(drawRunnable)
        }

        /**
         * Scale font to ~25% of surface width. Dynamic — works on any screen.
         */
        private fun recalculateFont() {
            if (surfaceWidth == 0) return
            numberPaint.textSize = surfaceWidth * 0.25f
        }

        private fun drawFrame() {
            if (!visible) return

            val holder = surfaceHolder
            var canvas: Canvas? = null
            try {
                canvas = holder.lockCanvas()
                if (canvas != null) {
                    drawNumber(canvas)
                }
            } finally {
                if (canvas != null) {
                    try {
                        holder.unlockCanvasAndPost(canvas)
                    } catch (_: IllegalArgumentException) {
                        // Surface already released
                    }
                }
            }

            // Redraw once per hour — week number changes at most once per week
            handler.removeCallbacks(drawRunnable)
            if (visible) {
                handler.postDelayed(drawRunnable, 60 * 60 * 1000L)
            }
        }

        private fun drawNumber(canvas: Canvas) {
            canvas.drawColor(Color.BLACK)

            val repository = BirthdayRepository(this@NumberWallpaperService)
            val birthday = repository.getBirthday() ?: return

            val remaining = WeekCalculator.weeksRemaining(birthday)
            val text = remaining.toString()

            // Centre the text both horizontally and vertically
            val cx = surfaceWidth / 2f
            val cy = surfaceHeight / 2f - (numberPaint.descent() + numberPaint.ascent()) / 2f

            canvas.drawText(text, cx, cy, numberPaint)
        }
    }
}
