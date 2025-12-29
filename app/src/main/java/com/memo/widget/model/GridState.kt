package com.memo.widget.model

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList

/**
 * Phase 1: Grid state manager with collision detection.
 *
 * Manages the 4x4 grid of task blocks, enforcing spatial constraints:
 * - No overlapping blocks
 * - All blocks must fit within grid bounds
 * - Collision detection for placement validation
 */
class GridState(
    private val gridSize: Int = 4
) {
    private val _blocks: SnapshotStateList<TaskBlock> = mutableStateListOf()

    /**
     * Get immutable list of all blocks
     */
    fun getBlocks(): List<TaskBlock> = _blocks.toList()

    /**
     * Get block by ID
     */
    fun getBlock(id: String): TaskBlock? {
        return _blocks.find { it.id == id }
    }

    /**
     * Check if a block can be placed at a position
     * @param block The block to place
     * @param position The target position
     * @return true if placement is valid (no collisions, within bounds)
     */
    fun canPlaceBlock(block: TaskBlock, position: GridPosition): Boolean {
        // Create temporary block at new position
        val testBlock = block.moveTo(position)

        // Check bounds
        if (!testBlock.isWithinBounds(gridSize)) {
            return false
        }

        // Get cells this block would occupy
        val occupiedCells = testBlock.getOccupiedCells().toSet()

        // Check collision with existing blocks (excluding self if moving)
        val existingCells = _blocks
            .filter { it.id != block.id }
            .flatMap { it.getOccupiedCells() }
            .toSet()

        return !testBlock.overlapsWith(existingCells)
    }

    /**
     * Add a new block to the grid
     * @return true if block was added, false if placement invalid
     */
    fun addBlock(block: TaskBlock): Boolean {
        if (!canPlaceBlock(block, block.position)) {
            return false
        }
        _blocks.add(block)
        return true
    }

    /**
     * Move a block to a new position
     * @return true if block was moved, false if placement invalid
     */
    fun moveBlock(blockId: String, newPosition: GridPosition): Boolean {
        val block = getBlock(blockId) ?: return false

        if (!canPlaceBlock(block, newPosition)) {
            return false
        }

        // Remove old block and add with new position
        _blocks.removeIf { it.id == blockId }
        _blocks.add(block.moveTo(newPosition))
        return true
    }

    /**
     * Remove a block from the grid
     * @return true if block was removed
     */
    fun removeBlock(blockId: String): Boolean {
        return _blocks.removeIf { it.id == blockId }
    }

    /**
     * Remove all blocks
     */
    fun clear() {
        _blocks.clear()
    }

    /**
     * Replace all blocks (for loading from persistence)
     */
    fun setBlocks(blocks: List<TaskBlock>) {
        _blocks.clear()
        _blocks.addAll(blocks)
    }

    /**
     * Get the block at a specific grid cell, if any
     * @return The block occupying this cell, or null
     */
    fun getBlockAtCell(x: Int, y: Int): TaskBlock? {
        return _blocks.find { block ->
            block.getOccupiedCells().contains(x to y)
        }
    }

    /**
     * Count occupied cells
     */
    fun getOccupiedCellCount(): Int {
        return _blocks.flatMap { it.getOccupiedCells() }.distinct().size
    }

    /**
     * Get remaining free cells
     */
    fun getFreeCellCount(): Int {
        return (gridSize * gridSize) - getOccupiedCellCount()
    }

    /**
     * Check if grid is full (no placement possible for smallest block)
     */
    fun isFull(): Boolean {
        // Check if any single cell is free
        for (y in 0 until gridSize) {
            for (x in 0 until gridSize) {
                if (getBlockAtCell(x, y) == null) {
                    return false
                }
            }
        }
        return true
    }
}
