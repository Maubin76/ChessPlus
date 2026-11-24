package com.chessplus.core.rules

import com.chessplus.core.model.Color
import com.chessplus.core.model.Move
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests de la notation algébrique abrégée.
 */
class NotationTest {
	/** Règles des échecs. */
	private val rules = defaultChessRules()

	/**
	 * Retourne la notation du coup UCI [inUci] dans la position [inFen], en vérifiant que la position n'a pas changé.
	 */
	private fun san(inFen: String, inUci: String): String {
		val vPosition = rules.positionFromFen(inFen)
		val vSan = vPosition.san(Move.fromUci(inUci))
		assertEquals(inFen, vPosition.fen(), "la position ne doit pas être modifiée")
		return vSan
	}

	/** Vérifie les coups de pion et de pièce simples. */
	@Test
	fun simpleMoves() {
		val vStart = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
		assertEquals("e4", san(vStart, "e2e4"))
		assertEquals("Nf3", san(vStart, "g1f3"))
		assertEquals("exd5", san("rnbqkbnr/ppp1pppp/8/3p4/4P3/8/PPPP1PPP/RNBQKBNR w KQkq d6 0 2", "e4d5"))
	}

	/** Vérifie la désambiguïsation par colonne, par rangée et par case. */
	@Test
	fun disambiguation() {
		assertEquals("Nbd2", san("4k3/8/8/8/8/8/8/1N2KN2 w - - 0 1", "b1d2"))
		assertEquals("R1a3", san("4k3/8/8/R7/8/8/8/R3K3 w - - 0 1", "a1a3"))
		assertEquals("Qa1b2", san("4k3/8/8/8/8/Q7/8/Q1Q1K3 w - - 0 1", "a1b2"))
	}

	/** Vérifie le roque des deux côtés. */
	@Test
	fun castling() {
		val vFen = "r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1"
		assertEquals("O-O", san(vFen, "e1g1"))
		assertEquals("O-O-O", san(vFen, "e1c1"))
	}

	/** Vérifie la promotion avec et sans prise, et la prise en passant. */
	@Test
	fun promotionAndEnPassant() {
		assertEquals("a8=Q", san("8/P7/8/8/8/7k/8/2K5 w - - 0 1", "a7a8q"))
		assertEquals("bxa8=N", san("r7/1P6/8/8/8/7k/8/2K5 w - - 0 1", "b7a8n"))
		assertEquals("dxe6", san("4k3/8/8/3Pp3/8/8/8/4K3 w - e6 0 2", "d5e6"))
	}

	/** Vérifie les suffixes d'échec et de mat. */
	@Test
	fun checkAndMate() {
		assertEquals("Ra8+", san("4k3/8/8/8/8/8/8/R3K3 w - - 0 1", "a1a8"))
		assertEquals("Rd8#", san("6k1/5ppp/8/8/8/8/5PPP/3R2K1 w - - 0 1", "d1d8"))
	}

	/** Vérifie la numérotation d'une suite qui commence par un coup noir. */
	@Test
	fun numberedLine() {
		val vLine = sanLine(rules, "r1bq1rk1/ppp2ppp/2np1n2/2b1p1B1/2B1P3/2NP1N2/PPP2PPP/R2Q1RK1 b - - 0 8", Move.listFromUci("c8g4 h2h3"))
		assertEquals(listOf(NumberedMove(8, Color.Black, "Bg4"), NumberedMove(9, Color.White, "h3")), vLine)
		assertEquals("8…", formatMoveNumber(vLine[0]))
		assertEquals("9.", formatMoveNumber(vLine[1]))
	}
}
