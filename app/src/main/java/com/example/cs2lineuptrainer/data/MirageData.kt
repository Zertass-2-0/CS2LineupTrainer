package com.example.cs2lineuptrainer.data

import com.example.cs2lineuptrainer.R
import com.example.cs2lineuptrainer.domain.Lineup
import com.example.cs2lineuptrainer.domain.MapInfo
import com.example.cs2lineuptrainer.domain.RadarCalibration

val MirageCalibration = RadarCalibration(posX = -1999f, posY = 3090.9f, scale = 5.310f)

// Данные по Cache. Сами лайнапы создаёт скрипт tools/convert_lineups.py (результат лежит в CacheGenerated.kt).
// У каждой карты свой такой файл: <Карта>Data.kt с калибровкой радара, MapInfo и списком лайнапов.
object MirageData {
    const val MAP_ID = "mirage"

    val map = MapInfo(id = MAP_ID, name = "Mirage", radar = R.drawable.mirage_radar)

    val lineups: List<Lineup> = MirageGenerated.lineups
}
