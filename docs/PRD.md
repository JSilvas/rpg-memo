# Product Requirements Document (PRD)

**Project Name:** Memo Widget (Android)  
**Version:** 1.1  
**Status:** Ready for Development  
**Platform:** Android (Samsung One UI optimized)  
**Last Updated:** December 2024

---

## 1. Executive Summary

Memo is a "digital sticky note" widget for Android that enforces prioritization through spatial constraints. Inspired by retro RPG inventory systems (e.g., Resident Evil 4, Deus Ex), users manage tasks as physical "blocks" on a grid. There is no scrolling; if a task doesn't fit, something else must be completed or deleted. It lives entirely on the Home Screen as a native widget.

The core philosophy: **Spatial pressure is a feature, not a limitation.** The constraint of finite space forces users to continuously triage what matters.

---

## 2. Problem Statement

Traditional to-do lists allow infinite accumulation of tasks, leading to "backlog fatigue." Users need a persistent, always-visible reminder system that:

- Visually represents the "weight" of tasks
- Forces decluttering of mental workspace through physical screen constraints
- Provides satisfaction through completion, not just accumulation
- Creates accountability through visual aging of neglected items

---

## 3. Core User Experience

The application consists of two distinct UI states due to Android Widget technical limitations regarding touch gestures:

| State | Description |
|-------|-------------|
| **Widget (View Mode)** | A static, non-scrolling Bitmap snapshot of the grid on the home screen |
| **Editor (Interactive Mode)** | A transparent-background Activity that overlays the screen when the widget is tapped, enabling drag-and-drop, resizing, and editing |

---

## 4. Interaction Model

### 4.1 Gesture Reference

| Gesture | Target | Result |
|---------|--------|--------|
| **Tap** | Empty cell | Create 1x1 block → opens Edit Modal |
| **Tap** | Existing block | Opens Edit Modal (title, description, emoji) |
| **Drag** | Empty cells | Drag-to-create defines initial block size |
| **Drag** | Existing block | Move block (Tetris-style repositioning) |
| **Long-press (hold)** | Existing block | "Bubble up" radial action menu (Check ✓ / Transfer ↗ / Delete ✕) with memo title shown. Animation similar to Messenger's growing emoji reaction. |
| **Corner grab** | Existing block | Resize handle appears, drag to expand/contract |
| **Multi-select toggle** | Bottom bar button | Enter selection mode, tap blocks to batch select |

### 4.2 Radial Action Menu

When long-pressing a block, a radial menu bubbles up (expanding animation) with three actions:

- **✓ Check (Complete)** — Triggers completion animation, increments counter
- **↗ Transfer** — Opens system share sheet with Markdown export
- **✕ Delete** — Removes block with animation, increments counter

The memo title is displayed above/below the radial menu for context.

### 4.3 Edit Modal

Triggered by tapping any block (new or existing):

```
┌─────────────────────────────────┐
│  [Emoji Picker] 📌              │
│                                 │
│  Title ________________________ │
│                                 │
│  Description (optional)         │
│  ______________________________ │
│  ______________________________ │
│                                 │
│              [Save]             │
└─────────────────────────────────┘
```

- **Emoji:** Optional. Renders on block when size is 1x1 (text hidden, emoji only). Larger blocks show emoji in corner with truncated title.
- **Title:** Required. Auto-scales or truncates based on block size.
- **Description:** Optional. Expandable text area.
- **Dismiss:** Tap Save button or tap outside modal to save and close.

---

## 5. Functional Requirements

### 5.1 Widget (Home Screen)

| Requirement | Description |
|-------------|-------------|
| **Flexible Sizing** | Support Android's standard resizing handles (Samsung One UI compliant) |
| **Adaptive Grid** | Internal grid resolution (e.g., 4x4, 6x4) scales based on widget's physical size |
| **Bitmap Rendering** | Displays current state as a single pre-rendered Bitmap image |
| **Tap Interaction** | Tapping anywhere on widget launches Editor Overlay |
| **Resize Blocking** | If widget resize would displace existing memos, resize is blocked with haptic rejection. User must triage in Editor first. |

### 5.2 Editor Overlay (Interactive Mode)

