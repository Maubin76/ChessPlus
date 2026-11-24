package com.chessplus.app.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
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
import com.chessplus.app.ui.components.ListGroup
import com.chessplus.app.ui.components.ListRow
import com.chessplus.app.ui.components.MiniBoard
import com.chessplus.app.ui.components.Section
import com.chessplus.app.ui.components.SegmentedControl
import com.chessplus.app.ui.components.TopBar
import com.chessplus.app.ui.components.formatDelta
import com.chessplus.app.ui.components.formatElapsed
import com.chessplus.app.ui.components.formatRating
import com.chessplus.app.ui.components.kIconSize
import com.chessplus.app.ui.theme.ChessPlusTheme
import com.chessplus.app.ui.themeLabel
import com.chessplus.core.model.Color
import com.chessplus.core.model.Piece
import com.chessplus.core.model.Square
import com.chessplus.core.model.ThemeSet
import com.chessplus.core.puzzle.PuzzleResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/** Taille de la vignette d'échiquier d'une ligne. */
private const val kThumbnailSize = 44

/** Nombre maximal de tentatives affichées. */
private const val kHistoryLimit = 300

/**
 * Ligne de l'historique.
 *
 * @property attemptId identifiant de la tentative.
 * @property puzzleId identifiant du puzzle.
 * @property pieces position présentée au joueur (après le coup de l'adversaire).
 * @property orientation camp du joueur.
 * @property themes thèmes du puzzle.
 * @property puzzleRating classement du puzzle.
 * @property playedAt date de la tentative.
 * @property success vrai si réussi.
 * @property delta variation du classement, ou null si non noté.
 */
data class HistoryRow(
	val attemptId: Long,
	val puzzleId: String,
	val pieces: Map<Square, Piece>,
	val orientation: Color,
	val themes: ThemeSet,
	val puzzleRating: Int,
	val playedAt: Long,
	val success: Boolean,
	val delta: Int?,
)

/**
 * ViewModel de l'historique : tentatives récentes avec leur position.
 *
 * @property container dépendances de l'application.
 */
class HistoryViewModel(private val container: AppContainer) : ViewModel() {
	/** Lignes publiées, ou null pendant le chargement. */
	private val mutableRows = MutableStateFlow<List<HistoryRow>?>(null)

	/** Lignes de l'historique. */
	val rows: StateFlow<List<HistoryRow>?> = mutableRows.asStateFlow()

	/** Positions déjà calculées, par puzzle. */
	private val positions = mutableMapOf<String, Pair<Map<Square, Piece>, Color>>()

	init {
		viewModelScope.launch {
			val (vPuzzles, vUser) = container.ready()
			vUser.userDao().observeRecentAttempts(kHistoryLimit).collect { inAttempts ->
				mutableRows.value = withContext(Dispatchers.Default) {
					inAttempts.mapNotNull { vAttempt ->
						val vPosition = positions[vAttempt.puzzleId] ?: vPuzzles.puzzleDao().getById(vAttempt.puzzleId)?.toPuzzle()?.let { inPuzzle ->
							val vBoard = container.rules.positionFromFen(inPuzzle.fen)
							vBoard.play(inPuzzle.moves.first())
							(Square.kAll.mapNotNull { vSquare -> vBoard.pieceAt(vSquare)?.let { vSquare to it } }.toMap() to inPuzzle.playerColor).also { positions[vAttempt.puzzleId] = it }
						} ?: return@mapNotNull null
						HistoryRow(
							attemptId = vAttempt.id,
							puzzleId = vAttempt.puzzleId,
							pieces = vPosition.first,
							orientation = vPosition.second,
							themes = ThemeSet(vAttempt.themesLow, vAttempt.themesHigh),
							puzzleRating = vAttempt.puzzleRating,
							playedAt = vAttempt.playedAt,
							success = vAttempt.result == PuzzleResult.Success,
							delta = if (vAttempt.rated) (vAttempt.playerRatingAfter.roundToInt() - vAttempt.playerRatingBefore.roundToInt()) else null,
						)
					}
				}
			}
		}
	}

	companion object {
		/**
		 * Retourne la fabrique du ViewModel.
		 */
		fun factory(inContainer: AppContainer): ViewModelProvider.Factory = viewModelFactory { initializer { HistoryViewModel(inContainer) } }
	}
}

/**
 * Groupe de l'historique : titre de jour et tentatives de ce jour.
 *
 * @property label libellé du jour.
 * @property rows tentatives du jour, de la plus récente à la plus ancienne.
 */
private data class HistoryDay(val label: String, val rows: List<HistoryRow>)

/**
 * Historique : tentatives récentes, filtrables, à rejouer (écran secondaire).
 */
@Composable
fun HistoryScreen(inViewModel: HistoryViewModel, inOnReplay: (String) -> Unit, inOnBack: () -> Unit) {
	val vRows by inViewModel.rows.collectAsStateWithLifecycle()
	HistoryContent(vRows, System.currentTimeMillis(), inOnReplay, inOnBack)
}

/**
 * Contenu de l'historique, sans état métier (prévisualisable). [inRows] vaut null pendant le chargement.
 */
