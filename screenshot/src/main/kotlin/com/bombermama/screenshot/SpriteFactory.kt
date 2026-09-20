package com.bombermama.screenshot

import com.bombermama.core.BonusType
import com.bombermama.core.Direction
import com.bombermama.core.EnemyType
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Генератор пиксельной графики — порт SpriteFactory из app-модуля.
 *
 * Спрайты рисуются теми же примитивами и той же палитрой, что и в игре,
 * поэтому скриншоты показывают настоящие спрайты BomberMama.
 * Отличие только в backend: здесь рисование идёт в [PixCanvas] на чистой JVM.
 */
class SpriteFactory(private val scale: Int = 4) {

    private val cache = HashMap<String, PixCanvas>()

    val pixelSize: Int get() = scale

    private fun create(): PixCanvas = PixCanvas(TILE * scale, TILE * scale)

    // ===================== Пол и стены =====================

    fun floorTile(dark: Boolean): PixCanvas = getOrPut("floor_$dark") {
        val s = (TILE * scale).toFloat()
        val c = create()
        val base = if (dark) Palette.FLOOR_A else Palette.FLOOR_B
        c.drawRect(0f, 0f, s, s, base)
        c.drawRect(0f, 0f, s, scale.toFloat(), Palette.FLOOR_LINE)
        c.drawRect(0f, 0f, scale.toFloat(), s, Palette.FLOOR_LINE)
        val speck = if (dark) Palette.FLOOR_B else Palette.FLOOR_A
        c.drawRect(scale * 3f, scale * 5f, scale * 4f, scale * 6f, speck)
        c.drawRect(scale * 11f, scale * 2f, scale * 12f, scale * 3f, speck)
        c
    }

    fun wallTile(): PixCanvas = getOrPut("wall") {
        val s = (TILE * scale).toFloat()
        val c = create()
        c.drawRect(0f, 0f, s, s, Palette.WALL_MID)
        c.drawRect(scale * 1f, scale * 1f, s - scale * 1f, scale * 3f, Palette.WALL_LIGHT)
        c.drawRect(scale * 1f, scale * 1f, scale * 3f, s - scale * 1f, Palette.WALL_LIGHT)
        c.drawRect(scale * 1f, s - scale * 3f, s - scale * 1f, s - scale * 1f, Palette.WALL_DARK)
        c.drawRect(s - scale * 3f, scale * 1f, s - scale * 1f, s - scale * 1f, Palette.WALL_DARK)
        c.drawRect(scale * 6f, scale * 6f, scale * 7f, scale * 11f, Palette.WALL_DEEP)
        c.drawRect(scale * 7f, scale * 9f, scale * 10f, scale * 10f, Palette.WALL_DEEP)
        c
    }

    fun blockTile(): PixCanvas = getOrPut("block") {
        val s = (TILE * scale).toFloat()
        val c = create()
        c.drawRect(0f, 0f, s, s, Palette.BLOCK_MID)
        c.drawRect(scale * 1f, scale * 1f, s - scale * 1f, scale * 2f, Palette.BLOCK_LIGHT)
        c.drawRect(scale * 1f, scale * 1f, scale * 2f, s - scale * 1f, Palette.BLOCK_LIGHT)
        c.drawRect(0f, scale * 7f, s, scale * 8f, Palette.BLOCK_DEEP)
        c.drawRect(0f, scale * 15f, s, scale * 16f, Palette.BLOCK_DEEP)
        c.drawRect(scale * 7f, scale * 8f, scale * 8f, scale * 15f, Palette.BLOCK_DEEP)
        c.drawRect(s - scale * 3f, scale * 9f, s - scale * 1f, s - scale * 1f, Palette.BLOCK_DARK)
        c
    }

    // ===================== Бомба =====================

    fun bomb(frame: Int): PixCanvas = getOrPut("bomb_$frame") {
        val s = (TILE * scale).toFloat()
        val c = create()
        val cx = s / 2f
        val cy = s * 0.58f
        val r = s * (if (frame == 0) 0.30f else 0.33f)
        drawPixelCircle(c, cx, cy, r, Palette.BOMB_MID)
        drawPixelCircle(c, cx - r * 0.3f, cy - r * 0.3f, r * 0.35f, Palette.BOMB_LIGHT)
        drawPixelCircle(c, cx + r * 0.35f, cy + r * 0.3f, r * 0.25f, Palette.BOMB_DARK)
        c.drawRect(cx - scale * 0.5f, cy - r - scale * 4f, cx + scale * 0.5f, cy - r, Palette.FUSE)
        if (frame == 1) {
            c.drawRect(cx - scale * 1.5f, cy - r - scale * 6f, cx + scale * 1.5f, cy - r - scale * 3f, Palette.SPARK)
            c.drawRect(cx - scale * 0.5f, cy - r - scale * 5f, cx + scale * 0.5f, cy - r - scale * 4f, Palette.SPARK_HOT)
        }
        c
    }

