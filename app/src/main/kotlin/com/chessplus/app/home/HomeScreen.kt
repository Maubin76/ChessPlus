package com.chessplus.app.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.chessplus.app.AppContainer
import com.chessplus.app.R
import com.chessplus.app.game.ChallengeKind
import com.chessplus.app.game.DailyDays
import com.chessplus.app.goal.GoalProgress
import com.chessplus.app.stats.StatsCalculator
import com.chessplus.app.ui.components.Chip
import com.chessplus.app.ui.components.FloatingNavBar
import com.chessplus.app.ui.components.ListGroup
import com.chessplus.app.ui.components.ListRow
import com.chessplus.app.ui.components.MiniBoard
import com.chessplus.app.ui.components.NavTab
import com.chessplus.app.ui.components.Overline
import com.chessplus.app.ui.components.PageTitle
import com.chessplus.app.ui.components.PrimaryButton
import com.chessplus.app.ui.components.ProgressBar
import com.chessplus.app.ui.components.SecondaryButton
import com.chessplus.app.ui.components.SurfaceCard
import com.chessplus.app.ui.components.formatRating
import com.chessplus.app.ui.components.kIconSize
import com.chessplus.app.ui.mainTheme
import com.chessplus.app.ui.theme.ChessPlusTheme
import com.chessplus.app.ui.theme.kMonoFamily
import com.chessplus.app.ui.themeLabel
import com.chessplus.core.model.Color
import com.chessplus.core.model.Piece
import com.chessplus.core.model.Square
import com.chessplus.core.model.Theme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/** Côté de la vignette du puzzle du jour. */
private const val kDailyBoardSize = 112

/** Côté de la tuile d'icône d'un mode. */
private const val kModeIconTile = 36

/** Hauteur minimale d'une ligne de mode. */
private const val kModeRowHeight = 64

/** Format de la date en tête de l'accueil (« lundi 23 novembre »). */
private const val kDatePattern = "EEEE d MMMM"

/**
 * Puzzle du jour affiché sur l'accueil.
 *
 * @property day jour du puzzle (« yyyyMMdd »).
 * @property puzzleId identifiant Lichess.
 * @property pieces position à résoudre (après le premier coup de l'adversaire).
 * @property side camp du joueur, au trait.
 * @property rating classement du puzzle.
 * @property theme thème principal, ou null.
 */
data class DailyInfo(val day: String, val puzzleId: String, val pieces: Map<Square, Piece>, val side: Color, val rating: Int, val theme: Theme?)

/**
 * État de l'accueil.
 *
 * @property rating classement du joueur, arrondi.
 * @property dayStreak série de jours consécutifs.
 * @property daily puzzle du jour, ou null pendant le chargement.
 * @property survivalBest record en Survie, ou null.
 * @property rushBest record en Rush, ou null.
 * @property offlineCount nombre de puzzles de la base hors ligne, ou null pendant le chargement.
 * @property solvedDays jours dont le puzzle du jour a été réussi.
 * @property goal avancement de l'objectif quotidien, ou null pendant le chargement.
 */
data class HomeUiState(
	val rating: Int = 0,
	val dayStreak: Int = 0,
	val daily: DailyInfo? = null,
	val survivalBest: Int? = null,
	val rushBest: Int? = null,
	val offlineCount: Int? = null,
	val solvedDays: Set<String> = emptySet(),
	val goal: GoalProgress? = null,
) {
	/** Vrai si le puzzle du jour affiché a déjà été réussi. */
	val dailySolved: Boolean
		get() = daily?.day?.let { it in solvedDays } ?: false
}

/**
 * ViewModel de l'accueil : classement, série, puzzle du jour, records, base hors ligne.
 *
 * @property container dépendances de l'application.
 */
class HomeViewModel(private val container: AppContainer) : ViewModel() {
	/** État publié. */
	private val mutableState = MutableStateFlow(HomeUiState())

	/** État de l'écran. */
	val state: StateFlow<HomeUiState> = mutableState.asStateFlow()

