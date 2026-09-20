package com.bombermama.game

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import com.bombermama.core.BonusType
import com.bombermama.core.Direction
import com.bombermama.core.GameEngine
import com.bombermama.core.GameState
import com.bombermama.core.Tile
import com.bombermama.input.TouchInputController
import com.bombermama.render.Palette
import com.bombermama.render.SpriteFactory
import kotlin.math.max
import kotlin.math.min

/**
 * Отрисовщик игрового мира на Canvas.
 *
 * Поле центрируется на экране; размер клетки подбирается так, чтобы
 * поле поместилось целиком. Спрайты кешируются в [SpriteFactory],
 * поэтому аллокаций в кадре нет.
 *
 * Отрисовка идёт слоями:
 * пол → стены/блоки → бомбы → бонусы → выход → взрывы → враги →
 * героиня → частицы → HUD → оверлеи (пауза/победа/поражение).
 */
class GameRenderer(private val sprites: SpriteFactory) {

    private var viewWidth = 0
    private var viewHeight = 0

    /** Левый верхний угол поля в пикселях. */
    private var offsetX = 0f
    private var offsetY = 0f
    /** Размер клетки в пикселях. */
    private var tileSize = 32f

    private val paint = Paint().apply { isAntiAlias = false; isFilterBitmap = false }

    private val srcRect = Rect()
    private val dstRect = Rect()

    // Прямоугольник HUD сверху.
    private val hudPaint = Paint().apply { isAntiAlias = true }
    private val hudText = Paint().apply {
        isAntiAlias = true
        color = Palette.UI_TEXT
        textAlign = Paint.Align.LEFT
    }
    private val dpadPaint = Paint().apply { isAntiAlias = true }
    private val bombBtnPaint = Paint().apply { isAntiAlias = true }

    fun onSizeChanged(width: Int, height: Int, fieldW: Int, fieldH: Int) {
        viewWidth = width
        viewHeight = height
        // Поле должно помещаться в область под HUD.
        val hudH = height * HUD_HEIGHT_FRACTION
        val availW = width * 0.98f
        val availH = height - hudH - height * 0.02f
        tileSize = min(availW / fieldW, availH / fieldH).coerceAtLeast(8f)
        offsetX = (width - fieldW * tileSize) / 2f
        offsetY = hudH + (availH - fieldH * tileSize) / 2f
        hudText.textSize = height * 0.030f
    }

    fun draw(
        canvas: Canvas,
        engine: GameEngine,
        prevPlayerX: Float,
        prevPlayerY: Float,
        alpha: Float,
        input: TouchInputController,
        width: Int,
        height: Int
    ) {
        if (viewWidth == 0) {
            onSizeChanged(width, height, engine.field.width, engine.field.height)
        }

        // Фон.
        canvas.drawColor(Palette.BLACK)

        drawFieldFrame(canvas, engine)
        drawField(canvas, engine)
        drawBombs(canvas, engine)
        drawBonuses(canvas, engine)
        drawExit(canvas, engine)
        drawExplosions(canvas, engine)
        drawEnemies(canvas, engine)
        drawPlayer(canvas, engine, prevPlayerX, prevPlayerY, alpha)
        drawHud(canvas, engine, height)
        drawTouchControls(canvas, input, height)

        // Затемнение на паузе.
        if (engine.state == GameState.PAUSED) {
            canvas.drawColor(Color.argb(150, 27, 33, 72))
        }
    }

    // =========================================================================
    // Рамка поля
    // =========================================================================

    private val framePaint = Paint().apply { isAntiAlias = false }

