package com.example.ui.screens

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
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
import com.example.model.CameraPreset
import com.example.model.ConversionEngineMode
import com.example.model.JobStatus
import com.example.model.RepairedVideoRecord
import com.example.ui.components.NeonBadge
import com.example.ui.components.NeonButton
import com.example.ui.components.NeonCard
import com.example.ui.components.VanguardistTerminal
import com.example.ui.theme.CyanCyber
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonEmeraldDim
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.NeonMagentaDim
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.RedAlert
import com.example.ui.theme.RetroConsoleFontFamily
import com.example.viewmodel.VvcStudioViewModel

@Composable
fun StudioScreen(
    viewModel: VvcStudioViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // File picker for .h264, .264, .mp4, and all video types
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val (name, size) = queryFileInfo(context, uri)
            viewModel.selectFile(uri, name, size)
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Visual Branding Hero Banner
        item {
            NeonCard(
                borderColor = Color(0xFFD4AF37).copy(alpha = 0.7f), // Oro cepillado
                glowColor = Color(0x33FF007F)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(ObsidianBlack)
                            .border(2.dp, NeonEmerald, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.foundation.Image(
                            painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.vvc_interface_logo_1791510645672),
                            contentDescription = "Emblema Oficial VVC Movie Studio",
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "VVC",
                                fontFamily = RetroConsoleFontFamily,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonMagenta,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = ">_",
                                fontFamily = RetroConsoleFontFamily,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonEmerald
                            )
                        }
                        Text(
                            text = "MOVIE STUDIO",
                            fontFamily = RetroConsoleFontFamily,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "IDEAS / GUION / EDICIÓN / CINE",
                            fontFamily = RetroConsoleFontFamily,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Medium,
                            color = NeonEmerald,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "TUS HISTORIAS. EN OTRO NIVEL.",
                            fontFamily = RetroConsoleFontFamily,
                            fontSize = 9.sp,
                            color = Color(0xFFD4AF37), // Oro
                            letterSpacing = 0.4.sp
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Branding Color Swatches
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf(
                                Color(0xFF080B0F) to "OBS",
                                Color(0xFFFF007F) to "MAG",
                                Color(0xFF00FF9D) to "ESM",
                                Color(0xFFC0C0C0) to "PLT",
                                Color(0xFFD4AF37) to "ORO"
                            ).forEach { (col, _) ->
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(col)
                                        .border(0.5.dp, Color.White.copy(alpha = 0.7f), CircleShape)
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "PALETA OFICIAL",
                                fontFamily = RetroConsoleFontFamily,
                                fontSize = 8.sp,
                                color = Color(0xFFA0AABF)
                            )
                        }
                    }
                }
            }
        }

        // Step 1: Input Video Selection
        item {
            NeonCard(
                borderColor = NeonMagenta.copy(alpha = 0.7f),
                glowColor = NeonMagentaDim
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "[01] ORIGEN DE VIDEO // CARGA DE ARCHIVO",
                            fontFamily = RetroConsoleFontFamily,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonMagenta
                        )

                        NeonBadge(
                            text = if (uiState.selectedFile != null) "CARGADO" else "EN ESPERA",
                            textColor = if (uiState.selectedFile != null) NeonEmerald else NeonMagenta,
                            borderColor = if (uiState.selectedFile != null) NeonEmerald else NeonMagenta
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (uiState.selectedFile != null) {
                        val file = uiState.selectedFile!!
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(ObsidianBlack)
                                .border(1.dp, NeonEmerald.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Videocam,
                                        contentDescription = null,
                                        tint = NeonEmerald,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = file.name,
                                        fontFamily = RetroConsoleFontFamily,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 1
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "TAMAÑO: ${file.formattedSize} | EXTENSIÓN: .${file.extension.uppercase()}",
                                    fontFamily = RetroConsoleFontFamily,
                                    fontSize = 11.sp,
                                    color = Color(0xFFA0AABF)
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "Seleccione un archivo de video corrupto o flujo .h264 extraído de tarjeta MicroSD de cámara IPC 360 Home.",
                            fontFamily = RetroConsoleFontFamily,
                            fontSize = 12.sp,
                            color = Color(0xFFA0AABF),
                            lineHeight = 17.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        NeonButton(
                            text = if (uiState.selectedFile == null) "EXPLORAR ARCHIVO" else "CAMBIAR ARCHIVO",
                            onClick = { filePickerLauncher.launch(arrayOf("*/*")) },
                            icon = Icons.Default.FolderOpen,
                            isPrimary = true,
                            modifier = Modifier.weight(1f),
                            testTag = "browse_file_button"
                        )

                        NeonButton(
                            text = "DEMO IPC 360",
                            onClick = { viewModel.loadSampleIpc360() },
                            icon = Icons.Default.Videocam,
                            isPrimary = false,
                            modifier = Modifier.weight(0.9f),
                            testTag = "load_sample_ipc360_button"
                        )
                    }
                }
            }
        }

        // Section: Basic Metadata of Selected Video File (Pre-Processing) & Undo Changes
        if (uiState.selectedFile != null) {
            val file = uiState.selectedFile!!
            val diag = uiState.diagnosticResult
            val isAnalyzing = uiState.isAnalyzing

            item {
                NeonCard(
                    borderColor = NeonEmerald.copy(alpha = 0.85f),
                    glowColor = Color(0x3300FF9D)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = NeonEmerald,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "[METADATOS BÁSICOS // PRE-PROCESAMIENTO]",
                                    fontFamily = RetroConsoleFontFamily,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonEmerald
                                )
                            }

                            if (isAnalyzing) {
                                NeonBadge(
                                    text = "ESCANEANDO...",
                                    textColor = CyanCyber,
                                    borderColor = CyanCyber
                                )
                            } else if (diag != null) {
                                NeonBadge(
                                    text = "${diag.repairConfidencePercent}% VIABLE",
                                    textColor = NeonEmerald,
                                    borderColor = NeonEmerald
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Metadata Grid
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(ObsidianBlack)
                                .border(1.dp, ObsidianBorder, RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                MetadataRowItem(
                                    label = "NOMBRE DE ARCHIVO:",
                                    value = file.name,
                                    valueColor = Color.White
                                )

                                val exactBytesFormatted = try {
                                    String.format("%,d bytes", file.sizeBytes)
                                } catch (_: Exception) {
                                    "${file.sizeBytes} B"
                                }
                                MetadataRowItem(
                                    label = "TAMAÑO DEL ARCHIVO:",
                                    value = "${file.formattedSize} ($exactBytesFormatted)",
                                    valueColor = Color(0xFFD4AF37) // Oro cepillado
                                )

                                val resText = if (diag != null) {
                                    val tag = when {
                                        diag.detectedWidth >= 3840 -> "4K UHD"
                                        diag.detectedWidth >= 1920 -> "1080p FHD"
                                        diag.detectedWidth >= 1280 -> "720p HD"
                                        else -> "SD"
                                    }
                                    "${diag.detectedWidth} × ${diag.detectedHeight} [$tag]"
                                } else if (isAnalyzing) {
                                    "Analizando SPS / NAL..."
                                } else {
                                    "1920 × 1080 (Esperado Full HD)"
                                }

                                MetadataRowItem(
                                    label = "RESOLUCIÓN DETECTADA:",
                                    value = resText,
                                    valueColor = NeonEmerald
                                )

                                val formatText = diag?.detectedFormat ?: "Flujo .${file.extension.uppercase()} (Annex B)"
                                MetadataRowItem(
                                    label = "FORMATO & CÓDEC:",
                                    value = formatText,
                                    valueColor = CyanCyber
                                )

                                val fpsText = if (diag != null) "${diag.estimatedFps} FPS" else "${uiState.repairConfig.targetFps} FPS"
                                val durText = if (diag != null && diag.estimatedDurationSeconds > 0) {
                                    val mins = (diag.estimatedDurationSeconds / 60).toInt()
                                    val secs = (diag.estimatedDurationSeconds % 60).toInt()
                                    String.format("%02d:%02d min (%.1f seg)", mins, secs, diag.estimatedDurationSeconds)
                                } else {
                                    "Calculando duración..."
                                }

                                MetadataRowItem(
                                    label = "FRAMERATE & DURACIÓN:",
                                    value = "$fpsText | ~$durText",
                                    valueColor = NeonMagenta
                                )

                                if (diag != null && diag.nalStats.totalNalUnits > 0) {
                                    MetadataRowItem(
                                        label = "TRAMAS IDENTIFICADAS:",
                                        value = "${diag.estimatedRecoverableFrames} tramas (${diag.nalStats.idrKeyframeCount} cuadros clave IDR)",
                                        valueColor = Color(0xFFA5D6A7)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Undo and Reset Controls
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            NeonButton(
                                text = "DESHACER / QUITAR ARCHIVO",
                                onClick = { viewModel.undoChanges() },
                                icon = Icons.Default.Undo,
                                isPrimary = false,
                                modifier = Modifier.weight(1f),
                                testTag = "undo_changes_button"
                            )
                        }
                    }
                }
            }
        }

        // Step 2: Camera Presets & FPS Settings
        item {
            NeonCard(borderColor = NeonEmerald.copy(alpha = 0.6f)) {
                Column {
                    Text(
                        text = "[02] PERFIL DE RECONSTRUCCIÓN IPC 360",
                        fontFamily = RetroConsoleFontFamily,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonEmerald
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Seleccione el motor adaptativo según el dispositivo de grabación:",
                        fontFamily = RetroConsoleFontFamily,
                        fontSize = 11.sp,
                        color = Color(0xFFA0AABF)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Preset Cards Grid
                    CameraPreset.PRESETS.forEach { preset ->
                        val isSelected = uiState.repairConfig.preset.id == preset.id
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) Color(0xFF141F28) else ObsidianBlack)
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) NeonEmerald else ObsidianBorder,
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .clickable { viewModel.updatePreset(preset) }
                                .padding(10.dp)
                                .testTag("preset_${preset.id}")
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = preset.name,
                                        fontFamily = RetroConsoleFontFamily,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) NeonEmerald else Color.White
                                    )

                                    NeonBadge(
                                        text = "${preset.defaultFps} FPS",
                                        textColor = if (isSelected) NeonEmerald else NeonMagenta,
                                        borderColor = if (isSelected) NeonEmerald else ObsidianBorder
                                    )
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = preset.description,
                                    fontFamily = RetroConsoleFontFamily,
                                    fontSize = 10.sp,
                                    color = Color(0xFF8E95A5),
                                    lineHeight = 14.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // FPS Selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "TIMESTAMPS FPS OBJETIVO:",
                            fontFamily = RetroConsoleFontFamily,
                            fontSize = 11.sp,
                            color = Color(0xFFA0AABF)
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(15, 20, 25, 30).forEach { fps ->
                                val isFpsActive = uiState.repairConfig.targetFps == fps
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (isFpsActive) NeonEmerald else ObsidianBlack)
                                        .border(1.dp, if (isFpsActive) NeonEmerald else ObsidianBorder, RoundedCornerShape(4.dp))
                                        .clickable { viewModel.updateFps(fps) }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                        .testTag("fps_$fps")
                                ) {
                                    Text(
                                        text = "$fps",
                                        fontFamily = RetroConsoleFontFamily,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isFpsActive) ObsidianBlack else NeonEmerald
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Conversion Engine Selector
                    Column {
                        Text(
                            text = "MOTOR DE CONVERSIÓN:",
                            fontFamily = RetroConsoleFontFamily,
                            fontSize = 11.sp,
                            color = Color(0xFFA0AABF)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ConversionEngineMode.values().forEach { mode ->
                                val isModeActive = uiState.repairConfig.conversionMode == mode
                                val activeColor = if (mode == ConversionEngineMode.MEDIACODEC_TRANSCODE) NeonMagenta else NeonEmerald
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isModeActive) activeColor.copy(alpha = 0.2f) else ObsidianBlack)
                                        .border(1.dp, if (isModeActive) activeColor else ObsidianBorder, RoundedCornerShape(6.dp))
                                        .clickable { viewModel.setConversionMode(mode) }
                                        .padding(horizontal = 8.dp, vertical = 8.dp)
                                        .testTag("mode_${mode.name.lowercase()}")
                                ) {
                                    Column {
                                        Text(
                                            text = mode.label,
                                            fontFamily = RetroConsoleFontFamily,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isModeActive) activeColor else Color.White
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = mode.shortDesc,
                                            fontFamily = RetroConsoleFontFamily,
                                            fontSize = 9.sp,
                                            color = Color(0xFF8E95A5),
                                            lineHeight = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Step 3: Diagnostic Preview Bar (if analyzed)
        if (uiState.diagnosticResult != null) {
            val diag = uiState.diagnosticResult!!
            item {
                NeonCard(
                    borderColor = CyanCyber.copy(alpha = 0.6f)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "[DIAGNÓSTICO RÁPIDO]",
                                fontFamily = RetroConsoleFontFamily,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyanCyber
                            )

                            NeonBadge(
                                text = "${diag.repairConfidencePercent}% VIABILIDAD",
                                textColor = NeonEmerald,
                                borderColor = NeonEmerald
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "FORMATO: ${diag.detectedFormat}",
                            fontFamily = RetroConsoleFontFamily,
                            fontSize = 11.sp,
                            color = Color.White
                        )
                        Text(
                            text = "RESOLUCIÓN DETECTADA: ${diag.detectedWidth}x${diag.detectedHeight} | TRAMAS ESTIMADAS: ${diag.estimatedRecoverableFrames}",
                            fontFamily = RetroConsoleFontFamily,
                            fontSize = 11.sp,
                            color = Color(0xFFA0AABF)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = diag.recommendation,
                            fontFamily = RetroConsoleFontFamily,
                            fontSize = 10.sp,
                            color = NeonEmerald,
                            lineHeight = 14.sp
                        )
                    }
                }
            }
        }

        // Step 4: Primary Action Button
        item {
            val isRepairing = uiState.repairJobState.status == JobStatus.REPAIRING
            NeonButton(
                text = if (isRepairing) "PROCESANDO RECONSTRUCCIÓN..." else "INICIAR RECONSTRUCCIÓN A MP4",
                onClick = { viewModel.startRepair() },
                icon = Icons.Default.Build,
                enabled = uiState.selectedFile != null && !isRepairing,
                isPrimary = true,
                modifier = Modifier.fillMaxWidth(),
                testTag = "start_repair_button"
            )
        }

        // Step 5: Live Progress Bar & Status (when repairing)
        val jobState = uiState.repairJobState
        if (jobState.status == JobStatus.REPAIRING || jobState.status == JobStatus.COMPLETED || jobState.status == JobStatus.FAILED) {
            item {
                NeonCard(
                    borderColor = when (jobState.status) {
                        JobStatus.COMPLETED -> NeonEmerald
                        JobStatus.FAILED -> RedAlert
                        else -> NeonMagenta
                    }
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = jobState.currentStep,
                                fontFamily = RetroConsoleFontFamily,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = when (jobState.status) {
                                    JobStatus.COMPLETED -> NeonEmerald
                                    JobStatus.FAILED -> RedAlert
                                    else -> Color.White
                                }
                            )

                            Text(
                                text = "${(jobState.progress * 100).toInt()}%",
                                fontFamily = RetroConsoleFontFamily,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonEmerald
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        LinearProgressIndicator(
                            progress = { jobState.progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = NeonEmerald,
                            trackColor = ObsidianBorder
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "TRAMAS RECUPERADAS: ${jobState.framesProcessed}",
                                fontFamily = RetroConsoleFontFamily,
                                fontSize = 10.sp,
                                color = Color(0xFFA0AABF)
                            )
                            if (jobState.currentFpsProcessing > 0) {
                                Text(
                                    text = "VELOCIDAD: ${jobState.currentFpsProcessing.toInt()} FPS",
                                    fontFamily = RetroConsoleFontFamily,
                                    fontSize = 10.sp,
                                    color = NeonMagenta
                                )
                            }
                        }

                        // Success Actions
                        if (jobState.status == JobStatus.COMPLETED && jobState.outputMp4Path != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                NeonButton(
                                    text = "REPRODUCIR MP4",
                                    onClick = {
                                        val rec = RepairedVideoRecord(
                                            id = "current",
                                            originalName = uiState.selectedFile?.name ?: "video.h264",
                                            outputFilePath = jobState.outputMp4Path!!,
                                            fileSizeBytes = jobState.outputFileSize,
                                            width = 1920,
                                            height = 1080,
                                            durationSeconds = jobState.outputDurationSeconds,
                                            totalFrames = jobState.framesProcessed
                                        )
                                        viewModel.setActivePlayerVideo(rec)
                                    },
                                    icon = Icons.Default.PlayArrow,
                                    isPrimary = false,
                                    modifier = Modifier.weight(1f),
                                    testTag = "play_repaired_button"
                                )

                                NeonButton(
                                    text = "COMPARTIR",
                                    onClick = {
                                        val rec = RepairedVideoRecord(
                                            id = "current",
                                            originalName = uiState.selectedFile?.name ?: "video.h264",
                                            outputFilePath = jobState.outputMp4Path!!,
                                            fileSizeBytes = jobState.outputFileSize,
                                            width = 1920,
                                            height = 1080,
                                            durationSeconds = jobState.outputDurationSeconds,
                                            totalFrames = jobState.framesProcessed
                                        )
                                        viewModel.shareVideo(context, rec)
                                    },
                                    icon = Icons.Default.Share,
                                    isPrimary = true,
                                    modifier = Modifier.weight(1f),
                                    testTag = "share_repaired_button"
                                )
                            }
                        }
                    }
                }
            }
        }

        // Live Terminal Log View
        item {
            VanguardistTerminal(
                logs = if (uiState.repairJobState.logs.isNotEmpty()) uiState.repairJobState.logs else uiState.globalTerminalLogs,
                terminalTitle = "CONSOLE // VVC_MUXER_ENGINE"
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

private fun queryFileInfo(context: Context, uri: Uri): Pair<String, Long> {
    var name = "video_stream.h264"
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

@Composable
private fun MetadataRowItem(
    label: String,
    value: String,
    valueColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontFamily = RetroConsoleFontFamily,
            fontSize = 10.sp,
            color = Color(0xFFA0AABF)
        )
        Text(
            text = value,
            fontFamily = RetroConsoleFontFamily,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = valueColor,
            maxLines = 1
        )
    }
}

