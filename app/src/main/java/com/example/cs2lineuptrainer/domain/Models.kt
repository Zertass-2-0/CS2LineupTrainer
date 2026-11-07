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

// Зажатая клавиша при броске: ничего, Shift (идти шагом) или Ctrl (присесть).
enum class ThrowModifier(val label: String) {
    NONE("Нет"),
    SHIFT("Shift"),
    CTRL("Ctrl"),
}

// Движение перед броском: стоять или идти в одну из четырёх сторон коротко (шаг) или долго (разбег).
enum class Movement(val label: String) {
    STAND("Стоя"),
    FORWARD_STEP("Шаг вперёд"),
    FORWARD_RUN("Разбег вперёд"),
    BACK_STEP("Шаг назад"),
    BACK_RUN("Разбег назад"),
    LEFT_STEP("Шаг влево"),
    LEFT_RUN("Разбег влево"),
    RIGHT_STEP("Шаг вправо"),
    RIGHT_RUN("Разбег вправо"),
}

// Какими кнопками мыши бросают.
enum class MouseButtons(val label: String) {
    LMB("ЛКМ"),
    RMB("ПКМ"),
    LMB_RMB("ЛКМ + ПКМ"),
}

// Способ броска целиком: четыре независимых выбора. Верным считается только полное совпадение всех четырёх.
// Значения по умолчанию = самый простой бросок (стоя, левая кнопка), с них начинается выбор игрока.
data class ThrowType(
    val modifier: ThrowModifier = ThrowModifier.NONE,
    val movement: Movement = Movement.STAND,
    val jump: Boolean = false,
    val buttons: MouseButtons = MouseButtons.LMB,
) {
    // Подпись вида «Ctrl + Разбег вперёд + Прыжок + ЛКМ»: пропускает то, что не выбрано.
    val label: String
        get() = listOfNotNull(
            modifier.label.takeIf { modifier != ThrowModifier.NONE },
            movement.label.takeIf { movement != Movement.STAND },
            "Прыжок".takeIf { jump },
            buttons.label,
        ).joinToString(" + ")
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
        // Запасной радиус зоны попадания в долях ширины ПОКАЗАННОГО кадра (окна). 0.01 на экране это
        // около 3.5dp, а в игре при зуме 2 только 0.005 исходного кадра (было 0.04, затем 0.02, каждый раз вдвое меньше).
        const val DEFAULT_TOLERANCE = 0.01f
    }
}
