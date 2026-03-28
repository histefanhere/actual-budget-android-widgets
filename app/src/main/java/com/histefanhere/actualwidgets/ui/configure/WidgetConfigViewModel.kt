package com.histefanhere.actualwidgets.ui.configure

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.histefanhere.actualwidgets.data.api.ApiClientFactory
import com.histefanhere.actualwidgets.data.api.BudgetFile
import com.histefanhere.actualwidgets.model.BarScaleMode
import com.histefanhere.actualwidgets.model.BudgetStat
import com.histefanhere.actualwidgets.model.CategoryGroupWithCategories
import com.histefanhere.actualwidgets.model.CategoryRowFormat
import com.histefanhere.actualwidgets.model.CategoryViewMode
import com.histefanhere.actualwidgets.widget.CategoryWidgetStateKeys
import com.histefanhere.actualwidgets.data.prefs.WidgetPrefsStore
import com.histefanhere.actualwidgets.data.repository.BudgetRepository
import com.histefanhere.actualwidgets.model.WidgetConfig
import com.histefanhere.actualwidgets.model.WidgetSize
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import com.histefanhere.actualwidgets.widget.BudgetWidget
import com.histefanhere.actualwidgets.widget.BudgetWidgetReceiver
import com.histefanhere.actualwidgets.widget.BudgetWidgetWorker
import com.histefanhere.actualwidgets.widget.CategoryGroupWidget
import com.histefanhere.actualwidgets.widget.CategoryGroupWidgetReceiver
import com.histefanhere.actualwidgets.widget.CategoryGroupWidgetWorker
import com.histefanhere.actualwidgets.widget.WidgetStateKeys
import kotlinx.coroutines.launch

