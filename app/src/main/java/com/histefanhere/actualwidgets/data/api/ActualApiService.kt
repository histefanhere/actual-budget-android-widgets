package com.histefanhere.actualwidgets.data.api

import retrofit2.http.GET
import retrofit2.http.Path

interface ActualApiService {

    /** Returns all budget files accessible with the configured API key. */
    @GET("budgets")
    suspend fun getBudgets(): BudgetsResponse

    /**
     * Returns budget details for [month] (format: `yyyy-MM`), including all
     * category groups and their per-category budgeted/spent/balance figures.
     */
    @GET("budgets/{budgetId}/months/{month}")
    suspend fun getBudgetMonth(
        @Path("budgetId") budgetId: String,
        @Path("month") month: String,
    ): BudgetMonthResponse
}
