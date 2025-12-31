package com.memo.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import com.memo.widget.repository.MemoRepository
import kotlinx.coroutines.runBlocking

/**
 * Phase 1: Widget provider that renders memos and launches the overlay on tap.
 * Aligned with Phase 1 specification.
 */
class MemoWidgetProvider : AppWidgetProvider() {

    companion object {
        private const val TAG = "MemoWidget"
        const val ACTION_REFRESH = "com.memo.widget.REFRESH"

        /**
         * Trigger widget update from EditorOverlayActivity
         */
        fun updateWidget(context: Context, filledCells: Set<Int>) {
            val intent = Intent(context, MemoWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(EXTRA_FILLED_CELLS, filledCells.toIntArray())
            }
            context.sendBroadcast(intent)
        }

        private const val EXTRA_FILLED_CELLS = "filled_cells"
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        Log.d(TAG, "onUpdate called for ${appWidgetIds.size} widgets")

        appWidgetIds.forEach { appWidgetId ->
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        when (intent.action) {
            ACTION_REFRESH, Intent.ACTION_BOOT_COMPLETED -> {
                Log.d(TAG, "onReceive: ${intent.action}")
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(
                    android.content.ComponentName(context, MemoWidgetProvider::class.java)
                )
                onUpdate(context, appWidgetManager, appWidgetIds)
            }
        }
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        try {
            // Load grid state from repository
            val repository = MemoRepository(context)
            val gridState = runBlocking {
                try {
                    repository.getGridState(appWidgetId)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to load grid state", e)
                    com.memo.widget.data.GridState(widgetId = appWidgetId)
                }
            }

            Log.d(TAG, "Updating widget $appWidgetId with ${gridState.memos.size} memos")

        // Get widget dimensions
        val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
        val widthDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 250)
        val heightDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 250)

        // Convert DP to pixels
        val density = context.resources.displayMetrics.density
        val widthPx = (widthDp * density).toInt()
        val heightPx = (heightDp * density).toInt()

        // Render bitmap with memos
        val renderer = BitmapRenderer(context)
        val bitmap = renderer.renderWidgetWithMemos(widthPx, heightPx, gridState.memos)

        // Create RemoteViews with grid overlay
        val views = RemoteViews(context.packageName, R.layout.widget_memo)
        views.setImageViewBitmap(R.id.widget_image, bitmap)

        // Set up tap handlers for each grid cell (4x4 grid)
        val cellIds = arrayOf(
            // Row 0
            intArrayOf(R.id.cell_0_0, R.id.cell_1_0, R.id.cell_2_0, R.id.cell_3_0),
            // Row 1
            intArrayOf(R.id.cell_0_1, R.id.cell_1_1, R.id.cell_2_1, R.id.cell_3_1),
            // Row 2
            intArrayOf(R.id.cell_0_2, R.id.cell_1_2, R.id.cell_2_2, R.id.cell_3_2),
            // Row 3
            intArrayOf(R.id.cell_0_3, R.id.cell_1_3, R.id.cell_2_3, R.id.cell_3_3)
        )

        for (row in 0 until 4) {
            for (col in 0 until 4) {
                val intent = Intent(context, EditorOverlayActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra("WIDGET_ID", appWidgetId)
                    putExtra("CELL_X", col)
                    putExtra("CELL_Y", row)
                }
                val pendingIntent = PendingIntent.getActivity(
                    context,
                    appWidgetId * 100 + row * 4 + col, // Unique request code for each cell
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(cellIds[row][col], pendingIntent)
            }
        }

            // Update widget
            appWidgetManager.updateAppWidget(appWidgetId, views)
            Log.d(TAG, "Widget $appWidgetId updated successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update widget $appWidgetId", e)
        }
    }
}
