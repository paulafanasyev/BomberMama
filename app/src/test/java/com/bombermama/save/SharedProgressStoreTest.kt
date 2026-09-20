package com.bombermama.save

import androidx.test.core.app.ApplicationProvider
import com.bombermama.core.Levels
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Тесты системы сохранений на JVM (через Robolectric).
 * Проверяем полный цикл: открытие уровня, запись рекорда, сброс.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SharedProgressStoreTest {

    private lateinit var store: SharedProgressStore

    @Before
    fun setUp() {
        store = SharedProgressStore(ApplicationProvider.getApplicationContext())
    }

    @Test
    fun `fresh store starts at level 1`() {
        assertEquals(1, store.maxUnlockedLevel)
        assertEquals(0, store.bestScore)
        assertTrue(store.soundEnabled)
        assertTrue(store.musicEnabled)
    }

    @Test
    fun `unlocking level persists progress`() {
        store.unlockLevel(5)
        assertEquals(5, store.maxUnlockedLevel)
        // Повторное открытие того же уровня не ломает состояние.
        store.unlockLevel(3)
        assertEquals(5, store.maxUnlockedLevel)
        store.unlockLevel(8)
        assertEquals(8, store.maxUnlockedLevel)
    }

    @Test
    fun `level score keeps only the best`() {
        store.recordLevelScore(1, 500)
        assertEquals(500, store.bestScoreForLevel(1))
        store.recordLevelScore(1, 300)
        assertEquals("худший результат не перезаписывает", 500, store.bestScoreForLevel(1))
        store.recordLevelScore(1, 900)
        assertEquals(900, store.bestScoreForLevel(1))
    }

    @Test
    fun `best score tracks maximum across levels`() {
        store.recordLevelScore(1, 400)
        store.recordLevelScore(2, 700)
        store.recordLevelScore(3, 100)
        assertEquals(700, store.bestScore)
    }

    @Test
    fun `sound and music toggles persist`() {
        store.soundEnabled = false
        store.musicEnabled = false
        assertFalse(store.soundEnabled)
        assertFalse(store.musicEnabled)
        store.soundEnabled = true
        assertTrue(store.soundEnabled)
    }

    @Test
    fun `survives store recreation`() {
        // Симулируем перезапуск приложения: новый экземпляр читает тот же файл.
        store.unlockLevel(6)
        store.recordLevelScore(2, 1234)
        val reopened = SharedProgressStore(ApplicationProvider.getApplicationContext())
        assertEquals(6, reopened.maxUnlockedLevel)
        assertEquals(1234, reopened.bestScoreForLevel(2))
    }

    @Test
    fun `reset clears everything`() {
        store.unlockLevel(10)
        store.recordLevelScore(1, 9999)
        store.reset()
        assertEquals(1, store.maxUnlockedLevel)
        assertEquals(0, store.bestScore)
        assertEquals(0, store.bestScoreForLevel(1))
    }

    @Test
    fun `progression opens all levels up to the last`() {
        // Проходение последнего уровня открывает "следующий" за пределами кампании.
        store.unlockLevel(Levels.count + 1)
        assertTrue(store.maxUnlockedLevel >= Levels.count)
    }
}
