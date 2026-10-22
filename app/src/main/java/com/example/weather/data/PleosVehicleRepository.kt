package com.example.weather.data

import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.api.model.area.DoorArea
import ai.pleos.playground.vehicle.constant.area.DoorValue
import ai.pleos.playground.vehicle.listener.DoorPositionListener
import android.content.Context

class PleosVehicleRepository(context: Context) : VehicleRepository {
    private val vehicle = Vehicle(context.applicationContext)
    private val door = vehicle.getDoor()
    private val area = DoorArea(DoorValue.ROW_1_LEFT)
    private var initialized = false
    private var listener: DoorPositionListener? = null

    override fun initialize(callback: (Result<Unit>) -> Unit) {
        if (initialized) return callback(Result.success(Unit))
        callback(runCatching {
            vehicle.initialize()
            initialized = true
        })
    }

    override fun checkDoorCapability(callback: (Result<DoorCapability>) -> Unit) = sdkCall(callback) {
        door.checkDoorPositionCapabilities(
            onComplete = { config, areaBounds ->
                val bounds = areaBounds[area]
                callback(Result.success(
                    DoorCapability(
                        accessType = config.accessType.name,
                        minimum = bounds?.minValue?.toInt(),
                        maximum = bounds?.maxValue?.toInt()
                    )
                ))
            },
            onFailed = { callback(Result.failure(it)) }
        )
    }

    override fun readDoorPosition(callback: (Result<Int?>) -> Unit) = sdkCall(callback) {
        door.getCurrentDoorPosition(
            area = area,
            onComplete = { callback(Result.success(it)) },
            onFailed = { callback(Result.failure(it)) }
        )
    }

    override fun setDoorPosition(position: Int, callback: (Result<Int?>) -> Unit) = sdkCall(callback) {
        door.setDoorPosition(
            area = area,
            value = position,
            onComplete = { _, accepted -> callback(Result.success(accepted)) },
            onFailed = { callback(Result.failure(it)) }
        )
    }

    override fun startDoorUpdates(
        onUpdate: (Int?) -> Unit,
        callback: (Result<Unit>) -> Unit
    ) = sdkCall(callback) {
        stopDoorUpdates()
        val newListener = object : DoorPositionListener {
            override fun onPositionUpdated(pos: Int?) = onUpdate(pos)
            override fun onFailed(e: Exception) = callback(Result.failure(e))
        }
        door.registerDoorPosition(area, newListener)
        listener = newListener
        callback(Result.success(Unit))
    }

    override fun stopDoorUpdates(callback: (Result<Unit>) -> Unit) {
        val activeListener = listener ?: return callback(Result.success(Unit))
        sdkCall(callback) {
            door.unregisterDoorPosition(area, activeListener)
            listener = null
            callback(Result.success(Unit))
        }
    }

    override fun release() {
        if (!initialized) return
        runCatching { stopDoorUpdates() }
        runCatching { vehicle.release() }
        initialized = false
    }

    private inline fun <T> sdkCall(
        callback: (Result<T>) -> Unit,
        action: () -> Unit
    ) {
        if (!initialized) {
            callback(Result.failure(IllegalStateException("Vehicle SDK is not initialized.")))
            return
        }
        runCatching(action).onFailure { callback(Result.failure(it)) }
    }
}
