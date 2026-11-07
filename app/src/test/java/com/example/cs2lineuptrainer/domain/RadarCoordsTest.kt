package com.example.cs2lineuptrainer.domain

import com.example.cs2lineuptrainer.data.CacheCalibration
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

    // Опорные точки калибровки: setpos в игре и места на радаре (в долях радара). Допуск 0.015 (30 px из 2000).
    @Test
    fun referenceSpots_landOnTheirRadarMarks() {
        val spots = listOf(
            Triple(-1742.97f, -245.94f, Offset01(0.0430f, 0.6006f)),
            Triple(1323.97f, 2018.96f, Offset01(0.6030f, 0.1992f)),
            Triple(2502.97f, -1121.45f, Offset01(0.8230f, 0.7825f)),
            Triple(579.24f, -1449.97f, Offset01(0.4785f, 0.8359f)), // sunroom_close: справа от серой полосы
        )
        for ((x, y, mark) in spots) {
            val p = worldToRadar(CacheCalibration, x, y)
            assertEquals("x для ($x, $y)", mark.x, p.x, 0.015f)
            assertEquals("y для ($x, $y)", mark.y, p.y, 0.015f)
        }
    }
}
