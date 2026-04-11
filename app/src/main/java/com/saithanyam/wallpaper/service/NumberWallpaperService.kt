package com.saithanyam.wallpaper.service

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.view.SurfaceHolder
import com.saithanyam.wallpaper.data.BirthdayRepository
import com.saithanyam.wallpaper.util.WeekCalculator

/**
 * Minimalist "Number Style" live wallpaper.
 *
 * Layout (all dimensions derived from the surface — no hardcoded pixels):
 *
 *   ┌──────────────────────────────┐
 *   │ ←12%→                  ←12%→ │
 *   │                              │
 *   │        2173 Weeks to End     │  ← main line, 3 paint segments
 *   │          (last digit red)    │
 *   │                              │
 *   │        1827 weeks gone       │  ← grey subtext, ~50% size
 *   │             (45.7%)          │
 *   │                              │
 *   └──────────────────────────────┘
 *
 * Font sizing:
 *   - Base "words" size  = 14% of surface width
 *   - Number segments    = 125% of base (slightly larger, bold)
 *   - Subtext (weeks gone) = 50% of number size
 *
 * The three main-line segments are drawn manually on a shared baseline so that
 * the last digit can be coloured red while the rest stays white. The subtext
 * line uses StaticLayout for centered alignment within the 12%-padded area.
 */
class NumberWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine = NumberEngine()

    inner class NumberEngine : Engine() {

        private val handler = Handler(Looper.getMainLooper())
        private val drawRunnable = Runnable { drawFrame() }

        private var visible = false
        private var surfaceWidth = 0
        private var surfaceHeight = 0

        // ---------- Layout metrics — recalculated in onSurfaceChanged ----------
        private var horizontalPadding = 0f
        private var maxTextWidth = 0f
        private var verticalGap = 0f

        // Base font sizes — stored so we can reset after per-frame overflow scaling
        private var baseWordSize = 0f
        private var baseNumberSize = 0f
        private var baseSubSize = 0f

        // ---------- Paints ----------
        private val whiteNumberPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.LEFT
        }

        private val redNumberPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FF3B30") // bright red
            style = Paint.Style.FILL
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.LEFT
        }

        private val wordPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            textAlign = Paint.Align.LEFT
        }

        private val subtextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#888888") // grey
            style = Paint.Style.FILL
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }

        override fun onVisibilityChanged(isVisible: Boolean) {
            visible = isVisible
            if (visible) drawFrame() else handler.removeCallbacks(drawRunnable)
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
            recalculatePositions()
            drawFrame()
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            super.onSurfaceDestroyed(holder)
            visible = false
            handler.removeCallbacks(drawRunnable)
        }

        /**
         * All layout metrics derived from surface dimensions.
         * Called on every surface change (rotation, size change, etc.).
         */
        private fun recalculatePositions() {
            if (surfaceWidth == 0 || surfaceHeight == 0) return

            // Padding — 12% of width on each side
            horizontalPadding = surfaceWidth * 0.12f
            maxTextWidth = surfaceWidth - 2f * horizontalPadding

            // Vertical gap between main text and subtext
            verticalGap = surfaceHeight * 0.04f

            // Font sizes — all relative to surface width
            baseWordSize = surfaceWidth * 0.14f           // ~14% of width
            baseNumberSize = baseWordSize * 1.25f         // 125% of base
            baseSubSize = baseNumberSize * 0.5f           // 50% of number size

            applyBaseSizes()
        }

        private fun applyBaseSizes() {
            wordPaint.textSize = baseWordSize
            whiteNumberPaint.textSize = baseNumberSize
            redNumberPaint.textSize = baseNumberSize
            subtextPaint.textSize = baseSubSize
        }

        private fun drawFrame() {
            if (!visible) return

            val holder = surfaceHolder
            var canvas: Canvas? = null
            try {
                canvas = holder.lockCanvas()
                if (canvas != null) drawContents(canvas)
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
            if (visible) handler.postDelayed(drawRunnable, 60 * 60 * 1000L)
        }

        private fun drawContents(canvas: Canvas) {
            canvas.drawColor(Color.BLACK)

            val repository = BirthdayRepository(this@NumberWallpaperService)
            val birthday = repository.getBirthday() ?: return

            val remaining = WeekCalculator.weeksRemaining(birthday)
            val lived = WeekCalculator.weeksLived(birthday)
            val pct = lived.toFloat() / WeekCalculator.TOTAL_WEEKS.toFloat() * 100f

            // ---------- Split main-line text into three paint segments ----------
            val remainingStr = remaining.toString()
            val otherDigits = if (remainingStr.length > 1) remainingStr.dropLast(1) else ""
            val lastDigit = remainingStr.takeLast(1)
            val wordsText = " Weeks to End"

            // Reset paint sizes in case previous frame scaled them down
            applyBaseSizes()

            // Measure each segment
            var otherW = whiteNumberPaint.measureText(otherDigits)
            var lastW = redNumberPaint.measureText(lastDigit)
            var wordsW = wordPaint.measureText(wordsText)
            var totalW = otherW + lastW + wordsW

            // If the group overflows the 12%-padded area, scale all main-line paints
            // (and the subtext proportionally) down until it fits. This preserves the
            // relative sizing between segments.
            if (totalW > maxTextWidth && totalW > 0f) {
                val scale = maxTextWidth / totalW
                whiteNumberPaint.textSize = baseNumberSize * scale
                redNumberPaint.textSize = baseNumberSize * scale
                wordPaint.textSize = baseWordSize * scale
                subtextPaint.textSize = baseSubSize * scale

                otherW = whiteNumberPaint.measureText(otherDigits)
                lastW = redNumberPaint.measureText(lastDigit)
                wordsW = wordPaint.measureText(wordsText)
                totalW = otherW + lastW + wordsW
            }

            // Calculate starting X so the whole group is centered on screen
            val startX = (surfaceWidth - totalW) / 2f

            // Baseline for the main line — tallest paint (whiteNumberPaint) drives vertical centring
            val mainBaselineY =
                surfaceHeight / 2f - (whiteNumberPaint.descent() + whiteNumberPaint.ascent()) / 2f

            // ---------- Draw the three main-line segments sequentially ----------
            var x = startX
            if (otherDigits.isNotEmpty()) {
                canvas.drawText(otherDigits, x, mainBaselineY, whiteNumberPaint)
                x += otherW
            }
            canvas.drawText(lastDigit, x, mainBaselineY, redNumberPaint)
            x += lastW
            canvas.drawText(wordsText, x, mainBaselineY, wordPaint)

            // ---------- Draw "X weeks gone (Y%)" subtext ----------
            val subText = String.format("%d weeks gone (%.1f%%)", lived, pct)
            drawSubtext(canvas, subText, mainBaselineY)
        }

        /**
         * Renders the grey "weeks gone" line using StaticLayout so that alignment
         * and wrapping (if the line is ever long enough to wrap) are handled for us.
         */
        private fun drawSubtext(canvas: Canvas, subText: String, mainBaselineY: Float) {
            val layoutWidth = maxTextWidth.toInt().coerceAtLeast(1)

            val layout: StaticLayout = StaticLayout.Builder
                .obtain(subText, 0, subText.length, subtextPaint, layoutWidth)
                .setAlignment(Layout.Alignment.ALIGN_CENTER)
                .setLineSpacing(0f, 1f)
                .setIncludePad(false)
                .build()

            // Y position: just below the main line's descent, then add the vertical gap
            val topY = mainBaselineY + whiteNumberPaint.descent() + verticalGap

            canvas.save()
            canvas.translate(horizontalPadding, topY)
            layout.draw(canvas)
            canvas.restore()
        }
    }
}
