package com.memo.widget.model

import kotlinx.serialization.Serializable

/**
 * Phase 1: Task priority levels.
 * Will influence color coding in Phase 1.
 */
@Serializable
enum class Priority {
    LOW,
    NORMAL,
    HIGH
}
