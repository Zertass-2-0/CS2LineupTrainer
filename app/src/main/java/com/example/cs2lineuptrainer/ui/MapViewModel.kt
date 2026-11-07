package com.example.cs2lineuptrainer.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.cs2lineuptrainer.CsApplication
import com.example.cs2lineuptrainer.data.LineupRepository
import com.example.cs2lineuptrainer.data.MapSelection
import com.example.cs2lineuptrainer.domain.MapInfo
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

// Что нужно переключателю карт: все карты и id выбранной.
data class MapPickerState(val maps: List<MapInfo>, val selectedId: String)

// Состояние переключателя карт. Сам выбор хранит MapSelection (общий для экранов), здесь только мост к интерфейсу.
class MapViewModel(
    lineupRepository: LineupRepository,
    private val mapSelection: MapSelection,
) : ViewModel() {

    private val maps = lineupRepository.getMaps()

    val state: StateFlow<MapPickerState> = mapSelection.selectedMapId
        .map { MapPickerState(maps, it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = MapPickerState(maps, mapSelection.selectedMapId.value),
        )

    fun onMapSelected(mapId: String) {
        mapSelection.select(mapId)
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as CsApplication
                MapViewModel(app.container.lineupRepository, app.container.mapSelection)
            }
        }
    }
}
