package com.memo.widget.render

import android.graphics.*
import com.memo.widget.data.GridState
import com.memo.widget.data.Memo
import com.memo.widget.data.RenderResult
import kotlin.math.min

/**
 * Renders GridState to a Bitmap for the widget.
 * This is the core rendering engine that produces the widget's visual representation.
 */
class RenderEngine {

    private val gridLinePaint = Paint().apply {
        color = Color.parseColor("#40000000")
        strokeWidth = 2f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }

    private val backgroundPaint = Paint().apply {
        color = Color.parseColor("#10FFFFFF")
        style = Paint.Style.FILL
    }

    private val textPaint = Paint().apply {
        color = Color.BLACK
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    /**
     * Renders the grid state to a bitmap.
     *
     * @param state The current grid state
     * @param widthPx Target bitmap width in pixels
     * @param heightPx Target bitmap height in pixels
     * @return RenderResult containing the bitmap and metadata
     */
    fun render(state: GridState, widthPx: Int, heightPx: Int): RenderResult {
        val startTime = System.currentTimeMillis()

        // Create bitmap
        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Calculate cell dimensions
        val cellWidth = widthPx.toFloat() / state.columns
        val cellHeight = heightPx.toFloat() / state.rows

        // Draw background
        canvas.drawRect(0f, 0f, widthPx.toFloat(), heightPx.toFloat(), backgroundPaint)

        // Draw grid lines
        for (i in 0..state.columns) {
            val x = i * cellWidth
            canvas.drawLine(x, 0f, x, heightPx.toFloat(), gridLinePaint)
        }
        for (i in 0..state.rows) {
            val y = i * cellHeight
            canvas.drawLine(0f, y, widthPx.toFloat(), y, gridLinePaint)
        }

        // Draw memos
        state.memos.forEach { memo ->
            drawMemo(canvas, memo, cellWidth, cellHeight)
        }

        val renderTime = System.currentTimeMillis() - startTime
        return RenderResult(bitmap, state, renderTime)
    }

    /**
     * Draws a single memo block on the canvas.
     */
    private fun drawMemo(canvas: Canvas, memo: Memo, cellWidth: Float, cellHeight: Float) {
        val left = memo.originX * cellWidth
        val top = memo.originY * cellHeight
        val right = left + (memo.width * cellWidth)
        val bottom = top + (memo.height * cellHeight)

        // Parse memo color
        val blockPaint = Paint().apply {
            color = try {
                Color.parseColor(memo.colorHex)
            } catch (e: Exception) {
                Color.parseColor("#FFE066") // Default yellow
            }
            style = Paint.Style.FILL
        }

        // Apply aging effect
        val agingTier = memo.agingTier()
        val alpha = when (agingTier) {
            0 -> 255
            1 -> (255 * 0.85).toInt()
            2 -> (255 * 0.70).toInt()
            else -> (255 * 0.60).toInt()
        }
        blockPaint.alpha = alpha

        // Draw rounded rectangle with padding
        val padding = 4f
        val rect = RectF(left + padding, top + padding, right - padding, bottom - padding)
        val cornerRadius = 12f
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, blockPaint)

        // Draw border
        val borderPaint = Paint().apply {
            color = Color.parseColor("#80000000")
            strokeWidth = 2f
            style = Paint.Style.STROKE
            isAntiAlias = true
            this.alpha = alpha
        }
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, borderPaint)

        // Draw text
        val textSize = min(cellWidth, cellHeight) * 0.25f
        textPaint.textSize = textSize
        textPaint.alpha = alpha

        val centerX = (left + right) / 2
        val centerY = (top + bottom) / 2

        // Draw title
        val title = memo.title.ifEmpty { "Memo" }
        drawCenteredText(canvas, title, centerX, centerY, rect, textPaint)
    }

    /**
     * Draws text centered within a rectangular area, with ellipsization if too long.
     */
    private fun drawCenteredText(
        canvas: Canvas,
        text: String,
        centerX: Float,
        centerY: Float,
        bounds: RectF,
        paint: Paint
    ) {
        val maxWidth = bounds.width() - 16f // Padding

        // Measure text and truncate if necessary
        var displayText = text
        var textWidth = paint.measureText(displayText)

        if (textWidth > maxWidth) {
            // Truncate with ellipsis
            while (textWidth > maxWidth && displayText.isNotEmpty()) {
                displayText = displayText.dropLast(1)
                textWidth = paint.measureText("$displayText…")
            }
            displayText = "$displayText…"
        }

        // Calculate vertical center
        val metrics = paint.fontMetrics
        val textHeight = metrics.descent - metrics.ascent
        val textY = centerY - (metrics.ascent + metrics.descent) / 2

        canvas.drawText(displayText, centerX, textY, paint)
    }
}
