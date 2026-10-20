package com.example.cs2lineuptrainer.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class RadarCoordsTest {

    @Test
    fun topLeftCorner_isZeroZero() {
        val p = worldToRadar(CacheCalibration, CacheCalibration.posX, CacheCalibration.posY)
        assertEquals(0f, p.x, 0.0001f)
        assertEquals(0f, p.y, 0.0001f)
    }

    @Test
    fun bottomRightCorner_isOneOne() {
        val c = CacheCalibration
        val p = worldToRadar(c, c.posX + c.scale * c.sizePx, c.posY - c.scale * c.sizePx)
        assertEquals(1f, p.x, 0.0001f)
        assertEquals(1f, p.y, 0.0001f)
    }

    @Test
    fun yAxisIsInverted_higherWorldYIsHigherOnRadar() {
        val low = worldToRadar(CacheCalibration, 0f, 0f)
        val high = worldToRadar(CacheCalibration, 0f, 1000f)
        assertEquals(true, high.y < low.y)
    }
}
