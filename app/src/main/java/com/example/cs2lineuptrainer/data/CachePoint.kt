package com.example.cs2lineuptrainer.data

import com.example.cs2lineuptrainer.domain.CacheCalibration
import com.example.cs2lineuptrainer.domain.NamedPoint
import com.example.cs2lineuptrainer.domain.worldToRadar

// Создаёт точку на радаре Cache из игровых координат (getpos), чтобы не угадывать их вручную.
fun cachePoint(id: String, name: String, worldX: Float, worldY: Float): NamedPoint {
    val p = worldToRadar(CacheCalibration, worldX, worldY)
    return NamedPoint(id, name, p.x, p.y)
}
