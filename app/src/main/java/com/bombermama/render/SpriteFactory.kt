package com.bombermama.render

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import kotlin.math.min

/**
 * Генератор пиксельной графики.
 *
 * Все спрайты рисуются процедурно на холсте фиксированного размера
 * с использованием единой палитры [Palette]. Это даёт:
 *  - единый визуальный стиль (никаких "случайных несогласованных ассетов");
 *  - малый вес APK (никаких растровых ресурсов);
 *  - масштабируемость под любые плотности экрана.
 *
 * Спрайты кешируются в [cache], чтобы не пересоздаваться каждый кадр.
 */
class SpriteFactory(private val scale: Int = 4) {

    private val cache = HashMap<String, Bitmap>()

    /** Размер "пикселя" искусства в обычных пикселях экрана. */
    val pixelSize: Int get() = scale

    // =========================================================================
    // Пол и стены
    // =========================================================================

    fun floorTile(dark: Boolean): Bitmap = getOrPut("floor_$dark") {
        val size = TILE * scale
        val s = size.toFloat()
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val p = Paint().apply { isAntiAlias = false }
        // База — трава двух оттенков в шахматном порядке.
        val base = if (dark) Palette.FLOOR_A else Palette.FLOOR_B
        p.color = base
        c.drawRect(0f, 0f, s, s, p)
        // Травяные "лужайки": короткие штрихи-травинки.
        val blade = if (dark) Palette.FLOOR_B else Palette.FLOOR_A
        p.color = blade
        val pxs = scale.toFloat()
        val blades = if (dark) {
            arrayOf(
                intArrayOf(2, 4, 3, 6), intArrayOf(9, 2, 10, 4), intArrayOf(11, 9, 12, 11),
                intArrayOf(4, 11, 5, 13), intArrayOf(13, 12, 14, 14)
            )
        } else {
            arrayOf(
                intArrayOf(3, 2, 4, 4), intArrayOf(10, 4, 11, 6), intArrayOf(5, 9, 6, 11),
                intArrayOf(12, 11, 13, 13), intArrayOf(2, 12, 3, 14)
            )
        }
        for (b in blades) {
            c.drawRect(b[0] * pxs, b[1] * pxs, b[2] * pxs, b[3] * pxs, p)
        }
        // Тёмная каёмка-шов между плитками (только сверху и слева).
        p.color = Palette.FLOOR_LINE
        c.drawRect(0f, 0f, s, pxs * 0.6f, p)
        c.drawRect(0f, 0f, pxs * 0.6f, s, p)
        // Светлая нижняя трава-блик.
        p.color = if (dark) Palette.FLOOR_A else Palette.FLOOR_B
        c.drawRect(pxs * 6f, s - pxs * 1.4f, pxs * 10f, s - pxs * 0.6f, p)
        bmp
    }

    fun wallTile(): Bitmap = getOrPut("wall") {
        val size = TILE * scale
        val s = size.toFloat()
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val p = Paint().apply { isAntiAlias = false }
        // Базовый камень.
        p.color = Palette.WALL_MID
        c.drawRect(0f, 0f, s, s, p)
        // Верхняя фаска-блик (свет слева сверху).
        p.color = Palette.WALL_LIGHT
        c.drawRect(0f, 0f, s, scale * 2f, p)
        c.drawRect(0f, 0f, scale * 2f, s, p)
        // Внутренняя плоскость.
        p.color = Palette.WALL_MID
        c.drawRect(scale * 2f, scale * 2f, s - scale * 2f, s - scale * 2f, p)
        // Нижняя правая тень-фаска.
        p.color = Palette.WALL_DARK
        c.drawRect(0f, s - scale * 2f, s, s, p)
        c.drawRect(s - scale * 2f, scale * 2f, s, s - scale * 2f, p)
        // Глубокая каёмка по самому краю.
        p.color = Palette.WALL_DEEP
        c.drawRect(0f, 0f, s, scale * 0.6f, p)
        c.drawRect(0f, 0f, scale * 0.6f, s, p)
        c.drawRect(0f, s - scale * 0.6f, s, s, p)
        c.drawRect(s - scale * 0.6f, 0f, s, s, p)
        // Лёгкая текстура камня: пара тёмных вкраплений.
        p.color = Palette.WALL_DARK
        c.drawRect(scale * 6f, scale * 6f, scale * 7f, scale * 7f, p)
        c.drawRect(scale * 10f, scale * 10f, scale * 11f, scale * 11f, p)
        p.color = Palette.WALL_LIGHT
        c.drawRect(scale * 5f, scale * 9f, scale * 6f, scale * 10f, p)
        bmp
    }

    fun blockTile(): Bitmap = getOrPut("block") {
        val size = TILE * scale
        val s = size.toFloat()
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val p = Paint().apply { isAntiAlias = false }
        // Базовый кирпич со скруглёнными визуально краями.
        p.color = Palette.BLOCK_MID
        c.drawRect(0f, 0f, s, s, p)
        // Верхний блик.
        p.color = Palette.BLOCK_LIGHT
        c.drawRect(scale * 1f, scale * 1f, s - scale * 1f, scale * 2.4f, p)
        c.drawRect(scale * 1f, scale * 1f, scale * 2.4f, s - scale * 1f, p)
        // Нижняя тень.
        p.color = Palette.BLOCK_DARK
        c.drawRect(scale * 1f, s - scale * 2.4f, s - scale * 1f, s - scale * 1f, p)
        c.drawRect(s - scale * 2.4f, scale * 1f, s - scale * 1f, s - scale * 1f, p)
        // Тёмные швы кирпичной кладки.
        p.color = Palette.BLOCK_DEEP
        c.drawRect(0f, scale * 7.4f, s, scale * 8.2f, p)
        c.drawRect(0f, scale * 15.2f, s, scale * 16f, p)
        c.drawRect(scale * 7.4f, scale * 8.2f, scale * 8.2f, scale * 15.2f, p)
        // Кирпичики: мелкие светлые прямоугольники для рельефа.
        p.color = Palette.BLOCK_LIGHT
        c.drawRect(scale * 2f, scale * 3f, scale * 5f, scale * 3.8f, p)
        c.drawRect(scale * 10f, scale * 3f, scale * 13f, scale * 3.8f, p)
        c.drawRect(scale * 3f, scale * 11f, scale * 6f, scale * 11.8f, p)
        c.drawRect(scale * 11f, scale * 11f, scale * 14f, scale * 11.8f, p)
        bmp
    }

