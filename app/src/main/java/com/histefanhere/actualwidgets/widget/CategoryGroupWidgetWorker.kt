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

class CategoryGroupWidgetWorker(
    private val context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val appWidgetId = inputData.getInt(KEY_WIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return Result.failure()

        val glanceId = GlanceAppWidgetManager(context).getGlanceIdBy(appWidgetId)
            ?: return Result.failure()

        val monthOffset = getAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId)
            .let { it[CategoryWidgetStateKeys.MONTH_OFFSET] ?: 0 }

        val config = WidgetPrefsStore(context).getConfig(appWidgetId)
        if (config == null) {
            setState(glanceId, appWidgetId) { prefs ->
                prefs[CategoryWidgetStateKeys.STATE_TYPE] = STATE_NOT_CONFIGURED
                prefs[CategoryWidgetStateKeys.MONTH_OFFSET] = monthOffset
            }
            return Result.success()
        }

        return try {
            val repo = BudgetRepository(ApiClientFactory.create(config.serverUrl, config.apiKey))
            val groupsSnapshot = repo.fetchCategoryGroups(config, monthOffset)
            val categoriesSnapshot = repo.fetchIndividualCategories(config, monthOffset)

            setState(glanceId, appWidgetId) { prefs ->
                prefs[CategoryWidgetStateKeys.STATE_TYPE] = STATE_SUCCESS
                prefs[CategoryWidgetStateKeys.GROUPS_JSON] = Gson().toJson(groupsSnapshot)
                prefs[CategoryWidgetStateKeys.CATEGORIES_JSON] = Gson().toJson(categoriesSnapshot)
                prefs[CategoryWidgetStateKeys.VIEW_MODE] = config.categoryViewMode.name
                prefs[CategoryWidgetStateKeys.NORMALIZED_SCALE] = config.normalizedScale
                prefs[CategoryWidgetStateKeys.WIDGET_SIZE] = config.widgetSize.name
                prefs[CategoryWidgetStateKeys.SHOW_CENTS]          = config.showCents
                prefs[CategoryWidgetStateKeys.SHOW_PROGRESS_BARS]  = config.showProgressBars
                prefs[CategoryWidgetStateKeys.CATEGORY_ROW_FORMAT] = config.categoryRowFormat.name
                prefs[CategoryWidgetStateKeys.BAR_SCALE_MODE]      = config.barScaleMode.name
                prefs[CategoryWidgetStateKeys.MONTH_OFFSET] = monthOffset
                prefs.remove(CategoryWidgetStateKeys.ERROR_MESSAGE)
            }
            Result.success()
        } catch (e: Exception) {
            setState(glanceId, appWidgetId) { prefs ->
                prefs[CategoryWidgetStateKeys.STATE_TYPE] = STATE_ERROR
                prefs[CategoryWidgetStateKeys.ERROR_MESSAGE] = e.message ?: "Network error"
                prefs[CategoryWidgetStateKeys.MONTH_OFFSET] = monthOffset
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
                this[CategoryWidgetStateKeys.APP_WIDGET_ID] = appWidgetId
                block(this)
            }
        }
        CategoryGroupWidget().update(context, glanceId)
    }

    companion object {
        private const val KEY_WIDGET_ID = "widget_id"
        private const val WORK_PREFIX = "actual_category_widget"

        private fun periodicWorkName(widgetId: Int) = "${WORK_PREFIX}_periodic_$widgetId"
        private fun networkConstraints() = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        fun enqueueOneTimeWork(context: Context, appWidgetId: Int) {
            val request = OneTimeWorkRequestBuilder<CategoryGroupWidgetWorker>()
                .setInputData(Data.Builder().putInt(KEY_WIDGET_ID, appWidgetId).build())
                .setConstraints(networkConstraints())
                .build()
            WorkManager.getInstance(context).enqueue(request)
        }

        fun enqueuePeriodicWork(context: Context, appWidgetId: Int) {
            val request = PeriodicWorkRequestBuilder<CategoryGroupWidgetWorker>(30, TimeUnit.MINUTES)
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
