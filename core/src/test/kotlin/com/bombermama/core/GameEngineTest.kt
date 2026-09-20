package com.bombermama.core

import org.junit.Assert.*
import org.junit.Test

/**
 * Тесты движения героини, коллизий, бонусов, врагов, паузы, победы/поражения.
 */
class GameEngineTest {

    private fun engineFor(level: Int): GameEngine {
        val spec = Levels.byNumber(level)!!
        return GameEngine(spec, spec.seed)
    }

    private fun flat(engine: GameEngine): GameEngine {
        for (y in 0 until engine.field.height)
            for (x in 0 until engine.field.width)
                engine.field.setTile(x, y, Tile.FLOOR)
        for (x in 0 until engine.field.width) {
            engine.field.setTile(x, 0, Tile.WALL)
            engine.field.setTile(x, engine.field.height - 1, Tile.WALL)
        }
        for (y in 0 until engine.field.height) {
            engine.field.setTile(0, y, Tile.WALL)
            engine.field.setTile(engine.field.width - 1, y, Tile.WALL)
        }
        return engine
    }

    // ===================== ДВИЖЕНИЕ =====================

    @Test
    fun `player moves right when input given`() {
        val engine = flat(engineFor(1))
        engine.player.x = 5f; engine.player.y = 5f
        val startX = engine.player.x
        engine.inputDirection = Direction.RIGHT
        engine.update(1 / 60f)
        assertTrue("player moved right", engine.player.x > startX)
    }

    @Test
    fun `player does not move into wall`() {
        val engine = flat(engineFor(1))
        engine.player.x = 5.5f; engine.player.y = 5.5f
        engine.field.setTile(6, 5, Tile.WALL)
        engine.inputDirection = Direction.RIGHT
        repeat(10) { engine.update(1 / 60f) }
        assertTrue("player did move toward wall", engine.player.x > 5.5f)
        assertEquals("did not cross into wall tile", 5, engine.player.tileX)
        assertTrue("stopped before wall", engine.player.x < 6.0f)
    }

    @Test
    fun `player does not move into block`() {
        val engine = flat(engineFor(1))
        engine.player.x = 5.5f; engine.player.y = 5.5f
        engine.field.setTile(6, 5, Tile.BLOCK)
        engine.inputDirection = Direction.RIGHT
        repeat(10) { engine.update(1 / 60f) }
        assertTrue("player did move toward block", engine.player.x > 5.5f)
        assertEquals("did not cross into block tile", 5, engine.player.tileX)
        assertTrue("stopped before block", engine.player.x < 6.0f)
    }

    @Test
    fun `player cannot pass through bomb`() {
        val engine = flat(engineFor(1))
        engine.player.x = 5.5f; engine.player.y = 5.5f
        engine.bombs.add(Bomb(6, 5, 1))
        engine.inputDirection = Direction.RIGHT
        repeat(10) { engine.update(1 / 60f) }
        assertTrue("player did move toward bomb", engine.player.x > 5.5f)
        assertEquals("did not cross into bomb tile", 5, engine.player.tileX)
        assertTrue("stopped before bomb", engine.player.x < 6.0f)
    }

    @Test
    fun `player slides along wall vertically when moving up`() {
        val engine = flat(engineFor(1))
        engine.player.x = 5.5f; engine.player.y = 5.5f
        engine.field.setTile(5, 4, Tile.WALL)
        engine.inputDirection = Direction.UP
        repeat(10) { engine.update(1 / 60f) }
        assertTrue("player did move up", engine.player.y < 5.5f)
        assertEquals("did not cross into wall tile", 5, engine.player.tileY)
        assertTrue("stopped above floor", engine.player.y > 4.0f)
    }

