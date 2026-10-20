package com.example.cs2lineuptrainer.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LineupProgressTest {

    private val fresh = LineupProgress("a")
    private val learned = LineupProgress("a", correctStreak = 3, learned = true)

    // Применяет серию попыток подряд.
    private fun LineupProgress.after(vararg hits: Boolean) =
        hits.fold(this) { p, hit -> p.afterAttempt(hit) }

    @Test
    fun hit_increasesCorrectStreak() {
        val p = fresh.afterAttempt(true)
        assertEquals(1, p.correctStreak)
        assertFalse(p.learned)
    }

    @Test
    fun threeHitsInRow_makeLearned() {
        assertTrue(fresh.after(true, true, true).learned)
    }

    @Test
    fun twoHits_notLearnedYet() {
        assertFalse(fresh.after(true, true).learned)
    }

    @Test
    fun missBeforeLearned_resetsCorrectStreak() {
        val p = fresh.after(true, true, false)
        assertEquals(0, p.correctStreak)
        assertFalse(p.learned)
    }

    @Test
    fun missBeforeLearned_needsThreeHitsAgain() {
        val p = fresh.after(true, true, false, true, true)
        assertFalse(p.learned)
        assertTrue(p.afterAttempt(true).learned)
    }

    @Test
    fun singleMissOnLearned_keepsLearned() {
        val p = learned.afterAttempt(false)
        assertTrue(p.learned)
        assertEquals(1, p.missStreak)
    }

    @Test
    fun twoMissesInRowOnLearned_removeLearned() {
        val p = learned.after(false, false)
        assertFalse(p.learned)
        assertEquals(0, p.correctStreak)
        assertEquals(0, p.missStreak)
    }

    @Test
    fun hitOnLearned_resetsMissStreak() {
        val p = learned.after(false, true)
        assertTrue(p.learned)
        assertEquals(0, p.missStreak)
    }

    @Test
    fun missesSeparatedByHit_doNotRemoveLearned() {
        assertTrue(learned.after(false, true, false).learned)
    }

    @Test
    fun afterForgetting_needsThreeHitsToLearnAgain() {
        val forgotten = learned.after(false, false)
        assertFalse(forgotten.after(true, true).learned)
        assertTrue(forgotten.after(true, true, true).learned)
    }
}
