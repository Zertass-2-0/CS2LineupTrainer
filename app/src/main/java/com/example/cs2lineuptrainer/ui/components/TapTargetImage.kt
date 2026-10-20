package com.example.cs2lineuptrainer.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.cs2lineuptrainer.domain.FrameWindow
import com.example.cs2lineuptrainer.domain.LineupMedia
import com.example.cs2lineuptrainer.domain.Offset01
import kotlin.math.roundToInt

private val MarkerSize = 28.dp

// Показывает только часть кадра (окно, увеличение вдвое). Тап ставит маркер, повторный тап его переставляет.
// Координаты тапа и маркера - доли показанного окна (0..1), поэтому не зависят от размера экрана.
@Composable
fun TapTargetImage(
    media: LineupMedia,
    window: FrameWindow,
    marker: Offset01?,
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
            Box(
                modifier = Modifier
                    .offset(x = maxWidth * marker.x - MarkerSize / 2, y = maxHeight * marker.y - MarkerSize / 2)
                    .size(MarkerSize)
                    .background(color.copy(alpha = 0.25f), CircleShape)
                    .border(3.dp, color, CircleShape),
            )
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
