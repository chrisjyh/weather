package com.example.weather.data

import com.example.weather.domain.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import java.time.Clock
import java.time.Duration
import java.time.LocalDateTime

class KmaWeatherRepository(private val api: KmaApi, private val dao: WeatherDao, private val clock: Clock = Clock.systemUTC()) : WeatherRepository {
    private val mapper = WeatherMapper()
    private val mutex = Mutex()
    override fun observe(grid: Grid) = dao.observe(grid.x, grid.y).map { mapper.map(grid, it, clock.instant()) }
    override suspend fun refresh(grid: Grid, force: Boolean): List<WeatherIssue> = mutex.withLock {
        val now = clock.instant()
        val issues = mutableListOf<WeatherIssue>()
        val records = dao.records(grid.x, grid.y).toMutableList()
        suspend fun fetch(product: Product, release: LocalDateTime, forced: Boolean): Boolean {
            if (!forced && records.any { it.product == product.name && it.release == release.toString() }) return true
            val items = api.fetch(product, grid, release)
            if (items.none { it.validAt != null }) throw KmaException(WeatherFailure.MALFORMED)
            val record = WeatherRecord(grid.x, grid.y, product.name, release.toString(), now.toEpochMilli(), KmaApi.json.encodeToString(items))
            dao.put(record)
            records.removeAll { it.product == record.product && it.release == record.release }
            records.add(record)
            return true
        }
        for (product in Product.entries) {
            val release = KmaTime.latest(product, now)
            try {
                try { fetch(product, release, force) } catch (e: KmaException) {
                    val available = release.atZone(KOREA).toInstant().plusSeconds(if (product == Product.ULTRA) 15 * 60 else 10 * 60)
                    if (e.failure == WeatherFailure.NO_DATA && Duration.between(available, now) < Duration.ofMinutes(30)) {
                        fetch(product, KmaTime.previous(product, release), false)
                        issues += WeatherIssue(WeatherFailure.NO_DATA, product.name)
                    } else throw e
                }
                if (product == Product.VILLAGE) {
                    val mapped = mapper.map(grid, records, now)
                    if (mapped?.high == null || mapped.low == null) {
                        // Yesterday 23:00 includes today's official minimum and maximum even after today's morning period.
                        val daily = now.atZone(KOREA).toLocalDate().minusDays(1).atTime(23, 0)
                        if (daily != release) fetch(product, daily, false)
                    }
                }
            } catch (e: CancellationException) { throw e
            } catch (e: KmaException) {
                issues += WeatherIssue(e.failure, product.name)
                if (e.failure in listOf(WeatherFailure.AUTHENTICATION, WeatherFailure.QUOTA, WeatherFailure.CONFIGURATION)) break
            }
        }
        dao.prune(now.minusSeconds(48 * 3600).toEpochMilli())
        issues
    }
}
