package com.example.engine

import android.content.Context
import android.media.MediaCodec
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import com.example.model.RepairConfig
import com.example.model.RepairJobState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.nio.ByteBuffer

class SequenceStitcher(private val context: Context) {

    suspend fun stitchSequences(
        inputUris: List<Pair<Uri, String>>, // Uri and display name
        outputFile: File,
        config: RepairConfig,
        onProgress: (RepairJobState) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        val logs = mutableListOf<String>()
        fun log(msg: String) {
            logs.add(msg)
        }

        var mediaMuxer: MediaMuxer? = null

        try {
            log("[STITCH] Iniciando fusión de secuencia (${inputUris.size} segmentos de cámara IPC 360)...")

            if (inputUris.isEmpty()) {
                return@withContext Result.failure(IllegalArgumentException("No hay archivos en la secuencia"))
            }

            // Step 1: Pre-scan first file for SPS/PPS parameters
            val firstUri = inputUris.first().first
            var spsData: ByteArray? = null
            var ppsData: ByteArray? = null
            var width = config.forceResolution?.first ?: 1920
            var height = config.forceResolution?.second ?: 1080

            context.contentResolver.openInputStream(firstUri)?.use { stream ->
                val analyzer = H264NalAnalyzer(stripIpcHeaders = config.preset.stripCustomHeaders)
                val units = analyzer.extractNalUnits(stream, maxUnits = 200)
                for (u in units) {
                    if (u.type == 7 && spsData == null) {
                        spsData = u.data
                        val parsed = SpsParser.parse(u.data)
                        if (parsed != null && config.forceResolution == null) {
                            width = parsed.width
                            height = parsed.height
                        }
                    }
                    if (u.type == 8 && ppsData == null) {
                        ppsData = u.data
                    }
                    if (spsData != null && ppsData != null) break
                }
            }

            if (spsData == null) spsData = SpsParser.createDefault1080pSps()
            if (ppsData == null) ppsData = SpsParser.createDefault1080pPps()

            if (outputFile.exists()) outputFile.delete()
            outputFile.parentFile?.mkdirs()

            mediaMuxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height)
            format.setByteBuffer("csd-0", ByteBuffer.wrap(spsData))
            format.setByteBuffer("csd-1", ByteBuffer.wrap(ppsData))
            format.setInteger(MediaFormat.KEY_FRAME_RATE, config.targetFps)

            val trackIndex = mediaMuxer.addTrack(format)
            mediaMuxer.start()

            val frameDurationUs = (1_000_000L / config.targetFps)
            var currentPtsUs = 0L
            var totalFramesWritten = 0
            val maxBufferSize = 2 * 1024 * 1024
            val frameBuffer = ByteBuffer.allocateDirect(maxBufferSize)
            val bufferInfo = MediaCodec.BufferInfo()

            val activeMuxer = mediaMuxer ?: return@withContext Result.failure(IllegalStateException("Muxer no inicializado"))
            inputUris.forEachIndexed { index, (uri, name) ->
                log("[SEGMENT] Uniendo segmento ${index + 1}/${inputUris.size}: $name")
                var inputStream: InputStream? = null
                try {
                    inputStream = context.contentResolver.openInputStream(uri) ?: return@forEachIndexed
                    val analyzer = H264NalAnalyzer(stripIpcHeaders = config.preset.stripCustomHeaders)
                    var segmentFrames = 0

                    analyzer.extractNalUnits(inputStream) { nal ->
                        if (nal.type == 7 || nal.type == 8 || nal.type == 9) return@extractNalUnits

                        frameBuffer.clear()
                        val writeLen = minOf(nal.data.size, maxBufferSize)
                        frameBuffer.put(nal.data, 0, writeLen)
                        frameBuffer.flip()

                        bufferInfo.offset = 0
                        bufferInfo.size = writeLen
                        bufferInfo.presentationTimeUs = currentPtsUs
                        bufferInfo.flags = if (nal.type == 5) MediaCodec.BUFFER_FLAG_KEY_FRAME else 0

                        activeMuxer.writeSampleData(trackIndex, frameBuffer, bufferInfo)
                        currentPtsUs += frameDurationUs
                        totalFramesWritten++
                        segmentFrames++
                    }

                    val progress = (index + 1).toFloat() / inputUris.size
                    onProgress(
                        RepairJobState(
                            currentStep = "Fusionando tramas ($totalFramesWritten acumuladas)...",
                            progress = progress * 0.95f,
                            framesProcessed = totalFramesWritten,
                            logs = logs.takeLast(12)
                        )
                    )
                } finally {
                    inputStream?.close()
                }
            }

            try {
                mediaMuxer.stop()
            } catch (_: Exception) {}
            mediaMuxer.release()
            mediaMuxer = null

            val totalSec = if (config.targetFps > 0) totalFramesWritten.toDouble() / config.targetFps else 0.0
            log("[FINISH] Fusión completada: $totalFramesWritten tramas unificadas sin cortes.")

            onProgress(
                RepairJobState(
                    currentStep = "Secuencia unificada con éxito",
                    progress = 1.0f,
                    framesProcessed = totalFramesWritten,
                    outputMp4Path = outputFile.absolutePath,
                    outputFileSize = outputFile.length(),
                    outputDurationSeconds = totalSec,
                    logs = logs.takeLast(15)
                )
            )

            Result.success(outputFile)
        } catch (e: Exception) {
            log("[ERROR] Error en fusión de secuencia: ${e.message}")
            try {
                mediaMuxer?.release()
            } catch (_: Exception) {}
            Result.failure(e)
        }
    }
}
