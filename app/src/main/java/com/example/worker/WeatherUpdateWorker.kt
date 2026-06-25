package com.example.worker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.BuildConfig
import com.example.R
import com.example.data.db.WeatherDatabase
import com.example.data.repository.WeatherRepository
import com.example.data.prefs.PreferencesManager
import kotlinx.coroutines.flow.firstOrNull

class WeatherUpdateWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val db = WeatherDatabase.getDatabase(applicationContext)
            val repo = WeatherRepository(db.weatherDao())
            val prefs = PreferencesManager(applicationContext)
            val apiKey = BuildConfig.OPENWEATHER_API_KEY
            
            val history = repo.getSearchHistory().firstOrNull()
            val lastCity = history?.firstOrNull()?.cityName
            
            if (lastCity != null) {
                val weather = repo.getWeatherByCity(lastCity, apiKey)
                val forecast = repo.getForecastByLocation(weather.coord.lat, weather.coord.lon, apiKey)
                
                val upcomingForecasts = forecast.list.take(4)
                
                if (prefs.isAlertEnabled(PreferencesManager.ALERT_RAIN_STORM)) {
                    val hasRain = upcomingForecasts.any { it.weather.any { w -> w.main.contains("Rain", true) || w.main.contains("Storm", true) || w.main.contains("Thunderstorm", true) } }
                    if (hasRain) showNotification("Rain/Storm Alert", "Rain or storm is expected in $lastCity soon.", 1)
                }
                
                if (prefs.isAlertEnabled(PreferencesManager.ALERT_SNOW)) {
                    val hasSnow = upcomingForecasts.any { it.weather.any { w -> w.main.contains("Snow", true) } }
                    if (hasSnow) showNotification("Snow Alert", "Snow is expected in $lastCity soon.", 2)
                }
                
                if (prefs.isAlertEnabled(PreferencesManager.ALERT_FOG)) {
                    val hasFog = upcomingForecasts.any { it.weather.any { w -> w.main.contains("Fog", true) || w.main.contains("Mist", true) } }
                    if (hasFog) showNotification("Fog Alert", "Fog is expected in $lastCity soon.", 3)
                }
                
                if (prefs.isAlertEnabled(PreferencesManager.ALERT_EXTREME_HEAT)) {
                    val hasHeat = upcomingForecasts.any { it.main.temp > 35.0 }
                    if (hasHeat) showNotification("Extreme Heat Alert", "Extreme heat (over 35°C) is expected in $lastCity.", 4)
                }
                
                if (prefs.isAlertEnabled(PreferencesManager.ALERT_STRONG_WINDS)) {
                    val hasWinds = upcomingForecasts.any { it.wind.speed > 15.0 }
                    if (hasWinds) showNotification("Strong Winds Alert", "Strong winds (over 15m/s) expected in $lastCity.", 5)
                }
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private fun showNotification(title: String, content: String, notificationId: Int) {
        val channelId = "weather_alerts"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Weather Alerts", NotificationManager.IMPORTANCE_DEFAULT)
            val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        if (ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            NotificationManagerCompat.from(applicationContext).notify(notificationId, builder.build())
        }
    }
}
