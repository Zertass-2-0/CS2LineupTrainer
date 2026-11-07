package com.example.cs2lineuptrainer.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.cs2lineuptrainer.domain.MapInfo

// Выбор карты выпадающим меню: кнопка с названием выбранной карты, по нажатию список всех карт.
@Composable
fun MapMenu(
    maps: List<MapInfo>,
    selectedId: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    // remember: открыто меню или нет, после поворота можно не помнить.
    var expanded by remember { mutableStateOf(false) }
    val selected = maps.firstOrNull { it.id == selectedId } ?: return

    Box(modifier = modifier) {
        TextButton(onClick = { expanded = true }, contentPadding = PaddingValues(horizontal = 8.dp)) {
            Text("${selected.name} \u25BE", maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            maps.forEach { map ->
                DropdownMenuItem(
                    text = { Text(map.name) },
                    onClick = {
                        expanded = false
                        onSelect(map.id)
                    },
                )
            }
        }
    }
}
