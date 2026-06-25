package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.SearchHistoryEntity
import com.example.data.model.CachedWeatherEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WeatherDao {
    @Query("SELECT * FROM search_history ORDER BY timestamp DESC LIMIT 10")
    fun getSearchHistory(): Flow<List<SearchHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSearchHistory(searchHistory: SearchHistoryEntity)

    @Query("DELETE FROM search_history WHERE cityName = :cityName")
    suspend fun deleteSearchHistory(cityName: String)

    @Query("SELECT * FROM cached_weather WHERE query = :query LIMIT 1")
    suspend fun getCachedWeather(query: String): CachedWeatherEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCachedWeather(cachedWeather: CachedWeatherEntity)
}
