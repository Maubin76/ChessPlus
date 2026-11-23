package com.chessplus.app.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
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
import com.chessplus.app.ui.ThemeFamily
import com.chessplus.app.ui.charts.BarRow
import com.chessplus.app.ui.charts.CalendarHeatmap
import com.chessplus.app.ui.charts.ColumnChart
import com.chessplus.app.ui.charts.ColumnItem
import com.chessplus.app.ui.charts.HeatmapLegend
import com.chessplus.app.ui.charts.LineChart
import com.chessplus.app.ui.charts.StackSegment
import com.chessplus.app.ui.charts.StackedBar
import com.chessplus.app.ui.components.BottomNavBar
import com.chessplus.app.ui.components.NavTab
import com.chessplus.app.ui.components.PrimaryButton
import com.chessplus.app.ui.components.ScreenTopBar
import com.chessplus.app.ui.components.SegmentedControl
import com.chessplus.app.ui.components.SurfaceCard
import com.chessplus.app.ui.components.formatDelta
import com.chessplus.app.ui.components.formatRating
import com.chessplus.app.ui.theme.ChessType
import com.chessplus.app.ui.theme.kLocalChessColors
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

/** Nombre minimal de tentatives pour qu'un thème figure dans « Forces et faiblesses ». */
private const val kMinAttemptsPerTheme = 3

/** Nombre de thèmes affichés avant « Voir tous les thèmes ». */
private const val kThemesPreview = 6

/** Nombre de thèmes jamais rencontrés affichés. */
private const val kUnseenPreview = 14

/** Rapport largeur / hauteur du calendrier d'activité (17 semaines de 7 jours, espaces compris). */
private const val kHeatmapRatio = 2.35f

/** Nombre minimal de tentatives pour afficher une tranche de difficulté. */
private const val kMinAttemptsPerBand = 3

/** Familles comparées dans « Par famille » (la longueur a sa propre carte, l'évaluation n'est pas un motif). */
private val kComparedFamilies = setOf(ThemeFamily.Tactics, ThemeFamily.Mates, ThemeFamily.Endgames, ThemeFamily.Phase)

/** Durée d'un jour en millisecondes. */
private const val kDayMs = 24L * 60 * 60 * 1000

/**
 * ViewModel des statistiques : recalcule le rapport à chaque nouvelle tentative.
 *
 * @property container dépendances de l'application.
 */
class StatsViewModel(private val container: AppContainer) : ViewModel() {
	/** Rapport publié, ou null pendant le chargement. */
	private val mutableReport = MutableStateFlow<StatsReport?>(null)

	/** Rapport de l'écran. */
	val report: StateFlow<StatsReport?> = mutableReport.asStateFlow()

	/** Calcul des statistiques. */
	private val calculator = StatsCalculator()

	init {
		viewModelScope.launch {
			container.ready().second.userDao().observeAllAttempts().collect { inAttempts ->
				mutableReport.value = withContext(Dispatchers.Default) { calculator.report(inAttempts, System.currentTimeMillis()) }
			}
		}
	}

	companion object {
		/**
		 * Retourne la fabrique du ViewModel.
		 */
		fun factory(inContainer: AppContainer): ViewModelProvider.Factory = viewModelFactory { initializer { StatsViewModel(inContainer) } }
	}
}

/**
 * Écran Statistiques.
 */
@Composable
fun StatsScreen(inViewModel: StatsViewModel, inOnTheme: (Theme) -> Unit, inOnTrainThemes: (Set<Theme>) -> Unit, inOnTab: (NavTab) -> Unit) {
	val vReport by inViewModel.report.collectAsStateWithLifecycle()
	StatsContent(vReport, System.currentTimeMillis(), inOnTheme, inOnTrainThemes, inOnTab)
}

/**
 * Contenu des statistiques, sans état métier (prévisualisable). [inReport] vaut null pendant le chargement.
 */
