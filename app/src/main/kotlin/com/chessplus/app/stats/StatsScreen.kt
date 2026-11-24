package com.chessplus.app.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.chessplus.app.ui.ThemeFamily
import com.chessplus.app.ui.charts.BarRow
import com.chessplus.app.ui.charts.CalendarHeatmap
import com.chessplus.app.ui.charts.ColumnChart
import com.chessplus.app.ui.charts.ColumnItem
import com.chessplus.app.ui.charts.LineChart
import com.chessplus.app.ui.charts.StackSegment
import com.chessplus.app.ui.charts.StackedBar
import com.chessplus.app.ui.components.FloatingNavBar
import com.chessplus.app.ui.components.LinkRow
import com.chessplus.app.ui.components.ListGroup
import com.chessplus.app.ui.components.ListRow
import com.chessplus.app.ui.components.RowTexts
import com.chessplus.app.ui.components.NavTab
import com.chessplus.app.ui.components.Overline
import com.chessplus.app.ui.components.PageTitle
import com.chessplus.app.ui.components.PrimaryButton
import com.chessplus.app.ui.components.Section
import com.chessplus.app.ui.components.SurfaceCard
import com.chessplus.app.ui.components.SegmentedControl
import com.chessplus.app.ui.components.StatTiles
import com.chessplus.app.ui.components.formatDelta
import com.chessplus.app.ui.components.formatRating
import com.chessplus.app.ui.components.kThinSpace
import com.chessplus.app.ui.theme.ChessPlusTheme
import com.chessplus.app.ui.themeFamily
import com.chessplus.app.ui.themeLabel
import com.chessplus.core.model.Theme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/** Nombre minimal de tentatives pour qu'un thème figure dans « Réussite par thème ». */
private const val kMinAttemptsPerTheme = 3

/** Nombre de thèmes affichés avant « Voir tous les thèmes ». */
private const val kThemesPreview = 4

/** Nombre de thèmes jamais rencontrés affichés. */
private const val kUnseenPreview = 12

/** Nombre minimal de tentatives pour afficher une tranche de difficulté. */
private const val kMinAttemptsPerBand = 3

/** Hauteur de la courbe du classement et du calendrier d'activité (maquette). */
private const val kChartHeight = 96

/** Hauteur des graphiques en colonnes. */
private const val kColumnsHeight = 110

/** Hauteur du graphique de réussite selon la longueur (une ligne d'effectif en plus). */
private const val kLengthChartHeight = 140

/** Familles comparées dans « Par famille » (la longueur a sa propre section, l'évaluation n'est pas un motif). */
private val kComparedFamilies = setOf(ThemeFamily.Tactics, ThemeFamily.Mates, ThemeFamily.Endgames, ThemeFamily.Phase)

/** Durée d'un jour en millisecondes. */
private const val kDayMs = 24L * 60 * 60 * 1000

/** Nombre de parties de défi sur la courbe des scores. */
private const val kChallengeRuns = 30

/** Hauteur de la courbe des scores d'un défi. */
private const val kChallengeChartHeight = 80

/**
 * Données de l'écran : rapport calculé, records et scores des défis.
 *
 * @property report rapport des tentatives.
 * @property rushBest meilleur score en Rush, ou null.
 * @property survivalBest meilleur score en Survie, ou null.
 * @property survivalScores scores des parties de Survie, de la plus ancienne à la plus récente.
 * @property rushScores scores des parties de Rush, de la plus ancienne à la plus récente.
 */
data class StatsData(
	val report: StatsReport,
	val rushBest: Int? = null,
	val survivalBest: Int? = null,
	val survivalScores: List<Int> = emptyList(),
	val rushScores: List<Int> = emptyList(),
)

/**
 * ViewModel des statistiques : recalcule le rapport à chaque nouvelle tentative et suit les records
 * des défis.
 *
 * @property container dépendances de l'application.
 */
class StatsViewModel(private val container: AppContainer) : ViewModel() {
	/** Données publiées, ou null pendant le chargement. */
	private val mutableData = MutableStateFlow<StatsData?>(null)

