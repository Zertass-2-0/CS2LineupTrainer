package com.example.cs2lineuptrainer.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.cs2lineuptrainer.domain.FrameWindow
import com.example.cs2lineuptrainer.domain.LineupMedia
import com.example.cs2lineuptrainer.domain.Offset01
import kotlin.math.roundToInt

// Показывает только часть кадра (окно, увеличение вдвое). Тап ставит маркер, повторный тап его переставляет.
// Координаты тапа и маркера - доли показанного окна (0..1), поэтому не зависят от размера экрана.
// Круг маркера честный: его радиус равен радиусу зоны попадания (tolerance - доля ширины показанного окна),
// то есть ответ верен ровно тогда, когда правильная точка внутри круга. Тонкий крест показывает центр.
@Composable
fun TapTargetImage(
    media: LineupMedia,
    window: FrameWindow,
    marker: Offset01?,
    tolerance: Float,
    onTap: (Offset01) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .pointerInput(onTap) {
                detectTapGestures { tap ->
                    onTap(Offset01(tap.x / size.width, tap.y / size.height))
                }
            },
    ) {
        when (media) {
            is LineupMedia.Image -> WindowedImage(media.res, window)
            // GIF как кадр вопроса не предполагается: показываем целиком.
            is LineupMedia.Gif -> LineupMediaView(media, Modifier.fillMaxSize())
        }

        if (marker != null) {
            val color = MaterialTheme.colorScheme.primary
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(marker.x * size.width, marker.y * size.height)
                val radius = tolerance * size.width
                val arm = radius + 10.dp.toPx()
                drawCircle(color.copy(alpha = 0.25f), radius, center)
                drawCircle(color, radius, center, style = Stroke(1.5.dp.toPx()))
                drawLine(color, Offset(center.x - arm, center.y), Offset(center.x + arm, center.y), 1.dp.toPx())
                drawLine(color, Offset(center.x, center.y - arm), Offset(center.x, center.y + arm), 1.dp.toPx())
            }
        }
    }
}

// Рисует на всю область только вырезанную часть картинки.
@Composable
private fun WindowedImage(res: Int, window: FrameWindow) {
    val bitmap: ImageBitmap = ImageBitmap.imageResource(res)
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawImage(
            image = bitmap,
            srcOffset = IntOffset((window.left * bitmap.width).roundToInt(), (window.top * bitmap.height).roundToInt()),
            srcSize = IntSize((window.size * bitmap.width).roundToInt(), (window.size * bitmap.height).roundToInt()),
            dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
            filterQuality = FilterQuality.Medium,
        )
    }
}
