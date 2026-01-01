# Widget Interaction Validation Plan

## Goal
Validate that Android home screen widgets can support sophisticated touch interactions similar to Google Calendar's widget, specifically:
- Visual feedback on touch (pulse animation)
- Differentiate between tap (quick touch-release) and long-press (tap-and-hold)
- Seamless transition to full-screen overlay for editing
- Maintain widget manipulability (move/resize) through long-press

## Reference: Google Calendar Widget Behavior
1. **Tap event item**: Single pulse → Opens event editor on release
2. **Tap empty space**: Opens full calendar app
3. **Long-press widget**: Enters widget reconfiguration mode (move/resize)

## Our Proposed Interaction Pattern

### 1. Quick Tap (< 500ms)
**Touch Down:**
- Detect which grid cell was touched (memo or empty space)
- Trigger single pulse animation on the touched region
- Visual feedback: slight scale up (1.05x) + subtle glow

**Touch Up (Release):**
- **If tapped on memo**: Launch full-screen memo edit overlay
- **If tapped on empty cell**: Launch full-screen app view (grid-only mode)

### 2. Long-Press (≥ 500ms)
**Touch Held:**
- Cancel pulse animation
- Delegate to Android's native widget manipulation system
- User can now drag widget to reposition or resize it

### 3. Full-Screen Overlay Design

**Layout:**
```
┌─────────────────────────────┐
│   Deep Blurple Background   │ #1A0F2E (dark, rich purple)
│                             │
│   ┌─────────────────┐       │
│   │                 │       │
│   │   4x4 Grid      │       │ ← Same size as widget grid
│   │   (Interactive) │       │    Positioned to align with widget
│   │                 │       │
│   └─────────────────┘       │
│                             │
│   [Edit Controls Below]     │
│                             │
└─────────────────────────────┘
```

**Key Properties:**
- Background color: `#1A0F2E` (deep blurple) with 90% opacity
- Grid dimensions: Match widget exactly (same cell count and proportions)
- Grid position: Aligned to overlap widget location on screen
- Close mechanism: Back button or tap outside grid area

## Technical Validation Checklist

### Phase 1: Touch Detection (Current Focus)
- [ ] Implement custom RemoteViews PendingIntent with touch coordinates
  - **Challenge**: RemoteViews has limited touch event access
  - **Workaround**: Use fillInIntent with cell coordinates pre-mapped
- [ ] Create touch gesture classifier (tap vs long-press)
  - Use `GestureDetector.SimpleOnGestureListener`
  - Override `onSingleTapUp()` for tap
  - Override `onLongPress()` for long-press
- [ ] Map touch coordinates to grid cells accurately

### Phase 2: Visual Feedback
- [ ] Implement pulse animation for bitmap rendering
  - Render two states: normal and pulsed (1.05x scale, +glow)
  - Use AppWidgetManager.updateAppWidget() to swap bitmaps
  - Timing: Show pulsed state for 150ms max
- [ ] Test animation performance on mid-tier devices (< 100ms response)
- [ ] Ensure pulse doesn't interfere with long-press detection

### Phase 3: Overlay Integration
- [ ] Create `FullScreenOverlayActivity` with transparent blurple background
- [ ] Pass widget dimensions and grid state via Intent extras
- [ ] Render interactive grid that perfectly overlays widget position
  - Calculate widget bounds using `AppWidgetManager.getAppWidgetOptions()`
  - Position overlay grid to match using window insets
- [ ] Implement close gesture (back button + tap outside)
- [ ] Test seamless transition (no flicker, instant appearance)

### Phase 4: Widget Manipulation Compatibility
- [ ] Verify long-press still triggers native widget move/resize
  - May need to use `setOnLongClickListener()` that returns `false` to propagate
- [ ] Test on Samsung One UI (known for custom launcher behavior)
- [ ] Test on Pixel Launcher (stock Android)

## Critical Risks & Mitigations

