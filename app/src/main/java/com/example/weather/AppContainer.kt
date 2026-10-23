package com.example.weather

import android.app.Application
import com.example.weather.data.*
import com.example.weather.domain.*
import com.example.weather.ui.WeatherViewModel

class WeatherApplication : Application() {
    val container by lazy { AppContainer(this) }
}
class AppContainer(application: Application) {
    private val dao = WeatherData.createDao(application)
    private val repository = KmaWeatherRepository(KmaApi.create(BuildConfig.KMA_SERVICE_KEY), dao)
    private val locations = ResolveLocation(AndroidLocationRepository(application, dao))
    private val driving = drivingRepository(application)
    val factory = WeatherViewModel.factory(locations, repository, RefreshWeather(repository), driving)
}
