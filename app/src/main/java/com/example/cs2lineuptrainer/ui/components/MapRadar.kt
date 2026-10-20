package com.example.cs2lineuptrainer.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

// Точка на радаре. x и y - нормализованные (0..1), onClick вызывается при тапе по точке.
data class RadarPoint(
    val id: String,
    val x: Float,
    val y: Float,
    val color: Color,
    val alpha: Float = 1f,
    val onClick: () -> Unit,
)

// Зона касания точки больше самой точки: палец крупнее, чем кружок на карте.
private val TouchSize = 44.dp
private val DotSize = 16.dp

// Квадратный радар карты с точками поверх. Тап мимо точек вызывает onEmptyClick.
// Размер задаёт вызывающий экран (например, fillMaxWidth() в портрете или size(...) в альбоме), форма всегда квадратная.
@Composable
fun MapRadar(
    @DrawableRes radar: Int,
    points: List<RadarPoint>,
    onEmptyClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier
            .aspectRatio(1f)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onEmptyClick,
            ),
    ) {
        Image(
            painter = painterResource(radar),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.fillMaxSize(),
        )
        // Позиция точки = доля размера радара; вычитаем половину зоны касания, чтобы центр совпал с координатой.
        points.forEach { point ->
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .offset(x = maxWidth * point.x - TouchSize / 2, y = maxHeight * point.y - TouchSize / 2)
                    .size(TouchSize)
                    .clip(CircleShape)
                    .clickable(onClick = point.onClick),
            ) {
                Box(
                    modifier = Modifier
                        .size(DotSize)
                        .alpha(point.alpha)
                        .background(point.color, CircleShape)
                        .border(1.5.dp, Color.White, CircleShape),
                )
            }
        }
    }
}
