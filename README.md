# Expense Tracker (Android)

A Kotlin + Jetpack Compose expense tracker with Room persistence and USD↔COP daily exchange rates.

## Features
- Add expenses with payment type, value, place, date, description
- Currency: COP or USD
- USD exchange rate fetched per date (Frankfurter API)
- Totals between date ranges (with quick Today/Month/Year)
- Export/Import JSON
- Spanish + English resources

## Build
Open the project in Android Studio and run the `app` configuration.

To generate an APK:
1. **Build > Build Bundle(s) / APK(s) > Build APK(s)** in Android Studio
2. Install the generated APK on your Android device

## Notes on exchange rates
This app uses ExchangeRate-API open access for USD→COP daily rates.

### Endpoints used
- `GET https://open.er-api.com/v6/latest/USD`

### Attribution
Exchange rates provided by ExchangeRate-API (open.er-api.com).

## Export / Import
Exports are a JSON file with a list of expense items. Importing appends items to the local database.
