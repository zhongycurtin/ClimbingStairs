package com.stairstep.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 爬楼单趟/批次记录实体
 */
@Entity(tableName = "climb_trips")
data class ClimbTrip(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    /**
     * 日期字符串，格式: "yyyy-MM-dd"
     */
    val date: String,
    /**
     * 开始时间戳 (毫秒)
     */
    val startTimeMillis: Long,
    /**
     * 结束时间戳 (毫秒)
     */
    val endTimeMillis: Long,
    /**
     * 本趟实际耗时 (秒)
     */
    val durationSeconds: Long,
    /**
     * 本趟爬升楼层数
     */
    val floors: Int,
    /**
     * 预估消耗卡路里 (千卡 kcal)
     */
    val estimatedCalories: Double,
    /**
     * 记录场景标签，如: "摸鱼爬", "晚间燃脂", "快速打卡", "漏记补录"
     */
    val tag: String = "日常爬楼",
    /**
     * 是否为历史补录记录
     */
    val isBackfill: Boolean = false
)
