package com.example.cs2lineuptrainer.ui.components

import com.example.cs2lineuptrainer.domain.Movement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MovementPadTest {

    @Test
    fun center_isStand() {
        assertEquals(Movement.STAND, sectorAt(0f, 0f))
        assertEquals(Movement.STAND, sectorAt(0.3f, -0.3f))
    }

    @Test
    fun sectorIsTheSameNearAndFarFromCenter() {
        assertEquals(Movement.FORWARD_STEP, sectorAt(0f, -0.5f))
        assertEquals(Movement.FORWARD_STEP, sectorAt(0f, -0.9f))
    }

    @Test
    fun allFourSectors() {
        assertEquals(Movement.BACK_STEP, sectorAt(0f, 0.5f))
        assertEquals(Movement.LEFT_STEP, sectorAt(-0.85f, 0f))
        assertEquals(Movement.RIGHT_STEP, sectorAt(0.5f, 0.1f))
    }

    @Test
    fun diagonalIsSplitByNearestSide() {
        assertEquals(Movement.FORWARD_STEP, sectorAt(0.5f, -0.55f))
        assertEquals(Movement.RIGHT_STEP, sectorAt(0.55f, -0.5f))
    }

    @Test
    fun outsideOctagon_isNull() {
        assertNull(sectorAt(1.2f, 0f))
        assertNull(sectorAt(0.95f, 0.95f)) // срезанный угол
    }

    @Test
    fun tapOnSector_togglesStepAndRun() {
        val step = Movement.FORWARD_STEP
        assertEquals(step, nextMovement(Movement.STAND, step))
        assertEquals(Movement.FORWARD_RUN, nextMovement(step, step))
        assertEquals(step, nextMovement(Movement.FORWARD_RUN, step))
    }

    @Test
    fun tapOnOtherSector_startsWithStep() {
        assertEquals(Movement.LEFT_STEP, nextMovement(Movement.FORWARD_RUN, Movement.LEFT_STEP))
    }

    @Test
    fun tapOnCenter_isStand() {
        assertEquals(Movement.STAND, nextMovement(Movement.RIGHT_RUN, Movement.STAND))
    }
}