	init {
		viewModelScope.launch {
			val vDao = container.ready().second.userDao()
			launch {
				vDao.observePlayer().collect { inPlayer ->
					val vRating = inPlayer?.rating ?: container.ratingSystem.initialRating.rating
					mutableState.update { it.copy(rating = vRating.roundToInt()) }
				}
			}
			launch {
				vDao.observeChallengeBests().collect { inBests ->
					val vByKind = inBests.associate { it.kind to it.score }
					mutableState.update { it.copy(survivalBest = vByKind[ChallengeKind.Survival.key], rushBest = vByKind[ChallengeKind.Rush.key]) }
				}
			}
			launch {
				vDao.observeAllAttempts().collect { inAttempts ->
					val vReport = withContext(Dispatchers.Default) { StatsCalculator().report(inAttempts, System.currentTimeMillis()) }
					mutableState.update { it.copy(dayStreak = vReport.dayStreak) }
				}
			}
			launch {
				// L'objectif se recalcule à chaque tentative enregistrée (défis compris) et à chaque changement d'objectif.
				combine(vDao.observeAllAttempts(), container.settingsStore.settings) { _, _ -> }
					.collect { refreshGoal() }
			}
			launch {
				vDao.observeSolvedDailyDays().collect { inDays -> mutableState.update { it.copy(solvedDays = inDays.toSet()) } }
			}
			launch {
				val vCount = container.offlineBaseInfo().puzzleCount
				mutableState.update { it.copy(offlineCount = vCount) }
			}
		}
	}

	/**
	 * Relit l'avancement de l'objectif quotidien.
	 */
	private suspend fun refreshGoal() {
		val vGoal = container.goalProgress()
		mutableState.update { it.copy(goal = vGoal) }
	}

	/**
	 * Relit le puzzle du jour et l'objectif (à chaque affichage de l'accueil : ils changent à minuit).
	 */
	fun refresh() {
		viewModelScope.launch { refreshGoal() }
		viewModelScope.launch {
			val vDay = DailyDays.keyOf(System.currentTimeMillis())
			val vPuzzle = container.dailyStore.puzzleFor(vDay) ?: return@launch
			val vPosition = container.rules.positionFromFen(vPuzzle.fen)
			vPuzzle.moves.firstOrNull()?.let { vPosition.play(it) }
			val vDaily = DailyInfo(
				day = vDay,
				puzzleId = vPuzzle.id,
				pieces = Square.kAll.mapNotNull { vSquare -> vPosition.pieceAt(vSquare)?.let { vSquare to it } }.toMap(),
				side = vPuzzle.playerColor,
				rating = vPuzzle.rating,
				theme = mainTheme(vPuzzle.themes),
			)
			mutableState.update { it.copy(daily = vDaily) }
		}
	}

	companion object {
		/**
		 * Retourne la fabrique du ViewModel.
		 */
		fun factory(inContainer: AppContainer): ViewModelProvider.Factory = viewModelFactory { initializer { HomeViewModel(inContainer) } }
	}
}

/**
 * Accueil : date et série, puzzle du jour, modes de jeu, base hors ligne.
 */
@Composable
fun HomeScreen(
	inViewModel: HomeViewModel,
	inOnDaily: (DailyInfo) -> Unit,
	inOnArcade: () -> Unit,
	inOnTraining: () -> Unit,
	inOnChallenge: (ChallengeKind) -> Unit,
	inOnTab: (NavTab) -> Unit,
) {
	val vState by inViewModel.state.collectAsStateWithLifecycle()
	LaunchedEffect(Unit) { inViewModel.refresh() }
	HomeContent(vState, System.currentTimeMillis(), inOnDaily, inOnArcade, inOnTraining, inOnChallenge, inOnTab)
}

/**
 * Contenu de l'accueil, sans état propre (prévisualisable).
 */
