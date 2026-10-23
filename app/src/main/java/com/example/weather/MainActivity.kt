package com.example.weather

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.weather.ui.*

class MainActivity : ComponentActivity() {
    private val viewModel by viewModels<WeatherViewModel> { (application as WeatherApplication).container.factory }
    private fun permission() = listOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION).any {
        ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { viewModel.permissionChanged(permission()) }
            DisposableEffect(lifecycle) {
                val observer = LifecycleEventObserver { _, event ->
                    when (event) {
                        Lifecycle.Event.ON_START -> viewModel.start(permission())
                        Lifecycle.Event.ON_RESUME -> viewModel.permissionChanged(permission())
                        Lifecycle.Event.ON_STOP -> viewModel.stop()
                        else -> Unit
                    }
                }
                lifecycle.addObserver(observer)
                if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) viewModel.start(permission())
                onDispose { lifecycle.removeObserver(observer); viewModel.stop() }
            }
            WeatherTheme {
                WeatherScreen(viewModel.uiState.collectAsStateWithLifecycle().value, viewModel::refresh, onPermission = {
                    val prefs = getPreferences(MODE_PRIVATE)
                    if (prefs.getBoolean("requested", false) && !shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION)) {
                        startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
                    } else {
                        prefs.edit().putBoolean("requested", true).apply()
                        launcher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                    }
                })
            }
        }
    }
}
