package com.example.cs2lineuptrainer.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ThrowTypeTest {

    @Test
    fun default_isStandingLeftClick() {
        assertEquals("ЛКМ", ThrowType().label)
    }

    @Test
    fun label_skipsUnselectedParts() {
        assertEquals("Прыжок + ЛКМ", ThrowType(jump = true).label)
    }

    @Test
    fun label_listsAllPartsInOrder() {
        val full = ThrowType(ThrowModifier.CTRL, Movement.FORWARD_RUN, jump = true, buttons = MouseButtons.LMB_RMB)
        assertEquals("Ctrl + Разбег вперёд + Прыжок + ЛКМ + ПКМ", full.label)
    }

    @Test
    fun equality_requiresAllPartsToMatch() {
        assertEquals(ThrowType(jump = true), ThrowType(jump = true))
        assertNotEquals(ThrowType(movement = Movement.FORWARD_STEP), ThrowType(movement = Movement.FORWARD_RUN))
    }
}
