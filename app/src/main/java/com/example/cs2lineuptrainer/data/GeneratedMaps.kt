package com.example.cs2lineuptrainer.data

import com.example.cs2lineuptrainer.domain.Lineup
import com.example.cs2lineuptrainer.domain.MapInfo

// Этот файл создаёт скрипт tools/convert_lineups.py: список всех карт из папок content/. Вручную не править.
// Первая карта списка выбрана при запуске приложения.
object GeneratedMaps {
    val maps: List<MapInfo> = listOf(CacheData.map, MirageData.map)
    val lineups: List<Lineup> = CacheData.lineups + MirageData.lineups
}
