package com.bombermama.screenshot

import com.bombermama.core.BonusType
import com.bombermama.core.Direction
import com.bombermama.core.EnemyType
import com.bombermama.core.GameEngine
import com.bombermama.core.GameState
import com.bombermama.core.Tile
import kotlin.math.max
import kotlin.math.min

/**
 * Рендерер игрового мира — порт GameRenderer из app-модуля.
 *
 * Повторяет ту же послойную отрисовку (пол → стены → бомбы → бонусы →
 * выход → взрывы → враги → героиня → HUD → управление) с теми же формулами
 * смещений и размера клетки, что и в игре.
 */
class SceneRenderer(private val sprites: SpriteFactory) {

    private var viewWidth = 0
    private var viewHeight = 0
    private var offsetX = 0f
    private var offsetY = 0f
    private var tileSize = 32f

    fun onSizeChanged(width: Int, height: Int, fieldW: Int, fieldH: Int) {
        viewWidth = width
        viewHeight = height
        val hudH = height * HUD_HEIGHT_FRACTION
        val availW = width * 0.98f
        val availH = height - hudH - height * 0.02f
        tileSize = min(availW / fieldW, availH / fieldH).coerceAtLeast(8f)
        offsetX = (width - fieldW * tileSize) / 2f
        offsetY = hudH + (availH - fieldH * tileSize) / 2f
    }

    fun draw(
        canvas: PixCanvas,
        engine: GameEngine,
        input: TouchZones,
        paused: Boolean = false
    ) {
        canvas.clear(Palette.BLACK)
        drawField(canvas, engine)
        drawBombs(canvas, engine)
        drawBonuses(canvas, engine)
        drawExit(canvas, engine)
        drawExplosions(canvas, engine)
        drawEnemies(canvas, engine)
        drawPlayer(canvas, engine)
        drawHud(canvas, engine)
        drawTouchControls(canvas, input)
        if (paused || engine.state == GameState.PAUSED) {
            canvas.drawRect(0f, 0f, viewWidth.toFloat(), viewHeight.toFloat(), argb(140, 26, 22, 38))
        }
    }

    private fun drawField(canvas: PixCanvas, engine: GameEngine) {
        val f = engine.field
        for (y in 0 until f.height) for (x in 0 until f.width) {
            val t = f[x, y]
            if (t == Tile.FLOOR || t == Tile.EXIT) {
                blit(canvas, sprites.floorTile((x + y) % 2 == 0), tileLeft(x), tileTop(y), tileSize)
            }
        }
        for (y in 0 until f.height) for (x in 0 until f.width) {
            when (f[x, y]) {
                Tile.WALL -> blit(canvas, sprites.wallTile(), tileLeft(x), tileTop(y), tileSize)
                Tile.BLOCK -> blit(canvas, sprites.blockTile(), tileLeft(x), tileTop(y), tileSize)
                else -> {}
            }
        }
    }

    private fun drawBombs(canvas: PixCanvas, engine: GameEngine) {
        for (bomb in engine.bombs) {
            if (!bomb.isActive) continue
            blit(canvas, sprites.bomb(bomb.blinkPhase), tileLeft(bomb.tileX), tileTop(bomb.tileY), tileSize)
        }
    }

    private fun drawBonuses(canvas: PixCanvas, engine: GameEngine) {
        for (bonus in engine.bonuses) {
            if (bonus.picked) continue
            blit(canvas, sprites.bonus(bonus.type, bonus.bobPhase), tileLeft(bonus.tileX), tileTop(bonus.tileY), tileSize)
        }
    }

    private fun drawExit(canvas: PixCanvas, engine: GameEngine) {
        val (ex, ey) = engine.field.exitTile ?: return
        if (engine.field[ex, ey] != Tile.EXIT) return
        val frame = ((engine.time * 4f).toInt() % 2)
        blit(canvas, sprites.exit(frame), tileLeft(ex), tileTop(ey), tileSize)
    }

    private fun drawExplosions(canvas: PixCanvas, engine: GameEngine) {
        for (seg in engine.explosions) {
            val kind = if (seg.isCenter) 0 else if (seg.isTip) 2 else 1
            val axis = if (seg.direction == Direction.LEFT || seg.direction == Direction.RIGHT) 0 else 1
            blit(canvas, sprites.explosion(kind, axis, seg.animPhase), tileLeft(seg.tileX), tileTop(seg.tileY), tileSize)
        }
    }

