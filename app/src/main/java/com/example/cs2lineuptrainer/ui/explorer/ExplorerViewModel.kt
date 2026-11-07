package com.example.cs2lineuptrainer.ui.explorer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.cs2lineuptrainer.CsApplication
import com.example.cs2lineuptrainer.data.LineupRepository
import com.example.cs2lineuptrainer.data.MapSelection
import com.example.cs2lineuptrainer.domain.GrenadeType
import com.example.cs2lineuptrainer.domain.Lineup
import com.example.cs2lineuptrainer.domain.MapInfo
import com.example.cs2lineuptrainer.domain.NamedPoint
import com.example.cs2lineuptrainer.ui.LineupRequests
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// Всё, что нужно экрану поиска. Шаг определяется тем, что уже выбрано:
// ничего -> grenade -> selectedTargetId -> selectedLineup.
data class ExplorerUiState(
    val map: MapInfo,
    val grenade: GrenadeType? = null,
    val targets: List<NamedPoint> = emptyList(),
    val selectedTargetId: String? = null,
    val lineupsForTarget: List<Lineup> = emptyList(),
    val selectedLineup: Lineup? = null,
)

class ExplorerViewModel(
    private val lineupRepository: LineupRepository,
    private val requests: LineupRequests = LineupRequests(),
    mapSelection: MapSelection = MapSelection(lineupRepository.getMaps().first().id),
) : ViewModel() {

    private var map = lineupRepository.getMap(mapSelection.selectedMapId.value)
    private var lineups = lineupRepository.getLineups(map.id)

    private val _state = MutableStateFlow(ExplorerUiState(map))
    val state: StateFlow<ExplorerUiState> = _state.asStateFlow()

    init {
        // Выбрали другую карту: показываем её с чистого листа (цели, позиции и инструкция прошлой карты не нужны).
        viewModelScope.launch {
            mapSelection.selectedMapId.collect { mapId ->
                if (mapId != map.id) {
                    map = lineupRepository.getMap(mapId)
                    lineups = lineupRepository.getLineups(mapId)
                    _state.value = ExplorerUiState(map)
                }
            }
        }

        // Если из Статистики попросили открыть лайнап - открываем его и сообщаем, что запрос выполнен.
        viewModelScope.launch {
            requests.openInExplorer.collect { lineupId ->
                if (lineupId != null) {
                    openLineup(lineupId)
                    requests.explorerHandled()
                }
            }
        }
    }

    // Показывает лайнап так, будто игрок сам прошёл все шаги: выбрана граната, цель и позиция, открыта инструкция.
    // Если такого лайнапа нет, ничего не меняется.
    fun openLineup(lineupId: String) {
        val lineup = lineups.firstOrNull { it.id == lineupId } ?: return
        _state.value = ExplorerUiState(
            map = map,
            grenade = lineup.grenade,
            targets = lineups.filter { it.grenade == lineup.grenade }.map { it.target }.distinctBy { it.id },
            selectedTargetId = lineup.target.id,
            lineupsForTarget = lineups.filter { it.grenade == lineup.grenade && it.target.id == lineup.target.id },
            selectedLineup = lineup,
        )
    }

    // Кнопка «К статистике» в инструкции: закрываем инструкцию (выбор цели и позиции остаётся) и просим Статистику показать лайнап.
    fun showInStats(lineupId: String) {
        _state.value = _state.value.copy(selectedLineup = null)
        requests.requestShowInStats(lineupId)
    }

    // Выбор типа гранаты: показываем цели только этого типа. Повторный тап по выбранному типу сбрасывает выбор.
    fun onGrenadeSelected(grenade: GrenadeType) {
        if (grenade == _state.value.grenade) {
            _state.value = ExplorerUiState(map)
            return
        }
        val targets = lineups
            .filter { it.grenade == grenade }
            .map { it.target }
            .distinctBy { it.id }
        _state.value = ExplorerUiState(map, grenade = grenade, targets = targets)
    }

    // Выбор цели: находим все лайнапы этого типа гранаты на эту цель, их позиции покажутся на радаре.
    fun onTargetSelected(targetId: String) {
        val current = _state.value
        _state.value = current.copy(
            selectedTargetId = targetId,
            lineupsForTarget = lineups.filter { it.grenade == current.grenade && it.target.id == targetId },
            selectedLineup = null,
        )
    }

    // Выбор позиции = выбор конкретного лайнапа, под него откроется нижняя панель.
    fun onLineupSelected(lineupId: String) {
        val current = _state.value
        _state.value = current.copy(selectedLineup = current.lineupsForTarget.firstOrNull { it.id == lineupId })
    }

    // Шаг назад: позиция -> цель -> тип гранаты -> начало.
    fun onBack() {
        val current = _state.value
        _state.value = when {
            current.selectedLineup != null -> current.copy(selectedLineup = null)
            current.selectedTargetId != null -> current.copy(selectedTargetId = null, lineupsForTarget = emptyList())
            current.grenade != null -> ExplorerUiState(map)
            else -> current
        }
    }

    companion object {
        // Создаёт ViewModel с доступом к репозиторию из AppContainer.
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as CsApplication
                ExplorerViewModel(app.container.lineupRepository, app.container.lineupRequests, app.container.mapSelection)
            }
        }
    }
}
