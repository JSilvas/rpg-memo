# Phase 1: Task Block Data Model & Interaction

**Goal**: Transform the validated overlay pattern into a functional task management system with spatial constraints.

**Phase 0 proved it works. Phase 1 makes it useful.**

---

## Phase 1 Scope

### Core Features
1. ✅ Task block data model with multiple shapes
2. ✅ Grid collision detection system
3. ✅ DataStore persistence (replace SharedPreferences)
4. ✅ Drag-to-place interaction
5. ✅ Text rendering in blocks

### Out of Scope (Phase 2)
- ❌ Animations (keep instant for now)
- ❌ Task editing UI (just drag/place)
- ❌ Multiple widgets
- ❌ Settings screen
- ❌ Overlay position alignment

---

## 1. Task Block Data Model

### Block Shapes (Rectangles Only)

**Design Decision**: No L-shapes or Tetris pieces. Only simple rectangles.
- Users resize blocks by dragging edges/corners (no rotation needed)
- All shapes are rectangular (1x1 to 4x4)
- Need a 1x2 instead of 2x1? Just resize the block.

```kotlin
enum class BlockShape(val width: Int, val height: Int) {
    SIZE_1X1(1, 1),  // Smallest unit
    SIZE_2X1(2, 1),  // Horizontal
    SIZE_1X2(1, 2),  // Vertical
    SIZE_2X2(2, 2),  // Square medium
    SIZE_3X1(3, 1),  // Wide horizontal
    SIZE_1X3(1, 3),  // Tall vertical
    SIZE_3X2(3, 2),  // Wide rectangle
    SIZE_2X3(2, 3),  // Tall rectangle
    SIZE_4X1(4, 1),  // Full row
    SIZE_1X4(1, 4),  // Full column
    SIZE_3X3(3, 3),  // Large square
    SIZE_4X2(4, 2),  // Wide large
    SIZE_2X4(2, 4),  // Tall large
    SIZE_4X3(4, 3),  // Very wide
    SIZE_3X4(3, 4),  // Very tall
    SIZE_4X4(4, 4);  // Maximum size

    // Generate cells for rectangle
    fun getCellsAt(x: Int, y: Int): List<Pair<Int, Int>> {
        return (0 until height).flatMap { dy ->
            (0 until width).map { dx -> (x + dx) to (y + dy) }
        }
    }

    companion object {
        // Get shape for dimensions
        fun fromDimensions(width: Int, height: Int): BlockShape? =
            values().find { it.width == width && it.height == height }

        // Validate dimensions
        fun isValidDimensions(width: Int, height: Int): Boolean =
            width in 1..4 && height in 1..4
    }
}
```

### Resize Support

```kotlin
// Add to TaskBlock class
fun resizeTo(newWidth: Int, newHeight: Int): TaskBlock? {
    val newShape = BlockShape.fromDimensions(newWidth, newHeight) ?: return null
    return copy(shape = newShape)
}
```

### Block Data Class

```kotlin
@Serializable
data class TaskBlock(
    val id: String = UUID.randomUUID().toString(),
    val shape: BlockShape,
    val position: GridPosition,
    val title: String,
    val priority: Priority = Priority.NORMAL,
    val color: BlockColor = BlockColor.DEFAULT
)

data class GridPosition(
    val x: Int,  // 0-3 for 4x4 grid
    val y: Int   // 0-3 for 4x4 grid
)

enum class Priority { LOW, NORMAL, HIGH }

enum class BlockColor(val hex: Int) {
    DEFAULT(0xFFE94560.toInt()),    // Red (Phase 0 fill color)
    URGENT(0xFFFF6B6B.toInt()),     // Bright red
    IMPORTANT(0xFFFFD93D.toInt()),  // Yellow
    ROUTINE(0xFF6BCB77.toInt())     // Green
}
```

---

## 2. Grid State Management

### GridState Class

```kotlin
class GridState(
    private val gridSize: Int = 4
) {
    private val blocks = mutableStateListOf<TaskBlock>()

    fun canPlaceBlock(block: TaskBlock, position: GridPosition): Boolean {
        // Check each cell the block would occupy
        val occupiedCells = getBlockCells(block, position)

        // Out of bounds check
        if (occupiedCells.any { (x, y) ->
            x < 0 || x >= gridSize || y < 0 || y >= gridSize
        }) {
            return false
        }

        // Collision check with existing blocks
        val existingCells = blocks
            .filter { it.id != block.id } // Ignore self when moving
            .flatMap { getBlockCells(it, it.position) }
            .toSet()

        return occupiedCells.none { it in existingCells }
    }

    fun placeBlock(block: TaskBlock): Boolean {
        if (!canPlaceBlock(block, block.position)) return false
        blocks.add(block)
        return true
    }

    fun moveBlock(blockId: String, newPosition: GridPosition): Boolean {
        val block = blocks.find { it.id == blockId } ?: return false
        val movedBlock = block.copy(position = newPosition)

        if (!canPlaceBlock(movedBlock, newPosition)) return false

        blocks.removeIf { it.id == blockId }
        blocks.add(movedBlock)
        return true
    }

    fun removeBlock(blockId: String) {
        blocks.removeIf { it.id == blockId }
    }

    fun getBlocks(): List<TaskBlock> = blocks.toList()

    private fun getBlockCells(
        block: TaskBlock,
        position: GridPosition
    ): List<Pair<Int, Int>> {
        return block.shape.cells.map { (dx, dy) ->
            (position.x + dx) to (position.y + dy)
        }
    }
}
```

---

## 3. DataStore Persistence

### Replace SharedPreferences Stub