    private fun drawEnemies(canvas: PixCanvas, engine: GameEngine) {
        for (enemy in engine.enemies) {
            val frame = ((enemy.animPhase(0f) * 4f).toInt() % 4)
            val bob = if (enemy.dead) 0f else {
                when (enemy.type) {
                    EnemyType.FAST, EnemyType.CHANGER ->
                        kotlin.math.sin((enemy.animPhase(0f) * Math.PI * 2).toDouble()).toFloat() * tileSize * 0.06f
                    else -> 0f
                }
            }
            blit(
                canvas, sprites.enemy(enemy.type, frame, enemy.dead),
                offsetX + enemy.x * tileSize - tileSize / 2f,
                offsetY + enemy.y * tileSize - tileSize / 2f + bob,
                tileSize
            )
        }
    }

    private fun drawPlayer(canvas: PixCanvas, engine: GameEngine) {
        val p = engine.player
        val pose = when {
            p.dead -> 3
            engine.state == GameState.WIN -> 2
            p.invulnerableTime > 0f && ((p.invulnerableTime * 10f).toInt() % 2 == 0) -> 1
            else -> 0
        }
        val dir = if (p.direction == Direction.NONE) Direction.DOWN else p.direction
        val frame = ((p.animPhase(0f) * 4f).toInt() % 4)
        blit(
            canvas, sprites.mama(dir, frame, pose),
            offsetX + p.x * tileSize - tileSize / 2f,
            offsetY + p.y * tileSize - tileSize / 2f,
            tileSize
        )
    }

    // ===================== HUD =====================

    private fun drawHud(canvas: PixCanvas, engine: GameEngine) {
        val hudH = viewHeight * HUD_HEIGHT_FRACTION
        canvas.drawRect(0f, 0f, viewWidth.toFloat(), hudH, Palette.UI_PANEL)
        canvas.drawRect(0f, hudH - viewHeight * 0.006f, viewWidth.toFloat(), hudH, Palette.UI_ACCENT)

        val p = engine.player
        val pad = viewWidth * 0.02f
        val fontH = viewHeight * 0.026f
        val items = listOf(
            Triple("HEART", "${p.lives}", Palette.UI_RED),
            Triple("BOMB", "${p.maxBombs}", Palette.UI_TEXT),
            Triple("FIRE", "${p.fireRange}", Palette.BONUS_FIRE_A),
            Triple("STAR", "${p.score}", Palette.BONUS_STAR_A)
        )
        var x = pad
        val y = hudH * 0.5f - fontH * 0.5f
        for ((icon, text, color) in items) {
            drawHudIcon(canvas, icon, x, y, fontH * 2.4f)
            canvas.drawText(text, x + fontH * 2.8f, y, fontH, color)
            x += fontH * 2.8f + canvas.textWidth(text, fontH) + viewWidth * 0.03f
        }

        // Номер уровня и время — справа.
        val levelText = "УРОВЕНЬ ${engine.spec.number}"
        canvas.drawText(levelText, viewWidth - pad, hudH * 0.30f, fontH * 0.9f, Palette.UI_TEXT, center = false)
        if (engine.timeLeft >= 0f) {
            val tc = if (engine.timeLeft < 30f) Palette.UI_RED else Palette.UI_DIM
            canvas.drawText("${engine.timeLeft.toInt()}", viewWidth - pad, hudH * 0.62f, fontH * 0.9f, tc)
        }

        if (engine.enemiesCleared) {
            canvas.drawText(
                "ВЫХОД ОТКРЫТ", viewWidth / 2f, hudH * 0.75f, fontH * 0.85f,
                Palette.UI_GREEN, center = true
            )
        }
    }