	/** Données de l'écran. */
	val data: StateFlow<StatsData?> = mutableData.asStateFlow()

	/** Calcul des statistiques. */
	private val calculator = StatsCalculator()

	/** Scores des parties de défi, par clé de défi, dans l'ordre chronologique. */
	private var scores: Map<String, List<Int>> = emptyMap()

	/** Dernier rapport calculé. */
	private var report: StatsReport? = null

	init {
		viewModelScope.launch {
			val vDao = container.ready().second.userDao()
			launch {
				vDao.observeChallengeRuns().collect { inRuns ->
					scores = inRuns.groupBy({ it.kind }, { it.score })
					publish()
				}
			}
			vDao.observeAllAttempts().collect { inAttempts ->
				report = withContext(Dispatchers.Default) { calculator.report(inAttempts, System.currentTimeMillis()) }
				publish()
			}
		}
	}

	/**
	 * Publie les données dès que le rapport est prêt.
	 */
	private fun publish() {
		val vReport = report ?: return
		val vSurvival = scores[ChallengeKind.Survival.key].orEmpty()
		val vRush = scores[ChallengeKind.Rush.key].orEmpty()
		mutableData.value = StatsData(vReport, vRush.maxOrNull(), vSurvival.maxOrNull(), vSurvival, vRush)
	}

	companion object {
		/**
		 * Retourne la fabrique du ViewModel.
		 */
		fun factory(inContainer: AppContainer): ViewModelProvider.Factory = viewModelFactory { initializer { StatsViewModel(inContainer) } }
	}
}

/**
 * Écran Statistiques (onglet).
 */
@Composable
fun StatsScreen(
	inViewModel: StatsViewModel,
	inOnTheme: (Theme) -> Unit,
	inOnTrainThemes: (Set<Theme>) -> Unit,
	inOnHistory: () -> Unit = {},
	inOnTab: (NavTab) -> Unit,
) {
	val vData by inViewModel.data.collectAsStateWithLifecycle()
	StatsContent(vData, System.currentTimeMillis(), inOnTheme, inOnTrainThemes, inOnHistory, inOnTab)
}

/**
 * Contenu des statistiques, sans état métier (prévisualisable). [inData] vaut null pendant le
 * chargement. La colonne défile, la barre de navigation reste fixe.
 */
@Composable
fun StatsContent(
	inData: StatsData?,
	inNow: Long,
	inOnTheme: (Theme) -> Unit,
	inOnTrainThemes: (Set<Theme>) -> Unit,
	inOnHistory: () -> Unit,
	inOnTab: (NavTab) -> Unit,
) {
	val vColors = ChessPlusTheme.colors
	val vSpacing = ChessPlusTheme.spacing
	Column(Modifier.fillMaxSize().background(vColors.surface).statusBarsPadding()) {
		Column(
			Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = vSpacing.space4).padding(top = vSpacing.space2, bottom = vSpacing.space4),
			verticalArrangement = Arrangement.spacedBy(20.dp),
		) {
			PageTitle(stringResource(R.string.stats_page_title))
			val vReport = inData?.report
			if (vReport != null && vReport.attempts == 0) {
				Text(stringResource(R.string.stats_none), style = ChessPlusTheme.typography.body, color = vColors.inkMuted)
			} else if (inData != null && vReport != null) {
				RatingSection(vReport, inNow)
				SummaryTiles(inData)
				ActivitySection(vReport)
				ThemesSection(vReport, inOnTheme)
				WeakSection(vReport, inOnTheme, inOnTrainThemes)
				VolumeSection(vReport)
				FamiliesSection(vReport)
				DifficultySection(vReport)
				ProfileSection(vReport)
				LengthSection(vReport)
				DayPartsSection(vReport)
				TimeSection(vReport)
				ErrorsSection(vReport)
				RecordsSection(inData)
				ChallengesSection(inData)
				CoverageSection(vReport)
			}
			if (inData != null) {
				ListGroup(inRows = listOf { LinkRow(stringResource(R.string.stats_history), null, inOnHistory, stringResource(R.string.stats_history_detail)) })
			}
		}
		FloatingNavBar(NavTab.Stats, inOnTab)
	}
}

