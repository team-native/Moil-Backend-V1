package com.teamnative.moil.domain.image.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

class ProfileColorExtractorTest {

    private val extractor = ProfileColorExtractor()

    @Test
    fun `maps the largest RGB cluster to the nearest Moil color`() {
        val image = BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB)
        for (y in 0 until image.height) {
            for (x in 0 until image.width) {
                image.setRGB(x, y, if (x < 8) Color.RED.rgb else Color.BLUE.rgb)
            }
        }

        assertEquals("VIVID_RED", extractor.extract(image.toPngBytes()))
    }

    @Test
    fun `falls back when the image cannot be decoded`() {
        assertEquals("RED", extractor.extract(byteArrayOf(1, 2, 3)))
    }

    private fun BufferedImage.toPngBytes(): ByteArray = ByteArrayOutputStream().use { output ->
        check(ImageIO.write(this, "png", output))
        output.toByteArray()
    }
}
