package com.memo.widget

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
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

    // Edit Modal state
    data class EditModalState(
        val isNewMemo: Boolean,
        val memoId: String? = null,
        val cellX: Int? = null,
        val cellY: Int? = null,
        val initialTitle: String = ""
    )
    private var editModalState by mutableStateOf<EditModalState?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        widgetId = intent.getIntExtra("WIDGET_ID", 0)
        repository = MemoRepository(this)

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
                        initialTitle = memo.title
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
                    editModalState = editModalState,
                    onCellTap = { x, y ->
                        // Check if cell is empty or has a memo
                        val memo = state.getMemoAtCell(x, y)
                        if (memo != null) {
                            // Edit existing memo
                            editModalState = EditModalState(
                                isNewMemo = false,
                                memoId = memo.id,
                                initialTitle = memo.title
                            )
                            Log.d(TAG, "Editing memo: ${memo.title}")
                        } else {
                            // Create new memo at this cell
                            editModalState = EditModalState(
                                isNewMemo = true,
                                cellX = x,
                                cellY = y,
                                initialTitle = ""
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
                    onEditModalSave = { title ->
                        lifecycleScope.launch {
                            val modal = editModalState
                            if (modal != null) {
                                if (modal.isNewMemo) {
                                    // Create new memo
                                    val memo = Memo(
                                        title = title,
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
                                        val updatedMemo = existingMemo.copy(title = title).touched()
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

@Composable
fun EditorOverlayContent(
    gridState: GridState,
    editModalState: EditorOverlayActivity.EditModalState?,
    onCellTap: (Int, Int) -> Unit,
    onMemoMove: (String, Int, Int) -> Unit,
    onEditModalSave: (String) -> Unit,
    onEditModalDismiss: () -> Unit,
    onClose: () -> Unit
) {
    // Phase 0 colors (keeping retro palette)
    val bgColor = Color(0xFF1A1A2E)
    val gridColor = Color(0xFF16213E)
    val borderColor = Color(0xFF0F3460)
    val dimColor = Color(0x80000000)

    val density = LocalDensity.current
    val gridSizeDp = 300.dp
    val gridSizePx = with(density) { gridSizeDp.toPx() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(dimColor)
            .pointerInput(editModalState) {
                detectTapGestures { tapOffset ->
                    // Calculate grid bounds (centered in screen)
                    val screenWidth = size.width
                    val screenHeight = size.height
                    val gridLeft = (screenWidth - gridSizePx) / 2f
                    val gridTop = (screenHeight - gridSizePx) / 2f
                    val gridRight = gridLeft + gridSizePx
                    val gridBottom = gridTop + gridSizePx

                    // Check if tap is outside grid bounds
                    val isOutsideGrid = tapOffset.x < gridLeft || tapOffset.x > gridRight ||
                                       tapOffset.y < gridTop || tapOffset.y > gridBottom

                    if (isOutsideGrid) {
                        // Tap outside grid → close both modal and overlay
                        if (editModalState != null) {
                            onEditModalDismiss() // Close modal first
                        }
                        onClose() // Then close overlay
                    } else if (editModalState == null) {
                        // Tap inside grid with no modal → close overlay
                        onClose()
                    }
                    // If tap inside grid with modal open → Dialog handles it
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // Grid Canvas
        MemoGridCanvas(
            gridState = gridState,
            bgColor = bgColor,
            gridColor = gridColor,
            borderColor = borderColor,
            onCellTap = onCellTap,
            onMemoMove = onMemoMove
        )

        // Edit Modal
        if (editModalState != null) {
            EditMemoModal(
                title = editModalState.initialTitle,
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

@Composable
fun EditMemoModal(
    title: String,
    isNewMemo: Boolean,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
    onDismissWithOverlay: () -> Unit
) {
    var editedTitle by remember { mutableStateOf(title) }

    // Custom modal overlay instead of Dialog for better tap control
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x80000000)) // Semi-transparent background
            .pointerInput(Unit) {
                detectTapGestures { tapOffset ->
                    // Calculate modal bounds (card centered with padding)
                    val modalWidth = size.width - 32.dp.toPx() // 16dp padding on each side
                    val modalLeft = 16.dp.toPx()
                    val modalRight = modalLeft + modalWidth

                    // Modal is centered vertically
                    val modalHeight = 250.dp.toPx() // Approximate modal height
                    val modalTop = (size.height - modalHeight) / 2f
                    val modalBottom = modalTop + modalHeight

                    // Check if tap is outside modal bounds
                    val isOutsideModal = tapOffset.x < modalLeft || tapOffset.x > modalRight ||
                                        tapOffset.y < modalTop || tapOffset.y > modalBottom

                    if (isOutsideModal) {
                        // Calculate grid bounds (300dp centered)
                        val gridSizePx = 300.dp.toPx()
                        val gridLeft = (size.width - gridSizePx) / 2f
                        val gridTop = (size.height - gridSizePx) / 2f
                        val gridRight = gridLeft + gridSizePx
                        val gridBottom = gridTop + gridSizePx

                        val isOutsideGrid = tapOffset.x < gridLeft || tapOffset.x > gridRight ||
                                           tapOffset.y < gridTop || tapOffset.y > gridBottom

                        if (isOutsideGrid) {
                            // Outside both modal and grid → close both
                            onDismissWithOverlay()
                        } else {
                            // Outside modal but inside grid → close modal only
                            onDismiss()
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1A1A2E)
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Title
                Text(
                    text = if (isNewMemo) "New Memo" else "Edit Memo",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                // Text field
                OutlinedTextField(
                    value = editedTitle,
                    onValueChange = { editedTitle = it },
                    label = { Text("Title") },
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

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = Color.Gray)
                    }
                    Button(
                        onClick = {
                            if (editedTitle.isNotBlank()) {
                                onSave(editedTitle.trim())
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2CD9C5),
                            contentColor = Color(0xFF0A1526)
                        ),
                        enabled = editedTitle.isNotBlank()
                    ) {
                        Text("Save")
                    }
                }
            }
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
    onMemoMove: (String, Int, Int) -> Unit
) {
    val density = LocalDensity.current
    val gridSizeDp = 300.dp
    val gridSizePx = with(density) { gridSizeDp.toPx() }
    val cellSize = gridSizePx / EditorOverlayActivity.GRID_SIZE

    var draggedMemo by remember { mutableStateOf<Memo?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var isDragging by remember { mutableStateOf(false) }

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
                        draggedMemo = gridState.getMemoAtCell(col, row)
                        dragOffset = Offset.Zero
                        isDragging = true
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragOffset += dragAmount
                    },
                    onDragEnd = {
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

        // Draw drop target indicator during drag
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

            // Draw text
            drawIntoCanvas { canvas ->
                val textPaint = android.graphics.Paint()
                textPaint.isAntiAlias = true
                textPaint.textSize = 14.dp.toPx()
                textPaint.color = android.graphics.Color.WHITE
                textPaint.textAlign = android.graphics.Paint.Align.CENTER

                canvas.nativeCanvas.drawText(
                    memo.title,
                    x + width / 2,
                    y + height / 2 + textPaint.textSize / 3,
                    textPaint
                )
            }
        }
    }
}
