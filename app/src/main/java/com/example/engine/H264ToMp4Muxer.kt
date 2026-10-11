package com.example.engine

import android.content.Context
import android.media.MediaCodec
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.util.Log
import com.example.model.ConversionEngineMode
import com.example.model.RepairConfig
import com.example.model.RepairJobState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.nio.ByteBuffer

class H264ToMp4Muxer(private val context: Context) {
    private val tag = "H264ToMp4Muxer"

    suspend fun convertH264ToMp4(
        inputUri: Uri,
        outputFile: File,
        config: RepairConfig,
        onProgress: (RepairJobState) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        var inputStream: InputStream? = null
        var mediaMuxer: MediaMuxer? = null

        val logs = mutableListOf<String>()
        fun log(msg: String) {
            logs.add(msg)
            Log.d(tag, msg)
        }

        try {
            log("[INIT] Iniciando motor de reconstrucción VVC MOVIE STUDIO...")
            log("[CONFIG] Preset: ${config.preset.name} | FPS objetivo: ${config.targetFps}")

            onProgress(
                RepairJobState(
                    currentStep = "Escaneando flujo Annex B y cabeceras...",
                    progress = 0.05f,
                    logs = logs.toList()
                )
            )

            // Step 1: Pre-scan for SPS and PPS
            inputStream = context.contentResolver.openInputStream(inputUri)
                ?: return@withContext Result.failure(IllegalStateException("No se pudo abrir el archivo de entrada"))

            val analyzer = H264NalAnalyzer(stripIpcHeaders = config.preset.stripCustomHeaders)
            var spsData: ByteArray? = null
            var ppsData: ByteArray? = null
            var detectedWidth = config.forceResolution?.first ?: 1920
            var detectedHeight = config.forceResolution?.second ?: 1080

            log("[SCAN] Buscando secuencias SPS/PPS en los primeros segmentos...")
            val preScanUnits = analyzer.extractNalUnits(inputStream, maxUnits = 250)
            for (u in preScanUnits) {
                if (u.type == 7 && spsData == null) {
                    spsData = u.data
                    val parsed = SpsParser.parse(u.data)
                    if (parsed != null && config.forceResolution == null) {
                        detectedWidth = parsed.width
                        detectedHeight = parsed.height
                        log("[SPS] Detectado: ${detectedWidth}x${detectedHeight} Profile:${parsed.profileIdc}")
                    }
                }
                if (u.type == 8 && ppsData == null) {
                    ppsData = u.data
                    log("[PPS] Parámetros de imagen detectados")
                }
                if (spsData != null && ppsData != null) break
            }

            // Fallback SPS/PPS if stream started truncated
            if (spsData == null) {
                log("[WARN] Flujo sin SPS inicial. Inyectando SPS reconstruido 1080p...")
                spsData = SpsParser.createDefault1080pSps()
            }
            if (ppsData == null) {
                log("[WARN] Flujo sin PPS inicial. Inyectando PPS reconstruido...")
                ppsData = SpsParser.createDefault1080pPps()
            }

            inputStream.close()

            // Optional Hardware Transcode via MediaCodec
            if (config.conversionMode == ConversionEngineMode.MEDIACODEC_TRANSCODE || config.preset.id == "mediacodec_deep") {
                log("[ENGINE] Modo Transcodificación MediaCodec Hardware activado.")
                val codecResult = MediaCodecProcessor(context).transcodeWithMediaCodec(
                    inputUri = inputUri,
                    outputFile = outputFile,
                    config = config,
                    spsData = spsData,
                    ppsData = ppsData,
                    detectedWidth = detectedWidth,
                    detectedHeight = detectedHeight,
                    onProgress = onProgress
                )
                if (codecResult.isSuccess) {
                    val savedFile = codecResult.getOrThrow()
                    val totalDurationSec = if (config.targetFps > 0) outputFile.length() / (1024.0 * 200.0) else 0.0
                    onProgress(
                        RepairJobState(
                            currentStep = "Transcodificación MediaCodec completada",
                            progress = 1.0f,
                            outputMp4Path = savedFile.absolutePath,
                            outputFileSize = savedFile.length(),
                            outputDurationSeconds = totalDurationSec,
                            logs = logs.takeLast(20)
                        )
                    )
                    return@withContext Result.success(savedFile)
                } else {
                    log("[FALLBACK] Transcodificación hardware omitida. Procediendo con Remuxer Muxer de alta compatibilidad...")
                }
            }

            // Step 2: Initialize MediaMuxer
            if (outputFile.exists()) {
                outputFile.delete()
            }
            outputFile.parentFile?.mkdirs()

            log("[MUXER] Creando contenedor ISO MP4: ${outputFile.name}")
            mediaMuxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

            val format = MediaFormat.createVideoFormat(
                MediaFormat.MIMETYPE_VIDEO_AVC,
                detectedWidth,
                detectedHeight
            )
            format.setByteBuffer("csd-0", ByteBuffer.wrap(spsData))
            format.setByteBuffer("csd-1", ByteBuffer.wrap(ppsData))
            format.setInteger(MediaFormat.KEY_FRAME_RATE, config.targetFps)

            val trackIndex = mediaMuxer.addTrack(format)
            mediaMuxer.start()
            log("[MUXER] Pista de video AVC inicializada con éxito")

            // Step 3: Stream and write NAL units
            inputStream = context.contentResolver.openInputStream(inputUri)
                ?: return@withContext Result.failure(IllegalStateException("No se pudo reabrir el archivo de entrada"))

            val frameDurationUs = (1_000_000L / config.targetFps)
            var presentationTimeUs = 0L
            var frameCount = 0
            var keyframeCount = 0
            var hasEncounteredFirstKeyframe = false

            val startTime = System.currentTimeMillis()
            var lastProgressTime = startTime

            // Access unit buffer for aggregating NALs per frame
            val bufferInfo = MediaCodec.BufferInfo()
            val maxBufferSize = 2 * 1024 * 1024 // 2MB frame buffer
            val frameByteBuffer = ByteBuffer.allocateDirect(maxBufferSize)

            log("[REPAIR] Reensamblando tramas NAL e indexando timestamps...")

            val activeMuxer = mediaMuxer ?: return@withContext Result.failure(IllegalStateException("Muxer no inicializado"))
            analyzer.extractNalUnits(inputStream) { nal ->
                // Skip non-slice or header NALs that don't need direct sample write if already in csd
                if (nal.type == 7 || nal.type == 8 || nal.type == 9) {
                    return@extractNalUnits
                }

                // Discard trailing corrupted P-slices before first IDR keyframe
                if (!hasEncounteredFirstKeyframe) {
                    if (nal.type == 5) {
                        hasEncounteredFirstKeyframe = true
                        log("[SYNC] Sincronizado con primer cuadro clave IDR en offset ${nal.offsetInStream}")
                    } else {
                        // skip pre-keyframe slice to prevent video decode glitch
                        return@extractNalUnits
                    }
                }

                try {
                    frameByteBuffer.clear()
                    if (nal.data.size > maxBufferSize) {
                        log("[WARN] Trama sobredimensionada (${nal.data.size} bytes). Truncando para seguridad.")
                        frameByteBuffer.put(nal.data, 0, maxBufferSize)
                        bufferInfo.size = maxBufferSize
                    } else {
                        frameByteBuffer.put(nal.data)
                        bufferInfo.size = nal.data.size
                    }
                    frameByteBuffer.flip()

                    val isKey = nal.type == 5
                    if (isKey) keyframeCount++

                    bufferInfo.offset = 0
                    bufferInfo.presentationTimeUs = presentationTimeUs
                    bufferInfo.flags = if (isKey) MediaCodec.BUFFER_FLAG_KEY_FRAME else 0

                    activeMuxer.writeSampleData(trackIndex, frameByteBuffer, bufferInfo)
                    presentationTimeUs += frameDurationUs
                    frameCount++

                    val now = System.currentTimeMillis()
                    if (now - lastProgressTime >= 200 || frameCount % 60 == 0) {
                        val elapsedSec = maxOf(0.001, (now - startTime) / 1000.0)
                        val processingFps = frameCount / elapsedSec
                        val progressPercent = minOf(0.95f, 0.1f + (frameCount * 0.0005f))

                        onProgress(
                            RepairJobState(
                                currentStep = "Procesando trama #$frameCount (I-Frames: $keyframeCount)...",
                                progress = progressPercent,
                                framesProcessed = frameCount,
                                currentFpsProcessing = processingFps,
                                logs = logs.takeLast(15)
                            )
                        )
                        lastProgressTime = now
                    }
                } catch (e: Exception) {
                    log("[WARN] Salto de trama corrupta #$frameCount: ${e.message}")
                }
            }

            // Step 4: Finalize MP4
            log("[FINALIZE] Cerrando contenedor y generando átomo MOOV...")
            try {
                mediaMuxer.stop()
            } catch (e: Exception) {
                log("[WARN] Excepción menor al detener muxer: ${e.message}")
            }
            mediaMuxer.release()
            mediaMuxer = null

            val totalDurationSec = if (config.targetFps > 0) frameCount.toDouble() / config.targetFps else 0.0
            log("[SUCCESS] Reparación completada: $frameCount tramas recuperadas ($keyframeCount claves)")
            log("[OUTPUT] Archivo MP4 generado: ${outputFile.length() / 1024} KB, Duración: ${String.format("%.1f", totalDurationSec)}s")

            onProgress(
                RepairJobState(
                    currentStep = "Completado con éxito",
                    progress = 1.0f,
                    framesProcessed = frameCount,
                    totalEstimatedFrames = frameCount,
                    outputMp4Path = outputFile.absolutePath,
                    outputFileSize = outputFile.length(),
                    outputDurationSeconds = totalDurationSec,
                    logs = logs.takeLast(20)
                )
            )

            Result.success(outputFile)
        } catch (e: Exception) {
            log("[ERROR] Falla en la reconstrucción: ${e.localizedMessage}")
            try {
                mediaMuxer?.release()
            } catch (_: Exception) {}

            onProgress(
                RepairJobState(
                    currentStep = "Error en el proceso",
                    progress = 0f,
                    logs = logs.takeLast(20),
                    errorMessage = e.localizedMessage
                )
            )
            Result.failure(e)
        } finally {
            try {
                inputStream?.close()
            } catch (_: Exception) {}
        }
    }
}
