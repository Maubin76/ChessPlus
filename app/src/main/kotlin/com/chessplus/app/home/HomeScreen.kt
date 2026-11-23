package com.chessplus.app.home

import com.chessplus.app.ui.components.pressClickable
import com.chessplus.app.ui.components.rememberPressFeedback
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
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
import com.chessplus.app.stats.StatsCalculator
import com.chessplus.app.stats.percent
import com.chessplus.app.ui.components.BottomNavBar
import com.chessplus.app.ui.components.MiniBoard
import com.chessplus.app.ui.components.NavTab
import com.chessplus.app.ui.components.ScreenTopBar
import com.chessplus.app.ui.components.SurfaceCard
import com.chessplus.app.ui.components.formatElapsed
import com.chessplus.app.ui.components.formatRating
import com.chessplus.app.ui.theme.ChessType
import com.chessplus.app.ui.theme.kLocalChessColors
import com.chessplus.core.model.Color
import com.chessplus.core.model.Piece
import com.chessplus.core.model.Square
import com.chessplus.core.puzzle.PuzzleSession
import com.chessplus.core.puzzle.PuzzleStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/** Opacité du sous-titre de la carte « Reprendre », dans la couleur du texte de la carte. */
private const val kResumeSubtitleAlpha = 0.8f

/** Taille de la pastille d'icône des cartes de mode. */
private const val kModeIconSize = 32

/** Opacité de la pastille d'icône sur la carte de mode principale. */
private const val kModeIconAlpha = 0.18f

/**
 * Problème en cours affiché sur l'accueil.
 *
 * @property pieces position courante.
 * @property orientation camp du joueur.
 * @property move numéro du coup à trouver.
 * @property total nombre de coups à trouver.
 * @property finished vrai si le problème est terminé.
 * @property startedAt date de début (millisecondes).
 */
data class ResumeInfo(
	val pieces: Map<Square, Piece>,
	val orientation: Color,
	val move: Int,
	val total: Int,
	val finished: Boolean,
	val startedAt: Long,
)

/**
 * État de l'accueil.
 *
 * @property solvedCount nombre de problèmes réussis.
 * @property rating cote du joueur, arrondie.
 * @property resume problème en cours, ou null.
 * @property dayStreak série de jours consécutifs en cours.
 * @property successRate taux de réussite global (0 à 1), ou null sans tentative.
 * @property survivalBest meilleur score en Survie, ou null.
 * @property rushBest meilleur score en Rush, ou null.
 */
data class HomeUiState(
	val solvedCount: Int = 0,
	val rating: Int = 0,
	val resume: ResumeInfo? = null,
	val dayStreak: Int = 0,
	val successRate: Float? = null,
	val survivalBest: Int? = null,
	val rushBest: Int? = null,
)

