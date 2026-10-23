package com.example.weather.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.weather.R
import com.example.weather.domain.*
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

private val hourFormat = DateTimeFormatter.ofPattern("HH시").withZone(KOREA)
private val stampFormat = DateTimeFormatter.ofPattern("MM.dd HH:mm").withZone(KOREA)
private fun Double?.degrees() = this?.let { "${it.roundToInt()}°" } ?: "—"
fun Condition.label(): String = when (this) {
    Condition.CLEAR -> "맑음"; Condition.CLOUDY -> "구름많음"; Condition.OVERCAST -> "흐림"
    Condition.RAIN -> "비"; Condition.RAIN_SNOW -> "비·눈"; Condition.SNOW -> "눈"; Condition.SHOWER -> "소나기"
    Condition.DRIZZLE -> "빗방울"; Condition.FLURRIES -> "눈날림"; Condition.UNKNOWN -> "정보 없음"
}
private fun WeatherFailure.label() = when (this) {
    WeatherFailure.CONFIGURATION -> "기상청 인증키 설정이 필요해요 · KMA_SERVICE_KEY"
    WeatherFailure.NETWORK -> "오프라인 · 네트워크 연결을 확인해 주세요"
    WeatherFailure.TIMEOUT -> "날씨 요청 시간이 초과됐어요"
    WeatherFailure.AUTHENTICATION -> "기상청 인증키와 서비스 이용 승인을 확인해 주세요"
    WeatherFailure.QUOTA -> "기상청 호출 한도를 초과했어요 · 나중에 다시 시도해 주세요"
    WeatherFailure.NO_DATA -> "일부 발표 자료가 아직 없어요"
    WeatherFailure.HTTP -> "기상청 서버에 연결하지 못했어요"
    WeatherFailure.MALFORMED -> "기상청 응답을 확인할 수 없어요"
    WeatherFailure.STORAGE -> "저장된 날씨를 읽거나 저장하지 못했어요"
}
@Composable
fun WeatherScreen(state: WeatherUiState, onRefresh: () -> Unit, onPermission: () -> Unit) {
    val simplified = state.driving.simplified
    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        BoxWithConstraints(Modifier.fillMaxSize().padding(padding)) {
            val wide = maxWidth >= 840.dp
            val compact = maxHeight < 700.dp
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState(), enabled = !simplified).padding(if (wide) 28.dp else 20.dp), verticalArrangement = Arrangement.spacedBy(if (compact) 10.dp else 16.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text(state.location.name ?: if (state.location.source == LocationSource.CURRENT) "현재 위치" else "마지막 위치", fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
                        Text(when (state.location.source) { LocationSource.CURRENT -> "현재 위치"; LocationSource.LAST -> "마지막 위치"; LocationSource.DEFAULT -> "기본 지역" } +
                            (state.weather?.fetchedAt?.let { " · ${stampFormat.format(it)} 조회" } ?: " · 날씨 확인"), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    WeatherActionButton(if (state.refreshing) "확인 중" else "새로고침", !state.refreshing, onRefresh)
                }
                if (simplified) StatusBanner(when (state.driving.status) {
                    DrivingStatus.UNKNOWN -> "차량 상태 확인 중 · 핵심 정보만 표시"
                    DrivingStatus.DISCONNECTED -> "차량 연결 끊김 · 핵심 정보만 표시"
                    else -> "주행 제한 · 핵심 정보만 표시"
                }, warning = false)
                val weather = state.weather
                if (state.initialLoading && weather?.hasData != true) {
                    LoadingWeather()
                } else if (weather?.hasData != true) {
                    Surface(shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(28.dp), verticalArrangement = Arrangement.spacedBy(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("날씨를 불러오지 못했어요", style = MaterialTheme.typography.headlineSmall)
                            Text(state.issues.firstOrNull()?.kind?.label() ?: "저장된 날씨가 없습니다. 연결 후 다시 시도해 주세요.")
                            WeatherActionButton("다시 시도", !state.refreshing, onRefresh)
                        }
                    }
                } else {
                    if (state.issues.isNotEmpty()) StatusBanner(state.issues.map { it.kind.label() }.distinct().joinToString("\n") + if (simplified) "" else "\n사용 가능한 저장 자료를 표시합니다")
                    if (state.stale) StatusBanner("오래된 실황 · ${weather.observedAt?.let(stampFormat::format) ?: "시각 미확인"} 기준")
                    if (wide) Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        Column(Modifier.weight(0.4f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            CurrentWeatherHero(weather, compact, spacious = !compact)
                            RainMetrics(weather)
                        }
                        Column(Modifier.weight(0.6f)) { HourlyForecast(weather.hourly, if (simplified) 2 else 6, simplified, compact, spacious = !compact) }
                    } else {
                        CurrentWeatherHero(weather, compact)
                        RainMetrics(weather)
                        HourlyForecast(weather.hourly, if (simplified) 2 else 4, simplified, compact)
                    }
                    if (!simplified) Text("기상청 · 실황 ${weather.observedAt?.let(stampFormat::format) ?: "자료 없음"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (!state.permissionGranted && !simplified) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("위치 권한이 꺼져 있어요", Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    WeatherActionButton("위치 허용", true, onPermission)
                } else if (state.location.problem != null && !simplified) Text(when (state.location.problem) {
                    LocationProblem.DISABLED -> "위치 서비스가 꺼져 있어 저장 위치 또는 기본 지역을 사용합니다"
                    LocationProblem.TIMEOUT -> "위치 확인 시간이 초과되어 저장 위치 또는 기본 지역을 사용합니다"
                    else -> "위치를 확인할 수 없어 저장 위치 또는 기본 지역을 사용합니다"
                }, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (!compact && !simplified) Text("PLEOSE WEATHER", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
@Composable
fun CurrentWeatherHero(weather: WeatherSnapshot, compact: Boolean = false, spacious: Boolean = false) {
    Surface(shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth().heightIn(min = if (spacious) 320.dp else 0.dp)) {
        Column(Modifier.padding(if (compact) 16.dp else 24.dp), verticalArrangement = if (spacious) Arrangement.SpaceBetween else Arrangement.spacedBy(8.dp)) {
            if (!compact) Text("오늘 · ${weather.day.format(DateTimeFormatter.ofPattern("M월 d일 EEEE", Locale.KOREAN))}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text(weather.temperature.degrees(), fontSize = if (compact) 64.sp else 84.sp, fontWeight = FontWeight.Light)
                    Text("최고 ${weather.high.degrees()}   최저 ${weather.low.degrees()}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    WeatherGlyph(weather.condition, Modifier.size(if (compact) 56.dp else 80.dp))
                    Text(weather.condition.label(), fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            if (spacious) Text(forecastSummary(weather.hourly), fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
private fun forecastSummary(hours: List<HourlyForecast>): String {
    val wet = hours.firstOrNull { it.condition in setOf(Condition.RAIN, Condition.RAIN_SNOW, Condition.SNOW, Condition.SHOWER, Condition.DRIZZLE, Condition.FLURRIES) }
    return if (wet != null) "${hourFormat.format(wet.time)} ${wet.condition.label()} 예보"
    else if (hours.size < 6 || hours.any { it.condition == Condition.UNKNOWN }) "일부 시간의 강수 정보가 없어요"
    else "향후 6시간 강수 예보 없음"
}
@Composable
private fun RainMetrics(weather: WeatherSnapshot) {
    val probability = weather.hourly.mapNotNull { it.precipitationProbability }.maxOrNull()
    val next = weather.hourly.firstOrNull()?.takeIf { it.time <= Instant.now().plusSeconds(3600) }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        WeatherMetric("향후 6시간 최대\n강수 확률", probability?.let { "$it%" } ?: "—", Modifier.weight(1f))
        WeatherMetric("다음 1시간\n예상 강수량", next?.precipitationAmount ?: "—", Modifier.weight(1f))
    }
}
@Composable
fun WeatherMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(modifier, shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, fontSize = 24.sp, fontWeight = FontWeight.Medium)
        }
    }
}
fun selectHours(hours: List<HourlyForecast>, count: Int): List<HourlyForecast> {
    if (hours.size <= count) return hours
    val change = hours.drop(1).firstOrNull { it.condition != hours.first().condition && it.condition != Condition.UNKNOWN }
    if (count == 2) return listOf(hours.first(), change ?: hours.last())
    return listOfNotNull(hours.first(), hours.getOrNull(2), change ?: hours.getOrNull(4), hours.last()).distinctBy { it.time }.let { chosen ->
        (chosen + hours.filter { h -> chosen.none { it.time == h.time } }.take(count - chosen.size)).sortedBy { it.time }
    }
}
@Composable
fun HourlyForecast(hours: List<HourlyForecast>, count: Int, simplified: Boolean, compact: Boolean = false, spacious: Boolean = false) {
    Surface(shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth().heightIn(min = if (spacious) 320.dp else 0.dp)) {
        Column(Modifier.padding(if (compact) 12.dp else 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(if (simplified) "주요 예보 · 2개 시점" else "시간별 예보 · 향후 6시간", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            if (spacious) Text(forecastSummary(hours), color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (hours.isEmpty()) Text("유효한 예보가 없습니다", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                selectHours(hours, count).forEachIndexed { index, hour ->
                    Column(Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(if (index == 0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface).padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(hourFormat.format(hour.time), style = MaterialTheme.typography.titleMedium)
                        WeatherGlyph(hour.condition, Modifier.size(if (compact) 28.dp else 36.dp))
                        Text(hour.temperature.degrees(), fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                        Text(hour.precipitationProbability?.let { "$it%" } ?: "—", color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}
@Composable
fun StatusBanner(message: String, warning: Boolean = true) {
    Surface(color = if (warning) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
        contentColor = if (warning) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer,
        shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Text(message, Modifier.padding(14.dp), style = MaterialTheme.typography.bodyMedium)
    }
}
@Composable
fun WeatherActionButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    Button(onClick, enabled = enabled, modifier = Modifier.heightIn(min = 56.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer)) {
        if (label == "새로고침") { WeatherDrawable("refresh", Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)) }
        Text(label, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}
@Composable
private fun LoadingWeather() {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.surface).padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Text("날씨 확인 중", style = MaterialTheme.typography.headlineSmall)
        listOf(100, 40, 72).forEach { height -> Box(Modifier.fillMaxWidth(if (height == 100) 0.45f else 1f).height(height.dp).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.primaryContainer)) }
    }
}
@Composable
private fun WeatherGlyph(condition: Condition, modifier: Modifier) {
    when (condition) {
        Condition.CLEAR -> WeatherDrawable("sun", modifier, condition.label())
        Condition.CLOUDY -> WeatherDrawable("partly", modifier, condition.label())
        Condition.OVERCAST -> WeatherDrawable("cloud", modifier, condition.label())
        Condition.RAIN, Condition.SHOWER, Condition.DRIZZLE -> WeatherDrawable("rain", modifier, condition.label())
        else -> Box(modifier, contentAlignment = Alignment.Center) { Text(if (condition == Condition.UNKNOWN) "—" else "❄", fontSize = 28.sp) }
    }
}
@Composable
private fun WeatherDrawable(name: String, modifier: Modifier, description: String? = null) {
    val dark = MaterialTheme.colorScheme.background.red < 0.5f
    val resource = when (name) {
        "sun" -> if (dark) R.drawable.night_sun else R.drawable.day_sun
        "partly" -> if (dark) R.drawable.night_partly else R.drawable.day_partly
        "rain" -> if (dark) R.drawable.night_rain else R.drawable.day_rain
        "refresh" -> if (dark) R.drawable.night_refresh else R.drawable.day_refresh
        else -> if (dark) R.drawable.night_cloud else R.drawable.day_cloud
    }
    Image(painterResource(resource), contentDescription = description, modifier = modifier)
}
