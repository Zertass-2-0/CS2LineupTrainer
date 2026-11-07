package com.example.cs2lineuptrainer.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.cs2lineuptrainer.CsApplication
import com.example.cs2lineuptrainer.data.LineupRepository
import com.example.cs2lineuptrainer.data.MapSelection
import com.example.cs2lineuptrainer.data.ProgressRepository
import com.example.cs2lineuptrainer.domain.GrenadeType
import com.example.cs2lineuptrainer.domain.Lineup
import com.example.cs2lineuptrainer.domain.LineupProgress
import com.example.cs2lineuptrainer.ui.LineupRequests
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// Состояние одного лайнапа: ещё не тренировался, учится или уже выучен.
enum class LineupStatus { NEW, LEARNING, LEARNED }

data class LineupStat(val lineup: Lineup, val status: LineupStatus, val correctStreak: Int)

// Сколько лайнапов одного типа гранаты выучено.
data class GrenadeStat(val grenade: GrenadeType, val learned: Int, val total: Int)

data class StatsUiState(
    val mapName: String,
    val learned: Int,
    val total: Int,
    val byGrenade: List<GrenadeStat>,
    val filter: GrenadeType?,
    val items: List<LineupStat>,
    val focusedLineupId: String? = null,
)

// Чистая функция: из лайнапов и прогресса собирает всё, что нужно экрану. filter = null значит «все типы».
// Список идёт в порядке: не тренировался -> учится -> выучен (внутри группы порядок как в данных).
// focusedLineupId - лайнап, на который нужно прокрутить и который нужно подсветить.
fun computeStats(
    mapName: String,
    lineups: List<Lineup>,
    progress: Map<String, LineupProgress>,
    filter: GrenadeType?,
    focusedLineupId: String? = null,
): StatsUiState {
    fun statOf(lineup: Lineup): LineupStat {
        val p = progress[lineup.id]
        val status = when {
            p == null -> LineupStatus.NEW
            p.learned -> LineupStatus.LEARNED
            else -> LineupStatus.LEARNING
        }
        return LineupStat(lineup, status, p?.correctStreak ?: 0)
    }

    val stats = lineups.map(::statOf)
    val byGrenade = GrenadeType.entries.mapNotNull { grenade ->
        val ofType = stats.filter { it.lineup.grenade == grenade }
        if (ofType.isEmpty()) null else GrenadeStat(grenade, ofType.count { it.status == LineupStatus.LEARNED }, ofType.size)
    }
    return StatsUiState(
        mapName = mapName,
        learned = stats.count { it.status == LineupStatus.LEARNED },
        total = stats.size,
        byGrenade = byGrenade,
        filter = filter,
        items = stats.filter { filter == null || it.lineup.grenade == filter }.sortedBy { it.status },
        focusedLineupId = focusedLineupId,
    )
}

class StatsViewModel(
    private val lineupRepository: LineupRepository,
    progressRepository: ProgressRepository,
    private val requests: LineupRequests = LineupRequests(),
    private val mapSelection: MapSelection = MapSelection(lineupRepository.getMaps().first().id),
) : ViewModel() {

    private val filter = MutableStateFlow<GrenadeType?>(null)
    private val focusedLineupId = MutableStateFlow<String?>(null)

    // Считает статистику выбранной карты: название и лайнапы берутся по её id.
    private fun statsFor(
        mapId: String,
        progress: Map<String, LineupProgress>,
        selected: GrenadeType?,
        focused: String?,
    ): StatsUiState =
        computeStats(lineupRepository.getMap(mapId).name, lineupRepository.getLineups(mapId), progress, selected, focused)

    // Пересчитывается при каждом изменении прогресса (после тренировки), смене фильтра, фокуса и карты.
    val state: StateFlow<StatsUiState> = combine(
        progressRepository.progress, filter, focusedLineupId, mapSelection.selectedMapId,
    ) { progress, selected, focused, mapId ->
        statsFor(mapId, progress, selected, focused)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = statsFor(mapSelection.selectedMapId.value, progressRepository.progress.value, null, null),
    )

    init {
        // Если из инструкции в Поиске попросили показать лайнап: выбираем его тип гранаты и фокусируемся на нём.
        viewModelScope.launch {
            requests.showInStats.collect { lineupId ->
                if (lineupId != null) {
                    lineupRepository.getLineups(mapSelection.selectedMapId.value).firstOrNull { it.id == lineupId }?.let { lineup ->
                        filter.value = lineup.grenade
                        focusedLineupId.value = lineup.id
                    }
                    requests.statsHandled()
                }
            }
        }
    }

    // null означает «все типы гранат». Смена фильтра снимает фокус с лайнапа.
    fun onFilterSelected(grenade: GrenadeType?) {
        filter.value = grenade
        focusedLineupId.value = null
    }

    // Тап по лайнапу: просим Поиск открыть его (вкладку переключает навигация).
    fun onLineupClick(lineupId: String) {
        requests.requestOpenInExplorer(lineupId)
    }

    // Экран уже прокрутил к лайнапу и подсветил его, фокус можно снять.
    fun onFocusHandled() {
        focusedLineupId.value = null
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as CsApplication
                StatsViewModel(
                    app.container.lineupRepository,
                    app.container.progressRepository,
                    app.container.lineupRequests,
                    app.container.mapSelection,
                )
            }
        }
    }
}
