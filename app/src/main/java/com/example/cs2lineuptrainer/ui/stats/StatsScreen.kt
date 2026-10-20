package com.example.cs2lineuptrainer.ui.stats

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.cs2lineuptrainer.domain.GrenadeType
import com.example.cs2lineuptrainer.domain.LEARN_STREAK
import com.example.cs2lineuptrainer.ui.components.LearnedProgress
import com.example.cs2lineuptrainer.ui.components.OptionChips
import com.example.cs2lineuptrainer.ui.isLandscape
import com.example.cs2lineuptrainer.ui.theme.HitColor
import kotlinx.coroutines.delay

// Сколько времени лайнап остаётся подсвеченным после перехода из инструкции.
private const val FOCUS_HIGHLIGHT_MS = 1800L

// За сколько миллисекунд подсветка лайнапа плавно гаснет (и появляется).
private const val HIGHLIGHT_FADE_MS = 700

// В портретном списке перед карточками лайнапов идут три блока: заголовок, сводка и фильтр.
private const val PORTRAIT_HEADER_ITEMS = 3

// Обёртка: берёт состояние из ViewModel и передаёт в «глупый» StatsContent.
// onOpenExplorer переключает вкладку на «Поиск» (это делает навигация).
@Composable
fun StatsScreen(
    viewModel: StatsViewModel,
    onOpenExplorer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    StatsContent(
        state = state,
        onFilterSelected = viewModel::onFilterSelected,
        onLineupClick = { lineupId ->
            viewModel.onLineupClick(lineupId)
            onOpenExplorer()
        },
        onFocusHandled = viewModel::onFocusHandled,
        modifier = modifier,
    )
}

// Статистика: общий прогресс, прогресс по типам гранат, фильтр и список лайнапов с их статусами.
// Портрет: один прокручиваемый список. Альбом: верхний блок и две прокручиваемые колонки.
@Composable
fun StatsContent(
    state: StatsUiState,
    onFilterSelected: (GrenadeType?) -> Unit,
    onLineupClick: (String) -> Unit,
    onFocusHandled: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val landscape = isLandscape()

    // Пришли из инструкции в Поиске: прокручиваем к нужному лайнапу, держим подсветку и снимаем фокус.
    LaunchedEffect(state.focusedLineupId) {
        val focusedId = state.focusedLineupId ?: return@LaunchedEffect
        val index = state.items.indexOfFirst { it.lineup.id == focusedId }
        if (index >= 0) {
            listState.animateScrollToItem(if (landscape) index else PORTRAIT_HEADER_ITEMS + index)
        }
        delay(FOCUS_HIGHLIGHT_MS)
        onFocusHandled()
    }

    if (landscape) {
        StatsLandscape(state, listState, onFilterSelected, onLineupClick, modifier)
    } else {
        StatsPortrait(state, listState, onFilterSelected, onLineupClick, modifier)
    }
}

@Composable
private fun StatsPortrait(
    state: StatsUiState,
    listState: LazyListState,
    onFilterSelected: (GrenadeType?) -> Unit,
    onLineupClick: (String) -> Unit,
    modifier: Modifier,
) {
    LazyColumn(
        state = listState,
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Text("Статистика", style = MaterialTheme.typography.headlineMedium) }

        item { SummaryCard(state) }

        item {
            // null в списке означает «все типы».
            OptionChips<GrenadeType?>(
                options = listOf(null) + GrenadeType.entries,
                selected = state.filter,
                label = { it?.label ?: "Все" },
                onSelect = onFilterSelected,
            )
        }

        if (state.items.isEmpty()) {
            item { Text("Лайнапов пока нет", style = MaterialTheme.typography.bodyLarge) }
        } else {
            items(state.items, key = { it.lineup.id }) { stat ->
                LineupStatCard(stat, highlighted = stat.lineup.id == state.focusedLineupId, onClick = { onLineupClick(stat.lineup.id) })
            }
        }
    }
}