@Composable
fun HomeContent(
	inState: HomeUiState,
	inNow: Long,
	inOnDaily: (DailyInfo) -> Unit,
	inOnArcade: () -> Unit,
	inOnTraining: () -> Unit,
	inOnChallenge: (ChallengeKind) -> Unit,
	inOnTab: (NavTab) -> Unit,
) {
	val vColors = ChessPlusTheme.colors
	val vSpacing = ChessPlusTheme.spacing
	Column(Modifier.fillMaxSize().background(vColors.surface).statusBarsPadding()) {
		Column(
			Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(start = vSpacing.space4, end = vSpacing.space4, top = vSpacing.space2, bottom = vSpacing.space4),
			verticalArrangement = Arrangement.spacedBy(vSpacing.space6),
		) {
			Row(verticalAlignment = Alignment.Bottom) {
				Column(Modifier.weight(1f)) {
					Overline(SimpleDateFormat(kDatePattern, Locale.FRANCE).format(Date(inNow)))
					PageTitle(stringResource(R.string.app_name))
				}
				// Série de jours : rien à afficher tant qu'elle est à zéro.
				if (inState.dayStreak > 0) {
					val vStreakDescription = stringResource(R.string.home_streak_description, inState.dayStreak)
					Chip(
						stringResource(R.string.home_streak_chip, inState.dayStreak),
						Modifier.semantics { contentDescription = vStreakDescription },
						inIcon = R.drawable.ic_ln_flame,
						inIconTint = vColors.streak,
					)
				}
			}
			DailyCard(inState.daily, inState.dailySolved, inOnDaily)
			inState.goal?.let { GoalCard(it) }
			Column(verticalArrangement = Arrangement.spacedBy(vSpacing.space2)) {
				Overline(stringResource(R.string.home_modes))
				ListGroup(
					inRows = listOf(
						{ ModeRow(R.drawable.ic_ln_target, stringResource(R.string.mode_arcade), stringResource(R.string.mode_arcade_detail), formatRating(inState.rating), inOnArcade) },
						{ ModeRow(R.drawable.ic_ln_clock, stringResource(R.string.mode_rush), stringResource(R.string.mode_rush_detail), inState.rushBest?.let { formatRating(it) } ?: "–") { inOnChallenge(ChallengeKind.Rush) } },
						{ ModeRow(R.drawable.ic_ln_heart, stringResource(R.string.mode_survival), stringResource(R.string.mode_survival_detail), inState.survivalBest?.let { formatRating(it) } ?: "–") { inOnChallenge(ChallengeKind.Survival) } },
						{ ModeRow(R.drawable.ic_ln_grid, stringResource(R.string.mode_training), stringResource(R.string.mode_training_detail), "${Theme.entries.size}", inOnTraining) },
					),
				)
			}
			inState.offlineCount?.let { inCount ->
				Row(Modifier.padding(top = 0.dp), verticalAlignment = Alignment.CenterVertically) {
					Icon(painterResource(R.drawable.ic_ln_offline), contentDescription = null, tint = vColors.inkMuted, modifier = Modifier.size(kIconSize))
					Spacer(Modifier.width(6.dp))
					Text(stringResource(R.string.home_offline, formatRating(inCount)), style = ChessPlusTheme.typography.small, color = vColors.inkMuted)
				}
			}
		}
		FloatingNavBar(NavTab.Home, inOnTab)
	}
}

/**
 * Carte du puzzle du jour : vignette sans coordonnées, thème, camp au trait et classement, bouton
 * principal « Résoudre » (seul bouton `accent` de l'écran). Une fois réussi, la carte le signale et
 * propose « Rejouer » en bouton secondaire.
 */