    // =========================================================================
    // Бомба
    // =========================================================================

    /**
     * @param frame 0 или 1 — фаза пульсации (мигание учащается к взрыву).
     */
    fun bomb(frame: Int): Bitmap = getOrPut("bomb_$frame") {
        val size = TILE * scale
        val s = size.toFloat()
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val p = Paint().apply { isAntiAlias = false }
        // Тень под бомбой.
        p.color = Color.argb(90, 20, 24, 50)
        drawPixelEllipse(c, s / 2f, s * 0.86f, s * 0.26f, s * 0.06f, p)
        // Корпус — круг из "пикселей".
        val cx = s / 2f
        val cy = s * 0.58f
        val r = s * (if (frame == 0) 0.30f else 0.33f)
        drawPixelCircle(c, cx, cy, r, Palette.BOMB_MID, p)
        // Нижняя тень корпуса.
        drawPixelCircle(c, cx + r * 0.18f, cy + r * 0.22f, r * 0.62f, Palette.BOMB_DARK, p)
        // Верхний блик.
        drawPixelCircle(c, cx - r * 0.28f, cy - r * 0.30f, r * 0.42f, Palette.BOMB_LIGHT, p)
        drawPixelCircle(c, cx - r * 0.32f, cy - r * 0.34f, r * 0.20f, Color.argb(200, 220, 226, 255), p)
        // Фитиль.
        p.color = Palette.FUSE
        c.drawRect(cx - scale * 0.6f, cy - r - scale * 4f, cx + scale * 0.6f, cy - r, p)
        // Искра.
        if (frame == 1) {
            p.color = Palette.SPARK
            c.drawRect(cx - scale * 1.5f, cy - r - scale * 6f, cx + scale * 1.5f, cy - r - scale * 3f, p)
            p.color = Palette.SPARK_HOT
            c.drawRect(cx - scale * 0.6f, cy - r - scale * 5f, cx + scale * 0.6f, cy - r - scale * 4f, p)
            p.color = Color.argb(230, 255, 255, 255)
            c.drawRect(cx - scale * 0.3f, cy - r - scale * 4.6f, cx + scale * 0.3f, cy - r - scale * 4f, p)
        }
        bmp
    }

    // =========================================================================
    // Взрыв
    // =========================================================================

    /**
     * Сегмент взрыва.
     * @param kind 0=центр, 1=рукав, 2=кончик
     * @param axis 0=горизонталь, 1=вертикаль (для рукава/кончика)
     * @param frame 0..2 фаза анимации
     */
    fun explosion(kind: Int, axis: Int, frame: Int): Bitmap =
        getOrPut("expl_${kind}_${axis}_$frame") {
            val size = TILE * scale
            val s = size.toFloat()
            val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val c = Canvas(bmp)
            val p = Paint().apply { isAntiAlias = false }

            // Цвета по фазе: 0 — яркое ядро, 1 — среднее, 2 — затухание.
            val (core, mid, outer) = when (frame) {
                0 -> Triple(Palette.EXPLO_CORE, Palette.EXPLO_INNER, Palette.EXPLO_MID)
                1 -> Triple(Palette.EXPLO_INNER, Palette.EXPLO_MID, Palette.EXPLO_OUTER)
                else -> Triple(Palette.EXPLO_MID, Palette.EXPLO_OUTER, Palette.EXPLO_FADE)
            }

            when (kind) {
                0 -> { // Центр: круглое ядро.
                    drawPixelCircle(c, s / 2f, s / 2f, s * 0.40f, outer, p)
                    drawPixelCircle(c, s / 2f, s / 2f, s * 0.30f, mid, p)
                    drawPixelCircle(c, s / 2f, s / 2f, s * 0.18f, core, p)
                }
                1 -> { // Рукав: полоса с утолщением в центре.
                    val thickness = s * 0.20f
                    if (axis == 0) {
                        p.color = outer
                        c.drawRect(0f, s / 2f - thickness, s.toFloat(), s / 2f + thickness, p)
                        p.color = mid
                        c.drawRect(0f, s / 2f - thickness * 0.65f, s.toFloat(), s / 2f + thickness * 0.65f, p)
                        p.color = core
                        c.drawRect(0f, s / 2f - thickness * 0.30f, s.toFloat(), s / 2f + thickness * 0.30f, p)
                    } else {
                        p.color = outer
                        c.drawRect(s / 2f - thickness, 0f, s / 2f + thickness, s.toFloat(), p)
                        p.color = mid
                        c.drawRect(s / 2f - thickness * 0.65f, 0f, s / 2f + thickness * 0.65f, s.toFloat(), p)
                        p.color = core
                        c.drawRect(s / 2f - thickness * 0.30f, 0f, s / 2f + thickness * 0.30f, s.toFloat(), p)
                    }
                }
                2 -> { // Кончик: закруглённое окончание.
                    val thickness = s * 0.20f
                    if (axis == 0) {
                        p.color = outer
                        c.drawRect(s / 2f, s / 2f - thickness, s.toFloat(), s / 2f + thickness, p)
                        drawPixelCircle(c, s / 2f, s / 2f, thickness, outer, p)
                        p.color = mid
                        c.drawRect(s / 2f, s / 2f - thickness * 0.6f, s.toFloat(), s / 2f + thickness * 0.6f, p)
                        drawPixelCircle(c, s / 2f, s / 2f, thickness * 0.6f, mid, p)
                    } else {
                        p.color = outer
                        c.drawRect(s / 2f - thickness, s / 2f, s / 2f + thickness, s.toFloat(), p)
                        drawPixelCircle(c, s / 2f, s / 2f, thickness, outer, p)
                        p.color = mid
                        c.drawRect(s / 2f - thickness * 0.6f, s / 2f, s / 2f + thickness * 0.6f, s.toFloat(), p)
                        drawPixelCircle(c, s / 2f, s / 2f, thickness * 0.6f, mid, p)
                    }
                }
            }
            bmp
        }

    // =========================================================================
    // Бонусы
    // =========================================================================

