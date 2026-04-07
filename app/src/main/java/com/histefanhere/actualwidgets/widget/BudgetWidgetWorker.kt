package com.histefanhere.actualwidgets.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.work.WorkerParameters
import com.google.gson.Gson
import com.histefanhere.actualwidgets.data.api.ApiClientFactory
import com.histefanhere.actualwidgets.data.prefs.WidgetPrefsStore
import com.histefanhere.actualwidgets.data.repository.BudgetRepository
import java.util.concurrent.TimeUnit

class BudgetWidgetWorker(
    private val context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val appWidgetId = inputData.getInt(KEY_WIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return Result.failure()

        val glanceId = GlanceAppWidgetManager(context).getGlanceIdBy(appWidgetId)
            ?: return Result.failure()

        val monthOffset = getAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId)
            .let { it[WidgetStateKeys.MONTH_OFFSET] ?: 0 }

        val config = WidgetPrefsStore(context).getConfig(appWidgetId)
        if (config == null) {
            setState(glanceId, appWidgetId) { prefs ->
                prefs[WidgetStateKeys.STATE_TYPE] = STATE_NOT_CONFIGURED
                prefs[WidgetStateKeys.MONTH_OFFSET] = monthOffset
            }
            return Result.success()
        }

        return try {
            val summary = BudgetRepository(
                ApiClientFactory.create(config.serverUrl, config.apiKey)
            ).fetchBudgetSummary(config, monthOffset)

            setState(glanceId, appWidgetId) { prefs ->
                prefs[WidgetStateKeys.STATE_TYPE] = STATE_SUCCESS
                prefs[WidgetStateKeys.SUMMARY_JSON] = Gson().toJson(summary)
                prefs[WidgetStateKeys.WIDGET_SIZE]          = config.widgetSize.name
                prefs[WidgetStateKeys.SHOW_CENTS]           = config.showCents
                prefs[WidgetStateKeys.SHOW_MONTH_ARROWS]    = config.showMonthArrows
                prefs[WidgetStateKeys.SHOW_REFRESH_ICON]    = config.showRefreshIcon
                prefs[WidgetStateKeys.VISIBLE_BUDGET_STATS] = config.visibleBudgetStats.joinToString(",") { it.name }
                prefs[WidgetStateKeys.MONTH_OFFSET] = monthOffset
                prefs.remove(WidgetStateKeys.ERROR_MESSAGE)
            }
            Result.success()
        } catch (e: Exception) {
            setState(glanceId, appWidgetId) { prefs ->
                prefs[WidgetStateKeys.STATE_TYPE] = STATE_ERROR
                prefs[WidgetStateKeys.ERROR_MESSAGE] = e.toErrorMessage()
                prefs[WidgetStateKeys.MONTH_OFFSET] = monthOffset
            }
            Result.retry()
        }
    }

    private suspend fun setState(
        glanceId: GlanceId,
        appWidgetId: Int,
        block: (MutablePreferences) -> Unit,
    ) {
        updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { current ->
            current.toMutablePreferences().apply {
                this[WidgetStateKeys.APP_WIDGET_ID] = appWidgetId
                block(this)
            }
        }
        BudgetWidget().update(context, glanceId)
    }

    companion object {
        private const val KEY_WIDGET_ID = "widget_id"
        private const val WORK_PREFIX = "actual_widget"

        private fun periodicWorkName(widgetId: Int) = "${WORK_PREFIX}_periodic_$widgetId"
        private fun networkConstraints() = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        fun enqueueOneTimeWork(context: Context, appWidgetId: Int) {
            val request = OneTimeWorkRequestBuilder<BudgetWidgetWorker>()
                .setInputData(Data.Builder().putInt(KEY_WIDGET_ID, appWidgetId).build())
                .setConstraints(networkConstraints())
                .build()
            WorkManager.getInstance(context).enqueue(request)
        }

        fun enqueuePeriodicWork(context: Context, appWidgetId: Int) {
            val request = PeriodicWorkRequestBuilder<BudgetWidgetWorker>(30, TimeUnit.MINUTES)
                .setInputData(Data.Builder().putInt(KEY_WIDGET_ID, appWidgetId).build())
                .setConstraints(networkConstraints())
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                periodicWorkName(appWidgetId),
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }

        fun cancelWork(context: Context, appWidgetId: Int) {
            WorkManager.getInstance(context).cancelUniqueWork(periodicWorkName(appWidgetId))
        }
    }
}