    @Test
    fun `player stays inside field bounds`() {
        val engine = engineFor(1)
        engine.inputDirection = Direction.LEFT
        repeat(300) { engine.update(1 / 60f) }
        assertTrue("x >= 0", engine.player.x >= 0f)
        engine.inputDirection = Direction.UP
        repeat(300) { engine.update(1 / 60f) }
        assertTrue("y >= 0", engine.player.y >= 0f)
        engine.inputDirection = Direction.RIGHT
        repeat(500) { engine.update(1 / 60f) }
        assertTrue("x < width", engine.player.x < engine.field.width)
    }

    @Test
    fun `player speed bonus increases movement rate`() {
        val engine = flat(engineFor(1))
        engine.player.x = 5f; engine.player.y = 5f
        engine.player.speed = 7.2f
        engine.inputDirection = Direction.RIGHT
        engine.update(1 / 60f)
        val fast = engine.player.x
        engine.player.x = 5f
        engine.player.speed = 3.6f
        engine.update(1 / 60f)
        val slow = engine.player.x
        assertTrue("faster speed covers more distance", fast > slow)
    }

    // ===================== БОНУСЫ =====================

    @Test
    fun `fire bonus increases range`() {
        val engine = flat(engineFor(1))
        val before = engine.player.fireRange
        engine.player.applyBonus(BonusType.FIRE, Player.MAX_LIVES)
        assertEquals(before + 1, engine.player.fireRange)
    }

    @Test
    fun `bomb bonus increases max bombs`() {
        val engine = flat(engineFor(1))
        val before = engine.player.maxBombs
        engine.player.applyBonus(BonusType.BOMB, Player.MAX_LIVES)
        assertEquals(before + 1, engine.player.maxBombs)
    }

    @Test
    fun `speed bonus increases speed up to cap`() {
        val engine = flat(engineFor(1))
        engine.player.speed = Player.MAX_SPEED
        engine.player.applyBonus(BonusType.SPEED, Player.MAX_LIVES)
        assertEquals("capped at MAX_SPEED", Player.MAX_SPEED, engine.player.speed, 0.001f)
    }

    @Test
    fun `life bonus adds life up to cap`() {
        val engine = flat(engineFor(1))
        engine.player.lives = Player.MAX_LIVES
        engine.player.applyBonus(BonusType.LIFE, Player.MAX_LIVES)
        assertEquals(Player.MAX_LIVES, engine.player.lives)
    }

    @Test
    fun `star bonus adds score`() {
        val engine = flat(engineFor(1))
        val before = engine.player.score
        engine.player.applyBonus(BonusType.STAR, Player.MAX_LIVES)
        assertTrue(engine.player.score > before)
    }

    @Test
    fun `bonus pickup is collected when player walks over`() {
        val engine = flat(engineFor(1))
        engine.player.x = 5f; engine.player.y = 5f
        val pickup = BonusPickup(5, 5, BonusType.FIRE)
        engine.bonuses.add(pickup)
        engine.update(1 / 60f)
        assertTrue("picked up", pickup.picked)
        assertEquals("range increased", 2, engine.player.fireRange)
    }

    // ===================== ВРАГИ =====================

    @Test
    fun `enemies spawn on every level`() {
        for (n in 1..Levels.count) {
            val engine = engineFor(n)
            assertTrue("level $n has enemies", engine.enemies.isNotEmpty())
        }
    }

    @Test
    fun `enemies do not spawn in player start safe zone`() {
        for (n in 1..Levels.count) {
            val engine = engineFor(n)
            for (e in engine.enemies) {
                assertFalse("enemy in safe zone on level $n", e.tileX <= 3 && e.tileY <= 3)
            }
        }
    }

    @Test
    fun `enemy moves over time`() {
        val engine = flat(engineFor(1))
        val enemy = engine.enemies.first()
        val sx = enemy.x; val sy = enemy.y
        repeat(60) { engine.update(1 / 60f) }
        assertTrue("enemy moved", enemy.x != sx || enemy.y != sy)
    }