| Risk | Impact | Likelihood | Mitigation |
|------|--------|------------|------------|
| RemoteViews can't detect touch coordinates | **BLOCKER** | Medium | Use grid of separate PendingIntents per cell (tested pattern) |
| Pulse animation causes lag | High | Low | Pre-render pulsed bitmaps, cache in memory |
| Long-press conflicts with our tap detection | High | Medium | Set short long-press threshold (500ms) and return false to propagate |
| Overlay grid doesn't align with widget | Medium | High | Use AppWidgetOptions to get exact bounds, account for launcher padding |
| Blurple backdrop feels jarring | Low | Medium | A/B test with user, add fade-in transition |

## Implementation Strategy

### Minimal Validation Prototype
**Goal**: Prove the interaction model works before building full features

**Scope**:
1. Static 4x4 grid widget (hardcoded colored rectangles, no real data)
2. PendingIntent per cell that passes cell coordinates
3. Simple overlay that shows which cell was tapped
4. Pulse animation on touch (toggle between two pre-rendered bitmaps)
5. Long-press test (verify widget can still be moved)

**Success Criteria**:
- Tap cell → see pulse → overlay opens instantly (< 150ms perceived)
- Long-press → Android's widget manipulation appears within 500ms
- Overlay grid visually aligns within 5dp of widget position
- No crashes on Samsung Galaxy A-series, Pixel 6+

**Estimated Effort**: 2-3 days

### Code Structure Preview

```kotlin
// GridWidgetProvider.kt
class GridWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, widgetIds: IntArray) {
        widgetIds.forEach { widgetId ->
            val views = RemoteViews(context.packageName, R.layout.widget_grid)

            // Create PendingIntent per cell
            for (row in 0..3) {
                for (col in 0..3) {
                    val cellId = getCellViewId(row, col)
                    val intent = createCellTapIntent(context, widgetId, row, col)
                    views.setOnClickPendingIntent(cellId, intent)
                }
            }

            manager.updateAppWidget(widgetId, views)
        }
    }

    private fun createCellTapIntent(
        context: Context,
        widgetId: Int,
        row: Int,
        col: Int
    ): PendingIntent {
        val intent = Intent(context, FullScreenOverlayActivity::class.java).apply {
            putExtra("widgetId", widgetId)
            putExtra("cellRow", row)
            putExtra("cellCol", col)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION
        }

        return PendingIntent.getActivity(
            context,
            requestCode = generateUniqueCode(widgetId, row, col),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}

// FullScreenOverlayActivity.kt
class FullScreenOverlayActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Get tapped cell info
        val row = intent.getIntExtra("cellRow", -1)
        val col = intent.getIntExtra("cellCol", -1)
        val widgetId = intent.getIntExtra("widgetId", -1)

        setContent {
            FullScreenOverlay(
                widgetId = widgetId,
                tappedCell = Pair(row, col),
                onClose = { finish() }
            )
        }
    }
}

@Composable
fun FullScreenOverlay(
    widgetId: Int,
    tappedCell: Pair<Int, Int>,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xE61A0F2E)) // Deep blurple, 90% opacity
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onClose() }) // Tap outside to close
            }
    ) {
        // TODO: Render grid that aligns with widget position
        GridOverlay(
            rows = 4,
            cols = 4,
            highlightedCell = tappedCell
        )
    }
}
```

## Next Steps
1. ✅ Document validation plan (this file)
2. ⏭️ Create minimal Android project structure
3. ⏭️ Implement static widget with per-cell PendingIntents
4. ⏭️ Build FullScreenOverlayActivity with blurple backdrop
5. ⏭️ Test on physical Samsung device
6. ⏭️ Measure and optimize response times
7. ⏭️ Document findings and adjust MVP plan accordingly

## Expected Outcomes

**If Successful:**
- Validates core interaction model
- Proves overlay approach is viable
- Informs final architecture decisions
- Gives confidence to proceed with full MVP

**If Blockers Found:**
- Pivot to alternative interaction patterns
- Consider WebView-based widget (less ideal)
- Explore Jetpack Glance with interactivity callbacks
- Fallback: Single tap always opens app, no in-widget editing

## Timeline
- **Day 1**: Project setup + static widget rendering
- **Day 2**: PendingIntent wiring + overlay basics
- **Day 3**: Pulse animation + alignment refinement
- **Day 4**: Device testing + performance validation
- **Day 5**: Documentation + decision checkpoint

---

**Status**: Ready to begin implementation
**Owner**: Development team
**Last Updated**: 2026-01-01
