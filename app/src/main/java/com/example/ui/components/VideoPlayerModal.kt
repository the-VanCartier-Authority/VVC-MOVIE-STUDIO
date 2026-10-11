package com.example.ui.components

import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.RepairedVideoRecord
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.RetroConsoleFontFamily
import kotlinx.coroutines.delay
import java.io.File

@Composable
fun VideoPlayerModal(
    record: RepairedVideoRecord,
    onDismiss: () -> Unit,
    onShare: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        var videoViewRef by remember { mutableStateOf<VideoView?>(null) }
        var isPlaying by remember { mutableStateOf(false) }
        var currentPositionMs by remember { mutableIntStateOf(0) }
        var durationMs by remember { mutableIntStateOf(0) }
        var isSeeking by remember { mutableStateOf(false) }

        // Polling playback progress
        LaunchedEffect(isPlaying) {
            while (true) {
                if (!isSeeking && videoViewRef != null) {
                    try {
                        val pos = videoViewRef?.currentPosition ?: 0
                        val dur = videoViewRef?.duration ?: 0
                        currentPositionMs = pos
                        if (dur > 0) durationMs = dur
                        isPlaying = videoViewRef?.isPlaying == true
                    } catch (_: Exception) {}
                }
                delay(250)
            }
        }

        DisposableEffect(Unit) {
            onDispose {
                try {
                    videoViewRef?.stopPlayback()
                } catch (_: Exception) {}
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xE6050608))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(ObsidianCard)
                    .border(1.5.dp, NeonEmerald.copy(alpha = 0.8f), RoundedCornerShape(14.dp))
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "REPRODUCTOR VVC // MP4 RECUPERADO",
                            fontFamily = RetroConsoleFontFamily,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonEmerald
                        )
                        Text(
                            text = File(record.outputFilePath).name,
                            fontFamily = RetroConsoleFontFamily,
                            fontSize = 11.sp,
                            color = Color(0xFFA0AABF),
                            maxLines = 1
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .testTag("close_player_button")
                            .size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar reproductor",
                            tint = NeonMagenta
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Video Surface View Container
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(ObsidianBlack)
                        .border(1.dp, ObsidianBorder, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    AndroidView(
                        factory = { ctx ->
                            VideoView(ctx).apply {
                                setVideoPath(record.outputFilePath)
                                setOnPreparedListener { mp ->
                                    mp.isLooping = true
                                    durationMs = mp.duration
                                    start()
                                    isPlaying = true
                                }
                                setOnErrorListener { _, _, _ ->
                                    true
                                }
                                videoViewRef = this
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Progress Bar Slider
                val progressFrac = if (durationMs > 0) currentPositionMs.toFloat() / durationMs.toFloat() else 0f
                Slider(
                    value = progressFrac.coerceIn(0f, 1f),
                    onValueChange = { frac ->
                        isSeeking = true
                        val newPos = (frac * durationMs).toInt()
                        currentPositionMs = newPos
                    },
                    onValueChangeFinished = {
                        isSeeking = false
                        videoViewRef?.seekTo(currentPositionMs)
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = NeonEmerald,
                        activeTrackColor = NeonEmerald,
                        inactiveTrackColor = ObsidianBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("video_seek_slider")
                )

                // Time indicators and Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${formatTime(currentPositionMs)} / ${formatTime(durationMs)}",
                        fontFamily = RetroConsoleFontFamily,
                        fontSize = 11.sp,
                        color = Color(0xFFA0AABF)
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                videoViewRef?.seekTo(0)
                                videoViewRef?.start()
                                isPlaying = true
                            },
                            modifier = Modifier
                                .testTag("replay_button")
                                .size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Replay,
                                contentDescription = "Reiniciar",
                                tint = NeonMagenta
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // Play / Pause main button
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(NeonEmerald)
                                .border(1.dp, Color.White, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            IconButton(
                                onClick = {
                                    if (isPlaying) {
                                        videoViewRef?.pause()
                                        isPlaying = false
                                    } else {
                                        videoViewRef?.start()
                                        isPlaying = true
                                    }
                                },
                                modifier = Modifier
                                    .testTag("play_pause_button")
                                    .size(44.dp)
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                                    tint = ObsidianBlack
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        IconButton(
                            onClick = onShare,
                            modifier = Modifier
                                .testTag("share_video_button")
                                .size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Compartir",
                                tint = NeonEmerald
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatTime(millis: Int): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