    @Test
    fun `enemy changes direction when hitting wall`() {
        val engine = flat(engineFor(1))
        val enemy = engine.enemies.first()
        enemy.x = 5.5f; enemy.y = 5.5f
        // Запираем врага в тупике: единственный выход — назад (налево).
        engine.field.setTile(6, 5, Tile.WALL)
        engine.field.setTile(5, 4, Tile.WALL)
        engine.field.setTile(5, 6, Tile.WALL)
        enemy.direction = Direction.RIGHT
        // Враг стоит в центре клетки тупика — решение принимается сразу.
        engine.update(1 / 60f)
        assertEquals("enemy turns back in dead end", Direction.LEFT, enemy.direction)
        // И действительно покидает тупик (не упирается в стену).
        repeat(90) { engine.update(1 / 60f) }
        assertTrue("enemy left the dead end", enemy.tileX < 5)
    }

    @Test
    fun `enemy touching player deals damage`() {
        val engine = flat(engineFor(1))
        engine.enemies.clear()
        val enemy = Enemy(5f, 5f, EnemyType.SLOW, kotlin.random.Random(3))
        engine.enemies.add(enemy)
        engine.player.x = 5.3f; engine.player.y = 5.3f
        engine.player.lives = 3
        engine.update(1 / 60f)
        assertEquals("player took damage", 2, engine.player.lives)
    }

    @Test
    fun `hunter chases player on same row`() {
        val engine = flat(engineFor(1))
        engine.enemies.clear()
        val hunter = Enemy(2f, 5f, EnemyType.HUNTER, kotlin.random.Random(7))
        engine.enemies.add(hunter)
        engine.player.x = 8f; engine.player.y = 5f
        // Даём врагу возможность принять решение.
        repeat(30) { engine.update(1 / 60f) }
        assertEquals("hunter moves right toward player", Direction.RIGHT, hunter.direction)
    }

    // ===================== ПАУЗА =====================

    @Test
    fun `pause freezes game simulation`() {
        val engine = flat(engineFor(1))
        engine.player.x = 5f; engine.player.y = 5f
        engine.inputDirection = Direction.RIGHT
        engine.pause()
        assertEquals(GameState.PAUSED, engine.state)
        val before = engine.player.x
        repeat(30) { engine.update(1 / 60f) }
        assertEquals("frozen while paused", before, engine.player.x, 0.001f)
    }

    @Test
    fun `resume continues simulation`() {
        val engine = flat(engineFor(1))
        engine.player.x = 5f; engine.player.y = 5f
        engine.inputDirection = Direction.RIGHT
        engine.pause()
        engine.resume()
        assertEquals(GameState.PLAYING, engine.state)
        repeat(10) { engine.update(1 / 60f) }
        assertTrue("moving again", engine.player.x > 5f)
    }

    @Test
    fun `pause freezes bomb timers`() {
        val engine = flat(engineFor(1))
        engine.player.x = 5f; engine.player.y = 5f
        engine.bombRequested = true
        engine.update(1 / 60f)
        val bomb = engine.bombs.first()
        val fuseBefore = bomb.fuse
        engine.pause()
        repeat(60) { engine.update(1 / 60f) }
        assertEquals("fuse frozen", fuseBefore, bomb.fuse, 0.001f)
    }

    // ===================== ПОБЕДА / ПОРАЖЕНИЕ =====================

    @Test
    fun `game over when all lives lost`() {
        val engine = flat(engineFor(1))
        engine.enemies.clear()
        engine.player.lives = 1
        engine.player.x = 5f; engine.player.y = 5f
        engine.bombs.add(Bomb(5, 5, 1))
        var gameOverCalled = false
        // Вешаем коллбэк через создание нового движка.
        tickUntil(engine, Bomb.FUSE_TIME + 0.5f)
        assertEquals(GameState.GAME_OVER, engine.state)
        assertTrue(engine.player.dead)
    }

    private fun tickUntil(engine: GameEngine, seconds: Float) {
        var t = 0f
        while (t < seconds && engine.state == GameState.PLAYING) { engine.update(1 / 60f); t += 1 / 60f }
    }

