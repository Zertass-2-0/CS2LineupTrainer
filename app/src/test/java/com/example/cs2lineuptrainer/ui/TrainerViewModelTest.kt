package com.example.cs2lineuptrainer.ui

import com.example.cs2lineuptrainer.data.InMemoryProgressRepository
import com.example.cs2lineuptrainer.data.LineupRepository
import com.example.cs2lineuptrainer.domain.Area
import com.example.cs2lineuptrainer.domain.DEFAULT_SESSION_LENGTH
import com.example.cs2lineuptrainer.domain.DEFAULT_ZOOM
import com.example.cs2lineuptrainer.domain.GrenadeType
import com.example.cs2lineuptrainer.domain.Lineup
import com.example.cs2lineuptrainer.domain.LineupMedia
import com.example.cs2lineuptrainer.domain.MapInfo
import com.example.cs2lineuptrainer.domain.NamedPoint
import com.example.cs2lineuptrainer.domain.Offset01
import com.example.cs2lineuptrainer.domain.Side
import com.example.cs2lineuptrainer.domain.ThrowType
import com.example.cs2lineuptrainer.ui.trainer.TrainerViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
class TrainerViewModelTest {

    // viewModelScope работает на главном потоке Android; в тестах подменяем его на тестовый диспетчер.
    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun lineup(id: String, side: Side, area: Area, zoom: Float = DEFAULT_ZOOM) = Lineup(
        id = id,
        mapId = "m",
        grenade = GrenadeType.SMOKE,
        side = side,
        area = area,
        title = id,
        target = NamedPoint("t_$id", "t", 0.3f, 0.3f),
        origin = NamedPoint("o_$id", "o", 0.6f, 0.6f),
        standView = LineupMedia.Image(0),
        aimPoint = Offset01(0.5f, 0.5f),
        tolerance = 0.05f,
        zoom = zoom,
        throwType = ThrowType.LMB,
        howTo = LineupMedia.Image(0),
        description = "",
    )

    private val lineups = listOf(
        lineup("t_a_1", Side.T, Area.A),
        lineup("t_a_2", Side.T, Area.A),
        lineup("t_b", Side.T, Area.B),
        lineup("ct_mid", Side.CT, Area.MID),
    )

    private val progress = InMemoryProgressRepository()

    private fun newViewModel() = TrainerViewModel(
        LineupRepository(maps = listOf(MapInfo("m", "Map", 0)), lineups = lineups),
        progress,
        Random(1),
    )

    // Окно кадра у каждого вопроса своё и случайное, поэтому правильную точку берём из текущего вопроса.
    private fun TrainerViewModel.rightMarker(): Offset01 {
        val question = state.value.session!!.current!!
        return question.window.toLocal(question.lineup.aimPoint)
    }

    // Точка в противоположном углу окна: заведомо далеко от правильной.
    private fun TrainerViewModel.wrongMarker(): Offset01 {
        val right = rightMarker()
        return Offset01(if (right.x < 0.5f) 0.97f else 0.03f, 0.03f)
    }

    @Test
    fun startsOnSetupWithTSideAndAllLineupsOfIt() {
        val state = newViewModel().state.value
        assertNull(state.session)
        assertEquals(Side.T, state.side)
        assertEquals(3, state.availableCount)
    }

    @Test
    fun changingSideAndArea_updatesAvailableCount() {
        val vm = newViewModel()
        vm.onSideSelected(Side.CT)
        assertEquals(1, vm.state.value.availableCount)

        vm.onSideSelected(Side.T)
        vm.onAreaSelected(Area.A)
        assertEquals(2, vm.state.value.availableCount)

        vm.onAreaSelected(null)
        assertEquals(3, vm.state.value.availableCount)
    }

    @Test
    fun start_createsSessionOfDefaultLengthFromChosenPoolOnly() {
        val vm = newViewModel()
        vm.onAreaSelected(Area.A)
        vm.onStart()
        val session = vm.state.value.session
        assertNotNull(session)
        assertEquals(DEFAULT_SESSION_LENGTH, session!!.questions.size)
        assertTrue(session.questions.all { it.lineup.side == Side.T && it.lineup.area == Area.A })
    }

    @Test
    fun everyQuestion_getsItsOwnRandomWindowInsideTheFrame() {
        val vm = newViewModel()
        vm.onSideSelected(Side.CT) // один лайнап на все 10 вопросов
        vm.onStart()
        val windows = vm.state.value.session!!.questions.map { it.window }
        assertTrue("окна должны отличаться между вопросами", windows.toSet().size > 1)
        assertTrue(windows.all { it.left >= 0f && it.left + it.size <= 1f && it.top >= 0f && it.top + it.size <= 1f })
    }

