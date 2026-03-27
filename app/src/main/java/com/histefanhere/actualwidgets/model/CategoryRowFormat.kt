package com.histefanhere.actualwidgets.model

enum class CategoryRowFormat {
    /** "$50 / $200" — amount spent out of budgeted (default) */
    SPENT_OF_BUDGETED,
    /** "$50" — amount spent only */
    SPENT,
    /** "$150" — remaining balance */
    BALANCE,
    /** "$50 / $250" — spent out of total available (balance + spent) */
    AVAILABLE_BREAKDOWN;

    val label: String get() = when (this) {
        SPENT_OF_BUDGETED   -> "Spent / Budgeted"
        SPENT               -> "Spent"
        BALANCE             -> "Balance"
        AVAILABLE_BREAKDOWN -> "Spent / Available"
    }

    val description: String get() = when (this) {
        SPENT_OF_BUDGETED   -> "How much you've spent out of your budget. e.g. \$50 / \$200"
        SPENT               -> "Total amount spent this month. e.g. \$50"
        BALANCE             -> "Remaining balance, including any carry-over from last month. e.g. \$150"
        AVAILABLE_BREAKDOWN -> "Spent vs. total available funds (budgeted + carry-over). e.g. \$50 / \$250"
    }
}
