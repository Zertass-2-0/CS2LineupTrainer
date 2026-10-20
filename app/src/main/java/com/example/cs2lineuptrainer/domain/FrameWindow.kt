package com.example.cs2lineuptrainer.domain

import kotlin.random.Random

// Во сколько раз увеличивается кадр в вопросе по умолчанию (2 = показывается половина кадра по каждой стороне).
// Для каждого лайнапа можно задать своё значение (поле zoom).
const val DEFAULT_ZOOM = 2f
const val MIN_ZOOM = 1f
const val MAX_ZOOM = 5f

// На сколько (в долях окна) точка прицела отодвигается от центра окна: от MIN до MAX.
// По горизонтали - в любую сторону, по вертикали - только вниз (см. randomAimPosition).
// Максимум 0.38 оставляет точку не ближе 12 % окна от края, чтобы по ней можно было попасть пальцем.
const val AIM_SHIFT_X_MIN = 0.10f
const val AIM_SHIFT_X_MAX = 0.38f
const val AIM_SHIFT_Y_MIN = 0.05f
const val AIM_SHIFT_Y_MAX = 0.38f

// Размер окна в долях целого кадра для данного увеличения: зум 2 -> 0.5, зум 4 -> 0.25.
fun windowSizeFor(zoom: Float): Float = 1f / zoom.coerceIn(MIN_ZOOM, MAX_ZOOM)

// Часть целого кадра, которая показывается в вопросе: положение левого верхнего угла и размер (всё в долях целого кадра).
data class FrameWindow(val left: Float, val top: Float, val size: Float = windowSizeFor(DEFAULT_ZOOM)) {

    // Точка целого кадра -> точка в окне (0..1 внутри окна).
    fun toLocal(point: Offset01) = Offset01((point.x - left) / size, (point.y - top) / size)

    // Точка в окне (0..1 внутри окна) -> точка целого кадра.
    fun toSource(local: Offset01) = Offset01(left + local.x * size, top + local.y * size)
}

// Окно заданного зума, в котором точка прицела (aimPoint, в долях целого кадра) оказывается в позиции wanted (в долях окна).
// Окно не выходит за границы кадра, поэтому у краёв (и при малом зуме) точка может сместиться.
fun frameWindowFor(aimPoint: Offset01, wanted: Offset01, zoom: Float = DEFAULT_ZOOM): FrameWindow {
    val size = windowSizeFor(zoom)
    val maxStart = 1f - size
    return FrameWindow(
        left = (aimPoint.x - wanted.x * size).coerceIn(0f, maxStart),
        top = (aimPoint.y - wanted.y * size).coerceIn(0f, maxStart),
        size = size,
    )
}

// Случайная позиция точки прицела в окне: в сторону по горизонтали (любую) и вниз от центра.
// Вниз, потому что граната в руке находится в правом нижнем углу исходного кадра: окно уходит вверх и не захватывает её.
fun randomAimPosition(random: Random): Offset01 {
    val shiftX = AIM_SHIFT_X_MIN + random.nextFloat() * (AIM_SHIFT_X_MAX - AIM_SHIFT_X_MIN)
    val shiftY = AIM_SHIFT_Y_MIN + random.nextFloat() * (AIM_SHIFT_Y_MAX - AIM_SHIFT_Y_MIN)
    val x = if (random.nextBoolean()) 0.5f + shiftX else 0.5f - shiftX
    return Offset01(x, 0.5f + shiftY)
}

fun randomFrameWindow(aimPoint: Offset01, random: Random, zoom: Float = DEFAULT_ZOOM): FrameWindow =
    frameWindowFor(aimPoint, randomAimPosition(random), zoom)
