package com.example.engine

import android.content.Context
import android.net.Uri
import com.example.model.CorruptionType
import com.example.model.DiagnosticResult
import com.example.model.NalUnitStats
import java.io.InputStream

class VideoDiagnosticEngine(private val context: Context) {

    fun analyze(uri: Uri, fileName: String): DiagnosticResult {
        var inputStream: InputStream? = null
        try {
            inputStream = context.contentResolver.openInputStream(uri)
                ?: throw IllegalStateException("No se pudo abrir el flujo del archivo")

            val headerBuffer = ByteArray(8192)
            val headerBytesRead = inputStream.read(headerBuffer)
            val effectiveHeader = if (headerBytesRead > 0) headerBuffer.copyOfRange(0, headerBytesRead) else ByteArray(0)

            // Re-open stream for deep NAL inspection
            inputStream.close()
            inputStream = context.contentResolver.openInputStream(uri)

            val fileSizeBytes = try {
                context.contentResolver.openFileDescriptor(uri, "r")?.use { it.statSize } ?: 0L
            } catch (e: Exception) {
                0L
            }

            // Detect MP4 Atoms
            val hasFtyp = hasBytesSequence(effectiveHeader, "ftyp".toByteArray())
            val hasMoov = hasBytesSequence(effectiveHeader, "moov".toByteArray()) ||
                    checkTrailingMoov(uri, fileSizeBytes)
            val hasMdat = hasBytesSequence(effectiveHeader, "mdat".toByteArray())

            // Detect Annex B NAL markers
            val hasAnnexB = hasAnnexBStartCodes(effectiveHeader)
            val isIpcProprietary = H264NalAnalyzer().hasIpc360Headers(effectiveHeader)

            // Deep NAL unit analysis
            val nalAnalyzer = H264NalAnalyzer()
            var spsCount = 0
            var ppsCount = 0
            var idrCount = 0
            var sliceCount = 0
            var seiCount = 0
            var audCount = 0
            var totalNals = 0

            var detectedSps: SpsInfo? = null

            // Inspect first 3000 NAL units or until sufficient info
            nalAnalyzer.extractNalUnits(inputStream ?: throw IllegalStateException(), maxUnits = 2000) { nal ->
                totalNals++
                when (nal.type) {
                    7 -> {
                        spsCount++
                        if (detectedSps == null) {
                            detectedSps = SpsParser.parse(nal.data)
                        }
                    }
                    8 -> ppsCount++
                    5 -> idrCount++
                    1 -> sliceCount++
                    6 -> seiCount++
                    9 -> audCount++
                }
            }

            val corruptionList = mutableListOf<CorruptionType>()
            var confidence = 95
            var detectedFormat = "Flujo Raw H.264 / AVC"

            if (isIpcProprietary) {
                corruptionList.add(CorruptionType.IPC360_PROPRIETARY_HEADER)
                detectedFormat = "Cámara IPC 360 Home (H.264 Encapsulado)"
            }

            if (hasFtyp && !hasMoov && hasMdat) {
                corruptionList.add(CorruptionType.MISSING_MOOV_ATOM)
                detectedFormat = "MP4 Corrupto (Átomo MOOV no finalizado)"
                confidence = 90
            } else if (hasFtyp && hasMoov) {
                corruptionList.add(CorruptionType.HEALTHY_MP4)
                detectedFormat = "Video MP4 Estándar"
                confidence = 100
            } else if (hasAnnexB || totalNals > 0) {
                corruptionList.add(CorruptionType.RAW_H264_NO_CONTAINER)
                detectedFormat = "Flujo Elemental H.264 (CCTV / IPC360)"
                confidence = 98
            } else {
                corruptionList.add(CorruptionType.UNKNOWN_STREAM)
                detectedFormat = "Flujo Binario Desconocido"
                confidence = 35
            }

            if (spsCount == 0) {
                corruptionList.add(CorruptionType.MISSING_INITIAL_SPS_PPS)
                confidence = minOf(confidence, 82)
            }

            val width = detectedSps?.width ?: 1920
            val height = detectedSps?.height ?: 1080
            val estimatedFps = if (isIpcProprietary || fileName.contains("360", ignoreCase = true)) 20.0 else 25.0
            val recoverableFrames = idrCount + sliceCount
            val estimatedDuration = if (estimatedFps > 0) recoverableFrames / estimatedFps else 0.0

            val hexSnippet = buildHexSnippet(effectiveHeader, minOf(effectiveHeader.size, 128))

            val recommendation = when {
                corruptionList.contains(CorruptionType.IPC360_PROPRIETARY_HEADER) ->
                    "Detectado flujo de cámara IPC 360 Home. Requiere desmultiplexado de cabeceras propietarias y generación de contenedor MP4 con timestamps corregidos."
                corruptionList.contains(CorruptionType.MISSING_MOOV_ATOM) ->
                    "Grabación interrumpida por pérdida de energía. El payload de video (mdat) está intacto. Se reconstruirá el árbol MOOV y la tabla STTS."
                corruptionList.contains(CorruptionType.RAW_H264_NO_CONTAINER) ->
                    "Flujo H.264 Annex B sin contenedor. Empaquetado a MP4 100% compatible mediante MediaMuxer nativo."
                corruptionList.contains(CorruptionType.MISSING_INITIAL_SPS_PPS) ->
                    "Falta SPS inicial. Se inyectará configuración estándar Full HD 1080p y se sincronizará con el primer cuadro IDR."
                else -> "El archivo es reparable y convertible con alta fidelidad a MP4."
            }

            return DiagnosticResult(
                fileName = fileName,
                fileSizeBytes = fileSizeBytes,
                detectedFormat = detectedFormat,
                repairConfidencePercent = confidence,
                corruptionTypes = corruptionList,
                nalStats = NalUnitStats(
                    spsCount = spsCount,
                    ppsCount = ppsCount,
                    idrKeyframeCount = idrCount,
                    nonIdrSliceCount = sliceCount,
                    seiCount = seiCount,
                    audCount = audCount,
                    totalNalUnits = totalNals,
                    proprietaryHeadersStripped = if (isIpcProprietary) totalNals else 0
                ),
                detectedWidth = width,
                detectedHeight = height,
                estimatedFps = estimatedFps,
                estimatedDurationSeconds = estimatedDuration,
                estimatedRecoverableFrames = recoverableFrames,
                hasValidSps = spsCount > 0,
                hasValidPps = ppsCount > 0,
                hexSnippet = hexSnippet,
                recommendation = recommendation,
                isRepairFeasible = confidence >= 50
            )

        } catch (e: Exception) {
            return DiagnosticResult(
                fileName = fileName,
                fileSizeBytes = 0L,
                detectedFormat = "Error de lectura",
                repairConfidencePercent = 10,
                corruptionTypes = listOf(CorruptionType.UNKNOWN_STREAM),
                nalStats = NalUnitStats(),
                detectedWidth = 1920,
                detectedHeight = 1080,
                estimatedFps = 25.0,
                estimatedDurationSeconds = 0.0,
                estimatedRecoverableFrames = 0,
                hasValidSps = false,
                hasValidPps = false,
                hexSnippet = "ERR: ${e.message}",
                recommendation = "No se pudo leer el archivo: ${e.localizedMessage}",
                isRepairFeasible = false
            )
        } finally {
            try {
                inputStream?.close()
            } catch (_: Exception) {}
        }
    }

