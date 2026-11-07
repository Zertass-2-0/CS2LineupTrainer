package com.example.cs2lineuptrainer

import com.example.cs2lineuptrainer.data.InMemoryProgressRepository
import com.example.cs2lineuptrainer.data.LineupRepository
import com.example.cs2lineuptrainer.data.MapSelection
import com.example.cs2lineuptrainer.data.ProgressRepository
import com.example.cs2lineuptrainer.ui.LineupRequests

// Ручной контейнер зависимостей: держит репозитории как синглтоны на всё время жизни приложения.
class AppContainer {
    val lineupRepository = LineupRepository()

    // Выбранная карта (общая для всех экранов). Сначала выбрана первая карта списка.
    val mapSelection = MapSelection(lineupRepository.getMaps().first().id)

    // Запросы между экранами (например, «открыть лайнап из статистики в Поиске»).
    val lineupRequests = LineupRequests()

    // Чтобы включить сохранение, достаточно заменить эту строку на DataStore-реализацию.
    val progressRepository: ProgressRepository = InMemoryProgressRepository()
}
