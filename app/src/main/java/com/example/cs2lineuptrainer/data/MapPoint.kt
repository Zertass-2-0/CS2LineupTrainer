package com.example.cs2lineuptrainer.data

import com.example.cs2lineuptrainer.domain.NamedPoint
import com.example.cs2lineuptrainer.domain.RadarCalibration
import com.example.cs2lineuptrainer.domain.worldToRadar

// Создаёт точку на радаре карты из игровых координат (getpos) по калибровке этой карты, чтобы не угадывать их вручную.
fun mapPoint(calibration: RadarCalibration, id: String, name: String, worldX: Float, worldY: Float): NamedPoint {
    val p = worldToRadar(calibration, worldX, worldY)
    return NamedPoint(id, name, p.x, p.y)
}