@Composable
fun StatsContent(inReport: StatsReport?, inNow: Long, inOnTheme: (Theme) -> Unit, inOnTrainThemes: (Set<Theme>) -> Unit, inOnTab: (NavTab) -> Unit) {
	val vColors = kLocalChessColors.current
	Column(Modifier.fillMaxSize().background(vColors.background).statusBarsPadding()) {
		ScreenTopBar(stringResource(R.string.nav_stats))
		if (inReport != null && inReport.attempts == 0) {
			Box(Modifier.weight(1f).fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
				Text(stringResource(R.string.stats_empty), style = ChessType.kBody, color = vColors.muted, textAlign = TextAlign.Center)
			}
		} else if (inReport != null) {
			LazyColumn(Modifier.weight(1f).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
				item { SummaryTiles(inReport) }
				item { RatingCard(inReport, inNow) }
				item { ActivityCard(inReport) }
				item { VolumeCard(inReport) }
				item { ThemesCard(inReport, inOnTheme) }
				item { WeakCard(inReport, inOnTheme, inOnTrainThemes) }
				item { FamiliesCard(inReport) }
				item { DifficultyCard(inReport) }
				item { ProfileCard(inReport) }
				item { LengthCard(inReport) }
				item { DayPartsCard(inReport) }
				item { TimeCard(inReport) }
				item { ErrorsCard(inReport) }
				item { RecordsCard(inReport) }
				item { CoverageCard(inReport) }
				item { Spacer(Modifier.height(8.dp)) }
			}
		} else {
			Spacer(Modifier.weight(1f))
		}
		BottomNavBar(NavTab.Stats, inOnTab)
	}
}

/**
 * Titre de carte et précision facultative.
 */
@Composable
internal fun CardTitle(inTitle: String, inDetail: String? = null) {
	val vColors = kLocalChessColors.current
	Text(inTitle, style = ChessType.kSection, color = vColors.text)
	inDetail?.let { Text(it, style = ChessType.kCaption, color = vColors.muted) }
	Spacer(Modifier.height(10.dp))
}

/**
 * Tuile du résumé : libellé, grande valeur, précision ([inDetail] vide : ligne réservée, null : pas de ligne).
 */
@Composable
internal fun Tile(inLabel: String, inValue: String, inDetail: String?, inModifier: Modifier, inDetailColor: Color? = null) {
	val vColors = kLocalChessColors.current
	Column(inModifier.background(vColors.surface, RoundedCornerShape(20.dp)).padding(16.dp)) {
		Text(inLabel, style = ChessType.kCaption, color = vColors.muted)
		Text(inValue, style = ChessType.kScreenTitle, color = vColors.text, maxLines = 1)
		inDetail?.let { Text(it, style = ChessType.kCaption, color = inDetailColor ?: vColors.muted, maxLines = 1) }
	}
}

/**
 * Résumé en quatre tuiles : cote, résolus, série, réussite.
 */
@Composable
private fun SummaryTiles(inReport: StatsReport) {
	val vColors = kLocalChessColors.current
	Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
		Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
			val vDelta = inReport.ratingDelta30Days
			Tile(
				stringResource(R.string.stats_tile_rating),
				inReport.currentRating?.let { formatRating(it) } ?: "–",
				vDelta?.let { stringResource(R.string.stats_delta_30, formatDelta(it)) } ?: "",
				Modifier.weight(1f),
				if (vDelta == null) null else if (vDelta >= 0) vColors.success else vColors.error,
			)
			Tile(
				stringResource(R.string.stats_tile_solved),
				formatRating(inReport.successes),
				stringResource(R.string.stats_tile_solved_detail, formatRating(inReport.attempts)),
				Modifier.weight(1f),
			)
		}
		Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
			Tile(
				stringResource(R.string.stats_tile_streak),
				pluralStringResource(R.plurals.stats_days, inReport.dayStreak, inReport.dayStreak),
				stringResource(R.string.stats_tile_streak_detail, pluralStringResource(R.plurals.stats_days, inReport.bestDayStreak, inReport.bestDayStreak)),
				Modifier.weight(1f),
			)
			Tile(
				stringResource(R.string.stats_tile_success),
				inReport.successRate?.let { percent(it) } ?: "–",
				stringResource(R.string.stats_tile_success_detail),
				Modifier.weight(1f),
			)
		}
	}
}

/**
 * Courbe de la cote avec sa bande d'incertitude, sur 7 jours, 30 jours ou tout l'historique.
 */
