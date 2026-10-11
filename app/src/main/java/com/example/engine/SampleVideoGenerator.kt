package com.example.engine

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.random.Random

object SampleVideoGenerator {

    suspend fun generateSampleIpc360H264(context: Context, frameCount: Int = 120): File = withContext(Dispatchers.IO) {
        val samplesDir = File(context.cacheDir, "sample_surveillance")
        samplesDir.mkdirs()
        val file = File(samplesDir, "ipc360_camera_ch01_raw.h264")
        if (file.exists() && file.length() > 5000) {
            return@withContext file
        }

        FileOutputStream(file).use { fos ->
            val sps = SpsParser.createDefault1080pSps()
            val pps = SpsParser.createDefault1080pPps()

            // Optional IPC360 proprietary frame tag simulation
            fun writeIpcHeader(frameIdx: Int, isKey: Boolean) {
                val header = ByteArray(16)
                header[0] = 0x49 // 'I'
                header[1] = 0x50 // 'P'
                header[2] = 0x43 // 'C'
                header[3] = 0x36 // '6'
                header[4] = 0x30 // '0'
                header[5] = if (isKey) 1 else 0
                header[6] = (frameIdx and 0xFF).toByte()
                header[7] = ((frameIdx shr 8) and 0xFF).toByte()
                fos.write(header)
            }

            // Write initial SPS/PPS
            writeIpcHeader(0, true)
            fos.write(sps)
            fos.write(pps)

            // Generate synthetic frames with GOP size 25
            val gopSize = 25
            for (i in 0 until frameCount) {
                val isIdr = (i % gopSize == 0)
                writeIpcHeader(i, isIdr)

                if (isIdr && i > 0) {
                    // Periodic SPS/PPS in surveillance streams
                    fos.write(sps)
                    fos.write(pps)
                }

                // Start code 00 00 00 01
                fos.write(byteArrayOf(0x00, 0x00, 0x00, 0x01))

                if (isIdr) {
                    // NAL Type 5: IDR Keyframe
                    fos.write(0x65)
                    // Synthesize plausible slice data payload (approx 6-12 KB for keyframe)
                    val keySize = 8192 + Random.nextInt(2048)
                    val dummy = ByteArray(keySize)
                    Random.nextBytes(dummy)
                    // sanitize dummy so no accidental 00 00 00 01 occurs inside payload
                    for (b in dummy.indices) {
                        if (dummy[b].toInt() == 0) dummy[b] = 0x11
                    }
                    fos.write(dummy)
                } else {
                    // NAL Type 1: Non-IDR Slice (P-frame)
                    fos.write(0x41)
                    val pSize = 2048 + Random.nextInt(1024)
                    val dummy = ByteArray(pSize)
                    Random.nextBytes(dummy)
                    for (b in dummy.indices) {
                        if (dummy[b].toInt() == 0) dummy[b] = 0x22
                    }
                    fos.write(dummy)
                }
            }
        }
        file
    }

    suspend fun generateCorruptedMissingMoovMp4(context: Context): File = withContext(Dispatchers.IO) {
        val samplesDir = File(context.cacheDir, "sample_surveillance")
        samplesDir.mkdirs()
        val file = File(samplesDir, "camera_powerloss_cut.mp4")
        if (file.exists() && file.length() > 5000) {
            return@withContext file
        }

        FileOutputStream(file).use { fos ->
            // ftyp box (32 bytes)
            val ftyp = byteArrayOf(
                0x00, 0x00, 0x00, 0x20, // size 32
                0x66, 0x74, 0x79, 0x70, // 'ftyp'
                0x69, 0x73, 0x6F, 0x6D, // 'isom'
                0x00, 0x00, 0x02, 0x00,
                0x69, 0x73, 0x6F, 0x6D, // compatible brands
                0x69, 0x73, 0x6F, 0x32,
                0x61, 0x76, 0x63, 0x31,
                0x6D, 0x70, 0x34, 0x31
            )
            fos.write(ftyp)

            // mdat header with large size
            val mdatHeader = byteArrayOf(
                0x00, 0x10, 0x00, 0x00, // size
                0x6D, 0x64, 0x61, 0x74  // 'mdat'
            )
            fos.write(mdatHeader)

            // Embed Annex B H.264 stream payload inside mdat
            val sps = SpsParser.createDefault1080pSps()
            val pps = SpsParser.createDefault1080pPps()
            fos.write(sps)
            fos.write(pps)

            for (i in 0 until 80) {
                val isIdr = (i % 20 == 0)
                fos.write(byteArrayOf(0x00, 0x00, 0x00, 0x01))
                if (isIdr) {
                    fos.write(0x65)
                    val dummy = ByteArray(4096) { 0x33 }
                    fos.write(dummy)
                } else {
                    fos.write(0x41)
                    val dummy = ByteArray(1500) { 0x44 }
                    fos.write(dummy)
                }
            }
            // Deliberately DO NOT WRITE 'moov' atom to simulate camera power interruption!
        }
        file
    }
}
