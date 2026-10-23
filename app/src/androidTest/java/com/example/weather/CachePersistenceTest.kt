package com.example.weather

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.example.weather.data.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class CachePersistenceTest {
    @Test fun dataAndLastLocationSurviveDatabaseReopenAndStayGridScoped() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "cache-persistence-test.db"
        context.deleteDatabase(name)
        fun open() = Room.databaseBuilder(context, WeatherDatabase::class.java, name).build()
        val record = WeatherRecord(61,126,"OBSERVATION","2026-10-23T12:00",1L,"[]")
        var database = open()
        try {
            database.weather().put(record)
            database.weather().putLocation(LocationRecord(latitude=37.5172,longitude=127.0473,name="서울 강남구"))
            database.close()
            database = open()
            assertEquals(listOf(record),database.weather().records(61,126))
            assertTrue(database.weather().records(60,127).isEmpty())
            assertEquals("서울 강남구",database.weather().location()!!.name)
        } finally { database.close(); context.deleteDatabase(name) }
    }
}
