package com.example.cs2lineuptrainer.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// Строка прогресса: подпись, «X из Y» и полоса. Используется и для всей карты, и для каждого типа гранат.
@Composable
fun LearnedProgress(
    label: String,
    learned: Int,
    total: Int,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text("$learned из $total", style = MaterialTheme.typography.bodyLarge)
        }
        LinearProgressIndicator(
            progress = { if (total == 0) 0f else learned.toFloat() / total },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
