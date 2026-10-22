package com.example.weather.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.weather.data.PleosVehicleRepository
import com.example.weather.data.VehicleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class VehicleViewModel(
    private val repository: VehicleRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(VehicleUiState())
    val uiState = _uiState.asStateFlow()

    init {
        connect()
    }

    fun connect() {
        _uiState.update { it.copy(isConnecting = true, error = null, message = "Vehicle Service에 연결 중입니다.") }
        repository.initialize { result ->
            result.onSuccess {
                _uiState.update { it.copy(isConnecting = false, isConnected = true, message = "SDK가 초기화되었습니다.") }
                loadCapability()
            }.onFailure(::showError)
        }
    }

    private fun loadCapability() {
        repository.checkDoorCapability { result ->
            result.onSuccess { capability ->
                _uiState.update {
                    it.copy(
                        capability = capability,
                        message = "문 위치 지원 범위를 확인했습니다.",
                        error = null
                    )
                }
                refresh()
            }.onFailure(::showError)
        }
    }

    fun refresh() {
        repository.readDoorPosition { result ->
            result.onSuccess { position ->
                _uiState.update {
                    it.copy(doorPosition = position, message = "현재 문 상태를 조회했습니다.", error = null)
                }
            }.onFailure(::showError)
        }
    }

    fun setDoorOpen(open: Boolean) {
        val capability = _uiState.value.capability
        if (capability?.canControl != true) {
            showError(IllegalStateException("이 영역에서 문 위치 제어를 지원하지 않습니다."))
            return
        }
        val requested = if (open) 1 else 0
        repository.setDoorPosition(requested) { result ->
            result.onSuccess {
                _uiState.update {
                    it.copy(
                        message = "${if (open) "열기" else "닫기"} 요청이 접수되었습니다. 실제 상태는 새로고침 또는 알림으로 확인하세요.",
                        error = null
                    )
                }
            }.onFailure(::showError)
        }
    }

    fun startUpdates() {
        repository.startDoorUpdates(
            onUpdate = { position ->
                _uiState.update {
                    it.copy(doorPosition = position, message = "차량 상태 변경 알림을 받았습니다.", error = null)
                }
            },
            callback = { result ->
                result.onSuccess {
                    _uiState.update { it.copy(isSubscribed = true, message = "실시간 알림을 구독했습니다.", error = null) }
                }.onFailure(::showError)
            }
        )
    }

    fun stopUpdates() {
        repository.stopDoorUpdates { result ->
            result.onSuccess {
                _uiState.update { it.copy(isSubscribed = false, message = "실시간 알림 구독을 해제했습니다.", error = null) }
            }.onFailure(::showError)
        }
    }

    private fun showError(error: Throwable) {
        _uiState.update {
            it.copy(
                isConnecting = false,
                error = error.message ?: error.javaClass.simpleName,
                message = "요청을 완료하지 못했습니다."
            )
        }
    }

    override fun onCleared() {
        repository.release()
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    VehicleViewModel(PleosVehicleRepository(context)) as T
            }
    }
}