@Composable
private fun DailyCard(inDaily: DailyInfo?, inSolved: Boolean, inOnDaily: (DailyInfo) -> Unit) {
	val vColors = ChessPlusTheme.colors
	SurfaceCard {
		Row(Modifier.height(kDailyBoardSize.dp), horizontalArrangement = Arrangement.spacedBy(ChessPlusTheme.spacing.space4)) {
			Box(Modifier.size(kDailyBoardSize.dp).clip(RoundedCornerShape(ChessPlusTheme.radius.radiusSm)).background(vColors.surfaceSunken)) {
				inDaily?.let { MiniBoard(it.pieces, it.side, Modifier.fillMaxSize()) }
			}
			Column(Modifier.weight(1f)) {
				Row(verticalAlignment = Alignment.CenterVertically) {
					Overline(stringResource(R.string.home_daily), Modifier.weight(1f, fill = false))
					if (inSolved) {
						Spacer(Modifier.width(ChessPlusTheme.spacing.space2))
						Icon(painterResource(R.drawable.ic_ln_check), contentDescription = null, tint = vColors.success, modifier = Modifier.size(16.dp))
						Spacer(Modifier.width(2.dp))
						Text(stringResource(R.string.home_daily_solved), style = ChessPlusTheme.typography.tag, color = vColors.success, maxLines = 1)
					}
				}
				Spacer(Modifier.height(2.dp))
				Text(
					inDaily?.theme?.let { stringResource(themeLabel(it)) } ?: stringResource(R.string.home_daily_default),
					style = ChessPlusTheme.typography.heading,
					color = vColors.ink,
					maxLines = 1,
					overflow = TextOverflow.Ellipsis,
				)
				if (inDaily != null) {
					Text(
						buildAnnotatedString {
							append(stringResource(if (inDaily.side == Color.White) R.string.home_daily_side_white else R.string.home_daily_side_black))
							append(" · ")
							withStyle(SpanStyle(fontFamily = kMonoFamily)) { append(formatRating(inDaily.rating)) }
						},
						style = ChessPlusTheme.typography.small,
						color = vColors.inkMuted,
						maxLines = 1,
					)
				}
				Spacer(Modifier.weight(1f))
				if (inSolved && inDaily != null) {
					SecondaryButton(stringResource(R.string.home_daily_replay), { inOnDaily(inDaily) }, Modifier.fillMaxWidth(), inIcon = R.drawable.ic_ln_replay, inHeight = 36.dp)
				} else {
					PrimaryButton(
						stringResource(R.string.home_daily_solve),
						{ inDaily?.let { inOnDaily(it) } },
						Modifier.fillMaxWidth(),
						inEnabled = inDaily != null,
						inHeight = 36.dp,
					)
				}
			}
		}
	}
}

/**
 * Objectif du jour : puzzles réussis sur l'objectif en mono et barre de progression (`success` une
 * fois atteint).
 */
@Composable
private fun GoalCard(inGoal: GoalProgress) {
	val vColors = ChessPlusTheme.colors
	SurfaceCard {
		Row(verticalAlignment = Alignment.CenterVertically) {
			Overline(stringResource(R.string.goal_title), Modifier.weight(1f))
			Text(stringResource(R.string.goal_value, inGoal.done, inGoal.target), style = ChessPlusTheme.typography.moves, color = if (inGoal.reached) vColors.success else vColors.ink)
		}
		Spacer(Modifier.height(ChessPlusTheme.spacing.space2))
		ProgressBar(inGoal.fraction, inColor = if (inGoal.reached) vColors.success else vColors.accent)
	}
}

/**
 * Ligne d'un mode de jeu : icône dans une tuile `surfaceSunken`, titre, description, valeur en mono.
 */
@Composable
private fun ModeRow(inIcon: Int, inTitle: String, inDetail: String, inValue: String, inOnClick: () -> Unit) {
	val vColors = ChessPlusTheme.colors
	ListRow(inOnClick = inOnClick, inMinHeight = kModeRowHeight.dp) {
		Box(
			Modifier.size(kModeIconTile.dp).clip(RoundedCornerShape(ChessPlusTheme.radius.radiusSm)).background(vColors.surfaceSunken),
			contentAlignment = Alignment.Center,
		) {
			Icon(painterResource(inIcon), contentDescription = null, tint = vColors.ink, modifier = Modifier.size(kIconSize))
		}
		Column(Modifier.weight(1f)) {
			Text(inTitle, style = ChessPlusTheme.typography.headingSmall, color = vColors.ink, maxLines = 1)
			Text(inDetail, style = ChessPlusTheme.typography.small, color = vColors.inkMuted, maxLines = 2, overflow = TextOverflow.Ellipsis)
		}
		Text(inValue, style = ChessPlusTheme.typography.movesSmall, color = vColors.inkMuted, maxLines = 1)
	}
}
