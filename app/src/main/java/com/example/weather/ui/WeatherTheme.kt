package com.example.weather.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.ExperimentalTextApi
import com.example.weather.R

@OptIn(ExperimentalTextApi::class)
private val AstaSans = FontFamily(listOf(300, 400, 500, 600, 700).map {
    Font(R.font.asta_sans, weight = FontWeight(it), variationSettings = FontVariation.Settings(FontVariation.weight(it)))
})
private val BaseTypography = Typography()
private val WeatherTypography = with(BaseTypography) { copy(
    displayLarge = displayLarge.copy(fontFamily = AstaSans),
    headlineSmall = headlineSmall.copy(fontFamily = AstaSans),
    bodyLarge = bodyLarge.copy(fontFamily = AstaSans),
    bodyMedium = bodyMedium.copy(fontFamily = AstaSans),
    bodySmall = bodySmall.copy(fontFamily = AstaSans),
    titleMedium = titleMedium.copy(fontFamily = AstaSans),
    labelLarge = labelLarge.copy(fontFamily = AstaSans),
    labelMedium = labelMedium.copy(fontFamily = AstaSans),
) }

@Composable
fun WeatherTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colors = if (dark) darkColorScheme(
        background = Color(0xFF0E151F), surface = Color(0xFF172330), onBackground = Color(0xFFF4F7FA),
        onSurface = Color(0xFFF4F7FA), onSurfaceVariant = Color(0xFFB2C0CF), primary = Color(0xFFA7D8FF),
        primaryContainer = Color(0xFF213A50), onPrimaryContainer = Color(0xFFA7D8FF),
        errorContainer = Color(0xFF3E3222), onErrorContainer = Color(0xFFFFD69A),
    ) else lightColorScheme(
        background = Color(0xFFEEF2F5), surface = Color.White, onBackground = Color(0xFF172937),
        onSurface = Color(0xFF172937), onSurfaceVariant = Color(0xFF4F6475), primary = Color(0xFF185C8D),
        primaryContainer = Color(0xFFDCECF8), onPrimaryContainer = Color(0xFF185C8D),
        errorContainer = Color(0xFFFFF0D9), onErrorContainer = Color(0xFF785017),
    )
    MaterialTheme(colorScheme = colors, typography = WeatherTypography, content = content)
}
