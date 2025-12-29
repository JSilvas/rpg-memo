package com.memo.widget.data

import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Represents a single memo block on the grid.
 * Position is absolute (cell coordinates), not relative.
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
}

/**
 * The complete state of the grid, including all memos and configuration.
 * This is the single source of truth, persisted to DataStore.
 */
@Serializable
data class GridState(
    val widgetId: Int,                      // Android widget instance ID
    val columns: Int = 4,                   // Grid width in cells
    val rows: Int = 4,                      // Grid height in cells
    val memos: List<Memo> = emptyList(),
    val stats: Stats = Stats()
) {
    /**
     * Returns a 2D map of which cells are occupied.
     * True = occupied, False/null = empty.
     */
    fun occupancyMap(): Map<Pair<Int, Int>, Memo?> {
        val map = mutableMapOf<Pair<Int, Int>, Memo?>()
        
        // Initialize all cells as empty
        for (x in 0 until columns) {
            for (y in 0 until rows) {
                map[Pair(x, y)] = null
            }
        }
        
        // Mark occupied cells
        memos.forEach { memo ->
            memo.occupiedCells().forEach { cell ->
                map[cell] = memo
            }
        }
        
        return map
    }
    
    /**
     * Checks if a memo can be placed/moved to a target position.
     * Returns false if it would overlap another memo or exceed grid bounds.
     */
    fun canPlace(memo: Memo, targetX: Int, targetY: Int, targetWidth: Int = memo.width, targetHeight: Int = memo.height): Boolean {
        // Check bounds
        if (targetX < 0 || targetY < 0) return false
        if (targetX + targetWidth > columns) return false
        if (targetY + targetHeight > rows) return false
        
        // Check for overlaps (excluding self)
        val occupancy = occupancyMap()
        for (dx in 0 until targetWidth) {
            for (dy in 0 until targetHeight) {
                val cell = Pair(targetX + dx, targetY + dy)
                val occupant = occupancy[cell]
                if (occupant != null && occupant.id != memo.id) {
                    return false
                }
            }
        }
        
        return true
    }
    
    /**
     * Checks if the grid is completely full (no empty cells).
     */
    fun isFull(): Boolean {
        val totalCells = columns * rows
        val occupiedCells = memos.sumOf { it.width * it.height }
        return occupiedCells >= totalCells
    }
    
    /**
     * Finds the first empty cell (for quick-add 1x1).
     * Returns null if grid is full.
     */
    fun firstEmptyCell(): Pair<Int, Int>? {
        val occupancy = occupancyMap()
        for (y in 0 until rows) {
            for (x in 0 until columns) {
                if (occupancy[Pair(x, y)] == null) {
                    return Pair(x, y)
                }
            }
        }
        return null
    }
    
    /**
     * Validates that the current grid can support all existing memos.
     * Used when widget is resized smaller.
     * Returns list of memos that would be displaced.
     */
    fun validateAgainstSize(newColumns: Int, newRows: Int): List<Memo> {
        return memos.filter { memo ->
            memo.originX + memo.width > newColumns ||
            memo.originY + memo.height > newRows
        }
    }
}

/**
 * Lifetime statistics for this widget instance.
 */
@Serializable
data class Stats(
    val completedCount: Int = 0,
    val transferredCount: Int = 0,
    val deletedCount: Int = 0
) {
    fun incrementCompleted() = copy(completedCount = completedCount + 1)
    fun incrementTransferred() = copy(transferredCount = transferredCount + 1)
    fun incrementDeleted() = copy(deletedCount = deletedCount + 1)
}

/**
 * Configuration for how the widget renders.
 * Separate from GridState to allow theme changes without affecting data.
 */
@Serializable
data class WidgetConfig(
    val widgetId: Int,
    
    // Visual preferences
    val cornerRadiusDp: Float = 12f,        // Match system or custom
    val useDarkMode: Boolean? = null,       // null = follow system
    val blockPaddingDp: Float = 2f,
    
    // Behavior preferences
    val hapticFeedbackEnabled: Boolean = true,
    val showAgingEffects: Boolean = true,
    
    // Grid sizing (set when widget is placed/resized)
    val targetCellWidthDp: Float = 80f,
    val targetCellHeightDp: Float = 80f
)

/**
 * Represents the result of a render pass.
 * Used to communicate between RenderEngine and Widget.
 */
data class RenderResult(
    val bitmap: android.graphics.Bitmap,
    val gridState: GridState,
    val renderTimeMs: Long
)

/**
 * Events emitted from the Editor to trigger haptic feedback.
 */
sealed class HapticEvent {
    object BlockPickedUp : HapticEvent()
    object BlockDropped : HapticEvent()
    object BlockResized : HapticEvent()
    object GridFull : HapticEvent()           // Double-beat vibration
    object ActionCompleted : HapticEvent()    // Completion satisfaction
    object ActionCancelled : HapticEvent()
}
