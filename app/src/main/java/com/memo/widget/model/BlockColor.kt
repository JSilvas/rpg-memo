package com.memo.widget.model

/**
 * Phase 1: Block color palette (retro RPG inspired).
 * Colors map to priority levels and visual hierarchy.
 */
enum class BlockColor(val hex: Int) {
    /** Default red (Phase 0 fill color) */
    DEFAULT(0xFFE94560.toInt()),

    /** Urgent tasks - bright red */
    URGENT(0xFFFF6B6B.toInt()),

    /** Important tasks - yellow/gold */
    IMPORTANT(0xFFFFD93D.toInt()),

    /** Routine tasks - green */
    ROUTINE(0xFF6BCB77.toInt()),

    /** Low priority - blue */
    LOW_PRIORITY(0xFF4D96FF.toInt());

    companion object {
        fun fromPriority(priority: Priority): BlockColor {
            return when (priority) {
                Priority.HIGH -> URGENT
                Priority.NORMAL -> DEFAULT
                Priority.LOW -> LOW_PRIORITY
            }
        }
    }
}