    /** Декоративная рамка вокруг поля + мягкая тень под ним. */
    private fun drawFieldFrame(canvas: Canvas, engine: GameEngine) {
        val f = engine.field
        val left = offsetX - tileSize * 0.18f
        val top = offsetY - tileSize * 0.18f
        val right = offsetX + f.width * tileSize + tileSize * 0.18f
        val bottom = offsetY + f.height * tileSize + tileSize * 0.18f
        // Внешняя тень.
        framePaint.color = Color.argb(90, 0, 0, 0)
        canvas.drawRect(left + tileSize * 0.1f, top + tileSize * 0.12f, right + tileSize * 0.1f, bottom + tileSize * 0.12f, framePaint)
        // Тёмная каёмка.
        framePaint.color = Palette.WALL_DEEP
        canvas.drawRect(left, top, right, bottom, framePaint)
        // Светлая внутренняя грань.
        framePaint.color = Palette.WALL_LIGHT
        val inset = tileSize * 0.06f
        canvas.drawRect(left + inset, top + inset, right - inset, top + inset + tileSize * 0.06f, framePaint)
        canvas.drawRect(left + inset, top + inset, left + inset + tileSize * 0.06f, bottom - inset, framePaint)
    }

    // =========================================================================
    // Пол
    // =========================================================================

    private fun drawField(canvas: Canvas, engine: GameEngine) {
        val f = engine.field
        for (y in 0 until f.height) {
            for (x in 0 until f.width) {
                val tile = f[x, y]
                if (tile == Tile.FLOOR || tile == Tile.EXIT) {
                    val bmp = sprites.floorTile((x + y) % 2 == 0)
                    blit(canvas, bmp, tileLeft(x), tileTop(y), tileSize)
                }
            }
        }
        for (y in 0 until f.height) {
            for (x in 0 until f.width) {
                when (f[x, y]) {
                    Tile.WALL -> {
                        val bmp = sprites.wallTile()
                        blit(canvas, bmp, tileLeft(x), tileTop(y), tileSize)
                    }
                    Tile.BLOCK -> {
                        val bmp = sprites.blockTile()
                        blit(canvas, bmp, tileLeft(x), tileTop(y), tileSize)
                    }
                    else -> {}
                }
            }
        }
    }

    // =========================================================================
    // Сущности
    // =========================================================================

    private fun drawBombs(canvas: Canvas, engine: GameEngine) {
        for (bomb in engine.bombs) {
            if (!bomb.isActive) continue
            val bmp = sprites.bomb(bomb.blinkPhase)
            blit(canvas, bmp, tileLeft(bomb.tileX), tileTop(bomb.tileY), tileSize)
        }
    }

    private fun drawBonuses(canvas: Canvas, engine: GameEngine) {
        for (bonus in engine.bonuses) {
            if (bonus.picked) continue
            val bmp = sprites.bonus(bonus.type, bonus.bobPhase)
            blit(canvas, bmp, tileLeft(bonus.tileX), tileTop(bonus.tileY), tileSize)
        }
    }

    private fun drawExit(canvas: Canvas, engine: GameEngine) {
        val (ex, ey) = engine.field.exitTile ?: return
        if (engine.field[ex, ey] != Tile.EXIT) return
        val frame = ((engine.time * 4f).toInt() % 2)
        val bmp = sprites.exit(frame)
        blit(canvas, bmp, tileLeft(ex), tileTop(ey), tileSize)
    }

    private fun drawExplosions(canvas: Canvas, engine: GameEngine) {
        for (seg in engine.explosions) {
            val kind = if (seg.isCenter) 0 else if (seg.isTip) 2 else 1
            // Ось рукава: для горизонтального — 0, вертикального — 1.
            val axis = if (seg.direction == Direction.LEFT || seg.direction == Direction.RIGHT) 0 else 1
            val bmp = sprites.explosion(kind, axis, seg.animPhase)
            blit(canvas, bmp, tileLeft(seg.tileX), tileTop(seg.tileY), tileSize)
        }
    }

    private fun drawEnemies(canvas: Canvas, engine: GameEngine) {
        for (enemy in engine.enemies) {
            val frame = ((enemy.animPhase(0f) * 4f).toInt() % 4)
            val bmp = sprites.enemy(enemy.type, frame, enemy.dead)
            // Лёгкое смещение по вертикали для "парящих" врагов.
            val bob = if (enemy.dead) 0f else {
                when (enemy.type) {
                    com.bombermama.core.EnemyType.FAST, com.bombermama.core.EnemyType.CHANGER ->
                        Math.sin((enemy.animPhase(0f) * Math.PI * 2).toDouble()).toFloat() * tileSize * 0.06f
                    else -> 0f
                }
            }
            blit(
                canvas, bmp,
                offsetX + enemy.x * tileSize - tileSize / 2f,
                offsetY + enemy.y * tileSize - tileSize / 2f + bob,
                tileSize
            )
        }
    }

