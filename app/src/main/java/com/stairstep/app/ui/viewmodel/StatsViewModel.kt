package com.stairstep.app.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.stairstep.app.data.db.AppDatabase
import com.stairstep.app.data.export.DataExporter
import com.stairstep.app.data.model.ClimbTrip
import com.stairstep.app.data.model.WeightRecord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class DailyActivitySummary(
    val date: String,
    val displayDate: String, // "10/08"
    val totalFloors: Int,
    val totalTrips: Int,
    val totalCalories: Double
)

data class MilestoneInfo(
    val landmarkName: String,
    val landmarkHeightMeters: Int,
    val currentMeters: Double,
    val progressPercent: Float,
    val isCompleted: Boolean
)

class StatsViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val climbDao = db.climbDao()
    private val weightDao = db.weightDao()

    val allTrips: StateFlow<List<ClimbTrip>> = climbDao.getAllTripsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allWeights: StateFlow<List<WeightRecord>> = weightDao.getAllWeightsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 统计天数过滤（7天 / 30天）
    private val _selectedDaysRange = MutableStateFlow(7)
    val selectedDaysRange: StateFlow<Int> = _selectedDaysRange.asStateFlow()

    fun setDaysRange(days: Int) {
        _selectedDaysRange.value = days
    }

    /**
     * 获取近 N 天每日运动量汇总序列
     */
    fun getDailyActivitySummaries(trips: List<ClimbTrip>, days: Int): List<DailyActivitySummary> {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val displayFormat = SimpleDateFormat("MM/dd", Locale.getDefault())

        val calendar = Calendar.getInstance()
        val result = mutableListOf<DailyActivitySummary>()

        for (i in (days - 1) downTo 0) {
            val cal = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -i)
            }
            val dateStr = dateFormat.format(cal.time)
            val displayStr = displayFormat.format(cal.time)

            val dayTrips = trips.filter { it.date == dateStr }
            val totalFloors = dayTrips.sumOf { it.floors }
            val totalCount = dayTrips.size
            val totalCalories = dayTrips.sumOf { it.estimatedCalories }

            result.add(
                DailyActivitySummary(
                    date = dateStr,
                    displayDate = displayStr,
                    totalFloors = totalFloors,
                    totalTrips = totalCount,
                    totalCalories = totalCalories
                )
            )
        }
        return result
    }

    /**
     * 计算地标成就
     */
    fun calculateMilestones(totalFloors: Int): List<MilestoneInfo> {
        val currentMeters = totalFloors * 3.0 // 默认每层楼约 3.0 米高
        val landmarks = listOf(
            "自由女神像" to 93,
            "埃菲尔铁塔" to 330,
            "东方明珠塔" to 468,
            "台北 101" to 508,
            "广州塔小蛮腰" to 600,
            "哈利法塔" to 828,
            "泰山之巅" to 1545,
            "富士山" to 3776
        )

        return landmarks.map { (name, height) ->
            val progress = (currentMeters / height.toDouble()).coerceIn(0.0, 1.0).toFloat()
            MilestoneInfo(
                landmarkName = name,
                landmarkHeightMeters = height,
                currentMeters = currentMeters,
                progressPercent = progress,
                isCompleted = currentMeters >= height
            )
        }
    }

    /**
     * 导出 CSV 文件
     */
    fun exportCsv(context: Context): Boolean {
        return DataExporter.exportToCsv(
            context = context,
            trips = allTrips.value,
            weights = allWeights.value
        )
    }

    /**
     * 导入恢复 CSV 备份文件
     */
    fun importCsv(
        context: Context,
        uri: android.net.Uri,
        onResult: (com.stairstep.app.data.export.ImportResult) -> Unit
    ) {
        viewModelScope.launch {
            val result = com.stairstep.app.data.export.DataImporter.importFromCsv(
                context = context,
                uri = uri,
                climbDao = climbDao,
                weightDao = weightDao
            )
            onResult(result)
        }
    }
}