@Composable
private fun RatingCard(inReport: StatsReport, inNow: Long) {
	val vColors = kLocalChessColors.current
	var vPeriod by rememberSaveable { mutableIntStateOf(1) }
	SurfaceCard {
		CardTitle(stringResource(R.string.stats_rating_title), inReport.bestRating?.let { stringResource(R.string.stats_best_rating, formatRating(it)) })
		val vFrom = when (vPeriod) {
			0 -> inNow - 7 * kDayMs
			1 -> inNow - 30 * kDayMs
			else -> Long.MIN_VALUE
		}
		val vSamples = (listOfNotNull(inReport.ratingHistory.lastOrNull { it.time < vFrom }) + inReport.ratingHistory.filter { it.time >= vFrom })
			.ifEmpty { listOfNotNull(inReport.ratingHistory.lastOrNull()) }
		val vBand = if (vSamples.all { it.deviation != null }) vSamples.map { (it.rating - it.deviation!!).toFloat() to (it.rating + it.deviation).toFloat() } else null
		LineChart(vSamples.map { it.rating.toFloat() }, Modifier.fillMaxWidth().height(140.dp), vBand)
		if (vBand != null) {
			Text(stringResource(R.string.stats_rating_band), style = ChessType.kCaption, color = vColors.muted)
		}
		Spacer(Modifier.height(10.dp))
		SegmentedControl(listOf(stringResource(R.string.stats_tab_7), stringResource(R.string.stats_tab_30), stringResource(R.string.stats_tab_all)), vPeriod) { vPeriod = it }
	}
}

/**
 * Calendrier d'activité et séries.
 */
@Composable
private fun ActivityCard(inReport: StatsReport) {
	SurfaceCard {
		CardTitle(stringResource(R.string.stats_activity), stringResource(R.string.stats_activity_detail))
		CalendarHeatmap(inReport.calendar.map { it.count }, Modifier.fillMaxWidth().aspectRatio(kHeatmapRatio))
		Spacer(Modifier.height(8.dp))
		HeatmapLegend(stringResource(R.string.stats_less), stringResource(R.string.stats_more))
		Spacer(Modifier.height(12.dp))
		Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
			MiniStat(stringResource(R.string.stats_streak_current), pluralStringResource(R.plurals.stats_days, inReport.dayStreak, inReport.dayStreak), Modifier.weight(1f))
			MiniStat(stringResource(R.string.stats_streak_best), pluralStringResource(R.plurals.stats_days, inReport.bestDayStreak, inReport.bestDayStreak), Modifier.weight(1f))
			MiniStat(stringResource(R.string.stats_run_best), "${inReport.bestSuccessRun}", Modifier.weight(1f))
		}
	}
}

/**
 * Petite statistique : libellé et valeur.
 */
@Composable
private fun MiniStat(inLabel: String, inValue: String, inModifier: Modifier) {
	val vColors = kLocalChessColors.current
	Column(inModifier.background(vColors.background, RoundedCornerShape(14.dp)).padding(10.dp)) {
		Text(inLabel, style = ChessType.kCaption, color = vColors.muted, maxLines = 2)
		Text(inValue, style = ChessType.kBodyStrong, color = vColors.text, maxLines = 1)
	}
}

/**
 * Problèmes joués par semaine.
 */
@Composable
private fun VolumeCard(inReport: StatsReport) {
	val vMax = inReport.weeklyVolume.maxOfOrNull { it.attempts }?.coerceAtLeast(1) ?: 1
	val vFormat = SimpleDateFormat("d/M", Locale.FRANCE)
	SurfaceCard {
		CardTitle(stringResource(R.string.stats_volume))
		ColumnChart(
			inReport.weeklyVolume.map { ColumnItem(vFormat.format(Date(it.weekStart)), it.attempts.toFloat() / vMax, if (it.attempts > 0) "${it.attempts}" else null) },
			Modifier.height(130.dp),
		)
	}
}

/**
 * Forces et faiblesses : thèmes classés par réussite ou par cote, avec accès à leur fiche.
 */