    @Test
    fun windowSize_followsLineupZoom() {
        val zoomed = TrainerViewModel(
            LineupRepository(
                maps = listOf(MapInfo("m", "Map", 0)),
                lineups = listOf(lineup("zoom_4", Side.T, Area.A, zoom = 4f), lineup("zoom_default", Side.CT, Area.A)),
            ),
            progress,
            Random(1),
        )
        zoomed.onStart() // сторона T: только лайнап с зумом 4
        assertTrue(zoomed.state.value.session!!.questions.all { it.window.size == 0.25f })

        zoomed.onBackToSetup()
        zoomed.onSideSelected(Side.CT)
        zoomed.onStart()
        assertTrue(zoomed.state.value.session!!.questions.all { it.window.size == 0.5f })
    }

    @Test
    fun answerIsCheckedAgainstTheZoomedWindow() {
        val vm = TrainerViewModel(
            LineupRepository(
                maps = listOf(MapInfo("m", "Map", 0)),
                lineups = listOf(lineup("zoom_3", Side.T, Area.A, zoom = 3f)),
            ),
            progress,
            Random(1),
        )
        vm.onStart()
        vm.onConfirm(vm.rightMarker(), ThrowType.LMB)
        assertTrue(vm.state.value.session!!.results.single().hit)
    }

    @Test
    fun start_withEmptyPool_doesNothing() {
        val vm = newViewModel()
        vm.onSideSelected(Side.CT)
        vm.onAreaSelected(Area.A) // у CT нет лайнапов на A
        vm.onStart()
        assertNull(vm.state.value.session)
    }

    @Test
    fun correctAnswer_isHit_movesToNextQuestionAndShowsFeedback() {
        val vm = newViewModel()
        vm.onStart()
        val first = vm.state.value.session!!.current!!

        vm.onConfirm(vm.rightMarker(), ThrowType.LMB)

        val state = vm.state.value
        assertEquals(1, state.session!!.results.size)
        assertTrue(state.session!!.results.first().hit)
        assertEquals(first.lineup.id, state.session!!.results.first().lineup.id)
        assertEquals(true, state.feedback?.hit)
        assertEquals(1, progress.progress.value[first.lineup.id]?.correctStreak)
    }

    @Test
    fun wrongSpot_isMiss() {
        val vm = newViewModel()
        vm.onStart()
        vm.onConfirm(vm.wrongMarker(), ThrowType.LMB)
        assertFalse(vm.state.value.session!!.results.first().hit)
        assertEquals(false, vm.state.value.feedback?.hit)
    }

    @Test
    fun wrongThrowType_isMissEvenWithRightSpot() {
        val vm = newViewModel()
        vm.onStart()
        vm.onConfirm(vm.rightMarker(), ThrowType.RMB)
        assertFalse(vm.state.value.session!!.results.first().hit)
    }

    @Test
    fun session_finishesAfterAllQuestions_andCountsHits() {
        val vm = newViewModel()
        vm.onStart()
        repeat(DEFAULT_SESSION_LENGTH) { number ->
            // Каждый второй ответ неверный.
            if (number % 2 == 0) vm.onConfirm(vm.rightMarker(), ThrowType.LMB) else vm.onConfirm(vm.wrongMarker(), ThrowType.LMB)
        }
        val session = vm.state.value.session!!
        assertTrue(session.isFinished)
        assertEquals(DEFAULT_SESSION_LENGTH / 2, session.hits)
        assertNull(session.current)
    }

    @Test
    fun answersAfterSessionEnd_areIgnored() {
        val vm = newViewModel()
        vm.onStart()
        repeat(DEFAULT_SESSION_LENGTH) { vm.onConfirm(vm.rightMarker(), ThrowType.LMB) }
        // Сессия закончилась: следующего вопроса нет, лишние ответы игнорируются.
        repeat(3) { vm.onConfirm(Offset01(0.5f, 0.5f), ThrowType.LMB) }
        assertEquals(DEFAULT_SESSION_LENGTH, vm.state.value.session!!.results.size)
    }

    @Test
    fun lineupBecomesLearned_andIsReportedInSummary() {
        val vm = newViewModel()
        vm.onSideSelected(Side.CT) // одна карточка: ct_mid, она повторяется во всех вопросах
        vm.onStart()
        repeat(DEFAULT_SESSION_LENGTH) { vm.onConfirm(vm.rightMarker(), ThrowType.LMB) }

        val session = vm.state.value.session!!
        assertEquals(setOf("ct_mid"), session.learnedNow)
        assertEquals(1, session.newlyLearned)
    }

    @Test
    fun feedbackShown_clearsFeedback() {
        val vm = newViewModel()
        vm.onStart()
        vm.onConfirm(vm.rightMarker(), ThrowType.LMB)
        vm.onFeedbackShown()
        assertNull(vm.state.value.feedback)
    }

    @Test
    fun backToSetup_dropsSessionButKeepsSettings() {
        val vm = newViewModel()
        vm.onSideSelected(Side.T)
        vm.onAreaSelected(Area.B)
        vm.onStart()
        vm.onBackToSetup()
        val state = vm.state.value
        assertNull(state.session)
        assertEquals(Area.B, state.area)
    }
}
