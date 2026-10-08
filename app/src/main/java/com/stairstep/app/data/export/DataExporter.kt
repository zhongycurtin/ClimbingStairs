package com.stairstep.app.data.export

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.stairstep.app.data.model.ClimbTrip
import com.stairstep.app.data.model.WeightRecord
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DataExporter {

    fun exportToCsv(
        context: Context,
        trips: List<ClimbTrip>,
        weights: List<WeightRecord>
    ): Boolean {
        return try {
            val exportDir = File(context.cacheDir, "exports")
            if (!exportDir.exists()) {
                exportDir.mkdirs()
            }

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val exportFile = File(exportDir, "stairstep_export_$timestamp.csv")

            val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

            val sb = java.lang.StringBuilder()
            // 写入爬楼记录区块
            sb.append("# 步步登峰 - 爬楼运动记录\n")
            sb.append("ID,日期,开始时间,结束时间,耗时(秒),爬升楼层,估算卡路里(kcal),标签,是否补录\n")
            for (trip in trips) {
                val startStr = timeFormat.format(Date(trip.startTimeMillis))
                val endStr = timeFormat.format(Date(trip.endTimeMillis))
                sb.append("${trip.id},\"${trip.date}\",\"$startStr\",\"$endStr\",${trip.durationSeconds},${trip.floors},${String.format(Locale.US, "%.1f", trip.estimatedCalories)},\"${trip.tag}\",${if (trip.isBackfill) "是" else "否"}\n")
            }

            sb.append("\n# 步步登峰 - 体重追踪记录\n")
            sb.append("ID,记录日期,记录时间,体重(kg)\n")
            for (weight in weights) {
                val recordTimeStr = timeFormat.format(Date(weight.timestampMillis))
                sb.append("${weight.id},\"${weight.date}\",\"$recordTimeStr\",${String.format(Locale.US, "%.1f", weight.weightKg)}\n")
            }

            exportFile.writeText(sb.toString(), Charsets.UTF_8)

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                exportFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "步步登峰数据备份_$timestamp.csv")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(Intent.createChooser(shareIntent, "导出/分享运动数据").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
