package com.example.weather

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import com.example.weather.data.CarDrivingRestrictionsRepository
import com.example.weather.domain.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*

/** Shell-only debug override. No receiver or override is compiled into release. */
fun drivingRepository(context: Context): DrivingRestrictionsRepository = object : DrivingRestrictionsRepository {
    private val platform = CarDrivingRestrictionsRepository(context)
    private val override = callbackFlow<DrivingRestrictions?> {
        trySend(null)
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                trySend(when (intent.getStringExtra("state")) {
                    "restricted" -> DrivingRestrictions(DrivingStatus.AVAILABLE, true, 511)
                    "unknown" -> DrivingRestrictions()
                    "disconnected" -> DrivingRestrictions(DrivingStatus.DISCONNECTED)
                    "unrestricted" -> DrivingRestrictions(DrivingStatus.AVAILABLE, false)
                    else -> null
                })
            }
        }
        ContextCompat.registerReceiver(context, receiver, IntentFilter("com.example.weather.DEBUG_DRIVING"), "android.permission.DUMP", null, ContextCompat.RECEIVER_EXPORTED)
        awaitClose { context.unregisterReceiver(receiver) }
    }
    override val restrictions = combine(platform.restrictions, override) { real, debug -> debug ?: real }
}