    fun bonus(type: com.bombermama.core.BonusType, frame: Int): Bitmap =
        getOrPut("bonus_${type}_$frame") {
            val size = TILE * scale
            val s = size.toFloat()
            val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val c = Canvas(bmp)
            val p = Paint().apply { isAntiAlias = false }
            val cx = s / 2f
            val bob = if (frame % 2 == 0) 0f else -scale * 1f
            val cy = s * 0.5f + bob

            // Подложка-блик под бонусом.
            p.color = Color.argb(60, 255, 255, 255)
            drawPixelCircle(c, cx, s * 0.78f, s * 0.30f, p.color, p)

            when (type) {
                com.bombermama.core.BonusType.FIRE -> {
                    // Огонь: три "язычка".
                    p.color = Palette.BONUS_FIRE_B
                    drawPixelTriangle(c, cx, cy - s * 0.28f, s * 0.24f, p)
                    p.color = Palette.BONUS_FIRE_A
                    drawPixelTriangle(c, cx, cy - s * 0.20f, s * 0.15f, p)
                    p.color = Palette.EXPLO_CORE
                    drawPixelTriangle(c, cx, cy - s * 0.12f, s * 0.07f, p)
                }
                com.bombermama.core.BonusType.BOMB -> {
                    // Бомба.
                    drawPixelCircle(c, cx, cy + s * 0.05f, s * 0.22f, Palette.BONUS_BOMB_B, p)
                    drawPixelCircle(c, cx - s * 0.06f, cy - s * 0.01f, s * 0.07f, Palette.BONUS_BOMB_A, p)
                    p.color = Palette.FUSE
                    c.drawRect(cx - scale * 0.5f, cy - s * 0.28f, cx + scale * 0.5f, cy - s * 0.14f, p)
                }
                com.bombermama.core.BonusType.SPEED -> {
                    // Стрела-молния.
                    p.color = Palette.BONUS_SPEED_B
                    val pts = floatArrayOf(
                        cx + s * 0.18f, cy - s * 0.28f,
                        cx - s * 0.10f, cy + s * 0.02f,
                        cx + s * 0.02f, cy + s * 0.02f,
                        cx - s * 0.18f, cy + s * 0.28f,
                        cx + s * 0.10f, cy - s * 0.02f,
                        cx - s * 0.02f, cy - s * 0.02f
                    )
                    drawPixelPoly(c, pts, p)
                    p.color = Palette.BONUS_SPEED_A
                    drawPixelPoly(c, pts.mapIndexed { i, v ->
                        if (i % 2 == 0) v + scale * 0.5f else v - scale * 0.5f
                    }.toFloatArray(), p)
                }
                com.bombermama.core.BonusType.LIFE -> {
                    // Сердце из двух кругов и треугольника.
                    drawPixelCircle(c, cx - s * 0.09f, cy - s * 0.06f, s * 0.12f, Palette.BONUS_LIFE_B, p)
                    drawPixelCircle(c, cx + s * 0.09f, cy - s * 0.06f, s * 0.12f, Palette.BONUS_LIFE_B, p)
                    p.color = Palette.BONUS_LIFE_B
                    drawPixelTriangleDown(c, cx, cy + s * 0.22f, s * 0.21f, p)
                    p.color = Palette.BONUS_LIFE_A
                    drawPixelCircle(c, cx - s * 0.05f, cy - s * 0.08f, s * 0.05f, p.color, p)
                }
                com.bombermama.core.BonusType.STAR -> {
                    // Звезда.
                    drawPixelStar(c, cx, cy, s * 0.28f, Palette.BONUS_STAR_B, p)
                    drawPixelStar(c, cx, cy, s * 0.18f, Palette.BONUS_STAR_A, p)
                }
            }
            bmp
        }

    // =========================================================================
    // Героиня — Мама
    // =========================================================================

    /**
     * Спрайт главной героини.
     *
     * @param dir направление взгляда/движения
     * @param frame фаза анимации ходьбы (0..3, зацикленная)
     * @param pose позца: 0=обычная, 1=получение урона, 2=победа, 3=поражение
     */
    fun mama(
        dir: com.bombermama.core.Direction,
        frame: Int,
        pose: Int
    ): Bitmap = getOrPut("mama_${dir}_${frame}_$pose") {
        val size = TILE * scale
        val s = size.toFloat()
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val p = Paint().apply { isAntiAlias = false }
        val cx = s / 2f

        // Фаза шага: ноги меняют положение.
        val step = frame % 4
        val legA = when (step) { 1 -> -1f; 3 -> 1f; else -> 0f }
        val legB = -legA

        when (pose) {
            2 -> drawMamaWin(c, p, s, cx)
            3 -> drawMamaLose(c, p, s, cx)
            else -> {
                // Тень под героиней.
                p.color = Color.argb(70, 26, 22, 38)
                drawPixelEllipse(c, cx, s * 0.86f, s * 0.24f, s * 0.07f, p)

                if (pose == 1) {
                    // При получении урона героиня мигает: рисуем лишь половину
                    // деталей, создавая эффект "мигания".
                    p.color = Color.argb(110, 245, 207, 168)
                    drawPixelEllipse(c, cx, s * 0.55f, s * 0.26f, s * 0.36f, p)
                }

                when (dir) {
                    com.bombermama.core.Direction.DOWN -> drawMamaFace(c, p, s, cx, legA, legB, pose == 1)
                    com.bombermama.core.Direction.UP -> drawMamaBack(c, p, s, cx, legA, legB)
                    com.bombermama.core.Direction.LEFT, com.bombermama.core.Direction.RIGHT ->
                        drawMamaSide(c, p, s, cx, legA, legB, dir)
                    else -> drawMamaFace(c, p, s, cx, legA, legB, false)
                }
            }
        }
        bmp
    }

