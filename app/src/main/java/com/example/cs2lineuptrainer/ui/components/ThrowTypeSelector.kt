package com.example.cs2lineuptrainer.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.example.cs2lineuptrainer.domain.Movement
import com.example.cs2lineuptrainer.domain.MouseButtons
import com.example.cs2lineuptrainer.domain.ThrowModifier
import com.example.cs2lineuptrainer.domain.ThrowType
import kotlin.math.abs
import kotlin.math.max

private val CellSize = 40.dp
private val CellGap = 4.dp
private val ModifierWidth = 76.dp
// Размер восьмиугольника выбора движения (квадрат); колонка с клавишами делается той же высоты.
private val PadSize = 216.dp
// Половина стороны центральной клетки «стоять» в долях радиуса восьмиугольника.
private const val PAD_CENTER = 0.36f
// Срез угла восьмиугольника в долях радиуса (правильный восьмиугольник: tan 22.5 градусов).
private const val OCTAGON_CUT = 0.4142f

// Выбор способа броска из четырёх независимых частей: клавиша (слева), движение (крестик),
// прыжок и кнопки мыши (внизу). Состояние хранит вызывающий экран, здесь только рисуем и сообщаем об изменениях.
@Composable
fun ThrowTypeSelector(
    selected: ThrowType,
    onChange: (ThrowType) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ModifierColumn(selected.modifier) { onChange(selected.copy(modifier = it)) }
            MovementPad(selected.movement) { onChange(selected.copy(movement = it)) }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ChoiceCell(
                text = "Прыжок",
                selected = selected.jump,
                onClick = { onChange(selected.copy(jump = !selected.jump)) },
                modifier = Modifier.weight(1f).height(64.dp),
            )
            MouseToggle(
                buttons = selected.buttons,
                onClick = { onChange(selected.copy(buttons = selected.buttons.next())) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

// Три положения сверху вниз: Ctrl, Shift, ничего.
@Composable
private fun ModifierColumn(selected: ThrowModifier, onSelect: (ThrowModifier) -> Unit) {
    Column(
        modifier = Modifier.width(ModifierWidth).height(PadSize),
        verticalArrangement = Arrangement.spacedBy(CellGap, Alignment.CenterVertically),
    ) {
        listOf(ThrowModifier.CTRL, ThrowModifier.SHIFT, ThrowModifier.NONE).forEach { option ->
            ChoiceCell(
                text = option.label,
                selected = option == selected,
                onClick = { onSelect(option) },
                modifier = Modifier.fillMaxWidth().height(CellSize),
            )
        }
    }
}

// Восьмиугольник движения: в центре «стоять», в четыре стороны по два шеврона:
// ближний к центру шаг, дальний разбег. Тап определяет зону через movementAt, рисуем на Canvas.
@Composable
private fun MovementPad(selected: Movement, onSelect: (Movement) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val background = colors.surfaceVariant
    val line = colors.outline
    val idle = colors.onSurfaceVariant.copy(alpha = 0.55f)
    val active = colors.primary
    val highlight = colors.primaryContainer

    Canvas(
        modifier = Modifier
            .size(PadSize)
            .pointerInput(selected, onSelect) {
                detectTapGestures { tap ->
                    val r = size.width / 2f
                    sectorAt((tap.x - r) / r, (tap.y - r) / r)?.let { onSelect(nextMovement(selected, it)) }
                }
            },
    ) {
        val r = size.width / 2f
        val c = Offset(r, r)
        val s = r * PAD_CENTER
        val cut = r * OCTAGON_CUT
        val thin = 2.dp.toPx()

        // Корпус: восьмиугольник с заливкой и контуром.
        val octagon = Path().apply {
            moveTo(c.x - cut, 0f)
            lineTo(c.x + cut, 0f)
            lineTo(size.width, c.y - cut)
            lineTo(size.width, c.y + cut)
            lineTo(c.x + cut, size.height)
            lineTo(c.x - cut, size.height)
            lineTo(0f, c.y + cut)
            lineTo(0f, c.y - cut)
            close()
        }
        drawPath(octagon, background)
        drawPath(octagon, line, style = Stroke(thin))

        // Центральная клетка и четыре диагональные перегородки от её углов к серединам косых сторон.
        drawRect(line, Offset(c.x - s, c.y - s), Size(2 * s, 2 * s), style = Stroke(thin))
        val diagonal = r * (cut / r + 1f) / 2f
        for (sx in listOf(-1f, 1f)) for (sy in listOf(-1f, 1f)) {
            drawLine(line, Offset(c.x + sx * s, c.y + sy * s), Offset(c.x + sx * diagonal, c.y + sy * diagonal), thin)
        }

        // X в центре.
        val arm = s * 0.5f
        val xColor = if (selected == Movement.STAND) active else idle
        val xWidth = 7.dp.toPx()
        drawLine(xColor, Offset(c.x - arm, c.y - arm), Offset(c.x + arm, c.y + arm), xWidth, StrokeCap.Round)
        drawLine(xColor, Offset(c.x - arm, c.y + arm), Offset(c.x + arm, c.y - arm), xWidth, StrokeCap.Round)

        // Секторы и шевроны: рисуем «вверх» и поворачиваем холст на 0, 90, 180, 270 градусов.
        // Шаг подсвечивает один (ближний) шеврон, разбег оба; выбранный сектор ещё и залит.
        val mid = (s + r) / 2f
        val stepDistance = (s + mid) / 2f
        val runDistance = (mid + r) / 2f
        val sector = Path().apply {
            moveTo(c.x - s, c.y - s)
            lineTo(c.x + s, c.y - s)
            lineTo(c.x + diagonal, c.y - diagonal)
            lineTo(c.x + cut, 0f)
            lineTo(c.x - cut, 0f)
            lineTo(c.x - diagonal, c.y - diagonal)
            close()
        }
        val sides = listOf(
            Triple(0f, Movement.FORWARD_STEP, Movement.FORWARD_RUN),
            Triple(90f, Movement.RIGHT_STEP, Movement.RIGHT_RUN),
            Triple(180f, Movement.BACK_STEP, Movement.BACK_RUN),
            Triple(270f, Movement.LEFT_STEP, Movement.LEFT_RUN),
        )
        for ((degrees, step, run) in sides) {
            val stepLit = selected == step || selected == run
            val runLit = selected == run
            rotate(degrees, pivot = c) {
                if (stepLit) drawPath(sector, highlight)
                drawChevron(c, stepDistance, r, if (stepLit) active else idle, stepLit)
                drawChevron(c, runDistance, r, if (runLit) active else idle, runLit)
            }
        }
    }
}

// Один шеврон «вверх» на расстоянии distance от центра; выбранный толще.
private fun DrawScope.drawChevron(center: Offset, distance: Float, radius: Float, color: Color, selected: Boolean) {
    val halfWidth = radius * 0.2f
    val halfHeight = radius * 0.08f
    val y = center.y - distance
    val path = Path().apply {
        moveTo(center.x - halfWidth, y + halfHeight)
        lineTo(center.x, y - halfHeight)
        lineTo(center.x + halfWidth, y + halfHeight)
    }
    val width = radius * if (selected) 0.1f else 0.06f
    drawPath(path, color, style = Stroke(width, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

// Какой сектор задет тапом. x и y: смещение от центра восьмиугольника в долях его радиуса (-1..1).
// Вне восьмиугольника null. Центральный квадрат это «стоять», дальше сектор определяется ближайшей стороной
// (диагональ делит пополам). Для сектора возвращается его «шаг»: переключение шаг/разбег делает nextMovement.
internal fun sectorAt(x: Float, y: Float): Movement? {
    val ax = abs(x)
    val ay = abs(y)
    if (ax > 1f || ay > 1f || ax + ay > 1f + OCTAGON_CUT) return null
    if (ax <= PAD_CENTER && ay <= PAD_CENTER) return Movement.STAND
    return if (ay >= ax) {
        if (y < 0) Movement.FORWARD_STEP else Movement.BACK_STEP
    } else {
        if (x < 0) Movement.LEFT_STEP else Movement.RIGHT_STEP
    }
}

// Что выбрано после тапа по сектору: центр это «стоять». Тап по другому сектору включает в нём шаг (одна стрелка),
// повторные тапы по тому же сектору переключают шаг и разбег (одна и две стрелки).
internal fun nextMovement(current: Movement, tapped: Movement): Movement {
    val run = when (tapped) {
        Movement.FORWARD_STEP -> Movement.FORWARD_RUN
        Movement.BACK_STEP -> Movement.BACK_RUN
        Movement.LEFT_STEP -> Movement.LEFT_RUN
        Movement.RIGHT_STEP -> Movement.RIGHT_RUN
        else -> return tapped
    }
    return if (current == tapped) run else tapped
}

// Клетка выбора: выбранная подсвечена. Используется для клавиши, движения и прыжка.
@Composable
private fun ChoiceCell(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(if (selected) colors.primaryContainer else colors.surfaceVariant)
            .border(1.dp, if (selected) colors.primary else colors.outlineVariant, shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) colors.onPrimaryContainer else colors.onSurfaceVariant,
        )
    }
}

// Мышь: тап переключает ЛКМ -> ПКМ -> ЛКМ + ПКМ. На рисунке закрашены нажатые кнопки.
@Composable
private fun MouseToggle(buttons: MouseButtons, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(8.dp)
    val outline = colors.onSurfaceVariant
    val fill = colors.primary
    Row(
        modifier = modifier
            .clip(shape)
            .border(1.dp, colors.outlineVariant, shape)
            .clickable(onClick = onClick)
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(modifier = Modifier.size(width = 32.dp, height = 48.dp)) {
            val buttonsHeight = size.height * 0.45f
            val half = size.width / 2
            val body = Path().apply {
                addRoundRect(RoundRect(0f, 0f, size.width, size.height, CornerRadius(half)))
            }
            clipPath(body) {
                if (buttons != MouseButtons.RMB) drawRect(fill, Offset.Zero, Size(half, buttonsHeight))
                if (buttons != MouseButtons.LMB) drawRect(fill, Offset(half, 0f), Size(half, buttonsHeight))
            }
            val line = 2.dp.toPx()
            drawRoundRect(outline, size = size, cornerRadius = CornerRadius(half), style = Stroke(line))
            drawLine(outline, Offset(0f, buttonsHeight), Offset(size.width, buttonsHeight), line)
            drawLine(outline, Offset(half, 0f), Offset(half, buttonsHeight), line)
        }
        Text(buttons.label, style = MaterialTheme.typography.labelLarge)
    }
}

// Следующий режим по кругу: ЛКМ -> ПКМ -> ЛКМ + ПКМ -> снова ЛКМ.
private fun MouseButtons.next(): MouseButtons = MouseButtons.entries[(ordinal + 1) % MouseButtons.entries.size]
