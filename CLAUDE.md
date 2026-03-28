# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Android app that provides home screen widgets for [Actual Budget](https://actualbudget.org/) via the [actual-http-api](https://github.com/jhonderson/actual-http-api) REST wrapper. The app has **no launcher Activity** — it is entirely widget-based. There are **two distinct widgets**:

1. **Monthly Summary** (`BudgetWidget`) — overview of budget statistics for the current month
2. **Category Breakdown** (`CategoryGroupWidget`) — spending progress bars per category/group

- **Package:** `com.histefanhere.actualwidgets`
- **Min SDK:** 26 (Android 8.0), **Compile/Target SDK:** 34
- **Language:** Kotlin + Jetpack Compose (config UI) + Jetpack Glance (widget UI)
- **Build system:** Gradle with Kotlin DSL, AGP 9.1.0, Kotlin 2.2.10, Java 17

## Build Commands

```bash
./gradlew assembleDebug          # Build debug APK
./gradlew assembleRelease        # Build release APK (minified with ProGuard)
./gradlew test                   # Run unit tests (JVM)
./gradlew connectedAndroidTest   # Run instrumentation tests (requires device/emulator)
./gradlew app:test --tests "com.histefanhere.actualwidgets.ExampleUnitTest"  # Single test
./gradlew clean                  # Clean build outputs
```

Note: Tests are currently placeholder-only (`ExampleUnitTest`, `ExampleInstrumentedTest`); no domain logic is tested yet.

## Architecture

Both widgets share the same `WidgetConfigActivity` and `WidgetPrefsStore`, but have parallel worker/receiver/state-key implementations.

### Widget lifecycle
1. User adds widget → Android launches `WidgetConfigActivity` (with `EXTRA_APPWIDGET_ID`)
2. User fills in Server URL, API Key, Currency Symbol, selects a budget and configures display options → taps **Save**
3. Config is persisted to `WidgetPrefsStore` (DataStore, keyed by `appWidgetId`)
4. A one-time + periodic worker (WorkManager, every 30 min, network-constrained) is enqueued
5. Worker fetches data, writes result into Glance state (`PreferencesGlanceStateDefinition`), then calls the widget's `.update()`
6. `provideGlance()` reads `currentState<Preferences>()` and renders the appropriate state view

Workers use `ExistingPeriodicWorkPolicy.KEEP` with a unique name `${WORK_PREFIX}_periodic_${widgetId}`. Work is cancelled in `onDeleted`. On failure, workers store the error message in Glance state and return `Result.retry()`.

### Storage layers
- **`WidgetPrefsStore`** (DataStore): persists user `WidgetConfig` per widget ID (server URL, API key, display options, hidden category IDs, etc.)
- **Glance Preferences** (via `updateAppWidgetState`): stores the current render state (loading/error/success) and data snapshot for display

### State machine
Both widgets use the same state type values, but separate keys objects (`WidgetStateKeys` vs `CategoryWidgetStateKeys`):

| `state_type` value | Meaning |
|---|---|
| `"loading"` | First load or transitional |
| `"not_configured"` | No config found for this widget ID |
| `"error"` | API call failed; `error_message` key has details |
| `"success"` | Data keys hold JSON snapshot for rendering |

`app_widget_id` is stored in Glance Preferences so that `RefreshAction` / `CategoryRefreshAction` can resolve the AppWidget ID from the opaque `GlanceId`.

### Widget configuration (`WidgetConfig`)
All configuration is stored in `WidgetConfig` and persisted via `WidgetPrefsStore`:
- `serverUrl`, `apiKey`, `currencySymbol`, `budgetId`, `budgetName`
- `widgetSize: WidgetSize` — `SMALL | MEDIUM | LARGE | MASSIVE`; controls font sizes (`headerSp`, `labelSp`, `valueSp`, `footerSp`, `paddingDp`)
- `visibleBudgetStats: Set<BudgetStat>` — which of 9 stats to show (default: `{BUDGETED, SPENT, BALANCE}`)
- `hiddenCategoryGroupIds: Set<String>`, `hiddenCategoryIds: Set<String>` — user-excluded items
- `categoryViewMode: CategoryViewMode` — `GROUPS | CATEGORIES`
- `categoryRowFormat: CategoryRowFormat` — `SPENT_OF_BUDGETED | SPENT | BALANCE | AVAILABLE_BREAKDOWN`
- `normalizedScale: Boolean` — scales all progress bars relative to the largest value
- `showProgressBars: Boolean`

### BudgetStat enum (9 values, render order matches declaration order)
`INCOME`, `FROM_LAST_MONTH`, `AVAILABLE_FUNDS`, `LAST_MONTH_OVERSPENT`, `FOR_NEXT_MONTH`, `BUDGETED`, `TO_BUDGET`, `SPENT`, `BALANCE`

### Key classes
| Class | Role |
|---|---|
| `BudgetWidget` / `CategoryGroupWidget` | `GlanceAppWidget` — renders widget UI from Glance Preferences state |
| `BudgetWidgetReceiver` / `CategoryGroupWidgetReceiver` | `GlanceAppWidgetReceiver` — entry point for system update broadcasts |
| `BudgetWidgetWorker` / `CategoryGroupWidgetWorker` | `CoroutineWorker` — fetches API data, updates Glance state |
| `RefreshAction` / `CategoryRefreshAction` | `ActionCallback` — triggered by ↻ button, enqueues one-time worker |
| `WidgetConfigActivity` | `ComponentActivity` (Compose) — shared config screen for both widgets |
| `WidgetConfigViewModel` | `AndroidViewModel` — drives config UI; `isBudgetWidget` flag differentiates behavior |
| `BudgetRepository` | Aggregates data from API; has separate fetch methods for summary, groups, individual categories |
| `ApiClientFactory` | Builds Retrofit client with `x-api-key` header; 15s connect / 30s read timeouts |
| `WidgetPrefsStore` | DataStore wrapper; per-widget config keyed by `appWidgetId` |

### Responsive layout
- `BudgetWidget` switches to 2-column layout when widget width ≥ 280dp
- `CategoryGroupWidget` has similar responsive breakpoints
- Widget sizes: Budget Summary targets 2×2 cells (min 110×110dp); Category Breakdown targets 2×3 cells (min 180×180dp)
- Both use `updatePeriodMillis=0` (no system polling; WorkManager handles all updates)

### actual-http-api integration
- Auth: `x-api-key` request header (set in `ApiClientFactory`)
- Endpoints used:
  - `GET /budgets` → list budget files
  - `GET /budgets/{budgetId}/months/{yyyy-MM}` → budget month detail (called twice: current + last month)
- Amounts are integer **cents** (`$10.50 → 1050`); divide by 100 to display
- Negative amounts are prefixed with "−" (Unicode minus, not hyphen)
- Income category groups (`is_income == true`) and hidden groups/categories are filtered out
- HTTP (cleartext) traffic is allowed via `network_security_config.xml` for self-hosted local instances

### Theming
Widget colors are defined in `res/values/colors.xml` (and `values-night/`) as semantic names: `widget_background`, `widget_accent` (purple), `widget_positive` (green), `widget_negative` (red), `widget_amber`, `widget_bar_track`, `widget_on_surface`, `widget_on_surface_variant`.

## Key Files

- `AndroidManifest.xml` — both widget receivers, config activity (no launcher)
- `res/xml/budget_widget_info.xml` / `res/xml/category_group_widget_info.xml` — `AppWidgetProviderInfo`
- `res/xml/network_security_config.xml` — permits cleartext HTTP
- `app/build.gradle.kts` — all dependency versions
