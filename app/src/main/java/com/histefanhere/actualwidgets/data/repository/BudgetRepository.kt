package com.histefanhere.actualwidgets.data.repository

import com.histefanhere.actualwidgets.data.api.ActualApiService
import com.histefanhere.actualwidgets.data.api.ApiCategoryGroup
import com.histefanhere.actualwidgets.data.api.ApiMonthCategory
import com.histefanhere.actualwidgets.data.api.BudgetFile
import com.histefanhere.actualwidgets.model.BudgetSummary
import com.histefanhere.actualwidgets.model.CategoryGroupEntry
import com.histefanhere.actualwidgets.model.CategoryGroupInfo
import com.histefanhere.actualwidgets.model.CategoryGroupWithCategories
import com.histefanhere.actualwidgets.model.CategoryGroupsSnapshot
import com.histefanhere.actualwidgets.model.CategoryInfo
import com.histefanhere.actualwidgets.model.WidgetConfig
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

class BudgetRepository(private val api: ActualApiService) {

    suspend fun fetchBudgets(): List<BudgetFile> = api.getBudgets().data

    suspend fun fetchBudgetSummary(config: WidgetConfig, monthOffset: Int = 0): BudgetSummary {
        val thisMonthStr = LocalDate.now().plusMonths(monthOffset.toLong()).format(MONTH_FORMAT)
        val data = api.getBudgetMonth(config.budgetId, thisMonthStr).data

        return BudgetSummary(
            currencySymbol = config.currencySymbol,
            budgetName = config.budgetName,
            monthLabel = formatMonthLabel(thisMonthStr),
            totalIncome = data?.totalIncome ?: 0L,
            fromLastMonth = data?.fromLastMonth ?: 0L,
            incomeAvailable = data?.incomeAvailable ?: 0L,
            lastMonthOverspent = data?.lastMonthOverspent ?: 0L,
            forNextMonth = data?.forNextMonth ?: 0L,
            totalBudgeted = data?.totalBudgeted ?: 0L,
            toBudget = data?.toBudget ?: 0L,
            totalSpent = data?.totalSpent ?: 0L,
            totalBalance = data?.totalBalance ?: 0L,
            lastUpdatedMs = System.currentTimeMillis(),
        )
    }

    suspend fun fetchCategoryGroups(config: WidgetConfig, monthOffset: Int = 0): CategoryGroupsSnapshot {
        val today = LocalDate.now().plusMonths(monthOffset.toLong())
        val thisMonthStr = today.format(MONTH_FORMAT)
        val monthData = api.getBudgetMonth(config.budgetId, thisMonthStr).data

        val groups = monthData?.categoryGroups
            ?.filter { !it.isIncome && !it.hidden && it.id !in config.hiddenCategoryGroupIds }
            ?.map { group ->
                val visible = group.categories.filter { !it.hidden && it.id !in config.hiddenCategoryIds }
                CategoryGroupEntry(
                    name = group.name,
                    budgeted = visible.sumOf { it.budgeted },
                    spent = visible.sumOf { it.spent },
                    balance = visible.sumOf { it.balance },
                )
            }
            ?: emptyList()

        return CategoryGroupsSnapshot(
            currencySymbol = config.currencySymbol,
            monthLabel = formatMonthLabel(thisMonthStr),
            groups = groups,
            lastUpdatedMs = System.currentTimeMillis(),
        )
    }

    /** Returns groups with their categories for the config UI visibility checklist. */
    suspend fun fetchCategoryGroupListWithCategories(config: WidgetConfig): List<CategoryGroupWithCategories> {
        val thisMonthStr = LocalDate.now().format(MONTH_FORMAT)
        return api.getBudgetMonth(config.budgetId, thisMonthStr).data
            ?.categoryGroups
            ?.filter { !it.isIncome && !it.hidden }
            ?.map { group ->
                CategoryGroupWithCategories(
                    id = group.id,
                    name = group.name,
                    categories = group.categories
                        .filter { !it.hidden }
                        .map { CategoryInfo(it.id, it.name) },
                )
            }
            ?: emptyList()
    }

    /** Flat list of individual categories — one entry per category, respecting hidden filters. */
    suspend fun fetchIndividualCategories(config: WidgetConfig, monthOffset: Int = 0): CategoryGroupsSnapshot {
        val today = LocalDate.now().plusMonths(monthOffset.toLong())
        val thisMonthStr = today.format(MONTH_FORMAT)
        val monthData = api.getBudgetMonth(config.budgetId, thisMonthStr).data

        val entries = monthData?.categoryGroups
            ?.filter { !it.isIncome && !it.hidden && it.id !in config.hiddenCategoryGroupIds }
            ?.flatMap { group ->
                group.categories
                    .filter { !it.hidden && it.id !in config.hiddenCategoryIds }
                    .map { cat ->
                        CategoryGroupEntry(
                            name = cat.name,
                            budgeted = cat.budgeted,
                            spent = cat.spent,
                            balance = cat.balance,
                        )
                    }
            }
            ?: emptyList()

        return CategoryGroupsSnapshot(
            currencySymbol = config.currencySymbol,
            monthLabel = formatMonthLabel(thisMonthStr),
            groups = entries,
            lastUpdatedMs = System.currentTimeMillis(),
        )
    }

    // Only include non-income, non-hidden category groups
    private fun List<ApiCategoryGroup>?.expenseCategories(): List<ApiMonthCategory> =
        this?.filter { !it.isIncome && !it.hidden }
            ?.flatMap { it.categories }
            ?.filter { !it.hidden }
            ?: emptyList()

    private fun formatMonthLabel(yearMonthStr: String): String {
        val ym = YearMonth.parse(yearMonthStr, MONTH_FORMAT)
        return ym.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()))
    }

    companion object {
        private val MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM")
    }
}
