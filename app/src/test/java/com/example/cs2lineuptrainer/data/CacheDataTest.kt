package com.example.cs2lineuptrainer.data

import com.example.cs2lineuptrainer.domain.MAX_ZOOM
import com.example.cs2lineuptrainer.domain.MIN_ZOOM
import com.example.cs2lineuptrainer.domain.checkAnswer
import com.example.cs2lineuptrainer.domain.randomFrameWindow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// Проверки целостности данных: ловят опечатки при ручном заполнении лайнапов.
class CacheDataTest {

    private val lineups = CacheData.lineups

    @Test
    fun lineupIds_areUnique() {
        assertEquals(lineups.size, lineups.map { it.id }.toSet().size)
    }

    // Точки с одним id должны быть одной и той же точкой (одинаковое имя и координаты).
    @Test
    fun pointsWithSameId_areIdentical() {
        val allPoints = lineups.flatMap { listOf(it.target, it.origin) }
        allPoints.groupBy { it.id }.forEach { (id, points) ->
            assertEquals("point '$id' differs between lineups", 1, points.distinct().size)
        }
    }

    @Test
    fun allCoordinates_areInsideRadar() {
        lineups.flatMap { listOf(it.target, it.origin) }.forEach { p ->
            assertTrue("${p.id} x=${p.x}", p.x in 0f..1f)
            assertTrue("${p.id} y=${p.y}", p.y in 0f..1f)
        }
    }

    @Test
    fun noTwoLineups_shareGrenadeTargetAndOrigin() {
        val keys = lineups.map { Triple(it.grenade, it.target.id, it.origin.id) }
        assertEquals(keys.size, keys.toSet().size)
    }

    @Test
    fun zoom_isInsideAllowedRange() {
        lineups.forEach { assertTrue("${it.id} zoom=${it.zoom}", it.zoom in MIN_ZOOM..MAX_ZOOM) }
    }

    // На каждом настоящем лайнапе с его зумом правильная точка всегда внутри окна (с запасом от края)
    // и тап в неё с верным броском засчитывается: ответить правильно возможно при любом случайном окне.
    @Test
    fun everyLineup_isAnswerableAtItsZoom() {
        lineups.forEach { lineup ->
            repeat(300) { seed ->
                val window = randomFrameWindow(lineup.aimPoint, kotlin.random.Random(seed), lineup.zoom)
                val correct = window.toLocal(lineup.aimPoint)
                val message = "${lineup.id} zoom=${lineup.zoom} seed=$seed correct=$correct"
                assertTrue(message, correct.x in 0.08f..0.92f && correct.y in 0.08f..0.92f)
                assertTrue(message, checkAnswer(lineup, window, correct, lineup.throwType))
            }
        }
    }
}
