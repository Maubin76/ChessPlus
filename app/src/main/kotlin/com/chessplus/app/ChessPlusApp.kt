package com.chessplus.app

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import com.chessplus.app.game.ChallengeKind
import com.chessplus.app.game.DailyDays
import com.chessplus.app.widget.ChessPlusWidget
import com.chessplus.app.game.GameMode
import com.chessplus.app.game.GameScreen
import com.chessplus.app.game.GameViewModel
import com.chessplus.app.game.TrainingCriteria
import com.chessplus.app.history.HistoryScreen
import com.chessplus.app.history.HistoryViewModel
import com.chessplus.app.home.HomeScreen
import com.chessplus.app.home.HomeViewModel
import com.chessplus.app.level.LevelScreen
import com.chessplus.app.navigation.BackStackEntry
import com.chessplus.app.navigation.Destination
import com.chessplus.app.navigation.NavigationViewModel
import com.chessplus.app.navigation.tab
import com.chessplus.app.settings.SettingsScreen
import com.chessplus.app.stats.StatsScreen
import com.chessplus.app.stats.StatsViewModel
import com.chessplus.app.stats.ThemeStatsScreen
import com.chessplus.app.stats.ThemeStatsViewModel
import com.chessplus.app.training.TargetedScreen
import com.chessplus.app.training.TargetedViewModel
import com.chessplus.app.ui.theme.ChessPlusTheme
import com.chessplus.core.model.ThemeSet
import kotlinx.coroutines.launch

/**
 * Racine de l'application : premier écran (choix du niveau ou accueil), pile de navigation,
 * retour système, et écran courant avec ses propres ViewModels.
 */
