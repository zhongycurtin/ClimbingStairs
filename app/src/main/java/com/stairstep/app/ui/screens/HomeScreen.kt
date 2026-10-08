package com.stairstep.app.ui.screens

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.EditCalendar
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Height
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.MonitorWeight
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stairstep.app.ui.components.BackfillDialog
import com.stairstep.app.ui.components.BigClimbButton
import com.stairstep.app.ui.components.FloorSettingDialog
import com.stairstep.app.ui.components.StatCard
import com.stairstep.app.ui.components.TripTimelineItem
import com.stairstep.app.ui.components.WeightInputDialog
import com.stairstep.app.ui.theme.AmberAccent
import com.stairstep.app.ui.theme.MintEmerald
import com.stairstep.app.ui.viewmodel.ClimbViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    viewModel: ClimbViewModel,
    modifier: Modifier = Modifier
) {
    val todayTrips by viewModel.todayTrips.collectAsState()
    val floorsPerLap by viewModel.floorsPerLap.collectAsState()
    val defaultWeight by viewModel.defaultWeight.collectAsState()
    val latestWeightRecord by viewModel.latestWeightRecord.collectAsState()
    val isTiming by viewModel.isTiming.collectAsState()
    val elapsedSeconds by viewModel.elapsedSeconds.collectAsState()

    var showFloorDialog by remember { mutableStateOf(false) }
    var showWeightDialog by remember { mutableStateOf(false) }
    var showBackfillDialog by remember { mutableStateOf(false) }

    // 计算今日关键指标
    val todayLaps = todayTrips.size
    val todayTotalFloors = todayTrips.sumOf { it.floors }
    val todayTotalSeconds = todayTrips.sumOf { it.durationSeconds }
    val todayAvgSeconds = if (todayLaps > 0) todayTotalSeconds / todayLaps else 0L
    val todayCalories = todayTrips.sumOf { it.estimatedCalories }

    val formattedTotalTime = remember(todayTotalSeconds) {
        val m = todayTotalSeconds / 60
        val s = todayTotalSeconds % 60
        if (m > 0) "${m}分${s}秒" else "${s}秒"
    }

    val formattedAvgTime = remember(todayAvgSeconds) {
        val m = todayAvgSeconds / 60
        val s = todayAvgSeconds % 60
        if (m > 0) "${m}分${s}秒" else "${s}秒"
    }

    val todayDisplayDate = remember {
        SimpleDateFormat("M月d日 EEEE", Locale.CHINESE).format(Date())
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 100.dp)
    ) {
        // 顶部品牌与问候栏
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = todayDisplayDate,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "步步登峰",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // 补录按钮
                    IconButton(
                        onClick = { showBackfillDialog = true },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.EditCalendar,
                            contentDescription = "漏记补录",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // 设置单趟楼层按钮
                    IconButton(
                        onClick = { showFloorDialog = true },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = "单趟楼层设置",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))
        }

        // 体重状态快捷条
        item {
            val currentWeightVal = latestWeightRecord?.weightKg ?: defaultWeight
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MintEmerald.copy(alpha = 0.08f))
                    .border(1.dp, MintEmerald.copy(alpha = 0.2f), RoundedCornerShape(14.dp))
                    .clickable { showWeightDialog = true }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.MonitorWeight,
                        contentDescription = null,
                        tint = MintEmerald,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "当前体重: ${String.format(Locale.US, "%.1f", currentWeightVal)} kg",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = "点击更新 >",
                    style = MaterialTheme.typography.labelSmall,
                    color = MintEmerald,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Bento 便当盒核心数据面板
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = "今日爬升",
                    value = "$todayLaps",
                    unit = "趟",
                    subtext = "单趟 $floorsPerLap 层 (累计 $todayTotalFloors 层)",
                    icon = Icons.Rounded.FitnessCenter,
                    iconTint = MintEmerald,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "预估消耗",
                    value = String.format(Locale.US, "%.0f", todayCalories),
                    unit = "kcal",
                    subtext = "垂直提升约 ${todayTotalFloors * 3} 米",
                    icon = Icons.Rounded.LocalFireDepartment,
                    iconTint = AmberAccent,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = "总耗时",
                    value = formattedTotalTime,
                    icon = Icons.Rounded.Schedule,
                    iconTint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "平均每趟",
                    value = if (todayLaps > 0) formattedAvgTime else "--",
                    icon = Icons.Rounded.Speed,
                    iconTint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))
        }

        // 核心交互大圆钮区域 (开始计时 / 完成+1)
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                BigClimbButton(
                    isTiming = isTiming,
                    elapsedSeconds = elapsedSeconds,
                    onStartClick = { viewModel.startClimbSession() },
                    onFinishClick = { viewModel.finishClimbSession() }
                )

                Spacer(modifier = Modifier.height(14.dp))

                // 快捷摸鱼操作按钮
                if (!isTiming) {
                    OutlinedButton(
                        onClick = { viewModel.quickAddTrip("摸鱼爬") },
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.height(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "摸鱼爬快捷+1趟 (无需计时)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    OutlinedButton(
                        onClick = { viewModel.cancelClimbSession() },
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text(
                            text = "放弃本次计时",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }

        // 今日单趟时间轴明细
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "今日明细流水 (${todayTrips.size}趟)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        if (todayTrips.isEmpty()) {
            item {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp)
                ) {
                    Text(
                        text = "今日尚未开爬，轻触大圆钮开启第一趟吧！",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                    )
                }
            }
        } else {
            itemsIndexed(todayTrips, key = { _, item -> item.id }) { index, trip ->
                // 显示流水序数 (从第1趟到第N趟)
                val tripNumber = todayTrips.size - index
                TripTimelineItem(
                    trip = trip,
                    tripIndex = tripNumber,
                    onDeleteClick = { viewModel.deleteTrip(trip) },
                    modifier = Modifier.padding(bottom = 10.dp)
                )
            }
        }
    }

    // 弹窗处理
    if (showFloorDialog) {
        FloorSettingDialog(
            currentFloors = floorsPerLap,
            onDismiss = { showFloorDialog = false },
            onConfirm = { newFloors ->
                viewModel.updateFloorsPerLap(newFloors)
                showFloorDialog = false
            }
        )
    }

    if (showWeightDialog) {
        val currentW = latestWeightRecord?.weightKg ?: defaultWeight
        WeightInputDialog(
            currentWeight = currentW,
            onDismiss = { showWeightDialog = false },
            onConfirm = { newWeight ->
                viewModel.recordWeight(newWeight)
                showWeightDialog = false
            }
        )
    }

    if (showBackfillDialog) {
        val currentW = latestWeightRecord?.weightKg ?: defaultWeight
        BackfillDialog(
            defaultWeight = currentW,
            onDismiss = { showBackfillDialog = false },
            onConfirm = { date, floors, weight ->
                viewModel.backfillRecord(date, floors, weight)
                showBackfillDialog = false
            }
        )
    }
}
