package com.stairstep.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stairstep.app.ui.theme.MintEmerald

@Composable
fun BigClimbButton(
    isTiming: Boolean,
    elapsedSeconds: Long,
    onStartClick: () -> Unit,
    onFinishClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (isTiming) 1.06f else 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isTiming) 800 else 2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val interactionSource = remember { MutableInteractionSource() }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(220.dp)
    ) {
        // 呼吸背景外光晕
        Box(
            modifier = Modifier
                .size(200.dp)
                .scale(pulseScale)
                .clip(CircleShape)
                .background(
                    if (isTiming) {
                        MintEmerald.copy(alpha = 0.15f)
                    } else {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                    }
                )
        )

        // 核心主圆形按钮
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(175.dp)
                .shadow(
                    elevation = if (isTiming) 12.dp else 6.dp,
                    shape = CircleShape,
                    spotColor = MintEmerald.copy(alpha = 0.4f)
                )
                .clip(CircleShape)
                .background(
                    if (isTiming) {
                        Brush.linearGradient(
                            listOf(
                                Color(0xFF10B981),
                                Color(0xFF059669)
                            )
                        )
                    } else {
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.surface,
                                MaterialTheme.colorScheme.surface
                            )
                        )
                    }
                )
                .border(
                    width = if (isTiming) 2.dp else 1.5.dp,
                    color = if (isTiming) Color.White.copy(alpha = 0.6f) else MintEmerald.copy(alpha = 0.35f),
                    shape = CircleShape
                )
                .clickable(
                    interactionSource = interactionSource,
                    indication = null
                ) {
                    if (isTiming) {
                        onFinishClick()
                    } else {
                        onStartClick()
                    }
                }
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (isTiming) {
                    val minutes = elapsedSeconds / 60
                    val seconds = elapsedSeconds % 60
                    val timeString = String.format("%02d:%02d", minutes, seconds)

                    Text(
                        text = timeString,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Done,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = " 完成并+1趟",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                } else {
                    Icon(
                        imageVector = Icons.Rounded.DirectionsRun,
                        contentDescription = null,
                        tint = MintEmerald,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "开始爬楼",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "点击启动计时",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                    )
                }
            }
        }
    }
}