    private fun drawPlayer(
        canvas: Canvas,
        engine: GameEngine,
        prevX: Float,
        prevY: Float,
        alpha: Float
    ) {
        val p = engine.player
        // Плавная интерполяция между логическими шагами.
        val x = prevX + (p.x - prevX) * alpha
        val y = prevY + (p.y - prevY) * alpha
        val pose = when {
            p.dead -> 3
            engine.state == GameState.WIN -> 2
            p.invulnerableTime > 0f && ((p.invulnerableTime * 10f).toInt() % 2 == 0) -> 1
            else -> 0
        }
        val dir = if (p.direction == Direction.NONE) Direction.DOWN else p.direction
        val frame = ((p.animPhase(0f) * 4f).toInt() % 4)
        val bmp = sprites.mama(dir, frame, pose)
        blit(
            canvas, bmp,
            offsetX + x * tileSize - tileSize / 2f,
            offsetY + y * tileSize - tileSize / 2f,
            tileSize
        )
    }

    // =========================================================================
    // HUD
    // =========================================================================

    private fun drawHud(canvas: Canvas, engine: GameEngine, height: Int) {
        val hudH = height * HUD_HEIGHT_FRACTION
        hudPaint.color = Palette.UI_PANEL
        canvas.drawRect(0f, 0f, viewWidth.toFloat(), hudH, hudPaint)
        hudPaint.color = Palette.UI_ACCENT
        canvas.drawRect(0f, hudH - height * 0.006f, viewWidth.toFloat(), hudH, hudPaint)

        val p = engine.player
        val pad = viewWidth * 0.02f
        hudText.textSize = height * 0.028f
        hudText.textAlign = Paint.Align.LEFT

        val items = listOf(
            "❤ $p.lives" to Palette.UI_RED,
            "💣 ${p.maxBombs}" to Palette.UI_TEXT,
            "🔥 ${p.fireRange}" to Palette.BONUS_FIRE_A,
            "⭐ ${p.score}" to Palette.BONUS_STAR_A
        )
        var x = pad
        val y = hudH * 0.55f - hudText.descent() / 2f
        for ((text, color) in items) {
            hudText.color = color
            canvas.drawText(text, x, y, hudText)
            x += hudText.measureText(text) + viewWidth * 0.035f
        }

        // Номер уровня и время — справа.
        hudText.color = Palette.UI_TEXT
        hudText.textAlign = Paint.Align.RIGHT
        val levelText = "УРОВЕНЬ ${engine.spec.number}"
        canvas.drawText(levelText, viewWidth - pad, y, hudText)
        if (engine.timeLeft >= 0f) {
            hudText.color = if (engine.timeLeft < 30f) Palette.UI_RED else Palette.UI_DIM
            canvas.drawText("⏱ ${engine.timeLeft.toInt()}", viewWidth - pad, hudH * 0.30f, hudText)
        }
        hudText.textAlign = Paint.Align.LEFT

        // Подсказка о выходе.
        if (engine.enemiesCleared) {
            hudText.color = Palette.UI_GREEN
            hudText.textAlign = Paint.Align.CENTER
            hudText.textSize = height * 0.024f
            canvas.drawText("ВЫХОД ОТКРЫТ — НАЙДИТЕ ДВЕРЬ!", viewWidth / 2f, hudH * 0.75f, hudText)
            hudText.textAlign = Paint.Align.LEFT
        }
    }

    // =========================================================================
    // Сенсорное управление
    // =========================================================================

