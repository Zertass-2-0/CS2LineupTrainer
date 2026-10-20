package com.example.cs2lineuptrainer.ui

import com.example.cs2lineuptrainer.data.LineupRepository
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

// Тесты работают на собственных тестовых данных и не зависят от настоящих лайнапов.
@OptIn(ExperimentalCoroutinesApi::class)
class ExplorerViewModelTest {

    // ViewModel слушает запросы из Статистики в viewModelScope, а он работает на главном потоке Android.
    // В тестах подменяем его на тестовый диспетчер.
    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private val aSite = NamedPoint("a_site", "A site", 0.3f, 0.2f)
    private val heaven = NamedPoint("heaven", "Heaven", 0.2f, 0.25f)
    private val bSite = NamedPoint("b_site", "B site", 0.3f, 0.8f)
    private val garage = NamedPoint("garage", "Garage", 0.7f, 0.3f)
    private val tMid = NamedPoint("t_mid", "T mid", 0.8f, 0.5f)

    private fun lineup(id: String, grenade: GrenadeType, target: NamedPoint, origin: NamedPoint) = Lineup(
        id = id,
        mapId = "m",
        grenade = grenade,
        side = Side.T,
        area = Area.A,
        title = id,
        target = target,
        origin = origin,
        standView = LineupMedia.Image(0),
        aimPoint = Offset01(0.5f, 0.5f),
        throwType = ThrowType.LMB,
        howTo = LineupMedia.Image(0),
        description = "",
    )

    // Две позиции на A site, одна позиция (Garage) на две цели, плюс флэш на другую цель.
    private val lineups = listOf(
        lineup("smoke_a_garage", GrenadeType.SMOKE, aSite, garage),
        lineup("smoke_a_tmid", GrenadeType.SMOKE, aSite, tMid),
        lineup("smoke_heaven_garage", GrenadeType.SMOKE, heaven, garage),
        lineup("flash_b_tmid", GrenadeType.FLASH, bSite, tMid),
    )

    private val requests = LineupRequests()

    private fun newViewModel() = ExplorerViewModel(
        LineupRepository(maps = listOf(MapInfo("m", "Map", 0)), lineups = lineups),
        requests,
    )

    @Test
    fun startsWithNothingSelected() {
        val state = newViewModel().state.value
        assertNull(state.grenade)
        assertTrue(state.targets.isEmpty())
    }

    @Test
    fun selectingGrenade_showsOnlyTargetsOfThatType() {
        val vm = newViewModel()
        vm.onGrenadeSelected(GrenadeType.SMOKE)
        val state = vm.state.value
        assertEquals(GrenadeType.SMOKE, state.grenade)
        assertEquals(setOf("a_site", "heaven"), state.targets.map { it.id }.toSet())
    }

    @Test
    fun selectingSameGrenadeTwice_resetsSelection() {
        val vm = newViewModel()
        vm.onGrenadeSelected(GrenadeType.SMOKE)
        vm.onGrenadeSelected(GrenadeType.SMOKE)
        assertNull(vm.state.value.grenade)
    }

    @Test
    fun selectingTarget_showsAllPositionsForIt() {
        val vm = newViewModel()
        vm.onGrenadeSelected(GrenadeType.SMOKE)
        vm.onTargetSelected("a_site")
        val state = vm.state.value
        assertEquals("a_site", state.selectedTargetId)
        assertEquals(setOf("garage", "t_mid"), state.lineupsForTarget.map { it.origin.id }.toSet())
    }

    @Test
    fun samePosition_servesDifferentTargets() {
        val vm = newViewModel()
        vm.onGrenadeSelected(GrenadeType.SMOKE)

        vm.onTargetSelected("a_site")
        val garageToASite = vm.state.value.lineupsForTarget.single { it.origin.id == "garage" }

        vm.onTargetSelected("heaven")
        val garageToHeaven = vm.state.value.lineupsForTarget.single { it.origin.id == "garage" }

        // Позиция Garage одна и та же, а лайнапы разные.
        assertNotEquals(garageToASite.id, garageToHeaven.id)
        assertEquals(garageToASite.origin, garageToHeaven.origin)
    }

