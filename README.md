# Android Weather App

A fully functional, modern Android Weather Application built with Kotlin and Jetpack Compose.

## Features
- **Modern Material 3 UI**: Clean, responsive, and adaptive interface following Material Design 3 guidelines.
- **MVVM Architecture**: Clean separation of concerns using ViewModel and Repository patterns.
- **Current Location Weather**: Uses GPS (`FusedLocationProviderClient`) to get weather for your current location.
- **City Search**: Search for any city worldwide to get current weather and forecasts.
- **Detailed Current Weather**: Displays temperature, humidity, wind speed, atmospheric pressure, and sunrise/sunset times.
- **Forecast Chart**: Visualizes the upcoming temperature trends using `MPAndroidChart` in a Jetpack Compose `AndroidView`.
- **Air Quality Index (AQI)**: Shows current AQI with health recommendations.
- **Local Persistence**: Caches your recent searches using Room Database.
- **Background Updates**: Uses `WorkManager` to periodically fetch weather data in the background (every 6 hours).
- **Severe Weather Alerts**: Sends local push notifications if rain or storms are detected in the upcoming forecast.
- **Dark/Light Mode**: Full support for system themes.

## Tech Stack
- **Kotlin & Jetpack Compose**
- **Retrofit & Gson**: For network requests to OpenWeatherMap API.
- **Room Database**: Local caching of search history.
- **WorkManager**: Reliable background processing.
- **MPAndroidChart**: Line chart for temperature forecasts.
- **Coil**: Image loading for weather condition icons.

## Setup Instructions

1. **Obtain API Key**
   - Create a free account at [OpenWeatherMap](https://openweathermap.org/).
   - Generate an API key.

2. **Configure API Key in the App**
   - In AI Studio, open the **Secrets** panel.
   - Add a new secret with the name `OPENWEATHER_API_KEY` and set its value to your API key.
   - The app reads this key securely at runtime via `BuildConfig.OPENWEATHER_API_KEY`.

3. **Build and Run**
   - The project is fully configured. You can use the AI Studio preview to run the application immediately.
   - Or, export the project and open it in Android Studio to run on a physical device or emulator.

## Note on Forecast
The free tier of OpenWeatherMap provides a 5-day / 3-hour forecast API. The chart displays the daily trends extracted from this free tier data.
