package com.chessplus.core.rating

import com.chessplus.core.puzzle.PuzzleResult
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests des calculs de cote Glicko-2 et Elo.
 */
class RatingTest {
	/**
	 * Vérifie qu'une valeur est proche de l'attendu.
	 */
	private fun assertClose(inExpected: Double, inActual: Double, inTolerance: Double) {
		assertTrue(abs(inExpected - inActual) <= inTolerance, "Attendu $inExpected, obtenu $inActual")
	}

	/** Reproduit l'exemple de l'article de Glickman (tau = 0,5). */
	@Test
	fun glicko2PaperExample() {
		val vSystem = Glicko2RatingSystem(tau = 0.5)
		val vPlayer = PlayerRating(1500.0, 200.0, 0.06)
		val vGames = listOf(
			Glicko2Game(PlayerRating(1400.0, 30.0, 0.06), 1.0),
			Glicko2Game(PlayerRating(1550.0, 100.0, 0.06), 0.0),
			Glicko2Game(PlayerRating(1700.0, 300.0, 0.06), 0.0),
		)
		val vResult = vSystem.rate(vPlayer, vGames)
		assertClose(1464.06, vResult.rating, 0.01)
		assertClose(151.52, vResult.deviation, 0.01)
		assertClose(0.05999, vResult.volatility, 0.00001)
	}

	/** Vérifie qu'une période sans partie n'augmente que l'écart de cote. */
	@Test
	fun glicko2InactivityIncreasesDeviation() {
		val vSystem = Glicko2RatingSystem()
		val vPlayer = PlayerRating(1600.0, 100.0, 0.06)
		val vResult = vSystem.rate(vPlayer, emptyList())
		assertEquals(1600.0, vResult.rating)
		assertTrue(vResult.deviation > 100.0)
	}

	/** Vérifie le sens des mises à jour après un problème et les bornes de l'écart de cote. */
	@Test
	fun glicko2PuzzleUpdates() {
		val vSystem = Glicko2RatingSystem()
		val vStart = vSystem.initialRating
		val vWin = vSystem.update(vStart, 1500, PuzzleResult.Success)
		val vLoss = vSystem.update(vStart, 1500, PuzzleResult.Failure)
		assertTrue(vWin.rating > vStart.rating)
		assertTrue(vLoss.rating < vStart.rating)
		assertTrue(vWin.deviation < vStart.deviation)
		// Battre un problème bien plus facile rapporte moins que battre un problème équivalent.
		val vEasyWin = vSystem.update(vStart, 900, PuzzleResult.Success)
		assertTrue(vEasyWin.rating - vStart.rating < vWin.rating - vStart.rating)
		// Après de nombreux problèmes, l'écart de cote se stabilise (vers 60 avec une volatilité
		// de 0,06) sans descendre sous le minimum.
		var vPlayer = vStart
		repeat(500) { inIndex ->
			vPlayer = vSystem.update(vPlayer, 1500, if (inIndex % 2 == 0) PuzzleResult.Success else PuzzleResult.Failure)
		}
		assertTrue(vPlayer.deviation >= Glicko2RatingSystem.kDefaultMinDeviation)
		assertTrue(vPlayer.deviation < 80.0)
		assertClose(1500.0, vPlayer.rating, 50.0)
		// Le plancher s'applique quand il est plus haut que l'équilibre naturel.
		val vFloored = Glicko2RatingSystem(minDeviation = 100.0).update(PlayerRating(1500.0, 100.0, 0.06), 1500, PuzzleResult.Success)
		assertEquals(100.0, vFloored.deviation)
	}

	/** Vérifie la formule Elo sur des cas simples. */
	@Test
	fun eloUpdates() {
		val vSystem = EloRatingSystem()
		val vStart = vSystem.initialRating
		assertClose(1516.0, vSystem.update(vStart, 1500, PuzzleResult.Success).rating, 1e-9)
		assertClose(1484.0, vSystem.update(vStart, 1500, PuzzleResult.Failure).rating, 1e-9)
		assertClose(0.0909, vSystem.expectedScore(1500.0, 1900.0), 0.0001)
		assertTrue(vSystem.update(vStart, 1900, PuzzleResult.Success).rating > 1529.0)
	}
}
