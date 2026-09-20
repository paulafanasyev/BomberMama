# 💣 BOMBERMAMA

**Современная интерпретация классического 8-битного Bomberman для Android.**

Оригинальный персонаж — мама-героиня — прокладывает путь через 24 уровня,
расставляя бомбы, взрывая блоки, собирая бонусы и побеждая врагов.

Полностью на **русском языке**. Работает **офлайн**. Никаких серверов.

---

## 📸 Скриншоты

Сгенерированы **реальным движком** (модуль `:screenshot`): настоящий `GameEngine`,
настоящий игровой цикл, настоящий рендер — не мокапы.

<table>
  <tr><td align="center">Главное меню</td><td align="center">Старт уровня</td></tr>
  <tr>
    <td><img src="https://github.com/paulafanasyev/BomberMama/releases/download/v1.0/menu.png" width="400"></td>
    <td><img src="https://github.com/paulafanasyev/BomberMama/releases/download/v1.0/level_1_start.png" width="400"></td>
  </tr>
  <tr><td align="center">Взрыв крестом</td><td align="center">Цепная реакция</td></tr>
  <tr>
    <td><img src="https://github.com/paulafanasyev/BomberMama/releases/download/v1.0/level_2_explosion.png" width="400"></td>
    <td><img src="https://github.com/paulafanasyev/BomberMama/releases/download/v1.0/chain_reaction.png" width="400"></td>
  </tr>
  <tr><td align="center">Бонусы</td><td align="center">Враги</td></tr>
  <tr>
    <td><img src="https://github.com/paulafanasyev/BomberMama/releases/download/v1.0/bonuses.png" width="400"></td>
    <td><img src="https://github.com/paulafanasyev/BomberMama/releases/download/v1.0/enemies.png" width="400"></td>
  </tr>
  <tr><td align="center">Пауза</td><td align="center">Победа</td></tr>
  <tr>
    <td><img src="https://github.com/paulafanasyev/BomberMama/releases/download/v1.0/paused.png" width="400"></td>
    <td><img src="https://github.com/paulafanasyev/BomberMama/releases/download/v1.0/win.png" width="400"></td>
  </tr>
</table>

Все скриншоты: [страница релиза v1.0](https://github.com/paulafanasyev/BomberMama/releases/tag/v1.0)

---

## 🎮 Геймплей

- **Вид сверху**, квадратное клеточное поле
- Перемещение по клеткам, **стены** (неразрушаемые) и **блоки** (разрушаемые)
- Бомбы со взрывом **крестом** по четырём направлениям
- **Цепные реакции**: взрыв детонирует другие бомбы (A → B → C)
- **5 видов бонусов**: 🔥 радиус взрыва · 💣 extra-бомба · 🏃 скорость · ❤️ жизнь · ⭐ очки
- **4 типа врагов** с разным AI: медленный, быстрый, меняющий направление, преследователь
- **24 уровня** с 10 уникальными схемами стен и нарастающей сложностью
- Ограниченное количество жизней, очки, пауза, победа, поражение
- **Бесконечный режим** после прохождения кампании

## 🛠 Технологии

Чистый Kotlin, без Unity и тяжёлых движков:

| Слой | Технология |
|---|---|
| Движок | Kotlin + JVM (модуль `:core`, **без Android-зависимостей**) |
| Графика | Canvas/SurfaceView, процедурный pixel-art |
| Аудио | AudioTrack, процедурный 8-битный синтез |
| UI | Activities + XML-разметка, кастомные стили |
| Сохранения | SharedPreferences (полностью офлайн) |

**Вся графика и звук генерируются процедурно** — ни одного внешнего ассета.
Это гарантирует единый визуальный стиль и малый размер APK (~2.7 МБ release).

## 📐 Архитектура

```
core/  — игровой движок (чистый Kotlin, тестируется на JVM)
       GameEngine · GameField · Player · Enemy · Bomb
       ExplosionSegment · BonusPickup · Levels · ProgressStore

app/   — Android-слой
       game/   — GameActivity, GameSurfaceView, GameRenderer
       render/ — SpriteFactory (генерация спрайтов), Palette
       audio/  — AudioEngine (SFX), MusicEngine (8-bit музыка)
       input/  — TouchInputController (D-pad + кнопка бомбы)
       save/   — SharedProgressStore
       ui/     — MainActivity, LevelsActivity, RecordsActivity,
                 SettingsActivity, HelpActivity

screenshot/ — генератор скриншотов (чистая JVM)
       ScreenshotMaker · SceneRenderer · SpriteFactory · PixCanvas
       PngEncoder · Glyph (пиксельный шрифт 5x7 с кириллицей)
       Запускает настоящий GameEngine и рендерит кадры геймплея.
```

## ✅ Качество

**103 автоматических теста**, все проходят:

- `core` (65 тестов) — движение и коллизии, бомбы, цепные реакции,
  двойной урон, враги, бонусы, полный игровой цикл от спавна до выхода,
  **проходимость всех 24 уровней**
- `app` (38 тестов) — рендеринг, обработка касаний, сохранения,
  игровой цикл (Robolectric)

### Найденные и исправленные реальные баги

1. Схема стен `CHECKERBOARD` делала уровни 4/10/18 **непроходимыми** —
   стены отрезали каждую клетку пола. Исправлено; добавлен тест проходимости.
2. Игрок мог **заспавниться в стене** (схемы `SPIRAL`/`ROOMS`) —
   добавлено принудительное вычищение стартовой зоны.
3. `damagePlayer()` **стирал кадр взрыва** и мог ломать текущую цепочку —
   теперь снимаются только невзорвавшиеся бомбы, а поиск цепочки идёт по снимку.
4. **Порядок каналов в PNG-энкодере** скриншотов был ARGB вместо RGBA —
   все цвета отображались смещёнными. Исправлено; проверено попиксельно.

## 🚀 Сборка

Требуется JDK 17, Android SDK 36, AGP 8.11.0, Kotlin 1.9.22.

```bash
./gradlew clean
./gradlew assembleDebug      # app/build/outputs/apk/debug/app-debug.apk
./gradlew assembleRelease    # app/build/outputs/apk/release/app-release-unsigned.apk
./gradlew test               # 103 теста
```

Установите `app-debug.apk` на Android 5.0+ (minSdk 21).

## 📱 Управление

- **Виртуальный D-pad** слева — движение (с поддержкой удержания)
- **Кнопка 💣** справа — установка бомбы
- Кнопка **⏸** — пауза (симуляция полностью останавливается)

## 🌐 Лицензия

Собственный оригинальный проект. Не использует защищённые ассеты, персонажей,
музыку или торговые марки оригинального Bomberman.

---

**BomberMama** · versionCode 1 · versionName 1.0
