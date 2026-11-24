package com.chessplus.app.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
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
import com.chessplus.app.ui.components.ListGroup
import com.chessplus.app.ui.components.ListRow
import com.chessplus.app.ui.components.PrimaryButton
import com.chessplus.app.ui.components.Section
import com.chessplus.app.ui.components.StatTiles
import com.chessplus.app.ui.components.TopBar
import com.chessplus.app.ui.components.formatElapsed
import com.chessplus.app.ui.components.formatRating
import com.chessplus.app.ui.components.kIconSize
import com.chessplus.app.ui.theme.ChessPlusTheme
import com.chessplus.app.ui.themeLabel
import com.chessplus.core.model.Theme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Hauteur des courbes de la fiche. */
private const val kCurveHeight = 96

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
 * Contenu de la fiche d'un thème, sans état métier (prévisualisable) : tuiles, courbes, derniers
 * puzzles, et bouton principal d'entraînement en bas.
 */
@Composable
fun ThemeStatsContent(inTheme: Theme, inState: ThemeStatsState?, inNow: Long, inOnBack: () -> Unit, inOnTrain: () -> Unit, inOnReplay: (String) -> Unit) {
	val vColors = ChessPlusTheme.colors
	val vSpacing = ChessPlusTheme.spacing
	Column(Modifier.fillMaxSize().background(vColors.surface).statusBarsPadding().navigationBarsPadding()) {
		TopBar(stringResource(themeLabel(inTheme)), inOnBack)
		Column(
			Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = vSpacing.space4).padding(top = vSpacing.space2, bottom = vSpacing.space4),
			verticalArrangement = Arrangement.spacedBy(20.dp),
		) {
			val vDetail = inState?.detail
			if (inState != null && vDetail == null) {
				Text(stringResource(R.string.theme_stats_empty), style = ChessPlusTheme.typography.body, color = vColors.inkMuted)
			}
			if (vDetail != null) {
				val vStat = vDetail.stat
				StatTiles(
					listOf(
						stringResource(R.string.theme_stats_rating) to formatRating(vStat.rating),
						stringResource(R.string.theme_stats_success) to percent(vStat.rate),
						stringResource(R.string.theme_stats_problems) to formatRating(vStat.attempts),
						stringResource(R.string.theme_stats_best) to (vStat.bestSolved?.let { formatRating(it) } ?: "–"),
					)
				)
				if (vDetail.ratingHistory.size >= 2) {
					Section(stringResource(R.string.theme_stats_rating_curve)) {
						LineChart(vDetail.ratingHistory.map { it.rating.toFloat() }, Modifier.fillMaxWidth().height(kCurveHeight.dp))
					}
				}
				if (vDetail.rollingRate.size >= 2) {
					Section(stringResource(R.string.theme_stats_rolling)) {
						Text(
							stringResource(R.string.theme_stats_rolling_now, percent(vDetail.rollingRate.last())),
							style = ChessPlusTheme.typography.moves,
							color = vColors.ink,
						)
						LineChart(vDetail.rollingRate, Modifier.fillMaxWidth().height(kCurveHeight.dp), inRange = 0f..1f)
					}
				}
				vStat.averageDurationMs?.let { inAverage ->
					ListGroup(
						inRows = listOf {
							ListRow {
								Text(stringResource(R.string.theme_stats_time), style = ChessPlusTheme.typography.body, color = vColors.ink, modifier = Modifier.weight(1f))
								Text(duration(inAverage), style = ChessPlusTheme.typography.moves, color = vColors.ink)
							}
						}
					)
				}
				if (vDetail.recent.isNotEmpty()) {
					Section(stringResource(R.string.theme_stats_recent)) {
						ListGroup(inRows = vDetail.recent.map { vAttempt -> { RecentRow(vAttempt, inNow) { inOnReplay(vAttempt.puzzleId) } } })
					}
				}
			}
		}
		PrimaryButton(
			stringResource(R.string.theme_stats_train),
			inOnTrain,
			Modifier.fillMaxWidth().padding(horizontal = vSpacing.space4).padding(top = vSpacing.space2, bottom = vSpacing.space4),
			inIcon = R.drawable.ic_ln_target,
		)
	}
}

/**
 * Ligne d'une tentative récente : résultat, classement du puzzle en mono, date, chevron.
 */
@Composable
private fun RecentRow(inAttempt: ThemeAttempt, inNow: Long, inOnClick: () -> Unit) {
	val vColors = ChessPlusTheme.colors
	ListRow(inOnClick = inOnClick) {
		Icon(
			painterResource(if (inAttempt.success) R.drawable.ic_ln_check else R.drawable.ic_ln_close),
			contentDescription = null,
			tint = if (inAttempt.success) vColors.success else vColors.danger,
			modifier = Modifier.size(kIconSize),
		)
		Text(formatRating(inAttempt.puzzleRating), style = ChessPlusTheme.typography.moves, color = vColors.ink, modifier = Modifier.weight(1f))
		Text(formatElapsed(inAttempt.playedAt, inNow), style = ChessPlusTheme.typography.small, color = vColors.inkMuted)
		Icon(painterResource(R.drawable.ic_ln_chevron), contentDescription = stringResource(R.string.theme_stats_replay), tint = vColors.inkMuted, modifier = Modifier.size(kIconSize))
	}
}
