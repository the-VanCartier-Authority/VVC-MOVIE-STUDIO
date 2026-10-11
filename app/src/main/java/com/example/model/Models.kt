package com.example.model

import android.net.Uri

data class VideoFile(
    val uri: Uri,
    val name: String,
    val sizeBytes: Long,
    val extension: String,
    val isSample: Boolean = false,
    val localPath: String? = null
) {
    val formattedSize: String
        get() {
            return when {
                sizeBytes >= 1024 * 1024 * 1024 -> String.format("%.2f GB", sizeBytes / (1024.0 * 1024.0 * 1024.0))
                sizeBytes >= 1024 * 1024 -> String.format("%.2f MB", sizeBytes / (1024.0 * 1024.0))
                sizeBytes >= 1024 -> String.format("%.1f KB", sizeBytes / 1024.0)
                else -> "$sizeBytes B"
            }
        }
}

enum class CorruptionType(val label: String, val severity: String) {
    RAW_H264_NO_CONTAINER("Flujo H.264 Raw (Sin contenedor MP4)", "MODERADO"),
    IPC360_PROPRIETARY_HEADER("Encapsulado propietario IPC 360 Home", "MODERADO"),
    MISSING_MOOV_ATOM("Átomo MOOV faltante (Grabación interrumpida)", "ALTO"),
    MISSING_INITIAL_SPS_PPS("SPS / PPS inicial ausente (GOP cortado)", "MODERADO"),
    FRAME_TRUNCATION("Truncamiento de tramas por corte de energía", "BAJO"),
    HEALTHY_MP4("Contenedor MP4 íntegro", "NINGUNO"),
    UNKNOWN_STREAM("Formato desconocido / corrupto", "CRÍTICO")
}

data class NalUnitStats(
    val spsCount: Int = 0,
    val ppsCount: Int = 0,
    val idrKeyframeCount: Int = 0,
    val nonIdrSliceCount: Int = 0,
    val seiCount: Int = 0,
    val audCount: Int = 0,
    val totalNalUnits: Int = 0,
    val proprietaryHeadersStripped: Int = 0
)

data class DiagnosticResult(
    val fileName: String,
    val fileSizeBytes: Long,
    val detectedFormat: String,
    val repairConfidencePercent: Int, // 0 - 100%
    val corruptionTypes: List<CorruptionType>,
    val nalStats: NalUnitStats,
    val detectedWidth: Int,
    val detectedHeight: Int,
    val estimatedFps: Double,
    val estimatedDurationSeconds: Double,
    val estimatedRecoverableFrames: Int,
    val hasValidSps: Boolean,
    val hasValidPps: Boolean,
    val hexSnippet: String,
    val recommendation: String,
    val isRepairFeasible: Boolean
)

data class CameraPreset(
    val id: String,
    val name: String,
    val description: String,
    val defaultFps: Int,
    val stripCustomHeaders: Boolean,
    val autoInjectSpsPps: Boolean,
    val defaultResolution: Pair<Int, Int> = Pair(1920, 1080)
) {
    companion object {
        val PRESETS = listOf(
            CameraPreset(
                id = "ipc360",
                name = "IPC 360 Home (CCTV)",
                description = "Optimizado para cámaras IPC 360 Home. Remueve cabeceras de paquete, sincroniza NAL y empaqueta a MP4.",
                defaultFps = 20,
                stripCustomHeaders = true,
                autoInjectSpsPps = true
            ),
            CameraPreset(
                id = "xiongmai",
                name = "Xiongmai / XM / V380",
                description = "Cámaras IP con flujo Annex B y timestamps por trama de 25 FPS.",
                defaultFps = 25,
                stripCustomHeaders = true,
                autoInjectSpsPps = true
            ),
            CameraPreset(
                id = "moov_fix",
                name = "MP4 Recuperación MOOV",
                description = "Repara videos MP4 cortados por apagón de cámara o celular sin índice MOOV.",
                defaultFps = 30,
                stripCustomHeaders = false,
                autoInjectSpsPps = true
            ),
            CameraPreset(
                id = "generic_cctv",
                name = "CCTV H.264 Genérico",
                description = "Convierte cualquier archivo raw .h264/.264/TS a MP4 compatible universal.",
                defaultFps = 25,
                stripCustomHeaders = true,
                autoInjectSpsPps = true
            ),
            CameraPreset(
                id = "mediacodec_deep",
                name = "MediaCodec Transcode (Hardware)",
                description = "Transcodificación profunda con decodificador/codificador MediaCodec. Estabiliza GOPs, resuelve macrobloques y fija FPS constante.",
                defaultFps = 30,
                stripCustomHeaders = true,
                autoInjectSpsPps = true
            )
        )
    }
}

enum class ConversionEngineMode(val label: String, val shortDesc: String) {
    REMUX_DIRECT("Remux Rápido (Lossless)", "Empaquetado ISO BMFF directo de paquetes AVC sin recodificar."),
    MEDIACODEC_TRANSCODE("MediaCodec Hardware", "Decodificación y recodificación completa mediante bus MediaCodec.")
}

data class RepairConfig(
    val targetFps: Int = 20,
    val preset: CameraPreset = CameraPreset.PRESETS.first(),
    val forceResolution: Pair<Int, Int>? = null,
    val customOutputName: String = "",
    val highCompatibilityMoovFastStart: Boolean = true,
    val conversionMode: ConversionEngineMode = ConversionEngineMode.REMUX_DIRECT
)

enum class JobStatus {
    IDLE, ANALYZING, REPAIRING, COMPLETED, FAILED
}

data class RepairJobState(
    val status: JobStatus = JobStatus.IDLE,
    val currentStep: String = "",
    val progress: Float = 0f, // 0.0 to 1.0
    val framesProcessed: Int = 0,
    val totalEstimatedFrames: Int = 0,
    val currentFpsProcessing: Double = 0.0,
    val outputMp4Path: String? = null,
    val outputFileSize: Long = 0L,
    val outputDurationSeconds: Double = 0.0,
    val logs: List<String> = emptyList(),
    val errorMessage: String? = null
)

data class RepairedVideoRecord(
    val id: String,
    val originalName: String,
    val outputFilePath: String,
    val fileSizeBytes: Long,
    val width: Int,
    val height: Int,
    val durationSeconds: Double,
    val totalFrames: Int,
    val timestampMillis: Long = System.currentTimeMillis()
)