@Composable
fun ChessPlusApp(
	inContainer: AppContainer,
	inVersion: String,
	inOnExit: () -> Unit,
	inOnRestart: () -> Unit = {},
	inOpen: String? = null,
	inOnOpened: () -> Unit = {},
) {
	val vNavigation: NavigationViewModel = viewModel()
	val vSettings by inContainer.settingsStore.settings.collectAsStateWithLifecycle()
	val vScope = rememberCoroutineScope()
	LaunchedEffect(Unit) {
		if (!vNavigation.isInitialized) {
			vNavigation.initialize(if (inContainer.hasPlayer()) Destination.Home else Destination.Level(firstLaunch = true))
		}
	}
	// Widget : ouvre le puzzle du jour une fois la navigation prête (et le joueur créé).
	LaunchedEffect(inOpen, vNavigation.isInitialized) {
		if (inOpen == ChessPlusWidget.kOpenDaily && vNavigation.isInitialized && inContainer.hasPlayer()) {
			val vDay = DailyDays.keyOf(System.currentTimeMillis())
			inContainer.dailyStore.puzzleFor(vDay)?.let { vNavigation.navigate(Destination.Game(GameMode.Daily(vDay, it.id))) }
			inOnOpened()
		}
	}
	val vEntry = vNavigation.backStack.lastOrNull()
	if (vEntry == null) {
		Box(Modifier.fillMaxSize().background(ChessPlusTheme.colors.surface), contentAlignment = Alignment.Center) {
			Text(stringResource(R.string.loading), style = ChessPlusTheme.typography.body, color = ChessPlusTheme.colors.inkMuted)
		}
		return
	}
	BackHandler(enabled = vNavigation.backStack.size > 1) { vNavigation.back() }
	val vBack: () -> Unit = { if (!vNavigation.back()) inOnExit() }
	val vTransition = updateTransition(vEntry, label = "écran")
	LaunchedEffect(vTransition.currentState, vTransition.targetState) {
		if (vTransition.currentState == vTransition.targetState) {
			vNavigation.clearRetired()
		}
	}
	vTransition.AnimatedContent(
		transitionSpec = { screenTransition(initialState, targetState) },
		contentKey = { it.id },
	) { inEntry ->
		CompositionLocalProvider(LocalViewModelStoreOwner provides inEntry) {
			key(inEntry.id) {
				when (val vDestination = inEntry.destination) {
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
						inOnDaily = { vNavigation.navigate(Destination.Game(GameMode.Daily(it.day, it.puzzleId))) },
						inOnArcade = { vNavigation.navigate(Destination.Game(GameMode.Continuous)) },
						inOnTraining = { vNavigation.navigate(Destination.Targeted) },
						inOnReview = { vNavigation.navigate(Destination.Game(GameMode.Review)) },
						inOnChallenge = { vNavigation.navigate(Destination.Game(GameMode.Challenge(it))) },
						inOnTab = vNavigation::selectTab,
					)
					Destination.History -> HistoryScreen(
						inViewModel = viewModel(factory = HistoryViewModel.factory(inContainer)),
						inOnReplay = { vNavigation.navigate(Destination.Game(GameMode.Replay(it))) },
						inOnBack = vBack,
					)
					Destination.Stats -> StatsScreen(
						inViewModel = viewModel(factory = StatsViewModel.factory(inContainer)),
						inOnTheme = { vNavigation.navigate(Destination.ThemeStats(it)) },
						inOnTrainThemes = { vNavigation.navigate(Destination.Game(GameMode.Targeted(TrainingCriteria(themes = ThemeSet.of(it))))) },
						inOnHistory = { vNavigation.navigate(Destination.History) },
						inOnTab = vNavigation::selectTab,
					)
					is Destination.ThemeStats -> ThemeStatsScreen(
						inViewModel = viewModel(factory = ThemeStatsViewModel.factory(inContainer, vDestination.theme)),
						inTheme = vDestination.theme,
						inOnBack = vBack,
						inOnTrain = { vNavigation.navigate(Destination.Game(GameMode.Targeted(TrainingCriteria(themes = ThemeSet.of(vDestination.theme))))) },
						inOnReplay = { vNavigation.navigate(Destination.Game(GameMode.Replay(it))) },
					)
					Destination.Settings -> SettingsScreen(
						inStore = inContainer.settingsStore,
						inVersion = inVersion,
						inLoadBaseInfo = inContainer::offlineBaseInfo,
						inOnLevel = { vNavigation.navigate(Destination.Level(firstLaunch = false)) },
						inOnTab = vNavigation::selectTab,
						inExport = inContainer::exportProgress,
						inStageImport = inContainer::stageImport,
						inRestart = inOnRestart,
					)
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
								GameMode.Review -> R.string.game_title_review
								is GameMode.Targeted -> R.string.game_title_targeted
								is GameMode.Replay -> R.string.game_title_replay
								is GameMode.Daily -> R.string.game_title_daily
								is GameMode.Challenge -> when (vDestination.mode.kind) {
									ChallengeKind.Survival -> R.string.game_title_survival
									ChallengeKind.Rush -> R.string.game_title_rush
								}
							}
						),
						inVibration = vSettings.vibration,
						inSounds = vSettings.sounds,
						inCoordinates = vSettings.coordinates,
						inOnBack = vBack,
					)
				}
			}
		}
	}
}

/** Durée des transitions entre écrans. */
private const val kTransitionMs = 280

/**
 * Transition entre deux écrans : fondu entre onglets ; sinon glissement et fondu, vers la gauche
 * en avançant, vers la droite en revenant.
 */
private fun screenTransition(inFrom: BackStackEntry, inTo: BackStackEntry): ContentTransform {
	if (inFrom.destination.tab() != null && inTo.destination.tab() != null) {
		return fadeIn(tween(kTransitionMs)) togetherWith fadeOut(tween(kTransitionMs / 2))
	}
	val vDirection = if (inTo.id > inFrom.id) 1 else -1
	return (slideInHorizontally(tween(kTransitionMs, easing = FastOutSlowInEasing)) { inWidth -> vDirection * inWidth / 4 } + fadeIn(tween(kTransitionMs))) togetherWith
		(slideOutHorizontally(tween(kTransitionMs, easing = FastOutSlowInEasing)) { inWidth -> -vDirection * inWidth / 8 } + fadeOut(tween(kTransitionMs / 2)))
}
