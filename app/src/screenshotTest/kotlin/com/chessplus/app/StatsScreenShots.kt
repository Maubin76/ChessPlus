package com.chessplus.app

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest

import com.chessplus.app.stats.StatsCalculator
import com.chessplus.app.stats.StatsContent
import com.chessplus.app.stats.StatsData
import com.chessplus.app.stats.ThemeStatsContent
import com.chessplus.app.stats.ThemeStatsState
import com.chessplus.app.ui.theme.ChessPlusTheme
import com.chessplus.core.model.Theme
import com.chessplus.core.model.ThemeSet
import com.chessplus.core.puzzle.PuzzleResult
import com.chessplus.core.rating.Glicko2RatingSystem
import com.chessplus.data.user.AttemptEntity
import java.util.TimeZone
import kotlin.math.exp
import kotlin.random.Random

/*
 * Captures des statistiques avec un historique factice de quatre mois, calculé par le vrai
 * StatsCalculator. Les variantes « longues » montrent l'écran entier, toutes sections déroulées.
 */

/** Date de référence des captures (20 novembre 2026, 12 h). */
private const val kNow = 1_795_172_400_000L

/** Durée d'un jour en millisecondes. */
private const val kDayMs = 24L * 60 * 60 * 1000

/** Nombre de jours d'historique factice. */
private const val kHistoryDays = 120

/** Appareil haut pour capturer tout l'écran Statistiques. */
private const val kTallDevice = "spec:width=384dp,height=4350dp,dpi=450"

/** Appareil haut pour capturer toute la fiche d'un thème. */
private const val kThemeDevice = "spec:width=384dp,height=1500dp,dpi=450"

/** Thèmes factices et aisance du joueur sur chacun (écart de cote ressenti). */
private val kFakeThemes = listOf(
	Theme.Fork to 120, Theme.MateIn2 to 90, Theme.Pin to 30, Theme.Skewer to -10, Theme.DiscoveredAttack to 10,
	Theme.HangingPiece to 160, Theme.BackRankMate to 70, Theme.Deflection to -90, Theme.Sacrifice to -60,
	Theme.RookEndgame to -140, Theme.PawnEndgame to -40, Theme.MateIn1 to 200, Theme.Zugzwang to -180, Theme.Attraction to -70,
)

/** Thèmes de longueur, du plus court au plus long, avec la pénalité associée. */
private val kFakeLengths = listOf(Theme.OneMove to 150, Theme.Short to 40, Theme.Long to -60, Theme.VeryLong to -160)

/**
 * Génère un historique factice reproductible : séances irrégulières puis série des neuf derniers
 * jours, cote rejouée avec Glicko-2.
 */
private fun fakeAttempts(): List<AttemptEntity> {
	val vRandom = Random(42)
	val vRating = Glicko2RatingSystem()
	var vPlayer = vRating.initialRating
	var vSkill = 1450.0
	val vResult = mutableListOf<AttemptEntity>()
	for (vDay in kHistoryDays downTo 0) {
		val vPlays = vDay < 9 || vRandom.nextFloat() < 0.55f
		if (!vPlays) {
			continue
		}
		val vCount = 2 + vRandom.nextInt(if (vRandom.nextFloat() < 0.2f) 14 else 6)
		val vHour = listOf(8, 13, 19, 21, 22, 23)[vRandom.nextInt(6)]
		vSkill += 1.6
		for (vIndex in 0 until vCount) {
			val vTheme = kFakeThemes[vRandom.nextInt(kFakeThemes.size)]
			val vSecond = kFakeThemes[vRandom.nextInt(kFakeThemes.size)].first
			val vLength = kFakeLengths[vRandom.nextInt(kFakeLengths.size)]
			val vPuzzleRating = (vPlayer.rating + vRandom.nextInt(-250, 300)).toInt()
			val vEdge = vSkill + vTheme.second + vLength.second - vPuzzleRating
			val vSuccess = vRandom.nextDouble() < 1.0 / (1.0 + exp(-vEdge / 170.0))
			val vMistake = !vSuccess && vRandom.nextFloat() < 0.7f
			val vOutcome = if (vSuccess) PuzzleResult.Success else PuzzleResult.Failure
			val vAfter = vRating.update(vPlayer, vPuzzleRating, vOutcome)
			val vThemes = ThemeSet.of(vTheme.first, vSecond, vLength.first)
			vResult.add(
				AttemptEntity(
					puzzleId = "f$vDay-$vIndex",
					playedAt = kNow - vDay * kDayMs - (12 - vHour) * 3_600_000L + vIndex * 70_000L,
					result = vOutcome,
					rated = true,
					puzzleRating = vPuzzleRating,
					themesLow = vThemes.low,
					themesHigh = vThemes.high,
					mistakeCount = if (vMistake) 1 else 0,
					revealedCount = if (vSuccess) 0 else 1,
					playerRatingBefore = vPlayer.rating,
					playerRatingAfter = vAfter.rating,
					durationMs = if (vDay < 60) 12_000L + vRandom.nextLong(70_000L) - vTheme.second * 60L else null,
					failedAtMove = if (vSuccess || vDay >= 60) null else 1 + minOf(vRandom.nextInt(4), vRandom.nextInt(4)),
					playerDeviationAfter = vAfter.deviation,
				)
			)
			vPlayer = vAfter
		}
	}
	return vResult
}

/** Calcul des captures, en UTC pour des images stables. */
private val kCalculator = StatsCalculator(TimeZone.getTimeZone("UTC"))

/** Historique factice partagé par les captures. */
private val kAttempts by lazy { fakeAttempts() }

/** Statistiques, haut de l'écran. */
@PreviewTest
@DesignPreviews
@Composable
fun StatsTopShot() = ChessPlusTheme {
	StatsContent(StatsData(kCalculator.report(kAttempts, kNow), rushBest = 28, survivalBest = 41), kNow, {}, {}, {}, {})
}

/** Statistiques, écran entier (clair). */
@PreviewTest
@Preview(name = "ClairLong", device = kTallDevice, showSystemUi = false, uiMode = Configuration.UI_MODE_NIGHT_NO or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
fun StatsFullLightShot() = ChessPlusTheme(inDarkTheme = false) {
	StatsContent(StatsData(kCalculator.report(kAttempts, kNow), rushBest = 28, survivalBest = 41), kNow, {}, {}, {}, {})
}

/** Statistiques, écran entier (sombre). */
@PreviewTest
@Preview(name = "SombreLong", device = kTallDevice, showSystemUi = false, uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
fun StatsFullDarkShot() = ChessPlusTheme(inDarkTheme = true) {
	StatsContent(StatsData(kCalculator.report(kAttempts, kNow), rushBest = 28, survivalBest = 41), kNow, {}, {}, {}, {})
}

/** Statistiques sans aucune tentative. */
@PreviewTest
@DesignPreviews
@Composable
fun StatsEmptyShot() = ChessPlusTheme {
	StatsContent(StatsData(kCalculator.report(emptyList(), kNow)), kNow, {}, {}, {}, {})
}

/** Fiche du thème « Finale de tours », entière. */
@PreviewTest
@Preview(name = "ClairLong", device = kThemeDevice, showSystemUi = false, uiMode = Configuration.UI_MODE_NIGHT_NO or Configuration.UI_MODE_TYPE_NORMAL)
@Preview(name = "SombreLong", device = kThemeDevice, showSystemUi = false, uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
fun ThemeStatsShot() = ChessPlusTheme {
	ThemeStatsContent(Theme.RookEndgame, ThemeStatsState(kCalculator.themeDetail(kAttempts, Theme.RookEndgame)), kNow, {}, {}, {})
}
