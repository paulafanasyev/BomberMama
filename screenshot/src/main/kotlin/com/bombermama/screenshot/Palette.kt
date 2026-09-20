package com.bombermama.screenshot

/**
 * Палитра игры в стиле современной 8-битной пиксель-арта.
 * Единый набор цветов обеспечивает согласованный визуальный стиль.
 *
 * Гамма: сочное травяное поле, холодный камень стен, тёплый кирпич блоков,
 * яркие насыщенные акценты. Фон вокруг поля — глубокий ночной синий,
 * чтобы поле "светилось" на экране.
 */
object Palette {
    // Базовые цвета
    const val TRANSPARENT = 0x00000000
    const val BLACK = 0xFF1B2148.toInt()        // ночной синий фон вокруг поля
    const val DARK = 0xFF27305E.toInt()
    const val SHADOW = 0xFF3A4477.toInt()

    // Пол (трава) — насыщенный, чистый зелёный с лёгким шахматным переходом
    const val FLOOR_A = 0xFF3FA23F.toInt()
    const val FLOOR_B = 0xFF4DB84D.toInt()
    const val FLOOR_LINE = 0xFF2E7A2E.toInt()

    // Стены (камень) — холодный, контрастный, с чёткими гранями
    const val WALL_LIGHT = 0xFFC6D2F0.toInt()
    const val WALL_MID = 0xFF7E8CC0.toInt()
    const val WALL_DARK = 0xFF4A5688.toInt()
    const val WALL_DEEP = 0xFF2A3060.toInt()

    // Разрушаемые блоки (кирпич) — тёплые, сочные, золотисто-оранжевые
    const val BLOCK_LIGHT = 0xFFF4C66A.toInt()
    const val BLOCK_MID = 0xFFDE8E34.toInt()
    const val BLOCK_DARK = 0xFFA45A18.toInt()
    const val BLOCK_DEEP = 0xFF6E3C0E.toInt()

    // Героиня (Мама)
    const val SKIN = 0xFFF8D6AE.toInt()
    const val SKIN_SHADE = 0xFFE0AC80.toInt()
    const val HAIR = 0xFF6E3A22.toInt()       // тёмно-каштановые
    const val HAIR_SHADE = 0xFF4A2414.toInt()
    const val DRESS = 0xFFFF5F8E.toInt()      // розово-красное платье
    const val DRESS_SHADE = 0xFFC63E68.toInt()
    const val APRON = 0xFFFFEFD0.toInt()      // фартук
    const val APRON_SHADE = 0xFFE4CE9E.toInt()
    const val EYE = 0xFF2A1E3E.toInt()
    const val CHEEK = 0xFFFF96A8.toInt()
    const val SHOE = 0xFF3A2E50.toInt()

    // Бомба
    const val BOMB_LIGHT = 0xFF6E6E86.toInt()
    const val BOMB_MID = 0xFF3E3E56.toInt()
    const val BOMB_DARK = 0xFF22223C.toInt()
    const val FUSE = 0xFFE8B45A.toInt()
    const val SPARK = 0xFFFFE066.toInt()
    const val SPARK_HOT = 0xFFFF8A3D.toInt()

    // Взрыв
    const val EXPLO_CORE = 0xFFFFF8DC.toInt()
    const val EXPLO_INNER = 0xFFFFE470.toInt()
    const val EXPLO_MID = 0xFFFFA840.toInt()
    const val EXPLO_OUTER = 0xFFE85C26.toInt()
    const val EXPLO_FADE = 0xFF8A2E2E.toInt()

    // Враги
    const val SLIME_A = 0xFF8AE89A.toInt()     // зелёный слайм
    const val SLIME_B = 0xFF4FA85C.toInt()
    const val SLIME_C = 0xFF2A7038.toInt()
    const val BAT_A = 0xFFC48AEC.toInt()       // фиолетовая летучая мышь
    const val BAT_B = 0xFF9452BC.toInt()
    const val BAT_C = 0xFF5E2E84.toInt()
    const val GHOST_A = 0xFF8EE8E4.toInt()     // бирюзовый призрак
    const val GHOST_B = 0xFF4FB0AA.toInt()
    const val GHOST_C = 0xFF2A7268.toInt()
    const val DEMON_A = 0xFFFF7E68.toInt()     // красный демон
    const val DEMON_B = 0xFFD84E38.toInt()
    const val DEMON_C = 0xFF8E2A1E.toInt()

    // Бонусы
    const val BONUS_FIRE_A = 0xFFFFA840.toInt()
    const val BONUS_FIRE_B = 0xFFE85C26.toInt()
    const val BONUS_BOMB_A = 0xFF9AB0F4.toInt()
    const val BONUS_BOMB_B = 0xFF5A6AB8.toInt()
    const val BONUS_SPEED_A = 0xFF8EE8C8.toInt()
    const val BONUS_SPEED_B = 0xFF3EAE8E.toInt()
    const val BONUS_LIFE_A = 0xFFFF6A96.toInt()
    const val BONUS_LIFE_B = 0xFFD8345E.toInt()
    const val BONUS_STAR_A = 0xFFFFE470.toInt()
    const val BONUS_STAR_B = 0xFFDEA82E.toInt()

    // Выход
    const val EXIT_A = 0xFF8EE8C8.toInt()
    const val EXIT_B = 0xFF2E9E86.toInt()
    const val EXIT_C = 0xFF1E6252.toInt()

    // UI
    const val UI_BG = 0xFF27305E.toInt()
    const val UI_PANEL = 0xFF3A4477.toInt()
    const val UI_ACCENT = 0xFFFF5F8E.toInt()
    const val UI_TEXT = 0xFFFFEFD0.toInt()
    const val UI_DIM = 0xFFAAB4D8.toInt()
    const val UI_GREEN = 0xFF8AE89A.toInt()
    const val UI_RED = 0xFFFF7E68.toInt()

    // Эффекты
    const val PARTICLE_A = 0xFFFFE470.toInt()
    const val PARTICLE_B = 0xFFFFA840.toInt()
    const val CONFETTI_1 = 0xFFFF5F8E.toInt()
    const val CONFETTI_2 = 0xFF8AE89A.toInt()
    const val CONFETTI_3 = 0xFFFFE470.toInt()
    const val CONFETTI_4 = 0xFF8EE8E4.toInt()
    const val CONFETTI_5 = 0xFFC48AEC.toInt()
}
