package com.example.cs2lineuptrainer

import android.app.Application
import android.os.Build
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.gif.AnimatedImageDecoder
import coil3.gif.GifDecoder

// Создаётся один раз при старте процесса, поэтому AppContainer живёт, пока жив процесс.
class CsApplication : Application(), SingletonImageLoader.Factory {
    val container = AppContainer()

    // Учит Coil показывать анимированные GIF (на новых Android - системным декодером, на старых - своим).
    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .components {
                if (Build.VERSION.SDK_INT >= 28) {
                    add(AnimatedImageDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .build()
}
