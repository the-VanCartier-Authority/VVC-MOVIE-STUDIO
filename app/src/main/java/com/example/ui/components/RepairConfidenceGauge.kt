package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.RedAlert
import com.example.ui.theme.RetroConsoleFontFamily

@Composable
fun RepairConfidenceGauge(
    percent: Int, // 0 to 100
    modifier: Modifier = Modifier,
    sizeDp: Dp = 120.dp,
    label: String = "RECUPERABILIDAD"
) {
    val animatedPercent by animateFloatAsState(
        targetValue = percent.toFloat(),
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "gauge_anim"
    )

    val gaugeColor = when {
        percent >= 80 -> NeonEmerald
        percent >= 50 -> Color(0xFFFFB703)
        else -> RedAlert
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(sizeDp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(sizeDp)) {
                val strokeW = 10.dp.toPx()
                val sweep = (animatedPercent / 100f) * 260f
                val start = 140f

                // Track
                drawArc(
                    color = ObsidianBorder,
                    startAngle = start,
                    sweepAngle = 260f,
                    useCenter = false,
                    style = Stroke(width = strokeW, cap = StrokeCap.Round),
                    topLeft = Offset(strokeW / 2, strokeW / 2),
                    size = Size(size.width - strokeW, size.height - strokeW)
                )

                // Fill Arc
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(NeonMagenta, gaugeColor)
                    ),
                    startAngle = start,
                    sweepAngle = sweep,
                    useCenter = false,
                    style = Stroke(width = strokeW, cap = StrokeCap.Round),
                    topLeft = Offset(strokeW / 2, strokeW / 2),
                    size = Size(size.width - strokeW, size.height - strokeW)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${percent}%",
                    fontFamily = RetroConsoleFontFamily,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = gaugeColor
                )
                Text(
                    text = if (percent >= 80) "VIABLE" else if (percent >= 50) "PARCIAL" else "CRÍTICO",
                    fontFamily = RetroConsoleFontFamily,
                    fontSize = 9.sp,
                    color = Color(0xFFA0AABF)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontFamily = RetroConsoleFontFamily,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFFA0AABF),
            letterSpacing = 0.5.sp
        )
    }
}