| Requirement | Description |
|-------------|-------------|
| **Transparent Canvas** | Launches instantly without animation. Background shows user's wallpaper (dimmed at 60% opacity). |
| **Add Memo** | Tap empty cell for 1x1, or drag across cells to define initial size |
| **Move Memo** | Drag and drop blocks. "Inventory Tetris" — manual organization required. |
| **Resize Memo** | Long-press or corner handle grab to expand/contract. Stops at collision boundaries. |
| **Edit Content** | Tap block to open Edit Modal |
| **Multi-Select** | Toggle in bottom bar. Tap blocks to select. Batch delete or share selected items. |

### 5.3 Grid System

**Position Model: Absolute Cell Coordinates**

- A 2x1 block at position (0,0) occupies cells (0,0) and (1,0)
- Positions are stored as integer coordinates, not percentages
- Collision detection prevents overlapping blocks
- Grid bounds enforce blocks cannot exceed widget dimensions

**Grid Full State:**
- When grid is 100% occupied and user taps an empty area (impossible) or attempts to add a new memo
- All blocks **jiggle** animation
- **Double-beat haptic vibration** (two quick pulses)
- This is the app working as intended — forces triage

### 5.4 Block Aging (Visual Decay)

Blocks visually age based on time since last interaction, creating "shame" for neglected items:

| Days Since Interaction | Aging Tier | Visual Effect |
|------------------------|------------|---------------|
| 0-2 days | Tier 0 (Fresh) | 100% opacity, clean appearance |
| 3-4 days | Tier 1 (Slight fade) | 85% opacity |
| 5-6 days | Tier 2 (Visible wear) | 70% opacity + paper texture overlay |
| 7+ days | Tier 3 (Maximum decay) | 60% opacity + crinkle/crack effect |

**Reset Trigger:** Any interaction (move, resize, edit, or explicit touch) resets the `lastInteractedAt` timestamp and refreshes appearance.

**Important:** Aging is visual only — does not auto-delete. Maximum decay state persists indefinitely until user action.

### 5.5 Completion Animation

When user taps ✓ Check in radial menu:

1. Block color shifts to "Done Green"
2. Checkmark icon overlays the block
3. **Linger for ~400ms** (satisfying pause)
4. Block **dissolves** (particle poof or fold-away animation)
5. Completed counter increments with subtle bounce animation
6. Satisfying haptic feedback

### 5.6 Stats Counters

Persistent counters displayed in Editor bottom bar:

| Icon | Stat | Trigger |
|------|------|---------|
| ✓ | Completed | Block marked complete via radial menu |
| ↗ | Transferred | Block shared via system share sheet |
| ✕ | Deleted | Block removed via radial menu or multi-select |

Counters persist across sessions as lifetime stats for the widget instance.

### 5.7 Transfer/Share

When user taps ↗ Transfer in radial menu (or batch shares via multi-select):

1. Generate Markdown formatted content:
   ```markdown
   - [Title]: [Description]
   - [Title]: [Description]
   ```
2. Open **System Share Sheet**
3. User selects destination (Notion, Telegram, Notes, etc.)
4. On successful share, increment Transferred counter
5. Original block remains (user can then complete or delete)

### 5.8 Data & State

| Requirement | Description |
|-------------|-------------|
| **Persistence** | Auto-save on every change using DataStore |
| **Zero-Scroll Policy** | Hard limit on items based on available grid cells. No "Show More" or overflow. |
| **Widget Instance Isolation** | Each widget instance has independent GridState and Stats |

---

## 6. Technical Implementation

### 6.1 The "Bitmap Snapshot" Rendering Pattern

**Constraint:** Android RemoteViews (widget technology) does not support drag-and-drop, complex touch gestures, or overlapping views.

**Solution:**
- Widget does not render individual layout files per block
- Background service generates a **single Bitmap** of the entire grid state
- Bitmap is pushed to Widget's ImageView
- Regeneration triggers:
  - On every frame during Editor drag operations (smooth UX priority)
  - On Editor close (final crisp render)
  - On widget resize

**Pros:** Pixel-perfect "RPG Inventory" aesthetics, non-standard grid layouts, zero lag on home screen.

**Cons:** No individual button clicks on widget (solved by Overlay strategy).

### 6.2 The Transparent Overlay Activity

