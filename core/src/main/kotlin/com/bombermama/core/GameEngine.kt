package com.bombermama.core

/**
 * Состояние игры (конечный автомат).
 */
enum class GameState {
    /** Уровень идёт. */
    PLAYING,
    /** Пауза: таймеры, враги и бомбы заморожены. */
    PAUSED,
    /** Уровень пройден. */
    WIN,
    /** Игрок потерял все жизни. */
    GAME_OVER
}

/**
 * Результат игровой сессии для UI и сохранений.
 */
data class SessionResult(
    val level: Int,
    val score: Int,
    val won: Boolean
)

/**
 * Игровой движок — "мозг" игры. Чистый Kotlin, без зависимостей от Android,
 * поэтому его логику можно полноценно тестировать на JVM.
 *
 * Отвечает за:
 *  - перемещение героини и коллизии со стенами/блоками/бомбами;
 *  - установку бомб, таймеры, взрывы крестом;
 *  - цепные реакции (взрыв → бомба → взрыв) без двойного урона;
 *  - разрушение блоков и спавн бонусов;
 *  - врагов, их движение, коллизии и смерть;
 *  - урон игроку, жизни, неуязвимость;
 *  - условие победы (все враги убиты + выход) и поражения;
 *  - очки и время.
 *
 * Обновление происходит из одного потока (game loop); UI читает снапшоты.
 */