    private fun hasBytesSequence(source: ByteArray, sequence: ByteArray): Boolean {
        if (sequence.isEmpty() || source.size < sequence.size) return false
        val max = source.size - sequence.size
        for (i in 0..max) {
            var match = true
            for (j in sequence.indices) {
                if (source[i + j] != sequence[j]) {
                    match = false
                    break
                }
            }
            if (match) return true
        }
        return false
    }

    private fun hasAnnexBStartCodes(data: ByteArray): Boolean {
        if (data.size < 4) return false
        for (i in 0 until data.size - 3) {
            if (data[i].toInt() == 0 && data[i + 1].toInt() == 0) {
                if (data[i + 2].toInt() == 1 || (data[i + 2].toInt() == 0 && data[i + 3].toInt() == 1)) {
                    return true
                }
            }
        }
        return false
    }

    private fun checkTrailingMoov(uri: Uri, fileSize: Long): Boolean {
        if (fileSize < 256) return false
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val skipBytes = maxOf(0L, fileSize - 8192)
                stream.skip(skipBytes)
                val tail = ByteArray(8192)
                val read = stream.read(tail)
                if (read > 0) hasBytesSequence(tail.copyOfRange(0, read), "moov".toByteArray()) else false
            } ?: false
        } catch (_: Exception) {
            false
        }
    }

    private fun buildHexSnippet(data: ByteArray, length: Int): String {
        val sb = StringBuilder()
        val len = minOf(data.size, length)
        for (i in 0 until len step 16) {
            sb.append(String.format("%04X  ", i))
            val chunkLen = minOf(16, len - i)
            for (j in 0 until chunkLen) {
                sb.append(String.format("%02X ", data[i + j]))
            }
            for (j in chunkLen until 16) {
                sb.append("   ")
            }
            sb.append(" |")
            for (j in 0 until chunkLen) {
                val b = data[i + j].toInt() and 0xFF
                val ch = if (b in 32..126) b.toChar() else '.'
                sb.append(ch)
            }
            sb.append("|\n")
        }
        return sb.toString()
    }
}
