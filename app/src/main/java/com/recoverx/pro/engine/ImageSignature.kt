package com.recoverx.pro.engine

import java.io.InputStream

/** Small streaming signature helper used by the forensic file-carving scanner. */
object ImageSignature {
    private val jpegStart = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte())
    private val jpegEnd = byteArrayOf(0xFF.toByte(), 0xD9.toByte())
    private val pngStart = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
    private val pngEnd = byteArrayOf(0x49, 0x45, 0x4E, 0x44, 0xAE.toByte(), 0x42, 0x60, 0x82.toByte())

    enum class Type { JPEG, PNG }

    fun findNext(input: InputStream, chunkSize: Int = 64 * 1024, maxImageBytes: Long = 40L * 1024L * 1024L): Pair<Type, ByteArray>? {
        // The returned byte array is a bounded extraction; this is intended for test-sized and forensic dump inputs.
        val buffer = ByteArray(chunkSize)
        val window = ByteArray(8)
        var windowSize = 0
        var consumed = 0L
        var jpeg = false
        var png = false
        val out = java.io.ByteArrayOutputStream()

        fun push(b: Byte) {
            if (windowSize < window.size) window[windowSize++] = b
            else {
                System.arraycopy(window, 1, window, 0, window.size - 1)
                window[window.lastIndex] = b
            }
        }
        fun endsWith(sig: ByteArray): Boolean {
            if (windowSize < sig.size) return false
            val offset = windowSize - sig.size
            for (i in sig.indices) if (window[offset + i] != sig[i]) return false
            return true
        }

        while (true) {
            val read = input.read(buffer)
            if (read <= 0) break
            for (i in 0 until read) {
                val b = buffer[i]
                consumed++
                if (!jpeg && !png) {
                    push(b)
                    if (endsWith(jpegStart)) {
                        jpeg = true
                        out.reset()
                        out.write(jpegStart)
                    } else if (endsWith(pngStart)) {
                        png = true
                        out.reset()
                        out.write(pngStart)
                    }
                } else {
                    out.write(b.toInt())
                    if (out.size().toLong() > maxImageBytes) {
                        jpeg = false; png = false; out.reset(); windowSize = 0
                    } else {
                        push(b)
                        if (jpeg && endsWith(jpegEnd)) return Type.JPEG to out.toByteArray()
                        if (png && endsWith(pngEnd)) return Type.PNG to out.toByteArray()
                    }
                }
            }
        }
        return null
    }
}
