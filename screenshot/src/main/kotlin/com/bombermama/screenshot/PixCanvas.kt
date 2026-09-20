package com.bombermama.screenshot

/**
 * Минимальный пиксельный растеризатор холста (замена android.graphics.Canvas).
 *
 * Работает напрямую с IntArray ARGB8888 — без AWT, без шрифтов, без нативных
 * библиотек. Поддерживает только то, что нужно для рендера игры:
 *  - drawRect (заливка, поэтому Robolectric-проблема с "контурами" отпадает);
 *  - drawBitmap с масштабированием по ближайшему соседу (как в игре);
 *  - попиксельная альфа-смесь для полупрозрачных цветов;
 *  - пиксельный текст 5x7 для латиницы, кириллицы, цифр и знаков.
 *
 * Все координаты — с округлением к целым пикселям, как требует pixel-art.
 */
class PixCanvas(val width: Int, val height: Int) {

    val pixels: IntArray = IntArray(width * height)

    fun clear(color: Int) {
        java.util.Arrays.fill(pixels, color)
    }

    /** Заливка прямоугольника (склейка альфы для полупрозрачных цветов). */
    fun drawRect(left: Float, top: Float, right: Float, bottom: Float, color: Int) {
        if (color == 0) return
        val a = (color ushr 24) and 0xFF
        if (a == 0) return
        val l = clampI(left.toInt(), 0, width)
        val t = clampI(top.toInt(), 0, height)
        val r = clampI(Math.ceil(right.toDouble()).toInt(), 0, width)
        val b = clampI(Math.ceil(bottom.toDouble()).toInt(), 0, height)
        if (a == 0xFF) {
            for (y in t until b) {
                val row = y * width
                for (x in l until r) pixels[row + x] = color
            }
        } else {
            for (y in t until b) {
                val row = y * width
                for (x in l until r) pixels[row + x] = blend(pixels[row + x], color, a)
            }
        }
    }

    /**
     * Рисует [src] (другой холст-тайл), масштабируя в [dstW]x[dstH]
     * методом ближайшего соседа — точно как blit() в GameRenderer.
     */
    fun drawBitmap(src: PixCanvas, dstX: Float, dstY: Float, dstW: Float, dstH: Float) {
        val dx0 = clampI(dstX.toInt(), 0, width)
        val dy0 = clampI(dstY.toInt(), 0, height)
        val dx1 = clampI(Math.ceil((dstX + dstW).toDouble()).toInt(), 0, width)
        val dy1 = clampI(Math.ceil((dstY + dstH).toDouble()).toInt(), 0, height)
        if (dx1 <= dx0 || dy1 <= dy0) return
        val sw = src.width
        val sh = src.height
        for (y in dy0 until dy1) {
            val srcY = ((y - dstY) * sh / dstH).toInt().coerceIn(0, sh - 1)
            val srcRow = srcY * sw
            val dstRow = y * width
            for (x in dx0 until dx1) {
                val srcX = ((x - dstX) * sw / dstW).toInt().coerceIn(0, sw - 1)
                val c = src.pixels[srcRow + srcX]
                if (c == 0) continue
                val a = (c ushr 24) and 0xFF
                if (a == 0xFF) pixels[dstRow + x] = c
                else if (a > 0) pixels[dstRow + x] = blend(pixels[dstRow + x], c, a)
            }
        }
    }

    // ===================== Текст (пиксельный 5x7) =====================

    fun drawText(text: String, x: Float, y: Float, size: Float, color: Int, center: Boolean = false) {
        // Пропускаем эмодзи (в скриншотах UI-иконки рисуются отдельно).
        val glyphs = text.mapNotNull { Glyph.of(it) }
        val totalW = glyphs.sumOf { it.width + 1 } * size
        var cx = if (center) x - totalW / 2f else x
        for (g in glyphs) {
            for (row in 0 until 7) {
                val bits = g.rows[row]
                for (col in 0 until 5) {
                    if ((bits shr (4 - col)) and 1 == 1) {
                        drawRect(cx + col * size, y + row * size, cx + (col + 1) * size, y + (row + 1) * size, color)
                    }
                }
            }
            cx += (g.width + 1) * size
        }
    }

    fun textWidth(text: String, size: Float): Float =
        text.mapNotNull { Glyph.of(it) }.sumOf { it.width + 1 } * size

    // ===================== Утилиты =====================

    private fun clampI(v: Int, lo: Int, hi: Int) = if (v < lo) lo else if (v > hi) hi else v

    private fun blend(dst: Int, src: Int, sa: Int): Int {
        val sr = (src shr 16) and 0xFF
        val sg = (src shr 8) and 0xFF
        val sb = src and 0xFF
        val alpha = sa / 255f
        val inv = 1f - alpha
        val dr = ((dst shr 16) and 0xFF) * inv + sr * alpha
        val dg = ((dst shr 8) and 0xFF) * inv + sg * alpha
        val db = (dst and 0xFF) * inv + sb * alpha
        return (0xFF shl 24) or (dr.toInt() shl 16) or (dg.toInt() shl 8) or db.toInt()
    }
}
