package com.histefanhere.actualwidgets.data.api

import com.google.gson.annotations.SerializedName

// ── Budget list ──────────────────────────────────────────────────────────────

data class BudgetsResponse(val data: List<BudgetFile>)

data class BudgetFile(
    val groupId: String,
    val name: String,
)

// ── Budget month ─────────────────────────────────────────────────────────────

data class BudgetMonthResponse(val data: BudgetMonthDetail?)

data class BudgetMonthDetail(
    val month: String,
    val totalIncome: Long = 0L,
    val fromLastMonth: Long = 0L,
    val incomeAvailable: Long = 0L,
    val lastMonthOverspent: Long = 0L,
    val forNextMonth: Long = 0L,
    val totalBudgeted: Long = 0L,
    @SerializedName("toBudget") val toBudget: Long = 0L,
    val totalSpent: Long = 0L,
    val totalBalance: Long = 0L,
    val categoryGroups: List<ApiCategoryGroup> = emptyList(),
)

data class ApiCategoryGroup(
    val id: String,
    val name: String,
    @SerializedName("is_income") val isIncome: Boolean = false,
    val hidden: Boolean = false,
    val categories: List<ApiMonthCategory> = emptyList(),
)

/**
 * Amounts are integers in units of 1/100 of the currency (i.e. integer cents).
 * $10.50 → 1050.  Spent values are negative (outflows).
 */
data class ApiMonthCategory(
    val id: String,
    val name: String,
    val budgeted: Long = 0L,
    val spent: Long = 0L,
    val balance: Long = 0L,
    val carryover: Boolean = false,
    val hidden: Boolean = false,
)
