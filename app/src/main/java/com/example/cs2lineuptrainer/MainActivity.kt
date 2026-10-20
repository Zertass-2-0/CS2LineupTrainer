package com.example.cs2lineuptrainer

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.cs2lineuptrainer.ui.AppNavigation
import com.example.cs2lineuptrainer.ui.theme.CS2LineupTrainerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Тёмные системные панели: приложение всегда тёмное, светлые значки были бы не видны.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        setContent {
            CS2LineupTrainerTheme {
                AppNavigation()
            }
        }
    }
}
