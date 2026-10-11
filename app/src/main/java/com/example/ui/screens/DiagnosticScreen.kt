package com.example.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.HexDumpViewer
import com.example.ui.components.NeonBadge
import com.example.ui.components.NeonButton
import com.example.ui.components.NeonCard
import com.example.ui.components.RadarScannerHUD
import com.example.ui.components.RepairConfidenceGauge
import com.example.ui.theme.CyanCyber
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.RedAlert
import com.example.ui.theme.RetroConsoleFontFamily
import com.example.viewmodel.VvcStudioViewModel

@Composable
fun DiagnosticScreen(
    viewModel: VvcStudioViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val diag = uiState.diagnosticResult
    val isAnalyzing = uiState.isAnalyzing

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Radar Scanner & Confidence HUD Header
        item {
            NeonCard(
                borderColor = NeonEmerald.copy(alpha = 0.7f),
                glowColor = NeonEmerald.copy(alpha = 0.2f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadarScannerHUD(
                        sizeDp = 110.dp,
                        isScanning = isAnalyzing,
                        statusText = if (isAnalyzing) "ESCANEANDO STREAM..." else "RADAR NAL: BLOQUEADO"
                    )

                    RepairConfidenceGauge(
                        percent = diag?.repairConfidencePercent ?: 0,
                        sizeDp = 110.dp,
                        label = "ÍNDICE RECUPERABILIDAD"
                    )
                }
            }
        }

        // Action: Re-Analyze button if file is present
        if (uiState.selectedFile != null) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ARCHIVO: ${uiState.selectedFile?.name}",
                        fontFamily = RetroConsoleFontFamily,
                        fontSize = 11.sp,
                        color = Color(0xFFA0AABF),
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )

                    NeonButton(
                        text = "RE-ANALIZAR",
                        onClick = { viewModel.analyzeFile() },
                        icon = Icons.Default.Analytics,
                        isPrimary = false,
                        modifier = Modifier.width(140.dp),
                        testTag = "reanalyze_button"
                    )
                }
            }
        }

        // Diagnosis Checklist & Corruption Classification
        if (diag != null) {
            item {
                NeonCard(borderColor = NeonMagenta.copy(alpha = 0.6f)) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "ANOMALÍAS DETECTADAS // CORRUPCIÓN",
                                fontFamily = RetroConsoleFontFamily,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonMagenta
                            )

                            NeonBadge(
                                text = "${diag.corruptionTypes.size} PROBLEMAS",
                                textColor = NeonMagenta,
                                borderColor = NeonMagenta
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        diag.corruptionTypes.forEach { issue ->
                            val isCrit = issue.severity == "ALTO" || issue.severity == "CRÍTICO"
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(ObsidianBlack)
                                    .border(1.dp, if (isCrit) RedAlert.copy(alpha = 0.5f) else ObsidianBorder, RoundedCornerShape(4.dp))
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isCrit) Icons.Default.Warning else Icons.Default.Check,
                                    contentDescription = null,
                                    tint = if (isCrit) RedAlert else NeonEmerald,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = issue.label,
                                        fontFamily = RetroConsoleFontFamily,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                                NeonBadge(
                                    text = issue.severity,
                                    textColor = if (isCrit) RedAlert else NeonEmerald,
                                    borderColor = if (isCrit) RedAlert else NeonEmerald
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "PLAN DE RECUPERACIÓN:",
                            fontFamily = RetroConsoleFontFamily,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonEmerald
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = diag.recommendation,
                            fontFamily = RetroConsoleFontFamily,
                            fontSize = 11.sp,
                            color = Color(0xFFC5D1E8),
                            lineHeight = 15.sp
                        )
                    }
                }
            }

            // NAL Unit Distribution Matrix
            item {
                NeonCard(borderColor = CyanCyber.copy(alpha = 0.6f)) {
                    Column {
                        Text(
                            text = "DISTRIBUCIÓN DE UNIDADES NAL (H.264 / AVC)",
                            fontFamily = RetroConsoleFontFamily,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyanCyber
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        val stats = diag.nalStats
                        NalStatRow("NAL 7 // SPS (Sequence Param Set)", "${stats.spsCount}", if (stats.spsCount > 0) NeonEmerald else RedAlert)
                        NalStatRow("NAL 8 // PPS (Picture Param Set)", "${stats.ppsCount}", if (stats.ppsCount > 0) NeonEmerald else RedAlert)
                        NalStatRow("NAL 5 // IDR (Cuadros Clave / Keyframes)", "${stats.idrKeyframeCount}", NeonMagenta)
                        NalStatRow("NAL 1 // Slices Diferenciales (P/B)", "${stats.nonIdrSliceCount}", Color(0xFF90CAF9))
                        NalStatRow("NAL 6 // Metadatos SEI", "${stats.seiCount}", Color(0xFFA0AABF))
                        NalStatRow("CABECERAS IPC 360 REMOVIDAS", "${stats.proprietaryHeadersStripped}", NeonEmerald)
                    }
                }
            }

            // Stream Resolution & Framerate Telemetry
            item {
                NeonCard(borderColor = ObsidianBorder) {
                    Column {
                        Text(
                            text = "PARÁMETROS DE VIDEO EXTRAÍDOS",
                            fontFamily = RetroConsoleFontFamily,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("RESOLUCIÓN", fontFamily = RetroConsoleFontFamily, fontSize = 10.sp, color = Color(0xFFA0AABF))
                                Text("${diag.detectedWidth} x ${diag.detectedHeight}", fontFamily = RetroConsoleFontFamily, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = NeonEmerald)
                            }

                            Column {
                                Text("FPS ESTIMADO", fontFamily = RetroConsoleFontFamily, fontSize = 10.sp, color = Color(0xFFA0AABF))
                                Text("${diag.estimatedFps} FPS", fontFamily = RetroConsoleFontFamily, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = NeonMagenta)
                            }

                            Column {
                                Text("DURACIÓN APROX.", fontFamily = RetroConsoleFontFamily, fontSize = 10.sp, color = Color(0xFFA0AABF))
                                Text("${String.format("%.1f", diag.estimatedDurationSeconds)}s", fontFamily = RetroConsoleFontFamily, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = CyanCyber)
                            }
                        }
                    }
                }
            }

            // Hex Dump Inspector
            item {
                HexDumpViewer(
                    hexSnippet = diag.hexSnippet,
                    title = "VOLCADO HEXADECIMAL FORENSE // CABECERA DE FLUJO"
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        } else {
            // Empty state placeholder
            item {
                NeonCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "NO HAY ARCHIVO SELECCIONADO PARA DIAGNÓSTICO",
                            fontFamily = RetroConsoleFontFamily,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonMagenta
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Cargue un archivo .h264 o video en la pestaña principal o use el botón Demo IPC 360 para ver el informe forense completo.",
                            fontFamily = RetroConsoleFontFamily,
                            fontSize = 11.sp,
                            color = Color(0xFFA0AABF),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NalStatRow(label: String, value: String, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontFamily = RetroConsoleFontFamily,
            fontSize = 11.sp,
            color = Color(0xFFA0AABF)
        )
        Text(
            text = value,
            fontFamily = RetroConsoleFontFamily,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}