    // ===================== Взрыв =====================

    fun explosion(kind: Int, axis: Int, frame: Int): PixCanvas =
        getOrPut("expl_${kind}_${axis}_$frame") {
            val s = (TILE * scale).toFloat()
            val c = create()
            val (core, mid, outer) = when (frame) {
                0 -> Triple(Palette.EXPLO_CORE, Palette.EXPLO_INNER, Palette.EXPLO_MID)
                1 -> Triple(Palette.EXPLO_INNER, Palette.EXPLO_MID, Palette.EXPLO_OUTER)
                else -> Triple(Palette.EXPLO_MID, Palette.EXPLO_OUTER, Palette.EXPLO_FADE)
            }
            when (kind) {
                0 -> {
                    drawPixelCircle(c, s / 2f, s / 2f, s * 0.40f, outer)
                    drawPixelCircle(c, s / 2f, s / 2f, s * 0.30f, mid)
                    drawPixelCircle(c, s / 2f, s / 2f, s * 0.18f, core)
                }
                1 -> {
                    val t = s * 0.20f
                    if (axis == 0) {
                        c.drawRect(0f, s / 2f - t, s, s / 2f + t, outer)
                        c.drawRect(0f, s / 2f - t * 0.65f, s, s / 2f + t * 0.65f, mid)
                        c.drawRect(0f, s / 2f - t * 0.30f, s, s / 2f + t * 0.30f, core)
                    } else {
                        c.drawRect(s / 2f - t, 0f, s / 2f + t, s, outer)
                        c.drawRect(s / 2f - t * 0.65f, 0f, s / 2f + t * 0.65f, s, mid)
                        c.drawRect(s / 2f - t * 0.30f, 0f, s / 2f + t * 0.30f, s, core)
                    }
                }
                2 -> {
                    val t = s * 0.20f
                    if (axis == 0) {
                        c.drawRect(s / 2f, s / 2f - t, s, s / 2f + t, outer)
                        drawPixelCircle(c, s / 2f, s / 2f, t, outer)
                        c.drawRect(s / 2f, s / 2f - t * 0.6f, s, s / 2f + t * 0.6f, mid)
                        drawPixelCircle(c, s / 2f, s / 2f, t * 0.6f, mid)
                    } else {
                        c.drawRect(s / 2f - t, s / 2f, s / 2f + t, s, outer)
                        drawPixelCircle(c, s / 2f, s / 2f, t, outer)
                        c.drawRect(s / 2f - t * 0.6f, s / 2f, s / 2f + t * 0.6f, s, mid)
                        drawPixelCircle(c, s / 2f, s / 2f, t * 0.6f, mid)
                    }
                }
            }
            c
        }

    // ===================== Бонусы =====================

    fun bonus(type: BonusType, frame: Int): PixCanvas = getOrPut("bonus_${type}_$frame") {
        val s = (TILE * scale).toFloat()
        val c = create()
        val cx = s / 2f
        val bob = if (frame % 2 == 0) 0f else -scale * 1f
        val cy = s * 0.5f + bob
        drawPixelCircle(c, cx, s * 0.78f, s * 0.30f, argb(60, 255, 255, 255))
        when (type) {
            BonusType.FIRE -> {
                drawPixelTriangle(c, cx, cy - s * 0.28f, s * 0.24f, Palette.BONUS_FIRE_B)
                drawPixelTriangle(c, cx, cy - s * 0.20f, s * 0.15f, Palette.BONUS_FIRE_A)
                drawPixelTriangle(c, cx, cy - s * 0.12f, s * 0.07f, Palette.EXPLO_CORE)
            }
            BonusType.BOMB -> {
                drawPixelCircle(c, cx, cy + s * 0.05f, s * 0.22f, Palette.BONUS_BOMB_B)
                drawPixelCircle(c, cx - s * 0.06f, cy - s * 0.01f, s * 0.07f, Palette.BONUS_BOMB_A)
                c.drawRect(cx - scale * 0.5f, cy - s * 0.28f, cx + scale * 0.5f, cy - s * 0.14f, Palette.FUSE)
            }
            BonusType.SPEED -> {
                val pts = floatArrayOf(
                    cx + s * 0.18f, cy - s * 0.28f,
                    cx - s * 0.10f, cy + s * 0.02f,
                    cx + s * 0.02f, cy + s * 0.02f,
                    cx - s * 0.18f, cy + s * 0.28f,
                    cx + s * 0.10f, cy - s * 0.02f,
                    cx - s * 0.02f, cy - s * 0.02f
                )
                drawPixelPoly(c, pts, Palette.BONUS_SPEED_B)
                drawPixelPoly(
                    c,
                    pts.mapIndexed { i, v -> if (i % 2 == 0) v + scale * 0.5f else v - scale * 0.5f }.toFloatArray(),
                    Palette.BONUS_SPEED_A
                )
            }
            BonusType.LIFE -> {
                drawPixelCircle(c, cx - s * 0.09f, cy - s * 0.06f, s * 0.12f, Palette.BONUS_LIFE_B)
                drawPixelCircle(c, cx + s * 0.09f, cy - s * 0.06f, s * 0.12f, Palette.BONUS_LIFE_B)
                drawPixelTriangleDown(c, cx, cy + s * 0.22f, s * 0.21f, Palette.BONUS_LIFE_B)
                drawPixelCircle(c, cx - s * 0.05f, cy - s * 0.08f, s * 0.05f, Palette.BONUS_LIFE_A)
            }
            BonusType.STAR -> {
                drawPixelStar(c, cx, cy, s * 0.28f, Palette.BONUS_STAR_B)
                drawPixelStar(c, cx, cy, s * 0.18f, Palette.BONUS_STAR_A)
            }
        }
        c
    }