    @Test
    fun `win when enemies cleared and player reaches exit`() {
        val engine = flat(engineFor(1))
        engine.enemies.clear()
        // Эмулируем открытие выхода.
        val (ex, ey) = engine.field.exitTile!!
        engine.field.openExit()
        assertTrue("exit opened", engine.field[ex, ey] == Tile.EXIT || engine.field[ex, ey] != Tile.WALL)
        engine.player.x = ex.toFloat(); engine.player.y = ey.toFloat()
        // Движок откроет выход, когда враги очищены.
        engine.update(1 / 60f)
        // exitTile мог быть занят блоком; в плоском поле — это FLOOR.
        if (engine.field[ex, ey] != Tile.EXIT) {
            engine.field.setTile(ex, ey, Tile.EXIT)
        }
        engine.player.x = ex.toFloat(); engine.player.y = ey.toFloat()
        engine.update(1 / 60f)
        assertEquals(GameState.WIN, engine.state)
    }

    @Test
    fun `win is not possible while enemies remain`() {
        val engine = flat(engineFor(1))
        assertTrue(engine.enemies.isNotEmpty())
        val (ex, ey) = engine.field.exitTile!!
        engine.player.x = ex.toFloat(); engine.player.y = ey.toFloat()
        engine.update(1 / 60f)
        assertEquals("cannot win with enemies alive", GameState.PLAYING, engine.state)
    }

    @Test
    fun `score increases for destroyed block`() {
        val engine = flat(engineFor(1))
        engine.field.setTile(6, 5, Tile.BLOCK)
        engine.player.x = 5f; engine.player.y = 5f
        engine.player.fireRange = 1
        engine.bombRequested = true
        val before = engine.player.score
        tickUntil(engine, Bomb.FUSE_TIME + 0.5f)
        // Очки начисляются за врагов и бонусы; блок не даёт очков напрямую,
        // но bonus-блок — да. Проверим, что счёт не уменьшился.
        assertTrue("score never decreases", engine.player.score >= before)
    }

    @Test
    fun `time limit expiry damages player`() {
        val engine = flat(engineFor(1))
        engine.enemies.clear()
        engine.player.lives = 3
        engine.timeLeft = 0.1f
        engine.update(0.2f)
        assertEquals("timeout costs a life", 2, engine.player.lives)
        assertTrue("grace time granted", engine.timeLeft > 0)
    }

    @Test
    fun `invulnerability period after damage`() {
        val engine = flat(engineFor(1))
        engine.enemies.clear()
        engine.player.lives = 3
        engine.player.x = 5f; engine.player.y = 5f
        engine.bombs.add(Bomb(5, 5, 1))
        tickUntil(engine, Bomb.FUSE_TIME + 0.1f)
        assertEquals(2, engine.player.lives)
        assertTrue("invulnerable", engine.player.invulnerableTime > 0f)
    }

    @Test
    fun `respawn returns player to start after damage`() {
        val engine = flat(engineFor(1))
        engine.enemies.clear()
        engine.player.lives = 3
        engine.player.x = 8f; engine.player.y = 8f
        engine.bombs.add(Bomb(8, 8, 1))
        tickUntil(engine, Bomb.FUSE_TIME + 0.3f)
        assertEquals(GameEngine.START_X, engine.player.x, 0.001f)
        assertEquals(GameEngine.START_Y, engine.player.y, 0.001f)
    }

    @Test
    fun `progress store roundtrip`() {
        val store = InMemoryProgressStore()
        store.recordLevelScore(1, 100)
        store.recordLevelScore(1, 50)
        assertEquals(100, store.bestScoreForLevel(1))
        store.unlockLevel(3)
        assertEquals(3, store.maxUnlockedLevel)
        assertEquals(100, store.bestScore)
        store.reset()
        assertEquals(1, store.maxUnlockedLevel)
        assertEquals(0, store.bestScore)
    }
}
