package com.example.engine

import android.util.Log

data class SpsInfo(
    val profileIdc: Int,
    val levelIdc: Int,
    val width: Int,
    val height: Int,
    val frameMbsOnlyFlag: Boolean,
    val rawBytes: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as SpsInfo
        return profileIdc == other.profileIdc &&
                levelIdc == other.levelIdc &&
                width == other.width &&
                height == other.height &&
                rawBytes.contentEquals(other.rawBytes)
    }

    override fun hashCode(): Int {
        var result = profileIdc
        result = 31 * result + levelIdc
        result = 31 * result + width
        result = 31 * result + height
        result = 31 * result + rawBytes.contentHashCode()
        return result
    }
}

object SpsParser {
    private const val TAG = "SpsParser"

    fun parse(spsNalBytes: ByteArray): SpsInfo? {
        return try {
            // Remove start code if present
            var offset = 0
            if (spsNalBytes.size > 4 && spsNalBytes[0].toInt() == 0 && spsNalBytes[1].toInt() == 0) {
                offset = if (spsNalBytes[2].toInt() == 1) 3 else 4
            }
            if (offset >= spsNalBytes.size) return null

            val nalHeader = spsNalBytes[offset].toInt() and 0xFF
            val nalType = nalHeader and 0x1F
            if (nalType != 7) {
                // Not SPS
                return null
            }

            val payload = spsNalBytes.copyOfRange(offset + 1, spsNalBytes.size)
            val reader = BitstreamReader(payload)

            val profileIdc = reader.readBits(8)
            val constraintSetFlags = reader.readBits(8)
            val levelIdc = reader.readBits(8)
            val seqParameterSetId = reader.readUE()

            if (profileIdc == 100 || profileIdc == 110 || profileIdc == 122 ||
                profileIdc == 244 || profileIdc == 44 || profileIdc == 83 ||
                profileIdc == 86 || profileIdc == 118 || profileIdc == 128
            ) {
                val chromaFormatIdc = reader.readUE()
                if (chromaFormatIdc == 3) {
                    reader.skipBits(1) // separate_colour_plane_flag
                }
                reader.readUE() // bit_depth_luma_minus8
                reader.readUE() // bit_depth_chroma_minus8
                reader.skipBits(1) // qpprime_y_zero_transform_bypass_flag
                val seqScalingMatrixPresentFlag = reader.readBit()
                if (seqScalingMatrixPresentFlag == 1) {
                    val count = if (chromaFormatIdc != 3) 8 else 12
                    for (i in 0 until count) {
                        val seqScalingListPresentFlag = reader.readBit()
                        if (seqScalingListPresentFlag == 1) {
                            skipScalingList(reader, if (i < 6) 16 else 64)
                        }
                    }
                }
            }

            val log2MaxFrameNumMinus4 = reader.readUE()
            val picOrderCntType = reader.readUE()
            if (picOrderCntType == 0) {
                reader.readUE() // log2_max_pic_order_cnt_lsb_minus4
            } else if (picOrderCntType == 1) {
                reader.skipBits(1) // delta_pic_order_always_zero_flag
                reader.readSE() // offset_for_non_ref_pic
                reader.readSE() // offset_for_top_to_bottom_field
                val numRefFramesInPicOrderCntCycle = reader.readUE()
                for (i in 0 until numRefFramesInPicOrderCntCycle) {
                    reader.readSE() // offset_for_ref_frame
                }
            }

            val maxNumRefFrames = reader.readUE()
            reader.skipBits(1) // gaps_in_frame_num_value_allowed_flag

            val picWidthInMbsMinus1 = reader.readUE()
            val picHeightInMapUnitsMinus1 = reader.readUE()
            val frameMbsOnlyFlag = reader.readBit() == 1

            if (!frameMbsOnlyFlag) {
                reader.skipBits(1) // mb_adaptive_frame_field_flag
            }
            reader.skipBits(1) // direct_8x8_inference_flag

            var cropLeft = 0
            var cropRight = 0
            var cropTop = 0
            var cropBottom = 0

            val frameCroppingFlag = reader.readBit() == 1
            if (frameCroppingFlag) {
                cropLeft = reader.readUE()
                cropRight = reader.readUE()
                cropTop = reader.readUE()
                cropBottom = reader.readUE()
            }

            var width = (picWidthInMbsMinus1 + 1) * 16 - (cropLeft + cropRight) * 2
            val heightMultiplier = if (frameMbsOnlyFlag) 1 else 2
            var height = (picHeightInMapUnitsMinus1 + 1) * 16 * heightMultiplier - (cropTop + cropBottom) * 2

            // Sanity check resolution
            if (width <= 0 || width > 7680) width = 1920
            if (height <= 0 || height > 4320) height = 1080

            SpsInfo(
                profileIdc = profileIdc,
                levelIdc = levelIdc,
                width = width,
                height = height,
                frameMbsOnlyFlag = frameMbsOnlyFlag,
                rawBytes = spsNalBytes
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed parsing SPS: ${e.message}")
            null
        }
    }

    private fun skipScalingList(reader: BitstreamReader, sizeOfScalingList: Int) {
        var lastScale = 8
        var nextScale = 8
        for (j in 0 until sizeOfScalingList) {
            if (nextScale != 0) {
                val deltaScale = reader.readSE()
                nextScale = (lastScale + deltaScale + 256) % 256
            }
            lastScale = if (nextScale == 0) lastScale else nextScale
        }
    }

    /**
     * Fallback standard SPS bytes for 1080p surveillance video
     * Profile: Baseline (66) or High (100) Level 4.0 1920x1080
     */
    fun createDefault1080pSps(): ByteArray {
        return byteArrayOf(
            0x00, 0x00, 0x00, 0x01,
            0x67, 0x42, 0x00, 0x28, 0x96.toByte(), 0x35, 0x40, 0xF0.toByte(),
            0x04, 0x4F, 0xCB.toByte(), 0x37, 0x01, 0x01, 0x01, 0x40
        )
    }

    fun createDefault1080pPps(): ByteArray {
        return byteArrayOf(
            0x00, 0x00, 0x00, 0x01,
            0x68, 0xCE.toByte(), 0x38, 0x80.toByte()
        )
    }

    fun createDefault720pSps(): ByteArray {
        return byteArrayOf(
            0x00, 0x00, 0x00, 0x01,
            0x67, 0x42, 0x00, 0x1F, 0x96.toByte(), 0x35, 0x40, 0xA0.toByte(),
            0x0B, 0x74.toByte(), 0xDC.toByte(), 0x04.toByte(), 0x04.toByte(), 0x05.toByte(), 0x00
        )
    }
}
