# Memo Widget - MVP Build Plan

## Philosophy
Validate the **riskiest assumptions first**. The core bet is that a Bitmap-rendered widget + transparent overlay can feel native and responsive. Everything else is implementation detail.

---

## Phase 0: Technical Validation Spike (3-5 days)

### Goal
Prove the Bitmap + Overlay pattern works on target devices before building features.

### Deliverables
1. **Minimal Widget** 
   - 4x4 grid, hardcoded
   - Renders a static Bitmap with colored rectangles (no real memos yet)
   - Responds to resize events by regenerating Bitmap

2. **Transparent Overlay Activity**
   - Launches from widget tap
   - Theme: `android:windowIsTranslucent=true`, no animation
   - Displays same grid as an interactive Canvas
   - Tap cell → fills with random color
   - Close (back button) → regenerates widget Bitmap

3. **Frame-rate Test**
   - Drag a block around the Canvas
   - Log frame times
   - Target: 60fps sustained on Samsung Galaxy A-series (mid-tier)

### Success Criteria
- [ ] Widget Bitmap updates within 100ms of overlay close
- [ ] No visible "flash" or animation on overlay launch
- [ ] Drag operations maintain 60fps
- [ ] Samsung One UI doesn't interfere with transparent activity

### Technical Notes
```kotlin
// Widget tap intent
val intent = Intent(context, EditorOverlayActivity::class.java).apply {
    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION
}
```

```xml
<!-- EditorOverlayActivity theme -->
<style name="TransparentOverlay" parent="Theme.Material3.DayNight.NoActionBar">
    <item name="android:windowIsTranslucent">true</item>
    <item name="android:windowBackground">@android:color/transparent</item>
    <item name="android:windowNoTitle">true</item>
    <item name="android:windowAnimationStyle">@null</item>
    <item name="android:backgroundDimEnabled">true</item>
    <item name="android:backgroundDimAmount">0.6</item>
</style>
```

---

## Phase 1: Core Interaction Loop (1-2 weeks)

### Goal
Build the minimum feature set that makes Memo *usable* as a daily driver.

### Features
1. **Create Memo**
   - Tap empty cell → 1x1 block created
   - Edit Modal with title only (skip description/emoji for now)

2. **Move Memo**
   - Drag block to new position
   - Collision detection (can't overlap)
   - Visual feedback: ghost outline during drag

3. **Delete Memo**
   - Long-press → radial menu appears
   - Tap ✕ → block disappears (no animation yet)

4. **Persistence**
   - DataStore (Preferences) for MVP simplicity
   - Save on every change
   - Load on widget/overlay initialization

5. **Basic Rendering**
   - Block colors (hardcoded palette)
   - Title text rendering (auto-truncate)
   - Grid lines visible

### Out of Scope (Phase 1)
- Resizing blocks
- Aging/decay visuals
- Completion animation
- Stats counters
- Transfer/share
- Emoji support
- Multi-select

### Data Model (Simplified for Phase 1)
```kotlin
data class Memo(
    val id: String,
    val title: String,
    val x: Int,
    val y: Int,
    // width/height always 1 for now
)
```

---

## Phase 2: Polish & Delight (1-2 weeks)

### Goal
Transform functional MVP into something that *feels* premium.

### Features (Priority Order)

#### P0 - Essential Polish
1. **Block Resizing**
   - Corner handles appear on selection
   - Drag to resize (1x1 → 2x2, etc.)
   - Collision blocking on resize

2. **Haptic Feedback**
   - Pick up: light tap
   - Drop: medium tap
   - Grid full: double-beat pattern
   - Complete: satisfying thunk

3. **Completion Flow**
   - Long-press radial menu with ✓
   - Block turns green
   - 400ms linger
   - Dissolve animation (particle poof)

4. **Stats Counter**
   - Bottom bar: ✓ X | ↗ Y | ✕ Z
   - Increment on actions
   - Persist with GridState

#### P1 - Experience Enhancement
5. **Aging Visuals**
   - Calculate tier from `lastInteractedAt`
   - Tier 0: 100% opacity
   - Tier 1: 85% opacity
   - Tier 2: 70% opacity + paper texture overlay
   - Tier 3: 60% opacity + crinkle effect

6. **Edit Modal Full Version**
   - Title (required)
   - Description (optional, expandable)
   - Emoji picker (grid of common emoji)

7. **Transfer/Share**
   - Radial menu ↗ action
   - Generate Markdown: `- [title]: [description]`
   - System share sheet

8. **Multi-Select Mode**
   - Toggle in bottom bar
   - Tap blocks to select (outline highlight)
   - Batch delete / batch share

#### P2 - Visual Refinement
9. **Block Colors**
   - Color picker in Edit Modal
   - Preset palette (Post-it yellow, Alert red, Done green, Sky blue, Lavender)

10. **Animation Polish**
    - Block pickup: slight scale up + shadow
    - Drop: bounce settle
    - Resize: smooth interpolation
    - Grid full: all blocks jiggle

11. **Dark Mode Support**
    - Detect system setting
    - Invert color palette appropriately
    - Grid lines adjust

---

## Phase 3: Production Hardening (1 week)

### Goal
Ship-ready stability and edge case handling.

### Tasks
1. **Widget Resize Handling**
   - Detect resize via `onAppWidgetOptionsChanged`
   - Calculate new grid dimensions
   - If memos would be displaced: block resize OR show warning
   - Regenerate Bitmap

2. **Error Recovery**
   - Corrupted state → reset to empty grid
   - Crash recovery → restore last known good state
   - Widget removed → cleanup data

3. **Performance Optimization**
   - Bitmap caching (don't regenerate if nothing changed)
   - Off-thread rendering with Coroutines
   - Memory profiling (Bitmap recycling)

4. **Samsung-Specific Testing**
   - One UI 5 / 6 compatibility
   - Edge Panel behavior
   - Multi-window mode
   - DeX mode (optional support)

5. **Accessibility**
   - Content descriptions on blocks
   - TalkBack navigation for overlay
   - High contrast mode support

---

## Risk Register

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| Transparent overlay feels janky | Medium | High | Phase 0 validates this first |
| Bitmap generation too slow | Low | Medium | Off-thread rendering, caching |
| Samsung kills background process | Medium | Low | DataStore persists immediately |
| Widget resize displaces memos | Certain | Medium | Block resize or forced triage UX |
| Touch targets too small on 4x4 grid | Medium | Medium | Minimum widget size enforcement |

---

## Definition of Done (MVP)

A user can:
- [ ] Place the widget on their Samsung home screen
- [ ] Tap to open overlay without visible transition
- [ ] Create a new 1x1 memo by tapping an empty cell
- [ ] Edit the memo title
- [ ] Drag the memo to a new cell
- [ ] Delete the memo via long-press menu
- [ ] See their changes reflected on the widget after closing overlay
- [ ] Have their memos persist across phone restarts

Everything else is gravy.
