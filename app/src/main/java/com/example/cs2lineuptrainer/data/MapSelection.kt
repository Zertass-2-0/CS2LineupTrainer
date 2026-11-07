package com.example.cs2lineuptrainer.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// Выбранная карта, общая для всех экранов: выбрали один раз, и Поиск, Тренировка и Статистика показывают её.
// Живёт в AppContainer (как прогресс), поэтому переживает смену вкладок и поворот экрана.
class MapSelection(initialMapId: String) {
    private val _selectedMapId = MutableStateFlow(initialMapId)
    val selectedMapId: StateFlow<String> = _selectedMapId.asStateFlow()

    fun select(mapId: String) {
        _selectedMapId.value = mapId
    }
}
