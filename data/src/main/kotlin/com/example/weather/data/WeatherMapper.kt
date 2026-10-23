package com.example.weather.data

import com.example.weather.domain.*
import java.time.Instant
import kotlin.math.abs

fun String?.weatherNumber(): Double? = this?.toDoubleOrNull()?.takeIf { it.isFinite() && abs(it) < 900 }
fun condition(pty: String?, sky: String?): Condition = when (pty?.weatherNumber()?.toInt()) {
    1 -> Condition.RAIN; 2, 6 -> Condition.RAIN_SNOW; 3 -> Condition.SNOW
    4 -> Condition.SHOWER; 5 -> Condition.DRIZZLE; 7 -> Condition.FLURRIES
    0 -> when (sky?.toIntOrNull()) { 1 -> Condition.CLEAR; 3 -> Condition.CLOUDY; 4 -> Condition.OVERCAST; else -> Condition.UNKNOWN }
    else -> Condition.UNKNOWN
}
fun precipitation(raw: String?): String? {
    if (raw == null || raw.isBlank()) return null
    if (raw == "-" || raw == "강수없음" || raw == "null") return "강수없음"
    val numeric = raw.toDoubleOrNull()
    return if (numeric != null) when {
        !numeric.isFinite() || numeric < 0 || numeric >= 900 -> null
        numeric == 0.0 -> "강수없음"
        numeric < 1 -> "1mm 미만"
        numeric < 30 -> "${raw}mm"
        numeric < 50 -> "30.0~50.0mm"
        else -> "50.0mm 이상"
    } else raw.takeIf { it.contains("mm") && !it.startsWith("-") }
}
class WeatherMapper {
    fun map(grid: Grid, records: List<WeatherRecord>, now: Instant): WeatherSnapshot? {
        if (records.isEmpty()) return null
        val sorted = records.sortedByDescending { it.release }
        fun items(product: Product): List<KmaItem> = sorted.filter { it.product == product.name }.flatMap {
            try { KmaApi.json.decodeFromString<List<KmaItem>>(it.items) } catch (_: Exception) { emptyList() }
        }
        val observations = items(Product.OBSERVATION)
        val observationTime = observations.mapNotNull { it.validAt }.filter { it <= now }.maxOrNull()
        val observation = observations.filter { it.validAt == observationTime }.associateBy { it.category }
        // Newest release wins for each category and valid hour; older official extrema remain usable for today.
        fun merged(product: Product) = items(product).filter { it.validAt != null }.distinctBy { it.validAt to it.category }
        val ultra = merged(Product.ULTRA)
        val village = merged(Product.VILLAGE)
        val byHour = (ultra + village).filter { it.category in setOf("T1H", "TMP", "SKY", "PTY", "POP", "PCP", "RN1") }.groupBy { it.validAt!! }
        val hours = byHour.keys.filter { it > now && it <= now.plusSeconds(6 * 3600) }.sorted().map { t ->
            val values = byHour.getValue(t).associateFirst()
            HourlyForecast(t, (values["T1H"] ?: values["TMP"]).weatherNumber(), condition(values["PTY"], values["SKY"]),
                values["POP"].weatherNumber()?.toInt()?.takeIf { it in 0..100 }, precipitation(values["PCP"] ?: values["RN1"]))
        }
        val today = now.atZone(KOREA).toLocalDate()
        fun extremum(category: String) = village.firstOrNull { it.category == category && it.validAt?.atZone(KOREA)?.toLocalDate() == today && it.value.weatherNumber() != null }?.value.weatherNumber()
        val near = ultra.filter { it.validAt!! >= now.minusSeconds(3600) && it.validAt!! <= now.plusSeconds(3600) }
            .groupBy { it.validAt!! }.minByOrNull { abs(it.key.epochSecond - now.epochSecond) }
        val sky = near?.value?.firstOrNull { it.category == "SKY" }?.value
        val observedPty = observation["PTY"]?.value
        val currentCondition = condition(observedPty, sky)
        return WeatherSnapshot(grid, observationTime, Instant.ofEpochMilli(records.maxOf { it.fetchedAt }),
            observation["T1H"]?.value.weatherNumber(), currentCondition, extremum("TMX"), extremum("TMN"), today, hours,
            if (observedPty == "0") near?.key else observationTime).validAt(now)
    }
    private fun List<KmaItem>.associateFirst(): Map<String, String?> = filter { item ->
        when (item.category) {
            "PCP", "RN1" -> precipitation(item.value) != null
            "SKY" -> item.value?.toIntOrNull() in listOf(1, 3, 4)
            "PTY" -> item.value?.toIntOrNull() in 0..7
            "POP" -> item.value.weatherNumber()?.let { it in 0.0..100.0 } == true
            else -> item.value.weatherNumber() != null
        }
    }.distinctBy { it.category }.associate { it.category to it.value }
}
