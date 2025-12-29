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
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.memo.widget.model.*
import com.memo.widget.repository.BlockRepository
import kotlinx.coroutines.launch

/**
 * Phase 1: Task block editor with drag-to-place and text rendering.
 *
 * Features:
 * - GridState for collision detection
 * - Drag blocks to move them
 * - Tap to select/delete blocks
 * - Text rendering in blocks
 * - DataStore persistence
 */
class EditorOverlayActivity : ComponentActivity() {

    companion object {
        private const val TAG = "EditorOverlay"
        const val GRID_SIZE = 4
    }

    private lateinit var repository: BlockRepository
    private lateinit var gridState: GridState
    private val selectedBlockId = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        repository = BlockRepository(this)
        gridState = GridState(GRID_SIZE)

        // Load blocks from repository
        lifecycleScope.launch {
            val blocks = repository.getBlocks()
            gridState.setBlocks(blocks)
            Log.d(TAG, "Loaded ${blocks.size} blocks from repository")
        }

        setContent {
            EditorOverlayContent(
                gridState = gridState,
                selectedBlockId = selectedBlockId.value,
                onBlockTap = { blockId ->
                    selectedBlockId.value = if (selectedBlockId.value == blockId) null else blockId
                },
                onBlockMove = { blockId, newPosition ->
                    if (gridState.moveBlock(blockId, newPosition)) {
                        Log.d(TAG, "Block $blockId moved to $newPosition")
                    }
                },
                onAddBlock = {
                    // Add a sample 2x2 block for testing
                    val block = TaskBlock(
                        shape = BlockShape.SIZE_2X2,
                        position = GridPosition(0, 0),
                        title = "Task ${gridState.getBlocks().size + 1}",
                        priority = Priority.NORMAL
                    )
                    if (gridState.addBlock(block)) {
                        Log.d(TAG, "Added block: ${block.title}")
                    }
                },
                onDeleteBlock = {
                    selectedBlockId.value?.let { blockId ->
                        gridState.removeBlock(blockId)
                        selectedBlockId.value = null
                        Log.d(TAG, "Deleted block $blockId")
                    }
                },
                onClose = {
                    saveAndClose()
                }
            )
        }
    }

    private fun saveAndClose() {
        lifecycleScope.launch {
            val blocks = gridState.getBlocks()
            repository.saveBlocks(blocks)
            Log.d(TAG, "Saved ${blocks.size} blocks to repository")

            // Trigger widget update
            val intent = android.content.Intent(this@EditorOverlayActivity, MemoWidgetProvider::class.java).apply {
                action = MemoWidgetProvider.ACTION_REFRESH
            }
            sendBroadcast(intent)

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
    selectedBlockId: String?,
    onBlockTap: (String) -> Unit,
    onBlockMove: (String, GridPosition) -> Unit,
    onAddBlock: () -> Unit,
    onDeleteBlock: () -> Unit,
    onClose: () -> Unit
) {
    val blocks by remember { derivedStateOf { gridState.getBlocks() } }

    // Phase 0 colors (keeping retro palette)
    val bgColor = Color(0xFF1A1A2E)
    val gridColor = Color(0xFF16213E)
    val borderColor = Color(0xFF0F3460)
    val dimColor = Color(0x80000000)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(dimColor)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    // Tap outside to close
                    onClose()
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Grid Canvas
            BlockGridCanvas(
                gridState = gridState,
                selectedBlockId = selectedBlockId,
                bgColor = bgColor,
                gridColor = gridColor,
                borderColor = borderColor,
                onBlockTap = onBlockTap,
                onBlockMove = onBlockMove
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Controls
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onAddBlock) {
                    Text("Add Block")
                }
                if (selectedBlockId != null) {
                    Button(onClick = onDeleteBlock) {
                        Text("Delete")
                    }
                }
            }
        }
    }
}

