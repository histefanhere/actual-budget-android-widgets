package com.histefanhere.actualwidgets.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalSize
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.google.gson.Gson
import java.text.DateFormat
import java.text.NumberFormat
import java.util.Date
import com.histefanhere.actualwidgets.R
import com.histefanhere.actualwidgets.model.BudgetStat
import com.histefanhere.actualwidgets.model.BudgetSummary
import com.histefanhere.actualwidgets.model.TextSizes
import com.histefanhere.actualwidgets.model.WidgetSize
import com.histefanhere.actualwidgets.model.textSizes
import kotlin.math.abs

private val ColorBackground       = ColorProvider(R.color.widget_background)
private val ColorAccent           = ColorProvider(R.color.widget_accent)
private val ColorOnSurface        = ColorProvider(R.color.widget_on_surface)
private val ColorOnSurfaceVariant = ColorProvider(R.color.widget_on_surface_variant)
private val ColorPositive         = ColorProvider(R.color.widget_positive)
private val ColorNegative         = ColorProvider(R.color.widget_negative)

class MonthlySummaryWidget : GlanceAppWidget() {

    override val stateDefinition = PreferencesGlanceStateDefinition
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val prefs = currentState<Preferences>()
            val stateType = prefs[MonthlySummaryStateKeys.STATE_TYPE] ?: WidgetState.LOADING

            GlanceTheme {
                when (stateType) {
                    WidgetState.NOT_CONFIGURED -> NotConfiguredContent()
                    WidgetState.ERROR -> {
                        val sizeStr = prefs[MonthlySummaryStateKeys.WIDGET_SIZE] ?: WidgetSize.MEDIUM.name
                        val sizes = runCatching { WidgetSize.valueOf(sizeStr) }.getOrDefault(WidgetSize.MEDIUM).textSizes
                        ErrorContent(prefs[MonthlySummaryStateKeys.ERROR_MESSAGE] ?: "Unknown error", sizes)
                    }
                    WidgetState.SUCCESS -> {
                        val json = prefs[MonthlySummaryStateKeys.SUMMARY_JSON]
                        val summary = json?.let { Gson().fromJson(it, BudgetSummary::class.java) }
                        val sizeStr = prefs[MonthlySummaryStateKeys.WIDGET_SIZE] ?: WidgetSize.MEDIUM.name
                        val sizes = runCatching { WidgetSize.valueOf(sizeStr) }
                            .getOrDefault(WidgetSize.MEDIUM).textSizes
                        val showCents = prefs[MonthlySummaryStateKeys.SHOW_CENTS] ?: true
                        val showMonthArrows = prefs[MonthlySummaryStateKeys.SHOW_MONTH_ARROWS] ?: true
                        val showRefreshIcon = prefs[MonthlySummaryStateKeys.SHOW_REFRESH_ICON] ?: true
                        val visibleStats = prefs[MonthlySummaryStateKeys.VISIBLE_BUDGET_STATS]
                            ?.split(",")
                            ?.mapNotNull { runCatching { BudgetStat.valueOf(it) }.getOrNull() }
                            ?.toSet()
                            ?: BudgetStat.DEFAULT
                        if (summary != null) SuccessContent(summary, sizes, showCents, visibleStats, showMonthArrows, showRefreshIcon) else LoadingContent()
                    }
                    else -> LoadingContent()
                }
            }
        }
    }
}

// ─── State views ─────────────────────────────────────────────────────────────

@Composable
private fun LoadingContent() {
    WidgetSurface {
        Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "Loading…",
                style = TextStyle(color = ColorOnSurfaceVariant, fontSize = 13.sp),
            )
        }
    }
}

@Composable
private fun NotConfiguredContent() {
    WidgetSurface {
        Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "Tap & hold to configure",
                style = TextStyle(color = ColorOnSurfaceVariant, fontSize = 13.sp),
            )
        }
    }
}

