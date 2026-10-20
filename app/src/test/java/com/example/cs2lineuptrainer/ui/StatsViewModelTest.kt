package com.example.cs2lineuptrainer.ui

import com.example.cs2lineuptrainer.data.InMemoryProgressRepository
import com.example.cs2lineuptrainer.data.LineupRepository
import com.example.cs2lineuptrainer.domain.Area
import com.example.cs2lineuptrainer.domain.GrenadeType
import com.example.cs2lineuptrainer.domain.Lineup
import com.example.cs2lineuptrainer.domain.LineupMedia
import com.example.cs2lineuptrainer.domain.LineupProgress
import com.example.cs2lineuptrainer.domain.MapInfo
import com.example.cs2lineuptrainer.domain.NamedPoint
import com.example.cs2lineuptrainer.domain.Offset01
import com.example.cs2lineuptrainer.domain.Side
import com.example.cs2lineuptrainer.domain.ThrowType
import com.example.cs2lineuptrainer.ui.LineupRequests
import com.example.cs2lineuptrainer.ui.stats.LineupStatus
import com.example.cs2lineuptrainer.ui.stats.StatsUiState
import com.example.cs2lineuptrainer.ui.stats.StatsViewModel
import com.example.cs2lineuptrainer.ui.stats.computeStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StatsViewModelTest {

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun lineup(id: String, grenade: GrenadeType) = Lineup(
        id = id,
        mapId = "m",
        grenade = grenade,
        side = Side.T,
        area = Area.A,
        title = id,
        target = NamedPoint("t_$id", "t", 0.3f, 0.3f),
        origin = NamedPoint("o_$id", "o", 0.6f, 0.6f),
        standView = LineupMedia.Image(0),
        aimPoint = Offset01(0.5f, 0.5f),
        throwType = ThrowType.LMB,
        howTo = LineupMedia.Image(0),
        description = "",
    )

    private val lineups = listOf(
        lineup("smoke_1", GrenadeType.SMOKE),
        lineup("smoke_2", GrenadeType.SMOKE),
        lineup("flash_1", GrenadeType.FLASH),
    )

    private fun statusOf(state: StatsUiState, id: String) =
        state.items.single { it.lineup.id == id }.status

    // ---------- Чистая функция computeStats ----------

    @Test
    fun withoutProgress_everythingIsNew() {
        val state = computeStats("Map", lineups, emptyMap(), null)
        assertEquals(0, state.learned)
        assertEquals(3, state.total)
        assertTrue(state.items.all { it.status == LineupStatus.NEW })
    }

    @Test
    fun statuses_followProgress() {
        val progress = mapOf(
            "smoke_1" to LineupProgress("smoke_1", correctStreak = 3, learned = true),
            "smoke_2" to LineupProgress("smoke_2", correctStreak = 1),
        )
        val state = computeStats("Map", lineups, progress, null)
        assertEquals(LineupStatus.LEARNED, statusOf(state, "smoke_1"))
        assertEquals(LineupStatus.LEARNING, statusOf(state, "smoke_2"))
        assertEquals(1, state.items.single { it.lineup.id == "smoke_2" }.correctStreak)
        assertEquals(LineupStatus.NEW, statusOf(state, "flash_1"))
        assertEquals(1, state.learned)
    }

    @Test
    fun byGrenade_countsLearnedPerType_andSkipsTypesWithoutLineups() {
        val progress = mapOf("smoke_1" to LineupProgress("smoke_1", learned = true))
        val state = computeStats("Map", lineups, progress, null)
        assertEquals(listOf(GrenadeType.SMOKE, GrenadeType.FLASH), state.byGrenade.map { it.grenade })
        assertEquals(1, state.byGrenade.first { it.grenade == GrenadeType.SMOKE }.learned)
        assertEquals(2, state.byGrenade.first { it.grenade == GrenadeType.SMOKE }.total)
        assertEquals(0, state.byGrenade.first { it.grenade == GrenadeType.FLASH }.learned)
    }

    @Test
    fun filter_limitsListButNotTheOverallProgress() {
        val progress = mapOf("smoke_1" to LineupProgress("smoke_1", learned = true))
        val state = computeStats("Map", lineups, progress, GrenadeType.FLASH)
        assertEquals(listOf("flash_1"), state.items.map { it.lineup.id })
        assertEquals(3, state.total)
        assertEquals(1, state.learned)
    }

    // Порядок списка: не тренировался -> учится -> выучен.
    @Test
    fun items_areOrderedNewThenLearningThenLearned() {
        val progress = mapOf(
            "smoke_1" to LineupProgress("smoke_1", learned = true),
            "flash_1" to LineupProgress("flash_1", correctStreak = 1),
        )
        val state = computeStats("Map", lineups, progress, null)
        assertEquals(listOf("smoke_2", "flash_1", "smoke_1"), state.items.map { it.lineup.id })
        assertEquals(
            listOf(LineupStatus.NEW, LineupStatus.LEARNING, LineupStatus.LEARNED),
            state.items.map { it.status },
        )
    }

    @Test
    fun noLineups_givesEmptyState() {
        val state = computeStats("Map", emptyList(), emptyMap(), null)
        assertEquals(0, state.total)
        assertTrue(state.items.isEmpty())
        assertTrue(state.byGrenade.isEmpty())
    }

    // ---------- StatsViewModel ----------

    private val progress = InMemoryProgressRepository()
    private val requests = LineupRequests()

    private fun newViewModel() = StatsViewModel(
        LineupRepository(maps = listOf(MapInfo("m", "Map", 0)), lineups = lineups),
        progress,
        requests,
    )

    @Test
    fun viewModel_startsWithNothingLearned() {
        val state = newViewModel().state.value
        assertEquals("Map", state.mapName)
        assertEquals(0, state.learned)
        assertEquals(3, state.total)
    }

    // После тренировки (прогресс изменился) статистика обновляется сама.
    @Test
    fun viewModel_updatesWhenProgressChanges() = runBlocking {
        val vm = newViewModel()
        repeat(3) { progress.recordAttempt("smoke_1", hit = true) }
        assertEquals(1, vm.state.value.learned)
        assertEquals(LineupStatus.LEARNED, statusOf(vm.state.value, "smoke_1"))
    }

    @Test
    fun viewModel_filterAppliesAndResets() {
        val vm = newViewModel()
        vm.onFilterSelected(GrenadeType.SMOKE)
        assertEquals(2, vm.state.value.items.size)
        vm.onFilterSelected(null)
        assertEquals(3, vm.state.value.items.size)
    }

    // ---------- Переходы между Статистикой и Поиском ----------

    @Test
    fun tapOnLineup_requestsOpeningItInExplorer() {
        newViewModel().onLineupClick("flash_1")
        assertEquals("flash_1", requests.openInExplorer.value)
    }

    @Test
    fun requestFromExplorer_selectsItsGrenade_andFocusesLineup() {
        val vm = newViewModel()
        requests.requestShowInStats("flash_1")
        val state = vm.state.value
        assertEquals(GrenadeType.FLASH, state.filter)
        assertEquals("flash_1", state.focusedLineupId)
        assertEquals(listOf("flash_1"), state.items.map { it.lineup.id })
        assertEquals(null, requests.showInStats.value) // запрос забран
    }

    @Test
    fun requestWithUnknownId_isIgnoredButConsumed() {
        val vm = newViewModel()
        requests.requestShowInStats("no_such_lineup")
        assertEquals(null, vm.state.value.filter)
        assertEquals(null, vm.state.value.focusedLineupId)
        assertEquals(null, requests.showInStats.value)
    }

    @Test
    fun focus_isClearedWhenHandledOrWhenFilterChanges() {
        val vm = newViewModel()
        requests.requestShowInStats("smoke_1")
        vm.onFocusHandled()
        assertEquals(null, vm.state.value.focusedLineupId)

        requests.requestShowInStats("smoke_1")
        vm.onFilterSelected(GrenadeType.FLASH)
        assertEquals(null, vm.state.value.focusedLineupId)
    }
}
