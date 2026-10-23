package com.example.weather.data

import android.car.Car
import android.car.drivingstate.CarUxRestrictions
import android.car.drivingstate.CarUxRestrictionsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import com.example.weather.domain.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*

class CarDrivingRestrictionsRepository(private val context: Context) : DrivingRestrictionsRepository {
    override val restrictions: Flow<DrivingRestrictions> = if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE)) {
        flowOf(DrivingRestrictions(DrivingStatus.UNSUPPORTED, false))
    } else automotiveRestrictions()
    private fun automotiveRestrictions(): Flow<DrivingRestrictions> = callbackFlow {
        var manager: CarUxRestrictionsManager? = null
        var closed = false
        fun emit(value: CarUxRestrictions) { trySend(DrivingRestrictions(DrivingStatus.AVAILABLE, value.isRequiresDistractionOptimization, value.activeRestrictions)) }
        val car = try {
            Car.createCar(context, Handler(Looper.getMainLooper()), Car.CAR_WAIT_TIMEOUT_DO_NOT_WAIT) { connected, ready ->
                if (!closed) {
                    if (ready) {
                        try {
                            manager = connected.getCarManager(Car.CAR_UX_RESTRICTION_SERVICE) as CarUxRestrictionsManager
                            manager?.registerListener(::emit)
                            manager?.currentCarUxRestrictions?.let(::emit)
                        } catch (_: RuntimeException) { trySend(DrivingRestrictions(DrivingStatus.DISCONNECTED)) }
                    } else { manager = null; trySend(DrivingRestrictions(DrivingStatus.DISCONNECTED)) }
                }
            }
        } catch (_: RuntimeException) { trySend(DrivingRestrictions(DrivingStatus.DISCONNECTED)); null }
        if (car == null) trySend(DrivingRestrictions(DrivingStatus.DISCONNECTED))
        awaitClose {
            closed = true
            try { manager?.unregisterListener() } catch (_: RuntimeException) { }
            car?.disconnect()
        }
    }.distinctUntilChanged()
}
