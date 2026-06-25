package com.example.data.prefs

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("weather_prefs", Context.MODE_PRIVATE)

    fun isAlertEnabled(alertType: String): Boolean {
        return prefs.getBoolean(alertType, false)
    }

    fun setAlertEnabled(alertType: String, enabled: Boolean) {
        prefs.edit().putBoolean(alertType, enabled).apply()
    }

    fun isFahrenheit(): Boolean {
        return prefs.getBoolean(UNIT_FAHRENHEIT, false)
    }

    fun setFahrenheit(enabled: Boolean) {
        prefs.edit().putBoolean(UNIT_FAHRENHEIT, enabled).apply()
    }

    companion object {
        const val ALERT_RAIN_STORM = "alert_rain_storm"
        const val ALERT_SNOW = "alert_snow"
        const val ALERT_FOG = "alert_fog"
        const val ALERT_EXTREME_HEAT = "alert_extreme_heat"
        const val ALERT_STRONG_WINDS = "alert_strong_winds"
        const val UNIT_FAHRENHEIT = "unit_fahrenheit"
    }
}
