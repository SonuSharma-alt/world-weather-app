package com.example.data.model

import com.google.gson.annotations.SerializedName
import androidx.room.Entity
import androidx.room.PrimaryKey

// Weather API response
data class WeatherResponse(
    val name: String,
    val coord: Coord,
    val main: MainData,
    val weather: List<WeatherData>,
    val wind: WindData,
    val sys: SysData,
    val clouds: CloudsData? = null,
    val visibility: Int? = null
)

data class Coord(
    val lat: Double,
    val lon: Double
)

data class MainData(
    val temp: Double,
    @SerializedName("feels_like") val feelsLike: Double = 0.0,
    @SerializedName("temp_min") val tempMin: Double = 0.0,
    @SerializedName("temp_max") val tempMax: Double = 0.0,
    val pressure: Int,
    val humidity: Int
)

data class WeatherData(
    val main: String,
    val description: String,
    val icon: String
)

data class WindData(
    val speed: Double,
    val deg: Int? = null,
    val gust: Double? = null
)

data class CloudsData(
    val all: Int
)

data class SysData(
    val sunrise: Long,
    val sunset: Long
)

// Forecast Response (5 days / 3 hours)
data class ForecastResponse(
    val list: List<ForecastItem>
)

data class ForecastItem(
    val dt: Long,
    val main: MainData,
    val weather: List<WeatherData>,
    val wind: WindData,
    @SerializedName("dt_txt") val dtTxt: String
)

// AQI Response
data class AqiResponse(
    val list: List<AqiItem>
)

data class AqiItem(
    val main: AqiMain,
    val components: AqiComponents
)

data class AqiMain(
    val aqi: Int
)

data class AqiComponents(
    val pm2_5: Double,
    val pm10: Double,
    val o3: Double,
    val no2: Double
)

// Geocoding Response
data class GeocodingItem(
    val name: String,
    val lat: Double,
    val lon: Double,
    val country: String,
    val state: String? = null
)

// DB Entity for Search History
@Entity(tableName = "search_history")
data class SearchHistoryEntity(
    @PrimaryKey
    val cityName: String,
    val lat: Double = 0.0,
    val lon: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "cached_weather")
data class CachedWeatherEntity(
    @PrimaryKey
    val query: String,
    val weatherJson: String,
    val forecastJson: String,
    val aqiJson: String,
    val timestamp: Long = System.currentTimeMillis()
)
