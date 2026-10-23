package com.example.weather.data

import android.annotation.SuppressLint
import android.content.Context
import android.location.*
import android.os.Bundle
import android.os.Looper
import com.example.weather.domain.*
import kotlinx.coroutines.*
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class AndroidLocationRepository(context: Context, private val dao: WeatherDao) : LocationRepository {
    private val manager = context.getSystemService(LocationManager::class.java)
    private val geocoder = Geocoder(context, Locale.KOREA)
    private var lastProblem: LocationProblem? = null
    override fun problem() = lastProblem
    override suspend fun lastLocation() = dao.location()?.let { WeatherLocation(it.latitude, it.longitude, it.name, LocationSource.LAST) }
    override suspend fun save(location: WeatherLocation) = dao.putLocation(LocationRecord(latitude = location.latitude, longitude = location.longitude, name = location.name))
    @SuppressLint("MissingPermission")
    override suspend fun currentLocation(): WeatherLocation? {
        lastProblem = null
        try {
            val providers = listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER).filter { manager.isProviderEnabled(it) }
            if (providers.isEmpty()) { lastProblem = LocationProblem.DISABLED; return null }
            val location = withTimeoutOrNull(10_000) {
                suspendCancellableCoroutine<Location> { continuation ->
                    val listener = object : LocationListener {
                        override fun onLocationChanged(location: Location) {
                            if (continuation.isActive) continuation.resume(location)
                            manager.removeUpdates(this)
                        }
                        override fun onProviderEnabled(provider: String) = Unit
                        override fun onProviderDisabled(provider: String) = Unit
                        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
                    }
                    continuation.invokeOnCancellation { manager.removeUpdates(listener) }
                    try { providers.forEach { manager.requestLocationUpdates(it, 0L, 0f, listener, Looper.getMainLooper()) } }
                    catch (e: SecurityException) { manager.removeUpdates(listener); if (continuation.isActive) continuation.resumeWithException(e) }
                }
            } ?: run { lastProblem = LocationProblem.TIMEOUT; return null }
            val name = reverseGeocode(location)
            return WeatherLocation(location.latitude, location.longitude, name, LocationSource.CURRENT)
        } catch (e: CancellationException) { throw e
        } catch (_: SecurityException) { lastProblem = LocationProblem.PERMISSION
        } catch (_: IllegalArgumentException) { lastProblem = LocationProblem.UNAVAILABLE }
        return null
    }
    @Suppress("DEPRECATION")
    private suspend fun reverseGeocode(location: Location): String? = withTimeoutOrNull(1500) {
        withContext(Dispatchers.IO) {
            try {
                geocoder.getFromLocation(location.latitude, location.longitude, 1)?.firstOrNull()?.let {
                    listOfNotNull(it.adminArea, it.subLocality ?: it.locality).distinct().joinToString(" ").ifBlank { null }
                }
            } catch (_: java.io.IOException) { null } catch (_: IllegalArgumentException) { null }
        }
    }
}
