package com.memo.widget.renderer

import android.content.Context
import android.graphics.*
import android.util.Log
import androidx.core.graphics.ColorUtils

/**
 * Renders pulse animation states for widget cells.
 *
 * Since RemoteViews can't do real-time animations, we pre-render
 * "pulsed" and "normal" states and swap bitmaps to create the effect.
 *
 * Pulse effect:
 * - 1.05x scale
 * - Subtle white glow around cell
 * - Slightly increased brightness
 */
class PulseAnimationRenderer {

    /**
     * Creates a bitmap with the specified cell pulsed.
     *
     * @param context Android context
     * @param widthPx Widget width in pixels
     * @param heightPx Widget height in pixels
     * @param rows Number of grid rows
     * @param cols Number of grid columns
     * @param cellColors Map of cell positions to colors (default gray for empty)
     * @param pulsedCell Which cell to render with pulse effect (null for no pulse)
     * @return Rendered bitmap
     */
    fun renderGrid(
        context: Context,
        widthPx: Int,
        heightPx: Int,
        rows: Int,
        cols: Int,
        cellColors: Map<Pair<Int, Int>, Int>,
        pulsedCell: Pair<Int, Int>? = null
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Background
        canvas.drawColor(Color.parseColor("#F5F5F5"))

        val padding = 8f
        val cellSpacing = 4f
        val availableWidth = widthPx - (padding * 2)
        val availableHeight = heightPx - (padding * 2)
        val cellWidth = (availableWidth - (cellSpacing * (cols - 1))) / cols
        val cellHeight = (availableHeight - (cellSpacing * (rows - 1))) / rows

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val cornerRadius = 12f

        for (row in 0 until rows) {
            for (col in 0 until cols) {
                val x = padding + (col * (cellWidth + cellSpacing))
                val y = padding + (row * (cellHeight + cellSpacing))

                val cellPos = Pair(row, col)
                val isPulsed = (cellPos == pulsedCell)

                // Get cell color
                val baseColor = cellColors[cellPos] ?: Color.parseColor("#E0E0E0")

                // Apply pulse effect
                if (isPulsed) {
                    renderPulsedCell(canvas, x, y, cellWidth, cellHeight, baseColor, cornerRadius, paint)
                } else {
                    renderNormalCell(canvas, x, y, cellWidth, cellHeight, baseColor, cornerRadius, paint)
                }
            }
        }

        return bitmap
    }

    private fun renderNormalCell(
        canvas: Canvas,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        color: Int,
        cornerRadius: Float,
        paint: Paint
    ) {
        val rect = RectF(x, y, x + width, y + height)

        // Draw cell
        paint.color = color
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint)

        // Subtle border
        paint.color = Color.parseColor("#10000000")
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint)
    }

    private fun renderPulsedCell(
        canvas: Canvas,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        color: Int,
        cornerRadius: Float,
        paint: Paint
    ) {
        // Pulse scale factor
        val scale = 1.05f
        val scaledWidth = width * scale
        val scaledHeight = height * scale

        // Center the scaled cell
        val offsetX = (scaledWidth - width) / 2
        val offsetY = (scaledHeight - height) / 2

        val rect = RectF(
            x - offsetX,
            y - offsetY,
            x - offsetX + scaledWidth,
            y - offsetY + scaledHeight
        )

        // Draw glow (outer ring)
        paint.color = Color.WHITE
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 6f
        paint.alpha = 100
        canvas.drawRoundRect(rect, cornerRadius * scale, cornerRadius * scale, paint)

        // Brighten the base color
        val brightenedColor = ColorUtils.blendARGB(color, Color.WHITE, 0.2f)

        // Draw cell with brightened color
        paint.color = brightenedColor
        paint.style = Paint.Style.FILL
        paint.alpha = 255
        canvas.drawRoundRect(rect, cornerRadius * scale, cornerRadius * scale, paint)

        // Stronger border for pulsed state
        paint.color = Color.WHITE
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        paint.alpha = 200
        canvas.drawRoundRect(rect, cornerRadius * scale, cornerRadius * scale, paint)
    }

    companion object {
        /**
         * Default cell color palette for validation prototype.
         */
        fun getDefaultCellColors(): Map<Pair<Int, Int>, Int> {
            return mapOf(
                // Yellow memo (2x2)
                Pair(0, 0) to Color.parseColor("#FFE066"),
                Pair(0, 1) to Color.parseColor("#FFE066"),
                Pair(1, 0) to Color.parseColor("#FFE066"),
                Pair(1, 1) to Color.parseColor("#FFE066"),

                // Blue memo (2x1)
                Pair(1, 2) to Color.parseColor("#90CAF9"),
                Pair(2, 2) to Color.parseColor("#90CAF9"),

                // Purple memo (2x1)
                Pair(2, 3) to Color.parseColor("#CE93D8"),
                Pair(3, 3) to Color.parseColor("#CE93D8"),

                // Empty cells default to gray (handled in renderGrid)
            )
        }

        /**
         * Calculates appropriate widget dimensions based on AppWidgetOptions.
         */
        fun getWidgetDimensions(context: Context): Pair<Int, Int> {
            // For validation, use a fixed 320x320dp size
            val density = context.resources.displayMetrics.density
            val widthPx = (320 * density).toInt()
            val heightPx = (320 * density).toInt()
            return Pair(widthPx, heightPx)
        }
    }
}