    /** Анфас: смотрит на игрока (движение "вниз"). */
    private fun drawMamaFace(c: Canvas, p: Paint, s: Float, cx: Float, legA: Float, legB: Float, hurt: Boolean) {
        val px = scale.toFloat()
        // Ноги.
        p.color = Palette.SHOE
        c.drawRect(cx - px * 3f, s * 0.74f, cx - px * 1f, s * 0.86f + legA * px, p)
        c.drawRect(cx + px * 1f, s * 0.74f, cx + px * 3f, s * 0.86f + legB * px, p)
        // Платье (трапеция).
        p.color = Palette.DRESS_SHADE
        c.drawRect(cx - px * 4.5f, s * 0.42f, cx + px * 4.5f, s * 0.80f, p)
        p.color = Palette.DRESS
        c.drawRect(cx - px * 3.5f, s * 0.42f, cx + px * 3.5f, s * 0.78f, p)
        // Фартук.
        p.color = Palette.APRON
        c.drawRect(cx - px * 2f, s * 0.46f, cx + px * 2f, s * 0.74f, p)
        p.color = Palette.APRON_SHADE
        c.drawRect(cx - px * 2f, s * 0.60f, cx + px * 2f, s * 0.63f, p)
        // Руки.
        p.color = Palette.SKIN
        c.drawRect(cx - px * 5.5f, s * 0.44f, cx - px * 4.2f, s * 0.62f, p)
        c.drawRect(cx + px * 4.2f, s * 0.44f, cx + px * 5.5f, s * 0.62f, p)
        // Шея.
        p.color = Palette.SKIN_SHADE
        c.drawRect(cx - px * 1.2f, s * 0.36f, cx + px * 1.2f, s * 0.44f, p)
        // Голова.
        p.color = Palette.SKIN
        drawPixelEllipse(c, cx, s * 0.27f, s * 0.17f, s * 0.17f, p)
        // Причёска — тёмно-каштановое каре с чёлкой.
        p.color = Palette.HAIR
        drawPixelEllipse(c, cx, s * 0.20f, s * 0.19f, s * 0.13f, p)
        c.drawRect(cx - px * 3.2f, s * 0.20f, cx + px * 3.2f, s * 0.30f, p)
        // Боковые пряди.
        c.drawRect(cx - px * 3.4f, s * 0.22f, cx - px * 2.2f, s * 0.40f, p)
        c.drawRect(cx + px * 2.2f, s * 0.22f, cx + px * 3.4f, s * 0.40f, p)
        // Чёлка.
        c.drawRect(cx - px * 2.8f, s * 0.16f, cx + px * 2.8f, s * 0.21f, p)
        p.color = Palette.HAIR_SHADE
        c.drawRect(cx - px * 2.8f, s * 0.19f, cx - px * 1.4f, s * 0.22f, p)
        // Глаза.
        if (!hurt) {
            p.color = Palette.EYE
            c.drawRect(cx - px * 1.8f, s * 0.26f, cx - px * 0.9f, s * 0.29f, p)
            c.drawRect(cx + px * 0.9f, s * 0.26f, cx + px * 1.8f, s * 0.29f, p)
            // Щёчки.
            p.color = Palette.CHEEK
            c.drawRect(cx - px * 2.6f, s * 0.30f, cx - px * 2.0f, s * 0.32f, p)
            c.drawRect(cx + px * 2.0f, s * 0.30f, cx + px * 2.6f, s * 0.32f, p)
            // Улыбка.
            p.color = Palette.HAIR_SHADE
            c.drawRect(cx - px * 0.8f, s * 0.33f, cx + px * 0.8f, s * 0.34f, p)
        } else {
            // При уроне — "иксы" вместо глаз.
            p.color = Palette.EYE
            c.drawRect(cx - px * 1.8f, s * 0.25f, cx - px * 1.4f, s * 0.26f, p)
            c.drawRect(cx - px * 1.0f, s * 0.25f, cx - px * 0.6f, s * 0.26f, p)
            c.drawRect(cx + px * 1.0f, s * 0.25f, cx + px * 1.4f, s * 0.26f, p)
            c.drawRect(cx + px * 1.6f, s * 0.25f, cx + px * 2.0f, s * 0.26f, p)
        }
    }

    /** Со спины: волосы закрыты фартуком-узлом. */
    private fun drawMamaBack(c: Canvas, p: Paint, s: Float, cx: Float, legA: Float, legB: Float) {
        val px = scale.toFloat()
        p.color = Palette.SHOE
        c.drawRect(cx - px * 3f, s * 0.74f, cx - px * 1f, s * 0.86f + legA * px, p)
        c.drawRect(cx + px * 1f, s * 0.74f, cx + px * 3f, s * 0.86f + legB * px, p)
        p.color = Palette.DRESS_SHADE
        c.drawRect(cx - px * 4.5f, s * 0.42f, cx + px * 4.5f, s * 0.80f, p)
        p.color = Palette.DRESS
        c.drawRect(cx - px * 3.5f, s * 0.42f, cx + px * 3.5f, s * 0.78f, p)
        // Узел фартука на талии.
        p.color = Palette.APRON
        c.drawRect(cx - px * 2f, s * 0.54f, cx + px * 2f, s * 0.70f, p)
        p.color = Palette.APRON_SHADE
        c.drawRect(cx - px * 0.6f, s * 0.58f, cx + px * 0.6f, s * 0.66f, p)
        p.color = Palette.SKIN
        c.drawRect(cx - px * 5.5f, s * 0.44f, cx - px * 4.2f, s * 0.62f, p)
        c.drawRect(cx + px * 4.2f, s * 0.44f, cx + px * 5.5f, s * 0.62f, p)
        // Голова со спины — волосы полностью.
        p.color = Palette.HAIR
        drawPixelEllipse(c, cx, s * 0.26f, s * 0.18f, s * 0.18f, p)
        c.drawRect(cx - px * 3.4f, s * 0.22f, cx + px * 3.4f, s * 0.38f, p)
        p.color = Palette.HAIR_SHADE
        c.drawRect(cx - px * 3.4f, s * 0.30f, cx + px * 3.4f, s * 0.34f, p)
        // Платок на затылке.
        p.color = Palette.DRESS
        drawPixelEllipse(c, cx, s * 0.18f, s * 0.13f, s * 0.09f, p)
    }

