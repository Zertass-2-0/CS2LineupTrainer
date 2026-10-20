package com.example.cs2lineuptrainer.ui.explorer

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.cs2lineuptrainer.domain.GrenadeType
import com.example.cs2lineuptrainer.domain.Lineup
import com.example.cs2lineuptrainer.ui.components.GrenadeTypeSelector
import com.example.cs2lineuptrainer.ui.components.LineupMediaView
import com.example.cs2lineuptrainer.ui.components.MapRadar
import com.example.cs2lineuptrainer.ui.components.RadarPoint
import com.example.cs2lineuptrainer.ui.isLandscape
import com.example.cs2lineuptrainer.ui.theme.PositionColor
import com.example.cs2lineuptrainer.ui.theme.TargetColor

// Обёртка: берёт состояние из ViewModel и передаёт в «глупый» ExplorerContent.
// onOpenStats переключает вкладку на статистику (это делает навигация).
@Composable
fun ExplorerScreen(
    viewModel: ExplorerViewModel,
    onOpenStats: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ExplorerContent(
        state = state,
        onGrenadeSelected = viewModel::onGrenadeSelected,
        onTargetSelected = viewModel::onTargetSelected,
        onLineupSelected = viewModel::onLineupSelected,
        onBack = viewModel::onBack,
        onShowInStats = { lineupId ->
            viewModel.showInStats(lineupId)
            onOpenStats()
        },
        modifier = modifier,
    )
}

// Сам экран: сам ничего не хранит, только рисует state и сообщает о нажатиях.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExplorerContent(
    state: ExplorerUiState,
    onGrenadeSelected: (GrenadeType) -> Unit,
    onTargetSelected: (String) -> Unit,
    onLineupSelected: (String) -> Unit,
    onBack: () -> Unit,
    onShowInStats: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Системная кнопка «назад» делает шаг назад, пока что-то выбрано.
    BackHandler(enabled = state.grenade != null, onBack = onBack)

    val hint = when {
        state.grenade == null -> "Выберите тип гранаты"
        state.selectedTargetId == null -> "Выберите цель"
        else -> "Выберите позицию, откуда бросать"
    }
    val points = buildRadarPoints(state, onTargetSelected, onLineupSelected)
    val landscape = isLandscape()

    if (landscape) {
        // Альбом: слева выбор гранаты и подсказка, справа радар по высоте экрана.
        Row(
            modifier = modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(
                modifier = Modifier
                    .width(180.dp)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                GrenadeTypeSelector(selected = state.grenade, onSelect = onGrenadeSelected, vertical = true)
                Text(hint, style = MaterialTheme.typography.bodyLarge)
            }
            BoxWithConstraints(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                contentAlignment = Alignment.Center,
            ) {
                MapRadar(
                    radar = state.map.radar,
                    points = points,
                    onEmptyClick = onBack,
                    modifier = Modifier.size(minOf(maxWidth, maxHeight)),
                )
            }
        }
    } else {
        // Портрет: всё в столбец, радар на всю ширину.
        Column(
            modifier = modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            GrenadeTypeSelector(selected = state.grenade, onSelect = onGrenadeSelected)
            Text(hint, style = MaterialTheme.typography.bodyLarge)
            MapRadar(
                radar = state.map.radar,
                points = points,
                onEmptyClick = onBack,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    state.selectedLineup?.let { lineup ->
        // skipPartiallyExpanded: панель сразу раскрывается полностью (в альбоме иначе виден только край).
        ModalBottomSheet(
            onDismissRequest = onBack,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            LineupDetails(lineup, landscape, onShowInStats)
        }
    }
}

// Красные точки - цели (невыбранные полупрозрачные, когда цель выбрана), голубые - позиции для выбранной цели.
private fun buildRadarPoints(
    state: ExplorerUiState,
    onTargetSelected: (String) -> Unit,
    onLineupSelected: (String) -> Unit,
): List<RadarPoint> {
    val targetPoints = state.targets.map { target ->
        val dimmed = state.selectedTargetId != null && target.id != state.selectedTargetId
        RadarPoint(
            id = "target_${target.id}",
            x = target.x,
            y = target.y,
            color = TargetColor,
            alpha = if (dimmed) 0.3f else 1f,
            onClick = { onTargetSelected(target.id) },
        )
    }
    val positionPoints = state.lineupsForTarget.map { lineup ->
        RadarPoint(
            id = "lineup_${lineup.id}",
            x = lineup.origin.x,
            y = lineup.origin.y,
            color = PositionColor,
            onClick = { onLineupSelected(lineup.id) },
        )
    }
    return targetPoints + positionPoints
}

// Содержимое нижней панели: как бросать, тип броска, описание и кнопка перехода к статистике.
// В альбоме картинка слева, текст и кнопка справа; в портрете всё в столбец.
@Composable
private fun LineupDetails(lineup: Lineup, landscape: Boolean, onShowInStats: (String) -> Unit) {
    val padding = Modifier
        .padding(horizontal = 16.dp)
        .padding(bottom = 16.dp)
        .navigationBarsPadding()

    if (landscape) {
        Row(
            modifier = padding,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            LineupMediaView(lineup.howTo, Modifier.weight(1.3f))
            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(lineup.title, style = MaterialTheme.typography.titleLarge)
                Text("Бросок: ${lineup.throwType.label}", style = MaterialTheme.typography.titleMedium)
                Text(lineup.description, style = MaterialTheme.typography.bodyMedium)
                Button(onClick = { onShowInStats(lineup.id) }) { Text("К статистике") }
            }
        }
    } else {
        Column(
            modifier = padding,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(lineup.title, style = MaterialTheme.typography.titleLarge)
            LineupMediaView(lineup.howTo)
            Text("Бросок: ${lineup.throwType.label}", style = MaterialTheme.typography.titleMedium)
            Text(lineup.description, style = MaterialTheme.typography.bodyMedium)
            Button(onClick = { onShowInStats(lineup.id) }, modifier = Modifier.fillMaxWidth()) {
                Text("К статистике")
            }
        }
    }
}
