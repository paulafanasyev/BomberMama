package com.bombermama.core

import kotlin.random.Random

/**
 * Игровое поле: сетка клеток, стены, разрушаемые блоки и выход.
 * Поле генерируется детерминированно из [LevelSpec] (по фиксированному seed),
 * поэтому любой уровень всегда имеет одно и то же строение.
 */
class GameField(
    val width: Int,
    val height: Int,
    private val spec: LevelSpec
) {
    /** Сетка: tile[y][x]. */
    private val tile: Array<TileArray> = Array(height) { TileArray(width) }

    /** Клетки, на которых лежат разрушаемые блоки с бонусами. */
    private val bonusInBlock: MutableSet<Long> = mutableSetOf()

    /** Выход с уровня. */
    var exitTile: Pair<Int, Int>? = null
        private set

    val cellsTotal: Int get() = width * height

    operator fun get(x: Int, y: Int): Tile = tile[y][x]

    fun isInside(x: Int, y: Int): Boolean = x in 0 until width && y in 0 until height

    /** Клетка проходима (пол, выход или бонус). */
    fun isPassable(x: Int, y: Int): Boolean {
        if (!isInside(x, y)) return false
        return when (this[x, y]) {
            Tile.FLOOR, Tile.EXIT -> true
            Tile.WALL, Tile.BLOCK -> false
        }
    }

    fun isDestructible(x: Int, y: Int): Boolean = isInside(x, y) && this[x, y] == Tile.BLOCK

    /** Разрушить блок. Возвращает true, если в блоке был бонус. */
    fun destroyBlock(x: Int, y: Int): Boolean {
        if (!isDestructible(x, y)) return false
        tile[y][x] = Tile.FLOOR
        return bonusInBlock.remove(key(x, y))
    }

    fun setTile(x: Int, y: Int, t: Tile) {
        if (isInside(x, y)) tile[y][x] = t
    }

    /** Выход открывается после уничтожения всех врагов. */
    fun openExit() {
        val (ex, ey) = exitTile ?: return
        if (isInside(ex, ey) && this[ex, ey] != Tile.WALL) {
            tile[ey][ex] = Tile.EXIT
        }
    }

    /** Есть ли бонус в разрушаемом блоке. */
    fun hasBonusInBlock(x: Int, y: Int): Boolean = bonusInBlock.contains(key(x, y))

    private fun key(x: Int, y: Int): Long = (y.toLong() shl 32) or (x.toLong() and 0xFFFFFFFFL)

    /**
     * Генерация поля согласно схеме уровня.
     */
    fun generate(random: Random) {
        // 1. Обрамление поля неразрушаемыми стенами.
        for (x in 0 until width) {
            tile[0][x] = Tile.WALL
            tile[height - 1][x] = Tile.WALL
        }
        for (y in 0 until height) {
            tile[y][0] = Tile.WALL
            tile[y][width - 1] = Tile.WALL
        }
        // 2. Внутренние стены по схеме.
        generateWalls(random)
        // 2b. Гарантированно освобождаем стартовую зону героини: некоторые схемы
        // (шахматка, спираль, комнаты) могут поставить стену прямо на спавн,
        // и героиня оказалась бы заперта. Это обязательное условие играбельности.
        carveStartZone()
        // 3. Разрушаемые блоки на свободных клетках (с защитой стартовой зоны).
        generateBlocks(random)
        // 4. Выход прячется под случайным разрушаемым блоком подальше от старта.
        placeExit(random)
    }

    private fun generateWalls(random: Random) {
        when (spec.wallScheme) {
            WallScheme.CLASSIC_PILLARS -> {
                for (y in 2 until height - 1 step 2)
                    for (x in 2 until width - 1 step 2)
                        tile[y][x] = Tile.WALL
            }
            WallScheme.CHECKERBOARD -> {
                // Шахматная расстановка опор на нечётных координатах.
                // Гарантированно проходима: все чётные столбцы и строки
                // остаются свободными коридорами, соединяющими всё поле.
                // (Сплошная шахматная доска x+y%2==0 непроходима:
                //  каждая клетка поля отрезана стенами от соседей.)
                for (y in 1 until height - 1 step 2) {
                    for (x in 1 until width - 1 step 2) {
                        tile[y][x] = Tile.WALL
                    }
                }
            }
            WallScheme.CORRIDORS_H -> {
                for (y in 3 until height - 1 step 4) {
                    var gap = random.nextInt(1, width - 2)
                    for (x in 1 until width - 1) {
                        if (x == gap || x == gap + 1) continue
                        tile[y][x] = Tile.WALL
                    }
                }
            }
            WallScheme.CORRIDORS_V -> {
                for (x in 3 until width - 1 step 4) {
                    var gap = random.nextInt(1, height - 2)
                    for (y in 1 until height - 1) {
                        if (y == gap || y == gap + 1) continue
                        tile[y][x] = Tile.WALL
                    }
                }
            }
            WallScheme.ROOMS -> {
                // Стены комнат 4x4 с дверными проёмами.
                for (ry in 2 until height - 2 step 5) {
                    for (rx in 2 until width - 2 step 5) {
                        val doorY = random.nextInt(0, 3)
                        for (i in 0..3) {
                            if (rx + i in 1 until width - 1) tile[ry][rx + i] = Tile.WALL
                            if (ry + i in 1 until height - 1) tile[ry + i][rx] = Tile.WALL
                        }
                        // Проём в нижней стене комнаты.
                        if (rx + doorY + 1 in 1 until width - 1) tile[ry][rx + doorY + 1] = Tile.FLOOR
                        if (ry + 3 in 1 until height - 1) {
                            tile[ry + 3][rx + 1] = Tile.FLOOR
                            tile[ry + 3][rx + 2] = Tile.FLOOR
                        }
                    }
                }
            }
            WallScheme.CROSS -> {
                val cx = width / 2
                val cy = height / 2
                for (i in -2..2) {
                    if (cx + i in 1 until width - 1) tile[cy][cx + i] = Tile.WALL
                    if (cy + i in 1 until height - 1) tile[cy + i][cx] = Tile.WALL
                }
                // Диагональные опоры.
                for (d in 3 until minOf(width, height) / 2 step 3) {
                    if (cx + d < width - 1 && cy + d < height - 1) tile[cy + d][cx + d] = Tile.WALL
                    if (cx - d > 0 && cy - d > 0) tile[cy - d][cx - d] = Tile.WALL
                    if (cx + d < width - 1 && cy - d > 0) tile[cy - d][cx + d] = Tile.WALL
                    if (cx - d > 0 && cy + d < height - 1) tile[cy + d][cx - d] = Tile.WALL
                }
            }
            WallScheme.MAZE_L -> {
                // Г-образные стены, чередующие ориентацию.
                var y = 2
                var flip = false
                while (y < height - 2) {
                    var x = if (flip) 2 else 4
                    while (x < width - 3) {
                        val lenH = 3
                        for (i in 0 until lenH) if (x + i < width - 1) tile[y][x + i] = Tile.WALL
                        for (j in 0..2) if (y + j < height - 1) tile[y + j][x] = Tile.WALL
                        x += 6
                    }
                    y += 4
                    flip = !flip
                }
            }
            WallScheme.OPEN -> {
                // Одиночные опоры в шахматном порядке с большими промежутками.
                for (y in 3 until height - 1 step 4)
                    for (x in 3 until width - 1 step 4)
                        tile[y][x] = Tile.WALL
            }
            WallScheme.SPIRAL -> {
                var x1 = 2; var y1 = 2; var x2 = width - 3; var y2 = height - 3
                var layer = 0
                while (x2 - x1 > 2 && y2 - y1 > 2) {
                    for (x in x1..x2) tile[y1][x] = Tile.WALL
                    for (y in y1..y2) tile[y][x2] = Tile.WALL
                    for (x in x1..x2) tile[y2][x] = Tile.WALL
                    for (y in y1..y2) tile[y][x1] = Tile.WALL
                    // Проход в спираль.
                    tile[y1][x1 + 1] = Tile.FLOOR
                    tile[y2 - 1][x2] = Tile.FLOOR
                    x1 += 3; y1 += 3; x2 -= 3; y2 -= 3
                    layer++
                    if (layer > 6) break
                }
            }
            WallScheme.DOUBLE_CROSS -> {
                generateWallsShape(random)
            }
        }
    }

    private fun generateWallsShape(@Suppress("UNUSED_PARAMETER") random: Random) {
        val cy = height / 2
        for (i in -3..3) {
            if (cy + i in 1 until height - 1) {
                tile[cy + i][width / 3] = Tile.WALL
                tile[cy + i][(2 * width) / 3] = Tile.WALL
            }
        }
        val cx = width / 2
        for (i in -3..3) {
            if (cx + i in 1 until width - 1) {
                tile[height / 3][cx + i] = Tile.WALL
                tile[(2 * height) / 3][cx + i] = Tile.WALL
            }
        }
        // Проёмы в крестах.
        tile[cy][width / 3 + 1] = Tile.FLOOR
        tile[cy][width / 3 - 1] = Tile.FLOOR
        tile[cy][(2 * width) / 3 + 1] = Tile.FLOOR
        tile[cy][(2 * width) / 3 - 1] = Tile.FLOOR
        tile[height / 3 + 1][cx] = Tile.FLOOR
        tile[height / 3 - 1][cx] = Tile.FLOOR
        tile[(2 * height) / 3 + 1][cx] = Tile.FLOOR
        tile[(2 * height) / 3 - 1][cx] = Tile.FLOOR
    }

    /**
     * Расстановка разрушаемых блоков на свободных клетках.
     * Стартовая зона героини (3x3 в углу) всегда свободна.
     */
    private fun generateBlocks(random: Random) {
        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                if (tile[y][x] != Tile.FLOOR) continue
                if (isStartZone(x, y)) continue
                if (random.nextFloat() < spec.blockDensity) {
                    tile[y][x] = Tile.BLOCK
                    // С некоторой вероятностью в блоке прячется бонус.
                    if (random.nextFloat() < BONUS_CHANCE) {
                        bonusInBlock.add(key(x, y))
                    }
                }
            }
        }
    }

    /** Зона старта героини: угловая область 3x3 (1,1) должна быть свободна. */
    private fun isStartZone(x: Int, y: Int): Boolean = x <= 2 && y <= 2

    /**
     * Принудительно освобождает стартовую зону 2x2 от стен.
     * Вызывается после генерации стен, чтобы героиня всегда могла
     * двигаться и ставить бомбы в начале уровня.
     */
    private fun carveStartZone() {
        for (y in 1..2) {
            for (x in 1..2) {
                tile[y][x] = Tile.FLOOR
            }
        }
    }

    /**
     * Выход прячется под разрушаемым блоком как можно дальше от старта.
     * Если подходящего блока нет — выход ставится на свободную клетку.
     */
    private fun placeExit(random: Random) {
        var best: Pair<Int, Int>? = null
        var bestDist = -1
        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                if (this[x, y] == Tile.BLOCK) {
                    val d = x + y // расстояние от старта (1,1)
                    if (d > bestDist) {
                        bestDist = d
                        best = x to y
                    }
                }
            }
        }
        val exit = best ?: run {
            // Нет блоков — ставим выход на пол в дальнем углу.
            var ex = width - 2; var ey = height - 2
            while (ex > 1 && this[ex, ey] != Tile.FLOOR) { ex-- }
            ex to ey
        }
        exitTile = exit
    }

    companion object {
        /** Вероятность того, что в разрушаемом блоке спрятан бонус. */
        const val BONUS_CHANCE = 0.30f

        fun create(spec: LevelSpec, seed: Long): GameField {
            val field = GameField(spec.width, spec.height, spec)
            field.generate(Random(seed))
            return field
        }
    }
}

/**
 * Вспомогательный массив клеток строки (избегает Array<Tile> поверхностного копирования).
 */
private class TileArray(val size: Int) {
    private val arr = ShortArray(size)
    operator fun get(i: Int): Tile = if (arr[i].toInt() == 0) Tile.FLOOR else Tile.values()[arr[i].toInt() - 1]
    operator fun set(i: Int, t: Tile) { arr[i] = (t.ordinal + 1).toShort() }
}