    // ===================== Выход =====================

    fun exit(frame: Int): PixCanvas = getOrPut("exit_$frame") {
        val s = (TILE * scale).toFloat()
        val c = create()
        val glow = if (frame % 2 == 0) 90 else 50
        drawPixelCircle(c, s / 2f, s / 2f, s * (if (frame % 2 == 0) 0.44f else 0.38f), argb(glow, 142, 232, 200))
        c.drawRect(s * 0.24f, s * 0.20f, s * 0.76f, s * 0.84f, Palette.EXIT_C)
        c.drawRect(s * 0.28f, s * 0.24f, s * 0.72f, s * 0.84f, Palette.EXIT_B)
        c.drawRect(s * 0.34f, s * 0.30f, s * 0.66f, s * 0.80f, Palette.EXIT_A)
        c.drawRect(s * 0.45f, s * 0.36f, s * 0.55f, s * 0.60f, Palette.EXIT_C)
        drawPixelTriangleDown(c, s / 2f, s * 0.68f, s * 0.10f, Palette.EXIT_C)
        c
    }

    // ===================== Героиня =====================

    fun mama(dir: Direction, frame: Int, pose: Int): PixCanvas =
        getOrPut("mama_${dir}_${frame}_$pose") {
            val s = (TILE * scale).toFloat()
            val c = create()
            val cx = s / 2f
            val step = frame % 4
            val legA = when (step) { 1 -> -1f; 3 -> 1f; else -> 0f }
            val legB = -legA
            when (pose) {
                2 -> drawMamaWin(c, s, cx)
                3 -> drawMamaLose(c, s, cx)
                else -> {
                    drawPixelEllipse(c, cx, s * 0.86f, s * 0.24f, s * 0.07f, argb(70, 26, 22, 38))
                    if (pose == 1) {
                        drawPixelEllipse(c, cx, s * 0.55f, s * 0.26f, s * 0.36f, argb(110, 245, 207, 168))
                    }
                    when (dir) {
                        Direction.DOWN -> drawMamaFace(c, s, cx, legA, legB, pose == 1)
                        Direction.UP -> drawMamaBack(c, s, cx, legA, legB)
                        Direction.LEFT, Direction.RIGHT -> drawMamaSide(c, s, cx, legA, legB, dir)
                        else -> drawMamaFace(c, s, cx, legA, legB, false)
                    }
                }
            }
            c
        }

