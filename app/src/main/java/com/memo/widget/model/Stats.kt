package com.memo.widget.model

import kotlinx.serialization.Serializable

/**
 * Lifetime statistics for this widget instance.
 *
 * Tracks user actions:
 * - Completed: Memo marked as done (✓ in radial menu)
 * - Transferred: Memo shared/exported (↗ in radial menu)
 * - Deleted: Memo removed without completion (✕ in radial menu)
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
