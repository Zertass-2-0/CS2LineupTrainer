package com.example.cs2lineuptrainer.data

import com.example.cs2lineuptrainer.R
import com.example.cs2lineuptrainer.domain.Lineup
import com.example.cs2lineuptrainer.domain.MapInfo

// Данные по Cache. Сами лайнапы создаёт скрипт tools/convert_lineups.py (результат лежит в CacheGenerated.kt).
object CacheData {
    const val MAP_ID = "cache"

    val map = MapInfo(id = MAP_ID, name = "Cache", radar = R.drawable.cache_radar)

    val lineups: List<Lineup> = CacheGenerated.lineups
}