    private fun drawMamaFace(c: PixCanvas, s: Float, cx: Float, legA: Float, legB: Float, hurt: Boolean) {
        val px = scale.toFloat()
        c.drawRect(cx - px * 3f, s * 0.74f, cx - px * 1f, s * 0.86f + legA * px, Palette.SHOE)
        c.drawRect(cx + px * 1f, s * 0.74f, cx + px * 3f, s * 0.86f + legB * px, Palette.SHOE)
        c.drawRect(cx - px * 4.5f, s * 0.42f, cx + px * 4.5f, s * 0.80f, Palette.DRESS_SHADE)
        c.drawRect(cx - px * 3.5f, s * 0.42f, cx + px * 3.5f, s * 0.78f, Palette.DRESS)
        c.drawRect(cx - px * 2f, s * 0.46f, cx + px * 2f, s * 0.74f, Palette.APRON)
        c.drawRect(cx - px * 2f, s * 0.60f, cx + px * 2f, s * 0.63f, Palette.APRON_SHADE)
        c.drawRect(cx - px * 5.5f, s * 0.44f, cx - px * 4.2f, s * 0.62f, Palette.SKIN)
        c.drawRect(cx + px * 4.2f, s * 0.44f, cx + px * 5.5f, s * 0.62f, Palette.SKIN)
        c.drawRect(cx - px * 1.2f, s * 0.36f, cx + px * 1.2f, s * 0.44f, Palette.SKIN_SHADE)
        drawPixelEllipse(c, cx, s * 0.27f, s * 0.17f, s * 0.17f, Palette.SKIN)
        drawPixelEllipse(c, cx, s * 0.20f, s * 0.19f, s * 0.13f, Palette.HAIR)
        c.drawRect(cx - px * 3.2f, s * 0.20f, cx + px * 3.2f, s * 0.30f, Palette.HAIR)
        c.drawRect(cx - px * 3.4f, s * 0.22f, cx - px * 2.2f, s * 0.40f, Palette.HAIR)
        c.drawRect(cx + px * 2.2f, s * 0.22f, cx + px * 3.4f, s * 0.40f, Palette.HAIR)
        c.drawRect(cx - px * 2.8f, s * 0.16f, cx + px * 2.8f, s * 0.21f, Palette.HAIR)
        c.drawRect(cx - px * 2.8f, s * 0.19f, cx - px * 1.4f, s * 0.22f, Palette.HAIR_SHADE)
        if (!hurt) {
            c.drawRect(cx - px * 1.8f, s * 0.26f, cx - px * 0.9f, s * 0.29f, Palette.EYE)
            c.drawRect(cx + px * 0.9f, s * 0.26f, cx + px * 1.8f, s * 0.29f, Palette.EYE)
            c.drawRect(cx - px * 2.6f, s * 0.30f, cx - px * 2.0f, s * 0.32f, Palette.CHEEK)
            c.drawRect(cx + px * 2.0f, s * 0.30f, cx + px * 2.6f, s * 0.32f, Palette.CHEEK)
            c.drawRect(cx - px * 0.8f, s * 0.33f, cx + px * 0.8f, s * 0.34f, Palette.HAIR_SHADE)
        } else {
            c.drawRect(cx - px * 1.8f, s * 0.25f, cx - px * 1.4f, s * 0.26f, Palette.EYE)
            c.drawRect(cx - px * 1.0f, s * 0.25f, cx - px * 0.6f, s * 0.26f, Palette.EYE)
            c.drawRect(cx + px * 1.0f, s * 0.25f, cx + px * 1.4f, s * 0.26f, Palette.EYE)
            c.drawRect(cx + px * 1.6f, s * 0.25f, cx + px * 2.0f, s * 0.26f, Palette.EYE)
        }
    }

    private fun drawMamaBack(c: PixCanvas, s: Float, cx: Float, legA: Float, legB: Float) {
        val px = scale.toFloat()
        c.drawRect(cx - px * 3f, s * 0.74f, cx - px * 1f, s * 0.86f + legA * px, Palette.SHOE)
        c.drawRect(cx + px * 1f, s * 0.74f, cx + px * 3f, s * 0.86f + legB * px, Palette.SHOE)
        c.drawRect(cx - px * 4.5f, s * 0.42f, cx + px * 4.5f, s * 0.80f, Palette.DRESS_SHADE)
        c.drawRect(cx - px * 3.5f, s * 0.42f, cx + px * 3.5f, s * 0.78f, Palette.DRESS)
        c.drawRect(cx - px * 2f, s * 0.54f, cx + px * 2f, s * 0.70f, Palette.APRON)
        c.drawRect(cx - px * 0.6f, s * 0.58f, cx + px * 0.6f, s * 0.66f, Palette.APRON_SHADE)
        c.drawRect(cx - px * 5.5f, s * 0.44f, cx - px * 4.2f, s * 0.62f, Palette.SKIN)
        c.drawRect(cx + px * 4.2f, s * 0.44f, cx + px * 5.5f, s * 0.62f, Palette.SKIN)
        drawPixelEllipse(c, cx, s * 0.26f, s * 0.18f, s * 0.18f, Palette.HAIR)
        c.drawRect(cx - px * 3.4f, s * 0.22f, cx + px * 3.4f, s * 0.38f, Palette.HAIR)
        c.drawRect(cx - px * 3.4f, s * 0.30f, cx + px * 3.4f, s * 0.34f, Palette.HAIR_SHADE)
        drawPixelEllipse(c, cx, s * 0.18f, s * 0.13f, s * 0.09f, Palette.DRESS)
    }

