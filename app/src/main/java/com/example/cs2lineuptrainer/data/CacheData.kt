package com.example.cs2lineuptrainer.data

import com.example.cs2lineuptrainer.R
import com.example.cs2lineuptrainer.domain.Lineup
import com.example.cs2lineuptrainer.domain.MapInfo
import com.example.cs2lineuptrainer.domain.RadarCalibration

// Подобрано методом наименьших квадратов по четырём опорным точкам (setpos в игре и места на радаре приложения):
// три красные метки карты и позиция sunroom_close, которую пользователь указал вручную (вес выше).
// Значения из de_cache.txt (posX = -2000, posY = 3250, scale = 5.5) давали сдвиг до 45 px на радаре 2000 px.
val CacheCalibration = RadarCalibration(posX = -1999f, posY = 3090.9f, scale = 5.310f)

// Данные по Cache. Сами лайнапы создаёт скрипт tools/convert_lineups.py (результат лежит в CacheGenerated.kt).
// У каждой карты свой такой файл: <Карта>Data.kt с калибровкой радара, MapInfo и списком лайнапов.
object CacheData {
    const val MAP_ID = "cache"

    val map = MapInfo(id = MAP_ID, name = "Cache", radar = R.drawable.cache_radar)

    val lineups: List<Lineup> = CacheGenerated.lineups
}