@Composable
private fun ThemesCard(inReport: StatsReport, inOnTheme: (Theme) -> Unit) {
	var vSortByRating by rememberSaveable { mutableStateOf(false) }
	var vShowAll by rememberSaveable { mutableStateOf(false) }
	val vEligible = inReport.themes.filter { it.attempts >= kMinAttemptsPerTheme && themeFamily(it.theme) in StatsCalculator.kTrainableFamilies }
	if (vEligible.isEmpty()) {
		return
	}
	val vSorted = if (vSortByRating) vEligible.sortedByDescending { it.rating } else vEligible.sortedWith(compareByDescending<ThemeStat> { it.rate }.thenByDescending { it.attempts })
	val vMaxRating = vEligible.maxOf { it.rating }.toFloat()
	val vMinRating = vEligible.minOf { it.rating }.toFloat() - 100f
	SurfaceCard {
		CardTitle(stringResource(R.string.stats_themes_title), stringResource(R.string.stats_themes_detail))
		SegmentedControl(listOf(stringResource(R.string.stats_sort_rate), stringResource(R.string.stats_sort_rating)), if (vSortByRating) 1 else 0) { vSortByRating = it == 1 }
		Spacer(Modifier.height(8.dp))
		for (vStat in if (vShowAll) vSorted else vSorted.take(kThemesPreview)) {
			BarRow(
				inLabel = stringResource(themeLabel(vStat.theme)),
				inValue = if (vSortByRating) formatRating(vStat.rating) else percent(vStat.rate),
				inFraction = if (vSortByRating) (vStat.rating - vMinRating) / (vMaxRating - vMinRating).coerceAtLeast(1f) else vStat.rate,
				inDetail = if (vSortByRating) percent(vStat.rate) else stringResource(R.string.stats_theme_rating, formatRating(vStat.rating)),
				inOnClick = { inOnTheme(vStat.theme) },
			)
		}
		if (vSorted.size > kThemesPreview) {
			Text(
				if (vShowAll) stringResource(R.string.stats_show_less) else stringResource(R.string.stats_show_all, vSorted.size),
				style = ChessType.kBodyStrong,
				color = kLocalChessColors.current.primary,
				modifier = Modifier.padding(top = 6.dp).clickable { vShowAll = !vShowAll }.padding(vertical = 4.dp),
			)
		}
	}
}

/**
 * Les trois thèmes les plus faibles et un entraînement ciblé sur eux.
 */
@Composable
private fun WeakCard(inReport: StatsReport, inOnTheme: (Theme) -> Unit, inOnTrainThemes: (Set<Theme>) -> Unit) {
	val vColors = kLocalChessColors.current
	SurfaceCard {
		CardTitle(stringResource(R.string.stats_weak_title), stringResource(R.string.stats_weak_detail))
		if (inReport.weakest.isEmpty()) {
			Text(stringResource(R.string.stats_weak_empty), style = ChessType.kBody, color = vColors.muted)
		} else {
			for (vStat in inReport.weakest) {
				BarRow(stringResource(themeLabel(vStat.theme)), percent(vStat.rate), vStat.rate, pluralStringResource(R.plurals.stats_problems, vStat.attempts, vStat.attempts)) { inOnTheme(vStat.theme) }
			}
			Spacer(Modifier.height(10.dp))
			PrimaryButton(stringResource(R.string.stats_weak_train), { inOnTrainThemes(inReport.weakest.map { it.theme }.toSet()) }, inIcon = R.drawable.ic_bolt_fill1, inHeight = 52)
		}
	}
}

/**
 * Réussite par famille de thèmes.
 */
@Composable
private fun FamiliesCard(inReport: StatsReport) {
	SurfaceCard {
		CardTitle(stringResource(R.string.stats_families))
		for (vGroup in inReport.families.filter { it.attempts > 0 && it.key in kComparedFamilies }) {
			BarRow(stringResource(vGroup.key.label), percent(vGroup.rate), vGroup.rate, pluralStringResource(R.plurals.stats_problems, vGroup.attempts, vGroup.attempts))
		}
	}
}

/**
 * Réussite par tranche de difficulté des problèmes.
 */
@Composable
private fun DifficultyCard(inReport: StatsReport) {
	SurfaceCard {
		CardTitle(stringResource(R.string.stats_difficulty), stringResource(R.string.stats_difficulty_detail))
		ColumnChart(inReport.difficulty.filter { it.attempts >= kMinAttemptsPerBand }.map { ColumnItem("${it.key}", it.rate, percent(it.rate)) }, Modifier.height(140.dp))
	}
}

/**
 * Répartition des fins de problème : réussi, indice, erreur.
 */