    /** В профиль: боковой вид с поворотом корпуса. */
    private fun drawMamaSide(c: Canvas, p: Paint, s: Float, cx: Float, legA: Float, legB: Float, dir: com.bombermama.core.Direction) {
        val px = scale.toFloat()
        val f = if (dir == com.bombermama.core.Direction.RIGHT) 1f else -1f
        // Ноги.
        p.color = Palette.SHOE
        c.drawRect(cx - px * 1f, s * 0.74f, cx + px * 1f, s * 0.86f + legA * px, p)
        c.drawRect(cx + px * 0.5f, s * 0.74f, cx + px * 2.5f, s * 0.86f + legB * px, p)
        // Платье.
        p.color = Palette.DRESS_SHADE
        c.drawRect(cx - px * 3f, s * 0.42f, cx + px * 3.5f, s * 0.80f, p)
        p.color = Palette.DRESS
        c.drawRect(cx - px * 2.5f, s * 0.42f, cx + px * 2.8f, s * 0.78f, p)
        // Фартук спереди.
        p.color = Palette.APRON
        c.drawRect(cx + f * px * 0.5f, s * 0.46f, cx + f * px * 2.8f, s * 0.74f, p)
        // Руки.
        p.color = Palette.SKIN
        c.drawRect(cx - px * 3.6f, s * 0.44f, cx - px * 2.4f, s * 0.62f, p)
        c.drawRect(cx + px * 2.6f, s * 0.44f, cx + px * 3.8f, s * 0.62f, p)
        // Голова.
        p.color = Palette.SKIN
        drawPixelEllipse(c, cx + f * px * 0.5f, s * 0.27f, s * 0.16f, s * 0.16f, p)
        // Причёска.
        p.color = Palette.HAIR
        drawPixelEllipse(c, cx + f * px * 0.5f, s * 0.19f, s * 0.18f, s * 0.12f, p)
        c.drawRect(cx - px * 2.4f, s * 0.20f, cx + px * 2.6f, s * 0.28f, p)
        // Чёлка свисает на лоб.
        c.drawRect(cx + f * px * 0.5f, s * 0.16f, cx + f * px * 2.6f, s * 0.21f, p)
        // Глаз.
        p.color = Palette.EYE
        c.drawRect(cx + f * px * 1.0f, s * 0.26f, cx + f * px * 1.9f, s * 0.29f, p)
        p.color = Palette.CHEEK
        c.drawRect(cx + f * px * 2.0f, s * 0.30f, cx + f * px * 2.5f, s * 0.32f, p)
    }

    /** Победа: руки вверх, сияние. */
    private fun drawMamaWin(c: Canvas, p: Paint, s: Float, cx: Float) {
        val px = scale.toFloat()
        // Сияние вокруг.
        p.color = Color.argb(80, 255, 224, 102)
        drawPixelCircle(c, cx, s * 0.5f, s * 0.46f, p.color, p)
        // Руки подняты.
        p.color = Palette.SKIN
        c.drawRect(cx - px * 5.5f, s * 0.12f, cx - px * 4.2f, s * 0.40f, p)
        c.drawRect(cx + px * 4.2f, s * 0.12f, cx + px * 5.5f, s * 0.40f, p)
        // Платье.
        p.color = Palette.DRESS
        c.drawRect(cx - px * 3.5f, s * 0.42f, cx + px * 3.5f, s * 0.82f, p)
        p.color = Palette.APRON
        c.drawRect(cx - px * 2f, s * 0.46f, cx + px * 2f, s * 0.74f, p)
        // Голова.
        p.color = Palette.SKIN
        drawPixelEllipse(c, cx, s * 0.27f, s * 0.17f, s * 0.17f, p)
        p.color = Palette.HAIR
        drawPixelEllipse(c, cx, s * 0.20f, s * 0.19f, s * 0.13f, p)
        c.drawRect(cx - px * 2.8f, s * 0.16f, cx + px * 2.8f, s * 0.28f, p)
        // Счастливые глаза (дугою).
        p.color = Palette.EYE
        c.drawRect(cx - px * 1.8f, s * 0.27f, cx - px * 0.9f, s * 0.28f, p)
        c.drawRect(cx + px * 0.9f, s * 0.27f, cx + px * 1.8f, s * 0.28f, p)
        // Широкая улыбка.
        p.color = Palette.HAIR_SHADE
        c.drawRect(cx - px * 1.2f, s * 0.32f, cx + px * 1.2f, s * 0.34f, p)
        // Звёздочки радости.
        p.color = Palette.EXPLO_CORE
        c.drawRect(cx - px * 6.5f, s * 0.08f, cx - px * 5.5f, s * 0.12f, p)
        c.drawRect(cx + px * 5.5f, s * 0.08f, cx + px * 6.5f, s * 0.12f, p)
    }

    /** Поражение: героиня сидит, голова опущена. */
    private fun drawMamaLose(c: Canvas, p: Paint, s: Float, cx: Float) {
        val px = scale.toFloat()
        // Приглушённые цвета — затемним силуэт.
        // Платье.
        p.color = Palette.DRESS_SHADE
        c.drawRect(cx - px * 3.5f, s * 0.46f, cx + px * 3.5f, s * 0.84f, p)
        // Голова опущена.
        p.color = Palette.SKIN_SHADE
        drawPixelEllipse(c, cx + px * 1.5f, s * 0.40f, s * 0.16f, s * 0.15f, p)
        // Волосы свисают.
        p.color = Palette.HAIR_SHADE
        drawPixelEllipse(c, cx + px * 1.5f, s * 0.33f, s * 0.18f, s * 0.12f, p)
        c.drawRect(cx - px * 0.5f, s * 0.32f, cx + px * 3.2f, s * 0.42f, p)
        // Виски-капли (грусть).
        p.color = Palette.EXIT_A
        c.drawRect(cx - px * 1.2f, s * 0.38f, cx - px * 0.7f, s * 0.43f, p)
        // Руки безвольно опущены.
        p.color = Palette.SKIN_SHADE
        c.drawRect(cx - px * 4.6f, s * 0.50f, cx - px * 3.4f, s * 0.80f, p)
        c.drawRect(cx + px * 3.4f, s * 0.50f, cx + px * 4.6f, s * 0.80f, p)
    }

    // =========================================================================
    // Враги
    // =========================================================================

    /**
     * Спрайт врага.
     * @param type тип врага
     * @param frame фаза анимации (0..3)
     * @param dead true — состояние смерти (сплющенный/призрачный)
     */
    fun enemy(type: com.bombermama.core.EnemyType, frame: Int, dead: Boolean): Bitmap =
        getOrPut("enemy_${type}_${frame}_$dead") {
            val size = TILE * scale
            val s = size.toFloat()
            val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val c = Canvas(bmp)
            val p = Paint().apply { isAntiAlias = false }
            val cx = s / 2f
            if (dead) {
                drawEnemyDead(c, p, s, cx, type)
            } else {
                val bob = when (frame % 4) { 1 -> -scale * 1f; 3 -> scale * 1f; else -> 0f }
                when (type) {
                    com.bombermama.core.EnemyType.SLOW -> drawSlime(c, p, s, cx, bob)
                    com.bombermama.core.EnemyType.FAST -> drawBat(c, p, s, cx, frame)
                    com.bombermama.core.EnemyType.CHANGER -> drawGhost(c, p, s, cx, frame)
                    com.bombermama.core.EnemyType.HUNTER -> drawDemon(c, p, s, cx, frame)
                }
            }
            bmp
        }

