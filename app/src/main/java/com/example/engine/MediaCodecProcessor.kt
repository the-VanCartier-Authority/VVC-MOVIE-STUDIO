package com.example.engine

import android.content.Context
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.util.Log
import com.example.model.RepairConfig
import com.example.model.RepairJobState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.nio.ByteBuffer

/**
 * Hardware-accelerated MediaCodec transcoder and pipeline.
 * Employs Android's native MediaCodec video decoder and encoder to normalize
 * irregular, variable-framerate, or corrupted .h264 surveillance footage into
 * fully compliant, seekable MP4 containers.
 */
class MediaCodecProcessor(private val context: Context) {
    private val tag = "MediaCodecProcessor"

    suspend fun transcodeWithMediaCodec(
        inputUri: Uri,
        outputFile: File,
        config: RepairConfig,
        spsData: ByteArray,
        ppsData: ByteArray,
        detectedWidth: Int,
        detectedHeight: Int,
        onProgress: (RepairJobState) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        val logs = mutableListOf<String>()
        fun log(msg: String) {
            logs.add(msg)
            Log.d(tag, msg)
        }

        var decoder: MediaCodec? = null
        var encoder: MediaCodec? = null
        var muxer: MediaMuxer? = null
        var inputStream: InputStream? = null

        try {
            log("[CODEC] Inicializando pipeline MediaCodec (Decoder + Encoder AVC)...")
            log("[CODEC] Resolución objetivo: ${detectedWidth}x${detectedHeight} @ ${config.targetFps} FPS")

            // 1. Configure Decoder
            val decoderFormat = MediaFormat.createVideoFormat(
                MediaFormat.MIMETYPE_VIDEO_AVC,
                detectedWidth,
                detectedHeight
            ).apply {
                setByteBuffer("csd-0", ByteBuffer.wrap(spsData))
                setByteBuffer("csd-1", ByteBuffer.wrap(ppsData))
            }

            decoder = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
            decoder.configure(decoderFormat, null, null, 0)
            decoder.start()
            log("[CODEC] Decodificador AVC iniciado")

            // 2. Configure Encoder
            val bitRate = when {
                detectedWidth >= 2560 -> 4_500_000
                detectedWidth >= 1920 -> 2_800_000
                detectedWidth >= 1280 -> 1_800_000
                else -> 1_200_000
            }

            val encoderFormat = MediaFormat.createVideoFormat(
                MediaFormat.MIMETYPE_VIDEO_AVC,
                detectedWidth,
                detectedHeight
            ).apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible)
                setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
                setInteger(MediaFormat.KEY_FRAME_RATE, config.targetFps)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1) // Keyframe every 1 second
            }

            encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
            encoder.configure(encoderFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            encoder.start()
            log("[CODEC] Codificador AVC configurado ($bitRate bps)")

            // 3. Configure Muxer
            if (outputFile.exists()) outputFile.delete()
            outputFile.parentFile?.mkdirs()
            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

            inputStream = context.contentResolver.openInputStream(inputUri)
                ?: return@withContext Result.failure(IllegalStateException("No se pudo abrir el stream de video"))

            val analyzer = H264NalAnalyzer(stripIpcHeaders = config.preset.stripCustomHeaders)
            val frameDurationUs = (1_000_000L / config.targetFps)
            var presentationTimeUs = 0L
            var framesDecoded = 0
            var framesEncoded = 0
            var trackIndex = -1
            var muxerStarted = false

            val decoderBufferInfo = MediaCodec.BufferInfo()
            val encoderBufferInfo = MediaCodec.BufferInfo()
            val timeoutUs = 10_000L

            log("[CODEC] Procesando tramas a través del bus de hardware...")

            // Feed input NAL units into decoder
            analyzer.extractNalUnits(inputStream) { nal ->
                if (nal.type == 7 || nal.type == 8 || nal.type == 9) return@extractNalUnits

                var inputQueued = false
                while (!inputQueued) {
                    val inIndex = decoder.dequeueInputBuffer(timeoutUs)
                    if (inIndex >= 0) {
                        val inBuffer = decoder.getInputBuffer(inIndex)
                        inBuffer?.clear()
                        inBuffer?.put(nal.data)
                        decoder.queueInputBuffer(
                            inIndex,
                            0,
                            nal.data.size,
                            presentationTimeUs,
                            if (nal.type == 5) MediaCodec.BUFFER_FLAG_KEY_FRAME else 0
                        )
                        presentationTimeUs += frameDurationUs
                        inputQueued = true
                    } else {
                        // Drain decoder & encoder while waiting for input buffer
                        drainDecoderToEncoder(decoder, encoder, decoderBufferInfo, timeoutUs)
                    }
                }

                // Drain output of encoder into muxer
                drainEncoderToMuxer(
                    encoder,
                    muxer,
                    encoderBufferInfo,
                    timeoutUs,
                    onTrackAdded = { idx ->
                        trackIndex = idx
                        muxerStarted = true
                    },
                    getTrackIndex = { trackIndex },
                    isMuxerStarted = { muxerStarted },
                    onFrameWritten = {
                        framesEncoded++
                        if (framesEncoded % 30 == 0) {
                            onProgress(
                                RepairJobState(
                                    currentStep = "Codificando trama MediaCodec #$framesEncoded...",
                                    progress = minOf(0.95f, 0.1f + framesEncoded * 0.0008f),
                                    framesProcessed = framesEncoded,
                                    logs = logs.takeLast(12)
                                )
                            )
                        }
                    }
                )
            }

            // End of stream signal
            val eosIndex = decoder.dequeueInputBuffer(timeoutUs)
            if (eosIndex >= 0) {
                decoder.queueInputBuffer(eosIndex, 0, 0, presentationTimeUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
            }

            // Drain remaining frames
            var drainAttempts = 0
            while (drainAttempts < 50) {
                drainDecoderToEncoder(decoder, encoder, decoderBufferInfo, timeoutUs)
                drainEncoderToMuxer(
                    encoder,
                    muxer,
                    encoderBufferInfo,
                    timeoutUs,
                    onTrackAdded = { idx -> trackIndex = idx; muxerStarted = true },
                    getTrackIndex = { trackIndex },
                    isMuxerStarted = { muxerStarted },
                    onFrameWritten = { framesEncoded++ }
                )
                drainAttempts++
            }

            if (muxerStarted) {
                try {
                    muxer.stop()
                } catch (e: Exception) {
                    log("[WARN] Excepción menor deteniendo muxer: ${e.message}")
                }
            }

            val totalSec = if (config.targetFps > 0) framesEncoded.toDouble() / config.targetFps else 0.0
            log("[SUCCESS] Transcodificación MediaCodec finalizada: $framesEncoded tramas generadas.")

            Result.success(outputFile)
        } catch (e: Exception) {
            log("[WARN] Pipeline MediaCodec reportó: ${e.message}. Activando respaldo a remuxer nativo.")
            Result.failure(e)
        } finally {
            try { decoder?.stop(); decoder?.release() } catch (_: Exception) {}
            try { encoder?.stop(); encoder?.release() } catch (_: Exception) {}
            try { muxer?.release() } catch (_: Exception) {}
            try { inputStream?.close() } catch (_: Exception) {}
        }
    }

    private fun drainDecoderToEncoder(
        decoder: MediaCodec,
        encoder: MediaCodec,
        bufferInfo: MediaCodec.BufferInfo,
        timeoutUs: Long
    ) {
        val outIndex = decoder.dequeueOutputBuffer(bufferInfo, timeoutUs)
        if (outIndex >= 0) {
            val decOutBuffer = decoder.getOutputBuffer(outIndex)
            val encInIndex = encoder.dequeueInputBuffer(timeoutUs)
            if (encInIndex >= 0 && decOutBuffer != null) {
                val encInBuffer = encoder.getInputBuffer(encInIndex)
                encInBuffer?.clear()
                decOutBuffer.position(bufferInfo.offset)
                decOutBuffer.limit(bufferInfo.offset + bufferInfo.size)
                encInBuffer?.put(decOutBuffer)
                encoder.queueInputBuffer(
                    encInIndex,
                    0,
                    bufferInfo.size,
                    bufferInfo.presentationTimeUs,
                    bufferInfo.flags
                )
            }
            decoder.releaseOutputBuffer(outIndex, false)
        }
    }

    private fun drainEncoderToMuxer(
        encoder: MediaCodec,
        muxer: MediaMuxer,
        bufferInfo: MediaCodec.BufferInfo,
        timeoutUs: Long,
        onTrackAdded: (Int) -> Unit,
        getTrackIndex: () -> Int,
        isMuxerStarted: () -> Boolean,
        onFrameWritten: () -> Unit
    ) {
        while (true) {
            val encOutIndex = encoder.dequeueOutputBuffer(bufferInfo, timeoutUs)
            if (encOutIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                val newFormat = encoder.outputFormat
                val trackIdx = muxer.addTrack(newFormat)
                muxer.start()
                onTrackAdded(trackIdx)
            } else if (encOutIndex >= 0) {
                val encodedData = encoder.getOutputBuffer(encOutIndex)
                if (encodedData != null && isMuxerStarted() && bufferInfo.size > 0) {
                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0) {
                        encodedData.position(bufferInfo.offset)
                        encodedData.limit(bufferInfo.offset + bufferInfo.size)
                        muxer.writeSampleData(getTrackIndex(), encodedData, bufferInfo)
                        onFrameWritten()
                    }
                }
                encoder.releaseOutputBuffer(encOutIndex, false)
                if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    break
                }
            } else {
                break
            }
        }
    }
}