```kotlin
// Launch from widget
val intent = Intent(context, EditorOverlayActivity::class.java).apply {
    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION
}
```

```xml
<!-- Theme configuration -->
<style name="TransparentOverlay" parent="Theme.Material3.DayNight.NoActionBar">
    <item name="android:windowIsTranslucent">true</item>
    <item name="android:windowBackground">@android:color/transparent</item>
    <item name="android:windowNoTitle">true</item>
    <item name="android:windowAnimationStyle">@null</item>
    <item name="android:backgroundDimEnabled">true</item>
    <item name="android:backgroundDimAmount">0.6</item>
</style>
```

**Lifecycle:**
- `onCreate()` — Load GridState, render interactive Canvas
- User interactions update local state and trigger frame-by-frame Bitmap regeneration
- `onPause()` — Save state, regenerate final Bitmap, call `AppWidgetManager.updateAppWidget()`

### 6.3 Haptic Feedback Events

| Event | Pattern |
|-------|---------|
| Block picked up | Light tap |
| Block dropped | Medium tap |
| Block resized | Light tap |
| Grid full (attempted add) | Double-beat vibration |
| Action completed (check/delete) | Satisfying thunk |
| Action cancelled | Short buzz |

### 6.4 Samsung One UI Specifics

| Requirement | Implementation |
|-------------|----------------|
| **Dark Mode** | Respect One UI Dark/Light mode settings via system theme detection |
| **Corner Radius** | Match system corner radius (Android 12+ `RoundedCorner` API) |
| **Edge Panel** | Test compatibility, no special handling expected |
| **Multi-window** | Support split-screen mode for Editor overlay |

---

## 7. Data Models

### 7.1 Memo

```kotlin
data class Memo(
    val id: String,
    val title: String,
    val description: String?,
    val emoji: String?,           // Single emoji for 1x1 display
    val originX: Int,             // Top-left cell X coordinate
    val originY: Int,             // Top-left cell Y coordinate
    val width: Int,               // Width in cells (1-4)
    val height: Int,              // Height in cells (1-4)
    val colorHex: String,         // Block color
    val createdAt: Long,
    val lastInteractedAt: Long    // For aging calculation
)
```

### 7.2 GridState

```kotlin
data class GridState(
    val widgetId: Int,
    val columns: Int,
    val rows: Int,
    val memos: List<Memo>,
    val stats: Stats
)
```

### 7.3 Stats

```kotlin
data class Stats(
    val completedCount: Int,
    val transferredCount: Int,
    val deletedCount: Int
)
```

---

## 8. User Interface Guidelines

