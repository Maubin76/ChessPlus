package com.chessplus.app.home

import com.chessplus.app.ui.components.pressClickable
import com.chessplus.app.ui.components.rememberPressFeedback
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.chessplus.app.ui.components.BottomNavBar
import com.chessplus.app.ui.components.MiniBoard
import com.chessplus.app.ui.components.NavTab
import com.chessplus.app.ui.components.PrimaryButton
import com.chessplus.app.ui.components.SecondaryButton
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** Hauteur des boutons de l'accueil, plus compacts que les boutons pleine largeur. */
private const val kHomeButtonHeight = 48

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
 */
data class HomeUiState(
	val solvedCount: Int = 0,
	val rating: Int = 0,
	val resume: ResumeInfo? = null,
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
 * Accueil : tableau de bord (problèmes résolus, cote), reprise, entraînements.
 */
@Composable
fun HomeScreen(
	inViewModel: HomeViewModel,
	inOnTrain: () -> Unit,
	inOnTargeted: () -> Unit,
	inOnSettings: () -> Unit,
	inOnTab: (NavTab) -> Unit,
) {
	val vState by inViewModel.state.collectAsStateWithLifecycle()
	LaunchedEffect(Unit) { inViewModel.refresh() }
	HomeContent(vState, System.currentTimeMillis(), inOnTrain, inOnTargeted, inOnSettings, inOnTab)
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
) {
	val vState = inState
	val vColors = kLocalChessColors.current
	Column(Modifier.fillMaxSize().background(vColors.background).statusBarsPadding()) {
		ScreenTopBar(stringResource(R.string.app_name), inActionIcon = R.drawable.ic_settings, inOnAction = inOnSettings)
		Column(Modifier.weight(1f).padding(horizontal = 20.dp)) {
			Spacer(Modifier.height(8.dp))
			SurfaceCard {
				Text(stringResource(R.string.home_solved), style = ChessType.kCaption, color = vColors.muted)
				Text(formatRating(vState.solvedCount), style = ChessType.kNumber, color = vColors.text)
				if (vState.rating > 0) {
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
						Text("$vProgress · $vWhen", style = ChessType.kCaption, color = vColors.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
					}
					Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = null, tint = vColors.onPrimary, modifier = Modifier.size(24.dp))
				}
			}
			Spacer(Modifier.weight(1f))
			Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
				SecondaryButton(stringResource(R.string.home_targeted), inOnTargeted, R.drawable.ic_tune, Modifier.weight(1f), inHeight = kHomeButtonHeight)
				PrimaryButton(stringResource(R.string.home_train), inOnTrain, Modifier.weight(1.4f), inIcon = R.drawable.ic_bolt_fill1, inHeight = kHomeButtonHeight)
			}
			Spacer(Modifier.height(8.dp))
		}
		BottomNavBar(NavTab.Home, inOnTab)
	}
}
