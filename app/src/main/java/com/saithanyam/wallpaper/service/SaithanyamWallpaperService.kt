package com.saithanyam.wallpaper.service

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import com.saithanyam.wallpaper.data.BirthdayRepository
import com.saithanyam.wallpaper.util.WeekCalculator
import kotlin.math.floor
import kotlin.math.sqrt

class SaithanyamWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine = SaithanyamEngine()

    inner class SaithanyamEngine : Engine() {

        private val handler = Handler(Looper.getMainLooper())
        private val drawRunnable = Runnable { drawFrame() }

        private var visible = false
        private var surfaceWidth = 0
        private var surfaceHeight = 0

        // Grid layout — recalculated on surface change
        private var columns = 0
        private var rows = 0
        private var dotRadius = 0f
        private var spacingX = 0f
        private var spacingY = 0f
        private var offsetX = 0f
        private var offsetY = 0f

        private val pastPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#666666") // grey
            style = Paint.Style.FILL
        }

        private val futurePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }

        private val currentFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }

        private val currentGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#66FFFFFF") // semi-transparent white
            style = Paint.Style.STROKE
            strokeWidth = 2f
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
            recalculateGrid()
            drawFrame()
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            super.onSurfaceDestroyed(holder)
            visible = false
            handler.removeCallbacks(drawRunnable)
        }

        /**
         * Calculate optimal grid dimensions so 4,000 dots fill the screen.
         * Columns and rows are chosen to approximate the screen aspect ratio.
         */
        private fun recalculateGrid() {
            if (surfaceWidth == 0 || surfaceHeight == 0) return

            val totalDots = WeekCalculator.TOTAL_WEEKS
            val aspectRatio = surfaceWidth.toFloat() / surfaceHeight.toFloat()

            // cols / rows ≈ aspectRatio, cols * rows >= totalDots
            columns = (sqrt(totalDots.toFloat() * aspectRatio)).toInt().coerceAtLeast(1)
            rows = (totalDots + columns - 1) / columns // ceil division

            // Padding: 5% on each side
            val paddingFractionX = 0.05f
            val paddingFractionY = 0.03f
            val usableWidth = surfaceWidth * (1f - 2 * paddingFractionX)
            val usableHeight = surfaceHeight * (1f - 2 * paddingFractionY)

            spacingX = usableWidth / columns
            spacingY = usableHeight / rows

            // Dot radius: fraction of the smaller spacing, so dots don't overlap
            dotRadius = minOf(spacingX, spacingY) * 0.3f

            // Offsets to centre the grid
            offsetX = surfaceWidth * paddingFractionX + spacingX / 2f
            offsetY = surfaceHeight * paddingFractionY + spacingY / 2f

            // Update glow stroke relative to dot size
            currentGlowPaint.strokeWidth = dotRadius * 0.5f
        }

        private fun drawFrame() {
            if (!visible) return

            val holder = surfaceHolder
            var canvas: Canvas? = null
            try {
                canvas = holder.lockCanvas()
                if (canvas != null) {
                    drawGrid(canvas)
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

            // Redraw once per hour — the wallpaper changes at most once a week
            handler.removeCallbacks(drawRunnable)
            if (visible) {
                handler.postDelayed(drawRunnable, 60 * 60 * 1000L)
            }
        }

        private fun drawGrid(canvas: Canvas) {
            canvas.drawColor(Color.BLACK)

            val repository = BirthdayRepository(this@SaithanyamWallpaperService)
            val birthday = repository.getBirthday() ?: return

            val weeksLived = WeekCalculator.weeksLived(birthday)
            val totalDots = WeekCalculator.TOTAL_WEEKS

            for (i in 0 until totalDots) {
                val col = i % columns
                val row = i / columns
                if (row >= rows) break

                val cx = offsetX + col * spacingX
                val cy = offsetY + row * spacingY

                when {
                    i < weeksLived -> {
                        // Past weeks — grey
                        canvas.drawCircle(cx, cy, dotRadius, pastPaint)
                    }
                    i == weeksLived -> {
                        // Current week — white with glow ring
                        canvas.drawCircle(cx, cy, dotRadius, currentFillPaint)
                        canvas.drawCircle(cx, cy, dotRadius * 1.8f, currentGlowPaint)
                    }
                    else -> {
                        // Remaining weeks — white
                        canvas.drawCircle(cx, cy, dotRadius, futurePaint)
                    }
                }
            }
        }
    }
}
