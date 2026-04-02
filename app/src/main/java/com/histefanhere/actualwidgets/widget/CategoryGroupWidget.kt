package com.histefanhere.actualwidgets.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
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
import com.histefanhere.actualwidgets.R
import com.histefanhere.actualwidgets.model.BarScaleMode
import com.histefanhere.actualwidgets.model.CategoryGroupEntry
import com.histefanhere.actualwidgets.model.CategoryGroupsSnapshot
import com.histefanhere.actualwidgets.model.CategoryRowFormat
import com.histefanhere.actualwidgets.model.CategoryViewMode
import com.histefanhere.actualwidgets.model.TextSizes
import com.histefanhere.actualwidgets.model.WidgetSize
import com.histefanhere.actualwidgets.model.textSizes
import java.text.DateFormat
import java.text.NumberFormat
import java.util.Date
import kotlin.math.abs
import kotlin.math.roundToInt

private val ColorBackground       = ColorProvider(R.color.widget_background)
private val ColorAccent           = ColorProvider(R.color.widget_accent)
private val ColorOnSurface        = ColorProvider(R.color.widget_on_surface)
private val ColorOnSurfaceVariant = ColorProvider(R.color.widget_on_surface_variant)
private val ColorPositive         = ColorProvider(R.color.widget_positive)
private val ColorAmber            = ColorProvider(R.color.widget_amber)
private val ColorNegative         = ColorProvider(R.color.widget_negative)


class CategoryGroupWidget : GlanceAppWidget() {

    override val stateDefinition = PreferencesGlanceStateDefinition
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val prefs = currentState<Preferences>()
            val stateType = prefs[CategoryWidgetStateKeys.STATE_TYPE] ?: STATE_LOADING