    private fun drawMamaSide(c: PixCanvas, s: Float, cx: Float, legA: Float, legB: Float, dir: Direction) {
        val px = scale.toFloat()
        val f = if (dir == Direction.RIGHT) 1f else -1f
        c.drawRect(cx - px * 1f, s * 0.74f, cx + px * 1f, s * 0.86f + legA * px, Palette.SHOE)
        c.drawRect(cx + px * 0.5f, s * 0.74f, cx + px * 2.5f, s * 0.86f + legB * px, Palette.SHOE)
        c.drawRect(cx - px * 3f, s * 0.42f, cx + px * 3.5f, s * 0.80f, Palette.DRESS_SHADE)
        c.drawRect(cx - px * 2.5f, s * 0.42f, cx + px * 2.8f, s * 0.78f, Palette.DRESS)
        c.drawRect(cx + f * px * 0.5f, s * 0.46f, cx + f * px * 2.8f, s * 0.74f, Palette.APRON)
        c.drawRect(cx - px * 3.6f, s * 0.44f, cx - px * 2.4f, s * 0.62f, Palette.SKIN)
        c.drawRect(cx + px * 2.6f, s * 0.44f, cx + px * 3.8f, s * 0.62f, Palette.SKIN)
        drawPixelEllipse(c, cx + f * px * 0.5f, s * 0.27f, s * 0.16f, s * 0.16f, Palette.SKIN)
        drawPixelEllipse(c, cx + f * px * 0.5f, s * 0.19f, s * 0.18f, s * 0.12f, Palette.HAIR)
        c.drawRect(cx - px * 2.4f, s * 0.20f, cx + px * 2.6f, s * 0.28f, Palette.HAIR)
        c.drawRect(cx + f * px * 0.5f, s * 0.16f, cx + f * px * 2.6f, s * 0.21f, Palette.HAIR)
        c.drawRect(cx + f * px * 1.0f, s * 0.26f, cx + f * px * 1.9f, s * 0.29f, Palette.EYE)
        c.drawRect(cx + f * px * 2.0f, s * 0.30f, cx + f * px * 2.5f, s * 0.32f, Palette.CHEEK)
    }

    private fun drawMamaWin(c: PixCanvas, s: Float, cx: Float) {
        val px = scale.toFloat()
        drawPixelCircle(c, cx, s * 0.5f, s * 0.46f, argb(80, 255, 224, 102))
        c.drawRect(cx - px * 5.5f, s * 0.12f, cx - px * 4.2f, s * 0.40f, Palette.SKIN)
        c.drawRect(cx + px * 4.2f, s * 0.12f, cx + px * 5.5f, s * 0.40f, Palette.SKIN)
        c.drawRect(cx - px * 3.5f, s * 0.42f, cx + px * 3.5f, s * 0.82f, Palette.DRESS)
        c.drawRect(cx - px * 2f, s * 0.46f, cx + px * 2f, s * 0.74f, Palette.APRON)
        drawPixelEllipse(c, cx, s * 0.27f, s * 0.17f, s * 0.17f, Palette.SKIN)
        drawPixelEllipse(c, cx, s * 0.20f, s * 0.19f, s * 0.13f, Palette.HAIR)
        c.drawRect(cx - px * 2.8f, s * 0.16f, cx + px * 2.8f, s * 0.28f, Palette.HAIR)
        c.drawRect(cx - px * 1.8f, s * 0.27f, cx - px * 0.9f, s * 0.28f, Palette.EYE)
        c.drawRect(cx + px * 0.9f, s * 0.27f, cx + px * 1.8f, s * 0.28f, Palette.EYE)
        c.drawRect(cx - px * 1.2f, s * 0.32f, cx + px * 1.2f, s * 0.34f, Palette.HAIR_SHADE)
        c.drawRect(cx - px * 6.5f, s * 0.08f, cx - px * 5.5f, s * 0.12f, Palette.EXPLO_CORE)
        c.drawRect(cx + px * 5.5f, s * 0.08f, cx + px * 6.5f, s * 0.12f, Palette.EXPLO_CORE)
    }

    private fun drawMamaLose(c: PixCanvas, s: Float, cx: Float) {
        val px = scale.toFloat()
        c.drawRect(cx - px * 3.5f, s * 0.46f, cx + px * 3.5f, s * 0.84f, Palette.DRESS_SHADE)
        drawPixelEllipse(c, cx + px * 1.5f, s * 0.40f, s * 0.16f, s * 0.15f, Palette.SKIN_SHADE)
        drawPixelEllipse(c, cx + px * 1.5f, s * 0.33f, s * 0.18f, s * 0.12f, Palette.HAIR_SHADE)
        c.drawRect(cx - px * 0.5f, s * 0.32f, cx + px * 3.2f, s * 0.42f, Palette.HAIR_SHADE)
        c.drawRect(cx - px * 1.2f, s * 0.38f, cx - px * 0.7f, s * 0.43f, Palette.EXIT_A)
        c.drawRect(cx - px * 4.6f, s * 0.50f, cx - px * 3.4f, s * 0.80f, Palette.SKIN_SHADE)
        c.drawRect(cx + px * 3.4f, s * 0.50f, cx + px * 4.6f, s * 0.80f, Palette.SKIN_SHADE)
    }

