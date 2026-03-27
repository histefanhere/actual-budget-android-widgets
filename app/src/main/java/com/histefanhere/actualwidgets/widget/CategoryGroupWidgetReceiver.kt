package com.histefanhere.actualwidgets.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import com.histefanhere.actualwidgets.data.prefs.WidgetPrefsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CategoryGroupWidgetReceiver : GlanceAppWidgetReceiver() {

    override val glanceAppWidget: GlanceAppWidget = CategoryGroupWidget()

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        appWidgetIds.forEach { id ->
            CategoryGroupWidgetWorker.enqueueOneTimeWork(context, id)
            CategoryGroupWidgetWorker.enqueuePeriodicWork(context, id)
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        appWidgetIds.forEach { id ->
            CategoryGroupWidgetWorker.cancelWork(context, id)
            CoroutineScope(Dispatchers.IO).launch {
                WidgetPrefsStore(context).clearConfig(id)
            }
        }
        super.onDeleted(context, appWidgetIds)
    }
}
