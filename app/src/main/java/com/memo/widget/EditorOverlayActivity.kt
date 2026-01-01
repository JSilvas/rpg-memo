package com.memo.widget

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.lifecycleScope
import com.memo.widget.data.GridState
import com.memo.widget.model.Memo
import com.memo.widget.repository.MemoRepository
import kotlinx.coroutines.launch

/**
 * Phase 1: Memo editor with tap-empty-cell creation and Edit Modal.
 *
 * Features:
 * - Tap empty cell → creates 1x1 memo with Edit Modal
 * - Tap existing memo → opens Edit Modal
 * - Drag memos to move them
 * - Edit Modal for title editing
 * - DataStore persistence
 *
 * Aligned with Phase 1 specification.
 */
class EditorOverlayActivity : ComponentActivity() {

    companion object {
        private const val TAG = "EditorOverlay"
        const val GRID_SIZE = 4
    }

    private lateinit var repository: MemoRepository
    private var widgetId: Int = 0
    private var gridState by mutableStateOf<GridState?>(null)
    private var widgetSizeDp by mutableStateOf(300)

    // Edit Modal state
    data class EditModalState(
        val isNewMemo: Boolean,
        val memoId: String? = null,
        val cellX: Int? = null,
        val cellY: Int? = null,
        val initialTitle: String = "",
        val initialDescription: String = ""
    )
    private var editModalState by mutableStateOf<EditModalState?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        widgetId = intent.getIntExtra("WIDGET_ID", 0)
        repository = MemoRepository(this)

        // Get widget dimensions to match overlay to actual widget size
        try {
            val appWidgetManager = android.appwidget.AppWidgetManager.getInstance(this)
            val options = appWidgetManager.getAppWidgetOptions(widgetId)
            val minWidth = options.getInt(android.appwidget.AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 250)
            val minHeight = options.getInt(android.appwidget.AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 250)
            // Use the smaller dimension to match widget's square grid calculation
            widgetSizeDp = kotlin.math.min(minWidth, minHeight)
            Log.d(TAG, "Widget dimensions: ${minWidth}x${minHeight}dp, using ${widgetSizeDp}dp for grid")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get widget dimensions, using default 300dp", e)
            widgetSizeDp = 300
        }

        // Check if specific cell was tapped on widget
        val tappedCellX = intent.getIntExtra("CELL_X", -1)
        val tappedCellY = intent.getIntExtra("CELL_Y", -1)

        // Immediately show Edit Modal if cell was tapped (don't wait for grid state)
        // This makes the modal appear simultaneously with the overlay
        if (tappedCellX != -1 && tappedCellY != -1) {
            editModalState = EditModalState(
                isNewMemo = true, // Assume new memo initially
                cellX = tappedCellX,
                cellY = tappedCellY,
                initialTitle = ""
            )
            Log.d(TAG, "Immediately showing Edit Modal for cell ($tappedCellX, $tappedCellY)")
        }

        // Load grid state from repository
        lifecycleScope.launch {
            val state = repository.getGridState(widgetId)
            gridState = state
            Log.d(TAG, "Loaded grid state with ${state.memos.size} memos")

            // Update Edit Modal if cell has existing memo
            if (tappedCellX != -1 && tappedCellY != -1) {
                val memo = state.getMemoAtCell(tappedCellX, tappedCellY)
                if (memo != null) {
                    // Update to edit existing memo
                    editModalState = EditModalState(
                        isNewMemo = false,
                        memoId = memo.id,
                        initialTitle = memo.title,
                        initialDescription = memo.description ?: ""
                    )
                    Log.d(TAG, "Updated Edit Modal to edit existing memo: ${memo.title}")
                }
            }
        }