            GlanceTheme {
                when (stateType) {
                    STATE_NOT_CONFIGURED -> NotConfiguredContent()
                    STATE_ERROR -> ErrorContent(prefs[CategoryWidgetStateKeys.ERROR_MESSAGE] ?: "Unknown error")
                    STATE_SUCCESS -> {
                        val viewMode = prefs[CategoryWidgetStateKeys.VIEW_MODE]
                            ?.let { runCatching { CategoryViewMode.valueOf(it) }.getOrNull() }
                            ?: CategoryViewMode.GROUPS
                        val json = when (viewMode) {
                            CategoryViewMode.GROUPS     -> prefs[CategoryWidgetStateKeys.GROUPS_JSON]
                            CategoryViewMode.CATEGORIES -> prefs[CategoryWidgetStateKeys.CATEGORIES_JSON]
                        }
                        val normalizedScale = prefs[CategoryWidgetStateKeys.NORMALIZED_SCALE] ?: false
                        val sizes = prefs[CategoryWidgetStateKeys.WIDGET_SIZE]
                            ?.let { runCatching { WidgetSize.valueOf(it) }.getOrNull() }
                            ?.textSizes ?: WidgetSize.MEDIUM.textSizes
                        val showCents = prefs[CategoryWidgetStateKeys.SHOW_CENTS] ?: true
                        val showProgressBars = prefs[CategoryWidgetStateKeys.SHOW_PROGRESS_BARS] ?: true
                        val rowFormat = prefs[CategoryWidgetStateKeys.CATEGORY_ROW_FORMAT]
                            ?.let { runCatching { CategoryRowFormat.valueOf(it) }.getOrNull() }
                            ?: CategoryRowFormat.SPENT_OF_BUDGETED
                        val barScaleMode = prefs[CategoryWidgetStateKeys.BAR_SCALE_MODE]
                            ?.let { runCatching { BarScaleMode.valueOf(it) }.getOrNull() }
                            ?: BarScaleMode.SPENT_OF_BUDGETED
                        val snapshot = json?.let { Gson().fromJson(it, CategoryGroupsSnapshot::class.java) }
                        if (snapshot != null) SuccessContent(snapshot, viewMode, normalizedScale, barScaleMode, sizes, showCents, showProgressBars, rowFormat) else LoadingContent()
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
private fun ErrorContent(message: String) {
    WidgetSurface {
        Column(
            modifier = GlanceModifier.fillMaxSize().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Error",
                style = TextStyle(color = ColorNegative, fontSize = 13.sp, fontWeight = FontWeight.Bold),
            )
            Spacer(GlanceModifier.height(4.dp))
            Text(
                text = message,
                style = TextStyle(color = ColorOnSurfaceVariant, fontSize = 11.sp),
                maxLines = 3,
            )
            Spacer(GlanceModifier.height(10.dp))
            Image(
                provider = ImageProvider(R.drawable.ic_refresh),
                contentDescription = "Retry",
                colorFilter = ColorFilter.tint(ColorAccent),
                modifier = GlanceModifier
                    .size(22.dp)
                    .clickable(actionRunCallback<CategoryRefreshAction>()),
            )
        }
    }
}

@Composable
private fun SuccessContent(
    snapshot: CategoryGroupsSnapshot,
    viewMode: CategoryViewMode,
    normalizedScale: Boolean,
    barScaleMode: BarScaleMode,
    sizes: TextSizes,
    showCents: Boolean,
    showProgressBars: Boolean,
    rowFormat: CategoryRowFormat,
) {
    val maxValue: Long = if (normalizedScale) {
        when (barScaleMode) {
            BarScaleMode.SPENT_OF_BUDGETED  -> snapshot.groups.maxOfOrNull { it.budgeted } ?: 0L
            BarScaleMode.SPENT_OF_AVAILABLE -> snapshot.groups.maxOfOrNull { it.balance + abs(it.spent) } ?: 0L
        }
    } else 0L
    WidgetSurface {
        Column(modifier = GlanceModifier.fillMaxSize().padding(sizes.paddingDp.dp)) {
            LazyColumn(modifier = GlanceModifier.fillMaxWidth().defaultWeight()) {
                item {
                    Row(
                        modifier = GlanceModifier.fillMaxWidth().padding(bottom = (sizes.paddingDp * 0.75f).dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = snapshot.monthLabel,
                            style = TextStyle(
                                color = ColorAccent,
                                fontSize = sizes.headerSp.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                            modifier = GlanceModifier.defaultWeight(),
                        )
                        Image(
                            provider = ImageProvider(R.drawable.ic_refresh),
                            contentDescription = "Refresh",
                            colorFilter = ColorFilter.tint(ColorOnSurfaceVariant),
                            modifier = GlanceModifier
                                .size(sizes.headerSp.dp)
                                .clickable(actionRunCallback<CategoryRefreshAction>()),
                        )
                    }
                }
                items(snapshot.groups) { group ->
                    GroupRow(group, snapshot.currencySymbol, maxValue, barScaleMode, sizes, showCents, showProgressBars, rowFormat)
                }
            }
            Text(
                text = "Updated ${formatTime(snapshot.lastUpdatedMs)}",
                style = TextStyle(color = ColorOnSurfaceVariant, fontSize = sizes.footerSp.sp),
            )
        }
    }
}

// ─── Group row ────────────────────────────────────────────────────────────────

@Composable
private fun GroupRow(group: CategoryGroupEntry, currencySymbol: String, maxValue: Long, barScaleMode: BarScaleMode, sizes: TextSizes, showCents: Boolean, showProgressBars: Boolean, rowFormat: CategoryRowFormat) {
    val context = LocalContext.current
    val widgetWidth = LocalSize.current.width
    val absSpent = abs(group.spent)
    val available = group.balance + absSpent  // budgeted + carry-over from last month
    val denominator: Long = when (barScaleMode) {
        BarScaleMode.SPENT_OF_BUDGETED  -> group.budgeted
        BarScaleMode.SPENT_OF_AVAILABLE -> available
    }
    val rawRatio = when {
        denominator <= 0L && absSpent == 0L -> 0f
        denominator <= 0L -> 2f
        else -> absSpent.toFloat() / denominator.toFloat()
    }
    val fillRatio = rawRatio.coerceIn(0f, 1f)
    val trackRatio = if (maxValue > 0L && denominator > 0L) {
        (denominator.toFloat() / maxValue.toFloat()).coerceIn(0.02f, 1f)
    } else {
        1f
    }
    val barColorRes = when {
        rawRatio >= 1f -> R.color.widget_negative
        rawRatio > 0.8f -> R.color.widget_amber
        else -> R.color.widget_positive
    }
    val barColorProvider = when {
        rawRatio >= 1f -> ColorNegative
        rawRatio > 0.8f -> ColorAmber
        else -> ColorPositive
    }
    val barHeightDp = sizes.paddingDp * 0.75f
    val density = context.resources.displayMetrics.density
    val barBitmap = remember(fillRatio, trackRatio, barColorRes, widgetWidth, density, sizes.paddingDp) {
        val widthPx = ((widgetWidth.value - sizes.paddingDp * 2f) * density).roundToInt().coerceAtLeast(1)
        val heightPx = (barHeightDp * density).roundToInt().coerceAtLeast(1)
        createBarBitmap(
            widthPx = widthPx,
            heightPx = heightPx,
            fillRatio = fillRatio,
            trackRatio = trackRatio,
            fillColor = context.getColor(barColorRes),
            trackColor = context.getColor(R.color.widget_bar_track),
        )
    }

    Column(modifier = GlanceModifier.fillMaxWidth()) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = group.name,
                style = TextStyle(color = ColorOnSurface, fontSize = sizes.valueSp.sp),
                modifier = GlanceModifier.defaultWeight(),
                maxLines = 1,
            )
            Text(
                text = when (rowFormat) {
                    CategoryRowFormat.SPENT_OF_BUDGETED   -> "${formatAmount(absSpent, currencySymbol, showCents)} / ${formatAmount(group.budgeted, currencySymbol, showCents)}"
                    CategoryRowFormat.SPENT               -> formatAmount(absSpent, currencySymbol, showCents)
                    CategoryRowFormat.BALANCE             -> formatAmount(group.balance, currencySymbol, showCents)
                    CategoryRowFormat.AVAILABLE_BREAKDOWN -> "${formatAmount(absSpent, currencySymbol, showCents)} / ${formatAmount(group.balance + absSpent, currencySymbol, showCents)}"
                },
                style = TextStyle(color = barColorProvider, fontSize = sizes.valueSp.sp, fontWeight = FontWeight.Bold),
                maxLines = 1,
            )
        }
        if (showProgressBars) {
            Spacer(GlanceModifier.height((sizes.paddingDp * 0.375f).dp))
            if (denominator > 0L || absSpent > 0L) {
                Image(
                    provider = ImageProvider(barBitmap),
                    contentDescription = null,
                    modifier = GlanceModifier.fillMaxWidth().height(barHeightDp.dp),
                )
            } else {
                Spacer(GlanceModifier.height(barHeightDp.dp))
            }
        }
        Spacer(GlanceModifier.height(sizes.paddingDp.dp))
    }
}

private fun createBarBitmap(
    widthPx: Int,
    heightPx: Int,
    fillRatio: Float,
    trackRatio: Float,
    fillColor: Int,
    trackColor: Int,
): Bitmap {
    val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    val radius = heightPx / 2f
    val trackRight = widthPx * trackRatio

    // Clip everything to the track's rounded outline so both the track and fill
    // are always perfectly pill-shaped regardless of how short the fill bar is.
    val clipPath = Path().apply {
        addRoundRect(RectF(0f, 0f, trackRight, heightPx.toFloat()), radius, radius, Path.Direction.CW)
    }
    canvas.clipPath(clipPath)

    // Track background
    paint.color = trackColor
    canvas.drawRect(0f, 0f, trackRight, heightPx.toFloat(), paint)

    // Fill bar: start at -radius so the left rounded cap is hidden by the clip,
    // leaving only the right cap visible and rounded at all fill levels.
    if (fillRatio > 0f) {
        paint.color = fillColor
        canvas.drawRoundRect(-radius, 0f, trackRight * fillRatio, heightPx.toFloat(), radius, radius, paint)
    }

    return bitmap
}

// ─── Shared composables ───────────────────────────────────────────────────────

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

// ─── Formatting helpers ───────────────────────────────────────────────────────

private fun formatAmount(cents: Long, symbol: String, showCents: Boolean): String {
    val whole = cents / 100
    val wholeFormatted = NumberFormat.getIntegerInstance().format(whole)
    return if (showCents) {
        val decimal = cents % 100
        "$symbol$wholeFormatted.${decimal.toString().padStart(2, '0')}"
    } else {
        "$symbol$wholeFormatted"
    }
}

private fun formatTime(epochMs: Long): String =
    DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(epochMs))
