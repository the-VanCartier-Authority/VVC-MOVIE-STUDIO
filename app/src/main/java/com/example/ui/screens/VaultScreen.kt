package com.example.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.RepairedVideoRecord
import com.example.ui.components.NeonBadge
import com.example.ui.components.NeonCard
import com.example.ui.theme.CyanCyber
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.RetroConsoleFontFamily
import com.example.viewmodel.VvcStudioViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun VaultScreen(
    viewModel: VvcStudioViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val videos = uiState.vaultVideos

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Vault Header
        item {
            NeonCard(
                borderColor = NeonEmerald.copy(alpha = 0.7f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "BÓVEDA DE VIDEOS MP4 RECUPERADOS",
                            fontFamily = RetroConsoleFontFamily,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonEmerald
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Archivos listos para reproducir, guardar o exportar",
                            fontFamily = RetroConsoleFontFamily,
                            fontSize = 11.sp,
                            color = Color(0xFFA0AABF)
                        )
                    }

                    NeonBadge(
                        text = "${videos.size} MP4s",
                        textColor = NeonEmerald,
                        borderColor = NeonEmerald
                    )
                }
            }
        }

        if (videos.isEmpty()) {
            item {
                NeonCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 30.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "NO HAY VIDEOS REPARADOS EN LA BÓVEDA",
                            fontFamily = RetroConsoleFontFamily,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonMagenta
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Procese un video desde la pestaña REPARAR o use el Laboratorio Test para generar su primer archivo recuperado.",
                            fontFamily = RetroConsoleFontFamily,
                            fontSize = 11.sp,
                            color = Color(0xFFA0AABF),
                            modifier = Modifier.padding(horizontal = 20.dp)
                        )
                    }
                }
            }
        } else {
            items(videos) { rec ->
                VideoRecordCard(
                    record = rec,
                    onPlay = { viewModel.setActivePlayerVideo(rec) },
                    onShare = { viewModel.shareVideo(context, rec) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun VideoRecordCard(
    record: RepairedVideoRecord,
    onPlay: () -> Unit,
    onShare: () -> Unit
) {
    val file = File(record.outputFilePath)
    val sizeText = when {
        record.fileSizeBytes >= 1024 * 1024 -> String.format("%.2f MB", record.fileSizeBytes / (1024.0 * 1024.0))
        record.fileSizeBytes >= 1024 -> String.format("%.1f KB", record.fileSizeBytes / 1024.0)
        else -> "${record.fileSizeBytes} B"
    }

    val dateText = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(record.timestampMillis))

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(ObsidianBlack)
            .border(1.dp, ObsidianBorder, RoundedCornerShape(8.dp))
            .clickable { onPlay() }
            .padding(12.dp)
            .testTag("vault_card_${file.name}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF131922))
                        .border(1.dp, NeonEmerald.copy(alpha = 0.5f), RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Videocam,
                        contentDescription = null,
                        tint = NeonEmerald,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = file.name,
                        fontFamily = RetroConsoleFontFamily,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "MP4 // $sizeText // $dateText",
                        fontFamily = RetroConsoleFontFamily,
                        fontSize = 10.sp,
                        color = Color(0xFFA0AABF)
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onPlay,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Reproducir",
                        tint = NeonEmerald
                    )
                }

                IconButton(
                    onClick = onShare,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Compartir",
                        tint = NeonMagenta
                    )
                }
            }
        }
    }
}
