package com.memo.widget.model

import kotlinx.serialization.Serializable

/**
 * Phase 1: Position on the 4x4 grid.
 * Origin (0,0) is top-left corner.
 */
@Serializable
data class GridPosition(
    val x: Int,  // 0-3 for 4x4 grid (column)
    val y: Int   // 0-3 for 4x4 grid (row)
) {
    init {
        require(x >= 0) { "Grid x position must be non-negative" }
        require(y >= 0) { "Grid y position must be non-negative" }
    }

    fun isWithinBounds(gridSize: Int = 4): Boolean {
        return x < gridSize && y < gridSize
    }

    operator fun plus(other: GridPosition): GridPosition {
        return GridPosition(x + other.x, y + other.y)
    }
}
