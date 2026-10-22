package com.example.weather

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.weather.ui.VehicleScreen
import com.example.weather.ui.VehicleViewModel

class MainActivity : ComponentActivity() {
    private val viewModel by viewModels<VehicleViewModel> {
        VehicleViewModel.factory(applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VehicleScreen(
                state = viewModel.uiState.collectAsStateWithLifecycle().value,
                onRefresh = viewModel::refresh,
                onSetDoorOpen = viewModel::setDoorOpen,
                onStartUpdates = viewModel::startUpdates,
                onStopUpdates = viewModel::stopUpdates,
                onRetry = viewModel::connect
            )
        }
    }
}