    /** Медленный враг: зелёный слайм с антеннами. */
    private fun drawSlime(c: Canvas, p: Paint, s: Float, cx: Float, bob: Float) {
        val px = scale.toFloat()
        // Тень.
        p.color = Color.argb(70, 26, 22, 38)
        drawPixelEllipse(c, cx, s * 0.84f, s * 0.26f, s * 0.06f, p)
        // Тело-капля.
        p.color = Palette.SLIME_C
        drawPixelEllipse(c, cx, s * 0.60f + bob, s * 0.28f, s * 0.24f, p)
        p.color = Palette.SLIME_B
        drawPixelEllipse(c, cx, s * 0.56f + bob, s * 0.25f, s * 0.22f, p)
        p.color = Palette.SLIME_A
        drawPixelEllipse(c, cx, s * 0.52f + bob, s * 0.20f, s * 0.17f, p)
        // Блик.
        p.color = Color.argb(140, 255, 255, 255)
        c.drawRect(cx - px * 2.4f, s * 0.42f + bob, cx - px * 1.2f, s * 0.47f + bob, p)
        // Антенны.
        p.color = Palette.SLIME_C
        c.drawRect(cx - px * 2.2f, s * 0.26f + bob, cx - px * 1.8f, s * 0.36f + bob, p)
        c.drawRect(cx + px * 1.8f, s * 0.26f + bob, cx + px * 2.2f, s * 0.36f + bob, p)
        p.color = Palette.SLIME_A
        c.drawRect(cx - px * 2.4f, s * 0.22f + bob, cx - px * 1.6f, s * 0.27f + bob, p)
        c.drawRect(cx + px * 1.6f, s * 0.22f + bob, cx + px * 2.4f, s * 0.27f + bob, p)
        // Глаза.
        p.color = Palette.EYE
        c.drawRect(cx - px * 1.9f, s * 0.56f + bob, cx - px * 1.0f, s * 0.61f + bob, p)
        c.drawRect(cx + px * 1.0f, s * 0.56f + bob, cx + px * 1.9f, s * 0.61f + bob, p)
        // Рот.
        p.color = Palette.SLIME_C
        c.drawRect(cx - px * 0.8f, s * 0.66f + bob, cx + px * 0.8f, s * 0.68f + bob, p)
    }

    /** Быстрый враг: фиолетовая летучая мышь. */
    private fun drawBat(c: Canvas, p: Paint, s: Float, cx: Float, frame: Int) {
        val px = scale.toFloat()
        val flap = if (frame % 2 == 0) 1 else -1
        // Тень.
        p.color = Color.argb(60, 26, 22, 38)
        drawPixelEllipse(c, cx, s * 0.86f, s * 0.24f, s * 0.05f, p)
        // Крылья.
        p.color = Palette.BAT_C
        if (flap > 0) {
            drawPixelTriangle(c, cx - s * 0.28f, s * 0.44f, s * 0.16f, p)
            c.drawRect(cx - s * 0.40f, s * 0.44f, cx - s * 0.12f, s * 0.50f, p)
            drawPixelTriangle(c, cx + s * 0.28f, s * 0.44f, s * 0.16f, p)
            c.drawRect(cx + s * 0.12f, s * 0.44f, cx + s * 0.40f, s * 0.50f, p)
        } else {
            drawPixelTriangle(c, cx - s * 0.28f, s * 0.56f, s * 0.16f, p)
            c.drawRect(cx - s * 0.40f, s * 0.50f, cx - s * 0.12f, s * 0.56f, p)
            drawPixelTriangle(c, cx + s * 0.28f, s * 0.56f, s * 0.16f, p)
            c.drawRect(cx + s * 0.12f, s * 0.50f, cx + s * 0.40f, s * 0.56f, p)
        }
        // Тело.
        p.color = Palette.BAT_B
        drawPixelEllipse(c, cx, s * 0.50f, s * 0.15f, s * 0.17f, p)
        p.color = Palette.BAT_A
        drawPixelEllipse(c, cx, s * 0.46f, s * 0.11f, s * 0.13f, p)
        // Уши.
        p.color = Palette.BAT_B
        drawPixelTriangle(c, cx - s * 0.12f, s * 0.28f, s * 0.06f, p)
        drawPixelTriangle(c, cx + s * 0.12f, s * 0.28f, s * 0.06f, p)
        // Глаза-бусинки.
        p.color = Palette.EXPLO_CORE
        c.drawRect(cx - px * 1.4f, s * 0.44f, cx - px * 0.7f, s * 0.47f, p)
        c.drawRect(cx + px * 0.7f, s * 0.44f, cx + px * 1.4f, s * 0.47f, p)
        // Клыки.
        p.color = Color.argb(220, 255, 255, 255)
        c.drawRect(cx - px * 0.9f, s * 0.54f, cx - px * 0.4f, s * 0.57f, p)
        c.drawRect(cx + px * 0.4f, s * 0.54f, cx + px * 0.9f, s * 0.57f, p)
    }

    /** Враг-менял направления: бирюзовый призрак. */
    private fun drawGhost(c: Canvas, p: Paint, s: Float, cx: Float, frame: Int) {
        val px = scale.toFloat()
        val bob = if (frame % 2 == 0) -scale else scale
        // Тень.
        p.color = Color.argb(50, 26, 22, 38)
        drawPixelEllipse(c, cx, s * 0.86f, s * 0.24f, s * 0.05f, p)
        // Полупрозрачное тело с волнистым низом.
        p.color = Color.argb(200, 122, 216, 212)
        drawPixelEllipse(c, cx, s * 0.48f + bob, s * 0.26f, s * 0.24f, p)
        c.drawRect(cx - s * 0.26f, s * 0.48f + bob, cx + s * 0.26f, s * 0.76f + bob, p)
        // Волнистый край.
        p.color = Color.argb(200, 122, 216, 212)
        for (i in 0..3) {
            val x0 = cx - s * 0.26f + i * s * 0.13f
            c.drawRect(x0, s * 0.76f + bob, x0 + s * 0.07f, s * 0.82f + bob, p)
        }
        // Внутренний слой.
        p.color = Color.argb(230, 78, 163, 158)
        drawPixelEllipse(c, cx, s * 0.44f + bob, s * 0.20f, s * 0.18f, p)
        // Глаза-щёлки.
        p.color = Palette.EYE
        c.drawRect(cx - px * 2.0f, s * 0.42f + bob, cx - px * 1.0f, s * 0.46f + bob, p)
        c.drawRect(cx + px * 1.0f, s * 0.42f + bob, cx + px * 2.0f, s * 0.46f + bob, p)
        // Рот-волна.
        p.color = Palette.GHOST_C
        c.drawRect(cx - px * 1.0f, s * 0.54f + bob, cx, s * 0.56f + bob, p)
        c.drawRect(cx, s * 0.56f + bob, cx + px * 1.0f, s * 0.58f + bob, p)
    }

