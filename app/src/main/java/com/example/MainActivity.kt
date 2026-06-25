package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.WeatherAppScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.worker.WeatherUpdateWorker
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    
    // Setup background worker to run every 6 hours
    val workRequest = PeriodicWorkRequestBuilder<WeatherUpdateWorker>(6, TimeUnit.HOURS).build()
    WorkManager.getInstance(this).enqueue(workRequest)
    
    setContent {
      MyApplicationTheme {
        val navController = rememberNavController()
        NavHost(navController = navController, startDestination = "weather") {
            composable("weather") {
                WeatherAppScreen(
                    onNavigateToSettings = { navController.navigate("settings") }
                )
            }
            composable("settings") {
                SettingsScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
      }
    }
  }
}