/**
 * Classement : grand chiffre, variation sur la période, courbe et choix de la période.
 */
@Composable
private fun RatingSection(inReport: StatsReport, inNow: Long) {
	val vColors = ChessPlusTheme.colors
	var vPeriod by rememberSaveable { mutableIntStateOf(1) }
	val vFrom = when (vPeriod) {
		0 -> inNow - 7 * kDayMs
		1 -> inNow - 30 * kDayMs
		2 -> inNow - 365 * kDayMs
		else -> Long.MIN_VALUE
	}
	val vSamples = (listOfNotNull(inReport.ratingHistory.lastOrNull { it.time < vFrom }) + inReport.ratingHistory.filter { it.time >= vFrom })
		.ifEmpty { listOfNotNull(inReport.ratingHistory.lastOrNull()) }
	Column {
		Overline(stringResource(R.string.stats_rating_section))
		Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
			Text(inReport.currentRating?.let { formatRating(it) } ?: "–", style = ChessPlusTheme.typography.display, color = vColors.ink)
			if (vSamples.size >= 2) {
				val vDelta = (vSamples.last().rating - vSamples.first().rating).roundToInt()
				val vLabel = when (vPeriod) {
					0 -> R.string.stats_delta_7
					1 -> R.string.stats_delta_30
					2 -> R.string.stats_delta_year
					else -> R.string.stats_delta_all
				}
				Text(
					stringResource(vLabel, formatDelta(vDelta)),
					style = ChessPlusTheme.typography.moves,
					color = if (vDelta >= 0) vColors.success else vColors.danger,
					modifier = Modifier.padding(bottom = 6.dp),
				)
			}
		}
		Spacer(Modifier.height(ChessPlusTheme.spacing.space2))
		LineChart(vSamples.map { it.rating.toFloat() }, Modifier.fillMaxWidth().height(kChartHeight.dp))
		Spacer(Modifier.height(ChessPlusTheme.spacing.space3))
		SegmentedControl(
			listOf(stringResource(R.string.stats_range_7), stringResource(R.string.stats_range_30), stringResource(R.string.stats_range_year), stringResource(R.string.stats_range_all)),
			vPeriod,
		) { vPeriod = it }
	}
}

/**
 * Tuiles 2 × 2 : résolus, réussite, série, chrono record (meilleur score en Rush).
 */
@Composable
private fun SummaryTiles(inData: StatsData) {
	val vReport = inData.report
	StatTiles(
		listOf(
			stringResource(R.string.stats_tile_solved) to formatRating(vReport.successes),
			stringResource(R.string.stats_tile_success) to (vReport.successRate?.let { percent(it) } ?: "–"),
			stringResource(R.string.stats_tile_streak) to stringResource(R.string.stats_days_short, vReport.dayStreak),
			stringResource(R.string.stats_tile_rush) to (inData.rushBest?.let { formatRating(it) } ?: "–"),
		)
	)
}

/**
 * Calendrier d'activité sur 12 semaines.
 */
@Composable
private fun ActivitySection(inReport: StatsReport) {
	Section(stringResource(R.string.stats_activity)) {
		CalendarHeatmap(inReport.calendar.map { it.count }, Modifier.fillMaxWidth().height(kChartHeight.dp))
	}
}

/**
 * Lien texte en `accent` (afficher plus ou moins).
 */
@Composable
private fun TextLink(inText: String, inOnClick: () -> Unit) {
	Text(
		inText,
		style = ChessPlusTheme.typography.small.copy(fontWeight = FontWeight(600)),
		color = ChessPlusTheme.colors.accent,
		modifier = Modifier.clip(RoundedCornerShape(ChessPlusTheme.radius.radiusXs)).clickable(onClick = inOnClick).padding(vertical = 6.dp),
	)
}

