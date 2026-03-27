package com.histefanhere.actualwidgets.model

/**
 * Each selectable stat that can be shown on the budget summary widget.
 * The declaration order here is the fixed render order in the widget.
 */
enum class BudgetStat {
    INCOME,
    FROM_LAST_MONTH,
    AVAILABLE_FUNDS,
    LAST_MONTH_OVERSPENT,
    FOR_NEXT_MONTH,
    BUDGETED,
    TO_BUDGET,
    SPENT,
    BALANCE;

    val label: String get() = when (this) {
        INCOME               -> "Income"
        FROM_LAST_MONTH      -> "From Last Month"
        AVAILABLE_FUNDS      -> "Available Funds"
        LAST_MONTH_OVERSPENT -> "Last Month Overspent"
        FOR_NEXT_MONTH       -> "For Next Month"
        BUDGETED             -> "Budgeted"
        TO_BUDGET            -> "To Budget"
        SPENT                -> "Spent"
        BALANCE              -> "Balance"
    }

    companion object {
        val ALL: Set<BudgetStat> = entries.toSet()
        val DEFAULT: Set<BudgetStat> = setOf(BUDGETED, SPENT, BALANCE)
    }
}
