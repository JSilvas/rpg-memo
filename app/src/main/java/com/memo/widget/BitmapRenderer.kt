package com.memo.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import com.memo.widget.model.Memo
import kotlin.math.min

/**
 * Phase 1: Renders a 4x4 grid with memos as a Bitmap for the widget.
 * Aligned with Phase 1 specification.
 */
class BitmapRenderer(private val context: Context) {

    companion object {
        const val GRID_SIZE = 4
        private const val CELL_PADDING = 4f
        private const val CORNER_RADIUS = 8f
        private const val STROKE_WIDTH = 2f

        // Phase 0 color palette (retro RPG inspired)
        private const val BG_COLOR = 0xFF1A1A2E.toInt()
        private const val GRID_COLOR = 0xFF16213E.toInt()
        private const val BORDER_COLOR = 0xFF0F3460.toInt()
        private const val FILL_COLOR = 0xFFE94560.toInt()
    }

    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = BG_COLOR
        style = Paint.Style.FILL
    }

    private val cellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = GRID_COLOR
        style = Paint.Style.FILL
    }

    private val cellFilledPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = FILL_COLOR
        style = Paint.Style.FILL
    }

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = BORDER_COLOR
        style = Paint.Style.STROKE
        strokeWidth = STROKE_WIDTH
    }

    /**
     * Helper function to draw wrapped text within bounds.
     * Splits text into lines that fit within the given width and centers them vertically.
     */
    private fun drawWrappedText(
        canvas: Canvas,
        text: String,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        paint: Paint,
        padding: Float
    ) {
        if (text.isBlank()) return

        val availableWidth = width - padding * 2
        val availableHeight = height - padding * 2

        // Split text into words
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = ""

        // Build lines that fit within available width
        words.forEach { word ->
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            val testWidth = paint.measureText(testLine)

            if (testWidth <= availableWidth) {
                currentLine = testLine
            } else {
                if (currentLine.isNotEmpty()) {
                    lines.add(currentLine)
                }
                currentLine = word
            }
        }
        if (currentLine.isNotEmpty()) {
            lines.add(currentLine)
        }

        // Calculate total text height
        val lineHeight = paint.textSize * 1.2f
        val totalTextHeight = lines.size * lineHeight

        // Calculate starting Y position (vertically centered)
        val startY = y + padding + (availableHeight - totalTextHeight) / 2 + paint.textSize

        // Draw each line
        lines.forEachIndexed { index, line ->
            val lineY = startY + index * lineHeight
            if (lineY < y + height - padding) { // Only draw if within bounds
                canvas.drawText(
                    line,
                    x + width / 2,
                    lineY,
                    paint
                )
            }
        }
    }

    /**
     * Renders the widget bitmap.
     * @param widthPx Widget width in pixels
     * @param heightPx Widget height in pixels
     * @param filledCells Set of cell indices (0-15) that should be filled
     */
    fun renderWidget(
        widthPx: Int,
        heightPx: Int,
        filledCells: Set<Int> = emptySet()
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Calculate grid dimensions
        val size = min(widthPx, heightPx)
        val cellSize = (size / GRID_SIZE).toFloat()

        // Draw background
        canvas.drawRect(0f, 0f, widthPx.toFloat(), heightPx.toFloat(), backgroundPaint)

        // Draw grid cells
        for (row in 0 until GRID_SIZE) {
            for (col in 0 until GRID_SIZE) {
                val cellIndex = row * GRID_SIZE + col
                val x = col * cellSize + CELL_PADDING
                val y = row * cellSize + CELL_PADDING
                val rect = RectF(
                    x,
                    y,
                    x + cellSize - CELL_PADDING * 2,
                    y + cellSize - CELL_PADDING * 2
                )

                // Fill cell if it's in the filled set
                val paint = if (cellIndex in filledCells) cellFilledPaint else cellPaint
                canvas.drawRoundRect(rect, CORNER_RADIUS, CORNER_RADIUS, paint)
                canvas.drawRoundRect(rect, CORNER_RADIUS, CORNER_RADIUS, borderPaint)
            }
        }

        return bitmap
    }

    /**
     * Phase 1: Renders the widget bitmap with memos.
     * @param widthPx Widget width in pixels
     * @param heightPx Widget height in pixels
     * @param memos List of memos to render
     */
    fun renderWidgetWithMemos(
        widthPx: Int,
        heightPx: Int,
        memos: List<Memo>
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Calculate grid dimensions with padding to match overlay
        // Overlay uses 8dp padding converted to px
        val paddingPx = 8f * context.resources.displayMetrics.density
        val size = min(widthPx, heightPx)
        val drawableSize = size - (paddingPx * 2)
        val cellSize = (drawableSize / GRID_SIZE).toFloat()

        // Draw background
        canvas.drawRect(0f, 0f, widthPx.toFloat(), heightPx.toFloat(), backgroundPaint)

        // Draw empty grid cells (offset by padding to match overlay)
        for (row in 0 until GRID_SIZE) {
            for (col in 0 until GRID_SIZE) {
                val x = paddingPx + col * cellSize + CELL_PADDING
                val y = paddingPx + row * cellSize + CELL_PADDING
                val rect = RectF(
                    x,
                    y,
                    x + cellSize - CELL_PADDING * 2,
                    y + cellSize - CELL_PADDING * 2
                )
                canvas.drawRoundRect(rect, CORNER_RADIUS, CORNER_RADIUS, cellPaint)
                canvas.drawRoundRect(rect, CORNER_RADIUS, CORNER_RADIUS, borderPaint)
            }
        }

        // Draw memos
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            textSize = 12f * context.resources.displayMetrics.density
            textAlign = Paint.Align.CENTER
        }

        memos.forEach { memo ->
            val memoPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.parseColor(memo.colorHex)
                style = Paint.Style.FILL
            }

            // Calculate memo rectangle (offset by padding to match overlay)
            val x = paddingPx + memo.originX * cellSize + CELL_PADDING
            val y = paddingPx + memo.originY * cellSize + CELL_PADDING
            val width = memo.width * cellSize - CELL_PADDING * 2
            val height = memo.height * cellSize - CELL_PADDING * 2

            val rect = RectF(x, y, x + width, y + height)

            // Draw memo background
            canvas.drawRoundRect(rect, CORNER_RADIUS, CORNER_RADIUS, memoPaint)
            canvas.drawRoundRect(rect, CORNER_RADIUS, CORNER_RADIUS, borderPaint)

            // Draw text (wrapped to fit within memo bounds)
            drawWrappedText(
                canvas = canvas,
                text = memo.title,
                x = x,
                y = y,
                width = width,
                height = height,
                paint = textPaint,
                padding = 8f * context.resources.displayMetrics.density
            )
        }

        return bitmap
    }
}
