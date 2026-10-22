package com.example.weather.ui

import com.example.weather.data.DoorCapability

data class VehicleUiState(
    val isConnecting: Boolean = true,
    val isConnected: Boolean = false,
    val capability: DoorCapability? = null,
    val doorPosition: Int? = null,
    val isSubscribed: Boolean = false,
    val message: String = "Vehicle Service에 연결 중입니다.",
    val error: String? = null
) {
    val doorLabel: String = when (doorPosition) {
        0 -> "닫힘"
        1 -> "열림"
        else -> "알 수 없음"
    }
}
