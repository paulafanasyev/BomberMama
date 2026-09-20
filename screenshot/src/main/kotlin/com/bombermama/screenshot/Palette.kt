package com.bombermama.screenshot

/**
 * Палитра игры в стиле современной 8-битной пиксель-арта.
 * Единый набор цветов обеспечивает согласованный визуальный стиль.
 */
object Palette {
    // Базовые цвета
    const val TRANSPARENT = 0x00000000
    const val BLACK = 0xFF1A1626.toInt()
    const val DARK = 0xFF2E2740.toInt()
    const val SHADOW = 0xFF453C5C.toInt()

    // Пол
    const val FLOOR_A = 0xFF3B3160.toInt()
    const val FLOOR_B = 0xFF453A6E.toInt()
    const val FLOOR_LINE = 0xFF2C2450.toInt()

    // Стены (камень)
    const val WALL_LIGHT = 0xFF8E8BA8.toInt()
    const val WALL_MID = 0xFF6C6889.toInt()
    const val WALL_DARK = 0xFF4A4664.toInt()
    const val WALL_DEEP = 0xFF332F4C.toInt()

    // Разрушаемые блоки (коричневый кирпич)
    const val BLOCK_LIGHT = 0xFFD8A85C.toInt()
    const val BLOCK_MID = 0xFFB5813F.toInt()
    const val BLOCK_DARK = 0xFF7E5322.toInt()
    const val BLOCK_DEEP = 0xFF523314.toInt()

    // Героиня (Мама)
    const val SKIN = 0xFFF5CFA8.toInt()
    const val SKIN_SHADE = 0xFFD9A57C.toInt()
    const val HAIR = 0xFF8A3E2E.toInt()       // тёмно-каштановые
    const val HAIR_SHADE = 0xFF5F281D.toInt()
    const val DRESS = 0xFFE0557A.toInt()      // розово-красное платье
    const val DRESS_SHADE = 0xFFB03A5A.toInt()
    const val APRON = 0xFFF6E3C8.toInt()      // фартук
    const val APRON_SHADE = 0xFFD9BE99.toInt()
    const val EYE = 0xFF2A1E3E.toInt()
    const val CHEEK = 0xFFE88898.toInt()
    const val SHOE = 0xFF3A2E50.toInt()

    // Бомба
    const val BOMB_LIGHT = 0xFF5A5A6E.toInt()
    const val BOMB_MID = 0xFF3A3A4E.toInt()
    const val BOMB_DARK = 0xFF222232.toInt()
    const val FUSE = 0xFFD8A85C.toInt()
    const val SPARK = 0xFFFFE066.toInt()
    const val SPARK_HOT = 0xFFFF8A3D.toInt()

    // Взрыв
    const val EXPLO_CORE = 0xFFFFF6D8.toInt()
    const val EXPLO_INNER = 0xFFFFE066.toInt()
    const val EXPLO_MID = 0xFFFF9F3D.toInt()
    const val EXPLO_OUTER = 0xFFE0552E.toInt()
    const val EXPLO_FADE = 0xFF8A2E2E.toInt()

    // Враги
    const val SLIME_A = 0xFF7BD88A.toInt()     // зелёный слайм
    const val SLIME_B = 0xFF4E9E5A.toInt()
    const val SLIME_C = 0xFF2E6E38.toInt()
    const val BAT_A = 0xFFB47AD8.toInt()       // фиолетовая летучая мышь
    const val BAT_B = 0xFF8A4EB0.toInt()
    const val BAT_C = 0xFF5A2E7E.toInt()
    const val GHOST_A = 0xFF7AD8D4.toInt()     // бирюзовый призрак
    const val GHOST_B = 0xFF4EA39E.toInt()
    const val GHOST_C = 0xFF2E6E6A.toInt()
    const val DEMON_A = 0xFFE06A5A.toInt()     // красный демон
    const val DEMON_B = 0xFFB04A3A.toInt()
    const val DEMON_C = 0xFF7E2E22.toInt()

    // Бонусы
    const val BONUS_FIRE_A = 0xFFFF9F3D.toInt()
    const val BONUS_FIRE_B = 0xFFE0552E.toInt()
    const val BONUS_BOMB_A = 0xFF8E9AD8.toInt()
    const val BONUS_BOMB_B = 0xFF5A6AB0.toInt()
    const val BONUS_SPEED_A = 0xFF8EE8C8.toInt()
    const val BONUS_SPEED_B = 0xFF4EB08E.toInt()
    const val BONUS_LIFE_A = 0xFFFF6A8E.toInt()
    const val BONUS_LIFE_B = 0xFFD83A5E.toInt()
    const val BONUS_STAR_A = 0xFFFFE066.toInt()
    const val BONUS_STAR_B = 0xFFD8A82E.toInt()

    // Выход
    const val EXIT_A = 0xFF8EE8C8.toInt()
    const val EXIT_B = 0xFF2E8E7A.toInt()
    const val EXIT_C = 0xFF1E5E4E.toInt()

    // UI
    const val UI_BG = 0xFF2E2740.toInt()
    const val UI_PANEL = 0xFF453C5C.toInt()
    const val UI_ACCENT = 0xFFE0557A.toInt()
    const val UI_TEXT = 0xFFF6E3C8.toInt()
    const val UI_DIM = 0xFF8E8BA8.toInt()
    const val UI_GREEN = 0xFF7BD88A.toInt()
    const val UI_RED = 0xFFE06A5A.toInt()

    // Эффекты
    const val PARTICLE_A = 0xFFFFE066.toInt()
    const val PARTICLE_B = 0xFFFF9F3D.toInt()
    const val CONFETTI_1 = 0xFFE0557A.toInt()
    const val CONFETTI_2 = 0xFF7BD88A.toInt()
    const val CONFETTI_3 = 0xFFFFE066.toInt()
    const val CONFETTI_4 = 0xFF7AD8D4.toInt()
    const val CONFETTI_5 = 0xFFB47AD8.toInt()
}