@Composable
private fun ProfileCard(inReport: StatsReport) {
	val vColors = kLocalChessColors.current
	SurfaceCard {
		CardTitle(stringResource(R.string.stats_profile))
		StackedBar(
			listOf(
				StackSegment(stringResource(R.string.stats_profile_clean), inReport.profile.clean, vColors.chartGood),
				StackSegment(stringResource(R.string.stats_profile_hint), inReport.profile.hint, vColors.chartHint),
				StackSegment(stringResource(R.string.stats_profile_mistake), inReport.profile.mistake, vColors.chartBad),
			)
		)
	}
}

/**
 * Réussite selon la longueur de la solution.
 */
@Composable
private fun LengthCard(inReport: StatsReport) {
	SurfaceCard {
		CardTitle(stringResource(R.string.stats_length))
		for (vGroup in inReport.lengths.filter { it.attempts > 0 }) {
			BarRow(stringResource(themeLabel(vGroup.key.theme)), percent(vGroup.rate), vGroup.rate, pluralStringResource(R.plurals.stats_problems, vGroup.attempts, vGroup.attempts))
		}
	}
}

/**
 * Réussite selon le moment de la journée.
 */
@Composable
private fun DayPartsCard(inReport: StatsReport) {
	val vLabels = mapOf(
		DayPart.Morning to R.string.stats_daypart_morning,
		DayPart.Afternoon to R.string.stats_daypart_afternoon,
		DayPart.Evening to R.string.stats_daypart_evening,
		DayPart.Night to R.string.stats_daypart_night,
	)
	SurfaceCard {
		CardTitle(stringResource(R.string.stats_dayparts))
		for (vGroup in inReport.dayParts.filter { it.attempts > 0 }) {
			BarRow(stringResource(vLabels.getValue(vGroup.key)), percent(vGroup.rate), vGroup.rate, pluralStringResource(R.plurals.stats_problems, vGroup.attempts, vGroup.attempts))
		}
	}
}

/**
 * Temps de résolution : moyenne, thèmes extrêmes, médiane par semaine.
 */
@Composable
private fun TimeCard(inReport: StatsReport) {
	val vColors = kLocalChessColors.current
	SurfaceCard {
		CardTitle(stringResource(R.string.stats_time))
		val vAverage = inReport.averageDurationMs
		if (vAverage == null) {
			Text(stringResource(R.string.stats_time_empty), style = ChessType.kBody, color = vColors.muted)
			return@SurfaceCard
		}
		Text(stringResource(R.string.stats_time_average), style = ChessType.kCaption, color = vColors.muted)
		Text(duration(vAverage), style = ChessType.kScreenTitle, color = vColors.text)
		val vTimed = inReport.themes.filter { it.averageDurationMs != null && it.successes >= kMinAttemptsPerTheme && themeFamily(it.theme) in StatsCalculator.kTrainableFamilies }
		if (vTimed.size >= 2) {
			val vFastest = vTimed.minBy { it.averageDurationMs!! }
			val vSlowest = vTimed.maxBy { it.averageDurationMs!! }
			Spacer(Modifier.height(6.dp))
			Text(stringResource(R.string.stats_time_fastest, stringResource(themeLabel(vFastest.theme)), duration(vFastest.averageDurationMs!!)), style = ChessType.kBody, color = vColors.text)
			Text(stringResource(R.string.stats_time_slowest, stringResource(themeLabel(vSlowest.theme)), duration(vSlowest.averageDurationMs!!)), style = ChessType.kBody, color = vColors.text)
		}
		if (inReport.weeklyDurationMs.size >= 2) {
			Spacer(Modifier.height(12.dp))
			Text(stringResource(R.string.stats_time_weekly), style = ChessType.kCaption, color = vColors.muted)
			val vMax = inReport.weeklyDurationMs.maxOf { it.second }.toFloat()
			val vFormat = SimpleDateFormat("d/M", Locale.FRANCE)
			ColumnChart(inReport.weeklyDurationMs.takeLast(8).map { ColumnItem(vFormat.format(Date(it.first)), it.second / vMax, stringResource(R.string.duration_seconds, (it.second / 1000).toInt())) }, Modifier.height(120.dp))
		}
	}
}

/**
 * Coup où le problème échoue.
 */