/**
 * ViewModel de l'accueil : compteur de problèmes résolus, cote, problème en cours.
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
			launch { vDao.observeSolvedCount().collect { inCount -> mutableState.update { it.copy(solvedCount = inCount) } } }
			launch {
				vDao.observeChallengeBests().collect { inBests ->
					val vByKind = inBests.associate { it.kind to it.score }
					mutableState.update { it.copy(survivalBest = vByKind[ChallengeKind.Survival.key], rushBest = vByKind[ChallengeKind.Rush.key]) }
				}
			}
			launch {
				vDao.observeAllAttempts().collect { inAttempts ->
					val vReport = withContext(Dispatchers.Default) { StatsCalculator().report(inAttempts, System.currentTimeMillis()) }
					mutableState.update { it.copy(dayStreak = vReport.dayStreak, successRate = vReport.successRate) }
				}
			}
			launch {
				vDao.observePlayer().collect { inPlayer ->
					val vRating = inPlayer?.rating ?: container.ratingSystem.initialRating.rating
					mutableState.update { it.copy(rating = vRating.roundToInt()) }
				}
			}
		}
	}

	/**
	 * Relit le problème en cours (à chaque affichage de l'accueil).
	 */
	fun refresh() {
		viewModelScope.launch {
			val vSaved = container.sessionStore.load()
			val vPuzzle = vSaved?.let { container.puzzleProvider.byId(it.snapshot.puzzleId) }
			val vSession = if (vSaved != null && vPuzzle != null) runCatching { PuzzleSession.restore(vPuzzle, container.rules, vSaved.snapshot) }.getOrNull() else null
			val vResume = if (vSession != null && vSaved != null) {
				val vTotal = vSession.puzzle.playerMoveCount
				ResumeInfo(
					pieces = Square.kAll.mapNotNull { vSquare -> vSession.board.pieceAt(vSquare)?.let { vSquare to it } }.toMap(),
					orientation = vSession.puzzle.playerColor,
					move = minOf(vSession.solutionPly / 2 + 1, vTotal),
					total = vTotal,
					finished = vSession.status == PuzzleStatus.Solved,
					startedAt = vSaved.startedAt,
				)
			} else {
				null
			}
			mutableState.update { it.copy(resume = vResume) }
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
 * Accueil : tableau de bord (problèmes résolus, cote), reprise, modes de jeu.
 */
@Composable
fun HomeScreen(
	inViewModel: HomeViewModel,
	inOnTrain: () -> Unit,
	inOnTargeted: () -> Unit,
	inOnSettings: () -> Unit,
	inOnTab: (NavTab) -> Unit,
	inOnChallenge: (ChallengeKind) -> Unit,
) {
	val vState by inViewModel.state.collectAsStateWithLifecycle()
	LaunchedEffect(Unit) { inViewModel.refresh() }
	HomeContent(vState, System.currentTimeMillis(), inOnTrain, inOnTargeted, inOnSettings, inOnTab, inOnChallenge)
}

/**
 * Contenu de l'accueil, sans état propre (prévisualisable).
 */
@Composable
fun HomeContent(
	inState: HomeUiState,
	inNow: Long,
	inOnTrain: () -> Unit,
	inOnTargeted: () -> Unit,
	inOnSettings: () -> Unit,
	inOnTab: (NavTab) -> Unit,
	inOnChallenge: (ChallengeKind) -> Unit = {},
) {
	val vState = inState
	val vColors = kLocalChessColors.current
	Column(Modifier.fillMaxSize().background(vColors.background).statusBarsPadding()) {
		ScreenTopBar(stringResource(R.string.app_name), inActionIcon = R.drawable.ic_settings, inOnAction = inOnSettings)
		Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
			Spacer(Modifier.height(8.dp))
			Column(
				Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(vColors.surface).clickable { inOnTab(NavTab.Stats) }.padding(horizontal = 20.dp, vertical = 16.dp),
			) {
				Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
					DashboardValue(stringResource(R.string.home_solved_short), formatRating(vState.solvedCount), Modifier.weight(0.9f))
					DashboardValue(stringResource(R.string.home_streak), pluralStringResource(R.plurals.stats_days, vState.dayStreak, vState.dayStreak), Modifier.weight(1.3f))
					DashboardValue(stringResource(R.string.home_success), vState.successRate?.let { percent(it) } ?: "–", Modifier.weight(0.9f))
				}
				if (vState.rating > 0) {
					Spacer(Modifier.height(8.dp))
					Text(stringResource(R.string.home_rating, formatRating(vState.rating)), style = ChessType.kBody, color = vColors.muted)
				}
			}
			vState.resume?.let { inResume ->
				Spacer(Modifier.height(12.dp))
				val vPress = rememberPressFeedback()
				Row(
					Modifier.then(vPress.scale).fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(vColors.primary).pressClickable(vPress, inOnClick = inOnTrain).padding(16.dp),
					verticalAlignment = Alignment.CenterVertically,
				) {
					Box(Modifier.size(64.dp).clip(RoundedCornerShape(10.dp))) {
						MiniBoard(inResume.pieces, inResume.orientation, Modifier.fillMaxSize())
					}
					Spacer(Modifier.width(14.dp))
					Column(Modifier.weight(1f)) {
						Text(stringResource(R.string.home_resume), style = ChessType.kSection, color = vColors.onPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
						val vWhen = formatElapsed(inResume.startedAt, inNow)
						val vProgress = if (inResume.finished) stringResource(R.string.home_resume_finished) else stringResource(R.string.home_resume_move, inResume.move, inResume.total)
						Text("$vProgress · $vWhen", style = ChessType.kCaption, color = vColors.onPrimary.copy(alpha = kResumeSubtitleAlpha), maxLines = 1, overflow = TextOverflow.Ellipsis)
					}
					Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = null, tint = vColors.onPrimary, modifier = Modifier.size(24.dp))
				}
			}
			Spacer(Modifier.height(20.dp))
			SectionTitle(stringResource(R.string.home_section_training))
			Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
				ModeCard(R.drawable.ic_bolt_fill1, stringResource(R.string.mode_arcade), stringResource(R.string.mode_arcade_detail), null, true, Modifier.weight(1f), inOnTrain)
				ModeCard(R.drawable.ic_tune, stringResource(R.string.mode_targeted), stringResource(R.string.mode_targeted_detail), null, false, Modifier.weight(1f), inOnTargeted)
			}
			Spacer(Modifier.height(16.dp))
			SectionTitle(stringResource(R.string.home_section_challenges))
			Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
				ModeCard(
					R.drawable.ic_favorite_fill1, stringResource(R.string.mode_survival), stringResource(R.string.mode_survival_detail),
					vState.survivalBest, false, Modifier.weight(1f),
				) { inOnChallenge(ChallengeKind.Survival) }
				ModeCard(
					R.drawable.ic_timer_fill1, stringResource(R.string.mode_rush), stringResource(R.string.mode_rush_detail),
					vState.rushBest, false, Modifier.weight(1f),
				) { inOnChallenge(ChallengeKind.Rush) }
			}
			Spacer(Modifier.height(12.dp))
		}
		BottomNavBar(NavTab.Home, inOnTab)
	}
}

