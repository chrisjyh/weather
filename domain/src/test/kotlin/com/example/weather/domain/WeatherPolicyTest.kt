package com.example.weather.domain

import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

class WeatherPolicyTest {
    @Test fun seoulAndGangnamGrid() {
        assertEquals(Grid(60, 127), toKmaGrid(37.5665, 126.9780))
        assertEquals(Grid(61, 126), ResolveLocation.DEFAULT.grid())
        assertEquals(Grid(98, 76), toKmaGrid(35.1796, 129.0756))
    }
    @Test fun permissionFallbackNeverRequestsLocation() = runTest {
        val repo = Locations()
        val resolve = ResolveLocation(repo)
        assertEquals(LocationSource.DEFAULT, resolve(false).source)
        assertEquals(0, repo.calls)
        repo.saved = ResolveLocation.DEFAULT.copy(name = "저장된 지역")
        assertEquals(LocationSource.LAST, resolve(false).source)
        assertEquals(LocationProblem.PERMISSION, resolve(false).problem)
        repo.current = ResolveLocation.DEFAULT.copy(name = "새 위치")
        assertEquals(LocationSource.CURRENT, resolve(true).source)
        assertEquals("새 위치", repo.saved?.name)
    }
    @Test fun disabledAndTimeoutFallBack() = runTest {
        val repo = Locations()
        for (problem in listOf(LocationProblem.DISABLED, LocationProblem.TIMEOUT)) {
            repo.issue = problem
            assertEquals(problem, ResolveLocation(repo)(true).problem)
        }
    }
    @Test fun expiryUsesObservationTimeAndDoesNotInventExtrema() {
        val now = Instant.parse("2026-10-23T03:00:00Z")
        val weather = WeatherSnapshot(Grid(60,127), now.minusSeconds(7200), now, 20.0, Condition.CLEAR, null, null,
            now.atZone(KOREA).toLocalDate(), listOf(HourlyForecast(now, 20.0, Condition.CLEAR, null, null), HourlyForecast(now.plusSeconds(3600), 21.0, Condition.CLEAR, null, null)))
        assertTrue(weather.isStale(now))
        assertEquals(1, weather.validAt(now).hourly.size)
        assertNull(weather.validAt(now).high)
        assertNull(weather.validAt(now.plusSeconds(24 * 3600)).temperature)
        assertEquals(Condition.UNKNOWN, weather.validAt(now.plusSeconds(24 * 3600)).condition)
    }
    @Test fun uncertainCarStateIsConservative() {
        assertTrue(DrivingRestrictions().simplified)
        assertTrue(DrivingRestrictions(DrivingStatus.DISCONNECTED).simplified)
        assertFalse(DrivingRestrictions(DrivingStatus.UNSUPPORTED, false).simplified)
        assertTrue(DrivingRestrictions(DrivingStatus.AVAILABLE, false, 1).simplified)
        assertFalse(DrivingRestrictions(DrivingStatus.AVAILABLE, false).simplified)
    }
    private class Locations : LocationRepository {
        var saved: WeatherLocation? = null
        var current: WeatherLocation? = null
        var calls = 0
        var issue: LocationProblem? = null
        override suspend fun lastLocation() = saved
        override suspend fun currentLocation(): WeatherLocation? { calls++; return current }
        override fun problem() = issue
        override suspend fun save(location: WeatherLocation) { saved = location }
    }
}
