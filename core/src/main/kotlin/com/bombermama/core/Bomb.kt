package com.bombermama.core

/**
 * Бомба, установленная героиней.
 *
 * Жизненный цикл:
 * 1. устанавливается ([placed]);
 * 2. таймер тикает, бомба мигает ([fuse]);
 * 3. взрывается ([explode]), создавая крест взрыва;
 * 4. уничтожает блоки, врагов, наносит урон игроку;
 * 5. исчезает.
 */
class Bomb(
    val tileX: Int,
    val tileY: Int,
    val fireRange: Int,
    var fuse: Float = FUSE_TIME
) {
    /** Бомба уже взорвалась (для корректной обработки цепных реакций). */
    var exploded: Boolean = false
        private set

    /** Время с момента установки — для анимации мигания. */
    var age: Float = 0f
        private set

    /** true, пока бомба "жива" (не взорвалась). */
    val isActive: Boolean get() = !exploded

    fun tick(dt: Float) {
        age += dt
        fuse -= dt
    }

    fun explode() {
        exploded = true
        fuse = 0f
    }

    /** Фаза мигания 0..3 для пиксельной анимации (учащается к концу). */
    val blinkPhase: Int
        get() {
            val period = if (fuse < 0.5f) 0.14f else if (fuse < 1.2f) 0.25f else 0.4f
            return ((age / period).toInt() % 2)
        }

    companion object {
        const val FUSE_TIME = 2.2f
    }
}

/**
 * Один сегмент взрыва (клетка креста).
 */
class ExplosionSegment(
    val tileX: Int,
    val tileY: Int,
    /** true для центральной клетки, false для "рукавов" креста. */
    val isCenter: Boolean,
    /** Направление рукава от центра. */
    val direction: Direction,
    /** true если это кончик рукава (последняя клетка). */
    val isTip: Boolean
) {
    var timeLeft: Float = DURATION

    fun tick(dt: Float): Boolean {
        timeLeft -= dt
        return timeLeft <= 0f
    }

    /** Фаза анимации 0..2 (расширение, удержание, затухание). */
    val animPhase: Int
        get() = when {
            timeLeft > DURATION - 0.08f -> 0
            timeLeft > DURATION * 0.45f -> 1
            else -> 2
        }

    companion object {
        const val DURATION = 0.62f
    }
}
