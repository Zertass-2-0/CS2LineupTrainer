package com.example.cs2lineuptrainer.ui.trainer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.cs2lineuptrainer.domain.Area
import com.example.cs2lineuptrainer.domain.DEFAULT_SESSION_LENGTH
import com.example.cs2lineuptrainer.domain.Side
import com.example.cs2lineuptrainer.ui.components.OptionChips

// Экран настроек тренировки: сторона и область.
@Composable
fun TrainerSetupContent(
    state: TrainerUiState,
    onSideSelected: (Side?) -> Unit,
    onAreaSelected: (Area?) -> Unit,
    onStart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Тренировка", style = MaterialTheme.typography.headlineMedium)
        Text("Карта: ${state.map.name}", style = MaterialTheme.typography.bodyLarge)

        Text("Сторона", style = MaterialTheme.typography.titleMedium)
        // null в списке означает «любая сторона»: гранату можно тренировать независимо от того, за кого её бросают.
        OptionChips<Side?>(
            options = listOf(null) + Side.entries,
            selected = state.side,
            label = { if (it == null) "Любая" else "За ${it.name}" },
            onSelect = onSideSelected,
        )

        Text("Область", style = MaterialTheme.typography.titleMedium)
        // null в списке означает «все области».
        OptionChips<Area?>(
            options = listOf(null) + state.areas,
            selected = state.area,
            label = { it?.label ?: "Все" },
            onSelect = onAreaSelected,
        )

        Text(
            text = if (state.availableCount == 0) {
                "Под эти настройки нет лайнапов"
            } else {
                "Доступно лайнапов: ${state.availableCount}. Вопросов в сессии: $DEFAULT_SESSION_LENGTH"
            },
            style = MaterialTheme.typography.bodyMedium,
        )

        Button(
            onClick = onStart,
            enabled = state.availableCount > 0,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Начать")
        }
    }
}
