package com.example.weather.data

import com.example.weather.domain.*
import kotlinx.serialization.encodeToString
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

class WeatherMapperTest {
    private val now = Instant.parse("2026-10-23T03:20:00Z")
    private val grid = Grid(61,126)
    private fun record(product: Product, fixture: String, release: String = "2026-10-23T11:00") = WeatherRecord(61,126,product.name,release,now.toEpochMilli(),KmaApi.json.encodeToString(KmaApi.parse(fixture(fixture))))
    @Test fun combinesSourcesAndKeepsCategories() {
        val result = WeatherMapper().map(grid, listOf(record(Product.OBSERVATION,"observation.json"),record(Product.ULTRA,"ultra.json"),record(Product.VILLAGE,"village.json")), now)!!
        assertEquals(21.4, result.temperature!!, 0.0)
        assertEquals(22.0, result.hourly.first().temperature!!, 0.0)
        assertEquals("1mm 미만", result.hourly.first().precipitationAmount)
        assertEquals(70, result.hourly.first().precipitationProbability)
        assertEquals(24.0, result.high!!, 0.0)
        assertEquals(14.0, result.low!!, 0.0)
    }
    @Test fun missingExtremaAndPtyStayUnknown() {
        val result = WeatherMapper().map(grid, listOf(record(Product.ULTRA,"ultra.json")), now)!!
        assertNull(result.temperature); assertNull(result.high); assertNull(result.low)
        assertEquals(Condition.UNKNOWN, result.condition)
        assertNull(result.hourly.first().precipitationProbability)
        assertEquals(Condition.UNKNOWN, condition(null,"1"))
        assertEquals(Condition.UNKNOWN, condition("-999","1"))
    }
    @Test fun oldOfficialDailyExtremaAreReusedOnlyForSameDay() {
        val record = record(Product.VILLAGE,"village.json","2026-10-22T23:00")
        assertEquals(24.0, WeatherMapper().map(grid,listOf(record),now)!!.high!!,0.0)
        assertNull(WeatherMapper().map(grid,listOf(record),now.plusSeconds(86400))!!.high)
    }
    @Test fun sentinelAndPrecipitationValues() {
        assertNull("-999".weatherNumber()); assertNull("NaN".weatherNumber())
        assertNull(precipitation(null)); assertNull(precipitation("-999"))
        assertEquals("강수없음", precipitation("0")); assertEquals("1mm 미만", precipitation("0.5"))
        assertEquals("30.0~50.0mm", precipitation("30")); assertEquals("50.0mm 이상", precipitation("50"))
    }
}