    /** Сложный враг: красный демон с рогами. */
    private fun drawDemon(c: Canvas, p: Paint, s: Float, cx: Float, frame: Int) {
        val px = scale.toFloat()
        val bob = if (frame % 2 == 0) -scale else scale
        // Тень.
        p.color = Color.argb(80, 26, 22, 38)
        drawPixelEllipse(c, cx, s * 0.86f, s * 0.27f, s * 0.06f, p)
        // Рога.
        p.color = Palette.DEMON_C
        drawPixelTriangle(c, cx - s * 0.16f, s * 0.20f, s * 0.07f, p)
        drawPixelTriangle(c, cx + s * 0.16f, s * 0.20f, s * 0.07f, p)
        // Голова.
        p.color = Palette.DEMON_C
        drawPixelEllipse(c, cx, s * 0.36f, s * 0.17f, s * 0.14f, p)
        p.color = Palette.DEMON_B
        drawPixelEllipse(c, cx, s * 0.33f, s * 0.13f, s * 0.11f, p)
        // Тело.
        p.color = Palette.DEMON_B
        drawPixelEllipse(c, cx, s * 0.62f + bob, s * 0.22f, s * 0.20f, p)
        p.color = Palette.DEMON_A
        drawPixelEllipse(c, cx, s * 0.58f + bob, s * 0.17f, s * 0.15f, p)
        // Блик.
        p.color = Color.argb(110, 255, 255, 255)
        c.drawRect(cx - px * 2.2f, s * 0.50f + bob, cx - px * 1.2f, s * 0.54f + bob, p)
        // Руки-когти.
        p.color = Palette.DEMON_C
        c.drawRect(cx - px * 4.4f, s * 0.52f, cx - px * 3.2f, s * 0.70f, p)
        c.drawRect(cx + px * 3.2f, s * 0.52f, cx + px * 4.4f, s * 0.70f, p)
        // Горящие глаза.
        p.color = Palette.EXPLO_CORE
        c.drawRect(cx - px * 1.8f, s * 0.32f, cx - px * 0.9f, s * 0.36f, p)
        c.drawRect(cx + px * 0.9f, s * 0.32f, cx + px * 1.8f, s * 0.36f, p)
        // Клыки.
        p.color = Color.argb(230, 255, 255, 255)
        c.drawRect(cx - px * 1.2f, s * 0.40f, cx - px * 0.6f, s * 0.44f, p)
        c.drawRect(cx + px * 0.6f, s * 0.40f, cx + px * 1.2f, s * 0.44f, p)
    }

    /** Смерть врага: сплющенное тело с крестиками в глазах. */
    private fun drawEnemyDead(c: Canvas, p: Paint, s: Float, cx: Float, type: com.bombermama.core.EnemyType) {
        val base = when (type) {
            com.bombermama.core.EnemyType.SLOW -> Palette.SLIME_C
            com.bombermama.core.EnemyType.FAST -> Palette.BAT_C
            com.bombermama.core.EnemyType.CHANGER -> Palette.GHOST_C
            com.bombermama.core.EnemyType.HUNTER -> Palette.DEMON_C
        }
        // Сплющенное тело.
        p.color = base
        drawPixelEllipse(c, cx, s * 0.72f, s * 0.28f, s * 0.10f, p)
        p.color = Color.argb(120, 255, 255, 255)
        drawPixelEllipse(c, cx, s * 0.70f, s * 0.20f, s * 0.06f, p)
        // Крестики в глазах.
        p.color = Palette.EYE
        c.drawRect(cx - px() * 2.4f, s * 0.68f, cx - px() * 1.8f, s * 0.74f, p)
        c.drawRect(cx - px() * 2.1f, s * 0.65f, cx - px() * 1.5f, s * 0.71f, p)
        c.drawRect(cx + px() * 1.8f, s * 0.68f, cx + px() * 2.4f, s * 0.74f, p)
        c.drawRect(cx + px() * 1.5f, s * 0.65f, cx + px() * 2.1f, s * 0.71f, p)
    }

    private fun px(): Float = scale.toFloat()

    // =========================================================================
    // Эффекты
    // =========================================================================

    /** Частица-вспышка для взрывов и бонусов. */
    fun particle(color: Int, size: Int): Bitmap = getOrPut("part_${color}_$size") {
        val s = size * scale
        val bmp = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val p = Paint().apply { isAntiAlias = false }
        p.color = color
        drawPixelCircle(c, s / 2f, s / 2f, s * 0.40f, color, p)
        p.color = Color.argb(150, 255, 255, 255)
        drawPixelCircle(c, s / 2f, s / 2f, s * 0.18f, p.color, p)
        bmp
    }

