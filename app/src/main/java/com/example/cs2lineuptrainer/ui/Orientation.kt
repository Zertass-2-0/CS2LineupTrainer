package com.example.cs2lineuptrainer.ui

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration

// true, если телефон повёрнут горизонтально. Экраны используют это, чтобы выбрать раскладку.
@Composable
fun isLandscape(): Boolean = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
