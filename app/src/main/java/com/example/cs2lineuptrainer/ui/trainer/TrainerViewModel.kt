package com.example.cs2lineuptrainer.ui.trainer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.cs2lineuptrainer.CsApplication
import com.example.cs2lineuptrainer.data.LineupRepository
import com.example.cs2lineuptrainer.data.ProgressRepository
import com.example.cs2lineuptrainer.domain.Area
import com.example.cs2lineuptrainer.domain.DEFAULT_SESSION_LENGTH
import com.example.cs2lineuptrainer.domain.FrameWindow
import com.example.cs2lineuptrainer.domain.Lineup
import com.example.cs2lineuptrainer.domain.LineupProgress
import com.example.cs2lineuptrainer.domain.MapInfo
import com.example.cs2lineuptrainer.domain.Offset01
import com.example.cs2lineuptrainer.domain.QuestionPicker
import com.example.cs2lineuptrainer.domain.Side
import com.example.cs2lineuptrainer.domain.ThrowType
import com.example.cs2lineuptrainer.domain.checkAnswer
import com.example.cs2lineuptrainer.domain.randomFrameWindow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

// Один вопрос: лайнап и та часть кадра (окно), которую увидит игрок. Окно случайное и своё у каждого вопроса.
data class Question(val lineup: Lineup, val window: FrameWindow)

// Итог одного вопроса.
data class QuestionResult(val lineup: Lineup, val hit: Boolean)

// Сессия тренировки: вопросы, ответы на них и какие лайнапы были выучены до начала и после.
data class TrainerSession(
    val questions: List<Question>,
    val learnedAtStart: Set<String>,
    val results: List<QuestionResult> = emptyList(),
    val learnedNow: Set<String> = emptySet(),
) {
    val current: Question? get() = questions.getOrNull(results.size)
    val isFinished: Boolean get() = results.size >= questions.size
    val hits: Int get() = results.count { it.hit }
    val newlyLearned: Int get() = (learnedNow - learnedAtStart).size
}

// Короткое сообщение «Попал / Не попал». number отличает одно сообщение от другого.
data class Feedback(val hit: Boolean, val number: Int)

// Всё, что нужно экрану тренировки. session == null означает экран настроек.
data class TrainerUiState(
    val map: MapInfo,
    val side: Side = Side.T,
    val area: Area? = null,
    val availableCount: Int = 0,
    val session: TrainerSession? = null,
    val feedback: Feedback? = null,
)

class TrainerViewModel(
    lineupRepository: LineupRepository,
    private val progressRepository: ProgressRepository,
    private val random: Random = Random.Default,
) : ViewModel() {

    private val map = lineupRepository.getMaps().first()
    private val allLineups = lineupRepository.getLineups(map.id)

    private val _state = MutableStateFlow(TrainerUiState(map, availableCount = poolFor(Side.T, null).size))
    val state: StateFlow<TrainerUiState> = _state.asStateFlow()

    // Лайнапы, подходящие под настройки: выбранная сторона и (если указана) область.
    private fun poolFor(side: Side, area: Area?): List<Lineup> =
        allLineups.filter { it.side == side && (area == null || it.area == area) }

    private fun learnedIds(progress: Map<String, LineupProgress>): Set<String> =
        progress.filterValues { it.learned }.keys

    fun onSideSelected(side: Side) {
        _state.update { it.copy(side = side, availableCount = poolFor(side, it.area).size) }
    }

    // area = null значит «все области».
    fun onAreaSelected(area: Area?) {
        _state.update { it.copy(area = area, availableCount = poolFor(it.side, area).size) }
    }

    // Начало сессии: подбираем вопросы (невыученные выпадают чаще).
    fun onStart() {
        val current = _state.value
        val pool = poolFor(current.side, current.area)
        if (pool.isEmpty()) return

        val progress = progressRepository.progress.value
        // Каждому вопросу даём своё случайное окно кадра, поэтому один и тот же лайнап выглядит по-разному.
        val questions = QuestionPicker.pick(pool, progress, DEFAULT_SESSION_LENGTH, random)
            .map { lineup -> Question(lineup, randomFrameWindow(lineup.aimPoint, random, lineup.zoom)) }
        _state.value = current.copy(
            session = TrainerSession(questions, learnedAtStart = learnedIds(progress)),
            feedback = null,
        )
    }

    // Ответ на вопрос: проверяем, сохраняем попытку в прогресс и переходим к следующему вопросу.
    fun onConfirm(marker: Offset01, throwType: ThrowType) {
        val session = _state.value.session ?: return
        val question = session.current ?: return
        val lineup = question.lineup

        viewModelScope.launch {
            val hit = checkAnswer(lineup, question.window, marker, throwType)
            progressRepository.recordAttempt(lineup.id, hit)

            var updated = session.copy(results = session.results + QuestionResult(lineup, hit))
            if (updated.isFinished) {
                updated = updated.copy(learnedNow = learnedIds(progressRepository.progress.value))
            }
            _state.update { it.copy(session = updated, feedback = Feedback(hit, updated.results.size)) }
        }
    }

    // Экран показал надпись, её можно забыть (чтобы она не появилась снова при пересоздании экрана).
    fun onFeedbackShown() {
        _state.update { it.copy(feedback = null) }
    }

    // Возврат на экран настроек (из вопроса или из итога).
    fun onBackToSetup() {
        _state.update { it.copy(session = null, feedback = null) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as CsApplication
                TrainerViewModel(app.container.lineupRepository, app.container.progressRepository)
            }
        }
    }
}
