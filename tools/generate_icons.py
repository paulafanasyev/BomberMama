#!/usr/bin/env python3
"""Генератор иконок BomberMama (pixel-art). Создаёт PNG для всех плотностей."""
import os
from PIL import Image, ImageDraw

# Палитра (зеркало Palette.kt)
C = {
    "bg":        (46, 39, 64, 255),
    "bg2":       (69, 60, 92, 255),
    "hair":      (138, 62, 46, 255),
    "hair_shade":(95, 40, 29, 255),
    "skin":      (245, 207, 168, 255),
    "dress":     (224, 85, 122, 255),
    "apron":     (246, 227, 200, 255),
    "eye":       (42, 30, 62, 255),
    "bomb":      (58, 58, 78, 255),
    "bomb_hi":   (90, 90, 110, 255),
    "fuse":      (216, 168, 92, 255),
    "spark":     (255, 224, 102, 255),
    "spark_hot": (255, 138, 61, 255),
    "star":      (255, 224, 102, 255),
    "cheek":     (232, 136, 152, 255),
}

# Логическая сетка иконки 27x27 "пикселей" арта
ART = 27

def pixel(draw, grid, x, y, color):
    """Закрасить клетку (x,y) сетки размером grid."""
    g = grid
    draw.rectangle([x*g, y*g, (x+1)*g-1, (y+1)*g-1], fill=color)

def draw_icon(size):
    """Рисуем иконку размера size x size пикселей."""
    img = Image.new("RGBA", (size, size), C["bg"])
    d = ImageDraw.Draw(img)
    g = size // ART
    # Верхняя половина чуть светлее (градиент-имитация)
    d.rectangle([0, 0, size, size//2], fill=C["bg2"])
    d.rectangle([0, 0, size, size], fill=None)

    # ---------- Бомба (слева от героини, в руке) ----------
    # Корпус
    for (x, y) in [(17,14),(18,14),(19,14),(20,14),
                   (16,15),(17,15),(18,15),(19,15),(20,15),(21,15),
                   (16,16),(17,16),(18,16),(19,16),(20,16),(21,16),
                   (16,17),(17,17),(18,17),(19,17),(20,17),(21,17),
                   (17,18),(18,18),(19,18),(20,18)]:
        pixel(d, g, x, y, C["bomb"])
    # Блик на бомбе
    for (x, y) in [(17,15),(18,15),(17,16)]:
        pixel(d, g, x, y, C["bomb_hi"])
    # Фитиль
    pixel(d, g, 18, 11, C["fuse"])
    pixel(d, g, 18, 12, C["fuse"])
    pixel(d, g, 18, 13, C["fuse"])
    # Искра
    pixel(d, g, 17, 9, C["spark"])
    pixel(d, g, 18, 9, C["spark"])
    pixel(d, g, 19, 9, C["spark"])
    pixel(d, g, 18, 10, C["spark"])
    pixel(d, g, 18, 8, C["spark_hot"])

    # ---------- Героиня (Мама) ----------
    # Волосы (контур)
    for y in (7, 8):
        for x in range(6, 16):
            pixel(d, g, x, y, C["hair"])
    for y in (9, 10, 11):
        for x in (5, 6):
            pixel(d, g, x, y, C["hair"])
        for x in (15, 16):
            pixel(d, g, x, y, C["hair"])
    # Лицо
    for y in (9, 10, 11):
        for x in range(7, 15):
            pixel(d, g, x, y, C["skin"])
    # Чёлка
    for x in range(6, 16):
        pixel(d, g, x, 8, C["hair_shade"])
    pixel(d, g, 7, 9, C["hair_shade"])
    # Глаза
    pixel(d, g, 9, 10, C["eye"])
    pixel(d, g, 12, 10, C["eye"])
    # Щёчки
    pixel(d, g, 8, 11, C["cheek"])
    pixel(d, g, 13, 11, C["cheek"])
    # Улыбка
    for x in (10, 11):
        pixel(d, g, x, 11, C["hair_shade"])
    # Шея
    for x in (10, 11):
        pixel(d, g, x, 12, C["skin"])
    # Платье
    for y in (13, 14, 15, 16, 17, 18):
        for x in range(6, 16):
            pixel(d, g, x, y, C["dress"])
    # Фартук
    for y in (14, 15, 16, 17):
        for x in range(8, 14):
            pixel(d, g, x, y, C["apron"])
    # Поясок фартука
    for x in range(8, 14):
        pixel(d, g, x, 16, C["hair_shade"])
    # Руки
    for y in (14, 15):
        pixel(d, g, 4, y, C["skin"])
        pixel(d, g, 5, y, C["skin"])
        pixel(d, g, 16, y, C["skin"])
        pixel(d, g, 17, y, C["skin"])
    # Ноги/ботинки
    for y in (19, 20):
        for x in (8, 9, 11, 12):
            pixel(d, g, x, y, (58, 46, 80, 255))

    # ---------- Звёздочки по углам ----------
    pixel(d, g, 2, 3, C["star"])
    pixel(d, g, 3, 3, C["star"])
    pixel(d, g, 2, 4, C["star"])
    pixel(d, g, 23, 3, C["star"])
    pixel(d, g, 24, 3, C["star"])
    pixel(d, g, 24, 4, C["star"])

    return img

def main():
    base = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    res = os.path.join(base, "app", "src", "main", "res")
    densities = {
        "mipmap-mdpi": 48,
        "mipmap-hdpi": 72,
        "mipmap-xhdpi": 96,
        "mipmap-xxhdpi": 144,
        "mipmap-xxxhdpi": 192,
    }
    for folder, size in densities.items():
        out_dir = os.path.join(res, folder)
        os.makedirs(out_dir, exist_ok=True)
        for name in ("ic_launcher.png", "ic_launcher_round.png"):
            img = draw_icon(size)
            img.save(os.path.join(out_dir, name))
            print(f"{folder}/{name} {size}px")

if __name__ == "__main__":
    main()
