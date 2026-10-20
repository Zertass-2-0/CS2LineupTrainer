package com.example.cs2lineuptrainer.data

import com.example.cs2lineuptrainer.domain.LineupProgress
import com.example.cs2lineuptrainer.domain.afterAttempt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

// Единственная точка входа для прогресса. Остальное приложение знает только этот интерфейс,
// поэтому позже реализацию можно заменить на DataStore/Room в одном месте (AppContainer).
interface ProgressRepository {
    val progress: StateFlow<Map<String, LineupProgress>>
    suspend fun recordAttempt(lineupId: String, hit: Boolean)
}

// Хранит прогресс в памяти: живёт, пока жив процесс приложения.
class InMemoryProgressRepository : ProgressRepository {
    private val _progress = MutableStateFlow<Map<String, LineupProgress>>(emptyMap())
    override val progress: StateFlow<Map<String, LineupProgress>> = _progress.asStateFlow()

    // Берёт прогресс лайнапа (или создаёт пустой), применяет попытку и кладёт обратно новую карту.
    override suspend fun recordAttempt(lineupId: String, hit: Boolean) {
        _progress.update { current ->
            val old = current[lineupId] ?: LineupProgress(lineupId)
            current + (lineupId to old.afterAttempt(hit))
        }
    }
}
