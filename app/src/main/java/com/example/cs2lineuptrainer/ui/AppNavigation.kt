package com.example.cs2lineuptrainer.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.cs2lineuptrainer.ui.components.MapMenu
import com.example.cs2lineuptrainer.ui.explorer.ExplorerScreen
import com.example.cs2lineuptrainer.ui.explorer.ExplorerViewModel
import com.example.cs2lineuptrainer.ui.stats.StatsScreen
import com.example.cs2lineuptrainer.ui.stats.StatsViewModel
import com.example.cs2lineuptrainer.ui.trainer.TrainerScreen
import com.example.cs2lineuptrainer.ui.trainer.TrainerViewModel

// Три раздела приложения: маршрут, подпись и значок на панели навигации.
private enum class Tab(val route: String, val label: String, val icon: ImageVector) {
    EXPLORER("explorer", "Поиск", Icons.Default.Search),
    TRAINER("trainer", "Тренировка", Icons.Default.PlayArrow),
    STATS("stats", "Статистика", Icons.Default.Star),
}

// Переключает раздел, сохраняя состояние остальных (чтобы выбор в Поиске не терялся при уходе на другую вкладку).
private fun NavHostController.openTab(tab: Tab) {
    navigate(tab.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

// Панель навигации и область, где показывается выбранный экран.
// Портрет: панель снизу. Альбом: панель слева (так экрану остаётся больше высоты).
// Дерево экранов (NavHost) стоит в одном и том же месте в обеих ориентациях. Это важно: ключ,
// под которым rememberSaveable хранит значения, зависит от положения в дереве, и если бы NavHost
// "переезжал" при повороте, сохранённые значения (например, маркер в тренировке) не находились бы.
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val landscape = isLandscape()
    val mapViewModel: MapViewModel = viewModel(factory = MapViewModel.Factory)
    val mapState by mapViewModel.state.collectAsStateWithLifecycle()

    // В альбоме отступы под системные панели и вырез камеры считаем сами (Scaffold их тогда не добавляет),
    // чтобы они не учитывались дважды.
    val safeArea = WindowInsets.systemBars.union(WindowInsets.displayCutout)

    Scaffold(
        bottomBar = { if (!landscape) BottomBar(currentRoute, navController) },
        contentWindowInsets = if (landscape) WindowInsets(0, 0, 0, 0) else ScaffoldDefaults.contentWindowInsets,
    ) { innerPadding ->
        Row(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            SideRail(landscape, currentRoute, navController, safeArea, mapState, mapViewModel::onMapSelected)

            // В альбоме содержимому нужны отступы сверху, снизу и справа (там может быть камера).
            val contentInsets = if (landscape) {
                Modifier.windowInsetsPadding(
                    safeArea.only(WindowInsetsSides.Top + WindowInsetsSides.Bottom + WindowInsetsSides.End),
                )
            } else {
                Modifier
            }

            // MapBar в портрете рисует строку выбора карты над экраном, в альбоме ничего (там выбор в боковой панели),
            // но стоит на месте в обеих ориентациях, как и NavHost под ним.
            Column(modifier = Modifier.weight(1f).then(contentInsets)) {
                MapBar(landscape, mapState, mapViewModel::onMapSelected)
                NavHost(
                    navController = navController,
                    startDestination = Tab.EXPLORER.route,
                    modifier = Modifier.weight(1f),
                ) {
                    composable(Tab.EXPLORER.route) {
                        val viewModel: ExplorerViewModel = viewModel(factory = ExplorerViewModel.Factory)
                        ExplorerScreen(viewModel, onOpenStats = { navController.openTab(Tab.STATS) })
                    }
                    composable(Tab.TRAINER.route) {
                        val viewModel: TrainerViewModel = viewModel(factory = TrainerViewModel.Factory)
                        TrainerScreen(viewModel)
                    }
                    composable(Tab.STATS.route) {
                        val viewModel: StatsViewModel = viewModel(factory = StatsViewModel.Factory)
                        StatsScreen(viewModel, onOpenExplorer = { navController.openTab(Tab.EXPLORER) })
                    }
                }
            }
        }
    }
}

// Строка выбора карты над экраном (портрет). Если карта одна или альбом, ничего не рисует.
@Composable
private fun MapBar(landscape: Boolean, mapState: MapPickerState, onSelect: (String) -> Unit) {
    if (landscape || mapState.maps.size < 2) return
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.End,
    ) {
        MapMenu(mapState.maps, mapState.selectedId, onSelect)
    }
}

// Нижняя панель (портрет).
@Composable
private fun BottomBar(currentRoute: String?, navController: NavHostController) {
    NavigationBar {
        Tab.entries.forEach { tab ->
            NavigationBarItem(
                selected = currentRoute == tab.route,
                onClick = { navController.openTab(tab) },
                icon = { Icon(tab.icon, contentDescription = null) },
                label = { Text(tab.label) },
            )
        }
    }
}

// Боковая панель (альбом). В портрете ничего не рисует, но остаётся на своём месте в дереве.
// Фон панели доходит до краёв экрана, а значки сдвигаются от выреза камеры (если она слева).
@Composable
private fun SideRail(
    landscape: Boolean,
    currentRoute: String?,
    navController: NavHostController,
    safeArea: WindowInsets,
    mapState: MapPickerState,
    onMapSelected: (String) -> Unit,
) {
    if (!landscape) return
    NavigationRail(
        windowInsets = safeArea.only(WindowInsetsSides.Start + WindowInsetsSides.Vertical),
        header = {
            if (mapState.maps.size > 1) MapMenu(mapState.maps, mapState.selectedId, onMapSelected)
        },
    ) {
        Tab.entries.forEach { tab ->
            NavigationRailItem(
                selected = currentRoute == tab.route,
                onClick = { navController.openTab(tab) },
                icon = { Icon(tab.icon, contentDescription = null) },
                label = { Text(tab.label) },
            )
        }
    }
}
