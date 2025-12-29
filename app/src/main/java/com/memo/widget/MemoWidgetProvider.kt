package com.memo.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews

/**
 * Phase 0: Widget provider that renders a static Bitmap and launches the overlay on tap.
 * Critical test: Does the transparent overlay feel native on Samsung One UI?
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

        // Get filled cells from storage (stubbed for Phase 0)
        val filledCells = getFilledCells(context)

        appWidgetIds.forEach { appWidgetId ->
            updateAppWidget(context, appWidgetManager, appWidgetId, filledCells)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        when (intent.action) {
            ACTION_REFRESH -> {
                val filledCells = intent.getIntArrayExtra(EXTRA_FILLED_CELLS)?.toSet() ?: emptySet()
                saveFilledCells(context, filledCells)

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
        appWidgetId: Int,
        filledCells: Set<Int>
    ) {
        Log.d(TAG, "Updating widget $appWidgetId with ${filledCells.size} filled cells")

        // Get widget dimensions (use default 4x4 cells for Phase 0)
        val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
        val widthDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 250)
        val heightDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 250)

        // Convert DP to pixels
        val density = context.resources.displayMetrics.density
        val widthPx = (widthDp * density).toInt()
        val heightPx = (heightDp * density).toInt()

        // Render bitmap
        val renderer = BitmapRenderer(context)
        val bitmap = renderer.renderWidget(widthPx, heightPx, filledCells)

        // Create RemoteViews with single ImageView
        val views = RemoteViews(context.packageName, R.layout.widget_memo)
        views.setImageViewBitmap(R.id.widget_image, bitmap)

        // Set up tap to launch overlay
        val intent = Intent(context, EditorOverlayActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_image, pendingIntent)

        // Update widget
        appWidgetManager.updateAppWidget(appWidgetId, views)
        Log.d(TAG, "Widget $appWidgetId updated successfully")
    }

    // Phase 0: Stub persistence (use SharedPreferences for simplicity)
    private fun getFilledCells(context: Context): Set<Int> {
        val prefs = context.getSharedPreferences("memo_phase0", Context.MODE_PRIVATE)
        val cellsString = prefs.getString("filled_cells", "") ?: ""
        return if (cellsString.isEmpty()) {
            emptySet()
        } else {
            cellsString.split(",").mapNotNull { it.toIntOrNull() }.toSet()
        }
    }

    private fun saveFilledCells(context: Context, cells: Set<Int>) {
        val prefs = context.getSharedPreferences("memo_phase0", Context.MODE_PRIVATE)
        prefs.edit().putString("filled_cells", cells.joinToString(",")).apply()
    }
}