/**
 * Réussite par thème : thèmes entraînables joués au moins 3 fois, du mieux réussi au moins bien réussi.
 */
@Composable
private fun ThemesSection(inReport: StatsReport, inOnTheme: (Theme) -> Unit) {
	var vShowAll by rememberSaveable { mutableStateOf(false) }
	val vSorted = inReport.themes
		.filter { it.attempts >= kMinAttemptsPerTheme && themeFamily(it.theme) in StatsCalculator.kTrainableFamilies }
		.sortedWith(compareByDescending<ThemeStat> { it.rate }.thenByDescending { it.attempts })
	if (vSorted.isEmpty()) {
		return
	}
	Section(stringResource(R.string.stats_themes_title)) {
		Column {
			for (vStat in if (vShowAll) vSorted else vSorted.take(kThemesPreview)) {
				BarRow(stringResource(themeLabel(vStat.theme)), percent(vStat.rate), vStat.rate, inOnClick = { inOnTheme(vStat.theme) })
			}
			if (vSorted.size > kThemesPreview) {
				TextLink(if (vShowAll) stringResource(R.string.stats_show_less) else stringResource(R.string.stats_show_all, vSorted.size)) { vShowAll = !vShowAll }
			}
		}
	}
}

/**
 * Les trois thèmes les plus faibles et l'entraînement ciblé sur eux (seul bouton principal de l'écran).
 */
@Composable
private fun WeakSection(inReport: StatsReport, inOnTheme: (Theme) -> Unit, inOnTrainThemes: (Set<Theme>) -> Unit) {
	Section(stringResource(R.string.stats_weak_title)) {
		if (inReport.weakest.isEmpty()) {
			Text(stringResource(R.string.stats_weak_empty), style = ChessPlusTheme.typography.small, color = ChessPlusTheme.colors.inkMuted)
		} else {
			Column {
				for (vStat in inReport.weakest) {
					BarRow(stringResource(themeLabel(vStat.theme)), percent(vStat.rate), vStat.rate, pluralStringResource(R.plurals.stats_problems, vStat.attempts, vStat.attempts)) { inOnTheme(vStat.theme) }
				}
			}
			PrimaryButton(stringResource(R.string.stats_weak_train), { inOnTrainThemes(inReport.weakest.map { it.theme }.toSet()) }, Modifier.fillMaxWidth(), inIcon = R.drawable.ic_ln_target)
		}
	}
}

/**
 * Puzzles joués par semaine.
 */
@Composable
private fun VolumeSection(inReport: StatsReport) {
	val vMax = inReport.weeklyVolume.maxOfOrNull { it.attempts }?.coerceAtLeast(1) ?: 1
	val vFormat = SimpleDateFormat("d/M", Locale.FRANCE)
	Section(stringResource(R.string.stats_volume)) {
		ColumnChart(
			inReport.weeklyVolume.map { ColumnItem(vFormat.format(Date(it.weekStart)), it.attempts.toFloat() / vMax, if (it.attempts > 0) "${it.attempts}" else null) },
			Modifier.height(kColumnsHeight.dp),
		)
	}
}

/**
 * Liste de barres de réussite de groupes (famille, longueur, moment).
 */
@Composable
private fun <K> GroupBars(inTitle: String, inGroups: List<GroupStat<K>>, inLabel: @Composable (K) -> String) {
	if (inGroups.isEmpty()) {
		return
	}
	Section(inTitle) {
		Column {
			for (vGroup in inGroups) {
				BarRow(inLabel(vGroup.key), percent(vGroup.rate), vGroup.rate, pluralStringResource(R.plurals.stats_problems, vGroup.attempts, vGroup.attempts))
			}
		}
	}
}

/**
 * Réussite par famille de thèmes.
 */
@Composable
private fun FamiliesSection(inReport: StatsReport) {
	GroupBars(stringResource(R.string.stats_families), inReport.families.filter { it.attempts > 0 && it.key in kComparedFamilies }) { stringResource(it.label) }
}

