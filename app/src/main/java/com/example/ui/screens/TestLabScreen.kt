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
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Videocam
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
import com.example.ui.components.NeonBadge
import com.example.ui.components.NeonButton
import com.example.ui.components.NeonCard
import com.example.ui.theme.CyanCyber
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.RetroConsoleFontFamily
import com.example.viewmodel.VvcStudioViewModel

@Composable
fun TestLabScreen(
    viewModel: VvcStudioViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            NeonCard(
                borderColor = CyanCyber.copy(alpha = 0.7f)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "LABORATORIO DE PRUEBAS FORENSES // VVC",
                            fontFamily = RetroConsoleFontFamily,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyanCyber
                        )
                        NeonBadge(
                            text = "SYNTH // SIMULADOR",
                            textColor = CyanCyber,
                            borderColor = CyanCyber
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Genere archivos de prueba sintetizados directamente en el dispositivo para verificar el algoritmo de diagnóstico y la reconstrucción a MP4 sin necesidad de conectar una cámara física de inmediato.",
                        fontFamily = RetroConsoleFontFamily,
                        fontSize = 11.sp,
                        color = Color(0xFFA0AABF),
                        lineHeight = 16.sp
                    )
                }
            }
        }

        // Test Scenario 1: IPC 360 Home raw H.264
        item {
            NeonCard(borderColor = NeonMagenta.copy(alpha = 0.6f)) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Videocam,
                                contentDescription = null,
                                tint = NeonMagenta,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "CASO A: CÁMARA IPC 360 HOME RAW (.H264)",
                                fontFamily = RetroConsoleFontFamily,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonMagenta
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Simula el volcado de video crudo Annex B de una cámara IPC 360 Home con cabecera de sincronización propietaria de 16 bytes y NAL units 7 (SPS), 8 (PPS), 5 (IDR) y 1 (Slices) sin contenedor MP4.",
                        fontFamily = RetroConsoleFontFamily,
                        fontSize = 11.sp,
                        color = Color(0xFFA0AABF),
                        lineHeight = 15.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    NeonButton(
                        text = "GENERAR & CARGAR CASO IPC 360",
                        onClick = { viewModel.loadSampleIpc360() },
                        icon = Icons.Default.Science,
                        isPrimary = true,
                        testTag = "gen_sample_ipc360"
                    )
                }
            }
        }

        // Test Scenario 2: Broken MP4 without MOOV atom
        item {
            NeonCard(borderColor = NeonEmerald.copy(alpha = 0.6f)) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = NeonEmerald,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "CASO B: MP4 CORTADO POR APAGÓN (SIN MOOV)",
                                fontFamily = RetroConsoleFontFamily,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonEmerald
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Simula una grabación MP4 que fue interrumpida abruptamente por corte de luz en la cámara o reinicio inesperado. Contiene el átomo 'ftyp' y 'mdat' pero carece del índice 'moov'.",
                        fontFamily = RetroConsoleFontFamily,
                        fontSize = 11.sp,
                        color = Color(0xFFA0AABF),
                        lineHeight = 15.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    NeonButton(
                        text = "GENERAR & CARGAR CASO SIN MOOV",
                        onClick = { viewModel.loadSampleBrokenMp4() },
                        icon = Icons.Default.Science,
                        isPrimary = false,
                        testTag = "gen_sample_broken_mp4"
                    )
                }
            }
        }

        // Architectural Whitepaper Card
        item {
            NeonCard(borderColor = ObsidianBorder) {
                Column {
                    Text(
                        text = "ESPECIFICACIÓN TÉCNICA DEL MOTOR VVC",
                        fontFamily = RetroConsoleFontFamily,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    val specItems = listOf(
                        "Desmultiplexado Annex-B" to "Reconoce códigos de inicio 0x00000001 y 0x000001 normalizando el flujo a unidades de acceso AVC.",
                        "Cabeceras IPC 360" to "Filtra preámbulos propietarios HK40 / DHAV / IPC extrayendo los NAL units internos limpios.",
                        "Reconstrucción SPS/PPS" to "Inyecta parámetros de secuencia y matrices de cuantización si el corte inicial dañó el primer GOP.",
                        "Sincronización PTS Monotónica" to "Genera timestamps de presentación microsegundo a microsegundo para evitar desfasaje de audio/video.",
                        "Empaquetado ISO BMFF" to "MediaMuxer genera átomos ftyp, moov, mvhd, trak, mdia, stbl y mdat 100% compatibles."
                    )

                    specItems.forEach { (title, desc) ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(ObsidianBlack)
                                .padding(8.dp)
                        ) {
                            Column {
                                Text(
                                    text = "• $title",
                                    fontFamily = RetroConsoleFontFamily,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonEmerald
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = desc,
                                    fontFamily = RetroConsoleFontFamily,
                                    fontSize = 10.sp,
                                    color = Color(0xFFA0AABF)
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
