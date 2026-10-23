package com.example.weather.ui

import com.example.weather.domain.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class WeatherViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val now = Instant.parse("2026-10-23T03:20:00Z")
    private val locations = Locations()
    private val repository = Repository()
    private val restrictions = MutableStateFlow(DrivingRestrictions(DrivingStatus.AVAILABLE,false))
    private lateinit var model: WeatherViewModel
    @Before fun setup() {
        Dispatchers.setMain(dispatcher)
        model = WeatherViewModel(ResolveLocation(locations),repository,RefreshWeather(repository),object : DrivingRestrictionsRepository { override val restrictions = this@WeatherViewModelTest.restrictions },Clock.fixed(now,ZoneOffset.UTC))
    }
    @After fun teardown() { model.stop(); Dispatchers.resetMain() }
    @Test fun cacheFirstAndFailureKeepsData() = runTest(dispatcher) {
        repository.values(ResolveLocation.DEFAULT.grid()).value = snapshot()
        repository.issues = listOf(WeatherIssue(WeatherFailure.NETWORK))
        model.start(false); runCurrent()
        assertEquals(21.0,model.uiState.value.weather!!.temperature!!,0.0)
        assertEquals(WeatherFailure.NETWORK,model.uiState.value.issues.single().kind)
        assertFalse(model.uiState.value.refreshing)
        model.stop()
    }
    @Test fun duplicateRefreshIsMergedAndForegroundStopsWork() = runTest(dispatcher) {
        repository.wait = CompletableDeferred()
        model.start(false); runCurrent()
        assertTrue(model.uiState.value.refreshing)
        model.refresh(); model.refresh(); runCurrent()
        assertEquals(1,repository.calls)
        model.stop(); runCurrent(); advanceTimeBy(20*60_000); runCurrent()
        assertEquals(1,repository.calls)
        repository.wait = null
        model.start(false); runCurrent()
        assertEquals(2,repository.calls)
        model.stop()
    }
    @Test fun locationChangeClearsDifferentGridAndLateCacheCannotReplaceIt() = runTest(dispatcher) {
        repository.values(ResolveLocation.DEFAULT.grid()).value = snapshot()
        model.start(false); runCurrent()
        locations.current = WeatherLocation(35.1796,129.0756,"부산",LocationSource.CURRENT)
        model.permissionChanged(true); runCurrent()
        assertEquals("부산",model.uiState.value.location.name)
        assertNull(model.uiState.value.weather)
        repository.values(ResolveLocation.DEFAULT.grid()).value = snapshot().copy(temperature=99.0)
        runCurrent()
        assertNull(model.uiState.value.weather)
        model.stop()
    }
    @Test fun restrictionChangesWithoutRefreshAndMinuteTickExpiresData() = runTest(dispatcher) {
        model.start(false); runCurrent()
        assertFalse(model.uiState.value.driving.simplified)
        restrictions.value = DrivingRestrictions(DrivingStatus.DISCONNECTED)
        runCurrent(); assertTrue(model.uiState.value.driving.simplified)
        assertEquals(1,repository.calls)
        model.stop()
    }
    @Test fun permissionChangeCancelsOldRequestAndUsesNewLocation() = runTest(dispatcher) {
        repository.wait = CompletableDeferred()
        model.start(false); runCurrent()
        locations.current = WeatherLocation(35.1796,129.0756,"부산",LocationSource.CURRENT)
        model.permissionChanged(true); runCurrent()
        assertEquals(2,repository.calls)
        repository.wait!!.complete(Unit); runCurrent()
        assertEquals("부산",model.uiState.value.location.name)
        assertFalse(model.uiState.value.refreshing)
        model.stop()
    }
    private fun snapshot() = WeatherSnapshot(ResolveLocation.DEFAULT.grid(),now,now,21.0,Condition.CLEAR,null,null,now.atZone(KOREA).toLocalDate(),emptyList())
    private class Locations : LocationRepository {
        var current: WeatherLocation? = null
        override suspend fun lastLocation() = null
        override suspend fun currentLocation() = current
        override suspend fun save(location: WeatherLocation) = Unit
        override fun problem(): LocationProblem? = null
    }
    private class Repository : WeatherRepository {
        private val cache = mutableMapOf<Grid,MutableStateFlow<WeatherSnapshot?>>()
        var calls = 0
        var wait: CompletableDeferred<Unit>? = null
        var issues = emptyList<WeatherIssue>()
        fun values(grid: Grid) = cache.getOrPut(grid) { MutableStateFlow(null) }
        override fun observe(grid: Grid) = values(grid)
        override suspend fun refresh(grid: Grid,force: Boolean): List<WeatherIssue> { calls++; wait?.await(); return issues }
    }
}