/**
 * Réussite par tranche de difficulté des puzzles.
 */
@Composable
private fun DifficultySection(inReport: StatsReport) {
	val vBands = inReport.difficulty.filter { it.attempts >= kMinAttemptsPerBand }
	if (vBands.isEmpty()) {
		return
	}
	Section(stringResource(R.string.stats_difficulty)) {
		ColumnChart(vBands.map { ColumnItem(formatRating(it.key), it.rate, percent(it.rate)) }, Modifier.height(kColumnsHeight.dp))
	}
}

/**
 * Répartition des fins de puzzle : réussi, indice, erreur.
 */
@Composable
private fun ProfileSection(inReport: StatsReport) {
	val vColors = ChessPlusTheme.colors
	Section(stringResource(R.string.stats_profile)) {
		Column {
			StackedBar(
				listOf(
					StackSegment(stringResource(R.string.stats_profile_clean), inReport.profile.clean, vColors.success),
					StackSegment(stringResource(R.string.stats_profile_hint), inReport.profile.hint, vColors.lineStrong),
					StackSegment(stringResource(R.string.stats_profile_mistake), inReport.profile.mistake, vColors.danger),
				)
			)
		}
	}
}

/**
 * Réussite selon la longueur de la solution : en abscisse le nombre de coups à trouver (thèmes de
 * longueur de Lichess : 1, 2, 3, 4 et plus), en ordonnée le taux de réussite sur une échelle fixe de
 * 0 à 100 %, avec le nombre de puzzles joués sous chaque colonne.
 */
@Composable
private fun LengthSection(inReport: StatsReport) {
	if (inReport.lengths.none { it.attempts > 0 }) {
		return
	}
	Section(stringResource(R.string.stats_length)) {
		Text(stringResource(R.string.stats_length_axis), style = ChessPlusTheme.typography.small, color = ChessPlusTheme.colors.inkMuted)
		ColumnChart(
			inReport.lengths.map { vGroup ->
				ColumnItem(
					label = when (vGroup.key) {
						PuzzleLength.VeryLong -> stringResource(R.string.stats_length_more)
						else -> pluralStringResource(R.plurals.stats_length_moves, vGroup.key.ordinal + 1, vGroup.key.ordinal + 1)
					},
					fraction = if (vGroup.attempts > 0) vGroup.rate else 0f,
					value = if (vGroup.attempts > 0) percent(vGroup.rate) else "–",
					detail = pluralStringResource(R.plurals.stats_length_played, vGroup.attempts, vGroup.attempts),
				)
			},
			Modifier.height(kLengthChartHeight.dp),
		)
	}
}

/**
 * Réussite selon le moment de la journée.
 */
@Composable
private fun DayPartsSection(inReport: StatsReport) {
	GroupBars(stringResource(R.string.stats_dayparts), inReport.dayParts.filter { it.attempts > 0 }) {
		stringResource(
			when (it) {
				DayPart.Morning -> R.string.stats_daypart_morning
				DayPart.Afternoon -> R.string.stats_daypart_afternoon
				DayPart.Evening -> R.string.stats_daypart_evening
				DayPart.Night -> R.string.stats_daypart_night
			}
		)
	}
}

/**
 * Temps de résolution : moyenne, thèmes extrêmes, médiane par semaine.
 */
