package com.recoverx.pro.engine

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayInputStream

class ImageSignatureTest {
    @Test fun findsJpeg() {
        val fake = byteArrayOf(1, 2, 3, 0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 9, 8, 7, 0xFF.toByte(), 0xD9.toByte(), 4)
        val result = ImageSignature.findNext(ByteArrayInputStream(fake))!!
        assertEquals(ImageSignature.Type.JPEG, result.first)
        assertEquals(8, result.second.size)
    }

    @Test fun findsPng() {
        val start = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
        val end = byteArrayOf(0x49, 0x45, 0x4E, 0x44, 0xAE.toByte(), 0x42, 0x60, 0x82.toByte())
        val data = start + byteArrayOf(1, 2, 3) + end
        val result = ImageSignature.findNext(ByteArrayInputStream(data))!!
        assertEquals(ImageSignature.Type.PNG, result.first)
        assertEquals(data.size, result.second.size)
    }
}
