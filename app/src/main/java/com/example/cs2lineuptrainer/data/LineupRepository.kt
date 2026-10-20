package com.example.cs2lineuptrainer.data

import com.example.cs2lineuptrainer.domain.Lineup
import com.example.cs2lineuptrainer.domain.MapInfo

// Отдаёт карты и лайнапы. Данные статические (Kotlin-списки), без сети и БД.
// Списки можно подменить в конструкторе, поэтому тесты не зависят от настоящего контента.
class LineupRepository(
    private val maps: List<MapInfo> = listOf(CacheData.map),
    private val lineups: List<Lineup> = CacheData.lineups,
) {
    fun getMaps(): List<MapInfo> = maps

    fun getLineups(mapId: String): List<Lineup> = lineups.filter { it.mapId == mapId }
}