@Composable
private fun ErrorContent(message: String, sizes: TextSizes) {
    WidgetSurface {
        Column(
            modifier = GlanceModifier.fillMaxSize().padding(sizes.paddingDp.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Error",
                style = TextStyle(
                    color = ColorNegative,
                    fontSize = sizes.headerSp.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
            Spacer(GlanceModifier.height((sizes.paddingDp * 0.5f).dp))
            Text(
                text = message,
                style = TextStyle(color = ColorOnSurfaceVariant, fontSize = sizes.labelSp.sp),
                maxLines = 3,
            )
            Spacer(GlanceModifier.height(sizes.paddingDp.dp))
            Image(
                provider = ImageProvider(R.drawable.ic_refresh),
                contentDescription = "Retry",
                colorFilter = ColorFilter.tint(ColorAccent),
                modifier = GlanceModifier
                    .size((sizes.headerSp * 1.4f).dp)
                    .clickable(actionRunCallback<MonthlySummaryRefreshAction>()),
            )
        }
    }
}

/** Resolves color for a stat value given its raw (unformatted) amount. */
private fun statColor(stat: BudgetStat, summary: BudgetSummary): ColorProvider = when (stat) {
    BudgetStat.INCOME ->
        if (summary.totalIncome >= 0) ColorPositive else ColorNegative
    BudgetStat.FROM_LAST_MONTH ->
        if (summary.fromLastMonth >= 0) ColorPositive else ColorNegative
    BudgetStat.AVAILABLE_FUNDS ->
        if (summary.incomeAvailable >= 0) ColorOnSurface else ColorNegative
    BudgetStat.LAST_MONTH_OVERSPENT ->
        if (summary.lastMonthOverspent >= 0) ColorOnSurface else ColorNegative
    BudgetStat.FOR_NEXT_MONTH -> ColorOnSurface
    BudgetStat.BUDGETED -> ColorOnSurface
    BudgetStat.TO_BUDGET ->
        if (summary.toBudget >= 0) ColorOnSurface else ColorNegative
    BudgetStat.SPENT ->
        if (summary.totalSpent < 0) ColorNegative else ColorOnSurface
    BudgetStat.BALANCE ->
        if (summary.totalBalance >= 0) ColorPositive else ColorNegative
}

private fun statRawValue(stat: BudgetStat, summary: BudgetSummary): Long = when (stat) {
    BudgetStat.INCOME               -> summary.totalIncome
    BudgetStat.FROM_LAST_MONTH      -> summary.fromLastMonth
    BudgetStat.AVAILABLE_FUNDS      -> summary.incomeAvailable
    BudgetStat.LAST_MONTH_OVERSPENT -> summary.lastMonthOverspent
    BudgetStat.FOR_NEXT_MONTH       -> summary.forNextMonth
    BudgetStat.BUDGETED             -> abs(summary.totalBudgeted)
    BudgetStat.TO_BUDGET            -> summary.toBudget
    BudgetStat.SPENT                -> abs(summary.totalSpent)
    BudgetStat.BALANCE              -> summary.totalBalance
}

@Composable
private fun SuccessContent(
    summary: BudgetSummary,
    sizes: TextSizes,
    showCents: Boolean,
    visibleStats: Set<BudgetStat>,
    showMonthArrows: Boolean,
    showRefreshIcon: Boolean,
) {
    val isWide = LocalSize.current.width >= 230.dp
    val statList = BudgetStat.entries.filter { it in visibleStats }

    WidgetSurface {
        if (isWide) {
            WideLayout(summary, sizes, showCents, statList, showMonthArrows, showRefreshIcon)
        } else {
            NarrowLayout(summary, sizes, showCents, statList, showMonthArrows, showRefreshIcon)
        }
    }
}

@Composable
private fun NarrowLayout(
    summary: BudgetSummary,
    sizes: TextSizes,
    showCents: Boolean,
    statList: List<BudgetStat>,
    showMonthArrows: Boolean,
    showRefreshIcon: Boolean,
) {
    Column(modifier = GlanceModifier.fillMaxSize().padding(sizes.paddingDp.dp)) {
        LazyColumn(modifier = GlanceModifier.fillMaxWidth().defaultWeight()) {
            item {
                StatHeader(summary, sizes, showMonthArrows, showRefreshIcon, GlanceModifier.padding(bottom = (sizes.paddingDp * 0.75f).dp))
            }
            statList.forEach { stat ->
                item {
                    StatColumn(
                        label = stat.label,
                        value = formatAmount(statRawValue(stat, summary), summary.currencySymbol, showCents),
                        valueColor = statColor(stat, summary),
                        sizes = sizes,
                    )
                }
            }
        }
        Text(
            text = "Updated ${formatTime(summary.lastUpdatedMs)}",
            style = TextStyle(color = ColorOnSurfaceVariant, fontSize = sizes.footerSp.sp),
        )
    }
}

@Composable
private fun WideLayout(
    summary: BudgetSummary,
    sizes: TextSizes,
    showCents: Boolean,
    statList: List<BudgetStat>,
    showMonthArrows: Boolean,
    showRefreshIcon: Boolean,
) {
    Column(modifier = GlanceModifier.fillMaxSize().padding(sizes.paddingDp.dp)) {
        LazyColumn(modifier = GlanceModifier.fillMaxWidth().defaultWeight()) {
            item {
                StatHeader(summary, sizes, showMonthArrows, showRefreshIcon, GlanceModifier.padding(bottom = (sizes.paddingDp * 0.75f).dp))
            }
            // Pair up consecutive stats so each row holds two side-by-side StatColumns
            statList.chunked(2).forEach { pair ->
                item {
                    Row(modifier = GlanceModifier.fillMaxWidth()) {
                        StatColumn(
                            label = pair[0].label,
                            value = formatAmount(statRawValue(pair[0], summary), summary.currencySymbol, showCents),
                            valueColor = statColor(pair[0], summary),
                            sizes = sizes,
                            modifier = GlanceModifier.defaultWeight(),
                        )
                        if (pair.size > 1) {
                            StatColumn(
                                label = pair[1].label,
                                value = formatAmount(statRawValue(pair[1], summary), summary.currencySymbol, showCents),
                                valueColor = statColor(pair[1], summary),
                                sizes = sizes,
                                modifier = GlanceModifier.defaultWeight(),
                            )
                        }
                    }
                }
            }
        }
        Text(
            text = "Updated ${formatTime(summary.lastUpdatedMs)}",
            style = TextStyle(color = ColorOnSurfaceVariant, fontSize = sizes.footerSp.sp),
        )
    }
}

@Composable
private fun StatHeader(
    summary: BudgetSummary,
    sizes: TextSizes,
    showMonthArrows: Boolean,
    showRefreshIcon: Boolean,
    modifier: GlanceModifier = GlanceModifier,
) {
    Row(
        modifier = GlanceModifier.fillMaxWidth().then(modifier),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SectionLabel(
            text = summary.monthLabel,
            fontSize = sizes.headerSp,
            modifier = GlanceModifier.defaultWeight(),
        )
        if (showMonthArrows) {
            Image(
                provider = ImageProvider(R.drawable.ic_chevron_left),
                contentDescription = "Previous month",
                colorFilter = ColorFilter.tint(ColorOnSurfaceVariant),
                modifier = GlanceModifier
                    .size((sizes.headerSp * 1.4f).dp)
                    .clickable(actionRunCallback<MonthlySummaryPreviousMonthAction>()),
            )
            Image(
                provider = ImageProvider(R.drawable.ic_chevron_right),
                contentDescription = "Next month",
                colorFilter = ColorFilter.tint(ColorOnSurfaceVariant),
                modifier = GlanceModifier
                    .size((sizes.headerSp * 1.4f).dp)
                    .clickable(actionRunCallback<MonthlySummaryNextMonthAction>()),
            )
        }
        if (showRefreshIcon) {
            Image(
                provider = ImageProvider(R.drawable.ic_refresh),
                contentDescription = "Refresh",
                colorFilter = ColorFilter.tint(ColorOnSurfaceVariant),
                modifier = GlanceModifier
                    .size((sizes.headerSp * 1.4f).dp)
                    .clickable(actionRunCallback<MonthlySummaryRefreshAction>()),
            )
        }
    }
}

// ─── Reusable composables ─────────────────────────────────────────────────────

@Composable
private fun WidgetSurface(content: @Composable () -> Unit) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorBackground)
            .cornerRadius(16.dp),
        content = content,
    )
}

