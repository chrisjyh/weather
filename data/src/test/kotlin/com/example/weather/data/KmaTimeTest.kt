package com.example.weather.data

import com.example.weather.domain.KOREA
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime

class KmaTimeTest {
    private fun slot(product: Product, local: String) = KmaTime.latest(product, LocalDateTime.parse(local).atZone(KOREA).toInstant()).toString()
    @Test fun observationBoundaryAndMidnight() {
        assertEquals("2026-10-22T23:00", slot(Product.OBSERVATION, "2026-10-23T00:09:59"))
        assertEquals("2026-10-23T00:00", slot(Product.OBSERVATION, "2026-10-23T00:10:00"))
    }
    @Test fun ultraBoundary() {
        assertEquals("2026-10-22T23:30", slot(Product.ULTRA, "2026-10-23T00:44:59"))
        assertEquals("2026-10-23T00:30", slot(Product.ULTRA, "2026-10-23T00:45:00"))
    }
    @Test fun villageBoundaryAndYearRollover() {
        assertEquals("2025-12-31T23:00", slot(Product.VILLAGE, "2026-01-01T02:09:59"))
        assertEquals("2026-01-01T02:00", slot(Product.VILLAGE, "2026-01-01T02:10:00"))
        assertEquals("2026-01-01T23:00", slot(Product.VILLAGE, "2026-01-01T23:10:00"))
    }
}