@Composable
private fun TimeSection(inReport: StatsReport) {
	val vColors = ChessPlusTheme.colors
	Section(stringResource(R.string.stats_time)) {
		val vAverage = inReport.averageDurationMs
		if (vAverage == null) {
			Text(stringResource(R.string.stats_time_empty), style = ChessPlusTheme.typography.small, color = vColors.inkMuted)
			return@Section
		}
		val vTimed = inReport.themes.filter { it.averageDurationMs != null && it.successes >= kMinAttemptsPerTheme && themeFamily(it.theme) in StatsCalculator.kTrainableFamilies }
		val vRows = mutableListOf<@Composable () -> Unit>({ ValueRow(stringResource(R.string.stats_time_average), null, duration(vAverage)) })
		if (vTimed.size >= 2) {
			val vFastest = vTimed.minBy { it.averageDurationMs!! }
			val vSlowest = vTimed.maxBy { it.averageDurationMs!! }
			vRows.add { ValueRow(stringResource(R.string.stats_time_fastest), stringResource(themeLabel(vFastest.theme)), duration(vFastest.averageDurationMs!!)) }
			vRows.add { ValueRow(stringResource(R.string.stats_time_slowest), stringResource(themeLabel(vSlowest.theme)), duration(vSlowest.averageDurationMs!!)) }
		}
		ListGroup(inRows = vRows)
		if (inReport.weeklyDurationMs.size >= 2) {
			Overline(stringResource(R.string.stats_time_weekly), Modifier.padding(top = ChessPlusTheme.spacing.space2))
			val vMax = inReport.weeklyDurationMs.maxOf { it.second }.toFloat()
			val vFormat = SimpleDateFormat("d/M", Locale.FRANCE)
			ColumnChart(
				inReport.weeklyDurationMs.takeLast(8).map { ColumnItem(vFormat.format(Date(it.first)), it.second / vMax, stringResource(R.string.duration_seconds, (it.second / 1000).toInt())) },
				Modifier.height(kColumnsHeight.dp),
			)
		}
	}
}

/**
 * Ligne de liste avec une valeur en mono à droite.
 */
@Composable
private fun ValueRow(inTitle: String, inSubtitle: String?, inValue: String) {
	ListRow {
		RowTexts(inTitle, inSubtitle)
		Text(inValue, style = ChessPlusTheme.typography.moves, color = ChessPlusTheme.colors.ink, maxLines = 1)
	}
}

/**
 * Coup où le puzzle échoue.
 */
@Composable
private fun ErrorsSection(inReport: StatsReport) {
	val vTotal = inReport.failedAtMove.sum()
	Section(stringResource(R.string.stats_errors)) {
		if (vTotal == 0) {
			Text(stringResource(R.string.stats_errors_empty), style = ChessPlusTheme.typography.small, color = ChessPlusTheme.colors.inkMuted)
		} else {
			val vLabels = listOf(R.string.stats_errors_move1, R.string.stats_errors_move2, R.string.stats_errors_move3)
			Column {
				inReport.failedAtMove.forEachIndexed { inIndex, inCount ->
					val vRate = inCount.toFloat() / vTotal
					BarRow(stringResource(vLabels[inIndex]), percent(vRate), vRate, pluralStringResource(R.plurals.stats_problems, inCount, inCount))
				}
			}
		}
	}
}

/**
 * Records : classement, puzzle le plus difficile, séries, défis.
 */
@Composable
private fun RecordsSection(inData: StatsData) {
	RecordsContent(inData)
}

/**
 * Évolution des scores de Survie et de Rush : une courbe par défi sur les dernières parties, avec
 * le record et le nombre de parties.
 */
@Composable
private fun ChallengesSection(inData: StatsData) {
	if (inData.survivalScores.isEmpty() && inData.rushScores.isEmpty()) {
		return
	}
	Section(stringResource(R.string.stats_challenges)) {
		ChallengeCurve(stringResource(R.string.stats_challenge_survival), inData.survivalScores)
		ChallengeCurve(stringResource(R.string.stats_challenge_rush), inData.rushScores)
	}
}

/**
 * Courbe des scores d'un défi : titre, record et nombre de parties, puis la courbe des
 * [kChallengeRuns] dernières parties (ou une invitation à jouer s'il y en a moins de deux).
 */