    // ===================== Враги =====================

    fun enemy(type: EnemyType, frame: Int, dead: Boolean): PixCanvas =
        getOrPut("enemy_${type}_${frame}_$dead") {
            val s = (TILE * scale).toFloat()
            val c = create()
            val cx = s / 2f
            if (dead) {
                drawEnemyDead(c, s, cx, type)
            } else {
                val bob = when (frame % 4) { 1 -> -scale * 1f; 3 -> scale * 1f; else -> 0f }
                when (type) {
                    EnemyType.SLOW -> drawSlime(c, s, cx, bob)
                    EnemyType.FAST -> drawBat(c, s, cx, frame)
                    EnemyType.CHANGER -> drawGhost(c, s, cx, frame)
                    EnemyType.HUNTER -> drawDemon(c, s, cx, frame)
                }
            }
            c
        }

    private fun drawSlime(c: PixCanvas, s: Float, cx: Float, bob: Float) {
        val px = scale.toFloat()
        drawPixelEllipse(c, cx, s * 0.84f, s * 0.26f, s * 0.06f, argb(70, 26, 22, 38))
        drawPixelEllipse(c, cx, s * 0.60f + bob, s * 0.28f, s * 0.24f, Palette.SLIME_C)
        drawPixelEllipse(c, cx, s * 0.56f + bob, s * 0.25f, s * 0.22f, Palette.SLIME_B)
        drawPixelEllipse(c, cx, s * 0.52f + bob, s * 0.20f, s * 0.17f, Palette.SLIME_A)
        c.drawRect(cx - px * 2.4f, s * 0.42f + bob, cx - px * 1.2f, s * 0.47f + bob, argb(140, 255, 255, 255))
        c.drawRect(cx - px * 2.2f, s * 0.26f + bob, cx - px * 1.8f, s * 0.36f + bob, Palette.SLIME_C)
        c.drawRect(cx + px * 1.8f, s * 0.26f + bob, cx + px * 2.2f, s * 0.36f + bob, Palette.SLIME_C)
        c.drawRect(cx - px * 2.4f, s * 0.22f + bob, cx - px * 1.6f, s * 0.27f + bob, Palette.SLIME_A)
        c.drawRect(cx + px * 1.6f, s * 0.22f + bob, cx + px * 2.4f, s * 0.27f + bob, Palette.SLIME_A)
        c.drawRect(cx - px * 1.9f, s * 0.56f + bob, cx - px * 1.0f, s * 0.61f + bob, Palette.EYE)
        c.drawRect(cx + px * 1.0f, s * 0.56f + bob, cx + px * 1.9f, s * 0.61f + bob, Palette.EYE)
        c.drawRect(cx - px * 0.8f, s * 0.66f + bob, cx + px * 0.8f, s * 0.68f + bob, Palette.SLIME_C)
    }

    private fun drawBat(c: PixCanvas, s: Float, cx: Float, frame: Int) {
        val px = scale.toFloat()
        val flap = if (frame % 2 == 0) 1 else -1
        drawPixelEllipse(c, cx, s * 0.86f, s * 0.24f, s * 0.05f, argb(60, 26, 22, 38))
        if (flap > 0) {
            drawPixelTriangle(c, cx - s * 0.28f, s * 0.44f, s * 0.16f, Palette.BAT_C)
            c.drawRect(cx - s * 0.40f, s * 0.44f, cx - s * 0.12f, s * 0.50f, Palette.BAT_C)
            drawPixelTriangle(c, cx + s * 0.28f, s * 0.44f, s * 0.16f, Palette.BAT_C)
            c.drawRect(cx + s * 0.12f, s * 0.44f, cx + s * 0.40f, s * 0.50f, Palette.BAT_C)
        } else {
            drawPixelTriangle(c, cx - s * 0.28f, s * 0.56f, s * 0.16f, Palette.BAT_C)
            c.drawRect(cx - s * 0.40f, s * 0.50f, cx - s * 0.12f, s * 0.56f, Palette.BAT_C)
            drawPixelTriangle(c, cx + s * 0.28f, s * 0.56f, s * 0.16f, Palette.BAT_C)
            c.drawRect(cx + s * 0.12f, s * 0.50f, cx + s * 0.40f, s * 0.56f, Palette.BAT_C)
        }
        drawPixelEllipse(c, cx, s * 0.50f, s * 0.15f, s * 0.17f, Palette.BAT_B)
        drawPixelEllipse(c, cx, s * 0.46f, s * 0.11f, s * 0.13f, Palette.BAT_A)
        drawPixelTriangle(c, cx - s * 0.12f, s * 0.28f, s * 0.06f, Palette.BAT_B)
        drawPixelTriangle(c, cx + s * 0.12f, s * 0.28f, s * 0.06f, Palette.BAT_B)
        c.drawRect(cx - px * 1.4f, s * 0.44f, cx - px * 0.7f, s * 0.47f, Palette.EXPLO_CORE)
        c.drawRect(cx + px * 0.7f, s * 0.44f, cx + px * 1.4f, s * 0.47f, Palette.EXPLO_CORE)
        c.drawRect(cx - px * 0.9f, s * 0.54f, cx - px * 0.4f, s * 0.57f, argb(220, 255, 255, 255))
        c.drawRect(cx + px * 0.4f, s * 0.54f, cx + px * 0.9f, s * 0.57f, argb(220, 255, 255, 255))
    }

