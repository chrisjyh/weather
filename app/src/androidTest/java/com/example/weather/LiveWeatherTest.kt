package com.example.weather

import androidx.test.platform.app.InstrumentationRegistry
import com.example.weather.data.*
import com.example.weather.domain.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test

/** Explicit opt-in; never substitutes fixtures into the running app. */
class LiveWeatherTest {
    @Test fun liveKmaForGangnam() = runBlocking {
        assumeTrue(InstrumentationRegistry.getArguments().getString("liveKma") == "true")
        assertTrue("Set KMA_SERVICE_KEY before building", BuildConfig.KMA_SERVICE_KEY.isNotBlank())
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val dao = WeatherData.createDao(context)
        val repository = KmaWeatherRepository(KmaApi.create(BuildConfig.KMA_SERVICE_KEY),dao)
        val issues = repository.refresh(ResolveLocation.DEFAULT.grid(),true)
        assertTrue("KMA failures: ${issues.map { it.kind }}",issues.isEmpty())
        val snapshot = repository.observe(ResolveLocation.DEFAULT.grid()).first()
        assertNotNull(snapshot?.temperature)
        assertTrue(snapshot!!.hourly.isNotEmpty())
    }
}
