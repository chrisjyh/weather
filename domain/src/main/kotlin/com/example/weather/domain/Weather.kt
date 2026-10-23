package com.example.weather.domain

import kotlinx.coroutines.flow.Flow
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

val KOREA: ZoneId = ZoneId.of("Asia/Seoul")
data class Grid(val x: Int, val y: Int)
enum class LocationSource { CURRENT, LAST, DEFAULT }
enum class LocationProblem { PERMISSION, DISABLED, TIMEOUT, UNAVAILABLE }
data class WeatherLocation(val latitude: Double, val longitude: Double, val name: String?, val source: LocationSource, val problem: LocationProblem? = null)
enum class Condition { CLEAR, CLOUDY, OVERCAST, RAIN, RAIN_SNOW, SNOW, SHOWER, DRIZZLE, FLURRIES, UNKNOWN }
data class HourlyForecast(val time: Instant, val temperature: Double?, val condition: Condition, val precipitationProbability: Int?, val precipitationAmount: String?)
data class WeatherSnapshot(
    val grid: Grid,
    val observedAt: Instant?,
    val fetchedAt: Instant,
    val temperature: Double?,
    val condition: Condition,
    val high: Double?,
    val low: Double?,
    val day: LocalDate,
    val hourly: List<HourlyForecast>,
    val conditionAt: Instant? = observedAt,
) {
    fun isStale(now: Instant) = observedAt == null || Duration.between(observedAt, now) >= Duration.ofHours(2)
    fun validAt(now: Instant): WeatherSnapshot {
        val expired = observedAt == null || Duration.between(observedAt, now) > Duration.ofHours(24)
        val conditionExpired = conditionAt == null || Duration.between(conditionAt, now) > Duration.ofHours(2)
        return copy(
            temperature = temperature.takeUnless { expired },
            condition = if (expired || conditionExpired) Condition.UNKNOWN else condition,
            high = high.takeIf { day == now.atZone(KOREA).toLocalDate() },
            low = low.takeIf { day == now.atZone(KOREA).toLocalDate() },
            hourly = hourly.filter { it.time > now && it.time <= now.plusSeconds(6 * 3600) },
        )
    }
    val hasData get() = temperature != null || condition != Condition.UNKNOWN || high != null || low != null || hourly.isNotEmpty()
}
enum class WeatherFailure { CONFIGURATION, NETWORK, TIMEOUT, HTTP, AUTHENTICATION, QUOTA, NO_DATA, MALFORMED, STORAGE }
data class WeatherIssue(val kind: WeatherFailure, val source: String? = null)
enum class DrivingStatus { UNKNOWN, AVAILABLE, UNSUPPORTED, DISCONNECTED }
data class DrivingRestrictions(val status: DrivingStatus = DrivingStatus.UNKNOWN, val requiresOptimization: Boolean = true, val activeRestrictions: Int = 0) {
    val simplified get() = status != DrivingStatus.UNSUPPORTED && (status != DrivingStatus.AVAILABLE || requiresOptimization || activeRestrictions != 0)
}
interface WeatherRepository {
    fun observe(grid: Grid): Flow<WeatherSnapshot?>
    suspend fun refresh(grid: Grid, force: Boolean): List<WeatherIssue>
}
interface LocationRepository {
    suspend fun lastLocation(): WeatherLocation?
    suspend fun currentLocation(): WeatherLocation?
    fun problem(): LocationProblem?
    suspend fun save(location: WeatherLocation)
}
interface DrivingRestrictionsRepository { val restrictions: Flow<DrivingRestrictions> }
class ResolveLocation(private val repository: LocationRepository) {
    suspend fun cached(): WeatherLocation = repository.lastLocation()?.copy(source = LocationSource.LAST) ?: DEFAULT
    suspend operator fun invoke(permission: Boolean): WeatherLocation {
        val current = if (permission) repository.currentLocation() else null
        if (current != null) {
            repository.save(current)
            return current.copy(source = LocationSource.CURRENT)
        }
        return cached().copy(problem = if (permission) repository.problem() ?: LocationProblem.UNAVAILABLE else LocationProblem.PERMISSION)
    }
    companion object {
        val DEFAULT = WeatherLocation(37.5172, 127.0473, "서울 강남구", LocationSource.DEFAULT)
    }
}
class RefreshWeather(private val repository: WeatherRepository) {
    suspend operator fun invoke(grid: Grid, force: Boolean) = repository.refresh(grid, force)
}
