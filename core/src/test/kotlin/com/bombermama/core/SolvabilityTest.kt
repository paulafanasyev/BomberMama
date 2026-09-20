package com.bombermama.core

import org.junit.Assert.*
import org.junit.Test

/**
 * Проверка проходимости: на каждом уровне выход должен быть достижим
 * после расчистки пути бомбами (структурная проверка через BFS по
 * уничтожаемым блокам).
 */
class SolvabilityTest {

    @Test
    fun `every level has a path from spawn to exit through destroyable terrain`() {
        for (n in 1..Levels.count) {
            val spec = Levels.byNumber(n)!!
            val field = GameField.create(spec, spec.seed)
            // Старт точно свободен.
            assertTrue("level $n spawn blocked", field.isPassable(1, 1))
            // Выход существует.
            val (ex, ey) = field.exitTile!!
            // BFS по клеткам, проходимым с учётом разрушения блоков:
            // считаем блоки "условно проходимыми" — их можно взорвать.
            val visited = Array(field.height) { BooleanArray(field.width) }
            val q = ArrayDeque<Pair<Int, Int>>()
            q.add(1 to 1); visited[1][1] = true
            var reached = false
            while (q.isNotEmpty()) {
                val (x, y) = q.removeFirst()
                if (x == ex && y == ey) { reached = true; break }
                for (d in Direction.MOVING) {
                    val nx = x + d.dx; val ny = y + d.dy
                    if (!field.isInside(nx, ny) || visited[ny][nx]) continue
                    val t = field[nx, ny]
                    if (t == Tile.WALL) continue
                    // FLOOR/BLOCK/EXIT — блок можно разрушить, стена нет.
                    visited[ny][nx] = true
                    q.add(nx to ny)
                }
            }
            assertTrue("level $n exit unreachable even with bombs", reached)
        }
    }

    @Test
    fun `every level spawn area has place for a bomb`() {
        for (n in 1..Levels.count) {
            val spec = Levels.byNumber(n)!!
            val field = GameField.create(spec, spec.seed)
            // Героиня стоит на (1,1); хотя бы одна соседняя клетка
            // должна быть не стеной, чтобы bombing имел смысл.
            val neighbors = Direction.MOVING.map { 1 + it.dx to 1 + it.dy }
            val anyPassable = neighbors.any { (x, y) -> field.isInside(x, y) && field[x, y] != Tile.WALL }
            assertTrue("level $n heroine is fully walled in", anyPassable)
        }
    }
}
