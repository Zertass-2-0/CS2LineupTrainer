package com.example.cs2lineuptrainer.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// Проверки списка карт, который создаёт скрипт: ловят ошибки при добавлении новых карт.
class GeneratedMapsTest {

    @Test
    fun atLeastOneMap() {
        assertTrue(GeneratedMaps.maps.isNotEmpty())
    }

    @Test
    fun mapIds_areUnique() {
        val ids = GeneratedMaps.maps.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun lineupIds_areUniqueAcrossAllMaps() {
        val ids = GeneratedMaps.lineups.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun everyLineup_belongsToARegisteredMap() {
        val mapIds = GeneratedMaps.maps.map { it.id }.toSet()
        GeneratedMaps.lineups.forEach { assertTrue("${it.id} mapId=${it.mapId}", it.mapId in mapIds) }
    }
}
