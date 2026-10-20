package com.example.cs2lineuptrainer.domain

// Определяет, попал ли прицел в зону. x и y нормализованы (0..1) относительно кадра.
fun interface HitMask {
    fun contains(x: Float, y: Float): Boolean
}

// Запасная маска: круг вокруг точки прицела.
// radius — доля ширины кадра; aspectRatio (ширина / высота) нужен, чтобы круг
// на экране был кругом, а не эллипсом: по вертикали нормализованная единица короче.
class CircleHitMask(
    private val center: Offset01,
    private val radius: Float,
    private val aspectRatio: Float = 16f / 9f,
) : HitMask {

    // Считаем расстояние в «долях ширины»: dy делим на aspectRatio, чтобы привести к тем же единицам, что и dx.
    override fun contains(x: Float, y: Float): Boolean {
        val dx = x - center.x
        val dy = (y - center.y) / aspectRatio
        return dx * dx + dy * dy <= radius * radius
    }
}
