package com.example.weather.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(primaryKeys = ["x", "y", "product", "release"])
data class WeatherRecord(val x: Int, val y: Int, val product: String, val release: String, val fetchedAt: Long, val items: String)
@Entity(tableName = "last_location")
data class LocationRecord(@PrimaryKey val id: Int = 1, val latitude: Double, val longitude: Double, val name: String?)
@Dao
interface WeatherDao {
    @Query("SELECT * FROM WeatherRecord WHERE x = :x AND y = :y ORDER BY release DESC")
    fun observe(x: Int, y: Int): Flow<List<WeatherRecord>>
    @Query("SELECT * FROM WeatherRecord WHERE x = :x AND y = :y ORDER BY release DESC")
    suspend fun records(x: Int, y: Int): List<WeatherRecord>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun put(record: WeatherRecord)
    @Query("DELETE FROM WeatherRecord WHERE fetchedAt < :before") suspend fun prune(before: Long)
    @Query("SELECT * FROM last_location WHERE id = 1") suspend fun location(): LocationRecord?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putLocation(location: LocationRecord)
}
@Database(entities = [WeatherRecord::class, LocationRecord::class], version = 1, exportSchema = true)
abstract class WeatherDatabase : RoomDatabase() { abstract fun weather(): WeatherDao }
