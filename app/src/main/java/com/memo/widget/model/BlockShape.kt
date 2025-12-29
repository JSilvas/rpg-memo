package com.memo.widget.model

/**
 * Phase 1: Rectangular block shapes only (no L-shapes or Tetris pieces).
 * Users resize blocks by dragging edges/corners to create any rectangular size.
 *
 * Each shape defines its dimensions and the cells it occupies relative to its origin.
 */
enum class BlockShape(
    val width: Int,
    val height: Int
) {
    /** 1x1 - Smallest unit */
    SIZE_1X1(1, 1),

    /** 2x1 - Horizontal */
    SIZE_2X1(2, 1),

    /** 1x2 - Vertical */
    SIZE_1X2(1, 2),

    /** 2x2 - Square medium */
    SIZE_2X2(2, 2),

    /** 3x1 - Wide horizontal */
    SIZE_3X1(3, 1),

    /** 1x3 - Tall vertical */
    SIZE_1X3(1, 3),

    /** 3x2 - Wide rectangle */
    SIZE_3X2(3, 2),

    /** 2x3 - Tall rectangle */
    SIZE_2X3(2, 3),

    /** 4x1 - Full row */
    SIZE_4X1(4, 1),

    /** 1x4 - Full column */
    SIZE_1X4(1, 4),

    /** 3x3 - Large square */
    SIZE_3X3(3, 3),

    /** 4x2 - Wide large */
    SIZE_4X2(4, 2),

    /** 2x4 - Tall large */
    SIZE_2X4(2, 4),

    /** 4x3 - Very wide */
    SIZE_4X3(4, 3),

    /** 3x4 - Very tall */
    SIZE_3X4(3, 4),

    /** 4x4 - Maximum size */
    SIZE_4X4(4, 4);

    /**
     * Get all cells occupied by this shape at a given position.
     * Generates a simple rectangle of cells.
     */
    fun getCellsAt(x: Int, y: Int): List<Pair<Int, Int>> {
        val cells = mutableListOf<Pair<Int, Int>>()
        for (dy in 0 until height) {
            for (dx in 0 until width) {
                cells.add((x + dx) to (y + dy))
            }
        }
        return cells
    }

    companion object {
        /**
         * Get BlockShape for given dimensions, or null if invalid
         */
        fun fromDimensions(width: Int, height: Int): BlockShape? {
            return values().find { it.width == width && it.height == height }
        }

        /**
         * Check if dimensions are valid (within 4x4 grid)
         */
        fun isValidDimensions(width: Int, height: Int): Boolean {
            return width in 1..4 && height in 1..4
        }
    }
}
