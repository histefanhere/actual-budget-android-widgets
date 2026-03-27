# Actual Budget Android Widgets

Home screen widgets for [Actual Budget](https://actualbudget.org/), connecting via the [actual-http-api](https://github.com/jhonderson/actual-http-api) REST wrapper.

## Widgets

### Monthly Summary
Shows an overview of this month's budget. You can choose which stats to display from:
- Income, From Last Month, Available Funds, Last Month Overspent, For Next Month
- Budgeted, To Budget, Spent, Balance

Automatically switches to a two-column layout on wider widgets (4+ columns).

### Category Breakdown
Shows spending progress for each budget category or group, with configurable progress bars and display formats:
- **Spent / Budgeted** — e.g. $50 / $200
- **Spent** — e.g. $50
- **Balance** — e.g. $150
- **Spent / Available** — e.g. $50 / $250 (includes carry-over)

## Requirements

- [Actual Budget](https://actualbudget.org/) — self-hosted budgeting app
- [actual-http-api](https://github.com/jhonderson/actual-http-api) — REST wrapper running alongside Actual Budget
- Android 8.0 (API 26) or higher

## Setup

1. Install and configure [actual-http-api](https://github.com/jhonderson/actual-http-api) pointing at your Actual Budget instance
2. Install the app (see [Releases](../../releases))
3. Long-press your home screen → Widgets → find **Monthly Summary** or **Category Breakdown**
4. Place the widget and fill in:
   - **Server URL** — e.g. `http://192.168.1.100:5006`
   - **API Key** — from your actual-http-api configuration
   - **Budget** — select from the list fetched from your server
5. Tap the check button to save; the widget will populate within a few seconds

Widgets refresh automatically every 30 minutes when connected to a network, or on demand via the refresh button.

## Configuration Options

### Both widgets
| Option | Description |
|--------|-------------|
| Symbol | Currency symbol shown next to amounts (e.g. `$`, `£`, `€`) |
| Size | Text/element size — Small, Medium, Large, or Massive |
| Show Cents | Toggle cent-precision vs. whole dollar amounts |

### Monthly Summary
| Option | Description |
|--------|-------------|
| Visible Stats | Choose which of the 9 stats appear on the widget |

### Category Breakdown
| Option | Description |
|--------|-------------|
| Show As | Groups or individual categories |
| Display Format | What figures to show per row (Spent/Budgeted, Spent, Balance, Spent/Available) |
| Progress Bars | Toggle the spending progress bars on/off |
| Proportional Scale | Scale all bars relative to the largest value (disabled when bars are off) |
| Hidden categories | Tap individual groups or categories to hide them from the widget |

## Building from Source

**Prerequisites:** Android Studio, JDK 17+

```bash
git clone https://github.com/histefanhere/ActualBudgetAndroidWidgets.git
cd ActualBudgetAndroidWidgets
./gradlew assembleDebug
```

The debug APK will be at `app/build/outputs/apk/debug/app-debug.apk`.

To install directly to a connected device:
```bash
./gradlew installDebug
```

## Notes on HTTP

Actual Budget is typically self-hosted on a local network over plain HTTP. The app permits cleartext HTTP traffic via `network_security_config.xml` to support this. HTTPS works without any changes.

## License

MIT — see [LICENSE](LICENSE)