// Альбом: сверху блок с картой и общим прогрессом, под ним две колонки со своей прокруткой:
// слева типы гранат (с прогрессом и выбором), справа лайнапы выбранного типа.
@Composable
private fun StatsLandscape(
    state: StatsUiState,
    listState: LazyListState,
    onFilterSelected: (GrenadeType?) -> Unit,
    onLineupClick: (String) -> Unit,
    modifier: Modifier,
) {
    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                Text(state.mapName, style = MaterialTheme.typography.titleLarge)
                LearnedProgress(
                    label = "Выучено лайнапов",
                    learned = state.learned,
                    total = state.total,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            LazyColumn(
                modifier = Modifier.width(220.dp).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    GrenadeFilterCard("Все", state.learned, state.total, selected = state.filter == null) {
                        onFilterSelected(null)
                    }
                }
                items(state.byGrenade, key = { it.grenade }) { stat ->
                    GrenadeFilterCard(stat.grenade.label, stat.learned, stat.total, selected = state.filter == stat.grenade) {
                        onFilterSelected(stat.grenade)
                    }
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (state.items.isEmpty()) {
                    item { Text("Лайнапов пока нет", style = MaterialTheme.typography.bodyLarge) }
                } else {
                    items(state.items, key = { it.lineup.id }) { stat ->
                        LineupStatCard(stat, highlighted = stat.lineup.id == state.focusedLineupId, onClick = { onLineupClick(stat.lineup.id) })
                    }
                }
            }
        }
    }
}

// Карточка с общим прогрессом по карте и по каждому типу гранат.
@Composable
private fun SummaryCard(state: StatsUiState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(state.mapName, style = MaterialTheme.typography.titleLarge)
            LearnedProgress(label = "Выучено лайнапов", learned = state.learned, total = state.total)
            state.byGrenade.forEach { stat ->
                LearnedProgress(label = stat.grenade.label, learned = stat.learned, total = stat.total)
            }
        }
    }
}

// Цвета карточки: обычные или заметная подсветка (выбранный фильтр, лайнап в фокусе).
@Composable
private fun highlightColors(highlighted: Boolean) =
    if (highlighted) {
        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
    } else {
        CardDefaults.cardColors()
    }

// Тип гранаты с прогрессом: тап выбирает его как фильтр (выбранный подсвечен).
@Composable
private fun GrenadeFilterCard(label: String, learned: Int, total: Int, selected: Boolean, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = highlightColors(selected),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            LearnedProgress(label = label, learned = learned, total = total)
        }
    }
}

// Один лайнап: название, краткое описание и статус выученности. Тап открывает его в Поиске.
// highlighted: короткая подсветка, когда пришли сюда по кнопке «К статистике».
@Composable
private fun LineupStatCard(stat: LineupStat, highlighted: Boolean, onClick: () -> Unit) {
    val lineup = stat.lineup
    val (statusText, statusColor) = when (stat.status) {
        LineupStatus.LEARNED -> "Выучен" to HitColor
        LineupStatus.LEARNING -> "Учится: ${stat.correctStreak} из $LEARN_STREAK подряд" to MaterialTheme.colorScheme.primary
        LineupStatus.NEW -> "Не тренировался" to Color.Gray
    }

    // Подсветка появляется и гаснет плавно: 0 - обычная карточка, 1 - полностью подсвеченная.
    val highlight by animateFloatAsState(
        targetValue = if (highlighted) 1f else 0f,
        animationSpec = tween(durationMillis = HIGHLIGHT_FADE_MS),
        label = "lineupHighlight",
    )
    val normalColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val primary = MaterialTheme.colorScheme.primary
    val highlightedColor = primary.copy(alpha = 0.3f).compositeOver(normalColor)

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = lerp(normalColor, highlightedColor, highlight)),
        border = if (highlight > 0.01f) BorderStroke(2.dp, primary.copy(alpha = highlight)) else null,
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(lineup.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "${lineup.grenade.label} · за ${lineup.side.name} · ${lineup.area.label}",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Text(statusText, color = statusColor, style = MaterialTheme.typography.labelLarge)
        }
    }
}
