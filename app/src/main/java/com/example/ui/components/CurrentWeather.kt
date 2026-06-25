package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.WeatherResponse
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CurrentWeather(weather: WeatherResponse, isFahrenheit: Boolean) {
    val unitSymbol = if (isFahrenheit) "°F" else "°C"
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(weather.name, style = MaterialTheme.typography.headlineMedium)
            val iconCode = weather.weather.firstOrNull()?.icon
            if (iconCode != null) {
                AsyncImage(
                    model = "https://openweathermap.org/img/wn/$iconCode@4x.png",
                    contentDescription = weather.weather.firstOrNull()?.description,
                    modifier = Modifier.size(120.dp)
                )
            }
            Text("${weather.main.temp}$unitSymbol", style = MaterialTheme.typography.displayLarge)
            Text(
                weather.weather.firstOrNull()?.description?.replaceFirstChar { 
                    if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() 
                } ?: "", 
                style = MaterialTheme.typography.titleMedium
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                WeatherDetailItem("Feels Like", "${weather.main.feelsLike}$unitSymbol")
                WeatherDetailItem("Min Temp", "${weather.main.tempMin}$unitSymbol")
                WeatherDetailItem("Max Temp", "${weather.main.tempMax}$unitSymbol")
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                WeatherDetailItem("Humidity", "${weather.main.humidity}%")
                WeatherDetailItem("Pressure", "${weather.main.pressure} hPa")
                if (weather.clouds != null) {
                    WeatherDetailItem("Clouds", "${weather.clouds.all}%")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                WeatherDetailItem("Wind", "${weather.wind.speed} m/s")
                if (weather.wind.gust != null) {
                    WeatherDetailItem("Gust", "${weather.wind.gust} m/s")
                }
                if (weather.wind.deg != null) {
                    WeatherDetailItem("Dir", "${weather.wind.deg}°")
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                val sunrise = sdf.format(Date(weather.sys.sunrise * 1000))
                val sunset = sdf.format(Date(weather.sys.sunset * 1000))
                
                WeatherDetailItem("Sunrise", sunrise)
                WeatherDetailItem("Sunset", sunset)
                if (weather.visibility != null) {
                    WeatherDetailItem("Visibility", "${weather.visibility / 1000} km")
                }
            }
        }
    }
}

@Composable
fun WeatherDetailItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}
