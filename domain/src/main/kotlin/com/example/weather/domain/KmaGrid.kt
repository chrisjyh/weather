package com.example.weather.domain

import kotlin.math.*

/** KMA Lambert conformal conic projection, 5 km grid. */
fun WeatherLocation.grid(): Grid = toKmaGrid(latitude, longitude)
fun toKmaGrid(latitude: Double, longitude: Double): Grid {
    require(latitude.isFinite() && latitude in -90.0..90.0 && longitude.isFinite() && longitude in -180.0..180.0)
    val rad = PI / 180.0
    val slat1 = 30 * rad
    val slat2 = 60 * rad
    val sn = ln(cos(slat1) / cos(slat2)) / ln(tan(PI / 4 + slat2 / 2) / tan(PI / 4 + slat1 / 2))
    val sf = tan(PI / 4 + slat1 / 2).pow(sn) * cos(slat1) / sn
    val ro = 6371.00877 / 5 * sf / tan(PI / 4 + 38 * rad / 2).pow(sn)
    val ra = 6371.00877 / 5 * sf / tan(PI / 4 + latitude * rad / 2).pow(sn)
    var theta = longitude * rad - 126 * rad
    if (theta > PI) theta -= 2 * PI
    if (theta < -PI) theta += 2 * PI
    theta *= sn
    return Grid(floor(ra * sin(theta) + 43.5).toInt(), floor(ro - ra * cos(theta) + 136.5).toInt())
}
