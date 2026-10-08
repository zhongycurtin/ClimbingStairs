package com.stairstep.app.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.stairstep.app.data.model.ClimbTrip
import kotlinx.coroutines.flow.Flow

@Dao
interface ClimbDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrip(trip: ClimbTrip): Long

    @Delete
    suspend fun deleteTrip(trip: ClimbTrip)

    @Query("SELECT * FROM climb_trips WHERE date = :date ORDER BY startTimeMillis DESC")
    fun getTripsByDateFlow(date: String): Flow<List<ClimbTrip>>

    @Query("SELECT * FROM climb_trips WHERE date = :date ORDER BY startTimeMillis DESC")
    suspend fun getTripsByDate(date: String): List<ClimbTrip>

    @Query("SELECT * FROM climb_trips ORDER BY startTimeMillis DESC")
    fun getAllTripsFlow(): Flow<List<ClimbTrip>>

    @Query("SELECT * FROM climb_trips ORDER BY startTimeMillis ASC")
    suspend fun getAllTrips(): List<ClimbTrip>

    @Query("SELECT SUM(floors) FROM climb_trips")
    fun getTotalFloorsFlow(): Flow<Int?>

    @Query("SELECT COUNT(*) FROM climb_trips")
    fun getTotalTripsCountFlow(): Flow<Int?>

    @Query("SELECT SUM(durationSeconds) FROM climb_trips WHERE date = :date")
    fun getTodayTotalDurationSecondsFlow(date: String): Flow<Long?>

    @Query("SELECT * FROM climb_trips WHERE date >= :startDate ORDER BY startTimeMillis ASC")
    fun getTripsSinceDateFlow(startDate: String): Flow<List<ClimbTrip>>
}