    private fun drawGhost(c: PixCanvas, s: Float, cx: Float, frame: Int) {
        val px = scale.toFloat()
        val bob = if (frame % 2 == 0) -scale else scale
        drawPixelEllipse(c, cx, s * 0.86f, s * 0.24f, s * 0.05f, argb(50, 26, 22, 38))
        val body = argb(200, 122, 216, 212)
        drawPixelEllipse(c, cx, s * 0.48f + bob, s * 0.26f, s * 0.24f, body)
        c.drawRect(cx - s * 0.26f, s * 0.48f + bob, cx + s * 0.26f, s * 0.76f + bob, body)
        for (i in 0..3) {
            val x0 = cx - s * 0.26f + i * s * 0.13f
            c.drawRect(x0, s * 0.76f + bob, x0 + s * 0.07f, s * 0.82f + bob, body)
        }
        drawPixelEllipse(c, cx, s * 0.44f + bob, s * 0.20f, s * 0.18f, argb(230, 78, 163, 158))
        c.drawRect(cx - px * 2.0f, s * 0.42f + bob, cx - px * 1.0f, s * 0.46f + bob, Palette.EYE)
        c.drawRect(cx + px * 1.0f, s * 0.42f + bob, cx + px * 2.0f, s * 0.46f + bob, Palette.EYE)
        c.drawRect(cx - px * 1.0f, s * 0.54f + bob, cx, s * 0.56f + bob, Palette.GHOST_C)
        c.drawRect(cx, s * 0.56f + bob, cx + px * 1.0f, s * 0.58f + bob, Palette.GHOST_C)
    }

    private fun drawDemon(c: PixCanvas, s: Float, cx: Float, frame: Int) {
        val px = scale.toFloat()
        val bob = if (frame % 2 == 0) -scale else scale
        drawPixelEllipse(c, cx, s * 0.86f, s * 0.27f, s * 0.06f, argb(80, 26, 22, 38))
        drawPixelTriangle(c, cx - s * 0.16f, s * 0.20f, s * 0.07f, Palette.DEMON_C)
        drawPixelTriangle(c, cx + s * 0.16f, s * 0.20f, s * 0.07f, Palette.DEMON_C)
        drawPixelEllipse(c, cx, s * 0.36f, s * 0.17f, s * 0.14f, Palette.DEMON_C)
        drawPixelEllipse(c, cx, s * 0.33f, s * 0.13f, s * 0.11f, Palette.DEMON_B)
        drawPixelEllipse(c, cx, s * 0.62f + bob, s * 0.22f, s * 0.20f, Palette.DEMON_B)
        drawPixelEllipse(c, cx, s * 0.58f + bob, s * 0.17f, s * 0.15f, Palette.DEMON_A)
        c.drawRect(cx - px * 2.2f, s * 0.50f + bob, cx - px * 1.2f, s * 0.54f + bob, argb(110, 255, 255, 255))
        c.drawRect(cx - px * 4.4f, s * 0.52f, cx - px * 3.2f, s * 0.70f, Palette.DEMON_C)
        c.drawRect(cx + px * 3.2f, s * 0.52f, cx + px * 4.4f, s * 0.70f, Palette.DEMON_C)
        c.drawRect(cx - px * 1.8f, s * 0.32f, cx - px * 0.9f, s * 0.36f, Palette.EXPLO_CORE)
        c.drawRect(cx + px * 0.9f, s * 0.32f, cx + px * 1.8f, s * 0.36f, Palette.EXPLO_CORE)
        c.drawRect(cx - px * 1.2f, s * 0.40f, cx - px * 0.6f, s * 0.44f, argb(230, 255, 255, 255))
        c.drawRect(cx + px * 0.6f, s * 0.40f, cx + px * 1.2f, s * 0.44f, argb(230, 255, 255, 255))
    }

