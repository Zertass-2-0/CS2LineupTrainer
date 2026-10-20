package com.example.cs2lineuptrainer.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.cs2lineuptrainer.domain.GrenadeType

// Кнопки-фильтры с типами гранат. selected = null, если ничего не выбрано.
// vertical = false: четыре кнопки в ряд (портрет); true: столбцом на всю ширину (узкая колонка в альбоме).
@Composable
fun GrenadeTypeSelector(
    selected: GrenadeType?,
    onSelect: (GrenadeType) -> Unit,
    modifier: Modifier = Modifier,
    vertical: Boolean = false,
) {
    @Composable
    fun Chip(type: GrenadeType, chipModifier: Modifier) {
        FilterChip(
            selected = type == selected,
            onClick = { onSelect(type) },
            label = {
                Text(
                    text = type.label,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            modifier = chipModifier,
        )
    }

    if (vertical) {
        Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            GrenadeType.entries.forEach { type -> Chip(type, Modifier.fillMaxWidth()) }
        }
    } else {
        Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GrenadeType.entries.forEach { type -> Chip(type, Modifier.weight(1f)) }
        }
    }
}
