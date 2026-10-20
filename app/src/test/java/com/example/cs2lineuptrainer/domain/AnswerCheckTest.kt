package com.example.cs2lineuptrainer.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnswerCheckTest {

    private val lineup = Lineup(
        id = "l",
        mapId = "m",
        grenade = GrenadeType.SMOKE,
        side = Side.T,
        area = Area.A,
        title = "t",
        target = NamedPoint("t", "t", 0f, 0f),
        origin = NamedPoint("o", "o", 0f, 0f),
        standView = LineupMedia.Image(0),
        aimPoint = Offset01(0.5f, 0.5f),
        tolerance = 0.05f,
        throwType = ThrowType.JUMP_THROW,
        howTo = LineupMedia.Image(0),
        description = "",
    )

    // Окно по центру: точка прицела оказывается в центре окна (0.5, 0.5).
    private val centered = FrameWindow(left = 0.25f, top = 0.25f)

    // Сдвинутое окно: точка прицела оказывается в окне в (0.8, 0.4).
    private val shifted = FrameWindow(left = 0.1f, top = 0.3f)

    @Test
    fun rightSpotAndRightThrow_isHit() {
        assertTrue(checkAnswer(lineup, centered, Offset01(0.5f, 0.5f), ThrowType.JUMP_THROW))
    }

    @Test
    fun rightSpotButWrongThrow_isMiss() {
        assertFalse(checkAnswer(lineup, centered, Offset01(0.5f, 0.5f), ThrowType.LMB))
    }

    @Test
    fun wrongSpotButRightThrow_isMiss() {
        assertFalse(checkAnswer(lineup, centered, Offset01(0.9f, 0.1f), ThrowType.JUMP_THROW))
    }

    @Test
    fun tapNearAimPointInsideTolerance_isHit() {
        assertTrue(checkAnswer(lineup, centered, Offset01(0.53f, 0.5f), ThrowType.JUMP_THROW))
    }

    // tolerance задан в долях показанного окна: 0.05 окна, то есть 0.025 целого кадра.
    @Test
    fun toleranceIsMeasuredInShownWindow() {
        assertTrue(checkAnswer(lineup, centered, Offset01(0.54f, 0.5f), ThrowType.JUMP_THROW))
        assertFalse(checkAnswer(lineup, centered, Offset01(0.58f, 0.5f), ThrowType.JUMP_THROW))
    }

    @Test
    fun answerDependsOnWindow_correctSpotMovesWithIt() {
        // В сдвинутом окне правильная точка уже не в центре.
        assertTrue(checkAnswer(lineup, shifted, Offset01(0.8f, 0.4f), ThrowType.JUMP_THROW))
        assertFalse(checkAnswer(lineup, shifted, Offset01(0.5f, 0.5f), ThrowType.JUMP_THROW))
    }

    // Зона на экране одного размера при любом зуме: тап в одном и том же месте окна даёт тот же результат.
    @Test
    fun toleranceOnScreenIsSameAtAnyZoom() {
        for (zoom in listOf(1.5f, 2f, 4f)) {
            val window = frameWindowFor(lineup.aimPoint, Offset01(0.5f, 0.5f), zoom)
            assertTrue("zoom=$zoom", checkAnswer(lineup, window, Offset01(0.54f, 0.5f), ThrowType.JUMP_THROW))
            assertFalse("zoom=$zoom", checkAnswer(lineup, window, Offset01(0.58f, 0.5f), ThrowType.JUMP_THROW))
        }
    }

    // Правильно ответить всегда возможно: тап ровно в правильную точку с верным броском засчитывается
    // при любом зуме и любом случайном окне.
    @Test
    fun tapOnTheCorrectPoint_isAlwaysAHit() {
        for (zoom in listOf(1f, 1.5f, 2f, 3f, 4f, 5f)) for (seed in 0 until 200) {
            val window = randomFrameWindow(lineup.aimPoint, kotlin.random.Random(seed), zoom)
            val correct = window.toLocal(lineup.aimPoint)
            assertTrue("zoom=$zoom seed=$seed", checkAnswer(lineup, window, correct, ThrowType.JUMP_THROW))
        }
    }

    // А в самой игре при большом зуме зона меньше, то есть точность выше.
    @Test
    fun toleranceInGameShrinksWithZoom() {
        val zoom2 = frameWindowFor(lineup.aimPoint, Offset01(0.5f, 0.5f), 2f)
        val zoom4 = frameWindowFor(lineup.aimPoint, Offset01(0.5f, 0.5f), 4f)
        val pointOnFrame = Offset01(0.5f + 0.02f, 0.5f)
        assertTrue(fallbackHitMask(lineup, zoom2).contains(pointOnFrame.x, pointOnFrame.y))
        assertFalse(fallbackHitMask(lineup, zoom4).contains(pointOnFrame.x, pointOnFrame.y))
    }

    // Логика проверки зависит только от интерфейса HitMask (он работает в координатах целого кадра): подставляем подделку.
    @Test
    fun customMask_isUsedInsteadOfCircle() {
        val cornerMask = HitMask { x, y -> x < 0.3f && y < 0.3f }
        // Точка (0.1, 0.1) окна centered лежит в целом кадре в (0.3, 0.3)... (граница не входит) и чуть левее - входит.
        assertTrue(checkAnswer(lineup, centered, Offset01(0.05f, 0.05f), ThrowType.JUMP_THROW, cornerMask))
        assertFalse(checkAnswer(lineup, centered, Offset01(0.5f, 0.5f), ThrowType.JUMP_THROW, cornerMask))
    }
}
