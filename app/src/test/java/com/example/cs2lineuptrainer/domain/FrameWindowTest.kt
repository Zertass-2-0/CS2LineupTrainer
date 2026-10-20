package com.example.cs2lineuptrainer.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class FrameWindowTest {

    private val center = Offset01(0.5f, 0.5f)

    @Test
    fun aimPointAppearsWhereRequested() {
        val window = frameWindowFor(center, wanted = Offset01(0.35f, 0.65f))
        val local = window.toLocal(center)
        assertEquals(0.35f, local.x, 0.0001f)
        assertEquals(0.65f, local.y, 0.0001f)
    }

    @Test
    fun windowSize_followsZoom() {
        assertEquals(0.5f, windowSizeFor(2f), 0.0001f)
        assertEquals(0.25f, windowSizeFor(4f), 0.0001f)
        assertEquals(0.25f, frameWindowFor(center, Offset01(0.5f, 0.5f), zoom = 4f).size, 0.0001f)
    }

    @Test
    fun zoom_isClampedToAllowedRange() {
        assertEquals(1f / MAX_ZOOM, windowSizeFor(100f), 0.0001f)
        assertEquals(1f, windowSizeFor(0.3f), 0.0001f)
    }

    @Test
    fun zoomOne_showsTheWholeFrame() {
        val window = frameWindowFor(center, Offset01(0.7f, 0.7f), zoom = 1f)
        assertEquals(0f, window.left, 0.0001f)
        assertEquals(0f, window.top, 0.0001f)
        assertEquals(1f, window.size, 0.0001f)
    }

    @Test
    fun aimPointAppearsWhereRequested_atOtherZoom() {
        val local = frameWindowFor(center, wanted = Offset01(0.4f, 0.7f), zoom = 3f).toLocal(center)
        assertEquals(0.4f, local.x, 0.0001f)
        assertEquals(0.7f, local.y, 0.0001f)
    }

    @Test
    fun randomWindow_followsRequestedZoom_andStaysInsideFrame() {
        for (seed in 0 until 200) {
            val window = randomFrameWindow(center, Random(seed), zoom = 3f)
            assertEquals(1f / 3f, window.size, 0.0001f)
            assertTrue(window.left >= 0f && window.left + window.size <= 1.0001f)
            assertTrue(window.top >= 0f && window.top + window.size <= 1.0001f)
        }
    }

    @Test
    fun toSourceAndToLocal_areInverse() {
        val window = FrameWindow(left = 0.1f, top = 0.3f)
        val point = Offset01(0.7f, 0.2f)
        val back = window.toLocal(window.toSource(point))
        assertEquals(point.x, back.x, 0.0001f)
        assertEquals(point.y, back.y, 0.0001f)
    }

    @Test
    fun windowNeverLeavesTheFrame() {
        // Даже если точка прицела у края кадра, окно остаётся внутри кадра.
        val window = frameWindowFor(Offset01(0.02f, 0.98f), wanted = Offset01(0.5f, 0.5f))
        assertEquals(0f, window.left, 0.0001f)
        assertEquals(1f - windowSizeFor(DEFAULT_ZOOM), window.top, 0.0001f)
    }

    @Test
    fun randomAimPosition_isAlwaysBelowCenter_andShiftedSideways() {
        for (seed in 0 until 500) {
            val aim = randomAimPosition(Random(seed))
            assertTrue("y=${aim.y}", aim.y in (0.5f + AIM_SHIFT_Y_MIN - 0.0001f)..(0.5f + AIM_SHIFT_Y_MAX + 0.0001f))
            val sideways = kotlin.math.abs(aim.x - 0.5f)
            assertTrue("x=${aim.x}", sideways in (AIM_SHIFT_X_MIN - 0.0001f)..(AIM_SHIFT_X_MAX + 0.0001f))
        }
    }

    // Окна заметно отличаются друг от друга: по обеим осям разброс не меньше пятой части кадра.
    @Test
    fun randomWindows_varyNoticeably() {
        val windows = (0 until 200).map { randomFrameWindow(center, Random(it)) }
        assertTrue(windows.maxOf { it.left } - windows.minOf { it.left } > 0.2f)
        assertTrue(windows.maxOf { it.top } - windows.minOf { it.top } > 0.15f)
    }

    @Test
    fun randomAimPosition_goesBothLeftAndRight() {
        val xs = (0 until 200).map { randomAimPosition(Random(it)).x }
        assertTrue(xs.any { it < 0.5f })
        assertTrue(xs.any { it > 0.5f })
    }

    // Защита от «невозможного» вопроса: при любом зуме и любом положении точки в кадре
    // правильная точка остаётся внутри показанного окна.
    @Test
    fun correctPointIsAlwaysInsideTheWindow_atAnyZoom() {
        val zooms = listOf(1f, 1.5f, 2f, 3f, 4f, 5f)
        val aimPoints = listOf(center, Offset01(0.05f, 0.95f), Offset01(0.98f, 0.02f), Offset01(0.3f, 0.7f))
        for (zoom in zooms) for (aim in aimPoints) for (seed in 0 until 300) {
            val window = randomFrameWindow(aim, Random(seed), zoom)
            val local = window.toLocal(aim)
            val message = "zoom=$zoom aim=$aim seed=$seed local=$local"
            assertTrue(message, local.x >= -0.0001f && local.x <= 1.0001f)
            assertTrue(message, local.y >= -0.0001f && local.y <= 1.0001f)
        }
    }

    // Для точки в центре кадра (так заполняет скрипт) точка не ближе 10 % окна к краю при любом зуме:
    // её легко найти и в неё можно попасть пальцем.
    @Test
    fun centerAimPoint_keepsSafeMarginFromWindowEdges_atAnyZoom() {
        for (zoom in listOf(1f, 1.2f, 1.5f, 2f, 3f, 4f, 5f)) for (seed in 0 until 500) {
            val local = randomFrameWindow(center, Random(seed), zoom).toLocal(center)
            val message = "zoom=$zoom seed=$seed local=$local"
            assertTrue(message, local.x in 0.1f..0.9f)
            assertTrue(message, local.y in 0.1f..0.9f)
        }
    }

    @Test
    fun randomWindow_staysInsideFrame_andKeepsAimPointVisible() {
        for (seed in 0 until 500) {
            val window = randomFrameWindow(center, Random(seed))
            assertTrue(window.left >= 0f && window.left + window.size <= 1f)
            assertTrue(window.top >= 0f && window.top + window.size <= 1f)
            val local = window.toLocal(center)
            assertTrue(local.x in 0f..1f && local.y in 0f..1f)
        }
    }

    @Test
    fun differentRandoms_giveDifferentWindows_sameSeedGivesSame() {
        assertNotEquals(randomFrameWindow(center, Random(1)), randomFrameWindow(center, Random(2)))
        assertEquals(randomFrameWindow(center, Random(7)), randomFrameWindow(center, Random(7)))
    }
}
