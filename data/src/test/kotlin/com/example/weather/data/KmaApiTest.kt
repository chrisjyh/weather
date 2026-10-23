package com.example.weather.data

import com.example.weather.domain.*
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

fun fixture(name: String) = checkNotNull(KmaApiTest::class.java.getResource("/fixtures/$name")).readText()
class KmaApiTest {
    private lateinit var server: MockWebServer
    private lateinit var api: KmaApi
    private val release = LocalDateTime.parse("2026-10-23T12:00")
    @Before fun setup() {
        server = MockWebServer(); server.start()
        val service = Retrofit.Builder().baseUrl(server.url("/")).client(OkHttpClient.Builder().readTimeout(150, TimeUnit.MILLISECONDS).retryOnConnectionFailure(false).build()).build().create(KmaService::class.java)
        api = KmaApi(service, "a+b/c=")
    }
    @After fun close() { server.shutdown() }
    @Test fun validJsonAndDecodedKeyEncoding() = runTest {
        server.enqueue(MockResponse().setBody(fixture("observation.json")))
        assertEquals("21.4", api.fetch(Product.OBSERVATION, Grid(61,126), release).first().value)
        val url = server.takeRequest().requestUrl!!
        assertEquals("a+b/c=", url.queryParameter("serviceKey"))
        assertEquals("61", url.queryParameter("nx"))
        assertEquals("1200", url.queryParameter("base_time"))
    }
    @Test fun emptyAuthQuotaAndMalformedAreDistinct() = runTest {
        listOf("empty.json" to WeatherFailure.NO_DATA, "auth.xml" to WeatherFailure.AUTHENTICATION, "quota.json" to WeatherFailure.QUOTA).forEach { (name, expected) ->
            server.enqueue(MockResponse().setBody(fixture(name)))
            assertFailure(expected)
        }
        server.enqueue(MockResponse().setBody("{broken")); assertFailure(WeatherFailure.MALFORMED)
        server.enqueue(MockResponse().setResponseCode(503)); assertFailure(WeatherFailure.HTTP)
        server.enqueue(MockResponse().setResponseCode(429)); assertFailure(WeatherFailure.QUOTA)
    }
    @Test fun timeoutDoesNotExposeRequestUrl() = runTest {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
        assertFailure(WeatherFailure.TIMEOUT)
    }
    @Test fun missingKeyDoesNotCallServer() = runTest {
        try { KmaApi.create("", server.url("/").toString()).fetch(Product.ULTRA, Grid(61,126), release); fail() }
        catch (e: KmaException) { assertEquals(WeatherFailure.CONFIGURATION, e.failure) }
        assertEquals(0, server.requestCount)
    }
    private suspend fun assertFailure(expected: WeatherFailure) {
        try { api.fetch(Product.OBSERVATION, Grid(61,126), release); fail("Expected $expected") }
        catch (e: KmaException) { assertEquals(expected, e.failure); assertFalse(e.message.orEmpty().contains("serviceKey")) }
    }
}
