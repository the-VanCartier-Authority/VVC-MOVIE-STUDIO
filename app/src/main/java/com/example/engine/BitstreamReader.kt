package com.example.engine

/**
 * Robust BitstreamReader for parsing H.264 SPS/PPS syntax elements,
 * including Exponential-Golomb codes (ue and se).
 */
class BitstreamReader(private val bytes: ByteArray) {
    private var byteOffset = 0
    private var bitOffset = 0 // 0 to 7 within current byte

    fun hasMoreBits(): Boolean {
        return byteOffset < bytes.size
    }

    fun readBit(): Int {
        if (byteOffset >= bytes.size) return 0
        val currentByte = bytes[byteOffset].toInt() and 0xFF
        val bit = (currentByte shr (7 - bitOffset)) and 0x01
        bitOffset++
        if (bitOffset == 8) {
            bitOffset = 0
            byteOffset++
            // Skip H.264 emulation prevention bytes (0x00 0x00 0x03)
            if (byteOffset + 1 < bytes.size &&
                bytes[byteOffset - 1].toInt() == 0 &&
                bytes[byteOffset].toInt() == 0 &&
                bytes[byteOffset + 1].toInt() == 3
            ) {
                byteOffset += 2 // skip the 0x03 byte
            }
        }
        return bit
    }

    fun readBits(n: Int): Int {
        var result = 0
        for (i in 0 until n) {
            result = (result shl 1) or readBit()
        }
        return result
    }

    /**
     * Reads unsigned Exponential-Golomb coded integer (ue(v))
     */
    fun readUE(): Int {
        var leadingZeros = 0
        while (readBit() == 0 && leadingZeros < 32 && hasMoreBits()) {
            leadingZeros++
        }
        if (leadingZeros == 0) return 0
        val suffix = readBits(leadingZeros)
        return (1 shl leadingZeros) - 1 + suffix
    }

    /**
     * Reads signed Exponential-Golomb coded integer (se(v))
     */
    fun readSE(): Int {
        val ue = readUE()
        val sign = if (ue % 2 == 1) 1 else -1
        return sign * ((ue + 1) / 2)
    }

    fun skipBits(n: Int) {
        for (i in 0 until n) {
            readBit()
        }
    }
}