/** Uppercase section title, optionally accepting a modifier (e.g. defaultWeight inside a Row). */
@Composable
private fun SectionLabel(text: String, fontSize: Float = 10f, modifier: GlanceModifier = GlanceModifier) {
    Text(
        text = text,
        style = TextStyle(
            color = ColorAccent,
            fontSize = fontSize.sp,
            fontWeight = FontWeight.Bold,
        ),
        modifier = modifier,
    )
}

@Composable
private fun StatColumn(
    label: String,
    value: String,
    sizes: TextSizes,
    valueColor: ColorProvider = ColorOnSurface,
    modifier: GlanceModifier = GlanceModifier,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.Start) {
        Text(
            text = label,
            style = TextStyle(
                color = ColorOnSurfaceVariant,
                fontSize = sizes.labelSp.sp,
                fontWeight = FontWeight.Medium,
            ),
            maxLines = 1,
        )
        Text(
            text = value,
            style = TextStyle(
                color = valueColor,
                fontSize = sizes.valueSp.sp,
                fontWeight = FontWeight.Bold,
            ),
            maxLines = 1,
        )
    }
}

// ─── Formatting helpers ───────────────────────────────────────────────────────

/**
 * Formats [cents] as a currency string.
 * Actual Budget stores amounts as integer cents: $10.50 → 1050.
 * Negative values are prefixed with "−".
 */
private fun formatAmount(cents: Long, symbol: String, showCents: Boolean): String {
    val negative = cents < 0
    val abs = abs(cents)
    val whole = abs / 100
    val wholeFormatted = NumberFormat.getIntegerInstance().format(whole)
    val formatted = if (showCents) {
        val decimal = abs % 100
        "$symbol$wholeFormatted.${decimal.toString().padStart(2, '0')}"
    } else {
        "$symbol$wholeFormatted"
    }
    return if (negative) "−$formatted" else formatted
}

private fun formatTime(epochMs: Long): String =
    DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(epochMs))
