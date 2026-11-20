package com.chessplus.app.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.chessplus.app.ui.components.BottomNavBar
import com.chessplus.app.ui.components.MiniBoard
import com.chessplus.app.ui.components.NavTab
import com.chessplus.app.ui.components.ScreenTopBar
import com.chessplus.app.ui.components.SegmentedControl
import com.chessplus.app.ui.components.formatDelta
import com.chessplus.app.ui.components.formatElapsed
import com.chessplus.app.ui.components.formatRating
import com.chessplus.app.ui.theme.ChessType
import com.chessplus.app.ui.theme.kLocalChessColors
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

/** Nombre maximal de tentatives affichées. */
private const val kHistoryLimit = 300

/**
 * Ligne de l'historique.
 *
 * @property attemptId identifiant de la tentative.
 * @property puzzleId identifiant du problème.
 * @property pieces position présentée au joueur (après le coup de l'adversaire).
 * @property orientation camp du joueur.
 * @property themes thèmes du problème.
 * @property puzzleRating cote du problème.
 * @property playedAt date de la tentative.
 * @property success vrai si réussi.
 * @property delta variation de la cote, ou null si non noté.
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

	/** Positions déjà calculées, par problème. */
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
 * Élément de la liste : titre de jour ou ligne.
 */
private sealed interface HistoryItem {
	/** Titre d'un jour. */
	data class Day(val label: String) : HistoryItem

	/** Ligne de tentative. */
	data class Attempt(val row: HistoryRow) : HistoryItem
}

/**
 * Historique : tentatives récentes, filtrables, à rejouer.
 */
@Composable
fun HistoryScreen(inViewModel: HistoryViewModel, inOnReplay: (String) -> Unit, inOnTab: (NavTab) -> Unit) {
	val vRows by inViewModel.rows.collectAsStateWithLifecycle()
	HistoryContent(vRows, System.currentTimeMillis(), inOnReplay, inOnTab)
}

/**
 * Contenu de l'historique, sans état métier (prévisualisable). [inRows] vaut null pendant le chargement.
 */
@Composable
fun HistoryContent(inRows: List<HistoryRow>?, inNow: Long, inOnReplay: (String) -> Unit, inOnTab: (NavTab) -> Unit) {
	val vRows = inRows
	val vColors = kLocalChessColors.current
	var vFilter by rememberSaveable { mutableIntStateOf(0) }
	val vToday = stringResource(R.string.history_today)
	val vYesterday = stringResource(R.string.history_yesterday)
	Column(Modifier.fillMaxSize().background(vColors.background).statusBarsPadding()) {
		ScreenTopBar(stringResource(R.string.nav_history))
		Column(Modifier.padding(horizontal = 20.dp)) {
			SegmentedControl(listOf(stringResource(R.string.history_all), stringResource(R.string.history_solved), stringResource(R.string.history_failed)), vFilter) { vFilter = it }
		}
		val vFiltered = vRows?.filter { vFilter == 0 || (vFilter == 1 && it.success) || (vFilter == 2 && !it.success) }
		if (vFiltered != null && vFiltered.isEmpty()) {
			Box(Modifier.weight(1f).fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
				Text(stringResource(R.string.history_empty), style = ChessType.kBody, color = vColors.muted, textAlign = TextAlign.Center)
			}
		} else {
			val vItems = buildList {
				var vLastDay: String? = null
				for (vRow in vFiltered.orEmpty()) {
					val vDay = dayLabel(vRow.playedAt, inNow, vToday, vYesterday)
					if (vDay != vLastDay) {
						add(HistoryItem.Day(vDay))
						vLastDay = vDay
					}
					add(HistoryItem.Attempt(vRow))
				}
			}
			LazyColumn(Modifier.weight(1f).padding(horizontal = 20.dp)) {
				items(vItems, key = { inItem -> if (inItem is HistoryItem.Attempt) "a${inItem.row.attemptId}" else "d${(inItem as HistoryItem.Day).label}" }) { inItem ->
					when (inItem) {
						is HistoryItem.Day -> Text(inItem.label.uppercase(), style = ChessType.kCaption, color = vColors.muted, modifier = Modifier.padding(top = 18.dp, bottom = 8.dp))
						is HistoryItem.Attempt -> HistoryRowView(inItem.row, inNow) { inOnReplay(inItem.row.puzzleId) }
					}
				}
				item {
					Text(stringResource(R.string.history_replay_note), style = ChessType.kCaption, color = vColors.muted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp))
				}
			}
		}
		BottomNavBar(NavTab.History, inOnTab)
	}
}

/**
 * Ligne de l'historique : vignette, thèmes, cote et date, résultat.
 */
@Composable
private fun HistoryRowView(inRow: HistoryRow, inNow: Long, inOnClick: () -> Unit) {
	val vColors = kLocalChessColors.current
	val vThemes = inRow.themes.filter { it != com.chessplus.core.model.Theme.Mate || inRow.themes.count() == 1 }.take(2).map { stringResource(themeLabel(it)) }.joinToString(" · ")
	val vWhen = formatElapsed(inRow.playedAt, inNow)
	Row(
		Modifier.fillMaxWidth().padding(vertical = 3.dp).clip(RoundedCornerShape(18.dp)).background(vColors.surface).clickable(onClick = inOnClick).padding(10.dp),
		verticalAlignment = Alignment.CenterVertically,
	) {
		Box(Modifier.size(52.dp).clip(RoundedCornerShape(10.dp))) {
			MiniBoard(inRow.pieces, inRow.orientation, Modifier.fillMaxSize())
		}
		Spacer(Modifier.width(14.dp))
		Column(Modifier.weight(1f)) {
			Text(vThemes, style = ChessType.kBodyStrong, color = vColors.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
			Text("${formatRating(inRow.puzzleRating)} · $vWhen", style = ChessType.kCaption, color = vColors.muted, maxLines = 1)
		}
		Icon(
			painterResource(if (inRow.success) R.drawable.ic_check_circle_fill1 else R.drawable.ic_cancel_fill1),
			contentDescription = null,
			tint = if (inRow.success) vColors.success else vColors.error,
			modifier = Modifier.size(20.dp),
		)
		Spacer(Modifier.width(6.dp))
		Text(inRow.delta?.let { formatDelta(it) } ?: "", style = ChessType.kBodyStrong, color = if (inRow.success) vColors.success else vColors.error, modifier = Modifier.width(36.dp))
	}
	Spacer(Modifier.height(3.dp))
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