    /** Маленькие пиксельные иконки в HUD: сердце, бомба, огонь, звезда. */
    private fun drawHudIcon(canvas: PixCanvas, kind: String, x: Float, y: Float, size: Float) {
        when (kind) {
            "HEART" -> {
                canvas.drawRect(x, y + size * 0.1f, x + size * 0.45f, y + size * 0.5f, Palette.BONUS_LIFE_B)
                canvas.drawRect(x + size * 0.55f, y + size * 0.1f, x + size, y + size * 0.5f, Palette.BONUS_LIFE_B)
                canvas.drawRect(x + size * 0.1f, y + size * 0.4f, x + size * 0.9f, y + size * 0.75f, Palette.BONUS_LIFE_B)
                canvas.drawRect(x + size * 0.25f, y + size * 0.7f, x + size * 0.75f, y + size, Palette.BONUS_LIFE_B)
            }
            "BOMB" -> {
                canvas.drawRect(x + size * 0.1f, y + size * 0.35f, x + size * 0.9f, y + size * 0.95f, Palette.BOMB_MID)
                canvas.drawRect(x + size * 0.2f, y + size * 0.45f, x + size * 0.5f, y + size * 0.7f, Palette.BOMB_LIGHT)
                canvas.drawRect(x + size * 0.45f, y + size * 0.05f, x + size * 0.55f, y + size * 0.35f, Palette.FUSE)
            }
            "FIRE" -> {
                canvas.drawRect(x + size * 0.3f, y, x + size * 0.7f, y + size * 0.4f, Palette.BONUS_FIRE_B)
                canvas.drawRect(x + size * 0.2f, y + size * 0.35f, x + size * 0.8f, y + size * 0.7f, Palette.BONUS_FIRE_B)
                canvas.drawRect(x + size * 0.1f, y + size * 0.65f, x + size * 0.9f, y + size, Palette.BONUS_FIRE_B)
                canvas.drawRect(x + size * 0.35f, y + size * 0.3f, x + size * 0.65f, y + size * 0.6f, Palette.BONUS_FIRE_A)
                canvas.drawRect(x + size * 0.4f, y + size * 0.5f, x + size * 0.6f, y + size * 0.8f, Palette.EXPLO_CORE)
            }
            "STAR" -> {
                canvas.drawRect(x + size * 0.4f, y, x + size * 0.6f, y + size, Palette.BONUS_STAR_A)
                canvas.drawRect(x, y + size * 0.4f, x + size, y + size * 0.6f, Palette.BONUS_STAR_A)
                canvas.drawRect(x + size * 0.15f, y + size * 0.15f, x + size * 0.85f, y + size * 0.85f, Palette.BONUS_STAR_B)
                canvas.drawRect(x + size * 0.35f, y + size * 0.35f, x + size * 0.65f, y + size * 0.65f, Palette.BONUS_STAR_A)
            }
        }
    }

    // ===================== Главное меню =====================

    /**
     * Рисует главное меню в том же стиле, что и activity_main.xml:
     * тёмный фон с пиксельной брусчаткой, большая рамка-логотип,
     * героиня с бомбой и шесть кнопок с пиксельными иконками.
     */
    fun drawMenu(canvas: PixCanvas, selected: Int = 0) {
        canvas.clear(Palette.UI_BG)
        // Лёгкая пиксельная текстура фона.
        val ps = 24f
        var y = 0f
        while (y < viewHeight) {
            var x = 0f
            while (x < viewWidth) {
                if (((x / ps).toInt() + (y / ps).toInt()) % 2 == 0) {
                    canvas.drawRect(x, y, x + ps, y + ps, Palette.DARK)
                }
                x += ps
            }
            y += ps
        }
        // Декоративная бомба в углу.
        drawHudIcon(canvas, "BOMB", viewWidth * 0.06f, viewHeight * 0.06f, viewHeight * 0.09f)

        // Логотип-рамка.
        val logoW = viewWidth * 0.62f
        val logoH = viewHeight * 0.20f
        val lx = (viewWidth - logoW) / 2f
        val ly = viewHeight * 0.06f
        drawPanel(canvas, lx, ly, logoW, logoH)
        canvas.drawText(
            "BOMBERMAMA", viewWidth / 2f, ly + logoH * 0.34f, logoH * 0.34f,
            Palette.UI_ACCENT, center = true
        )
        canvas.drawText(
            "СОВРЕМЕННАЯ КЛАССИКА", viewWidth / 2f, ly + logoH * 0.70f, logoH * 0.16f,
            Palette.UI_TEXT, center = true
        )

        // Героиня по центру.
        val mama = sprites.mama(Direction.DOWN, 0, 0)
        val ms = viewHeight * 0.16f
        canvas.drawBitmap(
            mama, viewWidth / 2f - ms / 2f, ly + logoH + viewHeight * 0.02f, ms, ms * 1.5f
        )

        // Кнопки в два столбца.
        val buttons = listOf(
            "НОВАЯ ИГРА" to "PLAY", "УРОВНИ" to "MAP",
            "РЕКОРДЫ" to "STAR", "НАСТРОЙКИ" to "GEAR",
            "ПОМОЩЬ" to "HELP", "ВЫХОД" to "DOOR"
        )
        val bw = viewWidth * 0.42f
        val bh = viewHeight * 0.085f
        val gap = viewHeight * 0.014f
        val startY = ly + logoH + viewHeight * 0.22f
        for ((i, pair) in buttons.withIndex()) {
            val col = i % 2
            val row = i / 2
            val bx = if (col == 0) viewWidth * 0.05f else viewWidth * 0.53f
            val by = startY + row * (bh + gap)
            drawMenuButton(canvas, bx, by, bw, bh, pair.first, pair.second, i == selected)
        }
    }

