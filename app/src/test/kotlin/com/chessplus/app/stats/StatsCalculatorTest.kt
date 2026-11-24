package com.chessplus.app.stats

import com.chessplus.app.ui.ThemeFamily
import com.chessplus.core.model.Theme
import com.chessplus.core.model.ThemeSet
import com.chessplus.core.puzzle.PuzzleResult
import com.chessplus.data.user.AttemptEntity
import java.util.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests du calcul des statistiques (fuseau UTC pour des jours et heures déterministes).
 */
class StatsCalculatorTest {
	/** Calculateur testé. */
	private val calculator = StatsCalculator(TimeZone.getTimeZone("UTC"))

	/** Lundi 16 novembre 2026, minuit UTC. */
	private val kMonday = 1794787200000L

	/** Une heure en millisecondes. */
	private val kHour = 60L * 60 * 1000

	/** Un jour en millisecondes. */
	private val kDay = 24 * kHour

	/**
	 * Crée une tentative notée.
	 */
	private fun attempt(
		inTime: Long,
		inSuccess: Boolean,
		inThemes: ThemeSet,
		inPuzzleRating: Int = 1500,
		inBefore: Double = 1500.0,
		inAfter: Double = 1500.0,
		inMistakes: Int = if (inSuccess) 0 else 1,
		inDuration: Long? = null,
		inFailedAt: Int? = null,
		inId: String = "p$inTime",
	) = AttemptEntity(
		puzzleId = inId, playedAt = inTime, result = if (inSuccess) PuzzleResult.Success else PuzzleResult.Failure, rated = true,
		puzzleRating = inPuzzleRating, themesLow = inThemes.low, themesHigh = inThemes.high, mistakeCount = inMistakes,
		revealedCount = if (inSuccess || inMistakes > 0) 0 else 1, playerRatingBefore = inBefore, playerRatingAfter = inAfter,
		durationMs = inDuration, failedAtMove = inFailedAt, playerDeviationAfter = 120.0,
	)

	/** Vérifie le rapport vide. */
	@Test
	fun emptyReport() {
		val vReport = calculator.report(emptyList(), kMonday)
		assertEquals(0, vReport.attempts)
		assertNull(vReport.successRate)
		assertNull(vReport.currentRating)
		assertEquals(0, vReport.dayStreak)
		assertEquals(Theme.entries.size, vReport.unseenThemes.size)
		assertEquals(12 * 7 - 6, vReport.calendar.size, "12 semaines, du lundi le plus ancien à aujourd’hui (un lundi)")
	}

	/** Vérifie les totaux, la cote, les séries et les records. */
	@Test
	fun totalsStreaksAndRecords() {
		val vFork = ThemeSet.of(Theme.Fork, Theme.Short, Theme.Middlegame)
		val vAttempts = listOf(
			attempt(kMonday + 9 * kHour, true, vFork, 1400, 1500.0, 1510.0),
			attempt(kMonday + 10 * kHour, true, vFork, 1900, 1510.0, 1530.0),
			attempt(kMonday + kDay + 20 * kHour, false, vFork, 1500, 1530.0, 1520.0),
			attempt(kMonday + 2 * kDay + 8 * kHour, true, vFork, 1600, 1520.0, 1535.0),
		)
		val vReport = calculator.report(vAttempts, kMonday + 2 * kDay + 12 * kHour)
		assertEquals(4, vReport.attempts)
		assertEquals(3, vReport.successes)
		assertEquals(0.75f, vReport.successRate)
		assertEquals(1535, vReport.currentRating)
		assertEquals(1535, vReport.bestRating)
		assertEquals(35, vReport.ratingDelta30Days)
		assertEquals(3, vReport.dayStreak)
		assertEquals(3, vReport.bestDayStreak)
		assertEquals(2, vReport.bestSuccessRun)
		assertEquals(1900, vReport.bestSolved?.rating)
		assertEquals(ResolutionProfile(clean = 3, hint = 0, mistake = 1), vReport.profile)
		assertEquals(2, vReport.calendar.last { it.count > 0 && it.dayStart == kMonday }.count)
	}

	/** Vérifie les bilans par thème, famille, longueur, difficulté et moment de la journée. */
	@Test
	fun groupings() {
		val vMate = ThemeSet.of(Theme.Mate, Theme.MateIn2, Theme.Short)
		val vRook = ThemeSet.of(Theme.RookEndgame, Theme.Endgame, Theme.Long)
		val vAttempts = (0 until 5).map { attempt(kMonday + it * kHour, true, vMate, 1250) } +
			(0 until 5).map { attempt(kMonday + 20 * kHour + it, it == 0, vRook, 1650) }
		val vReport = calculator.report(vAttempts, kMonday + kDay)
		val vMateStat = vReport.themes.first { it.theme == Theme.MateIn2 }
		assertEquals(5, vMateStat.attempts)
		assertEquals(1f, vMateStat.rate)
		assertTrue(vMateStat.rating > 1500)
		val vRookStat = vReport.themes.first { it.theme == Theme.RookEndgame }
		assertEquals(0.2f, vRookStat.rate)
		assertTrue(vRookStat.rating < vMateStat.rating)
		assertEquals(Theme.RookEndgame, vReport.weakest.first().theme)
		assertEquals(5, vReport.families.first { it.key == ThemeFamily.Mates }.attempts)
		assertEquals(5, vReport.lengths.first { it.key == PuzzleLength.Long }.attempts)
		assertEquals(listOf(1200, 1600), vReport.difficulty.map { it.key })
		assertEquals(5, vReport.dayParts.first { it.key == DayPart.Night }.attempts)
		assertEquals(5, vReport.dayParts.first { it.key == DayPart.Evening }.attempts)
	}

	/** Vérifie la durée moyenne, le coup de l'échec et la fiche d'un thème. */
	@Test
	fun durationFailuresAndThemeDetail() {
		val vPin = ThemeSet.of(Theme.Pin)
		val vAttempts = listOf(
			attempt(kMonday, true, vPin, inDuration = 10_000),
			attempt(kMonday + kHour, true, vPin, inDuration = 30_000),
			attempt(kMonday + 2 * kHour, false, vPin, inFailedAt = 1),
			attempt(kMonday + 3 * kHour, false, vPin, inFailedAt = 4),
		)
		val vReport = calculator.report(vAttempts, kMonday + kDay)
		assertEquals(20_000L, vReport.averageDurationMs)
		assertEquals(listOf(1, 0, 1), vReport.failedAtMove)
		val vDetail = assertNotNull(calculator.themeDetail(vAttempts, Theme.Pin))
		assertEquals(4, vDetail.stat.attempts)
		assertEquals(4, vDetail.ratingHistory.size)
		assertEquals(listOf(1f, 1f, 2f / 3, 0.5f), vDetail.rollingRate)
		assertEquals(kMonday + 3 * kHour, vDetail.recent.first().playedAt)
		assertNull(calculator.themeDetail(vAttempts, Theme.Fork))
	}

	/** Vérifie le début de semaine (lundi) et de jour. */
	@Test
	fun weeksAndDays() {
		assertEquals(kMonday, calculator.weekStart(kMonday + 3 * kDay + 5 * kHour))
		assertEquals(kMonday + kDay, calculator.dayStart(kMonday + kDay + 23 * kHour))
	}
}
