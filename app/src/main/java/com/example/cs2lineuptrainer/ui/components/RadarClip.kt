package com.example.cs2lineuptrainer.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.ImageBitmap
import com.example.cs2lineuptrainer.domain.NamedPoint
import com.example.cs2lineuptrainer.ui.theme.PositionColor
import kotlin.math.roundToInt

// Увеличенный кусок радара вокруг точки: показывает, откуда нужно бросать. Точка всегда в центре.
// zoom = во сколько раз увеличен радар (3 = виден примерно треть карты по ширине).
@Composable
fun RadarClip(
    @DrawableRes radar: Int,
    center: NamedPoint,
    modifier: Modifier = Modifier,
    zoom: Float = 3f,
) {
    val bitmap: ImageBitmap = ImageBitmap.imageResource(radar)

    Canvas(modifier = modifier.fillMaxWidth().aspectRatio(2f)) {
        // Сторона увеличенного радара и положение его левого верхнего угла так,
        // чтобы нужная точка радара оказалась в центре рамки.
        val side = size.width * zoom
        val left = size.width / 2 - center.x * side
        val top = size.height / 2 - center.y * side

        clipRect {
            drawImage(
                image = bitmap,
                srcSize = IntSize(bitmap.width, bitmap.height),
                dstOffset = IntOffset(left.roundToInt(), top.roundToInt()),
                dstSize = IntSize(side.roundToInt(), side.roundToInt()),
                filterQuality = FilterQuality.Medium,
            )
        }

        val middle = Offset(size.width / 2, size.height / 2)
        drawCircle(PositionColor, radius = 8.dp.toPx(), center = middle)
        drawCircle(Color.White, radius = 8.dp.toPx(), center = middle, style = Stroke(width = 1.5.dp.toPx()))
    }
}