    private fun drawTouchControls(canvas: Canvas, input: TouchInputController, height: Int) {
        // D-pad: круглый базовый круг + стрелка текущего направления.
        val z = input.dpadZone
        val cx = (z.left + z.right) / 2f
        val cy = (z.top + z.bottom) / 2f
        val r = (z.right - z.left) / 2f

        dpadPaint.color = Color.argb(60, 246, 227, 200)
        canvas.drawCircle(cx, cy, r, dpadPaint)
        dpadPaint.color = Color.argb(120, 246, 227, 200)
        dpadPaint.style = Paint.Style.STROKE
        dpadPaint.strokeWidth = height * 0.004f
        canvas.drawCircle(cx, cy, r, dpadPaint)
        dpadPaint.style = Paint.Style.FILL

        // Стрелки.
        dpadPaint.color = Color.argb(150, 246, 227, 200)
        val arr = r * 0.30f
        for (d in Direction.MOVING) {
            val ax = cx + d.dx * r * 0.62f
            val ay = cy + d.dy * r * 0.62f
            drawArrow(canvas, ax, ay, d, arr, dpadPaint)
        }

        // Активное направление — подсветим.
        if (input.direction != Direction.NONE) {
            dpadPaint.color = Color.argb(200, 224, 85, 122)
            val ax = cx + input.direction.dx * r * 0.62f
            val ay = cy + input.direction.dy * r * 0.62f
            drawArrow(canvas, ax, ay, input.direction, arr * 1.2f, dpadPaint)
        }

        // Кнопка бомбы.
        val bz = input.bombZone
        val bcx = (bz.left + bz.right) / 2f
        val bcy = (bz.top + bz.bottom) / 2f
        val br = (bz.right - bz.left) / 2f
        bombBtnPaint.color = if (input.bombPressed) Palette.BONUS_FIRE_B else Color.argb(70, 224, 85, 122)
        canvas.drawCircle(bcx, bcy, br, bombBtnPaint)
        bombBtnPaint.color = Palette.UI_TEXT
        bombBtnPaint.textSize = br * 0.5f
        bombBtnPaint.textAlign = Paint.Align.CENTER
        canvas.drawText("💣", bcx, bcy + br * 0.18f, bombBtnPaint)
    }

    private fun drawArrow(c: Canvas, x: Float, y: Float, dir: Direction, size: Float, p: Paint) {
        when (dir) {
            Direction.UP -> {
                c.drawRect(x - size * 0.3f, y - size, x + size * 0.3f, y, p)
                c.drawRect(x - size * 0.6f, y - size * 0.7f, x + size * 0.6f, y - size * 0.4f, p)
            }
            Direction.DOWN -> {
                c.drawRect(x - size * 0.3f, y, x + size * 0.3f, y + size, p)
                c.drawRect(x - size * 0.6f, y + size * 0.4f, x + size * 0.6f, y + size * 0.7f, p)
            }
            Direction.LEFT -> {
                c.drawRect(x - size, y - size * 0.3f, x, y + size * 0.3f, p)
                c.drawRect(x - size * 0.7f, y - size * 0.6f, x - size * 0.4f, y + size * 0.6f, p)
            }
            Direction.RIGHT -> {
                c.drawRect(x, y - size * 0.3f, x + size, y + size * 0.3f, p)
                c.drawRect(x + size * 0.4f, y - size * 0.6f, x + size * 0.7f, y + size * 0.6f, p)
            }
            else -> {}
        }
    }

    // =========================================================================
    // Утилиты
    // =========================================================================

    private fun tileLeft(tx: Int): Float = offsetX + tx * tileSize
    private fun tileTop(ty: Int): Float = offsetY + ty * tileSize

    private fun blit(canvas: Canvas, bmp: android.graphics.Bitmap, x: Float, y: Float, size: Float) {
        srcRect.set(0, 0, bmp.width, bmp.height)
        dstRect.set(x.toInt(), y.toInt(), (x + size).toInt(), (y + size).toInt())
        canvas.drawBitmap(bmp, srcRect, dstRect, paint)
    }

    companion object {
        private const val HUD_HEIGHT_FRACTION = 0.10f
    }
}
