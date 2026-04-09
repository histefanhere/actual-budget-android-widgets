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

class CategoryBreakdownWidgetWorker(
    private val context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val appWidgetId = inputData.getInt(KEY_WIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return Result.failure()

        val glanceId = GlanceAppWidgetManager(context).getGlanceIdBy(appWidgetId)
            ?: return Result.failure()

        val monthOffset = getAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId)
            .let { it[CategoryBreakdownStateKeys.MONTH_OFFSET] ?: 0 }

        val config = WidgetPrefsStore(context).getConfig(appWidgetId)
        if (config == null) {
            setState(glanceId, appWidgetId) { prefs ->
                prefs[CategoryBreakdownStateKeys.STATE_TYPE] = WidgetState.NOT_CONFIGURED
                prefs[CategoryBreakdownStateKeys.MONTH_OFFSET] = monthOffset
            }
            return Result.success()
        }

        return try {
            val repo = BudgetRepository(ApiClientFactory.create(config.serverUrl, config.apiKey))
            val groupsSnapshot = repo.fetchCategoryGroups(config, monthOffset)
            val categoriesSnapshot = repo.fetchIndividualCategories(config, monthOffset)

            setState(glanceId, appWidgetId) { prefs ->
                prefs[CategoryBreakdownStateKeys.STATE_TYPE] = WidgetState.SUCCESS
                prefs[CategoryBreakdownStateKeys.GROUPS_JSON] = Gson().toJson(groupsSnapshot)
                prefs[CategoryBreakdownStateKeys.CATEGORIES_JSON] = Gson().toJson(categoriesSnapshot)
                prefs[CategoryBreakdownStateKeys.VIEW_MODE] = config.categoryViewMode.name
                prefs[CategoryBreakdownStateKeys.NORMALIZED_SCALE] = config.normalizedScale
                prefs[CategoryBreakdownStateKeys.WIDGET_SIZE] = config.widgetSize.name
                prefs[CategoryBreakdownStateKeys.SHOW_CENTS]          = config.showCents
                prefs[CategoryBreakdownStateKeys.SHOW_PROGRESS_BARS]  = config.showProgressBars
                prefs[CategoryBreakdownStateKeys.SHOW_MONTH_ARROWS]   = config.showMonthArrows
                prefs[CategoryBreakdownStateKeys.SHOW_REFRESH_ICON]   = config.showRefreshIcon
                prefs[CategoryBreakdownStateKeys.CATEGORY_ROW_FORMAT] = config.categoryRowFormat.name
                prefs[CategoryBreakdownStateKeys.BAR_SCALE_MODE]      = config.barScaleMode.name
                prefs[CategoryBreakdownStateKeys.MONTH_OFFSET] = monthOffset
                prefs.remove(CategoryBreakdownStateKeys.ERROR_MESSAGE)
            }
            Result.success()
        } catch (e: Exception) {
            setState(glanceId, appWidgetId) { prefs ->
                prefs[CategoryBreakdownStateKeys.STATE_TYPE] = WidgetState.ERROR
                prefs[CategoryBreakdownStateKeys.ERROR_MESSAGE] = e.toErrorMessage()
                prefs[CategoryBreakdownStateKeys.MONTH_OFFSET] = monthOffset
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
                this[CategoryBreakdownStateKeys.APP_WIDGET_ID] = appWidgetId
                block(this)
            }
        }
        CategoryBreakdownWidget().update(context, glanceId)
    }

    companion object {
        private const val KEY_WIDGET_ID = "widget_id"
        private const val WORK_PREFIX = "category_breakdown_widget"

        private fun periodicWorkName(widgetId: Int) = "${WORK_PREFIX}_periodic_$widgetId"
        private fun networkConstraints() = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        fun enqueueOneTimeWork(context: Context, appWidgetId: Int) {
            val request = OneTimeWorkRequestBuilder<CategoryBreakdownWidgetWorker>()
                .setInputData(Data.Builder().putInt(KEY_WIDGET_ID, appWidgetId).build())
                .setConstraints(networkConstraints())
                .build()
            WorkManager.getInstance(context).enqueue(request)
        }

        fun enqueuePeriodicWork(context: Context, appWidgetId: Int) {
            val request = PeriodicWorkRequestBuilder<CategoryBreakdownWidgetWorker>(30, TimeUnit.MINUTES)
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
