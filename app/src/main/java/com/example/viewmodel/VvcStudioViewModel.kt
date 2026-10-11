package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.engine.H264ToMp4Muxer
import com.example.engine.SampleVideoGenerator
import com.example.engine.SequenceStitcher
import com.example.engine.VideoDiagnosticEngine
import com.example.model.CameraPreset
import com.example.model.ConversionEngineMode
import com.example.model.DiagnosticResult
import com.example.model.JobStatus
import com.example.model.RepairConfig
import com.example.model.RepairJobState
import com.example.model.RepairedVideoRecord
import com.example.model.VideoFile
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

enum class StudioTab(val label: String, val code: String) {
    STUDIO("REPARAR & CONVERTIR", "CORE"),
    DIAGNOSTIC("TERMINAL DIAGNÓSTICO", "DIAG"),
    SEQUENCE("SECUENCIA IPC360", "SEQ"),
    VAULT("BÓVEDA DE VIDEOS", "VAULT"),
    TEST_LAB("LABORATORIO TEST", "LAB")
}

data class StudioUiState(
    val activeTab: StudioTab = StudioTab.STUDIO,
    val selectedFile: VideoFile? = null,
    val diagnosticResult: DiagnosticResult? = null,
    val isAnalyzing: Boolean = false,
    val repairJobState: RepairJobState = RepairJobState(),
    val repairConfig: RepairConfig = RepairConfig(),
    val sequenceQueue: List<VideoFile> = emptyList(),
    val isStitchingSequence: Boolean = false,
    val sequenceJobState: RepairJobState = RepairJobState(),
    val vaultVideos: List<RepairedVideoRecord> = emptyList(),
    val activePlayerVideo: RepairedVideoRecord? = null,
    val globalTerminalLogs: List<String> = listOf(
        "[BOOT] VVC MOVIE STUDIO v3.2.0 - Kernel Online",
        "[INIT] Subsistema de decodificación Annex B & IPC 360 inicializado",
        "[READY] Esperando archivo de video (.h264, .264, .mp4) para diagnóstico..."
    )
)

class VvcStudioViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(StudioUiState())
    val uiState: StateFlow<StudioUiState> = _uiState.asStateFlow()

    private val diagnosticEngine = VideoDiagnosticEngine(application)
    private val h264Muxer = H264ToMp4Muxer(application)
    private val sequenceStitcher = SequenceStitcher(application)

    private var currentRepairJob: Job? = null

    init {
        loadVaultVideos()
    }

    fun setTab(tab: StudioTab) {
        _uiState.update { it.copy(activeTab = tab) }
    }

    fun appendTerminalLog(log: String) {
        _uiState.update { current ->
            current.copy(
                globalTerminalLogs = (current.globalTerminalLogs + log).takeLast(60)
            )
        }
    }

    fun selectFile(uri: Uri, name: String, size: Long, isSample: Boolean = false, localPath: String? = null) {
        val ext = name.substringAfterLast('.', "h264").lowercase()
        val file = VideoFile(
            uri = uri,
            name = name,
            sizeBytes = size,
            extension = ext,
            isSample = isSample,
            localPath = localPath
        )

        _uiState.update {
            it.copy(
                selectedFile = file,
                diagnosticResult = null,
                repairJobState = RepairJobState(status = JobStatus.IDLE)
            )
        }

        appendTerminalLog("[IMPORT] Archivo cargado: $name (${file.formattedSize})")
        analyzeFile(file)
    }

    fun analyzeFile(file: VideoFile? = null) {
        val target = file ?: _uiState.value.selectedFile ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isAnalyzing = true) }
            appendTerminalLog("[ANALYZE] Escaneando NAL units y átomos MP4 para ${target.name}...")

            val result = diagnosticEngine.analyze(target.uri, target.name)
            _uiState.update {
                it.copy(
                    diagnosticResult = result,
                    isAnalyzing = false
                )
            }

            appendTerminalLog("[RESULT] Formato: ${result.detectedFormat} | Confianza de reparación: ${result.repairConfidencePercent}%")
            appendTerminalLog("[NAL] SPS: ${result.nalStats.spsCount} | PPS: ${result.nalStats.ppsCount} | IDR: ${result.nalStats.idrKeyframeCount} | Slices: ${result.nalStats.nonIdrSliceCount}")
            if (result.corruptionTypes.isNotEmpty()) {
                result.corruptionTypes.forEach { issue ->
                    appendTerminalLog("[DIAG] Detectado: ${issue.label} (${issue.severity})")
                }
            }
        }
    }

    fun updateConfig(config: RepairConfig) {
        _uiState.update { it.copy(repairConfig = config) }
    }

    fun updatePreset(preset: CameraPreset) {
        _uiState.update {
            it.copy(
                repairConfig = it.repairConfig.copy(
                    preset = preset,
                    targetFps = preset.defaultFps
                )
            )
        }
        appendTerminalLog("[PRESET] Seleccionado: ${preset.name} (${preset.defaultFps} FPS)")
    }

    fun updateFps(fps: Int) {
        _uiState.update {
            it.copy(repairConfig = it.repairConfig.copy(targetFps = fps))
        }
    }

    fun setConversionMode(mode: ConversionEngineMode) {
        _uiState.update {
            it.copy(repairConfig = it.repairConfig.copy(conversionMode = mode))
        }
        appendTerminalLog("[ENGINE] Modo de conversión configurado a: ${mode.label}")
    }

    fun undoChanges() {
        currentRepairJob?.cancel()
        _uiState.update {
            it.copy(
                selectedFile = null,
                diagnosticResult = null,
                repairJobState = RepairJobState(status = JobStatus.IDLE),
                repairConfig = RepairConfig()
            )
        }
        appendTerminalLog("[DESHACER] Cambios revertidos. Archivo deseleccionado y parámetros restaurados a valores iniciales.")
    }

    fun clearSelectedFile() {
        currentRepairJob?.cancel()
        _uiState.update {
            it.copy(
                selectedFile = null,
                diagnosticResult = null,
                repairJobState = RepairJobState(status = JobStatus.IDLE)
            )
        }
        appendTerminalLog("[DESHACER] Selección de archivo descartada. Listo para nuevo análisis.")
    }

    fun startRepair() {
        val file = _uiState.value.selectedFile ?: return
        val config = _uiState.value.repairConfig

        currentRepairJob?.cancel()
        currentRepairJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    repairJobState = RepairJobState(
                        status = JobStatus.REPAIRING,
                        currentStep = "Preparando contenedor MP4...",
                        progress = 0.02f
                    )
                )
            }

            val app = getApplication<Application>()
            val outputDir = File(app.getExternalFilesDir(null) ?: app.filesDir, "repaired_videos")
            outputDir.mkdirs()

            val cleanBaseName = file.name.substringBeforeLast('.')
            val outName = if (config.customOutputName.isNotBlank()) {
                "${config.customOutputName.trim()}.mp4"
            } else {
                "VVC_RESTORED_${cleanBaseName}_${System.currentTimeMillis()}.mp4"
            }
            val outputFile = File(outputDir, outName)

            appendTerminalLog("[CORE] Iniciando reensamblado de video: ${outputFile.name}")

            val result = h264Muxer.convertH264ToMp4(
                inputUri = file.uri,
                outputFile = outputFile,
                config = config,
                onProgress = { jobState ->
                    _uiState.update { it.copy(repairJobState = jobState.copy(status = JobStatus.REPAIRING)) }
                }
            )

            result.onSuccess { savedFile ->
                val record = RepairedVideoRecord(
                    id = System.currentTimeMillis().toString(),
                    originalName = file.name,
                    outputFilePath = savedFile.absolutePath,
                    fileSizeBytes = savedFile.length(),
                    width = _uiState.value.diagnosticResult?.detectedWidth ?: 1920,
                    height = _uiState.value.diagnosticResult?.detectedHeight ?: 1080,
                    durationSeconds = _uiState.value.repairJobState.outputDurationSeconds,
                    totalFrames = _uiState.value.repairJobState.framesProcessed
                )

                _uiState.update {
                    it.copy(
                        repairJobState = it.repairJobState.copy(
                            status = JobStatus.COMPLETED,
                            progress = 1.0f,
                            outputMp4Path = savedFile.absolutePath,
                            outputFileSize = savedFile.length()
                        ),
                        vaultVideos = listOf(record) + it.vaultVideos
                    )
                }
                appendTerminalLog("[SUCCESS] Video reparado y empaquetado a MP4 con éxito: ${savedFile.name}")
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        repairJobState = it.repairJobState.copy(
                            status = JobStatus.FAILED,
                            errorMessage = err.localizedMessage
                        )
                    )
                }
                appendTerminalLog("[ERROR] Falla en la reparación: ${err.message}")
            }
        }
    }

    // Sequence stitching
    fun addSequenceFiles(files: List<VideoFile>) {
        _uiState.update { it.copy(sequenceQueue = it.sequenceQueue + files) }
        appendTerminalLog("[SEQ] Agregados ${files.size} archivos a la cola de secuencia.")
    }

    fun removeSequenceFile(index: Int) {
        _uiState.update {
            val list = it.sequenceQueue.toMutableList()
            if (index in list.indices) {
                list.removeAt(index)
            }
            it.copy(sequenceQueue = list)
        }
    }

    fun clearSequenceQueue() {
        _uiState.update { it.copy(sequenceQueue = emptyList()) }
    }

    fun startSequenceStitch() {
        val queue = _uiState.value.sequenceQueue
        if (queue.isEmpty()) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isStitchingSequence = true,
                    sequenceJobState = RepairJobState(status = JobStatus.REPAIRING, progress = 0.05f)
                )
            }
            appendTerminalLog("[SEQ] Iniciando unión de ${queue.size} segmentos de cámara continua...")

            val app = getApplication<Application>()
            val outputDir = File(app.getExternalFilesDir(null) ?: app.filesDir, "repaired_videos")
            outputDir.mkdirs()
            val outputFile = File(outputDir, "VVC_SEQUENCE_IPC360_${System.currentTimeMillis()}.mp4")

            val pairs = queue.map { Pair(it.uri, it.name) }
            val res = sequenceStitcher.stitchSequences(
                inputUris = pairs,
                outputFile = outputFile,
                config = _uiState.value.repairConfig,
                onProgress = { jobState ->
                    _uiState.update { it.copy(sequenceJobState = jobState.copy(status = JobStatus.REPAIRING)) }
                }
            )

            res.onSuccess { savedFile ->
                val record = RepairedVideoRecord(
                    id = System.currentTimeMillis().toString(),
                    originalName = "Secuencia IPC360 (${queue.size} clips)",
                    outputFilePath = savedFile.absolutePath,
                    fileSizeBytes = savedFile.length(),
                    width = 1920,
                    height = 1080,
                    durationSeconds = _uiState.value.sequenceJobState.outputDurationSeconds,
                    totalFrames = _uiState.value.sequenceJobState.framesProcessed
                )
                _uiState.update {
                    it.copy(
                        isStitchingSequence = false,
                        sequenceJobState = it.sequenceJobState.copy(
                            status = JobStatus.COMPLETED,
                            progress = 1.0f,
                            outputMp4Path = savedFile.absolutePath
                        ),
                        vaultVideos = listOf(record) + it.vaultVideos
                    )
                }
                appendTerminalLog("[SEQ SUCCESS] Secuencia fusionada en MP4: ${savedFile.name}")
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isStitchingSequence = false,
                        sequenceJobState = it.sequenceJobState.copy(
                            status = JobStatus.FAILED,
                            errorMessage = err.message
                        )
                    )
                }
                appendTerminalLog("[SEQ ERROR] Falla al fusionar secuencia: ${err.message}")
            }
        }
    }

    // Samples
    fun loadSampleIpc360() {
        viewModelScope.launch {
            appendTerminalLog("[LAB] Generando flujo Annex B de cámara IPC 360 Home...")
            val sampleFile = SampleVideoGenerator.generateSampleIpc360H264(getApplication())
            val uri = Uri.fromFile(sampleFile)
            selectFile(
                uri = uri,
                name = sampleFile.name,
                size = sampleFile.length(),
                isSample = true,
                localPath = sampleFile.absolutePath
            )
            setTab(StudioTab.STUDIO)
        }
    }

    fun loadSampleBrokenMp4() {
        viewModelScope.launch {
            appendTerminalLog("[LAB] Generando archivo MP4 corrupto sin átomo MOOV...")
            val sampleFile = SampleVideoGenerator.generateCorruptedMissingMoovMp4(getApplication())
            val uri = Uri.fromFile(sampleFile)
            selectFile(
                uri = uri,
                name = sampleFile.name,
                size = sampleFile.length(),
                isSample = true,
                localPath = sampleFile.absolutePath
            )
            setTab(StudioTab.STUDIO)
        }
    }

    // Vault
    private fun loadVaultVideos() {
        viewModelScope.launch {
            val app = getApplication<Application>()
            val outputDir = File(app.getExternalFilesDir(null) ?: app.filesDir, "repaired_videos")
            if (outputDir.exists()) {
                val files = outputDir.listFiles { f -> f.extension.equals("mp4", ignoreCase = true) }
                    ?.sortedByDescending { it.lastModified() }
                    ?.map { f ->
                        RepairedVideoRecord(
                            id = f.name,
                            originalName = f.name,
                            outputFilePath = f.absolutePath,
                            fileSizeBytes = f.length(),
                            width = 1920,
                            height = 1080,
                            durationSeconds = 0.0,
                            totalFrames = 0,
                            timestampMillis = f.lastModified()
                        )
                    } ?: emptyList()
                _uiState.update { it.copy(vaultVideos = files) }
            }
        }
    }

    fun setActivePlayerVideo(video: RepairedVideoRecord?) {
        _uiState.update { it.copy(activePlayerVideo = video) }
    }

    fun shareVideo(context: Context, record: RepairedVideoRecord) {
        try {
            val file = File(record.outputFilePath)
            if (!file.exists()) return
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "video/mp4"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Compartir Video Reparado MP4"))
        } catch (_: Exception) {
            // Fallback generic send
            val file = File(record.outputFilePath)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "video/mp4"
                putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file))
            }
            context.startActivity(Intent.createChooser(intent, "Compartir Video Reparado MP4"))
        }
    }
}