    private fun drawMenuButton(
        canvas: PixCanvas, x: Float, y: Float, w: Float, h: Float,
        text: String, icon: String, selected: Boolean
    ) {
        val bg = if (selected) Palette.UI_ACCENT else Palette.UI_PANEL
        canvas.drawRect(x, y, x + w, y + h, bg)
        canvas.drawRect(x, y + h - h * 0.09f, x + w, y + h, Palette.UI_ACCENT)
        canvas.drawRect(x, y, x + w, y + h * 0.05f, Palette.UI_DIM)
        val isz = h * 0.55f
        drawMenuIcon(canvas, icon, x + h * 0.22f, y + (h - isz) / 2f, isz)
        val tw = canvas.textWidth(text, h * 0.34f)
        canvas.drawText(text, x + h * 0.95f + (w - h * 0.95f - tw) / 2f, y + h * 0.30f, h * 0.34f, Palette.UI_TEXT)
    }

    /** Пиксельные иконки пунктов меню. */
    private fun drawMenuIcon(c: PixCanvas, kind: String, x: Float, y: Float, s: Float) {
        when (kind) {
            "PLAY" -> { // треугольник
                for (i in 0 until 5) {
                    val w = s * (0.2f + 0.16f * i)
                    c.drawRect(x + i * s * 0.16f, y + s / 2f - w / 2f, x + i * s * 0.16f + s * 0.18f, y + s / 2f + w / 2f, Palette.UI_GREEN)
                }
            }
            "MAP" -> { // карта-стол
                c.drawRect(x, y + s * 0.15f, x + s, y + s * 0.85f, Palette.BONUS_SPEED_B)
                c.drawRect(x + s * 0.12f, y + s * 0.28f, x + s * 0.42f, y + s * 0.44f, Palette.UI_TEXT)
                c.drawRect(x + s * 0.58f, y + s * 0.28f, x + s * 0.88f, y + s * 0.44f, Palette.UI_TEXT)
                c.drawRect(x + s * 0.12f, y + s * 0.56f, x + s * 0.42f, y + s * 0.72f, Palette.UI_TEXT)
                c.drawRect(x + s * 0.58f, y + s * 0.56f, x + s * 0.88f, y + s * 0.72f, Palette.UI_TEXT)
            }
            "STAR" -> {
                c.drawRect(x + s * 0.4f, y, x + s * 0.6f, y + s, Palette.BONUS_STAR_A)
                c.drawRect(x, y + s * 0.4f, x + s, y + s * 0.6f, Palette.BONUS_STAR_A)
                c.drawRect(x + s * 0.18f, y + s * 0.18f, x + s * 0.82f, y + s * 0.82f, Palette.BONUS_STAR_B)
            }
            "GEAR" -> { // шестерёнка
                c.drawRect(x + s * 0.35f, y, x + s * 0.65f, y + s, Palette.UI_DIM)
                c.drawRect(x, y + s * 0.35f, x + s, y + s * 0.65f, Palette.UI_DIM)
                c.drawRect(x + s * 0.18f, y + s * 0.18f, x + s * 0.82f, y + s * 0.82f, Palette.UI_DIM)
                c.drawRect(x + s * 0.3f, y + s * 0.3f, x + s * 0.7f, y + s * 0.7f, Palette.UI_PANEL)
            }
            "HELP" -> {
                c.drawText("?", x, y, s * 0.9f, Palette.BONUS_FIRE_A)
            }
            "DOOR" -> {
                c.drawRect(x + s * 0.1f, y, x + s, y + s, Palette.WALL_MID)
                c.drawRect(x + s * 0.22f, y + s * 0.14f, x + s * 0.88f, y + s, Palette.WALL_DEEP)
                c.drawRect(x + s * 0.72f, y + s * 0.5f, x + s * 0.8f, y + s * 0.6f, Palette.BONUS_STAR_A)
            }
        }
    }

    private fun drawPanel(c: PixCanvas, x: Float, y: Float, w: Float, h: Float) {
        c.drawRect(x, y, x + w, y + h, Palette.UI_PANEL)
        c.drawRect(x, y, x + w, y + h * 0.045f, Palette.UI_ACCENT)
        c.drawRect(x, y + h - h * 0.045f, x + w, y + h, Palette.UI_DIM)
        c.drawRect(x, y, x + w * 0.02f, y + h, Palette.UI_DIM)
        c.drawRect(x + w - w * 0.02f, y, x + w, y + h, Palette.UI_DIM)
    }

