package com.example.data.repository

import com.example.data.api.WeatherApi
import com.example.data.db.WeatherDao
import com.example.data.model.SearchHistoryEntity
import kotlinx.coroutines.flow.Flow
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor

class WeatherRepository(private val weatherDao: WeatherDao) {

    private val api: WeatherApi by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()
            
        Retrofit.Builder()
            .baseUrl("https://api.openweathermap.org/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WeatherApi::class.java)
    }

    fun getSearchHistory(): Flow<List<SearchHistoryEntity>> = weatherDao.getSearchHistory()

    suspend fun insertSearchHistory(cityName: String, lat: Double, lon: Double) {
        weatherDao.insertSearchHistory(SearchHistoryEntity(cityName, lat, lon))
    }

    suspend fun deleteSearchHistory(cityName: String) {
        weatherDao.deleteSearchHistory(cityName)
    }

    suspend fun getCachedWeather(query: String) = weatherDao.getCachedWeather(query)
    
    suspend fun insertCachedWeather(cachedWeather: com.example.data.model.CachedWeatherEntity) {
        weatherDao.insertCachedWeather(cachedWeather)
    }

    suspend fun searchCities(query: String, apiKey: String) = api.searchCities(query, limit = 5, apiKey = apiKey)

    suspend fun getWeatherByCity(city: String, apiKey: String, units: String = "metric") = api.getCurrentWeatherByCity(city, apiKey, units)
    suspend fun getWeatherByLocation(lat: Double, lon: Double, apiKey: String, units: String = "metric") = api.getCurrentWeatherByLocation(lat, lon, apiKey, units)
    suspend fun getForecastByLocation(lat: Double, lon: Double, apiKey: String, units: String = "metric") = api.getForecastByLocation(lat, lon, apiKey, units)
    suspend fun getAqiByLocation(lat: Double, lon: Double, apiKey: String) = api.getAqiByLocation(lat, lon, apiKey)
}
