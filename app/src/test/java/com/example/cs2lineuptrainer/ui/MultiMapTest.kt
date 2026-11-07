package com.example.cs2lineuptrainer.ui

import com.example.cs2lineuptrainer.data.InMemoryProgressRepository
import com.example.cs2lineuptrainer.data.LineupRepository
import com.example.cs2lineuptrainer.data.MapSelection
import com.example.cs2lineuptrainer.domain.Area
import com.example.cs2lineuptrainer.domain.GrenadeType
import com.example.cs2lineuptrainer.domain.Lineup
import com.example.cs2lineuptrainer.domain.LineupMedia
import com.example.cs2lineuptrainer.domain.MapInfo
import com.example.cs2lineuptrainer.domain.NamedPoint
import com.example.cs2lineuptrainer.domain.Offset01
import com.example.cs2lineuptrainer.domain.Side
import com.example.cs2lineuptrainer.domain.ThrowType
import com.example.cs2lineuptrainer.ui.explorer.ExplorerViewModel
import com.example.cs2lineuptrainer.ui.stats.StatsViewModel
import com.example.cs2lineuptrainer.ui.trainer.TrainerViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.random.Random

// Несколько карт: выбранная карта общая для экранов, а при её смене каждый экран переключается на новую.
@OptIn(ExperimentalCoroutinesApi::class)
class MultiMapTest {

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun lineup(mapId: String, id: String, side: Side = Side.T, area: Area = Area.A) = Lineup(
        id = id,
        mapId = mapId,
        grenade = GrenadeType.SMOKE,
        side = side,
        area = area,
        title = id,
        target = NamedPoint("t_$id", "t", 0.3f, 0.3f),
        origin = NamedPoint("o_$id", "o", 0.6f, 0.6f),
        standView = LineupMedia.Image(0),
        aimPoint = Offset01(0.5f, 0.5f),
        throwType = ThrowType(),
        howTo = LineupMedia.Image(0),
        description = "",
    )

    private val lineups = listOf(
        lineup("m1", "m1_a", Side.T, Area.A),
        lineup("m1", "m1_b", Side.CT, Area.B),
        lineup("m2", "m2_mid", Side.T, Area.MID),
    )

    private val repository = LineupRepository(
        maps = listOf(MapInfo("m1", "First", 0), MapInfo("m2", "Second", 0)),
        lineups = lineups,
    )

    private val selection = MapSelection("m1")

    @Test
    fun selection_startsWithGivenMapAndChanges() {
        assertEquals("m1", selection.selectedMapId.value)
        selection.select("m2")
        assertEquals("m2", selection.selectedMapId.value)
    }

    @Test
    fun explorer_switchingMap_showsTheOtherMapFromScratch() {
        val vm = ExplorerViewModel(repository, LineupRequests(), selection)
        vm.onGrenadeSelected(GrenadeType.SMOKE)
        assertEquals(setOf("t_m1_a", "t_m1_b"), vm.state.value.targets.map { it.id }.toSet())

        selection.select("m2")
        assertEquals("m2", vm.state.value.map.id)
        assertNull(vm.state.value.grenade)

        vm.onGrenadeSelected(GrenadeType.SMOKE)
        assertEquals(setOf("t_m2_mid"), vm.state.value.targets.map { it.id }.toSet())
    }

    @Test
    fun trainer_switchingMap_resetsAreaKeepsSideAndUsesNewLineups() {
        val vm = TrainerViewModel(repository, InMemoryProgressRepository(), Random(1), selection)
        assertEquals(listOf(Area.A, Area.B), vm.state.value.areas)
        vm.onAreaSelected(Area.A)

        selection.select("m2")
        val state = vm.state.value
        assertEquals("m2", state.map.id)
        assertEquals(listOf(Area.MID), state.areas)
        assertNull(state.area)
        assertEquals(Side.T, state.side)
        assertEquals(1, state.availableCount)
    }

    @Test
    fun trainer_switchingMap_dropsRunningSession() {
        val vm = TrainerViewModel(repository, InMemoryProgressRepository(), Random(1), selection)
        vm.onStart()
        assertTrue(vm.state.value.session != null)
        selection.select("m2")
        assertNull(vm.state.value.session)
    }

    // «Любая сторона»: тренируются и гранаты T, и гранаты CT.
    @Test
    fun trainer_anySide_mixesBothSides() {
        val vm = TrainerViewModel(repository, InMemoryProgressRepository(), Random(1), selection)
        vm.onSideSelected(null)
        assertNull(vm.state.value.side)
        assertEquals(2, vm.state.value.availableCount)

        vm.onStart()
        val sides = vm.state.value.session!!.questions.map { it.lineup.side }.toSet()
        assertEquals(setOf(Side.T, Side.CT), sides)
    }

    @Test
    fun stats_switchingMap_showsTheOtherMap() {
        val vm = StatsViewModel(repository, InMemoryProgressRepository(), LineupRequests(), selection)
        assertEquals("First", vm.state.value.mapName)
        assertEquals(2, vm.state.value.total)

        selection.select("m2")
        assertEquals("Second", vm.state.value.mapName)
        assertEquals(1, vm.state.value.total)
        assertEquals(listOf("m2_mid"), vm.state.value.items.map { it.lineup.id })
    }
}
