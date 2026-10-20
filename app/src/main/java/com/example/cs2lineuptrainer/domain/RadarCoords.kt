package com.example.cs2lineuptrainer.domain

// Параметры радара из файла карты (de_cache.txt): мировые координаты левого верхнего угла и масштаб.
data class RadarCalibration(
    val posX: Float,
    val posY: Float,
    val scale: Float,
    val sizePx: Float = 1024f,
)

val CacheCalibration = RadarCalibration(posX = -2000f, posY = 3250f, scale = 5.5f)

// Переводит игровые координаты (setpos) в нормализованные координаты на радаре (0..1).
// Ось Y инвертирована: в игре Y растёт вверх, на картинке — вниз.
fun worldToRadar(calibration: RadarCalibration, worldX: Float, worldY: Float): Offset01 =
    Offset01(
        x = (worldX - calibration.posX) / calibration.scale / calibration.sizePx,
        y = (calibration.posY - worldY) / calibration.scale / calibration.sizePx,
    )
