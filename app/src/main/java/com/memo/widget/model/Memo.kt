package com.memo.widget.model

import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Represents a single memo block on the grid.
 * Position is absolute (cell coordinates), not relative.
 *
 * Aligned with Phase 1 specification from memo_widget_data_models.kt
 */
@Serializable
data class Memo(
    val id: String = UUID.randomUUID().toString(),

    // Content
    val title: String = "",
    val description: String? = null,
    val emoji: String? = null,  // Single emoji character for 1x1 display

    // Grid Position (absolute cell coordinates)
    val originX: Int,           // Top-left cell X
    val originY: Int,           // Top-left cell Y
    val width: Int = 1,         // Width in cells (1-4)
    val height: Int = 1,        // Height in cells (1-4)

    // Timestamps
    val createdAt: Long = System.currentTimeMillis(),
    val lastInteractedAt: Long = System.currentTimeMillis(),

    // Visual State (computed at render time, but can be cached)
    val colorHex: String = "#FFE066"  // Default Post-it yellow
) {
    /**
     * Returns all grid cells this memo occupies.
     * Used for collision detection and rendering.
     */
    fun occupiedCells(): List<Pair<Int, Int>> {
        return (0 until width).flatMap { dx ->
            (0 until height).map { dy ->
                Pair(originX + dx, originY + dy)
            }
        }
    }

    /**
     * Calculates the aging/decay tier based on last interaction.
     * Returns 0 (fresh) to 3 (maximum decay).
     */
    fun agingTier(): Int {
        val daysSinceInteraction = (System.currentTimeMillis() - lastInteractedAt) / (24 * 60 * 60 * 1000)
        return when {
            daysSinceInteraction <= 2 -> 0  // Fresh
            daysSinceInteraction <= 4 -> 1  // Slight fade
            daysSinceInteraction <= 6 -> 2  // Visible wear
            else -> 3                        // Maximum decay
        }
    }

    /**
     * Creates a copy with refreshed interaction timestamp.
     * Call this on any user interaction (move, resize, edit).
     */
    fun touched(): Memo = copy(lastInteractedAt = System.currentTimeMillis())

    /**
     * Check if this memo overlaps with another memo's cells
     */
    fun overlapsWith(otherCells: Set<Pair<Int, Int>>): Boolean {
        return occupiedCells().any { it in otherCells }
    }

    /**
     * Check if this memo fits within grid bounds
     */
    fun isWithinBounds(gridSize: Int = 4): Boolean {
        return occupiedCells().all { (x, y) ->
            x >= 0 && x < gridSize && y >= 0 && y < gridSize
        }
    }

    /**
     * Create a copy with new position (for drag operations)
     */
    fun moveTo(newOriginX: Int, newOriginY: Int): Memo {
        return copy(originX = newOriginX, originY = newOriginY).touched()
    }

    /**
     * Resize memo to new dimensions
     * Returns new memo with updated dimensions, or null if invalid
     */
    fun resizeTo(newWidth: Int, newHeight: Int): Memo? {
        if (newWidth < 1 || newWidth > 4 || newHeight < 1 || newHeight > 4) {
            return null
        }
        return copy(width = newWidth, height = newHeight).touched()
    }
}
