package com.stairstep.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 体重记录实体
 */
@Entity(tableName = "weight_records")
data class WeightRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    /**
     * 日期字符串，格式: "yyyy-MM-dd"
     */
    val date: String,
    /**
     * 体重 (公斤 kg)
     */
    val weightKg: Double,
    /**
     * 记录时间戳 (毫秒)
     */
    val timestampMillis: Long = System.currentTimeMillis()
)
