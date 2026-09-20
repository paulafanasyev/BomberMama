#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Рисует логотип-баннер главного меню BomberMama в pixel-art стиле.

Состав: тёмно-зелёный фон в клеточку, бомба с горящим фитилем слева,
силуэт героини справа, высокая взрывная вспышка между ними.
Согласовано с палитрой игры (Palette.kt).
"""
import os
from PIL import Image

# Палитра игры (из Palette.kt)
C = {
    "floor_a": (63, 162, 63),
    "floor_b": (77, 184, 77),
    "floor_line": (46, 122, 46),
    "wall_light": (198, 210, 240),
    "wall_mid": (126, 140, 192),
    "wall_dark": (74, 86, 136),
    "wall_deep": (42, 48, 96),
    "block_light": (244, 198, 106),
    "block_mid": (222, 142, 52),
    "block_dark": (164, 90, 24),
    "bomb_light": (110, 110, 134),
    "bomb_mid": (62, 62, 86),
    "bomb_dark": (34, 34, 60),
    "fuse": (232, 180, 90),
    "spark": (255, 224, 102),
    "spark_hot": (255, 138, 61),
    "explo_core": (255, 248, 220),
    "explo_inner": (255, 228, 112),
    "explo_mid": (255, 168, 64),
    "explo_outer": (232, 92, 38),
    "skin": (248, 214, 174),
    "skin_shade": (224, 172, 128),
    "hair": (110, 58, 34),
    "hair_shade": (74, 36, 20),
    "dress": (255, 95, 142),
    "dress_shade": (198, 62, 104),
    "apron": (255, 239, 208),
    "eye": (42, 30, 62),
    "cheek": (255, 150, 168),
    "shoe": (58, 46, 80),
    "gold": (255, 224, 102),
    "pink": (224, 85, 122),
    "cream": (255, 246, 227),
    "navy": (27, 33, 72),
    "navy_deep": (18, 22, 50),
}

# Сетки пикселей: 1 = «крупный пиксель» (масштабируется PX)
PX = 4
W, H = 160, 96  # в крупных пикселях


def canvas():
    return Image.new("RGB", (W, H), C["navy_deep"])


def px(img, x, y, color):
    """Ставит один крупный пиксель."""
    if 0 <= x < W and 0 <= y < H:
        img.putpixel((x, y), color)


def rect(img, x0, y0, x1, y1, color):
    for y in range(y0, y1):
        for x in range(x0, x1):
            px(img, x, y, color)


def ellipse(img, cx, cy, rx, ry, color):
    for y in range(H):
        for x in range(W):
            dx = (x - cx) / max(rx, 0.5)
            dy = (y - cy) / max(ry, 0.5)
            if dx * dx + dy * dy <= 1.0:
                px(img, x, y, color)


def checker_bg(img):
    """Тёмный фон в мелкую клетку — как поле в тени."""
    for y in range(H):
        for x in range(W):
            rect_size = 8
            if ((x // rect_size) + (y // rect_size)) % 2 == 0:
                px(img, x, y, C["navy"])
            else:
                px(img, x, y, C["navy_deep"])


def draw_bomb(img, cx, cy, r, spark_frame=0):
    """Бомба: тёмный шар с бликом и горящим фитилем."""
    ellipse(img, cx, cy, r, r, C["bomb_mid"])
    ellipse(img, cx, cy, r - 2, r - 2, C["bomb_dark"])
    # Верхняя часть светлее (объём).
    ellipse(img, cx, cy - 1, r - 1, r - 1, C["bomb_mid"])
    ellipse(img, cx - r // 3, cy - r // 3, max(r // 4, 1), max(r // 4, 1), C["bomb_light"])
    # Фитиль.
    fx, fy = cx + r // 2 + 2, cy - r - 1
    px(img, fx, fy, C["fuse"])
    px(img, fx + 1, fy - 1, C["fuse"])
    px(img, fx + 2, fy - 2, C["fuse"])
    # Искры.
    if spark_frame == 0:
        px(img, fx + 3, fy - 3, C["spark"])
        px(img, fx + 4, fy - 2, C["spark_hot"])
        px(img, fx + 2, fy - 4, C["spark_hot"])
    else:
        px(img, fx + 3, fy - 4, C["spark"])
        px(img, fx + 4, fy - 3, C["spark"])
        px(img, fx + 1, fy - 3, C["spark_hot"])


def draw_burst(img, cx, cy, r):
    """Взрыв-звезда: ядро + лучи."""
    ellipse(img, cx, cy, r, r, C["explo_outer"])
    ellipse(img, cx, cy, r - 2, r - 2, C["explo_mid"])
    ellipse(img, cx, cy, r - 4, r - 4, C["explo_inner"])
    ellipse(img, cx, cy, r - 6, r - 6, C["explo_core"])
    # Лучи.
    for d in range(r + 2, r + 7):
        px(img, cx + d, cy, C["explo_mid"])
        px(img, cx - d, cy, C["explo_mid"])
        px(img, cx, cy + d, C["explo_mid"])
        px(img, cx, cy - d, C["explo_mid"])
    for d in range(r + 3, r + 6):
        px(img, cx + d, cy - 1, C["explo_inner"])
        px(img, cx - d, cy + 1, C["explo_inner"])
        px(img, cx + 1, cy + d, C["explo_inner"])
        px(img, cx - 1, cy - d, C["explo_inner"])


def draw_mama(img, cx, ground_y, scale=1):
    """Силуэт героини (фронт), pixel-art."""
    s = scale
    # Ноги.
    rect(img, cx - 3 * s, ground_y - 3 * s, cx - 1 * s, ground_y, C["shoe"])
    rect(img, cx + 1 * s, ground_y - 3 * s, cx + 3 * s, ground_y, C["shoe"])
    # Платье.
    rect(img, cx - 5 * s, ground_y - 12 * s, cx + 5 * s, ground_y - 2 * s, C["dress_shade"])
    rect(img, cx - 4 * s, ground_y - 12 * s, cx + 4 * s, ground_y - 3 * s, C["dress"])
    # Фартук.
    rect(img, cx - 2 * s, ground_y - 11 * s, cx + 2 * s, ground_y - 4 * s, C["apron"])
    # Руки.
    rect(img, cx - 6 * s, ground_y - 11 * s, cx - 4 * s, ground_y - 6 * s, C["skin"])
    rect(img, cx + 4 * s, ground_y - 11 * s, cx + 6 * s, ground_y - 6 * s, C["skin"])
    # Голова.
    hy = ground_y - 17 * s
    ellipse(img, cx, hy, 5 * s, 5 * s, C["skin"])
    # Причёска.
    ellipse(img, cx, hy - 3 * s, 6 * s, 4 * s, C["hair"])
    rect(img, cx - 5 * s, hy - 3 * s, cx + 5 * s, hy + 1 * s, C["hair"])
    rect(img, cx - 5 * s, hy - 3 * s, cx - 3 * s, hy + 4 * s, C["hair"])
    rect(img, cx + 3 * s, hy - 3 * s, cx + 5 * s, hy + 4 * s, C["hair"])
    # Чёлка.
    rect(img, cx - 4 * s, hy - 4 * s, cx + 4 * s, hy - 1 * s, C["hair_shade"])
    rect(img, cx - 4 * s, hy - 4 * s, cx + 4 * s, hy - 3 * s, C["hair"])
    # Глаза.
    rect(img, cx - 3 * s, hy, cx - 1 * s, hy + 1 * s, C["eye"])
    rect(img, cx + 1 * s, hy, cx + 3 * s, hy + 1 * s, C["eye"])
    # Щёки.
    px(img, cx - 4 * s, hy + 2 * s, C["cheek"])
    px(img, cx + 4 * s, hy + 2 * s, C["cheek"])
    # Улыбка.
    rect(img, cx - 1 * s, hy + 3 * s, cx + 1 * s, hy + 4 * s, C["hair_shade"])


def draw_stars(img):
    """Декоративные звёздочки на фоне."""
    stars = [(14, 14), (W - 16, 12), (22, H - 18), (W - 24, H - 22), (W // 2 - 30, 10)]
    for (sx, sy) in stars:
        px(img, sx, sy, C["gold"])
        px(img, sx - 1, sy, C["gold"])
        px(img, sx + 1, sy, C["gold"])
        px(img, sx, sy - 1, C["gold"])
        px(img, sx, sy + 1, C["gold"])
        px(img, sx, sy, C["explo_core"])


def make_banner(path, spark=0):
    img = canvas()
    checker_bg(img)
    draw_stars(img)
    # Бомба слева, героиня справа, взрыв по центру.
    draw_bomb(img, 42, H // 2 + 10, 16, spark)
    draw_mama(img, W - 46, H - 14, 2)
    draw_burst(img, W // 2 - 6, H // 2 + 2, 12)
    # Золотая каёмка.
    rect(img, 0, 0, W, 2, C["gold"])
    rect(img, 0, H - 2, W, H, C["gold"])
    rect(img, 0, 0, 2, H, C["gold"])
    rect(img, W - 2, 0, W, H, C["gold"])
    rect(img, 3, 3, W - 3, H - 3, C["pink"]) if False else None
    out = img.resize((W * PX, H * PX), Image.NEAREST)
    out.save(path, "PNG")
    print("saved", path, out.size)


if __name__ == "__main__":
    out_dir = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "app", "src", "main", "res", "drawable-nodpi")
    os.makedirs(out_dir, exist_ok=True)
    make_banner(os.path.join(out_dir, "logo_banner.png"), spark=0)
