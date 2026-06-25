package com.example.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.graphics.Color as AndroidColor
import android.location.Location
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.data.db.WeatherDatabase
import com.example.data.model.AqiItem
import com.example.data.model.ForecastItem
import com.example.data.model.WeatherResponse
import com.example.data.repository.WeatherRepository
import com.example.ui.components.CurrentWeather
import com.example.ui.viewmodel.WeatherUiState
import com.example.ui.viewmodel.WeatherViewModel
import com.example.ui.viewmodel.WeatherViewModelFactory
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.location.LocationServices
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun WeatherAppScreen(onNavigateToSettings: () -> Unit = {}) {
    val context = LocalContext.current
    val db = remember { WeatherDatabase.getDatabase(context) }
    val repository = remember { WeatherRepository(db.weatherDao()) }
    val prefs = remember { com.example.data.prefs.PreferencesManager(context) }
    val factory = remember { WeatherViewModelFactory(repository, prefs) }
    val viewModel: WeatherViewModel = viewModel(factory = factory)

    val uiState by viewModel.uiState.collectAsState()
    val searchHistory by viewModel.searchHistory.collectAsState(initial = emptyList())

    val locationPermissionState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    var searchQuery by remember { mutableStateOf("") }
    
    val suggestions by viewModel.suggestions.collectAsState()
    var expanded by remember { mutableStateOf(false) }
    
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        if (!locationPermissionState.status.isGranted) {
            locationPermissionState.launchPermissionRequest()
        }
    }

    LaunchedEffect(uiState) {
        if (uiState is WeatherUiState.Success && (uiState as WeatherUiState.Success).isOffline) {
            snackbarHostState.showSnackbar("No internet. Showing cached data.", duration = SnackbarDuration.Long)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Weather App") },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                    IconButton(onClick = {
                        if (locationPermissionState.status.isGranted) {
                            @SuppressLint("MissingPermission")
                            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                                location?.let { viewModel.getWeatherByLocation(it) }
                            }
                        } else {
                            locationPermissionState.launchPermissionRequest()
                        }
                    }) {
                        Icon(Icons.Default.LocationOn, contentDescription = "Current Location")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { 
                        searchQuery = it 
                        viewModel.searchCitySuggestions(it)
                        expanded = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    placeholder = { Text("Search city...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            if (searchQuery.isNotBlank()) {
                                viewModel.getWeatherByCity(searchQuery)
                                expanded = false
                            }
                        }
                    )
                )
                if (suggestions.isNotEmpty()) {
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        suggestions.forEach { suggestion ->
                            val displayName = listOfNotNull(suggestion.name, suggestion.state, suggestion.country).joinToString(", ")
                            DropdownMenuItem(
                                text = { Text(displayName) },
                                onClick = {
                                    searchQuery = displayName
                                    expanded = false
                                    viewModel.getWeatherByLatLon(suggestion.lat, suggestion.lon, displayName)
                                }
                            )
                        }
                    }
                }
            }
            
            when (val state = uiState) {
                is WeatherUiState.Initial -> {
                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        items(searchHistory) { history ->
                            ListItem(
                                headlineContent = { Text(history.cityName) },
                                modifier = Modifier.clickable {
                                    searchQuery = history.cityName
                                    if (history.lat != 0.0 || history.lon != 0.0) {
                                        viewModel.getWeatherByLatLon(history.lat, history.lon, history.cityName)
                                    } else {
                                        viewModel.getWeatherByCity(history.cityName)
                                    }
                                },
                                trailingContent = {
                                    IconButton(onClick = { viewModel.deleteSearchHistory(history.cityName) }) {
                                        Icon(Icons.Default.Close, contentDescription = "Delete")
                                    }
                                }
                            )
                        }
                    }
                }
                is WeatherUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is WeatherUiState.Success -> {
                    androidx.compose.material3.pulltorefresh.PullToRefreshBox(
                        isRefreshing = state.isRefreshing,
                        onRefresh = { viewModel.refresh() }
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            item {
                                CurrentWeather(weather = state.weather, isFahrenheit = state.isFahrenheit)
                            }
                            item {
                                HourlyForecastCard(forecast = state.forecast.list, isFahrenheit = state.isFahrenheit)
                            }
                            item {
                                Card(modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text("7-Day Forecast", style = MaterialTheme.typography.titleMedium)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        ForecastChart(forecast = state.forecast.list)
                                    }
                                }
                            }
                            item {
                                AqiCard(aqi = state.aqi.list.firstOrNull())
                            }
                        }
                    }
                }
                is WeatherUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = "Error: ${state.message}", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}


