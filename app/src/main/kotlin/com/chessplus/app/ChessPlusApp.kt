package com.chessplus.app

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chessplus.app.game.GameMode
import com.chessplus.app.game.GameScreen
import com.chessplus.app.game.GameViewModel
import com.chessplus.app.history.HistoryScreen
import com.chessplus.app.history.HistoryViewModel
import com.chessplus.app.home.HomeScreen
import com.chessplus.app.home.HomeViewModel
import com.chessplus.app.level.LevelScreen
import com.chessplus.app.navigation.Destination
import com.chessplus.app.navigation.NavigationViewModel
import com.chessplus.app.settings.SettingsScreen
import com.chessplus.app.stats.StatsScreen
import com.chessplus.app.stats.StatsViewModel
import com.chessplus.app.training.TargetedScreen
import com.chessplus.app.training.TargetedViewModel
import com.chessplus.app.ui.theme.ChessType
import com.chessplus.app.ui.theme.kLocalChessColors
import kotlinx.coroutines.launch

/**
 * Racine de l'application : premier écran (choix du niveau ou accueil), pile de navigation,
 * retour système, et écran courant avec ses propres ViewModels.
 */
@Composable
fun ChessPlusApp(inContainer: AppContainer, inVersion: String, inOnExit: () -> Unit) {
	val vNavigation: NavigationViewModel = viewModel()
	val vSettings by inContainer.settingsStore.settings.collectAsStateWithLifecycle()
	val vScope = rememberCoroutineScope()
	LaunchedEffect(Unit) {
		if (!vNavigation.isInitialized) {
			vNavigation.initialize(if (inContainer.hasPlayer()) Destination.Home else Destination.Level(firstLaunch = true))
		}
	}
	val vEntry = vNavigation.backStack.lastOrNull()
	if (vEntry == null) {
		Box(Modifier.fillMaxSize().background(kLocalChessColors.current.background), contentAlignment = Alignment.Center) {
			Text(stringResource(R.string.loading), style = ChessType.kBody, color = kLocalChessColors.current.muted)
		}
		return
	}
	BackHandler(enabled = vNavigation.backStack.size > 1) { vNavigation.back() }
	val vBack: () -> Unit = { if (!vNavigation.back()) inOnExit() }
	CompositionLocalProvider(LocalViewModelStoreOwner provides vEntry) {
		key(vEntry.id) {
			when (val vDestination = vEntry.destination) {
				is Destination.Level -> LevelScreen(
					inFirstLaunch = vDestination.firstLaunch,
					inOnBack = vBack,
					inOnChoose = { inRating ->
						vScope.launch {
							inContainer.setLevel(inRating)
							if (vDestination.firstLaunch) vNavigation.replace(Destination.Home) else vNavigation.back()
						}
					},
				)
				Destination.Home -> HomeScreen(
					inViewModel = viewModel(factory = HomeViewModel.factory(inContainer)),
					inOnTrain = { vNavigation.navigate(Destination.Game(GameMode.Continuous)) },
					inOnTargeted = { vNavigation.navigate(Destination.Targeted) },
					inOnSettings = { vNavigation.navigate(Destination.Settings) },
					inOnTab = vNavigation::selectTab,
				)
				Destination.History -> HistoryScreen(
					inViewModel = viewModel(factory = HistoryViewModel.factory(inContainer)),
					inOnReplay = { vNavigation.navigate(Destination.Game(GameMode.Replay(it))) },
					inOnTab = vNavigation::selectTab,
				)
				Destination.Stats -> StatsScreen(viewModel(factory = StatsViewModel.factory(inContainer)), vNavigation::selectTab)
				Destination.Settings -> SettingsScreen(inContainer.settingsStore, inVersion, vBack) { vNavigation.navigate(Destination.Level(firstLaunch = false)) }
				Destination.Targeted -> TargetedScreen(
					inViewModel = viewModel(factory = TargetedViewModel.factory(inContainer)),
					inOnBack = vBack,
					inOnStart = { vNavigation.navigate(Destination.Game(GameMode.Targeted(it))) },
				)
				is Destination.Game -> GameScreen(
					inViewModel = viewModel(factory = GameViewModel.factory(inContainer, vDestination.mode)),
					inTitle = stringResource(
						when (vDestination.mode) {
							GameMode.Continuous -> R.string.game_title
							is GameMode.Targeted -> R.string.game_title_targeted
							is GameMode.Replay -> R.string.game_title_replay
						}
					),
					inVibration = vSettings.vibration,
					inCoordinates = vSettings.coordinates,
					inOnBack = vBack,
				)
			}
		}
	}
}
