package com.bombermama.core

import kotlin.random.Random

/**
 * Враг. Перемещается по клеткам, реагирует на стены, уничтожается взрывом.
 *
 * Поведение зависит от [type]:
 *  - SLOW    — медленно бродит, редко меняет направление;
 *  - FAST    — быстро носится, часто меняет направление;
 *  - CHANGER — постоянно меняет направление на развилках;
 *  - HUNTER  — преследует игрока, когда видит его по прямой.
 */
class Enemy(
    startX: Float,
    startY: Float,
    val type: EnemyType,
    private val random: Random
) : GridEntity(startX, startY, pickInitialDirection(random)) {

    /** Уничтожен взрывом. */
    var dead: Boolean = false
        private set

    /** Время с момента гибели — для анимации смерти и последующего удаления. */
    var deathTime: Float = 0f
        private set

    private var animTime: Float = 0f
    private var decisionCooldown: Float = 0f

    fun kill() {
        if (!dead) {
            dead = true
            deathTime = 0f
            direction = Direction.NONE
        }
    }

    /** Продлить анимацию смерти. Вызывается движком каждый кадр. */
    fun tickDeath(dt: Float) {
        if (dead) deathTime += dt
    }

    override fun animPhase(time: Float): Float {
        return if (isMoving) (animTime * type.maxSpeed * 3f) % 1f else 0f
    }

    /**
     * Выбирает новое направление на основе типа врага и окружения.
     * Вызывается движком, когда враг доходит до центра клетки или упирается в стену.
     *
     * @param isPassable функция, проверяющая проходимость клетки
     * @param playerX позиция игрока по X (в клетках)
     * @param playerY позиция игрока по Y (в клетках)
     */
    fun chooseDirection(
        isPassable: (Int, Int) -> Boolean,
        playerX: Float,
        playerY: Float
    ): Direction {
        val tx = tileX
        val ty = tileY

        // Все доступные направления из текущей клетки.
        val available = Direction.MOVING.filter { d ->
            isPassable(tx + d.dx, ty + d.dy)
        }

        if (available.isEmpty()) {
            direction = Direction.NONE
            return Direction.NONE
        }

        // HUNTER: если игрок на одной линии и нет стены — двигаемся к нему.
        if (type.chasePlayer && tryChase(available, tx, ty, playerX, playerY)) {
            return direction
        }

        // Продолжаем текущее направление, если возможно и не хочется разворачиваться.
        val prefersCurrent = available.contains(direction) &&
            random.nextFloat() >= type.changeDirectionChance

        if (prefersCurrent) return direction

        // Не возвращаемся назад, если есть альтернатива.
        val noReverse = available.filter { it != direction.opposite() }
        val pool = if (noReverse.isNotEmpty()) noReverse else available

        direction = pool.random(random)
        return direction
    }

    private fun tryChase(
        available: List<Direction>,
        tx: Int,
        ty: Int,
        playerX: Float,
        playerY: Float
    ): Boolean {
        val ptx = (playerX + 0.5f).toInt()
        val pty = (playerY + 0.5f).toInt()
        if (ptx == tx && pty != ty) {
            val toward = if (pty > ty) Direction.DOWN else Direction.UP
            if (available.contains(toward)) {
                direction = toward
                return true
            }
        } else if (pty == ty && ptx != tx) {
            val toward = if (ptx > tx) Direction.RIGHT else Direction.LEFT
            if (available.contains(toward)) {
                direction = toward
                return true
            }
        }
        return false
    }

    fun tickAnimation(dt: Float) {
        animTime += dt
        if (decisionCooldown > 0) decisionCooldown -= dt
        tickDeath(dt)
    }

    companion object {
        private fun pickInitialDirection(random: Random): Direction =
            Direction.MOVING.random(random)
    }
}
