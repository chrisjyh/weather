package com.example.weather.data

import com.example.weather.domain.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.*
import org.junit.*
import org.junit.Assert.*
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class KmaWeatherRepositoryTest {
    private val now = Instant.parse("2026-10-23T03:20:00Z")
    private val grid = Grid(61,126)
    private lateinit var server: MockWebServer
    private lateinit var dao: MemoryDao
    private lateinit var repository: KmaWeatherRepository
    @Before fun setup() {
        server = MockWebServer(); server.start(); dao = MemoryDao()
        repository = KmaWeatherRepository(KmaApi.create("test-key", server.url("/").toString()), dao, Clock.fixed(now, ZoneOffset.UTC))
    }
    @After fun close() { server.shutdown() }
    private fun success() {
        server.enqueue(MockResponse().setBody(fixture("observation.json")))
        server.enqueue(MockResponse().setBody(fixture("ultra.json")))
        server.enqueue(MockResponse().setBody(fixture("village.json")))
    }
    @Test fun cachesSuccessfulReleaseAndManualRefreshRechecks() = runTest {
        success(); assertTrue(repository.refresh(grid,false).isEmpty())
        assertEquals(3,server.requestCount)
        repository.refresh(grid,false); assertEquals(3,server.requestCount)
        success(); repository.refresh(grid,true); assertEquals(6,server.requestCount)
        assertNotNull(repository.observe(grid).first())
        assertNull(repository.observe(Grid(60,127)).first())
    }
    @Test fun authAndQuotaDoNotRetryOrCallOtherProducts() = runTest {
        for (name in listOf("auth.xml","quota.json")) {
            val before = server.requestCount
            server.enqueue(MockResponse().setBody(fixture(name)))
            assertEquals(1,repository.refresh(grid,true).size)
            assertEquals(before+1,server.requestCount)
        }
    }
    @Test fun onlyNoDataNearPublicationFallsBackOnce() = runTest {
        server.enqueue(MockResponse().setBody(fixture("empty.json")))
        success()
        assertEquals(WeatherFailure.NO_DATA,repository.refresh(grid,false).single().kind)
        assertEquals("1200",server.takeRequest().requestUrl!!.queryParameter("base_time"))
        assertEquals("1100",server.takeRequest().requestUrl!!.queryParameter("base_time"))
        assertEquals(4,server.requestCount)
    }
    @Test fun partialFailureKeepsObservationAndOtherProducts() = runTest {
        success(); repository.refresh(grid,false)
        server.enqueue(MockResponse().setResponseCode(503))
        server.enqueue(MockResponse().setBody(fixture("ultra.json")))
        server.enqueue(MockResponse().setBody(fixture("village.json")))
        val issues = repository.refresh(grid,true)
        assertEquals(WeatherFailure.HTTP,issues.single().kind)
        assertEquals(21.4,repository.observe(grid).first()!!.temperature!!,0.0)
    }
    @Test fun missingDailyValuesFetchYesterdayOfficialRelease() = runTest {
        server.enqueue(MockResponse().setBody(fixture("observation.json")))
        server.enqueue(MockResponse().setBody(fixture("ultra.json")))
        server.enqueue(MockResponse().setBody(fixture("ultra.json")))
        server.enqueue(MockResponse().setBody(fixture("village.json")))
        repository.refresh(grid,false)
        repeat(3) { server.takeRequest() }
        val request = server.takeRequest().requestUrl!!
        assertEquals("20261022",request.queryParameter("base_date"))
        assertEquals("2300",request.queryParameter("base_time"))
        assertEquals(14.0,repository.observe(grid).first()!!.low!!,0.0)
    }
}
class MemoryDao : WeatherDao {
    private val rows = MutableStateFlow<List<WeatherRecord>>(emptyList())
    private var saved: LocationRecord? = null
    override fun observe(x: Int,y: Int) = rows.map { it.filter { r -> r.x == x && r.y == y } }
    override suspend fun records(x: Int,y: Int) = observe(x,y).first()
    override suspend fun put(record: WeatherRecord) { rows.value = rows.value.filterNot { it.x == record.x && it.y == record.y && it.product == record.product && it.release == record.release } + record }
    override suspend fun prune(before: Long) { rows.value = rows.value.filter { it.fetchedAt >= before } }
    override suspend fun location() = saved
    override suspend fun putLocation(location: LocationRecord) { saved = location }
}
