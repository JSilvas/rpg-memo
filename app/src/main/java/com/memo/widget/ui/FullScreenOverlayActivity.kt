package com.memo.widget.ui

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.memo.widget.provider.GridWidgetProvider

/**
 * Full-screen transparent overlay that appears when user taps a widget cell.
 *
 * Key Features:
 * 1. Deep blurple background (90% opacity)
 * 2. Interactive grid that matches widget dimensions
 * 3. Tap outside grid to close
 * 4. Shows which cell was tapped for validation
 */
class FullScreenOverlayActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Extract intent data
        val widgetId = intent.getIntExtra(GridWidgetProvider.EXTRA_WIDGET_ID, -1)
        val cellRow = intent.getIntExtra(GridWidgetProvider.EXTRA_CELL_ROW, -1)
        val cellCol = intent.getIntExtra(GridWidgetProvider.EXTRA_CELL_COL, -1)
        val tapTimestamp = intent.getLongExtra(GridWidgetProvider.EXTRA_TAP_TIMESTAMP, 0L)

        // Calculate launch latency for validation
        val launchLatency = System.currentTimeMillis() - tapTimestamp
        Log.d("OverlayValidation", "Launch latency: ${launchLatency}ms (target: <150ms)")

        setContent {
            FullScreenOverlay(
                widgetId = widgetId,
                tappedCell = Pair(cellRow, cellCol),
                launchLatencyMs = launchLatency,
                onClose = { finish() }
            )
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        // Disable back button animation for seamless exit
        overridePendingTransition(0, 0)
    }
}

@Composable
fun FullScreenOverlay(
    widgetId: Int,
    tappedCell: Pair<Int, Int>,
    launchLatencyMs: Long,
    onClose: () -> Unit
) {
    // Deep blurple background color
    val deepBlurple = Color(0xE61A0F2E) // #1A0F2E with 90% opacity

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(deepBlurple)
            .pointerInput(Unit) {
                // Tap anywhere outside the grid to close
                detectTapGestures(onTap = { onClose() })
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Validation info header
            ValidationHeader(
                widgetId = widgetId,
                tappedCell = tappedCell,
                latencyMs = launchLatencyMs
            )

            // Interactive grid matching widget size
            InteractiveGrid(
                rows = 4,
                cols = 4,
                highlightedCell = tappedCell,
                onCellTap = { row, col ->
                    Log.d("OverlayValidation", "Cell tapped in overlay: ($row, $col)")
                }
            )

            // Instructions
            Text(
                text = "Tap outside grid to close\nBack button also works",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
fun ValidationHeader(
    widgetId: Int,
    tappedCell: Pair<Int, Int>,
    latencyMs: Long
) {
    val latencyColor = if (latencyMs < 150) Color.Green else Color.Yellow

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Widget Interaction Validation",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Tapped Cell: (${tappedCell.first}, ${tappedCell.second})",
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 16.sp
        )

        Text(
            text = "Launch Latency: ${latencyMs}ms",
            color = latencyColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )

        if (latencyMs >= 150) {
            Text(
                text = "⚠️ Above target (<150ms)",
                color = Color.Yellow,
                fontSize = 12.sp
            )
        } else {
            Text(
                text = "✓ Within target",
                color = Color.Green,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun InteractiveGrid(
    rows: Int,
    cols: Int,
    highlightedCell: Pair<Int, Int>,
    onCellTap: (row: Int, col: Int) -> Unit
) {
    // Sample cell colors (matching widget layout for validation)
    val cellColors = mapOf(
        Pair(0, 0) to Color(0xFFFFE066), // Yellow memo
        Pair(0, 1) to Color(0xFFFFE066),
        Pair(0, 2) to Color(0xFFE0E0E0), // Empty
        Pair(0, 3) to Color(0xFFE0E0E0),
        Pair(1, 0) to Color(0xFFFFE066),
        Pair(1, 1) to Color(0xFFFFE066),
        Pair(1, 2) to Color(0xFF90CAF9), // Blue memo
        Pair(1, 3) to Color(0xFFE0E0E0),
        Pair(2, 0) to Color(0xFFE0E0E0),
        Pair(2, 1) to Color(0xFFE0E0E0),
        Pair(2, 2) to Color(0xFF90CAF9),
        Pair(2, 3) to Color(0xFFCE93D8), // Purple memo
        Pair(3, 0) to Color(0xFFE0E0E0),
        Pair(3, 1) to Color(0xFFE0E0E0),
        Pair(3, 2) to Color(0xFFE0E0E0),
        Pair(3, 3) to Color(0xFFCE93D8)
    )

    // Grid container - sized to match typical widget dimensions
    Box(
        modifier = Modifier
            .size(320.dp) // 4x4 grid with 80dp cells
            .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
            .padding(8.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            repeat(rows) { row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    repeat(cols) { col ->
                        GridCell(
                            row = row,
                            col = col,
                            color = cellColors[Pair(row, col)] ?: Color.Gray,
                            isHighlighted = (row == highlightedCell.first && col == highlightedCell.second),
                            onTap = { onCellTap(row, col) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RowScope.GridCell(
    row: Int,
    col: Int,
    color: Color,
    isHighlighted: Boolean,
    onTap: () -> Unit
) {
    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .background(color, RoundedCornerShape(6.dp))
            .border(
                width = if (isHighlighted) 3.dp else 1.dp,
                color = if (isHighlighted) Color.White else Color.Black.copy(alpha = 0.1f),
                shape = RoundedCornerShape(6.dp)
            )
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onTap() })
            },
        contentAlignment = Alignment.Center
    ) {
        if (isHighlighted) {
            Text(
                text = "✓",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
