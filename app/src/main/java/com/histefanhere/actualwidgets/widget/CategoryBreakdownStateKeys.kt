package com.histefanhere.actualwidgets.widget

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

/**
 * Keys used to store the Category Breakdown widget's display state inside
 * Glance's per-widget Preferences.
 */
object CategoryBreakdownStateKeys {
    val STATE_TYPE    = stringPreferencesKey("state_type")
    val ERROR_MESSAGE = stringPreferencesKey("error_message")
    val GROUPS_JSON      = stringPreferencesKey("groups_json")
    val CATEGORIES_JSON  = stringPreferencesKey("categories_json")
    val VIEW_MODE        = stringPreferencesKey("view_mode")
    val APP_WIDGET_ID    = intPreferencesKey("app_widget_id")
    val NORMALIZED_SCALE = booleanPreferencesKey("normalized_scale")
    val WIDGET_SIZE      = stringPreferencesKey("widget_size")
    val SHOW_CENTS         = booleanPreferencesKey("show_cents")
    val SHOW_PROGRESS_BARS = booleanPreferencesKey("show_progress_bars")
    val SHOW_MONTH_ARROWS  = booleanPreferencesKey("show_month_arrows")
    val SHOW_REFRESH_ICON  = booleanPreferencesKey("show_refresh_icon")
    val CATEGORY_ROW_FORMAT = stringPreferencesKey("category_row_format")
    val BAR_SCALE_MODE      = stringPreferencesKey("bar_scale_mode")
    /** Month offset from current month: 0 = current, -1 = last month, +1 = next month. */
    val MONTH_OFFSET = intPreferencesKey("month_offset")
}