class WidgetConfigViewModel(
    application: Application,
    private val appWidgetId: Int,
) : AndroidViewModel(application) {

    val isBudgetWidget: Boolean = AppWidgetManager.getInstance(application)
        .getAppWidgetInfo(appWidgetId)
        ?.provider?.className?.contains("CategoryGroup") != true

    private val prefsStore = WidgetPrefsStore(application)

    var serverUrl by mutableStateOf("")
    var apiKey by mutableStateOf("")
    var currencySymbol by mutableStateOf("$")

    var budgets by mutableStateOf<List<BudgetFile>>(emptyList())
    var selectedBudgetId by mutableStateOf("")
    var selectedBudgetName by mutableStateOf("")

    var widgetSize by mutableStateOf(WidgetSize.MEDIUM)

    var availableCategoryGroupsWithCategories by mutableStateOf<List<CategoryGroupWithCategories>>(emptyList())
    var hiddenCategoryGroupIds by mutableStateOf<Set<String>>(emptySet())
    var hiddenCategoryIds by mutableStateOf<Set<String>>(emptySet())
    var categoryViewMode by mutableStateOf(CategoryViewMode.GROUPS)
    var categoryRowFormat by mutableStateOf(CategoryRowFormat.SPENT_OF_BUDGETED)
    var barScaleMode by mutableStateOf(BarScaleMode.SPENT_OF_BUDGETED)
    var normalizedScale by mutableStateOf(false)
    var showCents by mutableStateOf(true)
    var showProgressBars by mutableStateOf(true)
    var visibleBudgetStats by mutableStateOf(BudgetStat.DEFAULT)
    var isLoadingGroups by mutableStateOf(false)
    var groupLoadError by mutableStateOf<String?>(null)

    var isLoadingBudgets by mutableStateOf(false)
    var budgetLoadError by mutableStateOf<String?>(null)
    var isSaving by mutableStateOf(false)

    init {
        // Pre-populate fields when reconfiguring an existing widget
        viewModelScope.launch {
            val existing = prefsStore.getConfig(appWidgetId)
            if (existing == null) {
                loadConnectionDefaults()
                return@launch
            }
            serverUrl = existing.serverUrl
            apiKey = existing.apiKey
            currencySymbol = existing.currencySymbol
            selectedBudgetId = existing.budgetId
            selectedBudgetName = existing.budgetName
            widgetSize = existing.widgetSize
            hiddenCategoryGroupIds = existing.hiddenCategoryGroupIds
            hiddenCategoryIds = existing.hiddenCategoryIds
            categoryViewMode = existing.categoryViewMode
            categoryRowFormat = existing.categoryRowFormat
            barScaleMode = existing.barScaleMode
            normalizedScale = existing.normalizedScale
            showCents = existing.showCents
            showProgressBars = existing.showProgressBars
            visibleBudgetStats = existing.visibleBudgetStats
            if (selectedBudgetName.isNotEmpty()) {
                budgets = listOf(BudgetFile(selectedBudgetId, selectedBudgetName))
            }
            if (!isBudgetWidget && selectedBudgetId.isNotEmpty()) {
                fetchGroupsForConfig()
            }
        }
    }

    fun fetchBudgets() {
        if (serverUrl.isBlank() || apiKey.isBlank()) return
        viewModelScope.launch {
            isLoadingBudgets = true
            budgetLoadError = null
            try {
                val repo = BudgetRepository(ApiClientFactory.create(serverUrl.trim(), apiKey.trim()))
                budgets = repo.fetchBudgets()
                if (budgets.isNotEmpty() && selectedBudgetId.isEmpty()) {
                    selectedBudgetId = budgets[0].groupId
                    selectedBudgetName = budgets[0].name
                }
                if (!isBudgetWidget) fetchGroupsForConfig()
            } catch (e: Exception) {
                budgetLoadError = "Failed to fetch budgets: ${e.message}"
            } finally {
                isLoadingBudgets = false
            }
        }
    }

    fun saveConfig(onSuccess: () -> Unit) {
        if (selectedBudgetId.isBlank()) return
        viewModelScope.launch {
            isSaving = true
            val config = WidgetConfig(
                serverUrl = serverUrl.trim(),
                apiKey = apiKey.trim(),
                budgetId = selectedBudgetId,
                budgetName = selectedBudgetName,
                currencySymbol = currencySymbol.ifBlank { "$" },
                widgetSize = widgetSize,
                hiddenCategoryGroupIds = hiddenCategoryGroupIds,
                hiddenCategoryIds = hiddenCategoryIds,
                categoryViewMode = categoryViewMode,
                categoryRowFormat = categoryRowFormat,
                barScaleMode = barScaleMode,
                normalizedScale = normalizedScale,
                showCents = showCents,
                showProgressBars = showProgressBars,
                visibleBudgetStats = visibleBudgetStats,
            )
            prefsStore.saveConfig(appWidgetId, config)
            if (isBudgetWidget) {
                BudgetWidgetWorker.enqueueOneTimeWork(getApplication(), appWidgetId)
                BudgetWidgetWorker.enqueuePeriodicWork(getApplication(), appWidgetId)
            } else {
                CategoryGroupWidgetWorker.enqueueOneTimeWork(getApplication(), appWidgetId)
                CategoryGroupWidgetWorker.enqueuePeriodicWork(getApplication(), appWidgetId)
            }
            isSaving = false
            onSuccess()
        }
    }

    /** Instantly re-renders the widget with the new size without triggering a network refresh. */
    fun applySize(size: WidgetSize) {
        widgetSize = size
        viewModelScope.launch {
            prefsStore.saveConfig(
                appWidgetId,
                WidgetConfig(
                    serverUrl = serverUrl,
                    apiKey = apiKey,
                    budgetId = selectedBudgetId,
                    budgetName = selectedBudgetName,
                    currencySymbol = currencySymbol.ifBlank { "$" },
                    widgetSize = size,
                    hiddenCategoryGroupIds = hiddenCategoryGroupIds,
                    hiddenCategoryIds = hiddenCategoryIds,
                    categoryViewMode = categoryViewMode,
                    categoryRowFormat = categoryRowFormat,
                    barScaleMode = barScaleMode,
                    normalizedScale = normalizedScale,
                    showCents = showCents,
                    visibleBudgetStats = visibleBudgetStats,
                ),
            )
            val glanceId = GlanceAppWidgetManager(getApplication()).getGlanceIdBy(appWidgetId)
                ?: return@launch
            updateAppWidgetState(getApplication(), PreferencesGlanceStateDefinition, glanceId) { current ->
                current.toMutablePreferences().apply {
                    this[WidgetStateKeys.WIDGET_SIZE] = size.name
                }
            }
            if (isBudgetWidget) {
                BudgetWidget().update(getApplication(), glanceId)
            } else {
                CategoryGroupWidget().update(getApplication(), glanceId)
            }
        }
    }

    fun fetchGroupsForConfig() {
        if (serverUrl.isBlank() || apiKey.isBlank() || selectedBudgetId.isBlank()) return
        viewModelScope.launch {
            isLoadingGroups = true
            groupLoadError = null
            try {
                val repo = BudgetRepository(ApiClientFactory.create(serverUrl.trim(), apiKey.trim()))
                val tempConfig = WidgetConfig(
                    serverUrl = serverUrl.trim(),
                    apiKey = apiKey.trim(),
                    budgetId = selectedBudgetId,
                )
                availableCategoryGroupsWithCategories = repo.fetchCategoryGroupListWithCategories(tempConfig)
            } catch (e: Exception) {
                groupLoadError = "Failed to load groups: ${e.message}"
            } finally {
                isLoadingGroups = false
            }
        }
    }

    fun toggleGroupHidden(groupId: String) {
        hiddenCategoryGroupIds = if (groupId in hiddenCategoryGroupIds) {
            hiddenCategoryGroupIds - groupId
        } else {
            hiddenCategoryGroupIds + groupId
        }
    }

    fun toggleCategoryHidden(categoryId: String) {
        hiddenCategoryIds = if (categoryId in hiddenCategoryIds) {
            hiddenCategoryIds - categoryId
        } else {
            hiddenCategoryIds + categoryId
        }
    }

    fun applyNormalizedScale(enabled: Boolean) {
        normalizedScale = enabled
        viewModelScope.launch {
            prefsStore.saveConfig(
                appWidgetId,
                WidgetConfig(
                    serverUrl = serverUrl,
                    apiKey = apiKey,
                    budgetId = selectedBudgetId,
                    budgetName = selectedBudgetName,
                    currencySymbol = currencySymbol.ifBlank { "$" },
                    widgetSize = widgetSize,
                    hiddenCategoryGroupIds = hiddenCategoryGroupIds,
                    hiddenCategoryIds = hiddenCategoryIds,
                    categoryViewMode = categoryViewMode,
                    categoryRowFormat = categoryRowFormat,
                    barScaleMode = barScaleMode,
                    normalizedScale = enabled,
                    showCents = showCents,
                    visibleBudgetStats = visibleBudgetStats,
                ),
            )
            val glanceId = GlanceAppWidgetManager(getApplication()).getGlanceIdBy(appWidgetId)
                ?: return@launch
            updateAppWidgetState(getApplication(), PreferencesGlanceStateDefinition, glanceId) { current ->
                current.toMutablePreferences().apply {
                    this[CategoryWidgetStateKeys.NORMALIZED_SCALE] = enabled
                }
            }
            CategoryGroupWidget().update(getApplication(), glanceId)
        }
    }

    fun applyBarScaleMode(mode: BarScaleMode) {
        barScaleMode = mode
        viewModelScope.launch {
            prefsStore.saveConfig(
                appWidgetId,
                WidgetConfig(
                    serverUrl = serverUrl,
                    apiKey = apiKey,
                    budgetId = selectedBudgetId,
                    budgetName = selectedBudgetName,
                    currencySymbol = currencySymbol.ifBlank { "$" },
                    widgetSize = widgetSize,
                    hiddenCategoryGroupIds = hiddenCategoryGroupIds,
                    hiddenCategoryIds = hiddenCategoryIds,
                    categoryViewMode = categoryViewMode,
                    categoryRowFormat = categoryRowFormat,
                    barScaleMode = mode,
                    normalizedScale = normalizedScale,
                    showCents = showCents,
                    visibleBudgetStats = visibleBudgetStats,
                ),
            )
            val glanceId = GlanceAppWidgetManager(getApplication()).getGlanceIdBy(appWidgetId)
                ?: return@launch
            updateAppWidgetState(getApplication(), PreferencesGlanceStateDefinition, glanceId) { current ->
                current.toMutablePreferences().apply {
                    this[CategoryWidgetStateKeys.BAR_SCALE_MODE] = mode.name
                }
            }
            CategoryGroupWidget().update(getApplication(), glanceId)
        }
    }

    fun applyViewMode(mode: CategoryViewMode) {
        categoryViewMode = mode
        viewModelScope.launch {
            prefsStore.saveConfig(
                appWidgetId,
                WidgetConfig(
                    serverUrl = serverUrl,
                    apiKey = apiKey,
                    budgetId = selectedBudgetId,
                    budgetName = selectedBudgetName,
                    currencySymbol = currencySymbol.ifBlank { "$" },
                    widgetSize = widgetSize,
                    hiddenCategoryGroupIds = hiddenCategoryGroupIds,
                    hiddenCategoryIds = hiddenCategoryIds,
                    categoryViewMode = mode,
                    categoryRowFormat = categoryRowFormat,
                    barScaleMode = barScaleMode,
                    normalizedScale = normalizedScale,
                    showCents = showCents,
                    visibleBudgetStats = visibleBudgetStats,
                ),
            )
            val glanceId = GlanceAppWidgetManager(getApplication()).getGlanceIdBy(appWidgetId)
                ?: return@launch
            updateAppWidgetState(getApplication(), PreferencesGlanceStateDefinition, glanceId) { current ->
                current.toMutablePreferences().apply {
                    this[CategoryWidgetStateKeys.VIEW_MODE] = mode.name
                }
            }
            CategoryGroupWidget().update(getApplication(), glanceId)
        }
    }

    private suspend fun loadConnectionDefaults() {
        val appWidgetManager = AppWidgetManager.getInstance(getApplication())
        val providers = listOf(
            ComponentName(getApplication(), BudgetWidgetReceiver::class.java),
            ComponentName(getApplication(), CategoryGroupWidgetReceiver::class.java),
        )
        for (provider in providers) {
            for (id in appWidgetManager.getAppWidgetIds(provider)) {
                if (id == appWidgetId) continue
                val config = prefsStore.getConfig(id) ?: continue
                serverUrl = config.serverUrl
                apiKey = config.apiKey
                currencySymbol = config.currencySymbol
                return
            }
        }
    }

    companion object {
        fun factory(appWidgetId: Int): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(
                modelClass: Class<T>,
                extras: CreationExtras,
            ): T = WidgetConfigViewModel(
                extras[APPLICATION_KEY]!!,
                appWidgetId,
            ) as T
        }
    }
}
