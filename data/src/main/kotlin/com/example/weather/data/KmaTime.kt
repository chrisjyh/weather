package com.example.weather.data

import com.example.weather.domain.KOREA
import java.time.Instant
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

enum class Product(val endpoint: String) { OBSERVATION("getUltraSrtNcst"), ULTRA("getUltraSrtFcst"), VILLAGE("getVilageFcst") }
object KmaTime {
    val date: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")
    val time: DateTimeFormatter = DateTimeFormatter.ofPattern("HHmm")
    // data.go.kr guide 2610 (2026-10-19), not the API Hub's separate 10-minute API.
    fun latest(product: Product, now: Instant): LocalDateTime {
        val local = now.atZone(KOREA).toLocalDateTime()
        return when (product) {
            Product.OBSERVATION -> local.minusMinutes(10).truncatedTo(ChronoUnit.HOURS)
            Product.ULTRA -> local.minusMinutes(45).truncatedTo(ChronoUnit.HOURS).plusMinutes(30)
            Product.VILLAGE -> {
                val available = local.minusMinutes(10)
                val hour = listOf(23, 20, 17, 14, 11, 8, 5, 2).firstOrNull { it <= available.hour }
                if (hour == null) available.toLocalDate().minusDays(1).atTime(23, 0)
                else available.toLocalDate().atTime(hour, 0)
            }
        }
    }
    fun previous(product: Product, release: LocalDateTime) = release.minusHours(if (product == Product.VILLAGE) 3 else 1)
    fun instant(date: String, time: String): Instant? = try {
        LocalDateTime.parse(date + time, DateTimeFormatter.ofPattern("yyyyMMddHHmm")).atZone(KOREA).toInstant()
    } catch (_: java.time.DateTimeException) { null }
}
