package com.example.ui.viewmodel

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.model.AqiResponse
import com.example.data.model.ForecastResponse
import com.example.data.model.GeocodingItem
import com.example.data.model.WeatherResponse
import com.example.data.prefs.PreferencesManager
import com.example.data.repository.WeatherRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

import com.google.gson.Gson
import com.example.data.model.CachedWeatherEntity

sealed class WeatherUiState {
    object Initial : WeatherUiState()
    object Loading : WeatherUiState()
    data class Success(
        val weather: WeatherResponse,
        val forecast: ForecastResponse,
        val aqi: AqiResponse,
        val isFahrenheit: Boolean = false,
        val isOffline: Boolean = false,
        val isRefreshing: Boolean = false
    ) : WeatherUiState()
    data class Error(val message: String) : WeatherUiState()
}

class WeatherViewModel(
    private val repository: WeatherRepository,
    private val prefs: PreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<WeatherUiState>(WeatherUiState.Initial)
    val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()
    
    private val _suggestions = MutableStateFlow<List<GeocodingItem>>(emptyList())
    val suggestions: StateFlow<List<GeocodingItem>> = _suggestions.asStateFlow()

    private var searchJob: Job? = null

    val searchHistory = repository.getSearchHistory()

    private fun getUnits(): String = if (prefs.isFahrenheit()) "imperial" else "metric"

    fun refresh() {
        val currentState = _uiState.value
        if (currentState is WeatherUiState.Success) {
            _uiState.value = currentState.copy(isRefreshing = true)
            viewModelScope.launch {
                val gson = Gson()
                try {
                    val apiKey = BuildConfig.OPENWEATHER_API_KEY
                    val units = getUnits()
                    val lat = currentState.weather.coord.lat
                    val lon = currentState.weather.coord.lon
                    val name = currentState.weather.name
                    
                    val weather = repository.getWeatherByLocation(lat, lon, apiKey, units)
                    val forecast = repository.getForecastByLocation(lat, lon, apiKey, units)
                    val aqi = repository.getAqiByLocation(lat, lon, apiKey)
                    
                    val cached = CachedWeatherEntity(
                        query = name,
                        weatherJson = gson.toJson(weather),
                        forecastJson = gson.toJson(forecast),
                        aqiJson = gson.toJson(aqi)
                    )
                    repository.insertCachedWeather(cached)
                    _uiState.value = WeatherUiState.Success(weather, forecast, aqi, prefs.isFahrenheit(), false, false)
                } catch (e: Exception) {
                    // Keep old data, but reset isRefreshing flag
                    _uiState.value = currentState.copy(isRefreshing = false, isOffline = true)
                }
            }
        }
    }
    
    fun searchCitySuggestions(query: String) {
        searchJob?.cancel()
        if (query.isBlank()) {
            _suggestions.value = emptyList()
            return
        }
        searchJob = viewModelScope.launch {
            delay(300) // debounce
            try {
                val apiKey = BuildConfig.OPENWEATHER_API_KEY
                val result = repository.searchCities(query, apiKey)
                _suggestions.value = result
            } catch (e: Exception) {
                // ignore errors for autocomplete
            }
        }
    }

    fun getWeatherByLatLon(lat: Double, lon: Double, displayName: String) {
        viewModelScope.launch {
            _uiState.value = WeatherUiState.Loading
            _suggestions.value = emptyList()
            val gson = Gson()
            try {
                val apiKey = BuildConfig.OPENWEATHER_API_KEY
                val units = getUnits()
                
                val weather = repository.getWeatherByLocation(lat, lon, apiKey, units)
                val forecast = repository.getForecastByLocation(lat, lon, apiKey, units)
                val aqi = repository.getAqiByLocation(lat, lon, apiKey)
                
                repository.insertSearchHistory(displayName, lat, lon)
                val cached = CachedWeatherEntity(
                    query = displayName,
                    weatherJson = gson.toJson(weather),
                    forecastJson = gson.toJson(forecast),
                    aqiJson = gson.toJson(aqi)
                )
                repository.insertCachedWeather(cached)
                _uiState.value = WeatherUiState.Success(weather, forecast, aqi, prefs.isFahrenheit(), false)
            } catch (e: Exception) {
                try {
                    val cached = repository.getCachedWeather(displayName)
                    if (cached != null) {
                        val weather = gson.fromJson(cached.weatherJson, WeatherResponse::class.java)
                        val forecast = gson.fromJson(cached.forecastJson, ForecastResponse::class.java)
                        val aqi = gson.fromJson(cached.aqiJson, AqiResponse::class.java)
                        _uiState.value = WeatherUiState.Success(weather, forecast, aqi, prefs.isFahrenheit(), true)
                    } else {
                        _uiState.value = WeatherUiState.Error("No internet connection and no cached data.")
                    }
                } catch (e2: Exception) {
                    _uiState.value = WeatherUiState.Error(e.message ?: "Unknown Error")
                }
            }
        }
    }

    fun getWeatherByCity(city: String) {
        viewModelScope.launch {
            _uiState.value = WeatherUiState.Loading
            val gson = Gson()
            try {
                val apiKey = BuildConfig.OPENWEATHER_API_KEY
                val units = getUnits()
                val weather = repository.getWeatherByCity(city, apiKey, units)
                val lat = weather.coord.lat
                val lon = weather.coord.lon
                
                val forecast = repository.getForecastByLocation(lat, lon, apiKey, units)
                val aqi = repository.getAqiByLocation(lat, lon, apiKey)
                
                repository.insertSearchHistory(city, lat, lon)
                val cached = CachedWeatherEntity(
                    query = city,
                    weatherJson = gson.toJson(weather),
                    forecastJson = gson.toJson(forecast),
                    aqiJson = gson.toJson(aqi)
                )
                repository.insertCachedWeather(cached)
                _uiState.value = WeatherUiState.Success(weather, forecast, aqi, prefs.isFahrenheit(), false)
            } catch (e: Exception) {
                try {
                    val cached = repository.getCachedWeather(city)
                    if (cached != null) {
                        val weather = gson.fromJson(cached.weatherJson, WeatherResponse::class.java)
                        val forecast = gson.fromJson(cached.forecastJson, ForecastResponse::class.java)
                        val aqi = gson.fromJson(cached.aqiJson, AqiResponse::class.java)
                        _uiState.value = WeatherUiState.Success(weather, forecast, aqi, prefs.isFahrenheit(), true)
                    } else {
                        _uiState.value = WeatherUiState.Error("No internet connection and no cached data.")
                    }
                } catch (e2: Exception) {
                    _uiState.value = WeatherUiState.Error(e.message ?: "Unknown Error")
                }
            }
        }
    }

    fun getWeatherByLocation(location: Location) {
        viewModelScope.launch {
            _uiState.value = WeatherUiState.Loading
            val gson = Gson()
            try {
                val apiKey = BuildConfig.OPENWEATHER_API_KEY
                val units = getUnits()
                val lat = location.latitude
                val lon = location.longitude
                
                val weather = repository.getWeatherByLocation(lat, lon, apiKey, units)
                val forecast = repository.getForecastByLocation(lat, lon, apiKey, units)
                val aqi = repository.getAqiByLocation(lat, lon, apiKey)
                
                val cached = CachedWeatherEntity(
                    query = "current_location",
                    weatherJson = gson.toJson(weather),
                    forecastJson = gson.toJson(forecast),
                    aqiJson = gson.toJson(aqi)
                )
                repository.insertCachedWeather(cached)
                _uiState.value = WeatherUiState.Success(weather, forecast, aqi, prefs.isFahrenheit(), false)
            } catch (e: Exception) {
                try {
                    val cached = repository.getCachedWeather("current_location")
                    if (cached != null) {
                        val weather = gson.fromJson(cached.weatherJson, WeatherResponse::class.java)
                        val forecast = gson.fromJson(cached.forecastJson, ForecastResponse::class.java)
                        val aqi = gson.fromJson(cached.aqiJson, AqiResponse::class.java)
                        _uiState.value = WeatherUiState.Success(weather, forecast, aqi, prefs.isFahrenheit(), true)
                    } else {
                        _uiState.value = WeatherUiState.Error("No internet connection and no cached data.")
                    }
                } catch (e2: Exception) {
                    _uiState.value = WeatherUiState.Error(e.message ?: "Unknown Error")
                }
            }
        }
    }
    
    fun deleteSearchHistory(city: String) {
        viewModelScope.launch {
            repository.deleteSearchHistory(city)
        }
    }
}

class WeatherViewModelFactory(
    private val repository: WeatherRepository,
    private val prefs: PreferencesManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WeatherViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WeatherViewModel(repository, prefs) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
