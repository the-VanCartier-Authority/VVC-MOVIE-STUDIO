package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.RedAlert
import com.example.ui.theme.RetroConsoleFontFamily
import com.example.ui.theme.TextMuted

@Composable
fun VanguardistTerminal(
    logs: List<String>,
    modifier: Modifier = Modifier,
    terminalTitle: String = "VVC_TERMINAL_OUT",
    maxHeightDp: Int = 220
) {
    val listState = rememberLazyListState()

    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(ObsidianBlack)
            .border(1.dp, NeonMagenta.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
    ) {
        // Terminal Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0F1118))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(RedAlert, CircleShape)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(Color(0xFFFFB703), CircleShape)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(NeonEmerald, CircleShape)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = terminalTitle,
                    fontFamily = RetroConsoleFontFamily,
                    fontSize = 11.sp,
                    color = NeonMagenta,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = "TTY // LIVE",
                fontFamily = RetroConsoleFontFamily,
                fontSize = 10.sp,
                color = NeonEmerald
            )
        }

        // Terminal Log Area
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 90.dp, max = maxHeightDp.dp)
                .padding(8.dp)
        ) {
            items(logs) { line ->
                val annotatedLine = formatLogLine(line)
                Text(
                    text = annotatedLine,
                    fontFamily = RetroConsoleFontFamily,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    modifier = Modifier.padding(vertical = 1.dp)
                )
            }
            if (logs.isEmpty()) {
                item {
                    Text(
                        text = "VVC_CORE>_ Esperando órdenes de flujo...",
                        fontFamily = RetroConsoleFontFamily,
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }
            }
        }
    }
}

private fun formatLogLine(raw: String) = buildAnnotatedString {
    when {
        raw.startsWith("[ERROR]") || raw.startsWith("[ERR]") -> {
            withStyle(SpanStyle(color = RedAlert, fontWeight = FontWeight.Bold)) {
                append(raw.substring(0, minOf(7, raw.length)))
            }
            withStyle(SpanStyle(color = Color(0xFFFF8A9E))) {
                append(raw.substring(minOf(7, raw.length)))
            }
        }
        raw.startsWith("[SUCCESS]") || raw.startsWith("[OK]") -> {
            val tagEnd = raw.indexOf(']') + 1
            withStyle(SpanStyle(color = NeonEmerald, fontWeight = FontWeight.Bold)) {
                append(raw.substring(0, tagEnd))
            }
            withStyle(SpanStyle(color = Color(0xFFA5FFD3))) {
                append(raw.substring(tagEnd))
            }
        }
        raw.startsWith("[WARN]") -> {
            withStyle(SpanStyle(color = Color(0xFFFFB703), fontWeight = FontWeight.Bold)) {
                append("[WARN]")
            }
            withStyle(SpanStyle(color = Color(0xFFFFD166))) {
                append(raw.substring(minOf(6, raw.length)))
            }
        }
        raw.startsWith("[SPS]") || raw.startsWith("[PPS]") || raw.startsWith("[IDR]") -> {
            val tagEnd = raw.indexOf(']') + 1
            withStyle(SpanStyle(color = NeonMagenta, fontWeight = FontWeight.Bold)) {
                append(raw.substring(0, tagEnd))
            }
            withStyle(SpanStyle(color = Color(0xFFFF85C2))) {
                append(raw.substring(tagEnd))
            }
        }
        raw.startsWith("[INIT]") || raw.startsWith("[BOOT]") || raw.startsWith("[CONFIG]") || raw.startsWith("[SCAN]") -> {
            val tagEnd = raw.indexOf(']') + 1
            withStyle(SpanStyle(color = Color(0xFF00F0FF), fontWeight = FontWeight.Bold)) {
                append(raw.substring(0, tagEnd))
            }
            withStyle(SpanStyle(color = Color(0xFFE0E5F0))) {
                append(raw.substring(tagEnd))
            }
        }
        else -> {
            withStyle(SpanStyle(color = Color(0xFFC0CADC))) {
                append(raw)
            }
        }
    }
}
