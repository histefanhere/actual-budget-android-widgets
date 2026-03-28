package com.histefanhere.actualwidgets.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.histefanhere.actualwidgets.model.BarScaleMode
import com.histefanhere.actualwidgets.model.BudgetStat
import com.histefanhere.actualwidgets.model.CategoryRowFormat
import com.histefanhere.actualwidgets.model.CategoryViewMode
import com.histefanhere.actualwidgets.model.WidgetConfig
import com.histefanhere.actualwidgets.model.WidgetSize
import kotlinx.coroutines.flow.first

// Single DataStore shared across all widget instances; keys are namespaced by appWidgetId.
val Context.widgetConfigDataStore: DataStore<Preferences> by preferencesDataStore(name = "widget_config")

class WidgetPrefsStore(private val context: Context) {

    private fun key(widgetId: Int, field: String) =
        stringPreferencesKey("config_${widgetId}_$field")

    private fun hiddenGroupsKey(widgetId: Int) =
        stringSetPreferencesKey("config_${widgetId}_hidden_groups")

    private fun hiddenCategoryIdsKey(widgetId: Int) =
        stringSetPreferencesKey("config_${widgetId}_hidden_category_ids")

    private fun normalizedScaleKey(widgetId: Int) =
        booleanPreferencesKey("config_${widgetId}_normalized_scale")

    private fun showCentsKey(widgetId: Int) =
        booleanPreferencesKey("config_${widgetId}_show_cents")

    private fun showProgressBarsKey(widgetId: Int) =
        booleanPreferencesKey("config_${widgetId}_show_progress_bars")

    private fun visibleBudgetStatsKey(widgetId: Int) =
        stringSetPreferencesKey("config_${widgetId}_visible_budget_stats")

    suspend fun saveConfig(widgetId: Int, config: WidgetConfig) {
        context.widgetConfigDataStore.edit { prefs ->
            prefs[key(widgetId, "server_url")] = config.serverUrl
            prefs[key(widgetId, "api_key")] = config.apiKey
            prefs[key(widgetId, "budget_id")] = config.budgetId
            prefs[key(widgetId, "budget_name")] = config.budgetName
            prefs[key(widgetId, "currency")] = config.currencySymbol
            prefs[key(widgetId, "size")] = config.widgetSize.name
            prefs[hiddenGroupsKey(widgetId)] = config.hiddenCategoryGroupIds
            prefs[hiddenCategoryIdsKey(widgetId)] = config.hiddenCategoryIds
            prefs[key(widgetId, "category_view_mode")] = config.categoryViewMode.name
            prefs[normalizedScaleKey(widgetId)] = config.normalizedScale
            prefs[showCentsKey(widgetId)] = config.showCents
            prefs[showProgressBarsKey(widgetId)] = config.showProgressBars
            prefs[key(widgetId, "category_row_format")] = config.categoryRowFormat.name
            prefs[key(widgetId, "bar_scale_mode")] = config.barScaleMode.name
            prefs[visibleBudgetStatsKey(widgetId)] = config.visibleBudgetStats.map { it.name }.toSet()
        }
    }

    suspend fun getConfig(widgetId: Int): WidgetConfig? {
        val prefs = context.widgetConfigDataStore.data.first()
        val url = prefs[key(widgetId, "server_url")] ?: return null
        val apiKey = prefs[key(widgetId, "api_key")] ?: return null
        val budgetId = prefs[key(widgetId, "budget_id")] ?: return null
        return WidgetConfig(
            serverUrl = url,
            apiKey = apiKey,
            budgetId = budgetId,
            budgetName = prefs[key(widgetId, "budget_name")] ?: "",
            currencySymbol = prefs[key(widgetId, "currency")] ?: "$",
            widgetSize = prefs[key(widgetId, "size")]
                ?.let { runCatching { WidgetSize.valueOf(it) }.getOrNull() }
                ?: WidgetSize.MEDIUM,
            hiddenCategoryGroupIds = prefs[hiddenGroupsKey(widgetId)] ?: emptySet(),
            hiddenCategoryIds = prefs[hiddenCategoryIdsKey(widgetId)] ?: emptySet(),
            categoryViewMode = prefs[key(widgetId, "category_view_mode")]
                ?.let { runCatching { CategoryViewMode.valueOf(it) }.getOrNull() }
                ?: CategoryViewMode.GROUPS,
            normalizedScale = prefs[normalizedScaleKey(widgetId)] ?: false,
            showCents = prefs[showCentsKey(widgetId)] ?: true,
            showProgressBars = prefs[showProgressBarsKey(widgetId)] ?: true,
            categoryRowFormat = prefs[key(widgetId, "category_row_format")]
                ?.let { runCatching { CategoryRowFormat.valueOf(it) }.getOrNull() }
                ?: CategoryRowFormat.SPENT_OF_BUDGETED,
            barScaleMode = prefs[key(widgetId, "bar_scale_mode")]
                ?.let { runCatching { BarScaleMode.valueOf(it) }.getOrNull() }
                ?: BarScaleMode.SPENT_OF_BUDGETED,
            visibleBudgetStats = prefs[visibleBudgetStatsKey(widgetId)]
                ?.mapNotNull { runCatching { BudgetStat.valueOf(it) }.getOrNull() }
                ?.toSet()
                ?: BudgetStat.DEFAULT,
        )
    }

    suspend fun clearConfig(widgetId: Int) {
        context.widgetConfigDataStore.edit { prefs ->
            listOf("server_url", "api_key", "budget_id", "budget_name", "currency", "size").forEach { field ->
                prefs.remove(key(widgetId, field))
            }
            prefs.remove(hiddenGroupsKey(widgetId))
            prefs.remove(hiddenCategoryIdsKey(widgetId))
            prefs.remove(key(widgetId, "category_view_mode"))
            prefs.remove(normalizedScaleKey(widgetId))
            prefs.remove(showCentsKey(widgetId))
            prefs.remove(showProgressBarsKey(widgetId))
            prefs.remove(key(widgetId, "category_row_format"))
            prefs.remove(key(widgetId, "bar_scale_mode"))
            prefs.remove(visibleBudgetStatsKey(widgetId))
        }
    }
}
