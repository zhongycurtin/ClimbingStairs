package com.stairstep.app.data.export

import android.content.Context
import android.net.Uri
import com.stairstep.app.data.db.ClimbDao
import com.stairstep.app.data.db.WeightDao
import com.stairstep.app.data.model.ClimbTrip
import com.stairstep.app.data.model.WeightRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Locale

data class ImportResult(
    val success: Boolean,
    val tripsImported: Int = 0,
    val weightsImported: Int = 0,
    val message: String = ""
)

object DataImporter {

    suspend fun importFromCsv(
        context: Context,
        uri: Uri,
        climbDao: ClimbDao,
        weightDao: WeightDao
    ): ImportResult = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext ImportResult(false, message = "无法打开选择的文件")

            val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
            val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

            var section = 0 // 1: trips, 2: weights
            var tripsCount = 0
            var weightsCount = 0

            val existingTrips = climbDao.getAllTrips().associateBy { it.startTimeMillis }
            val existingWeights = weightDao.getAllWeights().associateBy { it.date }

            reader.useLines { lines ->
                for (rawLine in lines) {
                    val line = rawLine.trim()
                    if (line.isEmpty()) continue

                    if (line.contains("爬楼运动记录")) {
                        section = 1
                        continue
                    } else if (line.contains("体重追踪记录")) {
                        section = 2
                        continue
                    }

                    // 跳过表头
                    if (line.startsWith("ID,") || line.startsWith("ID，")) continue

                    val tokens = parseCsvLine(line)
                    if (section == 1 && tokens.size >= 8) {
                        try {
                            // 格式: ID,日期,开始时间,结束时间,耗时(秒),爬升楼层,估算卡路里(kcal),标签,是否补录
                            val date = tokens[1]
                            val startTimeStr = tokens[2]
                            val endTimeStr = tokens[3]
                            val duration = tokens[4].toLongOrNull() ?: 60L
                            val floors = tokens[5].toIntOrNull() ?: 18
                            val calories = tokens[6].toDoubleOrNull() ?: 20.0
                            val tag = tokens[7]
                            val isBackfill = tokens.getOrNull(8)?.contains("是") == true

                            val startTime = timeFormat.parse(startTimeStr)?.time
                                ?: System.currentTimeMillis()
                            val endTime = timeFormat.parse(endTimeStr)?.time
                                ?: (startTime + duration * 1000)

                            if (!existingTrips.containsKey(startTime)) {
                                climbDao.insertTrip(
                                    ClimbTrip(
                                        date = date,
                                        startTimeMillis = startTime,
                                        endTimeMillis = endTime,
                                        durationSeconds = duration,
                                        floors = floors,
                                        estimatedCalories = calories,
                                        tag = tag,
                                        isBackfill = isBackfill
                                    )
                                )
                                tripsCount++
                            }
                        } catch (e: Exception) {
                            // 跳过畸形行
                        }
                    } else if (section == 2 && tokens.size >= 4) {
                        try {
                            // 格式: ID,记录日期,记录时间,体重(kg)
                            val date = tokens[1]
                            val recordTimeStr = tokens[2]
                            val weightKg = tokens[3].toDoubleOrNull() ?: 65.0
                            val timestamp = timeFormat.parse(recordTimeStr)?.time
                                ?: System.currentTimeMillis()

                            if (!existingWeights.containsKey(date)) {
                                weightDao.insertWeight(
                                    WeightRecord(
                                        date = date,
                                        weightKg = weightKg,
                                        timestampMillis = timestamp
                                    )
                                )
                                weightsCount++
                            }
                        } catch (e: Exception) {
                            // 跳过畸形行
                        }
                    }
                }
            }

            ImportResult(
                success = true,
                tripsImported = tripsCount,
                weightsImported = weightsCount,
                message = "成功恢复 $tripsCount 条爬楼记录与 $weightsCount 条体重记录"
            )
        } catch (e: Exception) {
            e.printStackTrace()
            ImportResult(false, message = "导入解析失败: ${e.localizedMessage}")
        }
    }

    private fun parseCsvLine(line: String): List<String> {
        val tokens = mutableListOf<String>()
        val sb = java.lang.StringBuilder()
        var inQuotes = false

        for (ch in line) {
            when {
                ch == '\"' -> inQuotes = !inQuotes
                ch == ',' && !inQuotes -> {
                    tokens.add(sb.toString().trim())
                    sb.clear()
                }
                else -> sb.append(ch)
            }
        }
        tokens.add(sb.toString().trim())
        return tokens
    }
}
