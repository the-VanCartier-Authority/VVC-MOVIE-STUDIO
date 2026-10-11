package com.example.engine

import java.io.InputStream

data class NalUnit(
    val type: Int, // 7=SPS, 8=PPS, 5=IDR, 1=Non-IDR, 6=SEI, 9=AUD
    val isKeyframe: Boolean,
    val data: ByteArray,
    val offsetInStream: Long
) {
    val typeName: String
        get() = when (type) {
            7 -> "SPS (Parámetros de Secuencia)"
            8 -> "PPS (Parámetros de Imagen)"
            5 -> "IDR (Cuadro Clave / I-Frame)"
            1 -> "Slice (Cuadro P/B Diferencial)"
            6 -> "SEI (Metadatos Suplementarios)"
            9 -> "AUD (Delimitador de Unidad de Acceso)"
            else -> "NAL Tipo $type"
        }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as NalUnit
        return type == other.type &&
                isKeyframe == other.isKeyframe &&
                offsetInStream == other.offsetInStream &&
                data.contentEquals(other.data)
    }

    override fun hashCode(): Int {
        var result = type
        result = 31 * result + isKeyframe.hashCode()
        result = 31 * result + offsetInStream.hashCode()
        result = 31 * result + data.contentHashCode()
        return result
    }
}

class H264NalAnalyzer(private val stripIpcHeaders: Boolean = true) {

    /**
     * Efficiently scans a byte array or stream for NAL units.
     * Identifies Annex B start codes (00 00 00 01 or 00 00 01)
     * and handles IPC 360 camera packet headers (e.g. DHAV, HK40, or frame prefixes).
     */
    fun extractNalUnits(
        inputStream: InputStream,
        maxUnits: Int = Int.MAX_VALUE,
        onNalFound: ((NalUnit) -> Unit)? = null
    ): List<NalUnit> {
        val result = mutableListOf<NalUnit>()
        val bufferSize = 64 * 1024
        val buffer = ByteArray(bufferSize)
        var carryOver = ByteArray(0)
        var streamOffset = 0L

        while (result.size < maxUnits) {
            val bytesRead = inputStream.read(buffer)
            if (bytesRead <= 0) break

            val currentChunk = ByteArray(carryOver.size + bytesRead)
            System.arraycopy(carryOver, 0, currentChunk, 0, carryOver.size)
            System.arraycopy(buffer, 0, currentChunk, carryOver.size, bytesRead)

            val startIndices = findStartCodes(currentChunk)
            if (startIndices.isEmpty()) {
                // Keep last 4 bytes in case start code is split across buffers
                val keepLen = minOf(currentChunk.size, 4)
                carryOver = currentChunk.copyOfRange(currentChunk.size - keepLen, currentChunk.size)
                streamOffset += currentChunk.size - keepLen
                continue
            }

            for (i in 0 until startIndices.size - 1) {
                val start = startIndices[i]
                val end = startIndices[i + 1]
                val nalBytes = currentChunk.copyOfRange(start, end)
                val nalUnit = parseNalUnit(nalBytes, streamOffset + start)
                if (nalUnit != null) {
                    result.add(nalUnit)
                    onNalFound?.invoke(nalUnit)
                    if (result.size >= maxUnits) break
                }
            }

            val lastStart = startIndices.last()
            carryOver = currentChunk.copyOfRange(lastStart, currentChunk.size)
            streamOffset += lastStart
        }

        // Process leftover buffer at EOF
        if (carryOver.isNotEmpty() && result.size < maxUnits) {
            val nalUnit = parseNalUnit(carryOver, streamOffset)
            if (nalUnit != null) {
                result.add(nalUnit)
                onNalFound?.invoke(nalUnit)
            }
        }

        return result
    }

    private fun findStartCodes(data: ByteArray): List<Int> {
        val indices = mutableListOf<Int>()
        var i = 0
        val maxIndex = data.size - 4
        while (i <= maxIndex) {
            if (data[i].toInt() == 0 && data[i + 1].toInt() == 0) {
                if (data[i + 2].toInt() == 1) {
                    // 3-byte start code 00 00 01
                    indices.add(i)
                    i += 3
                    continue
                } else if (data[i + 2].toInt() == 0 && data[i + 3].toInt() == 1) {
                    // 4-byte start code 00 00 00 01
                    indices.add(i)
                    i += 4
                    continue
                }
            }
            i++
        }
        return indices
    }

    private fun parseNalUnit(nalBytes: ByteArray, offset: Long): NalUnit? {
        if (nalBytes.size < 4) return null

        var headerOffset = 0
        if (nalBytes[0].toInt() == 0 && nalBytes[1].toInt() == 0) {
            headerOffset = if (nalBytes[2].toInt() == 1) 3 else 4
        }
        if (headerOffset >= nalBytes.size) return null

        val headerByte = nalBytes[headerOffset].toInt() and 0xFF
        val forbiddenBit = (headerByte shr 7) and 0x01
        if (forbiddenBit != 0) {
            // Corrupted NAL header
            return null
        }

        val nalType = headerByte and 0x1F
        val isKeyframe = nalType == 5 || nalType == 7

        // Ensure 4-byte start code at the beginning for standardization
        val normalizedBytes = if (headerOffset == 4) {
            nalBytes
        } else {
            val normalized = ByteArray(nalBytes.size - headerOffset + 4)
            normalized[0] = 0
            normalized[1] = 0
            normalized[2] = 0
            normalized[3] = 1
            System.arraycopy(nalBytes, headerOffset, normalized, 4, nalBytes.size - headerOffset)
            normalized
        }

        return NalUnit(
            type = nalType,
            isKeyframe = isKeyframe,
            data = normalizedBytes,
            offsetInStream = offset
        )
    }

    /**
     * Inspects if bytes contain IPC 360 / DHAV / Xiongmai proprietary frame wrappers.
     */
    fun hasIpc360Headers(sample: ByteArray): Boolean {
        if (sample.size < 16) return false
        val s = String(sample.copyOfRange(0, minOf(sample.size, 64)), Charsets.ISO_8859_1)
        return s.contains("DHAV") || s.contains("HK40") || s.contains("IPC") ||
                // Check if start code is consistently preceded by fixed 16/24/32 byte blocks
                (sample[0].toInt() == 0x44 && sample[1].toInt() == 0x48) ||
                (sample[0].toInt() == 0x48 && sample[1].toInt() == 0x4B)
    }
}
