package com.histefanhere.actualwidgets.model

/**
 * All budget figures available for display on the summary widget.
 * All monetary values are integer cents (divide by 100 to display).
 * [totalSpent] is negative per Actual Budget convention; take abs() before displaying.
 */
data class BudgetSummary(
    val currencySymbol: String,
    val budgetName: String,
    val monthLabel: String,
    val totalIncome: Long,
    val fromLastMonth: Long,
    val incomeAvailable: Long,
    val lastMonthOverspent: Long,
    val forNextMonth: Long,
    val totalBudgeted: Long,
    val toBudget: Long,
    val totalSpent: Long,
    val totalBalance: Long,
    /** Epoch-millis timestamp of when this summary was last fetched. */
    val lastUpdatedMs: Long,
)
