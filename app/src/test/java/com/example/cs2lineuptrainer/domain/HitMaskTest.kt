package com.example.cs2lineuptrainer.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HitMaskTest {

    // Подделка без Android: зона попадания - прямоугольник. Показывает, что логика зависит только от интерфейса.
    private val fakeMask = HitMask { x, y -> x in 0.4f..0.6f && y in 0.4f..0.6f }

    @Test
    fun fakeMask_insideAndOutside() {
        assertTrue(fakeMask.contains(0.5f, 0.5f))
        assertFalse(fakeMask.contains(0.9f, 0.1f))
    }

    @Test
    fun circle_centerIsHit() {
        val mask = CircleHitMask(Offset01(0.5f, 0.5f), radius = 0.02f)
        assertTrue(mask.contains(0.5f, 0.5f))
    }

    @Test
    fun circle_insideAndOutsideHorizontally() {
        val mask = CircleHitMask(Offset01(0.5f, 0.5f), radius = 0.02f)
        assertTrue(mask.contains(0.51f, 0.5f))
        assertFalse(mask.contains(0.53f, 0.5f))
    }

    @Test
    fun circle_isRoundOnScreen_notEllipse() {
        val mask = CircleHitMask(Offset01(0.5f, 0.5f), radius = 0.02f, aspectRatio = 2f)
        // По вертикали допустимо в 2 раза больше нормализованных единиц, чем по горизонтали.
        assertTrue(mask.contains(0.5f, 0.5f + 0.035f))
        assertFalse(mask.contains(0.5f + 0.035f, 0.5f))
    }

    @Test
    fun circle_onBoundaryCountsAsHit() {
        // 0.5, 0.75 и 0.25 точно представимы в float, поэтому граница считается без погрешности.
        val mask = CircleHitMask(Offset01(0.5f, 0.5f), radius = 0.25f)
        assertEquals(true, mask.contains(0.75f, 0.5f))
    }
}
