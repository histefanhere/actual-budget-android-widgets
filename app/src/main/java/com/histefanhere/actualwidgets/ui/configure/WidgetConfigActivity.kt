package com.histefanhere.actualwidgets.ui.configure

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalMinimumInteractiveComponentEnforcement
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.histefanhere.actualwidgets.R
import com.histefanhere.actualwidgets.model.BarScaleMode
import com.histefanhere.actualwidgets.model.BudgetStat
import com.histefanhere.actualwidgets.model.CategoryRowFormat
import com.histefanhere.actualwidgets.model.CategoryViewMode
import com.histefanhere.actualwidgets.model.WidgetSize

class WidgetConfigActivity : ComponentActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setResult(RESULT_CANCELED)

        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        val viewModel: WidgetConfigViewModel by viewModels {
            WidgetConfigViewModel.factory(appWidgetId)
        }

        setContent {
            MaterialTheme {
                WidgetConfigScreen(
                    viewModel = viewModel,
                    onSave = {
                        setResult(
                            RESULT_OK,
                            Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId),
                        )
                        finish()
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WidgetConfigScreen(
    viewModel: WidgetConfigViewModel,
    onSave: () -> Unit,
) {
    val canSave = viewModel.selectedBudgetId.isNotBlank() && !viewModel.isSaving

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(R.string.configure_widget_title))
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { if (canSave) viewModel.saveConfig(onSave) },
                containerColor = if (canSave) MaterialTheme.colorScheme.primary
                                 else MaterialTheme.colorScheme.surfaceVariant,
                contentColor = if (canSave) MaterialTheme.colorScheme.onPrimary
                               else MaterialTheme.colorScheme.onSurfaceVariant,
            ) {
                if (viewModel.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Icon(Icons.Default.Check, contentDescription = "Save widget")
                }
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {

            // ── Section 1: Server Connection ─────────────────────────────────
            ConfigSection(title = "Server Connection") {
                OutlinedTextField(
                    value = viewModel.serverUrl,
                    onValueChange = { viewModel.serverUrl = it },
                    label = { Text("Server URL") },
                    placeholder = { Text("http://192.168.1.1:5006") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Next,
                    ),
                )
                FieldHint("The address of your actual-http-api instance.")

                OutlinedTextField(
                    value = viewModel.apiKey,
                    onValueChange = { viewModel.apiKey = it },
                    label = { Text("API Key") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                )
                FieldHint("Found in your actual-http-api configuration file.")

                Button(
                    onClick = { viewModel.fetchBudgets() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = viewModel.serverUrl.isNotBlank()
                            && viewModel.apiKey.isNotBlank()
                            && !viewModel.isLoadingBudgets,
                ) {
                    if (viewModel.isLoadingBudgets) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Fetching…")
                    } else {
                        Text("Fetch Budgets")
                    }
                }

                viewModel.budgetLoadError?.let { error ->
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                if (viewModel.budgets.isNotEmpty()) {
                    var expanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = it },
                    ) {
                        OutlinedTextField(
                            value = viewModel.selectedBudgetName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Budget File") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                        ) {
                            viewModel.budgets.forEach { budget ->
                                DropdownMenuItem(
                                    text = { Text(budget.name) },
                                    onClick = {
                                        viewModel.selectedBudgetId = budget.groupId
                                        viewModel.selectedBudgetName = budget.name
                                        expanded = false
                                        if (!viewModel.isBudgetWidget) viewModel.fetchGroupsForConfig()
                                    },
                                )
                            }
                        }
                    }
                }
            }

            // ── Section 2: Widget Settings ────────────────────────────────────
            ConfigSection(title = "General") {
                OutlinedTextField(
                    value = viewModel.currencySymbol,
                    onValueChange = { viewModel.currencySymbol = it },
                    label = { Text("Symbol") },
                    modifier = Modifier.fillMaxWidth(0.4f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                )
                FieldHint("Currency to show all figures in.")

                SectionLabel("Widget Size")
                val sizeOptions = listOf(WidgetSize.SMALL, WidgetSize.MEDIUM, WidgetSize.LARGE, WidgetSize.MASSIVE)
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    sizeOptions.forEachIndexed { index, size ->
                        SegmentedButton(
                            selected = viewModel.widgetSize == size,
                            onClick = { viewModel.applySize(size) },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = sizeOptions.size),
                            label = { Text(size.name.lowercase().replaceFirstChar { it.uppercase() }) },
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                        Text(
                            text = "Show Cents",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            text = "Display figures in whole numbers only ($12) or full precision ($12.50).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = viewModel.showCents,
                        onCheckedChange = { viewModel.showCents = it },
                    )
                }
            }

            // ── Section 3: Content (budget widget only) ───────────────────────
            if (viewModel.isBudgetWidget) {
                ConfigSection(title = "Content") {
                    SectionLabel("Visible Stats")
                    FieldHint("Choose which figures appear on the widget. Order is fixed.")
                    CompositionLocalProvider(LocalMinimumInteractiveComponentEnforcement provides false) {
                        Column {
                            BudgetStat.entries.forEach { stat ->
                                val checked = stat in viewModel.visibleBudgetStats
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Checkbox(
                                        checked = checked,
                                        onCheckedChange = {
                                            viewModel.visibleBudgetStats =
                                                if (checked) viewModel.visibleBudgetStats - stat
                                                else viewModel.visibleBudgetStats + stat
                                        },
                                        modifier = Modifier.size(36.dp),
                                    )
                                    Text(
                                        text = stat.label,
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── Section 3/4: Display (category widget only) ───────────────────
            if (!viewModel.isBudgetWidget) {
                ConfigSection(title = "Display") {
                    // ── View ──────────────────────────────────────────────────
                    SectionLabel("View")
                    val viewModeOptions = listOf(CategoryViewMode.GROUPS, CategoryViewMode.CATEGORIES)
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        viewModeOptions.forEachIndexed { index, mode ->
                            SegmentedButton(
                                selected = viewModel.categoryViewMode == mode,
                                onClick = { viewModel.applyViewMode(mode) },
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = viewModeOptions.size),
                                label = { Text(if (mode == CategoryViewMode.GROUPS) "Groups" else "Categories") },
                            )
                        }
                    }
                    FieldHint("Groups combines all categories into one row per group.")

                    // ── Numbers ───────────────────────────────────────────────
                    SectionLabel("Number format")
                    var formatExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = formatExpanded,
                        onExpandedChange = { formatExpanded = it },
                    ) {
                        OutlinedTextField(
                            value = viewModel.categoryRowFormat.label,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(formatExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                        )
                        ExposedDropdownMenu(
                            expanded = formatExpanded,
                            onDismissRequest = { formatExpanded = false },
                        ) {
                            CategoryRowFormat.entries.forEach { format ->
                                DropdownMenuItem(
                                    text = { Text(format.label) },
                                    onClick = {
                                        viewModel.categoryRowFormat = format
                                        formatExpanded = false
                                    },
                                )
                            }
                        }
                    }
                    FieldHint(viewModel.categoryRowFormat.description)

                    // ── Progress Bars ─────────────────────────────────────────
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                            Text(
                                text = "Progress Bars",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Text(
                                text = "Show a spending bar on each row.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = viewModel.showProgressBars,
                            onCheckedChange = { viewModel.showProgressBars = it },
                        )
                    }

                    // ── Progress bar sub-options (indented) ───────────────────
                    if (viewModel.showProgressBars) {
                        Column(
                            modifier = Modifier.padding(start = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            // Bars represent
                            SectionLabel("Bars represent")
                            val barScaleModeOptions = listOf(BarScaleMode.SPENT_OF_BUDGETED, BarScaleMode.SPENT_OF_AVAILABLE)
                            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                                barScaleModeOptions.forEachIndexed { index, mode ->
                                    SegmentedButton(
                                        selected = viewModel.barScaleMode == mode,
                                        onClick = { viewModel.applyBarScaleMode(mode) },
                                        shape = SegmentedButtonDefaults.itemShape(index = index, count = barScaleModeOptions.size),
                                        label = { Text(mode.label) },
                                    )
                                }
                            }
                            FieldHint(viewModel.barScaleMode.description)

                            // Proportional width
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                                    Text(
                                        text = "Proportional width",
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                    Text(
                                        text = "Only the largest bar fills the full width; others are scaled proportionally.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Switch(
                                    checked = viewModel.normalizedScale,
                                    onCheckedChange = { viewModel.applyNormalizedScale(it) },
                                )
                            }
                        }
                    }
                }

                // ── Section 4: Filters (category widget only) ─────────────────
                ConfigSection(title = "Filters") {
                    FieldHint("Uncheck items to hide them from the widget.")
                    when {
                        viewModel.isLoadingGroups -> CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                        )
                        viewModel.groupLoadError != null -> Text(
                            text = viewModel.groupLoadError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                        viewModel.availableCategoryGroupsWithCategories.isNotEmpty() -> {
                            CompositionLocalProvider(LocalMinimumInteractiveComponentEnforcement provides false) {
                                Column {
                                    viewModel.availableCategoryGroupsWithCategories.forEach { group ->
                                        val groupVisible = group.id !in viewModel.hiddenCategoryGroupIds
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth(),
                                        ) {
                                            Checkbox(
                                                checked = groupVisible,
                                                onCheckedChange = { viewModel.toggleGroupHidden(group.id) },
                                                modifier = Modifier.size(36.dp),
                                            )
                                            Text(
                                                text = group.name,
                                                style = MaterialTheme.typography.bodyMedium,
                                            )
                                        }
                                        group.categories.forEach { category ->
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.fillMaxWidth().padding(start = 20.dp),
                                            ) {
                                                Checkbox(
                                                    checked = category.id !in viewModel.hiddenCategoryIds,
                                                    onCheckedChange = { viewModel.toggleCategoryHidden(category.id) },
                                                    enabled = groupVisible,
                                                    modifier = Modifier.size(36.dp),
                                                )
                                                Text(
                                                    text = category.name,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = if (groupVisible) MaterialTheme.colorScheme.onSurface
                                                            else MaterialTheme.colorScheme.onSurfaceVariant,
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        else -> FieldHint("Connect to your server and select a budget to manage filters.")
                    }
                }
            }

            // Space so the FAB doesn't overlap the last card
            Spacer(Modifier.height(80.dp))
        }
    }
}

// ── Section card ──────────────────────────────────────────────────────────────

@Composable
private fun ConfigSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            content()
        }
    }
}

// ── Shared helpers ────────────────────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun FieldHint(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