@Composable
private fun ChallengeCurve(inTitle: String, inScores: List<Int>) {
	val vColors = ChessPlusTheme.colors
	SurfaceCard {
		Row(verticalAlignment = Alignment.Bottom) {
			Text(inTitle, style = ChessPlusTheme.typography.headingSmall, color = vColors.ink, modifier = Modifier.weight(1f))
			if (inScores.isNotEmpty()) {
				Text(
					stringResource(R.string.stats_challenge_detail, inScores.max(), pluralStringResource(R.plurals.stats_challenge_runs, inScores.size, inScores.size)),
					style = ChessPlusTheme.typography.movesSmall,
					color = vColors.inkMuted,
				)
			}
		}
		Spacer(Modifier.height(ChessPlusTheme.spacing.space2))
		if (inScores.size < 2) {
			Text(stringResource(R.string.stats_challenge_empty), style = ChessPlusTheme.typography.small, color = vColors.inkMuted)
		} else {
			LineChart(inScores.takeLast(kChallengeRuns).map { it.toFloat() }, Modifier.fillMaxWidth().height(kChallengeChartHeight.dp), 0f..(inScores.max().toFloat() * 1.1f).coerceAtLeast(1f))
		}
	}
}

/**
 * Contenu de la section des records.
 */
@Composable
private fun RecordsContent(inData: StatsData) {
	val vReport = inData.report
	Section(stringResource(R.string.stats_records)) {
		StatTiles(
			listOf(
				stringResource(R.string.stats_record_rating) to (vReport.bestRating?.let { formatRating(it) } ?: "–"),
				stringResource(R.string.stats_record_solved) to (vReport.bestSolved?.let { formatRating(it.rating) } ?: "–"),
				stringResource(R.string.stats_record_days) to stringResource(R.string.stats_days_short, vReport.bestDayStreak),
				stringResource(R.string.stats_record_run) to formatRating(vReport.bestSuccessRun),
				stringResource(R.string.stats_record_survival) to (inData.survivalBest?.let { formatRating(it) } ?: "–"),
				stringResource(R.string.stats_record_rush) to (inData.rushBest?.let { formatRating(it) } ?: "–"),
			)
		)
	}
}

/**
 * Couverture : thèmes rencontrés et jamais rencontrés.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CoverageSection(inReport: StatsReport) {
	val vColors = ChessPlusTheme.colors
	val vTotal = Theme.entries.size
	Section(stringResource(R.string.stats_coverage_title)) {
		BarRow(stringResource(R.string.stats_coverage), stringResource(R.string.stats_coverage_value, inReport.seenThemes, vTotal), inReport.seenThemes.toFloat() / vTotal)
		if (inReport.unseenThemes.isNotEmpty()) {
			Text(stringResource(R.string.stats_coverage_unseen), style = ChessPlusTheme.typography.small, color = vColors.inkMuted)
			FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
				val vShape = RoundedCornerShape(ChessPlusTheme.radius.radiusSm)
				for (vTheme in inReport.unseenThemes.take(kUnseenPreview)) {
					Box(Modifier.clip(vShape).background(vColors.surfaceRaised).border(1.dp, vColors.line, vShape).padding(horizontal = ChessPlusTheme.spacing.space2, vertical = 4.dp)) {
						Text(stringResource(themeLabel(vTheme)), style = ChessPlusTheme.typography.small, color = vColors.ink)
					}
				}
				if (inReport.unseenThemes.size > kUnseenPreview) {
					Text(
						"+${inReport.unseenThemes.size - kUnseenPreview}",
						style = ChessPlusTheme.typography.movesSmall,
						color = vColors.inkMuted,
						textAlign = TextAlign.Center,
						modifier = Modifier.padding(4.dp),
					)
				}
			}
		}
	}
}

/**
 * Formate un taux de 0 à 1 en pourcentage entier avec une espace fine insécable (« 68 % »).
 */
fun percent(inRate: Float): String = "${(inRate * 100).roundToInt()}$kThinSpace%"

/**
 * Formate une durée (« 38 s », « 1 min 05 s »).
 */
@Composable
fun duration(inMs: Long): String {
	val vSeconds = (inMs / 1000).toInt()
	return if (vSeconds < 60) stringResource(R.string.duration_seconds, vSeconds) else stringResource(R.string.duration_minutes, vSeconds / 60, vSeconds % 60)
}