    @Test
    fun selectingAnotherTarget_replacesPositions() {
        val vm = newViewModel()
        vm.onGrenadeSelected(GrenadeType.SMOKE)
        vm.onTargetSelected("a_site")
        vm.onTargetSelected("heaven")
        val origins = vm.state.value.lineupsForTarget.map { it.origin.id }
        assertEquals(listOf("garage"), origins)
    }

    @Test
    fun selectingLineup_opensDetails() {
        val vm = newViewModel()
        vm.onGrenadeSelected(GrenadeType.SMOKE)
        vm.onTargetSelected("a_site")
        vm.onLineupSelected(vm.state.value.lineupsForTarget.first().id)
        assertNotNull(vm.state.value.selectedLineup)
    }

    @Test
    fun back_walksStepByStepToTheStart() {
        val vm = newViewModel()
        vm.onGrenadeSelected(GrenadeType.SMOKE)
        vm.onTargetSelected("a_site")
        vm.onLineupSelected(vm.state.value.lineupsForTarget.first().id)

        vm.onBack()
        assertNull(vm.state.value.selectedLineup)
        assertEquals("a_site", vm.state.value.selectedTargetId)

        vm.onBack()
        assertNull(vm.state.value.selectedTargetId)
        assertEquals(GrenadeType.SMOKE, vm.state.value.grenade)

        vm.onBack()
        assertNull(vm.state.value.grenade)

        vm.onBack() // на старте ничего не ломается
        assertNull(vm.state.value.grenade)
    }

    // ---------- Переходы между Статистикой и Поиском ----------

    @Test
    fun openLineup_selectsGrenadeTargetAndPosition_andOpensDetails() {
        val vm = newViewModel()
        vm.openLineup("smoke_heaven_garage")
        val state = vm.state.value
        assertEquals(GrenadeType.SMOKE, state.grenade)
        assertEquals("heaven", state.selectedTargetId)
        assertEquals("smoke_heaven_garage", state.selectedLineup?.id)
        assertEquals(setOf("heaven", "a_site"), state.targets.map { it.id }.toSet())
        assertEquals(listOf("smoke_heaven_garage"), state.lineupsForTarget.map { it.id })
    }

    @Test
    fun openLineup_withUnknownId_changesNothing() {
        val vm = newViewModel()
        vm.onGrenadeSelected(GrenadeType.FLASH)
        val before = vm.state.value
        vm.openLineup("no_such_lineup")
        assertEquals(before, vm.state.value)
    }

    @Test
    fun requestFromStats_opensLineup_andIsConsumedOnce() {
        val vm = newViewModel()
        requests.requestOpenInExplorer("flash_b_tmid")
        assertEquals("flash_b_tmid", vm.state.value.selectedLineup?.id)
        assertNull(requests.openInExplorer.value)
    }

    @Test
    fun openingTheSameLineupTwice_worksBothTimes() {
        val vm = newViewModel()
        requests.requestOpenInExplorer("smoke_a_garage")
        vm.onBack() // игрок закрыл инструкцию
        requests.requestOpenInExplorer("smoke_a_garage")
        assertEquals("smoke_a_garage", vm.state.value.selectedLineup?.id)
    }

    @Test
    fun showInStats_closesDetailsKeepsSelection_andRequestsFocus() {
        val vm = newViewModel()
        vm.openLineup("smoke_a_garage")
        vm.showInStats("smoke_a_garage")
        val state = vm.state.value
        assertNull(state.selectedLineup)
        assertEquals("a_site", state.selectedTargetId)
        assertEquals("smoke_a_garage", requests.showInStats.value)
    }
}
