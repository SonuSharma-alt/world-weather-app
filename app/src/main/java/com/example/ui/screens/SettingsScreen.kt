package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.data.prefs.PreferencesManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager(context) }
    
    var rainStormEnabled by remember { mutableStateOf(prefs.isAlertEnabled(PreferencesManager.ALERT_RAIN_STORM)) }
    var snowEnabled by remember { mutableStateOf(prefs.isAlertEnabled(PreferencesManager.ALERT_SNOW)) }
    var fogEnabled by remember { mutableStateOf(prefs.isAlertEnabled(PreferencesManager.ALERT_FOG)) }
    var heatEnabled by remember { mutableStateOf(prefs.isAlertEnabled(PreferencesManager.ALERT_EXTREME_HEAT)) }
    var windsEnabled by remember { mutableStateOf(prefs.isAlertEnabled(PreferencesManager.ALERT_STRONG_WINDS)) }
    var isFahrenheit by remember { mutableStateOf(prefs.isFahrenheit()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text("Units", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            AlertToggle("Use Fahrenheit", isFahrenheit) { 
                isFahrenheit = it
                prefs.setFahrenheit(it)
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            Text("Select phenomena for push notifications:", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(16.dp))
            
            AlertToggle("Rain or Storms", rainStormEnabled) { 
                rainStormEnabled = it
                prefs.setAlertEnabled(PreferencesManager.ALERT_RAIN_STORM, it)
            }
            AlertToggle("Snow", snowEnabled) { 
                snowEnabled = it
                prefs.setAlertEnabled(PreferencesManager.ALERT_SNOW, it)
            }
            AlertToggle("Fog or Mist", fogEnabled) { 
                fogEnabled = it
                prefs.setAlertEnabled(PreferencesManager.ALERT_FOG, it)
            }
            AlertToggle("Extreme Heat (>35°C)", heatEnabled) { 
                heatEnabled = it
                prefs.setAlertEnabled(PreferencesManager.ALERT_EXTREME_HEAT, it)
            }
            AlertToggle("Strong Winds (>15m/s)", windsEnabled) { 
                windsEnabled = it
                prefs.setAlertEnabled(PreferencesManager.ALERT_STRONG_WINDS, it)
            }
        }
    }
}

@Composable
fun AlertToggle(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
