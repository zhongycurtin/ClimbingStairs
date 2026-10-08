package com.stairstep.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Terrain
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.TrendingDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stairstep.app.ui.components.ActivityBarChart
import com.stairstep.app.ui.components.StatCard
import com.stairstep.app.ui.components.WeightLineChart
import com.stairstep.app.ui.theme.AmberAccent
import com.stairstep.app.ui.theme.MintEmerald
import com.stairstep.app.ui.viewmodel.StatsViewModel
import java.util.Locale

@Composable
fun StatsScreen(
    viewModel: StatsViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val allTrips by viewModel.allTrips.collectAsState()
    val allWeights by viewModel.allWeights.collectAsState()
    val selectedDaysRange by viewModel.selectedDaysRange.collectAsState()

    val dailySummaries = remember(allTrips, selectedDaysRange) {
        viewModel.getDailyActivitySummaries(allTrips, selectedDaysRange)
    }

    val totalFloors = remember(allTrips) { allTrips.sumOf { it.floors } }
    val totalCalories = remember(allTrips) { allTrips.sumOf { it.estimatedCalories } }
    val totalMeters = totalFloors * 3.0
    val estimatedFatLossGrams = (totalCalories / 7.7) // 每燃烧 7.7 kcal 约消耗 1g 脂肪

    val milestones = remember(totalFloors) {
        viewModel.calculateMilestones(totalFloors)
    }

    // 体重变化计算
    val weightDelta = remember(allWeights) {
        if (allWeights.size >= 2) {
            allWeights.last().weightKg - allWeights.first().weightKg
        } else 0.0
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 100.dp)
    ) {
        // 顶部标题与导出按钮
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "数据沉淀与进阶",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "趋势与成就",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // 导出 CSV 按钮
                Button(
                    onClick = {
                        val success = viewModel.exportCsv(context)
                        if (!success) {
                            Toast.makeText(context, "暂无数据或导出失败", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                ) {
                    Icon(
                        imageVector = Icons.Rounded.FileDownload,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MintEmerald
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "导出CSV",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))
        }

        // 累计成就统计卡片
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = "累计爬升高度",
                    value = String.format(Locale.US, "%.0f", totalMeters),
                    unit = "米",
                    subtext = "相当于 $totalFloors 层楼",
                    icon = Icons.Rounded.Terrain,
                    iconTint = MintEmerald,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "预计燃脂",
                    value = String.format(Locale.US, "%.0f", estimatedFatLossGrams),
                    unit = "克",
                    subtext = "累计耗能 ${String.format(Locale.US, "%.0f", totalCalories)} kcal",
                    icon = Icons.Rounded.LocalFireDepartment,
                    iconTint = AmberAccent,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
        }

        // 周期切换器 (近7天 / 近30天)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "每日运动量曲线",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .padding(3.dp)
                ) {
                    listOf(7 to "近7天", 30 to "近30天").forEach { (days, label) ->
                        val isSelected = selectedDaysRange == days
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) MintEmerald else Color.Transparent)
                                .clickable { viewModel.setDaysRange(days) }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }

        // 运动量柱状图组件
        item {
            ActivityBarChart(summaries = dailySummaries)
            Spacer(modifier = Modifier.height(20.dp))
        }

        // 体重趋势曲线
        item {
            WeightLineChart(weights = allWeights)
            Spacer(modifier = Modifier.height(24.dp))
        }

        // 地标攀登里程碑展示
        item {
            Text(
                text = "攀登里程碑",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        items(milestones.size) { index ->
            val milestone = milestones[index]
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (milestone.isCompleted) Icons.Rounded.CheckCircle else Icons.Rounded.Terrain,
                                contentDescription = null,
                                tint = if (milestone.isCompleted) MintEmerald else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = milestone.landmarkName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = "${milestone.landmarkHeightMeters} 米",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LinearProgressIndicator(
                        progress = milestone.progressPercent,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = if (milestone.isCompleted) MintEmerald else MintEmerald.copy(alpha = 0.6f),
                        trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = if (milestone.isCompleted) "已成功征服！" else "已登顶 ${(milestone.progressPercent * 100).toInt()}%",
                        fontSize = 10.sp,
                        color = if (milestone.isCompleted) MintEmerald else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                    )
                }
            }
        }
    }
}
