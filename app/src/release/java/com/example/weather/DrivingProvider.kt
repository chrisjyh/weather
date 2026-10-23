package com.example.weather

import android.content.Context
import com.example.weather.data.CarDrivingRestrictionsRepository
import com.example.weather.domain.DrivingRestrictionsRepository

fun drivingRepository(context: Context): DrivingRestrictionsRepository = CarDrivingRestrictionsRepository(context)
