package com.memo.widget.ui

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.memo.widget.data.GridState
import com.memo.widget.data.Memo
import kotlin.math.min

/**
 * Custom view that renders the interactive grid overlay.
 * This view is positioned and sized to perfectly match the underlying widget.
 *
 * KEY FIX #1: Widget Alignment
 * - The view calculates widget bounds from AppWidgetManager to match exact position
 * - Grid overlay is drawn at these exact coordinates, not centered on screen
 */
class GridOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var gridState: GridState? = null
    private var widgetBounds: RectF? = null

    // Interaction state
    private var selectedMemo: Memo? = null
    private var dragStartX = 0f
    private var dragStartY = 0f
    private var isDragging = false
    private var previewPosition: Pair<Int, Int>? = null

    // Callbacks
    var onMemoMoved: ((Memo, Int, Int) -> Unit)? = null
    var onCellTapped: ((Int, Int) -> Unit)? = null
    var onOutsideTapped: (() -> Unit)? = null
    var onHapticFeedback: ((HapticType) -> Unit)? = null

    enum class HapticType {
        LIGHT, MEDIUM, SUCCESS, ERROR
    }

    // Paint objects
    private val gridLinePaint = Paint().apply {
        color = Color.parseColor("#80FFFFFF")
        strokeWidth = 3f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }

    private val overlayBackgroundPaint = Paint().apply {
        color = Color.parseColor("#20000000")
        style = Paint.Style.FILL
    }

    private val blockPaint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val blockBorderPaint = Paint().apply {
        color = Color.parseColor("#FFFFFF")
        strokeWidth = 4f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }

    private val previewPaint = Paint().apply {
        color = Color.parseColor("#8000FF00")
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val invalidPreviewPaint = Paint().apply {
        color = Color.parseColor("#80FF0000")
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val textPaint = Paint().apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        setShadowLayer(4f, 0f, 2f, Color.BLACK)
    }

    /**
     * KEY FIX #1: Sets the widget bounds to ensure perfect alignment.
     * This is called from the activity after determining widget position.
     */
    fun setWidgetBounds(bounds: RectF) {
        this.widgetBounds = bounds
        invalidate()
    }

    fun setGridState(state: GridState) {
        this.gridState = state
        invalidate()
    }

    fun getGridState(): GridState? = gridState

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val state = gridState ?: return
        val bounds = widgetBounds ?: return

        // Draw semi-transparent overlay over entire screen
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), overlayBackgroundPaint)

        // Calculate cell dimensions based on widget bounds
        val cellWidth = bounds.width() / state.columns
        val cellHeight = bounds.height() / state.rows

        // Draw grid lines within widget bounds
        for (i in 0..state.columns) {
            val x = bounds.left + (i * cellWidth)
            canvas.drawLine(x, bounds.top, x, bounds.bottom, gridLinePaint)
        }
        for (i in 0..state.rows) {
            val y = bounds.top + (i * cellHeight)
            canvas.drawLine(bounds.left, y, bounds.right, y, gridLinePaint)
        }

        // Draw preview position if dragging
        if (isDragging && previewPosition != null && selectedMemo != null) {
            drawPreview(canvas, selectedMemo!!, previewPosition!!.first, previewPosition!!.second,
                        cellWidth, cellHeight, bounds, state)
        }

        // Draw all memos
        state.memos.forEach { memo ->
            // Don't draw the selected memo at its original position while dragging
            if (isDragging && memo.id == selectedMemo?.id) {
                // Skip - will be drawn at drag position
            } else {
                drawMemoBlock(canvas, memo, cellWidth, cellHeight, bounds, selected = false)
            }
        }

        // Draw dragging memo at finger position
        if (isDragging && selectedMemo != null) {
            drawDraggingMemo(canvas, selectedMemo!!, dragStartX, dragStartY, cellWidth, cellHeight)
        }
    }

    /**
     * Draws a memo block at its grid position.
     */
    private fun drawMemoBlock(
        canvas: Canvas,
        memo: Memo,
        cellWidth: Float,
        cellHeight: Float,
        bounds: RectF,
        selected: Boolean
    ) {
        val left = bounds.left + (memo.originX * cellWidth)
        val top = bounds.top + (memo.originY * cellHeight)
        val right = left + (memo.width * cellWidth)
        val bottom = top + (memo.height * cellHeight)

        // Parse color
        blockPaint.color = try {
            Color.parseColor(memo.colorHex)
        } catch (e: Exception) {
            Color.parseColor("#FFE066")
        }

        // Draw block
        val padding = 6f
        val rect = RectF(left + padding, top + padding, right - padding, bottom - padding)
        val cornerRadius = 16f
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, blockPaint)

        // Draw border (thicker if selected)
        if (selected) {
            blockBorderPaint.strokeWidth = 6f
        } else {
            blockBorderPaint.strokeWidth = 3f
        }
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, blockBorderPaint)

        // Draw text
        val textSize = min(cellWidth, cellHeight) * 0.3f
        textPaint.textSize = textSize

        val centerX = (left + right) / 2
        val centerY = (top + bottom) / 2

        val title = memo.title.ifEmpty { "Memo" }
        drawCenteredText(canvas, title, centerX, centerY, textPaint)
    }

    /**
     * Draws the memo being dragged at the current finger position.
     */
    private fun drawDraggingMemo(
        canvas: Canvas,
        memo: Memo,
        x: Float,
        y: Float,
        cellWidth: Float,
        cellHeight: Float
    ) {
        val bounds = widgetBounds ?: return

        // KEY FIX #2: Calculate dragging position from center of block
        val memoWidthPx = memo.width * cellWidth
        val memoHeightPx = memo.height * cellHeight

        val left = x - (memoWidthPx / 2)
        val top = y - (memoHeightPx / 2)
        val right = left + memoWidthPx
        val bottom = top + memoHeightPx

        // Parse color
        blockPaint.color = try {
            Color.parseColor(memo.colorHex)
        } catch (e: Exception) {
            Color.parseColor("#FFE066")
        }
        blockPaint.alpha = 200  // Slightly transparent while dragging

        // Draw block with elevation effect
        val padding = 4f
        val rect = RectF(left + padding, top + padding, right - padding, bottom - padding)
        val cornerRadius = 16f

        // Draw shadow
        val shadowPaint = Paint().apply {
            color = Color.BLACK
            alpha = 100
            maskFilter = BlurMaskFilter(12f, BlurMaskFilter.Blur.NORMAL)
        }
        canvas.drawRoundRect(
            RectF(rect.left + 8, rect.top + 8, rect.right + 8, rect.bottom + 8),
            cornerRadius, cornerRadius, shadowPaint
        )

        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, blockPaint)

        // Draw border
        blockBorderPaint.strokeWidth = 5f
        blockBorderPaint.alpha = 200
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, blockBorderPaint)
        blockBorderPaint.alpha = 255

        // Draw text
        val textSize = min(cellWidth, cellHeight) * 0.3f
        textPaint.textSize = textSize
        textPaint.alpha = 200

        val centerX = (left + right) / 2
        val centerY = (top + bottom) / 2

        val title = memo.title.ifEmpty { "Memo" }
        drawCenteredText(canvas, title, centerX, centerY, textPaint)

        textPaint.alpha = 255
        blockPaint.alpha = 255
    }

    /**
     * Draws the preview highlight showing where the memo will be placed.
     */
    private fun drawPreview(
        canvas: Canvas,
        memo: Memo,
        gridX: Int,
        gridY: Int,
        cellWidth: Float,
        cellHeight: Float,
        bounds: RectF,
        state: GridState
    ) {
        // KEY FIX #3: Check if position is valid (within bounds and no collisions)
        val isValid = state.canPlace(memo, gridX, gridY, memo.width, memo.height)

        val left = bounds.left + (gridX * cellWidth)
        val top = bounds.top + (gridY * cellHeight)
        val right = left + (memo.width * cellWidth)
        val bottom = top + (memo.height * cellHeight)

        val padding = 8f
        val rect = RectF(left + padding, top + padding, right - padding, bottom - padding)
        val cornerRadius = 16f

        // Use green for valid position, red for invalid
        val paint = if (isValid) previewPaint else invalidPreviewPaint
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint)
    }

    private fun drawCenteredText(canvas: Canvas, text: String, centerX: Float, centerY: Float, paint: Paint) {
        val metrics = paint.fontMetrics
        val textY = centerY - (metrics.ascent + metrics.descent) / 2
        canvas.drawText(text, centerX, textY, paint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val state = gridState ?: return false
        val bounds = widgetBounds ?: return false

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                // KEY FIX #4: Check if touch is outside widget bounds
                if (!bounds.contains(event.x, event.y)) {
                    onOutsideTapped?.invoke()
                    return true
                }

                val cellWidth = bounds.width() / state.columns
                val cellHeight = bounds.height() / state.rows

                val gridX = ((event.x - bounds.left) / cellWidth).toInt().coerceIn(0, state.columns - 1)
                val gridY = ((event.y - bounds.top) / cellHeight).toInt().coerceIn(0, state.rows - 1)

                // Find memo at touch position
                val touchedMemo = state.memos.find { memo ->
                    gridX >= memo.originX && gridX < memo.originX + memo.width &&
                    gridY >= memo.originY && gridY < memo.originY + memo.height
                }

                if (touchedMemo != null) {
                    selectedMemo = touchedMemo
                    dragStartX = event.x
                    dragStartY = event.y
                    isDragging = true
                    onHapticFeedback?.invoke(HapticType.LIGHT)
                } else {
                    onCellTapped?.invoke(gridX, gridY)
                }

                invalidate()
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                if (isDragging && selectedMemo != null) {
                    dragStartX = event.x
                    dragStartY = event.y

                    // KEY FIX #2: Calculate preview position from center of dragged block
                    val cellWidth = bounds.width() / state.columns
                    val cellHeight = bounds.height() / state.rows

                    // Use the center of the dragged block for position calculation
                    val gridX = ((event.x - bounds.left) / cellWidth).toInt()
                    val gridY = ((event.y - bounds.top) / cellHeight).toInt()

                    // KEY FIX #3: Ensure the entire block stays within grid bounds
                    val memo = selectedMemo!!
                    val maxX = state.columns - memo.width
                    val maxY = state.rows - memo.height

                    val constrainedX = gridX.coerceIn(0, maxX)
                    val constrainedY = gridY.coerceIn(0, maxY)

                    previewPosition = Pair(constrainedX, constrainedY)
                    invalidate()
                }
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isDragging && selectedMemo != null && previewPosition != null) {
                    val (newX, newY) = previewPosition!!
                    val memo = selectedMemo!!

                    // Only move if the position is valid
                    if (state.canPlace(memo, newX, newY, memo.width, memo.height)) {
                        onMemoMoved?.invoke(memo, newX, newY)
                        onHapticFeedback?.invoke(HapticType.MEDIUM)
                    } else {
                        onHapticFeedback?.invoke(HapticType.ERROR)
                    }
                }

                selectedMemo = null
                isDragging = false
                previewPosition = null
                invalidate()
                return true
            }
        }

        return super.onTouchEvent(event)
    }
}
