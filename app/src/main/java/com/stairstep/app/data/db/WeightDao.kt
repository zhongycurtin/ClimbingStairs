package com.stairstep.app.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.stairstep.app.data.model.WeightRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface WeightDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeight(record: WeightRecord): Long

    @Delete
    suspend fun deleteWeight(record: WeightRecord)

    @Query("SELECT * FROM weight_records ORDER BY date DESC, timestampMillis DESC LIMIT 1")
    fun getLatestWeightFlow(): Flow<WeightRecord?>

    @Query("SELECT * FROM weight_records ORDER BY date DESC, timestampMillis DESC LIMIT 1")
    suspend fun getLatestWeight(): WeightRecord?

    @Query("SELECT * FROM weight_records WHERE date = :date ORDER BY timestampMillis DESC LIMIT 1")
    suspend fun getWeightByDate(date: String): WeightRecord?

    @Query("SELECT * FROM weight_records ORDER BY date ASC, timestampMillis ASC")
    fun getAllWeightsFlow(): Flow<List<WeightRecord>>

    @Query("SELECT * FROM weight_records ORDER BY date ASC, timestampMillis ASC")
    suspend fun getAllWeights(): List<WeightRecord>

    @Query("SELECT * FROM weight_records WHERE date >= :startDate ORDER BY date ASC, timestampMillis ASC")
    fun getWeightsSinceDateFlow(startDate: String): Flow<List<WeightRecord>>
}
