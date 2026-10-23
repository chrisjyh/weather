package com.example.weather.data

import android.content.Context
import androidx.room.Room

object WeatherData {
    fun createDao(context: Context): WeatherDao = Room.databaseBuilder(context, WeatherDatabase::class.java, "weather.db").build().weather()
}
