package com.histefanhere.actualwidgets.model

data class CategoryGroupInfo(val id: String, val name: String)

data class CategoryInfo(val id: String, val name: String)

data class CategoryGroupWithCategories(
    val id: String,
    val name: String,
    val categories: List<CategoryInfo>,
)

data class CategoryGroupEntry(
    val name: String,
    val budgeted: Long,  // cents
    val spent: Long,     // cents, negative (outflow)
    val balance: Long,   // cents; may include carryover from previous months
)

data class CategoryGroupsSnapshot(
    val currencySymbol: String,
    val monthLabel: String,
    val groups: List<CategoryGroupEntry>,
    val lastUpdatedMs: Long,
)
