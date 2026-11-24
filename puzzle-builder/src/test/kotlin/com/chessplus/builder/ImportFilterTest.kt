package com.chessplus.builder

import com.chessplus.core.puzzle.LichessCsvRow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Tests des seuils de qualité et du plafond par tranche.
 */
class ImportFilterTest {
	/**
	 * Crée une ligne factice avec les valeurs utiles au filtrage.
	 */
	private fun row(inRating: Int, inPopularity: Int, inRd: Int = 70, inPlays: Int = 1000): LichessCsvRow =
		LichessCsvRow("id", "fen", "e2e4 e7e5", inRating, inRd, inPopularity, inPlays, "")

	/** Vérifie l'ordre et les bornes des seuils (bornes incluses). */
	@Test
	fun qualityThresholds() {
		val vThresholds = QualityThresholds(maxRatingDeviation = 80, minPopularity = 90, minPlays = 250)
		assertNull(vThresholds.check(row(1500, 90, 80, 250)))
		assertEquals(RejectReason.RatingDeviation, vThresholds.check(row(1500, 10, 81, 1)))
		assertEquals(RejectReason.Popularity, vThresholds.check(row(1500, 89, 80, 1)))
		assertEquals(RejectReason.Plays, vThresholds.check(row(1500, 95, 80, 249)))
	}

	/** Vérifie les seuils plus permissifs des problèmes faciles (sous la limite, bornes incluses). */
	@Test
	fun lowRatingThresholds() {
		val vThresholds = QualityThresholds(80, 90, 250, lowRatingLimit = 600, lowMaxRatingDeviation = 100, lowMinPopularity = 85, lowMinPlays = 100)
		assertNull(vThresholds.check(row(450, 85, 100, 100)))
		assertEquals(RejectReason.RatingDeviation, vThresholds.check(row(599, 95, 101, 1000)))
		assertEquals(RejectReason.Popularity, vThresholds.check(row(500, 84, 90, 1000)))
		assertEquals(RejectReason.Plays, vThresholds.check(row(500, 90, 90, 99)))
		assertEquals(RejectReason.RatingDeviation, vThresholds.check(row(600, 90, 90, 1000)), "à partir de la limite, les seuils généraux")
	}

	/** Vérifie les tranches de 200 points. */
	@Test
	fun ratingBands() {
		assertEquals(0, RatingBands.bandOf(199))
		assertEquals(7, RatingBands.bandOf(1400))
		assertEquals(7, RatingBands.bandOf(1599))
		assertEquals("1400-1599", RatingBands.label(7))
	}

	/** Vérifie que le plafond garde les plus populaires, puis les premières lignes à égalité. */
	@Test
	fun bandCapKeepsMostPopular() {
		val vRows = listOf(
			row(1500, 92), row(1500, 95), row(1500, 92), row(1500, 99), row(1500, 92), row(1500, 90),
			// Autre tranche, sous le plafond : tout est gardé.
			row(2500, 90), row(2500, 91),
		)
		val vPlanner = BandCapPlanner(maxPerBand = 4)
		vRows.forEach { vPlanner.count(it) }
		val vCaps = vPlanner.plan()
		val vAccepted = vRows.map { vCaps.tryAccept(it) }
		// Tranche 1400-1599 : 99 et 95 gardés, puis deux des trois à 92 (les premiers), 90 rejeté.
		assertEquals(listOf(true, true, true, true, false, false, true, true), vAccepted)
	}

	/** Vérifie l'absence de plafond. */
	@Test
	fun noCap() {
		assertEquals(true, BandCaps.kNone.tryAccept(row(1500, -100)))
	}
}
