package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.RetroConsoleFontFamily

@Composable
fun HexDumpViewer(
    hexSnippet: String,
    modifier: Modifier = Modifier,
    title: String = "FORENSIC BYTE SCAN // CABECERA NAL & ATOMS"
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(ObsidianBlack)
            .border(1.dp, NeonEmerald.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0D1217))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(14.dp)
                    .background(NeonEmerald)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                fontFamily = RetroConsoleFontFamily,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = NeonEmerald
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .padding(10.dp)
        ) {
            Text(
                text = hexSnippet.ifEmpty { "0000  00 00 00 01 67 42 00 28 ... [STREAM EMPTY]" },
                fontFamily = RetroConsoleFontFamily,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                color = Color(0xFF72F5B6)
            )
        }
    }
}
