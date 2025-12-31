package com.memo.widget.data

import com.memo.widget.model.Memo
import com.memo.widget.model.Stats
import kotlinx.serialization.Serializable

/**
 * The complete state of the grid, including all memos and configuration.
 * This is the single source of truth, persisted to DataStore.
 *
 * Aligned with Phase 1 specification from memo_widget_data_models.kt
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

    /**
     * Add a memo to the grid
     * Returns new GridState with memo added, or null if placement invalid
     */
    fun addMemo(memo: Memo): GridState? {
        if (!canPlace(memo, memo.originX, memo.originY)) {
            return null
        }
        return copy(memos = memos + memo)
    }

    /**
     * Move a memo to new position
     * Returns new GridState with memo moved, or null if placement invalid
     */
    fun moveMemo(memoId: String, newOriginX: Int, newOriginY: Int): GridState? {
        val memo = memos.find { it.id == memoId } ?: return null

        if (!canPlace(memo, newOriginX, newOriginY)) {
            return null
        }

        val updatedMemos = memos.map {
            if (it.id == memoId) it.moveTo(newOriginX, newOriginY) else it
        }

        return copy(memos = updatedMemos)
    }

    /**
     * Remove a memo from the grid
     * Returns new GridState with memo removed and stats updated
     */
    fun deleteMemo(memoId: String): GridState {
        return copy(
            memos = memos.filter { it.id != memoId },
            stats = stats.incrementDeleted()
        )
    }

    /**
     * Mark a memo as completed (removes it and updates stats)
     */
    fun completeMemo(memoId: String): GridState {
        return copy(
            memos = memos.filter { it.id != memoId },
            stats = stats.incrementCompleted()
        )
    }

    /**
     * Mark a memo as transferred (removes it and updates stats)
     */
    fun transferMemo(memoId: String): GridState {
        return copy(
            memos = memos.filter { it.id != memoId },
            stats = stats.incrementTransferred()
        )
    }

    /**
     * Update an existing memo
     */
    fun updateMemo(memo: Memo): GridState {
        val updatedMemos = memos.map {
            if (it.id == memo.id) memo else it
        }
        return copy(memos = updatedMemos)
    }

    /**
     * Get memo by ID
     */
    fun getMemo(memoId: String): Memo? {
        return memos.find { it.id == memoId }
    }

    /**
     * Get memo at specific cell
     */
    fun getMemoAtCell(x: Int, y: Int): Memo? {
        return memos.find { memo ->
            memo.occupiedCells().contains(x to y)
        }
    }
}
