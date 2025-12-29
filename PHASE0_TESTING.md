# Phase 0 Testing Guide

## 🎯 What We're Validating

**The entire product hinges on whether the transparent overlay feels seamless.**

If it flickers, lags, or Samsung One UI does something weird, we need to know NOW before building features on top of this pattern.

## 🧪 Test Environment

### Required Hardware
- **Primary**: Samsung device with One UI (Galaxy S21+, S22+, etc.)
- **Secondary**: Any Android 8.0+ device for baseline testing

### Why Samsung Specifically?
Samsung's One UI adds custom rendering layers, edge panels, and system-wide theming that can cause unexpected behavior with transparent activities. If it works on Samsung, it works everywhere.

## 📋 Testing Checklist

### Test 1: Widget Installation ✅

**Goal**: Verify bitmap renders correctly

1. Long-press home screen
2. Select "Widgets"
3. Find "Memo" widget
4. Drag to home screen (4x4 size)

**Expected Result**:
- 4x4 grid appears with dark background
- All cells are empty (dark blue/gray)
- Rounded corners visible
- No pixelation or scaling artifacts

**Pass Criteria**:
- ✅ Widget renders cleanly
- ✅ Grid cells are evenly spaced
- ✅ Colors match retro palette

### Test 2: Overlay Launch (CRITICAL) ⚡

**Goal**: Verify overlay launches with zero perceptible delay

1. Tap widget anywhere
2. Observe overlay appearance

**Expected Result**:
- Overlay appears **instantly**
- No white flash
- No loading screen
- Background dims smoothly
- 4x4 grid appears centered

**Pass Criteria**:
- ✅ Launch feels native (< 100ms)
- ✅ No visual glitches
- ✅ No Samsung One UI artifacts

**FAIL Criteria**:
- ❌ Any perceptible delay (> 200ms)
- ❌ Flicker or flash
- ❌ Animation stutter
- ❌ Grid not centered

### Test 3: Cell Interaction 👆

**Goal**: Verify tap responsiveness

1. With overlay open, tap any cell
2. Cell should turn red immediately
3. Tap same cell again
4. Cell should turn dark gray/blue
5. Repeat for multiple cells

**Expected Result**:
- Tap registers instantly
- Color change is immediate
- No lag between tap and visual feedback

**Pass Criteria**:
- ✅ < 16ms latency (1 frame @ 60fps)
- ✅ Color toggles correctly
- ✅ Multiple taps work consistently

### Test 4: 60fps Drag Test (CRITICAL) 🚀

**Goal**: Validate sustained 60fps during drag operations

**Setup**:
```bash
# Connect device via USB
adb devices

# Monitor FPS logs
adb logcat -s EditorOverlay:D
```

**Test Procedure**:
1. Open overlay
2. Drag finger continuously across grid in circles
3. Continue for 10-15 seconds
4. Watch logcat for FPS output

**Expected Logcat Output**:
```
EditorOverlay: FPS: 60 (target: 60)
EditorOverlay: FPS: 59 (target: 60)
EditorOverlay: FPS: 60 (target: 60)
```

**Pass Criteria**:
- ✅ FPS consistently 58-60
- ✅ No drops below 55
- ✅ Smooth visual motion

**FAIL Criteria**:
- ❌ FPS < 55 at any point
- ❌ Visible stuttering
- ❌ Lag during drag

### Test 5: State Persistence 💾

**Goal**: Verify widget updates reflect overlay changes

1. Open overlay
2. Toggle 3-5 cells to red
3. Close overlay (tap outside or back button)
4. Observe widget

**Expected Result**:
- Widget immediately shows red cells
- Cell positions match what was toggled
- No delay in widget update

**Pass Criteria**:
- ✅ Widget updates < 1 second
- ✅ State matches overlay
- ✅ Survives app restart

### Test 6: Samsung One UI Stress Test 📱

**Samsung-Specific Issues to Check**:

**Edge Panels**:
1. Enable Samsung edge panel
2. Open overlay
3. Swipe in from edge
4. Verify overlay doesn't break

**Dark Mode**:
1. Toggle Samsung dark mode
2. Verify overlay still dims background
3. Check grid colors remain consistent

**Gesture Navigation**:
1. Use Samsung gesture nav
2. Swipe up from bottom while overlay open
3. Verify no rendering glitches

**One-Handed Mode**:
1. Enable one-handed mode
2. Open overlay
3. Verify grid centers correctly

**Pass Criteria**:
- ✅ No Samsung-specific rendering bugs
- ✅ Overlay works with all One UI features
- ✅ No edge panel conflicts

## 🐛 Common Issues & Fixes

### Issue: White Flash on Launch
**Cause**: Window background not transparent
**Check**: `themes.xml` has `windowBackground=@android:color/transparent`

### Issue: Laggy Drag
**Cause**: Canvas not optimized
**Check**: Logcat for FPS < 55
**Action**: FAIL Phase 0 - optimization needed

### Issue: Cells Don't Toggle
**Cause**: Touch detection broken
**Check**: Tap coordinates calculation
**Action**: Debug `detectTapGestures` in `EditorOverlayActivity.kt`

### Issue: Widget Doesn't Update
**Cause**: Broadcast not received
**Check**: Logcat for `MemoWidget:D` logs
**Action**: Verify `ACTION_REFRESH` intent sent

## 📊 Success Decision Matrix

| Test | Weight | Pass Threshold |
|------|--------|----------------|
| Overlay Launch | **CRITICAL** | < 100ms, no flicker |
| 60fps Drag | **CRITICAL** | 55+ fps sustained |
| Cell Interaction | High | < 16ms latency |
| Widget Update | High | < 1s update |
| Samsung Compat | High | No UI-specific bugs |
| State Persist | Medium | Works after restart |

**Phase 0 PASSES if**:
- ALL critical tests pass
- ≥ 80% of high-priority tests pass
- No Samsung One UI blockers

**Phase 0 FAILS if**:
- ANY critical test fails
- FPS < 55 during drag
- Samsung One UI renders incorrectly
- Overlay doesn't feel instant

## 📝 Test Report Template

```markdown
## Phase 0 Test Results

**Device**: Samsung Galaxy S__ / Android __
**One UI Version**: __
**Date**: YYYY-MM-DD

### Test Results

- [ ] Widget Installation: PASS / FAIL
- [ ] Overlay Launch: PASS / FAIL (latency: __ ms)
- [ ] Cell Interaction: PASS / FAIL
- [ ] 60fps Drag: PASS / FAIL (FPS: __)
- [ ] State Persistence: PASS / FAIL
- [ ] Samsung Compat: PASS / FAIL

### Notes

(Any visual artifacts, lag, or Samsung-specific issues)

### Overall Decision

**PASS** / **FAIL** - Proceed to Phase 1: YES / NO
```

## 🎬 Next Steps

### If Phase 0 PASSES ✅
1. Document exact device/OS that passed
2. Proceed to Phase 1 (data model)
3. Keep overlay pattern as-is

### If Phase 0 FAILS ❌
1. Document exact failure mode
2. Consider alternatives:
   - Full-screen activity (no transparency)
   - Bottom sheet approach
   - Different rendering strategy
3. **DO NOT** proceed to Phase 1

---

**Remember**: This is validation, not implementation. If it doesn't feel perfect now, it never will.
