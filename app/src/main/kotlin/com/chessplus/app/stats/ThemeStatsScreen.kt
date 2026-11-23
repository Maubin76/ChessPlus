package com.chessplus.app.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.chessplus.app.AppContainer
import com.chessplus.app.R
import com.chessplus.app.ui.charts.LineChart
import com.chessplus.app.ui.components.PrimaryButton
import com.chessplus.app.ui.components.ScreenTopBar
import com.chessplus.app.ui.components.SurfaceCard
import com.chessplus.app.ui.components.formatElapsed
import com.chessplus.app.ui.components.formatRating
import com.chessplus.app.ui.theme.ChessType
import com.chessplus.app.ui.theme.kLocalChessColors
import com.chessplus.app.ui.themeLabel
import com.chessplus.core.model.Theme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel de la fiche d'un thème.
 *
 * @property container dépendances de l'application.
 * @property theme thème affiché.
 */
class ThemeStatsViewModel(private val container: AppContainer, private val theme: Theme) : ViewModel() {
	/** Fiche publiée ; null pendant le chargement. */
	private val mutableState = MutableStateFlow<ThemeStatsState?>(null)

	/** État de l'écran. */
	val state: StateFlow<ThemeStatsState?> = mutableState.asStateFlow()

	/** Calcul des statistiques. */
	private val calculator = StatsCalculator()

	init {
		viewModelScope.launch {
			container.ready().second.userDao().observeAllAttempts().collect { inAttempts ->
				mutableState.value = ThemeStatsState(withContext(Dispatchers.Default) { calculator.themeDetail(inAttempts, theme) })
			}
		}
	}

	companion object {
		/**
		 * Retourne la fabrique du ViewModel.
		 */
		fun factory(inContainer: AppContainer, inTheme: Theme): ViewModelProvider.Factory = viewModelFactory { initializer { ThemeStatsViewModel(inContainer, inTheme) } }
	}
}

/**
 * État chargé de la fiche d'un thème.
 *
 * @property detail fiche, ou null si le thème n'a jamais été joué.
 */
data class ThemeStatsState(val detail: ThemeDetail?)

/**
 * Fiche d'un thème.
 */
@Composable
fun ThemeStatsScreen(inViewModel: ThemeStatsViewModel, inTheme: Theme, inOnBack: () -> Unit, inOnTrain: () -> Unit, inOnReplay: (String) -> Unit) {
	val vState by inViewModel.state.collectAsStateWithLifecycle()
	ThemeStatsContent(inTheme, vState, System.currentTimeMillis(), inOnBack, inOnTrain, inOnReplay)
}

/**
 * Contenu de la fiche d'un thème, sans état métier (prévisualisable).
 */
@Composable
fun ThemeStatsContent(inTheme: Theme, inState: ThemeStatsState?, inNow: Long, inOnBack: () -> Unit, inOnTrain: () -> Unit, inOnReplay: (String) -> Unit) {
	val vColors = kLocalChessColors.current
	Column(Modifier.fillMaxSize().background(vColors.background).statusBarsPadding().navigationBarsPadding()) {
		ScreenTopBar(stringResource(themeLabel(inTheme)), inOnBack)
		val vDetail = inState?.detail
		LazyColumn(Modifier.weight(1f).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
			if (inState != null && vDetail == null) {
				item { Text(stringResource(R.string.stats_empty), style = ChessType.kBody, color = vColors.muted, modifier = Modifier.padding(vertical = 32.dp)) }
			}
			if (vDetail != null) {
				val vStat = vDetail.stat
				item {
					Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
						Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
							Tile(stringResource(R.string.theme_stats_rating), formatRating(vStat.rating), "", Modifier.weight(1f))
							Tile(stringResource(R.string.theme_stats_success), percent(vStat.rate), "${vStat.successes} / ${vStat.attempts}", Modifier.weight(1f))
						}
						Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
							Tile(stringResource(R.string.theme_stats_problems), "${vStat.attempts}", "", Modifier.weight(1f))
							Tile(stringResource(R.string.theme_stats_best), vStat.bestSolved?.let { formatRating(it) } ?: "–", "", Modifier.weight(1f))
						}
					}
				}
				if (vDetail.ratingHistory.size >= 2) {
					item {
						SurfaceCard {
							CardTitle(stringResource(R.string.theme_stats_rating_curve))
							LineChart(
								vDetail.ratingHistory.map { it.rating.toFloat() },
								Modifier.fillMaxWidth().height(130.dp),
								vDetail.ratingHistory.map { (it.rating - (it.deviation ?: 0.0)).toFloat() to (it.rating + (it.deviation ?: 0.0)).toFloat() },
							)
							Text(stringResource(R.string.stats_rating_band), style = ChessType.kCaption, color = vColors.muted)
						}
					}
				}
				if (vDetail.rollingRate.size >= 2) {
					item {
						SurfaceCard {
							CardTitle(stringResource(R.string.theme_stats_rolling), stringResource(R.string.theme_stats_rolling_now, percent(vDetail.rollingRate.last())))
							LineChart(vDetail.rollingRate, Modifier.fillMaxWidth().height(110.dp), inRange = 0f..1f)
							vStat.averageDurationMs?.let {
								Spacer(Modifier.height(8.dp))
								Text(stringResource(R.string.theme_stats_time, duration(it)), style = ChessType.kBody, color = vColors.text)
							}
						}
					}
				}
				item {
					SurfaceCard {
						CardTitle(stringResource(R.string.theme_stats_recent), stringResource(R.string.theme_stats_replay))
						for (vAttempt in vDetail.recent) {
							RecentRow(vAttempt, inNow) { inOnReplay(vAttempt.puzzleId) }
						}
					}
				}
			}
			item { Spacer(Modifier.height(4.dp)) }
		}
		PrimaryButton(stringResource(R.string.theme_stats_train), inOnTrain, Modifier.padding(horizontal = 20.dp, vertical = 12.dp), inIcon = R.drawable.ic_bolt_fill1, inHeight = 52)
	}
}

/**
 * Ligne d'une tentative récente : résultat, cote du problème, date.
 */
@Composable
private fun RecentRow(inAttempt: ThemeAttempt, inNow: Long, inOnClick: () -> Unit) {
	val vColors = kLocalChessColors.current
	Row(
		Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable(onClick = inOnClick).padding(vertical = 8.dp),
		verticalAlignment = Alignment.CenterVertically,
	) {
		Icon(
			painterResource(if (inAttempt.success) R.drawable.ic_check_circle_fill1 else R.drawable.ic_cancel_fill1),
			contentDescription = null,
			tint = if (inAttempt.success) vColors.success else vColors.error,
			modifier = Modifier.size(20.dp),
		)
		Spacer(Modifier.width(10.dp))
		Text(formatRating(inAttempt.puzzleRating), style = ChessType.kBodyStrong, color = vColors.text, modifier = Modifier.weight(1f))
		Text(formatElapsed(inAttempt.playedAt, inNow), style = ChessType.kCaption, color = vColors.muted)
		Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = null, tint = vColors.muted, modifier = Modifier.size(18.dp))
	}
}