        setContent {
            val state = gridState
            if (state != null) {
                EditorOverlayContent(
                    gridState = state,
                    widgetSizeDp = widgetSizeDp,
                    editModalState = editModalState,
                    onCellTap = { x, y ->
                        // Check if cell is empty or has a memo
                        val memo = state.getMemoAtCell(x, y)
                        if (memo != null) {
                            // Edit existing memo
                            editModalState = EditModalState(
                                isNewMemo = false,
                                memoId = memo.id,
                                initialTitle = memo.title,
                                initialDescription = memo.description ?: ""
                            )
                            Log.d(TAG, "Editing memo: ${memo.title}")
                        } else {
                            // Create new memo at this cell
                            editModalState = EditModalState(
                                isNewMemo = true,
                                cellX = x,
                                cellY = y,
                                initialTitle = "",
                                initialDescription = ""
                            )
                            Log.d(TAG, "Creating new memo at ($x, $y)")
                        }
                    },
                    onMemoMove = { memoId, newOriginX, newOriginY ->
                        lifecycleScope.launch {
                            val newState = state.moveMemo(memoId, newOriginX, newOriginY)
                            if (newState != null) {
                                gridState = newState
                                Log.d(TAG, "Memo $memoId moved to ($newOriginX, $newOriginY)")
                            }
                        }
                    },
                    onMemoResize = { memoId, newWidth, newHeight ->
                        lifecycleScope.launch {
                            val newState = state.resizeMemo(memoId, newWidth, newHeight)
                            if (newState != null) {
                                gridState = newState
                                Log.d(TAG, "Memo $memoId resized to ${newWidth}x$newHeight")
                            } else {
                                Log.w(TAG, "Failed to resize memo $memoId - invalid size or overlap")
                            }
                        }
                    },
                    onEditModalSave = { title, description ->
                        lifecycleScope.launch {
                            val modal = editModalState
                            if (modal != null) {
                                if (modal.isNewMemo) {
                                    // Create new 1x1 memo
                                    val memo = Memo(
                                        title = title,
                                        description = description.ifBlank { null },
                                        originX = modal.cellX!!,
                                        originY = modal.cellY!!,
                                        width = 1,
                                        height = 1
                                    )
                                    val newState = state.addMemo(memo)
                                    if (newState != null) {
                                        gridState = newState
                                        Log.d(TAG, "Created memo: $title")
                                    } else {
                                        Log.w(TAG, "Failed to create memo - invalid placement")
                                    }
                                } else {
                                    // Update existing memo
                                    val existingMemo = state.getMemo(modal.memoId!!)
                                    if (existingMemo != null) {
                                        val updatedMemo = existingMemo.copy(
                                            title = title,
                                            description = description.ifBlank { null }
                                        ).touched()
                                        val newState = state.updateMemo(updatedMemo)
                                        gridState = newState
                                        Log.d(TAG, "Updated memo: $title")
                                    }
                                }
                                editModalState = null
                            }
                        }
                    },
                    onEditModalDismiss = {
                        editModalState = null
                    },
                    onClose = {
                        saveAndClose()
                    }
                )
            } else {
                // Loading state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Loading...", color = Color.White)
                }
            }
        }
    }

    private fun saveAndClose() {
        lifecycleScope.launch {
            gridState?.let { state ->
                repository.saveGridState(state)
                Log.d(TAG, "Saved grid state with ${state.memos.size} memos")

                // Trigger widget update
                val intent = android.content.Intent(this@EditorOverlayActivity, MemoWidgetProvider::class.java).apply {
                    action = MemoWidgetProvider.ACTION_REFRESH
                }
                sendBroadcast(intent)
            }

            finish()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        saveAndClose()
        super.onBackPressed()
    }
}

/**
 * Resize handle types for memo blocks
 */
enum class ResizeHandle { NONE, RIGHT, BOTTOM, BOTTOM_RIGHT }

/**
 * Helper function to draw wrapped text on a native Canvas.
 */
