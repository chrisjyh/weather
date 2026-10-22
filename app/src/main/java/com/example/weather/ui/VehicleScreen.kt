package com.example.weather.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val VehicleColors: ColorScheme = darkColorScheme(
    primary = Color(0xFF4CC9F0),
    secondary = Color(0xFF80ED99),
    background = Color(0xFF0B1220),
    surface = Color(0xFF131D2E),
    error = Color(0xFFFF6B6B)
)

@Composable
fun VehicleScreen(
    state: VehicleUiState,
    onRefresh: () -> Unit,
    onSetDoorOpen: (Boolean) -> Unit,
    onStartUpdates: () -> Unit,
    onStopUpdates: () -> Unit,
    onRetry: () -> Unit
) {
    MaterialTheme(colorScheme = VehicleColors) {
        Scaffold { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Pleos Door Demo", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                Text("운전석 1열 왼쪽 · ROW_1_LEFT", color = MaterialTheme.colorScheme.onSurfaceVariant)

                StatusCard(state)
                DoorCard(state, onRefresh, onSetDoorOpen)
                SubscriptionCard(state, onStartUpdates, onStopUpdates)

                state.error?.let {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("오류", fontWeight = FontWeight.Bold)
                            Text(it)
                            if (!state.isConnected) Button(onClick = onRetry) { Text("다시 연결") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusCard(state: VehicleUiState) {
    val capability = state.capability
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Vehicle SDK", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(if (state.isConnecting) "연결 중" else if (state.isConnected) "초기화됨" else "연결 안 됨")
            Text("Access: ${capability?.accessType ?: "확인 전"}")
            Text("Range: ${capability?.minimum ?: "?"} .. ${capability?.maximum ?: "?"}")
            Text(state.message, color = MaterialTheme.colorScheme.secondary)
        }
    }
}

@Composable
private fun DoorCard(
    state: VehicleUiState,
    onRefresh: () -> Unit,
    onSetDoorOpen: (Boolean) -> Unit
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("문 위치", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(state.doorLabel, style = MaterialTheme.typography.displaySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onRefresh, enabled = state.isConnected) { Text("새로고침") }
                Button(onClick = { onSetDoorOpen(false) }, enabled = state.capability?.canControl == true) {
                    Text("닫기")
                }
                Button(onClick = { onSetDoorOpen(true) }, enabled = state.capability?.canControl == true) {
                    Text("열기")
                }
            }
            Text(
                "제어 요청 완료는 실제 상태 변경을 뜻하지 않습니다. 새로고침이나 변경 알림으로 확인하세요.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SubscriptionCard(
    state: VehicleUiState,
    onStartUpdates: () -> Unit,
    onStopUpdates: () -> Unit
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp)) {
            Text("실시간 변경 알림", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(if (state.isSubscribed) "구독 중" else "구독하지 않음")
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = if (state.isSubscribed) onStopUpdates else onStartUpdates,
                enabled = state.isConnected
            ) {
                Text(if (state.isSubscribed) "구독 해제" else "구독 시작")
            }
        }
    }
}