class GameEngine(
    val spec: LevelSpec,
    seed: Long = spec.seed,
    private val onWin: (SessionResult) -> Unit = {},
    private val onGameOver: (SessionResult) -> Unit = {}
) {
    val field: GameField = GameField.create(spec, seed)
    private val random = kotlin.random.Random(seed xor 0x5DEECE66DL)

    val player: Player = Player(START_X, START_Y)
    val enemies: MutableList<Enemy> = mutableListOf()
    val bombs: MutableList<Bomb> = mutableListOf()
    val explosions: MutableList<ExplosionSegment> = mutableListOf()
    val bonuses: MutableList<BonusPickup> = mutableListOf()

    /** Активный направленный ввод (устанавливается контроллером ввода). */
    @Volatile
    var inputDirection: Direction = Direction.NONE

    /** Запрос на установку бомбы (фронт-контроллер). */
    @Volatile
    var bombRequested: Boolean = false

    var state: GameState = GameState.PLAYING
        private set

    /** Оставшееся время уровня (сек). <0 = без лимита. */
    var timeLeft: Float = if (spec.timeLimit > 0) spec.timeLimit.toFloat() else -1f

    /** Игровое время для анимаций. */
    var time: Float = 0f
        private set

    /** Все враги уничтожены — выход открыт. */
    var enemiesCleared: Boolean = false
        private set

    var lastDt: Float = 0f
        private set

    init {
        spawnEnemies()
        // Уровень без врагов: выход открыт сразу.
        if (spec.totalEnemies == 0) {
            enemiesCleared = true
            field.openExit()
        }
    }

    // =========================================================================
    // Главный цикл
    // =========================================================================

    /**
     * Один шаг симуляции с фиксированным шагом (например, 1/60 c).
     * На паузе ничего не просчитывается — это и есть "реальная" пауза:
     * таймеры, враги и бомбы заморожены.
     */
    fun update(dt: Float) {
        lastDt = dt
        if (state != GameState.PLAYING) return
        time += dt

        tickTimer(dt)
        updatePlayer(dt)
        updateBombs(dt)
        updateExplosions(dt)
        updateEnemies(dt)
        updateBonuses(dt)
        checkWinCondition()
    }

    fun pause() { if (state == GameState.PLAYING) state = GameState.PAUSED }
    fun resume() { if (state == GameState.PAUSED) state = GameState.PLAYING }

    private fun tickTimer(dt: Float) {
        if (timeLeft >= 0) {
            timeLeft -= dt
            if (timeLeft <= 0) {
                timeLeft = 0f
                // Время вышло — героиня теряет жизнь и получает дополнительное время.
                damagePlayer()
                if (state == GameState.PLAYING) timeLeft = TIME_OUT_GRACE
            }
        }
    }

    // =========================================================================
    // Коллизии
    // =========================================================================

    /**
     * Блокирует ли клетка (tx,ty) данную сущность.
     *
     * Стены и блоки непроходимы. Бомбы тоже непроходимы, но сущность, центр
     * которой находится в клетке бомбы, может с неё сойти (иначе героиня
     * застряла бы на только что установленной ею бомбе).
     */
    private fun isTileBlockedFor(tx: Int, ty: Int, entity: GridEntity): Boolean {
        if (!field.isPassable(tx, ty)) return true
        for (bomb in bombs) {
            if (!bomb.isActive) continue
            if (bomb.tileX == tx && bomb.tileY == ty) {
                // Центр сущности в клетке бомбы — разрешаем выйти.
                if (entity.tileX != tx || entity.tileY != ty) return true
            }
        }
        return false
    }

    /**
     * Проверка столкновений по ведущей границе хитбокса.
     *
     * Используется модель "ведущего ребра": при движении по оси проверяется
     * только клетка, в которую входит передняя грань хитбокса. Хитбокс меньше
     * клетки, поэтому невозможно "врасти" в стену, а бомбу можно покинуть.
     *
     * @param leadX координата передней грани по X
     * @param y     координата центра по Y
     */
    private fun collidesAtX(leadX: Float, y: Float, entity: GridEntity, hh: Float): Boolean {
        val tx = leadX.toInt()
        val minY = (y - hh).toInt()
        val maxY = (y + hh).toInt()
        for (ty in minY..maxY) {
            if (isTileBlockedFor(tx, ty, entity)) return true
        }
        return false
    }

    private fun collidesAtY(x: Float, leadY: Float, entity: GridEntity, hw: Float): Boolean {
        val ty = leadY.toInt()
        val minX = (x - hw).toInt()
        val maxX = (x + hw).toInt()
        for (tx in minX..maxX) {
            if (isTileBlockedFor(tx, ty, entity)) return true
        }
        return false
    }

    /**
     * Покадровое перемещение с раздельной обработкой осей и "скольжением" вдоль стен.
     * Попутно применяется магнитное выравнивание к центру полосы — классическое
     * удобство управления, позволяющее заходить в узкие коридоры.
     *
     * @return фактически пройденное расстояние
     */
    private fun moveEntity(entity: GridEntity, dir: Direction, distance: Float, hw: Float, hh: Float): Float {
        if (distance <= 0f || dir == Direction.NONE) return 0f

        return when (dir) {
            Direction.LEFT, Direction.RIGHT -> {
                val sign = if (dir == Direction.RIGHT) 1f else -1f
                val lead = if (sign > 0) entity.x + hw else entity.x - hw
                if (!collidesAtX(lead + sign * distance, entity.y, entity, hh)) {
                    entity.x += sign * distance
                    // Магнитное выравнивание по поперечной оси для прохода в коридоры.
                    entity.y = applyRailSnap(entity.y, entity.x, hw, hh, entity, snapAxisY = true)
                    distance
                } else {
                    0f
                }
            }
            Direction.UP, Direction.DOWN -> {
                val sign = if (dir == Direction.DOWN) 1f else -1f
                val lead = if (sign > 0) entity.y + hh else entity.y - hh
                if (!collidesAtY(entity.x, lead + sign * distance, entity, hw)) {
                    entity.y += sign * distance
                    entity.x = applyRailSnap(entity.x, entity.y, hw, hh, entity, snapAxisY = false)
                    distance
                } else {
                    0f
                }
            }
            Direction.NONE -> 0f
        }
    }

    /**
     * Подтягивание к центру полосы (0.5 от клетки), если сущность уже близко к нему
     * и сдвиг не приведёт к столкновению. Делает управление прощающим.
     */
    private fun applyRailSnap(coord: Float, other: Float, hw: Float, hh: Float, entity: GridEntity, snapAxisY: Boolean): Float {
        val frac = coord - coord.toInt()
        val delta = 0.5f - frac
        if (kotlin.math.abs(delta) > RAIL_SNAP_RANGE) return coord
        val maxStep = RAIL_SNAP_SPEED * lastDt
        val step = delta.coerceIn(-maxStep, maxStep)
        val nc = coord + step
        val collides = if (snapAxisY) {
            collidesAtX(other + hw, nc, entity, hh) || collidesAtX(other - hw, nc, entity, hh)
        } else {
            collidesAtY(nc, entity.y + hh, entity, hw) || collidesAtY(nc, entity.y - hh, entity, hw)
        }
        return if (collides) coord else nc
    }

    // =========================================================================
    // Игрок
    // =========================================================================

    private fun updatePlayer(dt: Float) {
        player.tickInvulnerable(dt)
        if (player.dead) return

        if (inputDirection != Direction.NONE) {
            player.direction = inputDirection
            moveEntity(player, inputDirection, player.speed * dt, PLAYER_HW, PLAYER_HH)
        } else {
            player.direction = Direction.NONE
        }

        if (bombRequested) {
            bombRequested = false
            placeBomb()
        }

        // Подбор бонусов.
        for (bonus in bonuses.toList()) {
            if (!bonus.picked && bonus.tileX == player.tileX && bonus.tileY == player.tileY) {
                bonus.pick()
                player.applyBonus(bonus.type, Player.MAX_LIVES)
            }
        }

        // Столкновение с врагами.
        if (player.invulnerableTime <= 0f) {
            for (enemy in enemies) {
                if (enemy.dead) continue
                if (isOverlapping(player, enemy)) {
                    damagePlayer()
                    break
                }
            }
        }
    }

    private fun isOverlapping(a: GridEntity, b: GridEntity): Boolean {
        val dx = a.x - b.x
        val dy = a.y - b.y
        return dx * dx + dy * dy < OVERLAP_DISTANCE_SQ
    }

    // =========================================================================
    // Бомбы
    // =========================================================================

    val activeBombCount: Int get() = bombs.count { it.isActive }

    private fun placeBomb() {
        if (activeBombCount >= player.maxBombs) return
        val tx = player.tileX
        val ty = player.tileY
        if (!field.isPassable(tx, ty)) return
        if (field[tx, ty] == Tile.EXIT) return
        if (bombs.any { it.tileX == tx && it.tileY == ty && it.isActive }) return
        bombs.add(Bomb(tx, ty, player.fireRange))
    }

    private fun updateBombs(dt: Float) {
        for (bomb in bombs.toList()) {
            if (!bomb.isActive) continue
            bomb.tick(dt)
            if (bomb.fuse <= 0f) detonate(bomb)
        }
        bombs.removeAll { it.exploded }
    }

    /**
     * Детонация бомбы: создаёт крест взрыва по четырём направлениям.
     * Если взрыв задевает другую бомбу — она взрывается немедленно (цепная реакция).
     *
     * Используется обход в ширину: каждая бомба детонирует ровно один раз,
     * поэтому цепочка A → B → C гарантированно завершается,
     * а клетки взрыва не наносят двойной урон.
     */
    private fun detonate(bomb: Bomb) {
        val detonated = HashSet<Bomb>()
        val queue: ArrayDeque<Bomb> = ArrayDeque()
        queue.add(bomb)
        detonated.add(bomb)

        // Снимок списка бомб для поиска цепочек: если игрок пострадает прямо
        // в середине детонации и активные бомбы будут сняты, поиск по снимку
        // всё равно корректно достроит цепочку A → B → C.
        val chainLookup = bombs.toList()

        // Клетки, уже охваченные взрывом — защита от двойного урона.
        val explodedCells = HashSet<Long>()

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            current.explode()

            explodeCell(current.tileX, current.tileY, true, Direction.NONE, true, explodedCells)

            for (dir in Direction.MOVING) {
                for (range in 1..current.fireRange) {
                    val cx = current.tileX + dir.dx * range
                    val cy = current.tileY + dir.dy * range
                    if (!field.isInside(cx, cy)) break

                    val tile = field[cx, cy]
                    if (tile == Tile.WALL) break // взрыв не проходит сквозь стену.

                    val isTip = (range == current.fireRange) || (tile == Tile.BLOCK)
                    explodeCell(cx, cy, false, dir, isTip, explodedCells)

                    if (tile == Tile.BLOCK) {
                        val hadBonus = field.destroyBlock(cx, cy)
                        if (hadBonus) spawnBonus(cx, cy)
                        break // взрыв заканчивается на разрушенном блоке.
                    }

                    // Цепная реакция: взрыв задевает другую бомбу.
                    val hitBomb = chainLookup.firstOrNull { it.tileX == cx && it.tileY == cy && it.isActive }
                    if (hitBomb != null && !detonated.contains(hitBomb)) {
                        detonated.add(hitBomb)
                        queue.add(hitBomb)
                    }
                }
            }
        }
    }

    /**
     * Обработка одной клетки взрыва: урон игроку, уничтожение врагов,
     * создание визуального сегмента взрыва (ровно один раз на клетку).
     */
    private fun explodeCell(
        tx: Int, ty: Int, isCenter: Boolean, dir: Direction, isTip: Boolean,
        explodedCells: HashSet<Long>
    ) {
        val key = (ty.toLong() shl 32) or (tx.toLong() and 0xFFFFFFFFL)
        if (!explodedCells.add(key)) return // клетка уже охвачена — без двойного урона.

        explosions.add(ExplosionSegment(tx, ty, isCenter, dir, isTip))

        // Урон игроку.
        if (!player.dead && player.tileX == tx && player.tileY == ty) {
            damagePlayer()
        }

        // Уничтожение врагов.
        for (enemy in enemies) {
            if (enemy.dead) continue
            if (enemy.tileX == tx && enemy.tileY == ty) {
                enemy.kill()
                player.addScore(enemy.type.score)
            }
        }
    }

    private fun spawnBonus(tx: Int, ty: Int) {
        val type = BonusType.REGULAR[random.nextInt(BonusType.REGULAR.size)]
        bonuses.add(BonusPickup(tx, ty, type))
    }

    private fun updateExplosions(dt: Float) {
        val it = explosions.iterator()
        while (it.hasNext()) {
            if (it.next().tick(dt)) it.remove()
        }
    }

    // =========================================================================
    // Урон и жизни
    // =========================================================================

    private fun damagePlayer() {
        if (player.invulnerableTime > 0f || player.dead) return
        player.lives--
        player.addInvulnerable(INVULNERABLE_TIME)
        // Снимаем только ещё не взорвавшиеся бомбы — после возрождения героиня
        // не должна погибнуть мгновенно от собственной бомбы.
        // Сегменты взрыва остаются: они короткоживущие и dangerous-фаза учтена
        // неуязвимостью, а кадр взрыва не должен пропадать с экрана.
        bombs.removeAll { !it.exploded }
        if (player.lives <= 0) {
            player.lives = 0
            player.setDead()
            state = GameState.GAME_OVER
            onGameOver(SessionResult(spec.number, player.score, false))
        } else {
            // Возвращаем героиню на стартовую клетку.
            player.x = START_X
            player.y = START_Y
            player.direction = Direction.NONE
        }
    }

    // =========================================================================
    // Враги
    // =========================================================================

    private fun spawnEnemies() {
        val spawnTiles = mutableListOf<Pair<Int, Int>>()
        for (y in 1 until field.height - 1) {
            for (x in 1 until field.width - 1) {
                if (field[x, y] != Tile.FLOOR) continue
                if (x <= SAFE_ZONE && y <= SAFE_ZONE) continue // безопасная зона старта
                spawnTiles.add(x to y)
            }
        }
        spawnTiles.shuffle(random)

        var idx = 0
        for ((type, count) in spec.enemies) {
            repeat(count) {
                if (idx >= spawnTiles.size) return@repeat
                val (tx, ty) = spawnTiles[idx++]
                enemies.add(Enemy(tx.toFloat(), ty.toFloat(), type, random))
            }
        }
    }

    private fun updateEnemies(dt: Float) {
        for (enemy in enemies) {
            if (enemy.dead) continue
            enemy.tickAnimation(dt)

            // Враги двигаются строго по сетке: достигли центра клетки — выбираем направление.
            if (enemy.atTileCenter()) {
                enemy.chooseDirection(reselect(enemy), player.x, player.y)
            }

            if (enemy.direction != Direction.NONE) {
                // Плавное движение к центру следующей клетки.
                moveEnemyToNextTile(enemy, dt)
            }

            // Столкновение врага с игроком.
            if (player.invulnerableTime <= 0f && !player.dead && isOverlapping(enemy, player)) {
                damagePlayer()
            }
        }

        // Удаление убитых врагов после короткой анимации смерти.
        enemies.removeAll { it.dead && it.deathTime > ENEMY_DEATH_ANIM }

        // Если всех врагов уничтожили — открываем выход.
        if (!enemiesCleared && enemies.all { it.dead }) {
            enemiesCleared = true
            field.openExit()
        }
    }

    private fun moveEnemyToNextTile(enemy: Enemy, dt: Float) {
        val dir = enemy.direction
        if (dir == Direction.NONE) return
        // Целевой центр следующей клетки по направлению движения.
        if (dir.dx != 0) {
            val nextTx = enemy.tileX + dir.dx
            if (isTileBlockedFor(nextTx, enemy.tileY, enemy)) {
                enemy.chooseDirection(reselect(enemy), player.x, player.y)
                return
            }
            val targetX = if (dir.dx > 0) enemy.tileX + 1.5f else enemy.tileX - 0.5f
            val speed = enemy.type.maxSpeed * dt
            enemy.x = if (dir.dx > 0) minOf(enemy.x + speed, targetX) else maxOf(enemy.x - speed, targetX)
            // Держимся центра полосы, чтобы не цепляться за углы.
            enemy.y = enemy.tileY + 0.5f
        } else {
            val nextTy = enemy.tileY + dir.dy
            if (isTileBlockedFor(enemy.tileX, nextTy, enemy)) {
                enemy.chooseDirection(reselect(enemy), player.x, player.y)
                return
            }
            val targetY = if (dir.dy > 0) enemy.tileY + 1.5f else enemy.tileY - 0.5f
            val speed = enemy.type.maxSpeed * dt
            enemy.y = if (dir.dy > 0) minOf(enemy.y + speed, targetY) else maxOf(enemy.y - speed, targetY)
            enemy.x = enemy.tileX + 0.5f
        }
    }

    private fun reselect(enemy: Enemy): (Int, Int) -> Boolean =
        { tx, ty -> !isTileBlockedFor(tx, ty, enemy) }

    private fun GridEntity.atTileCenter(): Boolean {
        val fx = x - x.toInt()
        val fy = y - y.toInt()
        return fx in 0.45f..0.55f && fy in 0.45f..0.55f
    }

    private fun updateBonuses(dt: Float) {
        bonuses.removeAll { it.picked }
        for (b in bonuses) b.tick(dt)
    }

    // =========================================================================
    // Победа
    // =========================================================================

    private fun checkWinCondition() {
        if (state != GameState.PLAYING) return
        if (!enemiesCleared) return
        val (ex, ey) = field.exitTile ?: return
        if (field[ex, ey] != Tile.EXIT) return
        if (player.tileX == ex && player.tileY == ey) {
            state = GameState.WIN
            player.setWin()
            if (timeLeft > 0) player.addScore((timeLeft * 2).toInt())
            onWin(SessionResult(spec.number, player.score, true))
        }
    }

    companion object {
        // Стартовая позиция — центр клетки (1,1).
        const val START_X = 1.5f
        const val START_Y = 1.5f
        const val INVULNERABLE_TIME = 1.8f
        const val SAFE_ZONE = 3
        const val TIME_OUT_GRACE = 30f
        private const val OVERLAP_DISTANCE_SQ = 0.30f
        private const val PLAYER_HW = 0.30f
        private const val PLAYER_HH = 0.28f
        private const val ENEMY_DEATH_ANIM = 0.45f
        private const val RAIL_SNAP_RANGE = 0.40f
        private const val RAIL_SNAP_SPEED = 6.0f
    }
}
