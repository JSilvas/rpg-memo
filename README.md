# Memo - Retro RPG Inventory Widget

**Phase 0: Technical Validation Spike**

A digital sticky note widget for Android that enforces prioritization through spatial constraints. Inspired by retro RPG inventory systems (Resident Evil 4, Deus Ex), users manage tasks as physical "blocks" on a grid.

## 🎯 Phase 0 Goal

**Validate that the Bitmap-rendered widget + transparent overlay pattern feels native on Samsung devices.**

This is the **make-or-break validation**. If the overlay flickers, lags, or Samsung One UI does something weird, the entire product concept needs rethinking.

## 🏗️ What's Built

### Core Components

1. **BitmapRenderer** - Renders 4x4 grid as static Bitmap
   - Retro RPG color palette
   - Rounded corners, cell padding
   - Filled/unfilled cell states

2. **MemoWidgetProvider** - AppWidgetProvider implementation
   - Single ImageView with rendered Bitmap
   - Tap launches overlay
   - Widget regeneration on update

3. **EditorOverlayActivity** - **CRITICAL COMPONENT**
   - Transparent theme (no animation)
   - Jetpack Compose Canvas for interaction
   - Tap cell to toggle fill color
   - Drag test with 60fps logging
   - Background dimming

### Tech Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose (overlay only)
- **Min SDK**: 26 (Android 8.0)
- **Target SDK**: 34 (Android 14)
- **Persistence**: SharedPreferences (stub, DataStore in Phase 1)

## 🚀 Build & Test

### Prerequisites

- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17
- Samsung device running One UI (preferred) or any Android 8.0+ device

### Build Steps

```bash
# Clone repo
git clone <repo-url>
cd rpg-memo

# Build debug APK
./gradlew assembleDebug

# Install on connected device
./gradlew installDebug
```

### Testing Protocol

**THIS IS THE CRITICAL VALIDATION. Test thoroughly.**

1. **Widget Installation**
   - Long-press home screen
   - Add Memo widget (4x4 grid)
   - Verify bitmap renders correctly

2. **Overlay Launch Test**
   - Tap widget → overlay should appear **instantly**
   - ❌ FAIL if: flicker, lag, animation glitch
   - ✅ PASS if: seamless, feels native

3. **Interaction Test**
   - Tap cells to toggle fill color
   - Verify instant visual feedback
   - Close overlay (tap outside or back button)
   - Verify widget updates with new state

4. **60fps Drag Test** (CRITICAL)
   - Open overlay
   - Drag finger across grid continuously
   - Check logcat for FPS logs:
     ```bash
     adb logcat -s EditorOverlay:D
     ```
   - ❌ FAIL if: FPS < 55
   - ✅ PASS if: FPS consistently 58-60

5. **Samsung One UI Specific**
   - Test on Samsung device with One UI
   - Check for Samsung-specific rendering quirks
   - Verify transparent overlay looks native
   - Test with Samsung's edge panels active

### Logcat Monitoring

```bash
# Widget updates
adb logcat -s MemoWidget:D

# Overlay + FPS
adb logcat -s EditorOverlay:D

# All app logs
adb logcat -s com.memo.widget:D
```

## 📊 Success Criteria

Phase 0 is **SUCCESSFUL** if:

- ✅ Overlay launches with **zero perceptible delay**
- ✅ No flicker, animation glitches, or visual artifacts
- ✅ Cell taps register **instantly** (< 16ms latency)
- ✅ Drag operations maintain **60fps**
- ✅ Widget updates reflect overlay changes
- ✅ Feels native on Samsung One UI

Phase 0 **FAILS** if:

- ❌ Any visible lag or stutter
- ❌ Samsung One UI does weird rendering
- ❌ FPS drops below 55 during drag
- ❌ Widget/overlay state desync
- ❌ Doesn't feel "instant" and native

## 🔍 Key Files

| File | Purpose |
|------|---------|
| `BitmapRenderer.kt` | Renders 4x4 grid bitmap |
| `MemoWidgetProvider.kt` | Widget lifecycle management |
| `EditorOverlayActivity.kt` | **Critical overlay validation** |
| `themes.xml` | Transparent overlay theme config |
| `AndroidManifest.xml` | Widget + activity registration |

## 🐛 Known Phase 0 Limitations

- No actual task data (just colored cells)
- No drag-to-resize blocks
- No persistence beyond app restart
- Simple SharedPreferences (not DataStore)
- No animation polish
- Placeholder launcher icon

**This is intentional.** Phase 0 is purely technical validation.

## 📝 Next Steps (If Phase 0 Passes)

**Phase 1**: Task Block Data Model
- DataStore persistence
- Block shapes (1x1, 2x1, 2x2, etc.)
- Grid collision detection
- Drag-to-place blocks

**Phase 2**: Polish & Features
- Animations (if they don't break 60fps)
- Task text rendering in blocks
- Multi-widget support
- Settings screen

## ⚠️ Critical Notes

1. **No feature creep in Phase 0**
   - Only validate overlay pattern
   - Don't add task editing yet
   - Don't add animations yet

2. **Samsung One UI is primary target**
   - Test on real Samsung hardware
   - Emulator won't reveal UI quirks

3. **60fps is non-negotiable**
   - If we can't hit 60fps now, we never will
   - Bitmap rendering must be fast

## 📄 License

TBD

---

**Built with urgency. The entire product hinges on this overlay feeling seamless.**
