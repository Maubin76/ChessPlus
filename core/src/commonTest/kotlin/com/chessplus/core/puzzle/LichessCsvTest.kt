package com.chessplus.core.puzzle

import com.chessplus.core.SampleData
import com.chessplus.core.model.Color
import com.chessplus.core.model.Move
import com.chessplus.core.model.Theme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Tests du découpage du CSV Lichess et de la conversion en problème.
 */
class LichessCsvTest {
	/** Vérifie que l'en-tête attendu est celui du fichier réel. */
	@Test
	fun headerMatches() {
		assertEquals(SampleData.kHeader, LichessCsv.kHeader)
	}

	/** Vérifie le décodage complet du premier problème. */
	@Test
	fun parseFirstPuzzle() {
		val vRow = LichessCsv.parseLine(SampleData.kLine00sHx)
		assertEquals("00sHx", vRow.id)
		assertEquals("q3k1nr/1pp1nQpp/3p4/1P2p3/4P3/B1PP1b2/B5PP/5K2 b k - 0 17", vRow.fen)
		assertEquals("e8d7 a2e6 d7d8 f7f8", vRow.moves)
		assertEquals(1760, vRow.rating)
		assertEquals(80, vRow.ratingDeviation)
		assertEquals(83, vRow.popularity)
		assertEquals(72, vRow.nbPlays)
		val vPuzzle = vRow.toPuzzle()
		assertEquals(Move.listFromUci("e8d7 a2e6 d7d8 f7f8"), vPuzzle.moves)
		assertEquals(Color.Black, vPuzzle.opponentColor)
		assertEquals(Color.White, vPuzzle.playerColor)
		assertEquals(2, vPuzzle.playerMoveCount)
		assertTrue(Theme.MateIn2 in vPuzzle.themes)
		assertEquals("https://lichess.org/training/00sHx", vPuzzle.lichessUrl)
	}

	/** Vérifie les quatre lignes, dont celles qui finissent par des champs vides. */
	@Test
	fun parseAllSampleLines() {
		val vPuzzles = SampleData.kLines.map { LichessCsv.parseLine(it).toPuzzle() }
		assertEquals(listOf("00sHx", "00sJ9", "00sJb", "00sO1"), vPuzzles.map { it.id })
		assertEquals(listOf(1760, 2671, 2235, 998), vPuzzles.map { it.rating })
		assertEquals(listOf(4, 8, 6, 4), vPuzzles.map { it.moves.size })
		assertEquals(listOf(Color.White, Color.Black, Color.Black, Color.White), vPuzzles.map { it.playerColor })
		assertTrue(Theme.Master in vPuzzles[3].themes)
		assertTrue(Theme.VeryLong in vPuzzles[1].themes)
	}

	/** Vérifie que le nombre de champs est contrôlé strictement. */
	@Test
	fun wrongFieldCountFails() {
		// Une virgule finale en moins : 10 champs.
		assertFailsWith<CsvFormatException> { LichessCsv.parseLine(SampleData.kLine00sO1.dropLast(1)) }
		// Une virgule en trop : 12 champs.
		assertFailsWith<CsvFormatException> { LichessCsv.parseLine(SampleData.kLine00sO1 + ",") }
	}

	/** Vérifie qu'un nombre impair de coups est refusé. */
	@Test
	fun oddMoveCountFails() {
		val vLine = SampleData.kLine00sHx.replace("e8d7 a2e6 d7d8 f7f8", "e8d7 a2e6 d7d8")
		assertFailsWith<CsvFormatException> { LichessCsv.parseLine(vLine) }
		assertFailsWith<IllegalArgumentException> { Puzzle("x", "8/8/8/8/8/8/8/8 w - - 0 1", Move.listFromUci("e2e4"), 1500, com.chessplus.core.model.ThemeSet.kEmpty) }
	}

	/** Vérifie qu'un nombre invalide est refusé avec le nom de la colonne. */
	@Test
	fun invalidNumberFails() {
		val vLine = SampleData.kLine00sHx.replace(",1760,", ",abc,")
		val vError = assertFailsWith<CsvFormatException> { LichessCsv.parseLine(vLine) }
		assertTrue(vError.message!!.contains("Rating"))
	}

	/** Vérifie qu'un coup ou un thème invalide est refusé lors de la conversion. */
	@Test
	fun invalidMoveOrThemeFails() {
		val vBadMove = SampleData.kLine00sHx.replace("e8d7 a2e6", "e8d7 a2e9")
		assertFailsWith<IllegalArgumentException> { LichessCsv.parseLine(vBadMove).toPuzzle() }
		val vBadTheme = SampleData.kLine00sHx.replace("mate mateIn2", "mate mateIn9")
		assertFailsWith<com.chessplus.core.model.UnknownThemeException> { LichessCsv.parseLine(vBadTheme).toPuzzle() }
	}
}
