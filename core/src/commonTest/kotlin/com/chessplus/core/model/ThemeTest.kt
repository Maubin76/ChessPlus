package com.chessplus.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests des thèmes et de leur masque de bits.
 */
class ThemeTest {
	/** Thèmes recensés dans le CSV Lichess du 2026-11-20 (73 thèmes). */
	private val kCsvThemes = """
		advancedPawn advantage anastasiaMate arabianMate attackingF2F7 attraction backRankMate balestraMate
		bishopEndgame blindSwineMate bodenMate capturingDefender castling clearance collinearMove cornerMate
		crushing defensiveMove deflection discoveredAttack discoveredCheck doubleBishopMate doubleCheck
		dovetailMate enPassant endgame epauletteMate equality exposedKing fork hangingPiece hookMate
		interference intermezzo killBoxMate kingsideAttack knightEndgame long master masterVsMaster mate
		mateIn1 mateIn2 mateIn3 mateIn4 mateIn5 middlegame morphysMate oneMove opening operaMate pawnEndgame
		pillsburysMate pin promotion queenEndgame queenRookEndgame queensideAttack quietMove rookEndgame
		sacrifice short skewer smotheredMate superGM swallowstailMate trappedPiece triangleMate
		underPromotion veryLong vukovicMate xRayAttack zugzwang
	""".trimIndent().split(Regex("\\s+")).toSet()

	/** Vérifie que l'énumération couvre exactement les thèmes du CSV. */
	@Test
	fun enumMatchesCsvThemes() {
		assertEquals(73, kCsvThemes.size)
		assertEquals(kCsvThemes, Theme.entries.map { it.key }.toSet())
	}

	/** Vérifie que les bits sont uniques et dans les limites du masque. */
	@Test
	fun bitsAreUniqueAndInRange() {
		val vBits = Theme.entries.map { it.bit }
		assertEquals(vBits.size, vBits.toSet().size)
		assertTrue(vBits.all { it in 0 until Theme.kMaxBits })
		assertEquals(Theme.entries.size, Theme.entries.map { it.key }.toSet().size)
	}

	/** Vérifie la lecture du champ Themes et l'appartenance. */
	@Test
	fun parseThemes() {
		val vSet = ThemeSet.parse("mate mateIn2 middlegame short")
		assertTrue(Theme.Mate in vSet)
		assertTrue(Theme.MateIn2 in vSet)
		assertTrue(Theme.Middlegame in vSet)
		assertTrue(Theme.Short in vSet)
		assertFalse(Theme.Fork in vSet)
		assertEquals(4, vSet.count())
		assertEquals("mate mateIn2 middlegame short", vSet.toKeys())
		assertTrue(ThemeSet.parse("").isEmpty)
	}

	/** Vérifie le passage par les deux mots du masque, bit de signe compris. */
	@Test
	fun lowAndHighWords() {
		val vSet = ThemeSet.of(Theme.AdvancedPawn, Theme.SmotheredMate, Theme.SuperGM, Theme.Zugzwang)
		assertEquals(1L or (1L shl 63), vSet.low)
		assertEquals(1L or (1L shl 8), vSet.high)
		val vCopy = ThemeSet(vSet.low, vSet.high)
		assertEquals(vSet, vCopy)
		assertEquals(listOf(Theme.AdvancedPawn, Theme.SmotheredMate, Theme.SuperGM, Theme.Zugzwang), vCopy.toList())
		assertTrue(vSet.intersects(ThemeSet.of(Theme.Zugzwang)))
		assertFalse(vSet.intersects(ThemeSet.of(Theme.Fork)))
	}

	/** Vérifie l'aller-retour de chaque thème seul. */
	@Test
	fun everyThemeRoundTrip() {
		for (vTheme in Theme.entries) {
			val vSet = ThemeSet.of(vTheme)
			assertEquals(listOf(vTheme), ThemeSet(vSet.low, vSet.high).toList())
			assertEquals(vSet, ThemeSet.parse(vTheme.key))
		}
		val vAll = ThemeSet.of(Theme.entries)
		assertEquals(Theme.entries, ThemeSet(vAll.low, vAll.high).toList())
	}

	/** Vérifie qu'un thème inconnu fait échouer la lecture au lieu d'être ignoré. */
	@Test
	fun unknownThemeFails() {
		val vError = assertFailsWith<UnknownThemeException> { ThemeSet.parse("mate brandNewTheme") }
		assertEquals("brandNewTheme", vError.key)
	}
}