@Composable
fun HourlyForecastCard(forecast: List<ForecastItem>, isFahrenheit: Boolean) {
    val unitSymbol = if (isFahrenheit) "°F" else "°C"
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Hourly Forecast", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            androidx.compose.foundation.lazy.LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(forecast.take(8)) { item -> // Next 24 hours (8 * 3 hours)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                        val time = sdf.format(Date(item.dt * 1000))
                        Text(time, style = MaterialTheme.typography.bodyMedium)
                        val iconCode = item.weather.firstOrNull()?.icon
                        if (iconCode != null) {
                            AsyncImage(
                                model = "https://openweathermap.org/img/wn/$iconCode@2x.png",
                                contentDescription = null,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                        Text("${item.main.temp}$unitSymbol", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}


@Composable
fun ForecastChart(forecast: List<ForecastItem>) {
    val primaryColor = MaterialTheme.colorScheme.primary.toArgb()
    val textColor = MaterialTheme.colorScheme.onSurface.toArgb()

    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .padding(16.dp),
        factory = { context ->
            LineChart(context).apply {
                description.isEnabled = false
                legend.isEnabled = false
                axisRight.isEnabled = false
                
                xAxis.position = XAxis.XAxisPosition.BOTTOM
                xAxis.setDrawGridLines(false)
                xAxis.textColor = textColor
                
                axisLeft.textColor = textColor
                axisLeft.setDrawGridLines(true)
            }
        },
        update = { chart ->
            val dataPoints = forecast.filterIndexed { index, _ -> index % 8 == 0 }.take(7)
            val entries = dataPoints.mapIndexed { index, item ->
                Entry(index.toFloat(), item.main.temp.toFloat())
            }
            val labels = dataPoints.map { item ->
                val sdf = SimpleDateFormat("EEE", Locale.getDefault())
                sdf.format(Date(item.dt * 1000))
            }
            chart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)

            val dataSet = LineDataSet(entries, "Temperature").apply {
                color = primaryColor
                setCircleColor(primaryColor)
                lineWidth = 2f
                circleRadius = 4f
                setDrawValues(true)
                valueTextColor = textColor
                valueTextSize = 10f
                mode = LineDataSet.Mode.CUBIC_BEZIER
            }
            chart.data = LineData(dataSet)
            chart.invalidate()
        }
    )
}

@Composable
fun AqiCard(aqi: AqiItem?) {
    if (aqi == null) return
    
    val (status, color, rec) = when (aqi.main.aqi) {
        1 -> Triple("Good", AndroidColor.GREEN, "Air quality is considered satisfactory, and air pollution poses little or no risk.")
        2 -> Triple("Fair", AndroidColor.YELLOW, "Air quality is acceptable; however, for some pollutants there may be a moderate health concern.")
        3 -> Triple("Moderate", AndroidColor.parseColor("#FFA500"), "Members of sensitive groups may experience health effects.")
        4 -> Triple("Poor", AndroidColor.RED, "Everyone may begin to experience health effects.")
        else -> Triple("Very Poor", AndroidColor.parseColor("#800080"), "Health warnings of emergency conditions.")
    }
    
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Air Quality Index (AQI)", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(androidx.compose.ui.graphics.Color(color), shape = androidx.compose.foundation.shape.CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(aqi.main.aqi.toString(), color = androidx.compose.ui.graphics.Color.White, style = MaterialTheme.typography.titleLarge)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(status, style = MaterialTheme.typography.titleMedium)
                    Text("PM2.5: ${aqi.components.pm2_5} | PM10: ${aqi.components.pm10}", style = MaterialTheme.typography.bodySmall)
                    Text("O3: ${aqi.components.o3} | NO2: ${aqi.components.no2}", style = MaterialTheme.typography.bodySmall)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(rec, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
