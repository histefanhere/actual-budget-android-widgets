package com.histefanhere.actualwidgets.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition

class CategoryBreakdownRefreshAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val prefs = getAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId)
        val appWidgetId = prefs[CategoryBreakdownStateKeys.APP_WIDGET_ID] ?: return
        CategoryBreakdownWidgetWorker.enqueueOneTimeWork(context, appWidgetId)
    }
}
