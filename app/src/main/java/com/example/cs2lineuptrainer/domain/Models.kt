package com.example.cs2lineuptrainer.domain

import androidx.annotation.DrawableRes

enum class GrenadeType(val label: String) {
    SMOKE("Smoke"),
    FLASH("Flash"),
    MOLOTOV("Molotov"),
    HE("HE"),
}

enum class Side { T, CT }

// Область карты, к которой относится цель лайнапа.
enum class Area(val label: String) {
    A("A"),
    B("B"),
    MID("Мид"),
}

enum class ThrowType(val label: String) {
    LMB("ЛКМ"),
    RMB("ПКМ"),
    LMB_RMB("ЛКМ + ПКМ"),
    JUMP_THROW("Jumpthrow"),
    RUN_JUMP_THROW("Run + jumpthrow"),
    W_LMB("W + ЛКМ"),
}

// Точка в нормализованных координатах: x и y от 0f до 1f, не зависят от размера экрана.
data class Offset01(val x: Float, val y: Float)

// Карта и её радар (изображение сверху).
data class MapInfo(val id: String, val name: String, @param:DrawableRes val radar: Int)

// Именованная точка на радаре: цель или позиция броска.
data class NamedPoint(val id: String, val name: String, val x: Float, val y: Float)

// Медиа лайнапа: неподвижная картинка или GIF. Sealed, чтобы можно было добавить другие виды.
sealed interface LineupMedia {
    data class Image(@param:DrawableRes val res: Int) : LineupMedia
    data class Gif(@param:DrawableRes val res: Int) : LineupMedia
}

// Один лайнап: откуда, куда, чем и как бросать.
data class Lineup(
    val id: String,
    val mapId: String,
    val grenade: GrenadeType,
    val side: Side,
    val area: Area,
    val title: String,
    val target: NamedPoint,
    val origin: NamedPoint,
    val standView: LineupMedia,
    val aimPoint: Offset01,
    val tolerance: Float = DEFAULT_TOLERANCE,
    val zoom: Float = DEFAULT_ZOOM,   // во сколько раз увеличивается кадр в тренировке
    @param:DrawableRes val hitMask: Int? = null,
    val throwType: ThrowType,
    val howTo: LineupMedia,
    val description: String,
) {
    companion object {
        // Запасной радиус зоны попадания в долях ширины ПОКАЗАННОГО кадра (окна). 0.02 на экране это
        // около 7dp, а в игре при зуме 2 только 0.01 исходного кадра (было 0.04, уменьшено вдвое).
        const val DEFAULT_TOLERANCE = 0.02f
    }
}