@Composable
fun BlockGridCanvas(
    gridState: GridState,
    selectedBlockId: String?,
    bgColor: Color,
    gridColor: Color,
    borderColor: Color,
    onBlockTap: (String) -> Unit,
    onBlockMove: (String, GridPosition) -> Unit
) {
    val density = LocalDensity.current
    val gridSizeDp = 300.dp
    val gridSizePx = with(density) { gridSizeDp.toPx() }
    val cellSize = gridSizePx / EditorOverlayActivity.GRID_SIZE

    var draggedBlock by remember { mutableStateOf<TaskBlock?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }

    Canvas(
        modifier = Modifier
            .size(gridSizeDp)
            .background(bgColor, RoundedCornerShape(12.dp))
            .padding(8.dp)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val col = (offset.x / cellSize).toInt().coerceIn(0, EditorOverlayActivity.GRID_SIZE - 1)
                    val row = (offset.y / cellSize).toInt().coerceIn(0, EditorOverlayActivity.GRID_SIZE - 1)
                    gridState.getBlockAtCell(col, row)?.let { block ->
                        onBlockTap(block.id)
                    }
                }
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val col = (offset.x / cellSize).toInt().coerceIn(0, EditorOverlayActivity.GRID_SIZE - 1)
                        val row = (offset.y / cellSize).toInt().coerceIn(0, EditorOverlayActivity.GRID_SIZE - 1)
                        draggedBlock = gridState.getBlockAtCell(col, row)
                        dragOffset = Offset.Zero
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragOffset += dragAmount
                    },
                    onDragEnd = {
                        draggedBlock?.let { block ->
                            // Calculate new position based on drag offset
                            val newCol = ((block.position.x * cellSize + dragOffset.x) / cellSize)
                                .toInt().coerceIn(0, EditorOverlayActivity.GRID_SIZE - 1)
                            val newRow = ((block.position.y * cellSize + dragOffset.y) / cellSize)
                                .toInt().coerceIn(0, EditorOverlayActivity.GRID_SIZE - 1)

                            val newPosition = GridPosition(newCol, newRow)
                            if (newPosition != block.position) {
                                onBlockMove(block.id, newPosition)
                            }
                        }
                        draggedBlock = null
                        dragOffset = Offset.Zero
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

        // Draw blocks
        gridState.getBlocks().forEach { block ->
            val isDragging = draggedBlock?.id == block.id
            val isSelected = selectedBlockId == block.id

            // Calculate block position (with drag offset if dragging)
            val baseX = block.position.x * cellSize
            val baseY = block.position.y * cellSize
            val offsetX = if (isDragging) dragOffset.x else 0f
            val offsetY = if (isDragging) dragOffset.y else 0f

            val x = baseX + offsetX + cellPadding
            val y = baseY + offsetY + cellPadding

            // Block size
            val width = block.shape.width * cellSize - cellPadding * 2
            val height = block.shape.height * cellSize - cellPadding * 2

            // Block color
            val color = Color(block.getColor().hex)
            val alpha = if (isDragging) 0.7f else 1f

            // Draw block background
            drawRoundRect(
                color = color.copy(alpha = alpha),
                topLeft = Offset(x, y),
                size = Size(width, height),
                cornerRadius = CornerRadius(cornerRadius, cornerRadius)
            )

            // Draw border (thicker if selected)
            drawRoundRect(
                color = if (isSelected) Color.White else borderColor,
                topLeft = Offset(x, y),
                size = Size(width, height),
                cornerRadius = CornerRadius(cornerRadius, cornerRadius),
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = if (isSelected) strokeWidth * 2 else strokeWidth
                )
            )

            // Draw text
            drawIntoCanvas { canvas ->
                val textPaint = android.graphics.Paint()
                textPaint.isAntiAlias = true
                textPaint.textSize = 14.dp.toPx()
                textPaint.color = android.graphics.Color.WHITE
                textPaint.textAlign = android.graphics.Paint.Align.CENTER

                canvas.nativeCanvas.drawText(
                    block.title,
                    x + width / 2,
                    y + height / 2 + textPaint.textSize / 3,
                    textPaint
                )
            }
        }
    }
}
