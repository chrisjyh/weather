package com.example.weather.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.weather.domain.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.time.Clock

data class WeatherUiState(
    val location: WeatherLocation = ResolveLocation.DEFAULT,
    val weather: WeatherSnapshot? = null,
    val initialLoading: Boolean = true,
    val refreshing: Boolean = false,
    val permissionGranted: Boolean = false,
    val issues: List<WeatherIssue> = emptyList(),
    val driving: DrivingRestrictions = DrivingRestrictions(),
    val stale: Boolean = false,
)
class WeatherViewModel(
    private val resolveLocation: ResolveLocation,
    private val repository: WeatherRepository,
    private val refreshWeather: RefreshWeather,
    private val driving: DrivingRestrictionsRepository,
    private val clock: Clock = Clock.systemUTC(),
) : ViewModel() {
    private val mutable = MutableStateFlow(WeatherUiState())
    val uiState = mutable.asStateFlow()
    private var foreground: Job? = null
    private var refreshJob: Job? = null
    private var cacheJob: Job? = null
    private var scope: CoroutineScope? = null
    private var generation = 0
    private var selectedGrid: Grid? = null
    private var initializing = false
    fun start(permission: Boolean) {
        if (foreground?.isActive == true) { permissionChanged(permission); return }
        initializing = true
        mutable.update { it.copy(permissionGranted = permission, refreshing = true) }
        foreground = viewModelScope.launch {
            supervisorScope {
                scope = this
                launch { driving.restrictions.catch { emit(DrivingRestrictions(DrivingStatus.DISCONNECTED)) }.collect { restriction -> mutable.update { it.copy(driving = restriction) } } }
                try { select(resolveLocation.cached()) } catch (e: CancellationException) { throw e } catch (_: Exception) { mutable.update { it.copy(issues = listOf(WeatherIssue(WeatherFailure.STORAGE))) } }
                initializing = false
                requestRefresh(false)
                launch {
                    while (isActive) {
                        delay(60_000)
                        mutable.update { it.copy(weather = it.weather?.validAt(clock.instant()), stale = it.weather?.isStale(clock.instant()) ?: false) }
                    }
                }
                while (isActive) { delay(10 * 60_000); requestRefresh(false) }
            }
        }
    }
    fun stop() {
        generation++
        foreground?.cancel(); foreground = null
        refreshJob = null; cacheJob = null; scope = null; selectedGrid = null; initializing = false
        mutable.update { it.copy(refreshing = false) }
    }
    fun permissionChanged(granted: Boolean) {
        if (uiState.value.permissionGranted == granted) return
        mutable.update { it.copy(permissionGranted = granted) }
        generation++
        refreshJob?.cancel(); refreshJob = null
        requestRefresh(false)
    }
    fun refresh() = requestRefresh(true)
    private fun requestRefresh(force: Boolean) {
        val activeScope = scope ?: return
        if (initializing || refreshJob?.isActive == true) return
        val token = generation
        mutable.update { it.copy(refreshing = true, initialLoading = it.weather?.hasData != true) }
        refreshJob = activeScope.launch {
            try {
                val location = resolveLocation(uiState.value.permissionGranted)
                ensureActive()
                if (token != generation) return@launch
                select(location)
                val issues = refreshWeather(location.grid(), force)
                ensureActive()
                if (token == generation) mutable.update { it.copy(issues = issues) }
            } catch (e: CancellationException) { throw e
            } catch (_: Exception) {
                if (token == generation) mutable.update { it.copy(issues = listOf(WeatherIssue(WeatherFailure.STORAGE))) }
            } finally {
                if (token == generation) mutable.update { it.copy(initialLoading = false, refreshing = false) }
            }
        }
    }
    private suspend fun select(location: WeatherLocation) {
        val grid = location.grid()
        mutable.update { it.copy(location = location) }
        if (selectedGrid == grid) return
        cacheJob?.cancel()
        selectedGrid = grid
        mutable.update { it.copy(weather = null, issues = emptyList(), initialLoading = true) }
        fun accept(weather: WeatherSnapshot?) {
            if (selectedGrid == grid) mutable.update { it.copy(weather = weather?.validAt(clock.instant()), initialLoading = weather == null && it.refreshing,
                stale = weather?.isStale(clock.instant()) ?: false) }
        }
        accept(repository.observe(grid).first())
        cacheJob = scope?.launch {
            repository.observe(grid).catch { mutable.update { it.copy(issues = listOf(WeatherIssue(WeatherFailure.STORAGE))) } }.collect(::accept)
        }
    }
    companion object {
        fun factory(resolve: ResolveLocation, repository: WeatherRepository, refresh: RefreshWeather, driving: DrivingRestrictionsRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = WeatherViewModel(resolve, repository, refresh, driving) as T
        }
    }
}