```kotlin
class BlockRepository(private val context: Context) {
    private val dataStore = context.dataStore

    companion object {
        private val Context.dataStore by preferencesDataStore("memo_blocks")
        private val BLOCKS_KEY = stringPreferencesKey("blocks_json")
    }

    suspend fun saveBlocks(blocks: List<TaskBlock>) {
        val json = Json.encodeToString(blocks)
        dataStore.edit { prefs ->
            prefs[BLOCKS_KEY] = json
        }
    }

    fun getBlocks(): Flow<List<TaskBlock>> {
        return dataStore.data.map { prefs ->
            val json = prefs[BLOCKS_KEY] ?: "[]"
            Json.decodeFromString(json)
        }
    }

    suspend fun clearBlocks() {
        dataStore.edit { it.clear() }
    }
}
```

### Dependencies (app/build.gradle.kts)

```kotlin
// Already included in Phase 0
implementation("androidx.datastore:datastore-preferences:1.0.0")

// Add Kotlin serialization
implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
```

---

## 4. Drag-to-Place Interaction

### Drag State

```kotlin
sealed class DragState {
    object Idle : DragState()
    data class Dragging(
        val block: TaskBlock,
        val offset: Offset
    ) : DragState()
}
```

### Compose Canvas with Drag

```kotlin
@Composable
fun BlockGridCanvas(
    gridState: GridState,
    onBlockMoved: (blockId: String, newPosition: GridPosition) -> Unit
) {
    var dragState by remember { mutableStateOf<DragState>(DragState.Idle) }

    Canvas(
        modifier = Modifier
            .size(300.dp)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        // Find block at touch position
                        val block = findBlockAtPosition(offset)
                        if (block != null) {
                            dragState = DragState.Dragging(block, offset)
                        }
                    },
                    onDrag = { change, dragAmount ->
                        if (dragState is DragState.Dragging) {
                            // Update visual drag offset
                            val current = dragState as DragState.Dragging
                            dragState = current.copy(
                                offset = current.offset + dragAmount
                            )
                        }
                    },
                    onDragEnd = {
                        if (dragState is DragState.Dragging) {
                            val dragging = dragState as DragState.Dragging
                            val snapPosition = snapToGrid(dragging.offset)

                            // Attempt to place block
                            if (gridState.canPlaceBlock(dragging.block, snapPosition)) {
                                onBlockMoved(dragging.block.id, snapPosition)
                            }

                            dragState = DragState.Idle
                        }
                    }
                )
            }
    ) {
        // Draw grid background
        drawGridBackground()

        // Draw placed blocks
        gridState.getBlocks().forEach { block ->
            if (dragState is DragState.Dragging &&
                (dragState as DragState.Dragging).block.id == block.id) {
                // Draw with drag offset (semi-transparent)
                drawBlock(block, alpha = 0.7f)
            } else {
                drawBlock(block)
            }
        }

        // Draw snap preview during drag
        if (dragState is DragState.Dragging) {
            drawSnapPreview((dragState as DragState.Dragging).block)
        }
    }
}
```

---

## 5. Text Rendering in Blocks

### Simple Text Rendering (Phase 1)

```kotlin
fun DrawScope.drawBlock(block: TaskBlock, alpha: Float = 1f) {
    val cellSize = size.width / 4

    // Draw block shape
    block.shape.cells.forEach { (dx, dy) ->
        val x = (block.position.x + dx) * cellSize
        val y = (block.position.y + dy) * cellSize

        drawRoundRect(
            color = Color(block.color.hex).copy(alpha = alpha),
            topLeft = Offset(x + 4f, y + 4f),
            size = Size(cellSize - 8f, cellSize - 8f),
            cornerRadius = CornerRadius(8f)
        )
    }

    // Draw text (simple, centered in first cell)
    val textPaint = Paint().asFrameworkPaint().apply {
        isAntiAlias = true
        textSize = 12.sp.toPx()
        color = android.graphics.Color.WHITE
    }

    val x = block.position.x * cellSize + cellSize / 2
    val y = block.position.y * cellSize + cellSize / 2

    drawContext.canvas.nativeCanvas.drawText(
        block.title,
        x,
        y,
        textPaint
    )
}
```

---

## Phase 1 Implementation Order

### Week 1: Data Model & Grid Logic
1. ☐ Create `TaskBlock` data class
2. ☐ Define `BlockShape` enum
3. ☐ Implement `GridState` collision detection
4. ☐ Unit tests for collision logic

### Week 2: Persistence
1. ☐ Add Kotlin serialization plugin
2. ☐ Implement `BlockRepository` with DataStore
3. ☐ Migrate from SharedPreferences
4. ☐ Widget update flow with blocks

### Week 3: Drag Interaction
1. ☐ Implement drag-to-place in overlay
2. ☐ Visual feedback during drag
3. ☐ Snap-to-grid logic
4. ☐ Invalid placement feedback (red tint)

### Week 4: Text & Polish
1. ☐ Text rendering in blocks
2. ☐ Block colors by priority
3. ☐ Long-press to delete blocks
4. ☐ Testing on Samsung S25 Ultra

---

## Success Criteria

Phase 1 is **COMPLETE** when:
- ✅ Multiple blocks can be placed on grid
- ✅ Blocks have different shapes (at least 3)
- ✅ Collision detection prevents overlaps
- ✅ Drag-to-place works smoothly (60fps+)
- ✅ Blocks persist across app restarts
- ✅ Text is readable in blocks
- ✅ Widget reflects block state

---

## Out of Scope Reminders

**DO NOT ADD**:
- Animations (Phase 2)
- Task editing UI (Phase 2)
- Multiple widgets (Phase 2)
- Settings (Phase 2)
- Widget rotation (Phase 2)

**Keep it simple. Make blocks work.**