fun drawWrappedTextOnCanvas(
    canvas: android.graphics.Canvas,
    text: String,
    x: Float,
    y: Float,
    width: Float,
    height: Float,
    paint: android.graphics.Paint,
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

@Composable
fun EditorOverlayContent(
    gridState: GridState,
    widgetSizeDp: Int,
    editModalState: EditorOverlayActivity.EditModalState?,
    onCellTap: (Int, Int) -> Unit,
    onMemoMove: (String, Int, Int) -> Unit,
    onMemoResize: (String, Int, Int) -> Unit,
    onEditModalSave: (String, String) -> Unit,
    onEditModalDismiss: () -> Unit,
    onClose: () -> Unit
) {
    // Match widget colors exactly
    val bgColor = Color(0xFF1A1A2E)
    val gridColor = Color(0xFF16213E)
    val borderColor = Color(0xFF0F3460)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent) // Fully transparent when no modal
    ) {
        // Grid positioned at top-start (where widgets typically are)
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 16.dp, top = 120.dp) // Typical widget position
        ) {
            MemoGridCanvas(
                gridState = gridState,
                bgColor = bgColor,
                gridColor = gridColor,
                borderColor = borderColor,
                onCellTap = onCellTap,
                onMemoMove = onMemoMove,
                onMemoResize = onMemoResize,
                isCalibrating = false,
                gridSizeDp = widgetSizeDp.dp
            )
        }

        // Dim background when modal is open
        if (editModalState != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f)) // Dim when modal open
                    .pointerInput(Unit) {
                        detectTapGestures {
                            // Tap anywhere outside modal to dismiss
                            onEditModalDismiss()
                        }
                    }
            )

            // Edit Modal - centered over grid
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(top = 120.dp) // Offset to align with grid area
            ) {
                EditMemoModal(
                    title = editModalState.initialTitle,
                    description = editModalState.initialDescription,
                    isNewMemo = editModalState.isNewMemo,
                    onSave = onEditModalSave,
                    onDismiss = onEditModalDismiss,
                    onDismissWithOverlay = {
                        onEditModalDismiss() // Close modal
                        onClose() // Close overlay
                    }
                )
            }
        }
    }
}

