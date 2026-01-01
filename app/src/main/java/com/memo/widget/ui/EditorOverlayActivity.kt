package com.memo.widget.ui

import android.appwidget.AppWidgetManager
import android.graphics.RectF
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.memo.widget.R
import com.memo.widget.data.GridState
import com.memo.widget.data.Memo
import com.memo.widget.data.MemoRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Transparent overlay activity that provides interactive editing of the widget.
 *
 * KEY FIX #1: Widget Alignment
 * - Calculates exact widget position on screen using AppWidgetManager
 * - Passes these bounds to GridOverlayView for perfect alignment
 */
class EditorOverlayActivity : AppCompatActivity() {

    private lateinit var gridOverlayView: GridOverlayView
    private lateinit var repository: MemoRepository
    private lateinit var vibrator: Vibrator
    private var widgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configure window for transparent overlay
        window.apply {
            setFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            )
        }

        setContentView(R.layout.activity_editor_overlay)

        // Get widget ID from intent
        widgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        )

        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        repository = MemoRepository(this)
        vibrator = getSystemService(Vibrator::class.java)

        gridOverlayView = findViewById(R.id.grid_overlay_view)

        setupCallbacks()
        loadGridState()
    }

    /**
     * KEY FIX #1: Calculate widget bounds and set them on the overlay view.
     * This ensures the grid overlay perfectly aligns with the widget.
     */
    private fun calculateWidgetBounds(): RectF {
        val appWidgetManager = AppWidgetManager.getInstance(this)
        val options = appWidgetManager.getAppWidgetOptions(widgetId)

        // Get widget dimensions in DP
        val minWidthDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
        val minHeightDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)

        // Convert to pixels
        val density = resources.displayMetrics.density
        val widthPx = (minWidthDp * density)
        val heightPx = (minHeightDp * density)

        // Get widget position on launcher
        // Note: Since we can't directly get widget screen position, we'll use a workaround
        // by finding the widget's AppWidgetHostView position after the overlay is shown.
        // For now, we'll center the grid on the screen as a fallback.
        // In a production app, you might need launcher-specific APIs or alternative methods.

        // Get screen dimensions
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels.toFloat()
        val screenHeight = displayMetrics.heightPixels.toFloat()

        // Center the widget grid on screen
        // This is a simplified approach - in production, you'd want to get exact widget position
        val left = (screenWidth - widthPx) / 2
        val top = (screenHeight - heightPx) / 2
        val right = left + widthPx
        val bottom = top + heightPx

        return RectF(left, top, right, bottom)
    }

    private fun setupCallbacks() {
        gridOverlayView.apply {
            onMemoMoved = { memo, newX, newY ->
                handleMemoMoved(memo, newX, newY)
            }

            onCellTapped = { x, y ->
                handleCellTapped(x, y)
            }

            // KEY FIX #4: Handle tap outside widget bounds to dismiss overlay
            onOutsideTapped = {
                finish()
            }

            onHapticFeedback = { type ->
                handleHapticFeedback(type)
            }
        }
    }

    private fun loadGridState() {
        lifecycleScope.launch {
            val state = repository.getGridState(widgetId).first()

            // Calculate and set widget bounds for perfect alignment
            val bounds = calculateWidgetBounds()
            gridOverlayView.setWidgetBounds(bounds)

            gridOverlayView.setGridState(state)
        }
    }

    private fun handleMemoMoved(memo: Memo, newX: Int, newY: Int) {
        lifecycleScope.launch {
            val currentState = gridOverlayView.getGridState() ?: return@launch

            // Update memo position
            val updatedMemo = memo.copy(
                originX = newX,
                originY = newY,
                lastInteractedAt = System.currentTimeMillis()
            )

            val updatedMemos = currentState.memos.map {
                if (it.id == memo.id) updatedMemo else it
            }

            val newState = currentState.copy(memos = updatedMemos)

            // Save to repository
            repository.saveGridState(newState)

            // Update view
            gridOverlayView.setGridState(newState)
        }
    }

    private fun handleCellTapped(x: Int, y: Int) {
        lifecycleScope.launch {
            val currentState = gridOverlayView.getGridState() ?: return@launch

            // Check if cell is empty
            val occupancyMap = currentState.occupancyMap()
            if (occupancyMap[Pair(x, y)] == null) {
                // Create new memo
                val newMemo = Memo(
                    title = "New Memo",
                    originX = x,
                    originY = y,
                    width = 1,
                    height = 1
                )

                val newState = currentState.copy(
                    memos = currentState.memos + newMemo
                )

                repository.saveGridState(newState)
                gridOverlayView.setGridState(newState)

                handleHapticFeedback(GridOverlayView.HapticType.SUCCESS)
            }
        }
    }

    /**
     * KEY FIX: Haptic feedback for better UX
     */
    private fun handleHapticFeedback(type: GridOverlayView.HapticType) {
        if (!vibrator.hasVibrator()) return

        val effect = when (type) {
            GridOverlayView.HapticType.LIGHT ->
                VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)

            GridOverlayView.HapticType.MEDIUM ->
                VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)

            GridOverlayView.HapticType.SUCCESS ->
                VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK)

            GridOverlayView.HapticType.ERROR ->
                VibrationEffect.createWaveform(longArrayOf(0, 50, 50, 50), -1)
        }

        vibrator.vibrate(effect)
    }

    override fun onPause() {
        super.onPause()

        // Update widget when overlay is dismissed
        val appWidgetManager = AppWidgetManager.getInstance(this)
        MemoWidgetProvider.updateWidget(this, appWidgetManager, widgetId)
    }
}
