package com.chessplus.app.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.chessplus.app.ui.components.NavTab
import com.chessplus.app.ui.components.ScreenTopBar
import com.chessplus.app.ui.components.SegmentedControl
import com.chessplus.app.ui.components.SurfaceCard
import com.chessplus.app.ui.components.formatDelta
import com.chessplus.app.ui.components.formatRating
import com.chessplus.app.ui.theme.ChessType
import com.chessplus.app.ui.theme.kLocalChessColors
import com.chessplus.app.ui.themeLabel
import com.chessplus.core.model.Theme
import com.chessplus.core.model.ThemeSet
import com.chessplus.core.puzzle.PuzzleResult
import com.chessplus.data.user.AttemptEntity
import com.chessplus.data.user.RatingPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** Nombre minimal de tentatives pour afficher un thème. */
private const val kMinAttemptsPerTheme = 3

/** Nombre maximal de thèmes affichés. */
private const val kMaxThemes = 8

/** Durée d'un jour en millisecondes. */
private const val kDayMs = 24L * 60 * 60 * 1000

/**
 * Réussite sur un thème.
 *
 * @property theme thème.
 * @property attempts nombre de tentatives notées.
 * @property rate taux de réussite, de 0 à 1.
 */
data class ThemeRate(val theme: Theme, val attempts: Int, val rate: Float)

/**
 * Données des statistiques.
 *
 * @property history évolution de la cote.
 * @property themes réussite par thème, des plus joués aux moins joués.
 */
data class StatsData(val history: List<RatingPoint>, val themes: List<ThemeRate>)

/**
 * ViewModel des statistiques : évolution de la cote et réussite par thème.
 *
 * @property container dépendances de l'application.
 */
class StatsViewModel(private val container: AppContainer) : ViewModel() {
	/** Données publiées, ou null pendant le chargement. */
	private val mutableData = MutableStateFlow<StatsData?>(null)

	/** Données de l'écran. */
	val data: StateFlow<StatsData?> = mutableData.asStateFlow()

	init {
		viewModelScope.launch {
			val vDao = container.ready().second.userDao()
			vDao.observeRatingHistory().collect { inHistory ->
				mutableData.value = StatsData(inHistory, themeRates(vDao.getRatedAttempts()))
			}
		}
	}

	/**
	 * Calcule la réussite par thème à partir des tentatives notées.
	 */
	private fun themeRates(inAttempts: List<AttemptEntity>): List<ThemeRate> {
		val vTotals = mutableMapOf<Theme, IntArray>()
		for (vAttempt in inAttempts) {
			for (vTheme in ThemeSet(vAttempt.themesLow, vAttempt.themesHigh)) {
				val vCounts = vTotals.getOrPut(vTheme) { IntArray(2) }
				vCounts[0]++
				if (vAttempt.result == PuzzleResult.Success) {
					vCounts[1]++
				}
			}
		}
		return vTotals.filter { it.value[0] >= kMinAttemptsPerTheme }
			.map { ThemeRate(it.key, it.value[0], it.value[1].toFloat() / it.value[0]) }
			.sortedByDescending { it.attempts }
			.take(kMaxThemes)
	}

	companion object {
		/**
		 * Retourne la fabrique du ViewModel.
		 */
		fun factory(inContainer: AppContainer): ViewModelProvider.Factory = viewModelFactory { initializer { StatsViewModel(inContainer) } }
	}
}

/**
 * Statistiques : cote (courbe par période) et réussite par thème.
 */
@Composable
fun StatsScreen(inViewModel: StatsViewModel, inOnTab: (NavTab) -> Unit) {
	val vData by inViewModel.data.collectAsStateWithLifecycle()
	StatsContent(vData, System.currentTimeMillis(), inOnTab)
}

/**
 * Contenu des statistiques, sans état métier (prévisualisable). [inData] vaut null pendant le chargement.
 */