@Composable
fun HistoryContent(inRows: List<HistoryRow>?, inNow: Long, inOnReplay: (String) -> Unit, inOnBack: () -> Unit) {
	val vColors = ChessPlusTheme.colors
	val vSpacing = ChessPlusTheme.spacing
	var vFilter by rememberSaveable { mutableIntStateOf(0) }
	val vToday = stringResource(R.string.hist_today)
	val vYesterday = stringResource(R.string.hist_yesterday)
	val vFiltered = inRows?.filter { vFilter == 0 || (vFilter == 1 && it.success) || (vFilter == 2 && !it.success) }
	val vDays = buildList<HistoryDay> {
		for (vRow in vFiltered.orEmpty()) {
			val vLabel = dayLabel(vRow.playedAt, inNow, vToday, vYesterday)
			if (lastOrNull()?.label == vLabel) {
				set(lastIndex, last().copy(rows = last().rows + vRow))
			} else {
				add(HistoryDay(vLabel, listOf(vRow)))
			}
		}
	}
	Column(Modifier.fillMaxSize().background(vColors.surface).statusBarsPadding().navigationBarsPadding()) {
		TopBar(stringResource(R.string.hist_title), inOnBack)
		LazyColumn(
			Modifier.weight(1f).padding(horizontal = vSpacing.space4),
			verticalArrangement = Arrangement.spacedBy(vSpacing.space6),
			contentPadding = PaddingValues(top = vSpacing.space2, bottom = vSpacing.space4),
		) {
			item {
				SegmentedControl(listOf(stringResource(R.string.hist_all), stringResource(R.string.hist_solved), stringResource(R.string.hist_failed)), vFilter) { vFilter = it }
			}
			if (vFiltered != null && vFiltered.isEmpty()) {
				item { Text(stringResource(R.string.hist_empty), style = ChessPlusTheme.typography.body, color = vColors.inkMuted) }
			}
			items(vDays, key = { it.label }) { inDay ->
				Section(inDay.label) {
					ListGroup(inRows = inDay.rows.map { vRow -> { HistoryRowView(vRow, inNow) { inOnReplay(vRow.puzzleId) } } })
				}
			}
			if (!vDays.isEmpty()) {
				item {
					Text(stringResource(R.string.hist_replay_note), style = ChessPlusTheme.typography.small, color = vColors.inkMuted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
				}
			}
		}
	}
}

/**
 * Ligne de l'historique : vignette, thèmes, classement et date, résultat et variation.
 */
@Composable
private fun HistoryRowView(inRow: HistoryRow, inNow: Long, inOnClick: () -> Unit) {
	val vColors = ChessPlusTheme.colors
	val vThemes = inRow.themes.filter { it != com.chessplus.core.model.Theme.Mate || inRow.themes.count() == 1 }.take(2).map { stringResource(themeLabel(it)) }.joinToString(" · ")
	val vResultColor = if (inRow.success) vColors.success else vColors.danger
	ListRow(inOnClick = inOnClick, inMinHeight = 64.dp) {
		MiniBoard(inRow.pieces, inRow.orientation, Modifier.size(kThumbnailSize.dp))
		Column(Modifier.weight(1f)) {
			Text(vThemes, style = ChessPlusTheme.typography.headingSmall, color = vColors.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
			Row(verticalAlignment = Alignment.CenterVertically) {
				Text(formatRating(inRow.puzzleRating), style = ChessPlusTheme.typography.movesSmall, color = vColors.inkMuted)
				Text(" · ${formatElapsed(inRow.playedAt, inNow)}", style = ChessPlusTheme.typography.small, color = vColors.inkMuted, maxLines = 1)
			}
		}
		Icon(
			painterResource(if (inRow.success) R.drawable.ic_ln_check else R.drawable.ic_ln_close),
			contentDescription = stringResource(if (inRow.success) R.string.hist_success else R.string.hist_failure),
			tint = vResultColor,
			modifier = Modifier.size(kIconSize),
		)
		Text(inRow.delta?.let { formatDelta(it) } ?: "", style = ChessPlusTheme.typography.movesSmall, color = vResultColor, textAlign = TextAlign.End, modifier = Modifier.width(36.dp))
	}
}

/**
 * Retourne le libellé du jour d'une date : « Aujourd'hui », « Hier » ou « 12 novembre ».
 */
private fun dayLabel(inTime: Long, inNow: Long, inToday: String, inYesterday: String): String {
	val vDay = Calendar.getInstance().apply { timeInMillis = inTime }
	val vNow = Calendar.getInstance().apply { timeInMillis = inNow }
	val vSameYear = vDay.get(Calendar.YEAR) == vNow.get(Calendar.YEAR)
	if (vSameYear && vDay.get(Calendar.DAY_OF_YEAR) == vNow.get(Calendar.DAY_OF_YEAR)) {
		return inToday
	}
	vNow.add(Calendar.DAY_OF_YEAR, -1)
	if (vDay.get(Calendar.YEAR) == vNow.get(Calendar.YEAR) && vDay.get(Calendar.DAY_OF_YEAR) == vNow.get(Calendar.DAY_OF_YEAR)) {
		return inYesterday
	}
	val vPattern = if (vSameYear) "d MMMM" else "d MMMM yyyy"
	return SimpleDateFormat(vPattern, Locale.FRANCE).format(Date(inTime))
}