    // ===================== Управление =====================

    private fun drawTouchControls(canvas: PixCanvas, input: TouchZones) {
        val z = input.dpad
        val cx = (z.left + z.right) / 2f
        val cy = (z.top + z.bottom) / 2f
        val r = (z.right - z.left) / 2f
        drawPixelRing(canvas, cx, cy, r, argb(120, 246, 227, 200))
        for (d in Direction.MOVING) {
            drawArrow(canvas, cx + d.dx * r * 0.62f, cy + d.dy * r * 0.62f, d, r * 0.30f, argb(150, 246, 227, 200))
        }
        if (input.direction != Direction.NONE) {
            drawArrow(
                canvas, cx + input.direction.dx * r * 0.62f, cy + input.direction.dy * r * 0.62f,
                input.direction, r * 0.36f, Palette.UI_ACCENT
            )
        }
        // Кнопка бомбы.
        val bz = input.bomb
        val bcx = (bz.left + bz.right) / 2f
        val bcy = (bz.top + bz.bottom) / 2f
        val br = (bz.right - bz.left) / 2f
        drawPixelDisc(canvas, bcx, bcy, br, argb(70, 224, 85, 122))
        drawHudIcon(canvas, "BOMB", bcx - br * 0.55f, bcy - br * 0.55f, br * 1.1f)
    }

    private fun drawArrow(c: PixCanvas, x: Float, y: Float, dir: Direction, size: Float, color: Int) {
        when (dir) {
            Direction.UP -> {
                c.drawRect(x - size * 0.3f, y - size, x + size * 0.3f, y, color)
                c.drawRect(x - size * 0.6f, y - size * 0.7f, x + size * 0.6f, y - size * 0.4f, color)
            }
            Direction.DOWN -> {
                c.drawRect(x - size * 0.3f, y, x + size * 0.3f, y + size, color)
                c.drawRect(x - size * 0.6f, y + size * 0.4f, x + size * 0.6f, y + size * 0.7f, color)
            }
            Direction.LEFT -> {
                c.drawRect(x - size, y - size * 0.3f, x, y + size * 0.3f, color)
                c.drawRect(x - size * 0.7f, y - size * 0.6f, x - size * 0.4f, y + size * 0.6f, color)
            }
            Direction.RIGHT -> {
                c.drawRect(x, y - size * 0.3f, x + size, y + size * 0.3f, color)
                c.drawRect(x + size * 0.4f, y - size * 0.6f, x + size * 0.7f, y + size * 0.6f, color)
            }
            else -> {}
        }
    }

    private fun drawPixelRing(c: PixCanvas, cx: Float, cy: Float, r: Float, color: Int) {
        val ps = max(2f, r * 0.06f)
        var a = 0.0
        while (a < Math.PI * 2) {
            val x = cx + r * Math.cos(a)
            val y = cy + r * Math.sin(a)
            c.drawRect(x.toFloat() - ps / 2, y.toFloat() - ps / 2, x.toFloat() + ps / 2, y.toFloat() + ps / 2, color)
            a += 0.18
        }
    }

    private fun drawPixelDisc(c: PixCanvas, cx: Float, cy: Float, r: Float, color: Int) {
        val rr = r * r
        var py = (cy - r).toInt()
        while (py <= cy + r) {
            val dy = py + 0.5f - cy
            val dx = kotlin.math.sqrt(maxOf(0.0, (rr - dy * dy).toDouble())).toFloat()
            c.drawRect(cx - dx, py.toFloat(), cx + dx, py + 1f, color)
            py++
        }
    }

    // ===================== Утилиты =====================

    private fun tileLeft(tx: Int): Float = offsetX + tx * tileSize
    private fun tileTop(ty: Int): Float = offsetY + ty * tileSize

    private fun blit(canvas: PixCanvas, bmp: PixCanvas, x: Float, y: Float, size: Float) {
        canvas.drawBitmap(bmp, x, y, size, size)
    }

    private fun argb(a: Int, r: Int, g: Int, b: Int): Int = (a shl 24) or (r shl 16) or (g shl 8) or b

    companion object {
        private const val HUD_HEIGHT_FRACTION = 0.10f
    }
}

/** Зоны управления (как TouchInputController, но для рендера на JVM). */
data class TouchZones(
    val dpad: RectF,
    val bomb: RectF,
    val direction: Direction = Direction.NONE
)

data class RectF(val left: Float, val top: Float, val right: Float, val bottom: Float)