/**
 * Chiffre du tableau de bord : libellé et valeur.
 */
@Composable
private fun DashboardValue(inLabel: String, inValue: String, inModifier: Modifier) {
	val vColors = kLocalChessColors.current
	Column(inModifier) {
		Text(inLabel, style = ChessType.kCaption, color = vColors.muted, maxLines = 1)
		Text(inValue, style = ChessType.kFeedback, color = vColors.text, maxLines = 1)
	}
}

/**
 * Titre d'un groupe de modes de jeu.
 */
@Composable
private fun SectionTitle(inTitle: String) {
	Text(inTitle, style = ChessType.kSection, color = kLocalChessColors.current.text, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
}

/**
 * Carte d'un mode de jeu : icône, nom, description et record éventuel ; [inHighlighted] pour le
 * mode principal (fond vert).
 */
@Composable
private fun ModeCard(inIcon: Int, inTitle: String, inDetail: String, inRecord: Int?, inHighlighted: Boolean, inModifier: Modifier, inOnClick: () -> Unit) {
	val vColors = kLocalChessColors.current
	val vPress = rememberPressFeedback()
	val vText = if (inHighlighted) vColors.onPrimary else vColors.text
	val vMuted = if (inHighlighted) vColors.onPrimary.copy(alpha = kResumeSubtitleAlpha) else vColors.muted
	Column(
		inModifier
			.fillMaxHeight()
			.then(vPress.scale)
			.clip(RoundedCornerShape(20.dp))
			.background(if (inHighlighted) vColors.primary else vColors.surface)
			.pressClickable(vPress, inOnClick = inOnClick)
			.padding(14.dp),
	) {
		Row(verticalAlignment = Alignment.CenterVertically) {
			Box(
				Modifier.size(kModeIconSize.dp).clip(CircleShape).background(if (inHighlighted) vColors.onPrimary.copy(alpha = kModeIconAlpha) else vColors.background),
				contentAlignment = Alignment.Center,
			) {
				Icon(painterResource(inIcon), contentDescription = null, tint = if (inHighlighted) vColors.onPrimary else vColors.primary, modifier = Modifier.size(20.dp))
			}
			if (inRecord != null) {
				Spacer(Modifier.weight(1f))
				Text(stringResource(R.string.mode_record, inRecord), style = ChessType.kLabel, color = vMuted, maxLines = 1)
			}
		}
		Spacer(Modifier.height(8.dp))
		Text(inTitle, style = ChessType.kSection, color = vText, maxLines = 1, overflow = TextOverflow.Ellipsis)
		Spacer(Modifier.height(2.dp))
		Text(inDetail, style = ChessType.kCaption, color = vMuted, maxLines = 2, overflow = TextOverflow.Ellipsis)
	}
}
