package com.example.weather.data

data class DoorCapability(
    val accessType: String,
    val minimum: Int?,
    val maximum: Int?
) {
    val isSupported: Boolean = minimum != null && maximum != null
    val canControl: Boolean =
        isSupported && "WRITE" in accessType.uppercase() && minimum!! <= 0 && maximum!! >= 1
}

interface VehicleRepository {
    fun initialize(callback: (Result<Unit>) -> Unit)
    fun checkDoorCapability(callback: (Result<DoorCapability>) -> Unit)
    fun readDoorPosition(callback: (Result<Int?>) -> Unit)
    fun setDoorPosition(position: Int, callback: (Result<Int?>) -> Unit)
    fun startDoorUpdates(onUpdate: (Int?) -> Unit, callback: (Result<Unit>) -> Unit)
    fun stopDoorUpdates(callback: (Result<Unit>) -> Unit = {})
    fun release()
}
