package com.histefanhere.actualwidgets.widget

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

/**
 * Keys used to store the Monthly Summary widget's display state inside
 * Glance's per-widget Preferences.
 */
object MonthlySummaryStateKeys {
    val STATE_TYPE = stringPreferencesKey("state_type")
    val ERROR_MESSAGE = stringPreferencesKey("error_message")
    val SUMMARY_JSON = stringPreferencesKey("summary_json")
    /** The Android AppWidget ID stored alongside the data so actions can look it up. */
    val APP_WIDGET_ID = intPreferencesKey("app_widget_id")
    /** Serialised [com.histefanhere.actualwidgets.model.WidgetSize] name. */
    val WIDGET_SIZE          = stringPreferencesKey("widget_size")
    val SHOW_CENTS           = booleanPreferencesKey("show_cents")
    val SHOW_MONTH_ARROWS    = booleanPreferencesKey("show_month_arrows")
    val SHOW_REFRESH_ICON    = booleanPreferencesKey("show_refresh_icon")
    /** Comma-separated [com.histefanhere.actualwidgets.model.BudgetStat] names. Empty = all visible. */
    val VISIBLE_BUDGET_STATS = stringPreferencesKey("visible_budget_stats")
    /** Month offset from current month: 0 = current, -1 = last month, +1 = next month. */
    val MONTH_OFFSET = intPreferencesKey("month_offset")
}
