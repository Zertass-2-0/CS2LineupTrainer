package com.example.cs2lineuptrainer.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import coil3.compose.AsyncImage
import com.example.cs2lineuptrainer.domain.LineupMedia

// Показывает кадр лайнапа (формат 16:9): обычную картинку или анимированный GIF.
@Composable
fun LineupMediaView(media: LineupMedia, modifier: Modifier = Modifier) {
    val frameModifier = modifier
        .fillMaxWidth()
        .aspectRatio(16f / 9f)

    when (media) {
        is LineupMedia.Image -> Image(
            painter = painterResource(media.res),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = frameModifier,
        )

        is LineupMedia.Gif -> AsyncImage(
            model = media.res,
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = frameModifier,
        )
    }
}
