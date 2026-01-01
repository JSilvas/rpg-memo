package com.memo.widget.ui

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.memo.widget.R
import com.memo.widget.data.MemoRepository
import com.memo.widget.render.RenderEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Widget provider that handles widget lifecycle and rendering.
 */
class MemoWidgetProvider : AppWidgetProvider() {

    private val scope = CoroutineScope(Dispatchers.Main)

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { widgetId ->
            updateWidget(context, appWidgetManager, widgetId)
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        val repository = MemoRepository(context)
        scope.launch {
            appWidgetIds.forEach { widgetId ->
                repository.deleteGridState(widgetId)
            }
        }
    }

    companion object {
        /**
         * Updates a specific widget instance.
         * This is called both from onUpdate and from the overlay activity.
         */
        fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, widgetId: Int) {
            val repository = MemoRepository(context)
            val renderEngine = RenderEngine()

            CoroutineScope(Dispatchers.IO).launch {
                // Load grid state
                val gridState = repository.getGridStateSync(widgetId)

                // Get widget dimensions
                val options = appWidgetManager.getAppWidgetOptions(widgetId)
                val widthPx = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH) * 4
                val heightPx = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT) * 4

                // Render bitmap
                val result = renderEngine.render(
                    gridState,
                    if (widthPx > 0) widthPx else 1000,
                    if (heightPx > 0) heightPx else 1000
                )

                // Update widget on main thread
                CoroutineScope(Dispatchers.Main).launch {
                    val views = RemoteViews(context.packageName, R.layout.widget_memo)
                    views.setImageViewBitmap(R.id.widget_grid_image, result.bitmap)

                    // Set click intent to open overlay
                    val intent = Intent(context, EditorOverlayActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                    }

                    val pendingIntent = PendingIntent.getActivity(
                        context,
                        widgetId,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    views.setOnClickPendingIntent(R.id.widget_grid_image, pendingIntent)

                    appWidgetManager.updateAppWidget(widgetId, views)
                }
            }
        }
    }
}