@Composable
fun StatsContent(inData: StatsData?, inNow: Long, inOnTab: (NavTab) -> Unit) {
	val vData = inData
	val vColors = kLocalChessColors.current
	var vPeriod by rememberSaveable { mutableIntStateOf(1) }
	Column(Modifier.fillMaxSize().background(vColors.background).statusBarsPadding()) {
		ScreenTopBar(stringResource(R.string.nav_stats))
		Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
			val vHistory = vData?.history.orEmpty()
			if (vData != null && vHistory.isEmpty()) {
				Box(Modifier.fillMaxWidth().padding(vertical = 64.dp), contentAlignment = Alignment.Center) {
					Text(stringResource(R.string.stats_empty), style = ChessType.kBody, color = vColors.muted, textAlign = TextAlign.Center)
				}
			}
			if (vHistory.isNotEmpty()) {
				val vNow = inNow
				val vFrom = when (vPeriod) {
					0 -> vNow - 7 * kDayMs
					1 -> vNow - 30 * kDayMs
					else -> Long.MIN_VALUE
				}
				val vInPeriod = vHistory.filter { it.playedAt >= vFrom }
				val vBefore = vHistory.lastOrNull { it.playedAt < vFrom }
				val vSeries = listOfNotNull(vBefore) + vInPeriod
				val vCurrent = vHistory.last().rating.roundToInt()
				val vStart = (vSeries.firstOrNull() ?: vHistory.last()).rating.roundToInt()
				SurfaceCard {
					Text(stringResource(R.string.stats_rating), style = ChessType.kCaption, color = vColors.muted)
					Row(verticalAlignment = Alignment.Bottom) {
						Text(formatRating(vCurrent), style = ChessType.kNumber, color = vColors.text)
						Spacer(Modifier.width(10.dp))
						val vDelta = vCurrent - vStart
						Text(
							stringResource(R.string.stats_delta, formatDelta(vDelta), stringResource(listOf(R.string.stats_period_7, R.string.stats_period_30, R.string.stats_period_all)[vPeriod])),
							style = ChessType.kBodyStrong,
							color = if (vDelta >= 0) vColors.success else vColors.error,
							modifier = Modifier.padding(bottom = 10.dp),
						)
					}
					Spacer(Modifier.height(8.dp))
					RatingChart(vSeries.map { it.rating.toFloat() }, Modifier.fillMaxWidth().height(110.dp))
					Spacer(Modifier.height(12.dp))
					SegmentedControl(listOf(stringResource(R.string.stats_tab_7), stringResource(R.string.stats_tab_30), stringResource(R.string.stats_tab_all)), vPeriod) { vPeriod = it }
				}
				Spacer(Modifier.height(12.dp))
				SurfaceCard {
					Text(stringResource(R.string.stats_themes), style = ChessType.kSection, color = vColors.text)
					Spacer(Modifier.height(8.dp))
					val vThemes = vData?.themes.orEmpty()
					if (vThemes.isEmpty()) {
						Text(stringResource(R.string.stats_themes_empty), style = ChessType.kCaption, color = vColors.muted)
					}
					for (vRate in vThemes) {
						Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
							Text(stringResource(themeLabel(vRate.theme)), style = ChessType.kBody, color = vColors.text, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.width(130.dp))
							Box(Modifier.weight(1f).height(8.dp).background(vColors.track, CircleShape)) {
								Box(Modifier.fillMaxWidth(vRate.rate.coerceAtLeast(0.02f)).fillMaxHeight().background(vColors.text, CircleShape))
							}
							Text("${(vRate.rate * 100).roundToInt()} %", style = ChessType.kBodyStrong, color = vColors.text, textAlign = TextAlign.End, modifier = Modifier.width(52.dp))
						}
					}
				}
				Spacer(Modifier.height(16.dp))
			}
		}
		BottomNavBar(NavTab.Stats, inOnTab)
	}
}

/**
 * Courbe de la cote.
 */
@Composable
private fun RatingChart(inValues: List<Float>, inModifier: Modifier) {
	val vColor = kLocalChessColors.current.text
	Canvas(inModifier) {
		if (inValues.isEmpty()) {
			return@Canvas
		}
		val vValues = if (inValues.size == 1) listOf(inValues[0], inValues[0]) else inValues
		val vMin = vValues.min() - 10f
		val vMax = vValues.max() + 10f
		val vPath = Path()
		vValues.forEachIndexed { inIndex, inValue ->
			val vX = size.width * inIndex / (vValues.size - 1)
			val vY = size.height * (1f - (inValue - vMin) / (vMax - vMin))
			if (inIndex == 0) vPath.moveTo(vX, vY) else vPath.lineTo(vX, vY)
		}
		drawPath(vPath, vColor, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
		drawCircle(vColor, 4.dp.toPx(), Offset(size.width, size.height * (1f - (vValues.last() - vMin) / (vMax - vMin))))
	}
}
