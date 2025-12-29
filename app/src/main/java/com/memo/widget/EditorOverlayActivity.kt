package com.memo.widget

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.math.min

/**
 * Phase 0: Transparent overlay that launches from widget tap.
 * CRITICAL VALIDATION: Does this feel seamless on Samsung One UI?
 *
 * Requirements:
 * - Transparent background with dimming
 * - No animation flicker
 * - 60fps during drag operations
 * - Instant cell tap response
 */
class EditorOverlayActivity : ComponentActivity() {

    companion object {
        private const val TAG = "EditorOverlay"
        private const val GRID_SIZE = 4
        private const val TARGET_FPS = 60
    }

    private val filledCells = mutableStateOf<Set<Int>>(emptySet())
    private var frameCount = 0L
    private var lastFpsLog = System.currentTimeMillis()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Load existing filled cells
        filledCells.value = getFilledCells()

        Log.d(TAG, "Overlay opened with ${filledCells.value.size} filled cells")

        setContent {
            EditorOverlayContent(
                filledCells = filledCells.value,
                onCellTap = { cellIndex ->
                    toggleCell(cellIndex)
                },
                onDrag = { offset ->
                    logFrameRate()
                },
                onClose = {
                    closeOverlay()
                }
            )
        }
    }

    private fun toggleCell(cellIndex: Int) {
        filledCells.value = if (cellIndex in filledCells.value) {
            filledCells.value - cellIndex
        } else {
            filledCells.value + cellIndex
        }
        Log.d(TAG, "Cell $cellIndex toggled. Filled cells: ${filledCells.value}")
    }

    private fun logFrameRate() {
        frameCount++
        val now = System.currentTimeMillis()
        val elapsed = now - lastFpsLog

        if (elapsed >= 1000) {
            val fps = frameCount * 1000 / elapsed
            Log.d(TAG, "FPS: $fps (target: $TARGET_FPS)")
            frameCount = 0
            lastFpsLog = now
        }
    }

    private fun closeOverlay() {
        Log.d(TAG, "Closing overlay, saving ${filledCells.value.size} filled cells")
        saveFilledCells(filledCells.value)

        // Trigger widget update
        val intent = android.content.Intent(this, MemoWidgetProvider::class.java).apply {
            action = MemoWidgetProvider.ACTION_REFRESH
            putExtra("filled_cells", filledCells.value.toIntArray())
        }
        sendBroadcast(intent)

        finish()
    }

    private fun getFilledCells(): Set<Int> {
        val prefs = getSharedPreferences("memo_phase0", MODE_PRIVATE)
        val cellsString = prefs.getString("filled_cells", "") ?: ""
        return if (cellsString.isEmpty()) {
            emptySet()
        } else {
            cellsString.split(",").mapNotNull { it.toIntOrNull() }.toSet()
        }
    }

    private fun saveFilledCells(cells: Set<Int>) {
        val prefs = getSharedPreferences("memo_phase0", MODE_PRIVATE)
        prefs.edit().putString("filled_cells", cells.joinToString(",")).apply()
    }

    override fun onBackPressed() {
        closeOverlay()
        super.onBackPressed()
    }
}

@Composable
fun EditorOverlayContent(
    filledCells: Set<Int>,
    onCellTap: (Int) -> Unit,
    onDrag: (Offset) -> Unit,
    onClose: () -> Unit
) {
    // Phase 0 color scheme (retro RPG inspired)
    val bgColor = Color(0xFF1A1A2E)
    val gridColor = Color(0xFF16213E)
    val borderColor = Color(0xFF0F3460)
    val fillColor = Color(0xFFE94560)
    val dimColor = Color(0x80000000)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(dimColor)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    // Tap outside grid to close
                    onClose()
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // 4x4 Grid
        GridCanvas(
            gridSize = EditorOverlayActivity.GRID_SIZE,
            filledCells = filledCells,
            bgColor = bgColor,
            gridColor = gridColor,
            borderColor = borderColor,
            fillColor = fillColor,
            onCellTap = onCellTap,
            onDrag = onDrag
        )
    }
}

@Composable
fun GridCanvas(
    gridSize: Int,
    filledCells: Set<Int>,
    bgColor: Color,
    gridColor: Color,
    borderColor: Color,
    fillColor: Color,
    onCellTap: (Int) -> Unit,
    onDrag: (Offset) -> Unit
) {
    val density = LocalDensity.current
    val gridSizeDp = 300.dp
    val gridSizePx = with(density) { gridSizeDp.toPx() }
    val cellSize = gridSizePx / gridSize
    val cellPadding = 4f
    val cornerRadius = 8f
    val strokeWidth = 2f

    Canvas(
        modifier = Modifier
            .size(gridSizeDp)
            .background(bgColor, RoundedCornerShape(12.dp))
            .padding(8.dp)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val col = (offset.x / cellSize).toInt().coerceIn(0, gridSize - 1)
                    val row = (offset.y / cellSize).toInt().coerceIn(0, gridSize - 1)
                    val cellIndex = row * gridSize + col
                    onCellTap(cellIndex)
                }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onDrag(dragAmount)
                }
            }
    ) {
        // Draw grid cells
        for (row in 0 until gridSize) {
            for (col in 0 until gridSize) {
                val cellIndex = row * gridSize + col
                val x = col * cellSize + cellPadding
                val y = row * cellSize + cellPadding

                // Fill cell
                val color = if (cellIndex in filledCells) fillColor else gridColor
                drawRoundRect(
                    color = color,
                    topLeft = Offset(x, y),
                    size = Size(cellSize - cellPadding * 2, cellSize - cellPadding * 2),
                    cornerRadius = CornerRadius(cornerRadius, cornerRadius)
                )

                // Border
                drawRoundRect(
                    color = borderColor,
                    topLeft = Offset(x, y),
                    size = Size(cellSize - cellPadding * 2, cellSize - cellPadding * 2),
                    cornerRadius = CornerRadius(cornerRadius, cornerRadius),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(strokeWidth)
                )
            }
        }
    }
}
