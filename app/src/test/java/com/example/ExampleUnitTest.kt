package com.example

import com.example.engine.BitstreamReader
import com.example.engine.H264NalAnalyzer
import com.example.engine.SpsParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testBitstreamReaderExpGolomb() {
    // 0 = '1'
    val readerZero = BitstreamReader(byteArrayOf(0x80.toByte()))
    assertEquals(0, readerZero.readUE())

    // 1 = '010'
    val readerOne = BitstreamReader(byteArrayOf(0x40.toByte()))
    assertEquals(1, readerOne.readUE())
  }

  @Test
  fun testSpsParserDefault1080p() {
    val spsBytes = SpsParser.createDefault1080pSps()
    val spsInfo = SpsParser.parse(spsBytes)
    assertNotNull(spsInfo)
    assertEquals(1920, spsInfo?.width)
    assertEquals(1080, spsInfo?.height)
  }

  @Test
  fun testH264NalAnalyzerExtractsUnits() {
    val sps = SpsParser.createDefault1080pSps()
    val pps = SpsParser.createDefault1080pPps()
    val combined = ByteArray(sps.size + pps.size)
    System.arraycopy(sps, 0, combined, 0, sps.size)
    System.arraycopy(pps, 0, combined, sps.size, pps.size)

    val analyzer = H264NalAnalyzer()
    val units = analyzer.extractNalUnits(ByteArrayInputStream(combined))
    assertTrue(units.size >= 2)
    assertEquals(7, units[0].type)
    assertEquals(8, units[1].type)
  }

  @Test
  fun testConversionEngineModeValues() {
    val modes = com.example.model.ConversionEngineMode.values()
    assertEquals(2, modes.size)
    assertEquals(com.example.model.ConversionEngineMode.REMUX_DIRECT, modes[0])
    assertEquals(com.example.model.ConversionEngineMode.MEDIACODEC_TRANSCODE, modes[1])
  }

  @Test
  fun testMediaCodecPresetAvailable() {
    val presets = com.example.model.CameraPreset.PRESETS
    val mediaCodecPreset = presets.find { it.id == "mediacodec_deep" }
    assertNotNull(mediaCodecPreset)
    assertEquals(30, mediaCodecPreset?.defaultFps)
  }
}
