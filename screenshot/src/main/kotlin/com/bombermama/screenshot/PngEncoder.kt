package com.bombermama.screenshot

import java.io.ByteArrayOutputStream
import java.util.zip.CRC32
import java.util.zip.Deflater
import java.util.zip.DeflaterOutputStream

/**
 * Кодировщик PNG (ARGB8888) на чистой JVM — без AWT/ImageIO.
 *
 * PNG = сигнатура + чанки IHDR/IDAT/IEND. Пиксельные данные кодируются
 * по строкам: 1 байт фильтра (0 = None) + RGBA на пиксель, затем сжимаются
 * через Deflater (zlib). Каждое поле имеет CRC32.
 */
object PngEncoder {

    private val SIGNATURE = byteArrayOf(
        0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
    )

    fun encode(canvas: PixCanvas): ByteArray {
        val w = canvas.width
        val h = canvas.height
        val px = canvas.pixels

        // Сырые данные с фильтром None по строкам.
        // PNG color type 6 требует порядок байт R,G,B,A (не ARGB).
        val raw = ByteArrayOutputStream(w * h * 4 + h)
        val row = ByteArray(w * 4)
        for (y in 0 until h) {
            raw.write(0)
            var k = 0
            for (x in 0 until w) {
                val c = px[y * w + x]
                row[k++] = (c shr 16 and 0xFF).toByte()
                row[k++] = (c shr 8 and 0xFF).toByte()
                row[k++] = (c and 0xFF).toByte()
                row[k++] = (c ushr 24).toByte()
            }
            raw.write(row)
        }

        val zlib = ByteArrayOutputStream()
        DeflaterOutputStream(zlib, Deflater(Deflater.BEST_COMPRESSION)).use {
            it.write(raw.toByteArray())
        }
        val idat = zlib.toByteArray()

        val out = ByteArrayOutputStream()
        out.write(SIGNATURE)
        writeChunk(out, "IHDR", ihdr(w, h))
        writeChunk(out, "IDAT", idat)
        writeChunk(out, "IEND", ByteArray(0))
        return out.toByteArray()
    }

    private fun ihdr(w: Int, h: Int): ByteArray {
        val b = ByteArray(13)
        b[0] = (w ushr 24).toByte(); b[1] = (w ushr 16).toByte()
        b[2] = (w ushr 8).toByte(); b[3] = w.toByte()
        b[4] = (h ushr 24).toByte(); b[5] = (h ushr 16).toByte()
        b[6] = (h ushr 8).toByte(); b[7] = h.toByte()
        b[8] = 8   // bit depth
        b[9] = 6   // color type RGBA
        b[10] = 0  // compression
        b[11] = 0  // filter
        b[12] = 0  // interlace
        return b
    }

    private fun writeChunk(out: ByteArrayOutputStream, type: String, data: ByteArray) {
        val len = data.size
        out.write((len ushr 24) and 0xFF); out.write((len ushr 16) and 0xFF)
        out.write((len ushr 8) and 0xFF); out.write(len and 0xFF)
        val typeBytes = type.toByteArray(Charsets.US_ASCII)
        out.write(typeBytes)
        out.write(data)
        val crc = CRC32()
        crc.update(typeBytes)
        crc.update(data)
        val v = crc.value
        out.write((v ushr 24).and(0xFFL).toInt())
        out.write((v ushr 16).and(0xFFL).toInt())
        out.write((v ushr 8).and(0xFFL).toInt())
        out.write(v.and(0xFFL).toInt())
    }
}
