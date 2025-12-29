package com.memo.widget.model

import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Phase 1: Task block with spatial properties.
 * Represents a task that occupies physical space on the grid.
 *
 * Inspired by RPG inventory systems:
 * - Shape determines physical size
 * - Position on grid enforces spatial constraints
 * - No infinite scrolling - if it doesn't fit, something must be completed/deleted
 */
@Serializable
data class TaskBlock(
    val id: String = UUID.randomUUID().toString(),

    /** Shape defines the physical dimensions */
    val shape: BlockShape,

    /** Top-left position on grid */
    val position: GridPosition,

    /** Task title (rendered in block) */
    val title: String,

    /** Priority influences color */
    val priority: Priority = Priority.NORMAL,

    /** Optional custom color (overrides priority color) */
    val customColor: BlockColor? = null
) {
    /**
     * Get the color for this block (custom or priority-based)
     */
    fun getColor(): BlockColor {
        return customColor ?: BlockColor.fromPriority(priority)
    }

    /**
     * Get all cells occupied by this block
     */
    fun getOccupiedCells(): List<Pair<Int, Int>> {
        return shape.getCellsAt(position.x, position.y)
    }

    /**
     * Check if this block overlaps with another block's cells
     */
    fun overlapsWith(otherCells: Set<Pair<Int, Int>>): Boolean {
        return getOccupiedCells().any { it in otherCells }
    }

    /**
     * Check if this block fits within grid bounds
     */
    fun isWithinBounds(gridSize: Int = 4): Boolean {
        return getOccupiedCells().all { (x, y) ->
            x >= 0 && x < gridSize && y >= 0 && y < gridSize
        }
    }

    /**
     * Create a copy with new position (for drag operations)
     */
    fun moveTo(newPosition: GridPosition): TaskBlock {
        return copy(position = newPosition)
    }

    /**
     * Resize block to new dimensions
     * Returns new block with updated shape, or null if invalid
     */
    fun resizeTo(newWidth: Int, newHeight: Int): TaskBlock? {
        val newShape = BlockShape.fromDimensions(newWidth, newHeight) ?: return null
        return copy(shape = newShape)
    }
}
