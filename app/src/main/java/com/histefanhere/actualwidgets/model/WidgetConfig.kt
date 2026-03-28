package com.histefanhere.actualwidgets.model

data class WidgetConfig(
    val serverUrl: String,
    val apiKey: String,
    val budgetId: String,
    val budgetName: String = "",
    /** Symbol prepended to monetary values, e.g. "$", "€", "£". */
    val currencySymbol: String = "$",
    val widgetSize: WidgetSize = WidgetSize.MEDIUM,
    /** IDs of category groups the user has chosen to hide in the category health widget. */
    val hiddenCategoryGroupIds: Set<String> = emptySet(),
    val hiddenCategoryIds: Set<String> = emptySet(),
    val categoryViewMode: CategoryViewMode = CategoryViewMode.GROUPS,
    val categoryRowFormat: CategoryRowFormat = CategoryRowFormat.SPENT_OF_BUDGETED,
    /** Controls what value progress bars measure against: budgeted amount or total available (budgeted + carry-over). */
    val barScaleMode: BarScaleMode = BarScaleMode.SPENT_OF_BUDGETED,
    /** When true, bar widths are proportional to budget size so the largest fills full width. */
    val normalizedScale: Boolean = false,
    /** When false, amounts are displayed as whole numbers with no decimal places. */
    val showCents: Boolean = true,
    /** When false, the spending progress bars are hidden in the category widget. */
    val showProgressBars: Boolean = true,
    /** Which stats appear on the budget summary widget, in fixed enum declaration order. */
    val visibleBudgetStats: Set<BudgetStat> = BudgetStat.DEFAULT,
)