@Composable
fun EditMemoModal(
    title: String,
    description: String = "",
    isNewMemo: Boolean,
    onSave: (String, String) -> Unit, // (title, description)
    onDismiss: () -> Unit,
    onDismissWithOverlay: () -> Unit
) {
    var editedTitle by remember { mutableStateOf(title) }
    var editedDescription by remember { mutableStateOf(description) }

    // Auto-save helper function
    fun autoSave() {
        if (editedTitle.isNotBlank()) {
            onSave(editedTitle.trim(), editedDescription.trim())
        }
    }

    // Compact card modal
    Box {
        Card(
            modifier = Modifier
                .width(280.dp), // Compact width
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1A1A2E)
            )
        ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Title
                    Text(
                        text = if (isNewMemo) "New Memo" else "Edit Memo",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                // Title field (max ~3 words)
                OutlinedTextField(
                    value = editedTitle,
                    onValueChange = { if (it.length <= 30) editedTitle = it },
                    label = { Text("Title (max 3 words)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF2CD9C5),
                        unfocusedBorderColor = Color(0xFF16213E),
                        focusedLabelColor = Color(0xFF2CD9C5),
                        unfocusedLabelColor = Color.Gray,
                        cursorColor = Color(0xFF2CD9C5)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Description field (optional, not shown in grid)
                OutlinedTextField(
                    value = editedDescription,
                    onValueChange = { editedDescription = it },
                    label = { Text("Description (optional)") },
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF2CD9C5),
                        unfocusedBorderColor = Color(0xFF16213E),
                        focusedLabelColor = Color(0xFF2CD9C5),
                        unfocusedLabelColor = Color.Gray,
                        cursorColor = Color(0xFF2CD9C5)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Hints
                Column {
                    Text(
                        text = "• Drag edges/corners to resize",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = "• Tap outside to save & close",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
            }
        }

        // Close button (X in top-right corner of modal)
        IconButton(
            onClick = {
                autoSave()
                onDismiss()
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .size(28.dp)
                .background(Color(0xFF2CD9C5), CircleShape)
        ) {
            Text(
                text = "✕",
                color = Color(0xFF0A1526),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun MemoGridCanvas(
    gridState: GridState,
    bgColor: Color,
    gridColor: Color,
    borderColor: Color,
    onCellTap: (Int, Int) -> Unit,
    onMemoMove: (String, Int, Int) -> Unit,
    onMemoResize: (String, Int, Int) -> Unit,
    isCalibrating: Boolean = false,
    gridSizeDp: androidx.compose.ui.unit.Dp = 300.dp
) {
    val density = LocalDensity.current
    val canvasPaddingDp = 8.dp
    val gridSizePx = with(density) { gridSizeDp.toPx() }
    val canvasPaddingPx = with(density) { canvasPaddingDp.toPx() }
    // Calculate cell size based on drawable area (after padding)
    val drawableAreaPx = gridSizePx - (canvasPaddingPx * 2)
    val cellSize = drawableAreaPx / EditorOverlayActivity.GRID_SIZE

    // Move state
    var draggedMemo by remember { mutableStateOf<Memo?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var isDragging by remember { mutableStateOf(false) }

    // Resize state
    var resizingMemo by remember { mutableStateOf<Memo?>(null) }
    var resizeHandle by remember { mutableStateOf(ResizeHandle.NONE) }
    var resizeStartSize by remember { mutableStateOf(Pair(0, 0)) }
    var resizeDelta by remember { mutableStateOf(Offset.Zero) }
    var isResizing by remember { mutableStateOf(false) }

    // Handle detection threshold (in pixels)
    val handleSize = 20f

    Canvas(
        modifier = Modifier
            .size(gridSizeDp)
            .background(bgColor, RoundedCornerShape(12.dp))
            .padding(8.dp)
            .pointerInput(gridState) {
                detectTapGestures { offset ->
                    // Only handle taps if not dragging
                    if (!isDragging) {
                        val col = (offset.x / cellSize).toInt().coerceIn(0, EditorOverlayActivity.GRID_SIZE - 1)
                        val row = (offset.y / cellSize).toInt().coerceIn(0, EditorOverlayActivity.GRID_SIZE - 1)
                        onCellTap(col, row)
                    }
                }
            }
            .pointerInput(gridState) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val col = (offset.x / cellSize).toInt().coerceIn(0, EditorOverlayActivity.GRID_SIZE - 1)
                        val row = (offset.y / cellSize).toInt().coerceIn(0, EditorOverlayActivity.GRID_SIZE - 1)
                        val memo = gridState.getMemoAtCell(col, row)

                        if (memo != null) {
                            // Calculate memo bounds
                            val memoLeft = memo.originX * cellSize
                            val memoTop = memo.originY * cellSize
                            val memoRight = memoLeft + memo.width * cellSize
                            val memoBottom = memoTop + memo.height * cellSize

                            // Check if tap is on resize handles
                            val onRightEdge = offset.x >= memoRight - handleSize && offset.x <= memoRight
                            val onBottomEdge = offset.y >= memoBottom - handleSize && offset.y <= memoBottom
                            val onBottomRightCorner = onRightEdge && onBottomEdge

                            if (onBottomRightCorner) {
                                // Start corner resize
                                resizingMemo = memo
                                resizeHandle = ResizeHandle.BOTTOM_RIGHT
                                resizeStartSize = Pair(memo.width, memo.height)
                                resizeDelta = Offset.Zero
                                isResizing = true
                            } else if (onRightEdge) {
                                // Start right edge resize
                                resizingMemo = memo
                                resizeHandle = ResizeHandle.RIGHT
                                resizeStartSize = Pair(memo.width, memo.height)
                                resizeDelta = Offset.Zero
                                isResizing = true
                            } else if (onBottomEdge) {
                                // Start bottom edge resize
                                resizingMemo = memo
                                resizeHandle = ResizeHandle.BOTTOM
                                resizeStartSize = Pair(memo.width, memo.height)
                                resizeDelta = Offset.Zero
                                isResizing = true
                            } else {
                                // Start move
                                draggedMemo = memo
                                dragOffset = Offset.Zero
                                isDragging = true
                            }
                        }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        if (isResizing) {
                            resizeDelta += dragAmount
                        } else if (isDragging) {
                            dragOffset += dragAmount
                        }
                    },
                    onDragEnd = {
                        if (isResizing) {
                            resizingMemo?.let { memo ->
                                // Calculate new size based on drag delta and handle type
                                val widthDelta = (resizeDelta.x / cellSize).toInt()
                                val heightDelta = (resizeDelta.y / cellSize).toInt()

                                val newWidth = when (resizeHandle) {
                                    ResizeHandle.RIGHT, ResizeHandle.BOTTOM_RIGHT ->
                                        (resizeStartSize.first + widthDelta).coerceIn(1, 4)
                                    else -> resizeStartSize.first
                                }

                                val newHeight = when (resizeHandle) {
                                    ResizeHandle.BOTTOM, ResizeHandle.BOTTOM_RIGHT ->
                                        (resizeStartSize.second + heightDelta).coerceIn(1, 4)
                                    else -> resizeStartSize.second
                                }

                                if (newWidth != memo.width || newHeight != memo.height) {
                                    onMemoResize(memo.id, newWidth, newHeight)
                                }
                            }
                            resizingMemo = null
                            resizeHandle = ResizeHandle.NONE
                            resizeDelta = Offset.Zero
                            isResizing = false
                        } else if (isDragging) {
                            draggedMemo?.let { memo ->
                                // Calculate center of the dragged memo
                                val memoCenterX = memo.originX * cellSize + dragOffset.x + (memo.width * cellSize) / 2f
                                val memoCenterY = memo.originY * cellSize + dragOffset.y + (memo.height * cellSize) / 2f

                                // Determine target position based on center
                                val newCol = (memoCenterX / cellSize).toInt().coerceIn(0, EditorOverlayActivity.GRID_SIZE - memo.width)
                                val newRow = (memoCenterY / cellSize).toInt().coerceIn(0, EditorOverlayActivity.GRID_SIZE - memo.height)

                                if (newCol != memo.originX || newRow != memo.originY) {
                                    onMemoMove(memo.id, newCol, newRow)
                                }
                            }
                            draggedMemo = null
                            dragOffset = Offset.Zero
                            isDragging = false
                        }
                    }
                )
            }
    ) {
        val cellPadding = 4f
        val cornerRadius = 8f
        val strokeWidth = 2f

        // Draw grid background cells
        for (row in 0 until EditorOverlayActivity.GRID_SIZE) {
            for (col in 0 until EditorOverlayActivity.GRID_SIZE) {
                val x = col * cellSize + cellPadding
                val y = row * cellSize + cellPadding

                drawRoundRect(
                    color = gridColor,
                    topLeft = Offset(x, y),
                    size = Size(cellSize - cellPadding * 2, cellSize - cellPadding * 2),
                    cornerRadius = CornerRadius(cornerRadius, cornerRadius)
                )
            }
        }

        // Draw drop target indicator during drag (move)
        draggedMemo?.let { memo ->
            // Calculate center of the dragged memo
            val memoCenterX = memo.originX * cellSize + dragOffset.x + (memo.width * cellSize) / 2f
            val memoCenterY = memo.originY * cellSize + dragOffset.y + (memo.height * cellSize) / 2f

            // Determine which cell the center is over - this becomes the target
            val targetCol = (memoCenterX / cellSize).toInt().coerceIn(0, EditorOverlayActivity.GRID_SIZE - memo.width)
            val targetRow = (memoCenterY / cellSize).toInt().coerceIn(0, EditorOverlayActivity.GRID_SIZE - memo.height)

            // Check if target position is valid
            val isValidPlacement = gridState.canPlace(memo, targetCol, targetRow)

            // Draw indicator outline
            val indicatorX = targetCol * cellSize + cellPadding
            val indicatorY = targetRow * cellSize + cellPadding
            val indicatorWidth = memo.width * cellSize - cellPadding * 2
            val indicatorHeight = memo.height * cellSize - cellPadding * 2

            // Color: green if valid, red if invalid
            val indicatorColor = if (isValidPlacement) {
                Color(0xFF2CD9C5) // Teal/green from theme
            } else {
                Color(0xFFE94560) // Red from theme
            }

            // Draw dashed outline indicator
            drawRoundRect(
                color = indicatorColor.copy(alpha = 0.3f),
                topLeft = Offset(indicatorX, indicatorY),
                size = Size(indicatorWidth, indicatorHeight),
                cornerRadius = CornerRadius(cornerRadius, cornerRadius)
            )

            // Draw thicker border
            drawRoundRect(
                color = indicatorColor,
                topLeft = Offset(indicatorX, indicatorY),
                size = Size(indicatorWidth, indicatorHeight),
                cornerRadius = CornerRadius(cornerRadius, cornerRadius),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth * 2)
            )
        }

        // Draw resize preview during drag (resize)
        resizingMemo?.let { memo ->
            // Calculate new size based on drag delta
            val widthDelta = (resizeDelta.x / cellSize).toInt()
            val heightDelta = (resizeDelta.y / cellSize).toInt()

            val newWidth = when (resizeHandle) {
                ResizeHandle.RIGHT, ResizeHandle.BOTTOM_RIGHT ->
                    (resizeStartSize.first + widthDelta).coerceIn(1, 4)
                else -> resizeStartSize.first
            }

            val newHeight = when (resizeHandle) {
                ResizeHandle.BOTTOM, ResizeHandle.BOTTOM_RIGHT ->
                    (resizeStartSize.second + heightDelta).coerceIn(1, 4)
                else -> resizeStartSize.second
            }

            // Check if new size is valid
            val isValidResize = gridState.canPlace(memo, memo.originX, memo.originY, newWidth, newHeight)

            // Draw preview outline
            val previewX = memo.originX * cellSize + cellPadding
            val previewY = memo.originY * cellSize + cellPadding
            val previewWidth = newWidth * cellSize - cellPadding * 2
            val previewHeight = newHeight * cellSize - cellPadding * 2

            val previewColor = if (isValidResize) {
                Color(0xFF2CD9C5) // Teal/green
            } else {
                Color(0xFFE94560) // Red
            }

            // Draw preview
            drawRoundRect(
                color = previewColor.copy(alpha = 0.3f),
                topLeft = Offset(previewX, previewY),
                size = Size(previewWidth, previewHeight),
                cornerRadius = CornerRadius(cornerRadius, cornerRadius)
            )

            drawRoundRect(
                color = previewColor,
                topLeft = Offset(previewX, previewY),
                size = Size(previewWidth, previewHeight),
                cornerRadius = CornerRadius(cornerRadius, cornerRadius),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth * 2)
            )
        }

        // Draw memos
        gridState.memos.forEach { memo ->
            val isDraggingThis = draggedMemo?.id == memo.id

            // Calculate memo position (with drag offset if dragging)
            val baseX = memo.originX * cellSize
            val baseY = memo.originY * cellSize
            val offsetX = if (isDraggingThis) dragOffset.x else 0f
            val offsetY = if (isDraggingThis) dragOffset.y else 0f

            val x = baseX + offsetX + cellPadding
            val y = baseY + offsetY + cellPadding

            // Memo size
            val width = memo.width * cellSize - cellPadding * 2
            val height = memo.height * cellSize - cellPadding * 2

            // Memo color
            val color = Color(android.graphics.Color.parseColor(memo.colorHex))
            val alpha = if (isDraggingThis) 0.7f else 1f

            // Draw memo background
            drawRoundRect(
                color = color.copy(alpha = alpha),
                topLeft = Offset(x, y),
                size = Size(width, height),
                cornerRadius = CornerRadius(cornerRadius, cornerRadius)
            )

            // Draw border
            drawRoundRect(
                color = borderColor,
                topLeft = Offset(x, y),
                size = Size(width, height),
                cornerRadius = CornerRadius(cornerRadius, cornerRadius),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
            )

            // Draw text (wrapped)
            drawIntoCanvas { canvas ->
                val textPaint = android.graphics.Paint()
                textPaint.isAntiAlias = true
                textPaint.textSize = 14.dp.toPx()
                textPaint.color = android.graphics.Color.WHITE
                textPaint.textAlign = android.graphics.Paint.Align.CENTER

                // Draw wrapped text
                drawWrappedTextOnCanvas(
                    canvas = canvas.nativeCanvas,
                    text = memo.title,
                    x = x,
                    y = y,
                    width = width,
                    height = height,
                    paint = textPaint,
                    padding = 8.dp.toPx()
                )
            }

            // Draw resize handles (right edge, bottom edge, bottom-right corner)
            if (!isDraggingThis && resizingMemo?.id != memo.id) {
                val handleColor = Color(0xFF2CD9C5).copy(alpha = 0.6f)
                val handleThickness = 3f

                // Right edge handle
                drawLine(
                    color = handleColor,
                    start = Offset(x + width, y + height * 0.3f),
                    end = Offset(x + width, y + height * 0.7f),
                    strokeWidth = handleThickness
                )

                // Bottom edge handle
                drawLine(
                    color = handleColor,
                    start = Offset(x + width * 0.3f, y + height),
                    end = Offset(x + width * 0.7f, y + height),
                    strokeWidth = handleThickness
                )

                // Bottom-right corner handle (small circle)
                drawCircle(
                    color = handleColor,
                    radius = 6f,
                    center = Offset(x + width, y + height)
                )
            }
        }
    }
}
