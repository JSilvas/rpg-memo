package com.memo.widget.provider

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.memo.widget.R
import com.memo.widget.ui.FullScreenOverlayActivity

/**
 * Widget provider for the Memo Grid.
 *
 * This validation prototype uses individual PendingIntents per grid cell
 * to detect which cell was tapped. In production, this will be replaced
 * with a single Bitmap and coordinate calculation.
 */
class GridWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { widgetId ->
            val views = buildRemoteViews(context, widgetId)
            appWidgetManager.updateAppWidget(widgetId, views)
        }
    }

    private fun buildRemoteViews(context: Context, widgetId: Int): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_grid)

        // Wire up PendingIntents for each cell
        // This allows us to detect which cell was tapped
        val cellIds = listOf(
            // Row 0
            Triple(0, 0, R.id.cell_0_0),
            Triple(0, 1, R.id.cell_0_1),
            Triple(0, 2, R.id.cell_0_2),
            Triple(0, 3, R.id.cell_0_3),
            // Row 1
            Triple(1, 0, R.id.cell_1_0),
            Triple(1, 1, R.id.cell_1_1),
            Triple(1, 2, R.id.cell_1_2),
            Triple(1, 3, R.id.cell_1_3),
            // Row 2
            Triple(2, 0, R.id.cell_2_0),
            Triple(2, 1, R.id.cell_2_1),
            Triple(2, 2, R.id.cell_2_2),
            Triple(2, 3, R.id.cell_2_3),
            // Row 3
            Triple(3, 0, R.id.cell_3_0),
            Triple(3, 1, R.id.cell_3_1),
            Triple(3, 2, R.id.cell_3_2),
            Triple(3, 3, R.id.cell_3_3)
        )

        cellIds.forEach { (row, col, viewId) ->
            val pendingIntent = createCellTapIntent(context, widgetId, row, col)
            views.setOnClickPendingIntent(viewId, pendingIntent)
        }

        return views
    }

    private fun createCellTapIntent(
        context: Context,
        widgetId: Int,
        row: Int,
        col: Int
    ): PendingIntent {
        val intent = Intent(context, FullScreenOverlayActivity::class.java).apply {
            putExtra(EXTRA_WIDGET_ID, widgetId)
            putExtra(EXTRA_CELL_ROW, row)
            putExtra(EXTRA_CELL_COL, col)
            putExtra(EXTRA_TAP_TIMESTAMP, System.currentTimeMillis())

            // Critical flags for seamless overlay launch
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_NO_ANIMATION or
                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
        }

        // Generate unique request code to avoid PendingIntent collision
        val requestCode = (widgetId * 100) + (row * 10) + col

        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        const val EXTRA_WIDGET_ID = "widget_id"
        const val EXTRA_CELL_ROW = "cell_row"
        const val EXTRA_CELL_COL = "cell_col"
        const val EXTRA_TAP_TIMESTAMP = "tap_timestamp"
    }
}