| Element | Specification |
|---------|---------------|
| **Aesthetic** | "Utility Minimalist" — high contrast lines, solid block colors |
| **Color Palette** | Post-it Yellow (#FFE066), Alert Red (#E67B7B), Done Green (#2CD9C5), Sky Blue (#6A9CD6), Lavender (#B794F6) |
| **Typography** | Monospace font (JetBrains Mono or Roboto Mono) for "Data/Inventory" feel |
| **Haptics** | Enabled by default, respects system haptic settings |
| **Animations** | Block pickup (scale up + shadow), drop (bounce settle), completion (color shift → linger → dissolve) |

---

## 9. MVP Build Phases

### Phase 0: Technical Validation Spike (3-5 days)

Validate the Bitmap + Overlay pattern works on Samsung devices before building features.

**Deliverables:**
1. Minimal 4x4 grid widget rendering static Bitmap
2. Transparent overlay Activity launching without animation
3. Interactive Canvas with tap-to-fill-color
4. Bitmap regeneration on overlay close
5. Frame-rate logging during drag (target: 60fps)

**Success Criteria:**
- Widget Bitmap updates within 100ms of overlay close
- No visible flash on overlay launch
- Drag operations maintain 60fps on Samsung Galaxy A-series

### Phase 1: Core Interaction Loop (1-2 weeks)

Minimum feature set for daily usability.

**Features:**
- Create 1x1 memo (tap empty cell)
- Edit Modal (title only)
- Move memo (drag and drop)
- Delete memo (long-press radial menu)
- DataStore persistence
- Basic block rendering

### Phase 2: Polish & Delight (1-2 weeks)

**P0 - Essential:**
- Block resizing
- Full haptic feedback
- Completion animation flow
- Stats counters

**P1 - Enhancement:**
- Aging visuals
- Full Edit Modal (title, description, emoji)
- Transfer/Share
- Multi-select mode

**P2 - Refinement:**
- Color customization
- Animation polish
- Dark mode support

### Phase 3: Production Hardening (1 week)

- Widget resize handling
- Error recovery
- Performance optimization
- Samsung-specific testing
- Accessibility support

---

## 10. Definition of Done (MVP)

A user can:

- [ ] Place the widget on their Samsung home screen
- [ ] Tap to open overlay without visible transition
- [ ] Create a new 1x1 memo by tapping an empty cell
- [ ] Edit the memo title
- [ ] Drag the memo to a new cell
- [ ] Delete the memo via long-press radial menu
- [ ] See changes reflected on widget after closing overlay
- [ ] Have memos persist across phone restarts

---

## 11. Risk Register

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| Transparent overlay feels janky | Medium | High | Phase 0 validates first |
| Bitmap generation too slow | Low | Medium | Off-thread rendering, caching |
| Samsung kills background process | Medium | Low | DataStore persists immediately |
| Widget resize displaces memos | Certain | Medium | Block resize with haptic rejection |
| Touch targets too small on 4x4 | Medium | Medium | Minimum widget size enforcement |

---

## 12. Future Considerations (Post-MVP)

- **Themes:** Additional color palettes (Dark Mode native, Pastel, Neon)
- **Widgets:** Multiple independent widget instances with different grids
- **Sync:** Optional cloud backup/sync across devices
- **Integrations:** Direct Notion/Todoist import
- **Wear OS:** Companion watch widget for quick capture
- **Automation:** Tasker/IFTTT integration for auto-creating memos

---

## Appendix A: Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                        HOME SCREEN                               │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │              Widget (RemoteViews + ImageView)            │    │
│  │                   [Rendered Bitmap]                      │    │
│  └─────────────────────────────────────────────────────────┘    │
└─────────────────────────┬───────────────────────────────────────┘
                          │ Tap
                          ▼
┌─────────────────────────────────────────────────────────────────┐
│                    EDITOR OVERLAY ACTIVITY                       │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │              Interactive Canvas (Custom View)            │    │
│  │         [Drag, Resize, Tap → Edit Modal]                │    │
│  └─────────────────────────────────────────────────────────┘    │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │   Bottom Bar: [Multi-select] | ✓ 12 | ↗ 5 | ✕ 8        │    │
│  └─────────────────────────────────────────────────────────┘    │
└─────────────────────────┬───────────────────────────────────────┘
                          │ onPause()
                          ▼
┌─────────────────────────────────────────────────────────────────┐
│                        DATA LAYER                                │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │
│  │  GridState   │  │  MemoRepo    │  │    Stats     │          │
│  │  (DataStore) │  │   (CRUD)     │  │  (Counters)  │          │
│  └──────────────┘  └──────────────┘  └──────────────┘          │
└─────────────────────────┬───────────────────────────────────────┘
                          │ onChange
                          ▼
┌─────────────────────────────────────────────────────────────────┐
│                      RENDER ENGINE                               │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │
│  │ BitmapGen    │◄─│ AgingCalc    │◄─│ BlockRender  │          │
│  │              │  │ (decay tier) │  │ (colors,tex) │          │
│  └──────────────┘  └──────────────┘  └──────────────┘          │
└─────────────────────────┬───────────────────────────────────────┘
                          │ Bitmap
                          ▼
┌─────────────────────────────────────────────────────────────────┐
│                    ANDROID SYSTEM                                │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │
│  │ AppWidget    │  │ ShareSheet   │  │   Haptics    │          │
│  │ Manager      │  │  (Markdown)  │  │  (Vibrator)  │          │
│  └──────────────┘  └──────────────┘  └──────────────┘          │
└─────────────────────────────────────────────────────────────────┘
```

---

## Appendix B: Revision History

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | Dec 2024 | Initial draft |
| 1.1 | Dec 2024 | Added: Interaction model detail, radial action menu, Edit Modal spec, aging/decay system, completion animation, stats counters, absolute grid positioning, resize blocking, haptic patterns, technical implementation details, MVP phases, data models |