    private fun drawEnemyDead(c: PixCanvas, s: Float, cx: Float, type: EnemyType) {
        val base = when (type) {
            EnemyType.SLOW -> Palette.SLIME_C
            EnemyType.FAST -> Palette.BAT_C
            EnemyType.CHANGER -> Palette.GHOST_C
            EnemyType.HUNTER -> Palette.DEMON_C
        }
        drawPixelEllipse(c, cx, s * 0.72f, s * 0.28f, s * 0.10f, base)
        drawPixelEllipse(c, cx, s * 0.70f, s * 0.20f, s * 0.06f, argb(120, 255, 255, 255))
        c.drawRect(cx - scale * 2.4f, s * 0.68f, cx - scale * 1.8f, s * 0.74f, Palette.EYE)
        c.drawRect(cx - scale * 2.1f, s * 0.65f, cx - scale * 1.5f, s * 0.71f, Palette.EYE)
        c.drawRect(cx + scale * 1.8f, s * 0.68f, cx + scale * 2.4f, s * 0.74f, Palette.EYE)
        c.drawRect(cx + scale * 1.5f, s * 0.65f, cx + scale * 2.1f, s * 0.71f, Palette.EYE)
    }

    // ===================== Пиксельные примитивы =====================

    private fun drawPixelCircle(c: PixCanvas, cx: Float, cy: Float, r: Float, color: Int) {
        val ps = scale.toFloat()
        val rr = r * r
        var py = ((cy - r).toInt() / ps).toInt() * ps
        while (py <= cy + r) {
            val dy = py + ps / 2f - cy
            val dx = sqrt(maxOf(0.0, (rr - dy * dy).toDouble())).toFloat()
            val half = ((dx / ps).toInt()) * ps
            c.drawRect(cx - half, py, cx + half, py + ps, color)
            py += ps
        }
    }

    private fun drawPixelEllipse(c: PixCanvas, cx: Float, cy: Float, rx: Float, ry: Float, color: Int) {
        val ps = scale.toFloat()
        var py = ((cy - ry).toInt() / ps).toInt() * ps
        while (py <= cy + ry) {
            val dy = py + ps / 2f - cy
            val t = dy / ry
            if (t * t <= 1f) {
                val dx = rx * sqrt(1.0 - t * t.toDouble()).toFloat()
                val half = ((dx / ps).toInt()) * ps
                c.drawRect(cx - half, py, cx + half, py + ps, color)
            }
            py += ps
        }
    }

    private fun drawPixelTriangle(c: PixCanvas, cx: Float, topY: Float, half: Float, color: Int) {
        val ps = scale.toFloat()
        var y = topY
        var h = 0f
        while (h < half * 2f) {
            val w = half * (h / (half * 2f))
            c.drawRect(cx - w, y, cx + w, y + ps, color)
            y += ps
            h += ps
        }
    }

    private fun drawPixelTriangleDown(c: PixCanvas, cx: Float, bottomY: Float, half: Float, color: Int) {
        val ps = scale.toFloat()
        var y = bottomY
        var h = 0f
        while (h < half * 2f) {
            val w = half * (h / (half * 2f))
            c.drawRect(cx - w, y - ps, cx + w, y, color)
            y -= ps
            h += ps
        }
    }

    private fun drawPixelPoly(c: PixCanvas, pts: FloatArray, color: Int) {
        var minX = Float.MAX_VALUE; var maxX = Float.MIN_VALUE
        var minY = Float.MAX_VALUE; var maxY = Float.MIN_VALUE
        var i = 0
        while (i < pts.size) {
            minX = min(minX, pts[i]); maxX = max(maxX, pts[i])
            minY = min(minY, pts[i + 1]); maxY = max(maxY, pts[i + 1])
            i += 2
        }
        val ps = scale.toFloat()
        var y = ((minY / ps).toInt()) * ps
        while (y <= maxY) {
            var x = ((minX / ps).toInt()) * ps
            while (x <= maxX) {
                if (pointInPoly(x + ps / 2f, y + ps / 2f, pts)) {
                    c.drawRect(x, y, x + ps, y + ps, color)
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

    private fun drawPixelStar(c: PixCanvas, cx: Float, cy: Float, r: Float, color: Int) {
        drawPixelCircle(c, cx, cy, r * 0.55f, color)
        for (k in 0 until 5) {
            val a = -Math.PI / 2 + k * 2 * Math.PI / 5
            val tipX = (cx + r * Math.cos(a)).toFloat()
            val tipY = (cy + r * Math.sin(a)).toFloat()
            drawPixelCircle(c, (tipX + cx) / 2f, (tipY + cy) / 2f, r * 0.18f, color)
            c.drawRect(cx, cy, tipX, tipY, color)
        }
    }

    private fun argb(a: Int, r: Int, g: Int, b: Int): Int =
        (a shl 24) or (r shl 16) or (g shl 8) or b

    private fun getOrPut(key: String, create: () -> PixCanvas): PixCanvas =
        cache[key] ?: create().also { cache[key] = it }

    private fun max(a: Float, b: Float) = if (a > b) a else b

    companion object {
        const val TILE = 16
    }
}
