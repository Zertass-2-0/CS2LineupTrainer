package com.example.cs2lineuptrainer.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class QuestionPickerTest {

    private fun lineup(id: String) = Lineup(
        id = id,
        mapId = "m",
        grenade = GrenadeType.SMOKE,
        side = Side.T,
        area = Area.A,
        title = "t",
        target = NamedPoint("t", "t", 0f, 0f),
        origin = NamedPoint("o", "o", 0f, 0f),
        standView = LineupMedia.Image(0),
        aimPoint = Offset01(0.5f, 0.5f),
        throwType = ThrowType.LMB,
        howTo = LineupMedia.Image(0),
        description = "",
    )

    private val pool = listOf("a", "b", "c", "d", "e").map(::lineup)

    @Test
    fun emptyPool_returnsEmptyList() {
        assertTrue(QuestionPicker.pick(emptyList(), emptyMap()).isEmpty())
    }

    @Test
    fun returnsRequestedCount() {
        assertEquals(10, QuestionPicker.pick(pool, emptyMap(), count = 10, random = Random(1)).size)
    }

    @Test
    fun noRepeatsWhileOthersRemain() {
        val ids = QuestionPicker.pick(pool, emptyMap(), count = 5, random = Random(2)).map { it.id }
        assertEquals(5, ids.toSet().size)
    }

    @Test
    fun neverSameLineupTwiceInRow_acrossManySeeds() {
        for (seed in 0 until 200) {
            val ids = QuestionPicker.pick(pool, emptyMap(), count = 20, random = Random(seed)).map { it.id }
            for (i in 1 until ids.size) assertNotEquals("seed=$seed i=$i", ids[i - 1], ids[i])
        }
    }

    @Test
    fun singleLineupPool_repeatsBecauseNoChoice() {
        val ids = QuestionPicker.pick(pool.take(1), emptyMap(), count = 3).map { it.id }
        assertEquals(listOf("a", "a", "a"), ids)
    }

    @Test
    fun unlearnedAppearMoreOftenThanLearned() {
        val progress = mapOf("a" to LineupProgress("a", correctStreak = 3, learned = true))
        val two = pool.take(2)
        var learnedFirst = 0
        var unlearnedFirst = 0
        for (seed in 0 until 2000) {
            val first = QuestionPicker.pick(two, progress, count = 1, random = Random(seed)).first()
            if (first.id == "a") learnedFirst++ else unlearnedFirst++
        }
        // Веса 1 : 3, значит невыученный первым выпадает примерно втрое чаще.
        assertTrue("learned=$learnedFirst unlearned=$unlearnedFirst", unlearnedFirst > learnedFirst * 2)
    }
}
