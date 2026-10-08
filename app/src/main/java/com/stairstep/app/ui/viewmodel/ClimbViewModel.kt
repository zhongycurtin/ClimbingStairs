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

data class SessionSummary(
    val laps: Int,
    val totalFloors: Int,
    val totalSeconds: Long,
    val totalCalories: Double
)

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

    // 运动会话状态
    private val _isSessionActive = MutableStateFlow(false)
    val isSessionActive: StateFlow<Boolean> = _isSessionActive.asStateFlow()
    val isTiming: StateFlow<Boolean> = _isSessionActive.asStateFlow()

    private val _sessionLapCount = MutableStateFlow(0)
    val sessionLapCount: StateFlow<Int> = _sessionLapCount.asStateFlow()

    private val _sessionStartTime = MutableStateFlow(0L)
    val sessionStartTime: StateFlow<Long> = _sessionStartTime.asStateFlow()

    private val _currentLapStartTime = MutableStateFlow(0L)
    val currentLapStartTime: StateFlow<Long> = _currentLapStartTime.asStateFlow()

    private val _sessionTotalSeconds = MutableStateFlow(0L)
    val sessionTotalSeconds: StateFlow<Long> = _sessionTotalSeconds.asStateFlow()

    private val _currentLapSeconds = MutableStateFlow(0L)
    val currentLapSeconds: StateFlow<Long> = _currentLapSeconds.asStateFlow()
    val elapsedSeconds: StateFlow<Long> = _currentLapSeconds.asStateFlow()

    private val _lastCompletedLapDuration = MutableStateFlow<Long?>(null)
    val lastCompletedLapDuration: StateFlow<Long?> = _lastCompletedLapDuration.asStateFlow()

    private val _sessionCalories = MutableStateFlow(0.0)

    private val _sessionSummary = MutableStateFlow<SessionSummary?>(null)
    val sessionSummary: StateFlow<SessionSummary?> = _sessionSummary.asStateFlow()

    private var timerJob: Job? = null

    /**
     * 开启爬楼运动会话 (支持一次运动连续爬多趟)
     */
    fun startWorkoutSession() {
        if (_isSessionActive.value) return
        val now = System.currentTimeMillis()
        _sessionStartTime.value = now
        _currentLapStartTime.value = now
        _sessionTotalSeconds.value = 0L
        _currentLapSeconds.value = 0L
        _sessionLapCount.value = 0
        _sessionCalories.value = 0.0
        _lastCompletedLapDuration.value = null
        _isSessionActive.value = true

        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_isSessionActive.value) {
                delay(1000L)
                val curTime = System.currentTimeMillis()
                _sessionTotalSeconds.value = (curTime - _sessionStartTime.value) / 1000
                _currentLapSeconds.value = (curTime - _currentLapStartTime.value) / 1000
            }
        }
    }

    /**
     * 会话内完成一趟并记录 (+1)，计时器不退出，继续为下一趟计时
     */
    fun recordLapAndContinue(tag: String? = null) {
        if (!_isSessionActive.value) return
        val now = System.currentTimeMillis()
        val lapStart = _currentLapStartTime.value
        val duration = maxOf(1L, (now - lapStart) / 1000)
        val floors = floorsPerLap.value
        val weight = latestWeightRecord.value?.weightKg ?: defaultWeight.value

        // 计算卡路里：以爬楼 MET=8.8 计算
        val calByTime = (8.8 * weight * (duration / 3600.0))
        val calByFloors = (floors * 0.14 * (weight / 60.0))
        val calories = maxOf(calByTime, calByFloors)

        val autoTag = tag ?: determineAutoTag(lapStart)

        viewModelScope.launch {
            climbDao.insertTrip(
                ClimbTrip(
                    date = todayDate,
                    startTimeMillis = lapStart,
                    endTimeMillis = now,
                    durationSeconds = duration,
                    floors = floors,
                    estimatedCalories = calories,
                    tag = autoTag,
                    isBackfill = false
                )
            )
        }

        _sessionLapCount.value += 1
        _lastCompletedLapDuration.value = duration
        _sessionCalories.value += calories

        // 为下一趟重新计时
        _currentLapStartTime.value = now
        _currentLapSeconds.value = 0L
    }

    /**
     * 结束本次运动会话并生成结算报告
     */
    fun finishWorkoutSession() {
        if (!_isSessionActive.value) return
        val laps = _sessionLapCount.value
        val totalSecs = _sessionTotalSeconds.value
        val floors = laps * floorsPerLap.value
        val cals = _sessionCalories.value

        if (laps > 0) {
            _sessionSummary.value = SessionSummary(
                laps = laps,
                totalFloors = floors,
                totalSeconds = totalSecs,
                totalCalories = cals
            )
        }

        resetWorkoutSession()
    }

    fun dismissSessionSummary() {
        _sessionSummary.value = null
    }

    /**
     * 取消/放弃本次运动会话
     */
    fun cancelWorkoutSession() {
        resetWorkoutSession()
    }

    private fun resetWorkoutSession() {
        _isSessionActive.value = false
        _sessionStartTime.value = 0L
        _currentLapStartTime.value = 0L
        _sessionTotalSeconds.value = 0L
        _currentLapSeconds.value = 0L
        _sessionLapCount.value = 0
        _sessionCalories.value = 0.0
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
