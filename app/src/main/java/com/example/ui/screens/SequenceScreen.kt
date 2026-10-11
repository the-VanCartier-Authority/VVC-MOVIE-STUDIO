package com.example.ui.screens

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Merge
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import com.example.model.JobStatus
import com.example.model.RepairedVideoRecord
import com.example.model.VideoFile
import com.example.ui.components.NeonBadge
import com.example.ui.components.NeonButton
import com.example.ui.components.NeonCard
import com.example.ui.components.VanguardistTerminal
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.RedAlert
import com.example.ui.theme.RetroConsoleFontFamily
import com.example.viewmodel.VvcStudioViewModel

@Composable
fun SequenceScreen(
    viewModel: VvcStudioViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val isStitching = uiState.isStitchingSequence
    val queue = uiState.sequenceQueue

    val multiFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            val videoFiles = uris.map { uri ->
                val (name, size) = queryFileInfo(context, uri)
                VideoFile(
                    uri = uri,
                    name = name,
                    sizeBytes = size,
                    extension = name.substringAfterLast('.', "h264")
                )
            }
            viewModel.addSequenceFiles(videoFiles)
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Info card explaining IPC 360 multi-segment stitching
        item {
            NeonCard(
                borderColor = NeonEmerald.copy(alpha = 0.7f)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "FUSIÓN DE SECUENCIA // IPC 360 HOME",
                            fontFamily = RetroConsoleFontFamily,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonEmerald
                        )

                        NeonBadge(
                            text = "${queue.size} SEGMENTOS",
                            textColor = NeonEmerald,
                            borderColor = NeonEmerald
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Las cámaras de vigilancia IPC 360 dividen las grabaciones en clips continuos de 1 minuto (ej. 20261008120000.h264). Esta herramienta concatena y empaqueta la secuencia completa sin cortes en un único archivo MP4 con sincronización de timestamps.",
                        fontFamily = RetroConsoleFontFamily,
                        fontSize = 11.sp,
                        color = Color(0xFFA0AABF),
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        NeonButton(
                            text = "AGREGAR ARCHIVOS",
                            onClick = { multiFilePicker.launch(arrayOf("*/*")) },
                            icon = Icons.Default.Add,
                            isPrimary = true,
                            modifier = Modifier.weight(1f),
                            testTag = "add_sequence_files_button"
                        )

                        if (queue.isNotEmpty()) {
                            NeonButton(
                                text = "LIMPIAR",
                                onClick = { viewModel.clearSequenceQueue() },
                                icon = Icons.Default.Clear,
                                isPrimary = false,
                                modifier = Modifier.weight(0.7f),
                                testTag = "clear_sequence_button"
                            )
                        }
                    }
                }
            }
        }

        // Action button to start merge
        if (queue.isNotEmpty()) {
            item {
                NeonButton(
                    text = if (isStitching) "FUSIONANDO SECUENCIA..." else "FUSIONAR ${queue.size} CLIPS A MP4 MAESTRO",
                    onClick = { viewModel.startSequenceStitch() },
                    icon = Icons.Default.Merge,
                    enabled = !isStitching,
                    isPrimary = true,
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "start_stitch_button"
                )
            }
        }

        // Live progress for sequence merge
        val seqState = uiState.sequenceJobState
        if (isStitching || seqState.status == JobStatus.COMPLETED) {
            item {
                NeonCard(
                    borderColor = if (seqState.status == JobStatus.COMPLETED) NeonEmerald else NeonMagenta
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = seqState.currentStep,
                                fontFamily = RetroConsoleFontFamily,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "${(seqState.progress * 100).toInt()}%",
                                fontFamily = RetroConsoleFontFamily,
                                fontSize = 12.sp,
                                color = NeonEmerald
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        LinearProgressIndicator(
                            progress = { seqState.progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = NeonEmerald,
                            trackColor = ObsidianBorder
                        )

                        if (seqState.status == JobStatus.COMPLETED && seqState.outputMp4Path != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            NeonButton(
                                text = "REPRODUCIR SECUENCIA UNIFICADA",
                                onClick = {
                                    val rec = RepairedVideoRecord(
                                        id = "seq",
                                        originalName = "Secuencia IPC360 (${queue.size} clips)",
                                        outputFilePath = seqState.outputMp4Path!!,
                                        fileSizeBytes = seqState.outputFileSize,
                                        width = 1920,
                                        height = 1080,
                                        durationSeconds = seqState.outputDurationSeconds,
                                        totalFrames = seqState.framesProcessed
                                    )
                                    viewModel.setActivePlayerVideo(rec)
                                },
                                icon = Icons.Default.PlayArrow,
                                isPrimary = false,
                                testTag = "play_stitched_sequence_button"
                            )
                        }
                    }
                }
            }
        }

        // Queue List
        if (queue.isNotEmpty()) {
            item {
                Text(
                    text = "ORDEN DE SECUENCIA TEMPORAL:",
                    fontFamily = RetroConsoleFontFamily,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonMagenta
                )
            }

            itemsIndexed(queue) { index, item ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(ObsidianBlack)
                        .border(1.dp, ObsidianBorder, RoundedCornerShape(6.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            NeonBadge(
                                text = String.format("#%02d", index + 1),
                                textColor = NeonEmerald,
                                borderColor = NeonEmerald
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = item.name,
                                    fontFamily = RetroConsoleFontFamily,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1
                                )
                                Text(
                                    text = item.formattedSize,
                                    fontFamily = RetroConsoleFontFamily,
                                    fontSize = 10.sp,
                                    color = Color(0xFFA0AABF)
                                )
                            }
                        }

                        IconButton(
                            onClick = { viewModel.removeSequenceFile(index) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Eliminar de cola",
                                tint = RedAlert,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        // Live Terminal Log View
        item {
            VanguardistTerminal(
                logs = if (seqState.logs.isNotEmpty()) seqState.logs else uiState.globalTerminalLogs,
                terminalTitle = "CONSOLE // SEQUENCE_STITCHER"
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

private fun queryFileInfo(context: Context, uri: Uri): Pair<String, Long> {
    var name = "clip.h264"
    var size = 0L
    try {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (cursor.moveToFirst()) {
                if (nameIndex != -1) name = cursor.getString(nameIndex) ?: name
                if (sizeIndex != -1) size = cursor.getLong(sizeIndex)
            }
        }
    } catch (_: Exception) {}
    return Pair(name, size)
}
