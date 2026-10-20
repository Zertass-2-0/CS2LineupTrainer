package com.example.cs2lineuptrainer.ui

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// Связь между экранами: «открой этот лайнап в Поиске» и «покажи этот лайнап в Статистике».
// Один экран кладёт запрос, другой забирает его один раз (забрал - запрос очищается).
// Так ViewModel'и не знают друг о друге, а переход не повторяется при повороте экрана.
class LineupRequests {
    private val _openInExplorer = MutableStateFlow<String?>(null)
    val openInExplorer: StateFlow<String?> = _openInExplorer.asStateFlow()

    private val _showInStats = MutableStateFlow<String?>(null)
    val showInStats: StateFlow<String?> = _showInStats.asStateFlow()

    fun requestOpenInExplorer(lineupId: String) {
        _openInExplorer.value = lineupId
    }

    fun requestShowInStats(lineupId: String) {
        _showInStats.value = lineupId
    }

    fun explorerHandled() {
        _openInExplorer.value = null
    }

    fun statsHandled() {
        _showInStats.value = null
    }
}
