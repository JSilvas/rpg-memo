package com.memo.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import com.memo.widget.model.TaskBlock
import kotlin.math.min

/**
 * Phase 1: Renders a 4x4 grid with task blocks as a Bitmap for the widget.
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
     * Phase 1: Renders the widget bitmap with task blocks.
     * @param widthPx Widget width in pixels
     * @param heightPx Widget height in pixels
     * @param blocks List of task blocks to render
     */
    fun renderWidgetWithBlocks(
        widthPx: Int,
        heightPx: Int,
        blocks: List<TaskBlock>
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Calculate grid dimensions
        val size = min(widthPx, heightPx)
        val cellSize = (size / GRID_SIZE).toFloat()

        // Draw background
        canvas.drawRect(0f, 0f, widthPx.toFloat(), heightPx.toFloat(), backgroundPaint)

        // Draw empty grid cells
        for (row in 0 until GRID_SIZE) {
            for (col in 0 until GRID_SIZE) {
                val x = col * cellSize + CELL_PADDING
                val y = row * cellSize + CELL_PADDING
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

        // Draw task blocks
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            textSize = 12f * context.resources.displayMetrics.density
            textAlign = Paint.Align.CENTER
        }

        blocks.forEach { block ->
            val blockPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = block.getColor().hex
                style = Paint.Style.FILL
            }

            // Calculate block rectangle
            val x = block.position.x * cellSize + CELL_PADDING
            val y = block.position.y * cellSize + CELL_PADDING
            val width = block.shape.width * cellSize - CELL_PADDING * 2
            val height = block.shape.height * cellSize - CELL_PADDING * 2

            val rect = RectF(x, y, x + width, y + height)

            // Draw block background
            canvas.drawRoundRect(rect, CORNER_RADIUS, CORNER_RADIUS, blockPaint)
            canvas.drawRoundRect(rect, CORNER_RADIUS, CORNER_RADIUS, borderPaint)

            // Draw text
            canvas.drawText(
                block.title,
                x + width / 2,
                y + height / 2 + textPaint.textSize / 3,
                textPaint
            )
        }

        return bitmap
    }
}
