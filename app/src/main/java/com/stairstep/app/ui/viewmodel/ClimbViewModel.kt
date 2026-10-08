package com.stairstep.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.stairstep.app.data.db.AppDatabase
import com.stairstep.app.data.model.ClimbTrip
import com.stairstep.app.data.model.WeightRecord
import com.stairstep.app.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ClimbViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val climbDao = db.climbDao()
    private val weightDao = db.weightDao()
    private val prefs = UserPreferencesRepository(application)

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val todayDate: String = dateFormat.format(Date())

    // 用户设置流
    val floorsPerLap: StateFlow<Int> = prefs.floorsPerLapFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 18)

    val defaultWeight: StateFlow<Double> = prefs.defaultWeightFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 65.0)

    val dailyGoalLaps: StateFlow<Int> = prefs.dailyGoalLapsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 5)

    // 今日爬楼流水列表
    val todayTrips: StateFlow<List<ClimbTrip>> = climbDao.getTripsByDateFlow(todayDate)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 最新体重记录
    val latestWeightRecord: StateFlow<WeightRecord?> = weightDao.getLatestWeightFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // 实时计时器状态
    private val _isTiming = MutableStateFlow(false)
    val isTiming: StateFlow<Boolean> = _isTiming.asStateFlow()

    private val _sessionStartTime = MutableStateFlow(0L)
    val sessionStartTime: StateFlow<Long> = _sessionStartTime.asStateFlow()

    private val _elapsedSeconds = MutableStateFlow(0L)
    val elapsedSeconds: StateFlow<Long> = _elapsedSeconds.asStateFlow()

    private var timerJob: Job? = null

    /**
     * 开启爬楼计时
     */
    fun startClimbSession() {
        if (_isTiming.value) return
        val now = System.currentTimeMillis()
        _sessionStartTime.value = now
        _elapsedSeconds.value = 0L
        _isTiming.value = true

        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_isTiming.value) {
                delay(1000L)
                _elapsedSeconds.value = (System.currentTimeMillis() - _sessionStartTime.value) / 1000
            }
        }
    }

    /**
     * 完成当前趟并记录 (+1)
     */
    fun finishClimbSession(tag: String? = null) {
        if (!_isTiming.value) return
        val endTime = System.currentTimeMillis()
        val startTime = _sessionStartTime.value
        val duration = maxOf(1L, (endTime - startTime) / 1000)
        val floors = floorsPerLap.value
        val weight = latestWeightRecord.value?.weightKg ?: defaultWeight.value

        // 计算卡路里：以爬楼 MET=8.8 计算，每小时耗能 = 8.8 * weight * (duration/3600)
        // 阶梯做功最低保底每层约 0.15 * weight / 10
        val calByTime = (8.8 * weight * (duration / 3600.0))
        val calByFloors = (floors * 0.14 * (weight / 60.0))
        val calories = maxOf(calByTime, calByFloors)

        val autoTag = tag ?: determineAutoTag(startTime)

        viewModelScope.launch {
            climbDao.insertTrip(
                ClimbTrip(
                    date = todayDate,
                    startTimeMillis = startTime,
                    endTimeMillis = endTime,
                    durationSeconds = duration,
                    floors = floors,
                    estimatedCalories = calories,
                    tag = autoTag,
                    isBackfill = false
                )
            )
            resetTimer()
        }
    }

    /**
     * 取消当前计时
     */
    fun cancelClimbSession() {
        resetTimer()
    }

    private fun resetTimer() {
        _isTiming.value = false
        _sessionStartTime.value = 0L
        _elapsedSeconds.value = 0L
        timerJob?.cancel()
        timerJob = null
    }

    /**
     * 快捷 +1 趟 (无需长按计时，智能沿用历史均速或默认配速)
     */
    fun quickAddTrip(customTag: String? = null) {
        val now = System.currentTimeMillis()
        val currentTrips = todayTrips.value
        val avgDuration = if (currentTrips.isNotEmpty()) {
            currentTrips.map { it.durationSeconds }.average().toLong()
        } else {
            // 默认每层约 6~8 秒
            (floorsPerLap.value * 7L).coerceAtLeast(30L)
        }
        val startTime = now - (avgDuration * 1000)
        val floors = floorsPerLap.value
        val weight = latestWeightRecord.value?.weightKg ?: defaultWeight.value
        val calories = maxOf(8.8 * weight * (avgDuration / 3600.0), floors * 0.14 * (weight / 60.0))
        val tag = customTag ?: determineAutoTag(now)

        viewModelScope.launch {
            climbDao.insertTrip(
                ClimbTrip(
                    date = todayDate,
                    startTimeMillis = startTime,
                    endTimeMillis = now,
                    durationSeconds = avgDuration,
                    floors = floors,
                    estimatedCalories = calories,
                    tag = tag,
                    isBackfill = false
                )
            )
        }
    }

    /**
     * 漏记补录功能 (支持指定任意历史日期、输入爬升楼层数与当日体重)
     */
    fun backfillRecord(date: String, floors: Int, weightKg: Double?) {
        viewModelScope.launch {
            val weight = weightKg ?: latestWeightRecord.value?.weightKg ?: defaultWeight.value
            // 写入该日体重
            if (weightKg != null && weightKg > 0) {
                weightDao.insertWeight(
                    WeightRecord(
                        date = date,
                        weightKg = weightKg,
                        timestampMillis = parseDateToMillis(date)
                    )
                )
            }
            // 写入该日爬楼记录
            val estimatedDuration = (floors * 7L).coerceAtLeast(60L)
            val startTime = parseDateToMillis(date) + (18 * 3600 * 1000) // 默认补录在傍晚 18:00
            val endTime = startTime + (estimatedDuration * 1000)
            val calories = maxOf(8.8 * weight * (estimatedDuration / 3600.0), floors * 0.14 * (weight / 60.0))

            climbDao.insertTrip(
                ClimbTrip(
                    date = date,
                    startTimeMillis = startTime,
                    endTimeMillis = endTime,
                    durationSeconds = estimatedDuration,
                    floors = floors,
                    estimatedCalories = calories,
                    tag = "历史补录",
                    isBackfill = true
                )
            )
        }
    }

    /**
     * 记录/更新当前体重
     */
    fun recordWeight(weight: Double, date: String = todayDate) {
        viewModelScope.launch {
            weightDao.insertWeight(
                WeightRecord(
                    date = date,
                    weightKg = weight,
                    timestampMillis = System.currentTimeMillis()
                )
            )
            prefs.updateDefaultWeight(weight)
        }
    }

    fun updateFloorsPerLap(floors: Int) {
        viewModelScope.launch {
            prefs.updateFloorsPerLap(floors)
        }
    }

    fun deleteTrip(trip: ClimbTrip) {
        viewModelScope.launch {
            climbDao.deleteTrip(trip)
        }
    }

    private fun determineAutoTag(timeMillis: Long): String {
        val hour = java.util.Calendar.getInstance().apply {
            timeInMillis = timeMillis
        }.get(java.util.Calendar.HOUR_OF_DAY)

        return when (hour) {
            in 9..17 -> "白天摸鱼"
            in 18..23 -> "晚间燃脂"
            in 5..8 -> "晨间唤醒"
            else -> "深夜攀爬"
        }
    }

    private fun parseDateToMillis(dateStr: String): Long {
        return try {
            dateFormat.parse(dateStr)?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }
}
