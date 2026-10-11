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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonEmeraldDim
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.NeonMagentaDim
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.RetroConsoleFontFamily
import com.example.ui.theme.TextPrimary

@Composable
fun CyberBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "cyber_grid")
    val gridOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 60f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "grid_anim"
    )

    val scanLineY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scan_anim"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBlack)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val gridSize = 60f

            // Cyber grid lines in faint obsidian/cyan
            val gridColor = Color(0x0C00FF87)
            var x = (gridOffset % gridSize)
            while (x < canvasWidth) {
                drawLine(
                    color = gridColor,
                    start = Offset(x, 0f),
                    end = Offset(x, canvasHeight),
                    strokeWidth = 1f
                )
                x += gridSize
            }

            var y = (gridOffset % gridSize)
            while (y < canvasHeight) {
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(canvasWidth, y),
                    strokeWidth = 1f
                )
                y += gridSize
            }

            // Laser scanning horizontal beam with neon magenta/emerald glow
            val scanY = scanLineY * canvasHeight
            drawLine(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        NeonMagenta.copy(alpha = 0.35f),
                        NeonEmerald.copy(alpha = 0.5f),
                        NeonMagenta.copy(alpha = 0.35f),
                        Color.Transparent
                    )
                ),
                start = Offset(0f, scanY),
                end = Offset(canvasWidth, scanY),
                strokeWidth = 2f
            )
        }

        content()
    }
}

@Composable
fun CyberHeader(
    title: String = "VVC MOVIE STUDIO",
    subtitle: String = "IDEAS / GUION / EDICIÓN / CINE // REPARACIÓN & CONVERSIÓN",
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Official VVC Branding Emblem
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(ObsidianBlack)
                        .border(1.5.dp, NeonEmerald, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.vvc_official_icon_1791510629627),
                        contentDescription = "VVC Logo",
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "VVC",
                            fontFamily = RetroConsoleFontFamily,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonMagenta,
                            letterSpacing = 1.2.sp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = ">_",
                            fontFamily = RetroConsoleFontFamily,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonEmerald
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "MOVIE STUDIO",
                            fontFamily = RetroConsoleFontFamily,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            letterSpacing = 1.sp
                        )
                    }
                    Text(
                        text = "TUS HISTORIAS. EN OTRO NIVEL.",
                        fontFamily = RetroConsoleFontFamily,
                        fontSize = 9.sp,
                        color = Color(0xFFD4AF37), // Oro cepillado
                        letterSpacing = 0.5.sp
                    )
                }
            }

            NeonBadge(
                text = "4K // 60fps",
                textColor = NeonEmerald,
                borderColor = NeonEmerald.copy(alpha = 0.8f),
                backgroundColor = NeonEmeraldDim.copy(alpha = 0.3f)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Branding Sub-strip with circuit accents
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFF0F131C))
                .border(1.dp, ObsidianBorder, RoundedCornerShape(4.dp))
                .padding(horizontal = 8.dp, vertical = 3.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = subtitle,
                fontFamily = RetroConsoleFontFamily,
                fontSize = 10.sp,
                color = Color(0xFFA0AABF),
                letterSpacing = 0.6.sp,
                maxLines = 1
            )
            Text(
                text = "IPC 360",
                fontFamily = RetroConsoleFontFamily,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = NeonMagenta
            )
        }
    }
}

@Composable
fun NeonCard(
    modifier: Modifier = Modifier,
    borderColor: Color = ObsidianBorder,
    glowColor: Color = Color.Transparent,
    cornerRadius: Dp = 12.dp,
    content: @Composable BoxScope.() -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        borderColor,
                        if (glowColor != Color.Transparent) glowColor else borderColor
                    )
                ),
                shape = RoundedCornerShape(cornerRadius)
            ),
        shape = RoundedCornerShape(cornerRadius),
        colors = CardDefaults.cardColors(containerColor = ObsidianCard)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            content()
        }
    }
}

@Composable
fun NeonBadge(
    text: String,
    modifier: Modifier = Modifier,
    textColor: Color = NeonMagenta,
    borderColor: Color = NeonMagenta.copy(alpha = 0.6f),
    backgroundColor: Color = ObsidianBlack
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontFamily = RetroConsoleFontFamily,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = textColor,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
fun NeonButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    isPrimary: Boolean = true,
    enabled: Boolean = true,
    testTag: String = "neon_button"
) {
    val bgBrush = if (isPrimary) {
        Brush.horizontalGradient(
            colors = listOf(
                NeonMagenta,
                NeonMagenta.copy(alpha = 0.85f)
            )
        )
    } else {
        Brush.horizontalGradient(
            colors = listOf(
                NeonEmerald,
                NeonEmerald.copy(alpha = 0.85f)
            )
        )
    }

    val contentColor = if (isPrimary) ObsidianBlack else ObsidianBlack
    val borderColor = if (isPrimary) NeonMagenta else NeonEmerald

    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .testTag(testTag)
            .height(48.dp)
            .fillMaxWidth()
            .border(1.dp, if (enabled) borderColor else Color.Gray, RoundedCornerShape(8.dp)),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isPrimary) NeonMagenta else NeonEmerald,
            contentColor = contentColor,
            disabledContainerColor = Color(0xFF222630),
            disabledContentColor = Color(0xFF555E70)
        ),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = contentColor
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                fontFamily = RetroConsoleFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                letterSpacing = 1.sp,
                color = contentColor
            )
        }
    }
}
