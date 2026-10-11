package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonEmeraldDim
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.RetroConsoleFontFamily
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun RadarScannerHUD(
    modifier: Modifier = Modifier,
    sizeDp: Dp = 150.dp,
    isScanning: Boolean = true,
    statusText: String = "ESCANEANDO STREAM H.264"
) {
    val infiniteTransition = rememberInfiniteTransition(label = "radar")
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweep_angle"
    )

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(sizeDp)
                .clip(CircleShape)
                .background(ObsidianBlack)
                .border(1.5.dp, NeonEmerald.copy(alpha = 0.7f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(sizeDp)) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val radius = size.width / 2f - 4f

                // Concentric circles
                drawCircle(
                    color = NeonEmeraldDim.copy(alpha = 0.5f),
                    radius = radius * 0.33f,
                    center = center,
                    style = Stroke(width = 1f)
                )
                drawCircle(
                    color = NeonEmeraldDim.copy(alpha = 0.5f),
                    radius = radius * 0.66f,
                    center = center,
                    style = Stroke(width = 1f)
                )
                drawCircle(
                    color = NeonEmerald.copy(alpha = 0.3f),
                    radius = radius,
                    center = center,
                    style = Stroke(width = 1.5f)
                )

                // Crosshairs
                drawLine(
                    color = NeonEmeraldDim.copy(alpha = 0.6f),
                    start = Offset(center.x, 4f),
                    end = Offset(center.x, size.height - 4f),
                    strokeWidth = 1f
                )
                drawLine(
                    color = NeonEmeraldDim.copy(alpha = 0.6f),
                    start = Offset(4f, center.y),
                    end = Offset(size.width - 4f, center.y),
                    strokeWidth = 1f
                )

                if (isScanning) {
                    // Sweeping radar beam
                    val rad = Math.toRadians(sweepAngle.toDouble())
                    val endX = center.x + (radius * cos(rad)).toFloat()
                    val endY = center.y + (radius * sin(rad)).toFloat()

                    drawLine(
                        brush = Brush.linearGradient(
                            colors = listOf(NeonEmerald, Color.Transparent),
                            start = center,
                            end = Offset(endX, endY)
                        ),
                        start = center,
                        end = Offset(endX, endY),
                        strokeWidth = 2.5f
                    )

                    // Arc trailing glow
                    drawArc(
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                Color.Transparent,
                                NeonEmerald.copy(alpha = 0.25f),
                                NeonEmerald.copy(alpha = 0.6f)
                            )
                        ),
                        startAngle = sweepAngle - 45f,
                        sweepAngle = 45f,
                        useCenter = true,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
                    )
                }

                // Center core dot
                drawCircle(
                    color = NeonMagenta,
                    radius = 4f * pulseScale,
                    center = center
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = statusText,
            fontFamily = RetroConsoleFontFamily,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = NeonEmerald,
            letterSpacing = 1.sp
        )
    }
}