    /** Конфетти для экрана победы. */
    fun confetti(color: Int): Bitmap = getOrPut("conf_$color") {
        val s = 3 * scale
        val bmp = Bitmap.createBitmap(s, s * 2, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val p = Paint().apply { isAntiAlias = false }
        p.color = color
        c.drawRect(0f, 0f, s.toFloat(), (s * 0.4f), p)
        c.drawRect(s * 0.3f, s * 0.4f, s * 0.7f, (s * 1.2f), p)
        c.drawRect(0f, (s * 1.2f), s.toFloat(), (s * 1.6f), p)
        bmp
    }

    // =========================================================================
    // Выход
    // =========================================================================

    fun exit(frame: Int): Bitmap = getOrPut("exit_$frame") {
        val size = TILE * scale
        val s = size.toFloat()
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val p = Paint().apply { isAntiAlias = false }
        // Сияние.
        val glowAlpha = if (frame % 2 == 0) 90 else 50
        p.color = Color.argb(glowAlpha, 142, 232, 200)
        drawPixelCircle(c, s / 2f, s / 2f, s * (if (frame % 2 == 0) 0.44f else 0.38f), p.color, p)
        // Дверь-арка.
        p.color = Palette.EXIT_C
        c.drawRect(s * 0.24f, s * 0.20f, s * 0.76f, s * 0.84f, p)
        p.color = Palette.EXIT_B
        c.drawRect(s * 0.28f, s * 0.24f, s * 0.72f, s * 0.84f, p)
        p.color = Palette.EXIT_A
        c.drawRect(s * 0.34f, s * 0.30f, s * 0.66f, s * 0.80f, p)
        // Стрелка вниз.
        p.color = Palette.EXIT_C
        c.drawRect(s * 0.45f, s * 0.36f, s * 0.55f, s * 0.60f, p)
        drawPixelTriangleDown(c, s / 2f, s * 0.68f, s * 0.10f, p)
        bmp
    }

    // =========================================================================
    // Вспомогательная отрисовка "пиксельными" примитивами
    // =========================================================================

    private fun drawPixelCircle(c: Canvas, cx: Float, cy: Float, r: Float, color: Int, p: Paint) {
        p.color = color
        val ps = scale.toFloat()
        val rr = r * r
        val minY = (cy - r).toInt()
        val maxY = (cy + r).toInt()
        var py = (minY / ps).toInt() * ps
        while (py <= maxY) {
            val dy = py + ps / 2f - cy
            val dx = Math.sqrt(maxOf(0.0, (rr - dy * dy).toDouble())).toFloat()
            val half = ((dx / ps).toInt()) * ps
            c.drawRect(cx - half, py, cx + half, py + ps, p)
            py += ps
        }
    }

    /** Эллипс из "пикселей" — для тел персонажей. */
    private fun drawPixelEllipse(c: Canvas, cx: Float, cy: Float, rx: Float, ry: Float, p: Paint) {
        val ps = scale.toFloat()
        val minY = (cy - ry).toInt()
        val maxY = (cy + ry).toInt()
        var py = (minY / ps).toInt() * ps
        while (py <= maxY) {
            val dy = py + ps / 2f - cy
            val t = dy / ry
            if (t * t <= 1f) {
                val dx = rx * Math.sqrt(1.0 - t * t).toFloat()
                val half = ((dx / ps).toInt()) * ps
                c.drawRect(cx - half, py, cx + half, py + ps, p)
            }
            py += ps
        }
    }

    private fun drawPixelTriangle(c: Canvas, cx: Float, topY: Float, half: Float, p: Paint) {
        // Вверх остриём: треугольник с вершиной в (cx, topY).
        val ps = scale.toFloat()
        var y = topY
        var h = 0f
        while (h < half * 2f) {
            val w = half * (h / (half * 2f))
            c.drawRect(cx - w, y, cx + w, y + ps, p)
            y += ps
            h += ps
        }
    }

    private fun drawPixelTriangleDown(c: Canvas, cx: Float, bottomY: Float, half: Float, p: Paint) {
        val ps = scale.toFloat()
        var y = bottomY
        var h = 0f
        while (h < half * 2f) {
            val w = half * (h / (half * 2f))
            c.drawRect(cx - w, y - ps, cx + w, y, p)
            y -= ps
            h += ps
        }
    }

    private fun drawPixelPoly(c: Canvas, pts: FloatArray, p: Paint) {
        // Грубая растеризация: заливаем охватывающий прямоугольник и проверяем
        // принадлежность точки многоугольнику.
        var minX = Float.MAX_VALUE; var maxX = Float.MIN_VALUE
        var minY = Float.MAX_VALUE; var maxY = Float.MIN_VALUE
        var i = 0
        while (i < pts.size) {
            minX = min(minX, pts[i]); maxX = max(maxX, pts[i])
            minY = min(minY, pts[i + 1]); maxY = max(maxY, pts[i + 1])
            i += 2
        }
        val ps = scale.toFloat()
        var y = (minY / ps).toInt() * ps
        while (y <= maxY) {
            var x = (minX / ps).toInt() * ps
            while (x <= maxX) {
                if (pointInPoly(x + ps / 2f, y + ps / 2f, pts)) {
                    c.drawRect(x, y, x + ps, y + ps, p)
                }
                x += ps
            }
            y += ps
        }
    }

    private fun pointInPoly(px: Float, py: Float, pts: FloatArray): Boolean {
        var inside = false
        var j = pts.size - 2
        var i = 0
        while (i < pts.size) {
            val xi = pts[i]; val yi = pts[i + 1]
            val xj = pts[j]; val yj = pts[j + 1]
            val intersect = ((yi > py) != (yj > py)) &&
                px < (xj - xi) * (py - yi) / (yj - yi + 1e-9f) + xi
            if (intersect) inside = !inside
            j = i
            i += 2
        }
        return inside
    }

    private fun drawPixelStar(c: Canvas, cx: Float, cy: Float, r: Float, color: Int, p: Paint) {
        // Пятиконечная звезда.
        val pts = FloatArray(10)
        for (k in 0 until 5) {
            val a = -Math.PI / 2 + k * 2 * Math.PI / 5
            pts[k * 2] = (cx + r * Math.cos(a)).toFloat()
            pts[k * 2 + 1] = (cy + r * Math.sin(a)).toFloat()
        }
        // Растеризуем выпуклую оболочку упрощённо: два треугольника вверх/вниз
        // + центральный круг.
        p.color = color
        drawPixelCircle(c, cx, cy, r * 0.55f, color, p)
        for (k in 0 until 5) {
            val a = -Math.PI / 2 + k * 2 * Math.PI / 5
            val tipX = (cx + r * Math.cos(a)).toFloat()
            val tipY = (cy + r * Math.sin(a)).toFloat()
            drawPixelCircle(c, (tipX + cx) / 2f, (tipY + cy) / 2f, r * 0.18f, color, p)
            c.drawRect(cx, cy, tipX, tipY, p)
        }
    }

    private inline fun max(a: Float, b: Float) = if (a > b) a else b
    private inline fun min(a: Float, b: Float) = if (a < b) a else b

    private inline fun getOrPut(key: String, create: () -> Bitmap): Bitmap {
        return cache[key] ?: create().also { cache[key] = it }
    }

    companion object {
        const val TILE = 16 // логических "пикселей" арт в одной клетке
    }
}
