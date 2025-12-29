package com.memo.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import com.memo.widget.repository.BlockRepository
import kotlinx.coroutines.runBlocking

/**
 * Phase 1: Widget provider that renders task blocks and launches the overlay on tap.
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
            // Load blocks from repository
            val repository = BlockRepository(context)
            val blocks = runBlocking {
                try {
                    repository.getBlocks()
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to load blocks", e)
                    emptyList()
                }
            }

            Log.d(TAG, "Updating widget $appWidgetId with ${blocks.size} blocks")

        // Get widget dimensions
        val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
        val widthDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 250)
        val heightDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 250)

        // Convert DP to pixels
        val density = context.resources.displayMetrics.density
        val widthPx = (widthDp * density).toInt()
        val heightPx = (heightDp * density).toInt()

        // Render bitmap with task blocks
        val renderer = BitmapRenderer(context)
        val bitmap = renderer.renderWidgetWithBlocks(widthPx, heightPx, blocks)

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
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update widget $appWidgetId", e)
        }
    }
}
