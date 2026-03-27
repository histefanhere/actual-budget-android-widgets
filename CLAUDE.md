# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Android app that provides home screen widgets for [Actual Budget](https://actualbudget.org/) via the [actual-http-api](https://github.com/jhonderson/actual-http-api) REST wrapper. The app has **no launcher Activity** — it is entirely widget-based.

- **Package:** `com.histefanhere.actualwidgets`
- **Min SDK:** 26 (Android 8.0), **Compile/Target SDK:** 34
- **Language:** Kotlin + Jetpack Compose (config UI) + Jetpack Glance (widget UI)
- **Build system:** Gradle 8.0 with Kotlin DSL, AGP 8.1.0, Kotlin 1.9.22

## Build Commands

```bash
./gradlew assembleDebug          # Build debug APK
./gradlew assembleRelease        # Build release APK
./gradlew test                   # Run unit tests (JVM)
./gradlew connectedAndroidTest   # Run instrumentation tests (requires device/emulator)
./gradlew app:test --tests "com.histefanhere.actualwidgets.ExampleUnitTest"  # Single test
./gradlew clean                  # Clean build outputs
```

## Architecture

### Widget lifecycle
1. User adds widget → Android launches `WidgetConfigActivity` (with `EXTRA_APPWIDGET_ID`)
2. User fills in Server URL, API Key, Currency Symbol, selects a budget → taps **Save**
3. Config is persisted to `WidgetPrefsStore` (DataStore, keyed by `appWidgetId`)
4. A one-time + periodic `BudgetWidgetWorker` (WorkManager, every 30 min) is enqueued
5. Worker fetches data, writes result into Glance state (`PreferencesGlanceStateDefinition`), then calls `BudgetWidget().update()`
6. `BudgetWidget.provideGlance()` reads `currentState<Preferences>()` and renders the appropriate state view

### State machine (stored in Glance Preferences via `WidgetStateKeys`)
| `state_type` key | Meaning |
|---|---|
| `"loading"` | First load or transitional |
| `"not_configured"` | No config found for this widget ID |
| `"error"` | API call failed; `error_message` key has details |
| `"success"` | `summary_json` key holds a `BudgetSummary` JSON object |

The `app_widget_id` integer is also stored in Glance Preferences so that `RefreshAction` can look up the AppWidget ID without casting the opaque `GlanceId`.

### Key classes
| Class | Role |
|---|---|
| `BudgetWidget` | `GlanceAppWidget` — renders widget UI from Glance Preferences state |
| `BudgetWidgetReceiver` | `GlanceAppWidgetReceiver` — entry point for system update broadcasts |
| `BudgetWidgetWorker` | `CoroutineWorker` — fetches API data, updates Glance state |
| `RefreshAction` | `ActionCallback` — triggered by the ↻ button, enqueues a one-time worker |
| `WidgetConfigActivity` | `ComponentActivity` (Compose) — widget configuration screen |
| `WidgetConfigViewModel` | `AndroidViewModel` — drives config UI state, calls `BudgetRepository` |
| `BudgetRepository` | Aggregates expense category data from two API calls (this month + last month) |
| `ApiClientFactory` | Builds a Retrofit client with `x-api-key` header injection |
| `WidgetPrefsStore` | DataStore wrapper; per-widget config keyed by `appWidgetId` |

### actual-http-api integration
- Auth: `x-api-key` request header (set at `ApiClientFactory`)
- Endpoints used:
  - `GET /budgets` → list budget files
  - `GET /budgets/{budgetId}/months/{yyyy-MM}` → budget month detail
- Amounts are integer **cents** (`$10.50 → 1050`); divide by 100 to display
- Income category groups are filtered out (`is_income == true`); hidden groups/categories are also excluded
- HTTP (cleartext) traffic is allowed via `network_security_config.xml` for self-hosted instances on local networks

## Key Files

- `AndroidManifest.xml` — widget receiver, config activity (no launcher)
- `res/xml/budget_widget_info.xml` — `AppWidgetProviderInfo` (size, reconfigurable flag, config activity)
- `res/xml/network_security_config.xml` — permits cleartext HTTP
- `app/build.gradle.kts` — all dependency versions
