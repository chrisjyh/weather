package com.example.weather

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.example.weather.domain.*
import com.example.weather.ui.*
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.time.Instant
import java.time.temporal.ChronoUnit

class WeatherScreenTest {
    @get:Rule val compose = createComposeRule()
    private val now = Instant.now().truncatedTo(ChronoUnit.HOURS)
    private val snapshot = WeatherSnapshot(Grid(61,126), now, now, 21.0, Condition.CLOUDY,23.0,14.0,now.atZone(KOREA).toLocalDate(),
        (1..6).map { HourlyForecast(now.plusSeconds(it*3600L),22.0-it,if(it<3) Condition.CLOUDY else Condition.RAIN,if(it<3) 10 else 70,"1mm 미만") })
    private val normal = WeatherUiState(weather=snapshot,initialLoading=false,permissionGranted=true,driving=DrivingRestrictions(DrivingStatus.AVAILABLE,false))
    @Test fun responsiveDayNightAndRestrictedScreens() {
        val width = mutableStateOf(560)
        val dark = mutableStateOf(false)
        val state = mutableStateOf(normal)
        compose.setContent { WeatherTheme(dark.value) { Box(Modifier.requiredSize(width.value.dp,800.dp).testTag("screen")) { WeatherScreen(state.value, {}, {}) } } }
        for (wide in listOf(false,true)) for (night in listOf(false,true)) {
            compose.runOnIdle { width.value = if(wide) 1120 else 560; dark.value=night; state.value=normal }
            compose.onAllNodesWithText("21°", useUnmergedTree=true)[0].assertIsDisplayed()
            compose.onNodeWithText("향후 6시간 최대\n강수 확률").assertIsDisplayed()
            screenshot("${if(wide) "wide" else "narrow"}-${if(night) "night" else "day"}")
        }
        compose.runOnIdle { width.value=560; state.value=normal.copy(driving=DrivingRestrictions(DrivingStatus.AVAILABLE,true),permissionGranted=false) }
        compose.onNodeWithText("주요 예보 · 2개 시점").assertIsDisplayed()
        compose.onNodeWithText("위치 허용").assertDoesNotExist()
        screenshot("restricted")
    }
    @Test fun compactRestrictedWindowKeepsBothForecastsVisible() {
        compose.setContent { WeatherTheme(true) { Box(Modifier.requiredSize(360.dp,600.dp).testTag("screen")) {
            WeatherScreen(normal.copy(driving=DrivingRestrictions(DrivingStatus.AVAILABLE,true)), {}, {})
        } } }
        compose.onNodeWithText("19°",useUnmergedTree=true).assertIsDisplayed()
        screenshot("compact-restricted")
    }
    @Test fun recoveryStatesAndDisabledRefresh() {
        val state = mutableStateOf(normal.copy(initialLoading=true,weather=null,refreshing=true))
        compose.setContent { WeatherTheme(true) { Box(Modifier.requiredSize(560.dp,800.dp).testTag("screen")) { WeatherScreen(state.value, {}, {}) } } }
        compose.onNodeWithText("날씨 확인 중").assertIsDisplayed()
        compose.onNodeWithText("확인 중").assertIsNotEnabled()
        screenshot("loading")
        compose.runOnIdle { state.value=normal.copy(weather=null,issues=listOf(WeatherIssue(WeatherFailure.CONFIGURATION))) }
        compose.onNodeWithText("다시 시도").assertIsDisplayed()
        screenshot("error")
        compose.runOnIdle { state.value=normal.copy(permissionGranted=false,issues=listOf(WeatherIssue(WeatherFailure.NETWORK))) }
        compose.onAllNodesWithText("21°",useUnmergedTree=true)[0].assertIsDisplayed()
        compose.onNodeWithText("위치 허용").assertExists()
        screenshot("offline-permission")
    }
    @OptIn(ExperimentalTestApi::class)
    private fun screenshot(name: String) {
        compose.waitForIdle()
        val bitmap = compose.onNodeWithTag("screen").captureToImage().asAndroidBitmap()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val directory = File(context.filesDir,"weather-qa").apply { mkdirs() }
        File(directory,"$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it) }
    }
}
