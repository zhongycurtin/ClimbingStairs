package com.stairstep.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stairstep.app.data.model.WeightRecord
import com.stairstep.app.ui.theme.MintEmerald
import com.stairstep.app.ui.viewmodel.DailyActivitySummary

/**
 * 每日运动量柱状图
 */
@Composable
fun ActivityBarChart(
    summaries: List<DailyActivitySummary>,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "每日爬升楼层趋势",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                val maxFloors = summaries.maxOfOrNull { it.totalFloors } ?: 0
                Text(
                    text = "峰值 $maxFloors 层",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            val maxVal = maxOf(1, summaries.maxOfOrNull { it.totalFloors } ?: 1)

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
            ) {
                val canvasWidth = size.width
                val canvasHeight = size.height - 20.dp.toPx()
                val count = summaries.size
                if (count == 0) return@Canvas

                val slotWidth = canvasWidth / count
                val barWidth = (slotWidth * 0.5f).coerceAtMost(28.dp.toPx())

                summaries.forEachIndexed { index, item ->
                    val x = index * slotWidth + (slotWidth - barWidth) / 2
                    val ratio = (item.totalFloors.toFloat() / maxVal.toFloat()).coerceIn(0f, 1f)
                    val barHeight = (canvasHeight * ratio).coerceAtLeast(4.dp.toPx())
                    val y = canvasHeight - barHeight

                    val isToday = index == count - 1
                    val barColor = if (isToday) MintEmerald else MintEmerald.copy(alpha = 0.35f)

                    drawRoundRect(
                        color = barColor,
                        topLeft = Offset(x, y),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                    )
                }
            }

            // 底部日期文字
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                summaries.forEach { item ->
                    Text(
                        text = item.displayDate,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                    )
                }
            }
        }
    }
}

/**
 * 体重平滑趋势折线图
 */
@Composable
fun WeightLineChart(
    weights: List<WeightRecord>,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "体重变化曲线",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (weights.isNotEmpty()) {
                    val latest = weights.last().weightKg
                    Text(
                        text = "当前 ${String.format("%.1f", latest)} kg",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MintEmerald
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (weights.size < 2) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                ) {
                    Text(
                        text = if (weights.isEmpty()) "暂无体重记录，点击右上角快速添加" else "记录至少 2 天体重后生成平滑曲线",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                    )
                }
            } else {
                val minW = weights.minOf { it.weightKg } - 0.5
                val maxW = weights.maxOf { it.weightKg } + 0.5
                val range = maxOf(0.1, maxW - minW)

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                ) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height
                    val count = weights.size
                    val stepX = canvasWidth / (count - 1)

                    val points = weights.mapIndexed { index, record ->
                        val x = index * stepX
                        val ratio = ((record.weightKg - minW) / range).coerceIn(0.0, 1.0).toFloat()
                        val y = canvasHeight - (ratio * canvasHeight * 0.85f) - (canvasHeight * 0.08f)
                        Offset(x, y)
                    }

                    // 绘制平滑贝塞尔曲线
                    val path = Path()
                    val fillPath = Path()
                    path.moveTo(points.first().x, points.first().y)
                    fillPath.moveTo(points.first().x, canvasHeight)
                    fillPath.lineTo(points.first().x, points.first().y)

                    for (i in 0 until points.size - 1) {
                        val p0 = points[i]
                        val p1 = points[i + 1]
                        val cx = (p0.x + p1.x) / 2
                        path.cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
                        fillPath.cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
                    }

                    fillPath.lineTo(points.last().x, canvasHeight)
                    fillPath.close()

                    // 曲线渐变阴影
                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            listOf(
                                MintEmerald.copy(alpha = 0.25f),
                                Color.Transparent
                            )
                        )
                    )

                    // 曲线本体
                    drawPath(
                        path = path,
                        color = MintEmerald,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // 绘制各点实心圆
                    points.forEach { pt ->
                        drawCircle(
                            color = Color.White,
                            radius = 4.dp.toPx(),
                            center = pt
                        )
                        drawCircle(
                            color = MintEmerald,
                            radius = 2.5.dp.toPx(),
                            center = pt
                        )
                    }
                }
            }
        }
    }
}
