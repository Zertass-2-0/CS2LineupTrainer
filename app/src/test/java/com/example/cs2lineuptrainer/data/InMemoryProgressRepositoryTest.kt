package com.example.cs2lineuptrainer.data

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InMemoryProgressRepositoryTest {

    @Test
    fun startsEmpty() {
        assertTrue(InMemoryProgressRepository().progress.value.isEmpty())
    }

    @Test
    fun recordAttempt_createsAndUpdatesProgress() = runBlocking {
        val repo = InMemoryProgressRepository()
        repo.recordAttempt("a", true)
        repo.recordAttempt("a", true)
        assertEquals(2, repo.progress.value["a"]?.correctStreak)
    }

    @Test
    fun recordAttempt_learnedAfterThreeHits() = runBlocking {
        val repo = InMemoryProgressRepository()
        repeat(3) { repo.recordAttempt("a", true) }
        assertEquals(true, repo.progress.value["a"]?.learned)
    }

    @Test
    fun lineupsAreTrackedIndependently() = runBlocking {
        val repo = InMemoryProgressRepository()
        repo.recordAttempt("a", true)
        assertNull(repo.progress.value["b"])
    }
}