@Composable
private fun ErrorsCard(inReport: StatsReport) {
	val vColors = kLocalChessColors.current
	val vTotal = inReport.failedAtMove.sum()
	SurfaceCard {
		CardTitle(stringResource(R.string.stats_errors), stringResource(R.string.stats_errors_detail))
		if (vTotal == 0) {
			Text(stringResource(R.string.stats_errors_empty), style = ChessType.kBody, color = vColors.muted)
		} else {
			val vLabels = listOf(R.string.stats_errors_move1, R.string.stats_errors_move2, R.string.stats_errors_move3)
			inReport.failedAtMove.forEachIndexed { inIndex, inCount ->
				val vRate = inCount.toFloat() / vTotal
				BarRow(stringResource(vLabels[inIndex]), percent(vRate), vRate, "$inCount")
			}
		}
	}
}

/**
 * Records : meilleure cote, problème le plus difficile réussi, séries.
 */
@Composable
private fun RecordsCard(inReport: StatsReport) {
	val vColors = kLocalChessColors.current
	SurfaceCard {
		CardTitle(stringResource(R.string.stats_records))
		Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
			MiniStat(stringResource(R.string.stats_record_rating), inReport.bestRating?.let { formatRating(it) } ?: "–", Modifier.weight(1f))
			MiniStat(stringResource(R.string.stats_record_days), pluralStringResource(R.plurals.stats_days, inReport.bestDayStreak, inReport.bestDayStreak), Modifier.weight(1f))
			MiniStat(stringResource(R.string.stats_record_run), "${inReport.bestSuccessRun}", Modifier.weight(1f))
		}
		inReport.bestSolved?.let { inBest ->
			Spacer(Modifier.height(10.dp))
			Column(Modifier.fillMaxWidth().background(vColors.background, RoundedCornerShape(14.dp)).padding(12.dp)) {
				Text(stringResource(R.string.stats_record_solved), style = ChessType.kCaption, color = vColors.muted)
				Text(formatRating(inBest.rating), style = ChessType.kSection, color = vColors.text)
				Text(
					inBest.themes.filter { themeFamily(it) in StatsCalculator.kTrainableFamilies }.take(3).map { stringResource(themeLabel(it)) }.joinToString(" · "),
					style = ChessType.kCaption,
					color = vColors.muted,
				)
			}
		}
	}
}

/**
 * Couverture : thèmes rencontrés et jamais rencontrés.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CoverageCard(inReport: StatsReport) {
	val vColors = kLocalChessColors.current
	val vTotal = Theme.entries.size
	SurfaceCard {
		CardTitle(stringResource(R.string.stats_coverage_title))
		BarRow(stringResource(R.string.stats_coverage), stringResource(R.string.stats_coverage_value, inReport.seenThemes, vTotal), inReport.seenThemes.toFloat() / vTotal)
		if (inReport.unseenThemes.isNotEmpty()) {
			Spacer(Modifier.height(8.dp))
			Text(stringResource(R.string.stats_coverage_unseen), style = ChessType.kCaption, color = vColors.muted)
			Spacer(Modifier.height(6.dp))
			FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
				for (vTheme in inReport.unseenThemes.take(kUnseenPreview)) {
					Text(
						stringResource(themeLabel(vTheme)),
						style = ChessType.kCaption,
						color = vColors.text,
						modifier = Modifier.background(vColors.background, CircleShape).padding(horizontal = 10.dp, vertical = 5.dp),
					)
				}
				if (inReport.unseenThemes.size > kUnseenPreview) {
					Text("+${inReport.unseenThemes.size - kUnseenPreview}", style = ChessType.kCaption, color = vColors.muted, modifier = Modifier.padding(5.dp))
				}
			}
		}
	}
}

/**
 * Formate un taux de 0 à 1 en pourcentage entier (« 68 % »).
 */
fun percent(inRate: Float): String = "${(inRate * 100).roundToInt()} %"

/**
 * Formate une durée (« 38 s », « 1 min 05 s »).
 */
@Composable
fun duration(inMs: Long): String {
	val vSeconds = (inMs / 1000).toInt()
	return if (vSeconds < 60) stringResource(R.string.duration_seconds, vSeconds) else stringResource(R.string.duration_minutes, vSeconds / 60, vSeconds % 60)
}

