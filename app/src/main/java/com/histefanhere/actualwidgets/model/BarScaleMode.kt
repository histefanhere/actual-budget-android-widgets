package com.histefanhere.actualwidgets.model

enum class BarScaleMode {
    /** Bar fills based on spent / budgeted (default) */
    SPENT_OF_BUDGETED,
    /** Bar fills based on spent / available (budgeted + carry-over) */
    SPENT_OF_AVAILABLE;

    val label: String get() = when (this) {
        SPENT_OF_BUDGETED  -> "Spent / Budgeted"
        SPENT_OF_AVAILABLE -> "Spent / Available"
    }

    val description: String get() = when (this) {
        SPENT_OF_BUDGETED  -> "Bar fills based on how much you've spent vs. your budget."
        SPENT_OF_AVAILABLE -> "Bar fills based on how much you've spent vs. total available funds (budgeted + carry-over)."
    }
}
