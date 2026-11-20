package com.chessplus.core.rules

import com.chessplus.core.SampleData
import com.chessplus.core.model.Color
import com.chessplus.core.model.Move
import com.chessplus.core.model.Piece
import com.chessplus.core.model.PieceType
import com.chessplus.core.model.Square
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests des règles des échecs, écrits contre l'interface [ChessRules] pour rester valables
 * avec une autre implémentation.
 */
class ChessRulesTest {
	/** Règles testées. */
	private val rules: ChessRules = defaultChessRules()

	/** FEN de la position initiale. */
	private val kStartFen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"

	/**
	 * Raccourci : case à partir de son nom algébrique.
	 */
	private fun sq(inName: String): Square = Square.fromAlgebraic(inName)

	/**
	 * Raccourci : joue une suite de coups UCI.
	 */
	private fun ChessPosition.playAll(inUciList: String) {
		Move.listFromUci(inUciList).forEach { play(it) }
	}

	/** Vérifie la position initiale : pièces, trait, coups légaux et export FEN. */
	@Test
	fun startingPosition() {
		val vPosition = rules.positionFromFen(kStartFen)
		assertEquals(kStartFen, vPosition.fen())
		assertEquals(Color.White, vPosition.sideToMove)
		assertEquals(Piece(Color.White, PieceType.King), vPosition.pieceAt(sq("e1")))
		assertEquals(Piece(Color.Black, PieceType.Queen), vPosition.pieceAt(sq("d8")))
		assertNull(vPosition.pieceAt(sq("e4")))
		assertEquals(20, vPosition.legalMoves().size)
		assertEquals(setOf("g1f3", "g1h3"), vPosition.legalMovesFrom(sq("g1")).map { it.toUci() }.toSet())
		assertTrue(vPosition.legalMovesFrom(sq("e1")).isEmpty())
		assertTrue(vPosition.legalMovesFrom(sq("e4")).isEmpty())
		assertEquals(GameStatus.Ongoing, vPosition.status())
	}

	/** Vérifie l'export FEN des positions des problèmes de l'extrait. */
	@Test
	fun samplePuzzleFensRoundTrip() {
		for (vPuzzle in SampleData.kPuzzles) {
			assertEquals(vPuzzle.fen, rules.positionFromFen(vPuzzle.fen).fen())
		}
	}

	/** Vérifie qu'un coup joué puis annulé restaure exactement la position. */
	@Test
	fun playAndUndo() {
		val vPosition = rules.positionFromFen(kStartFen)
		vPosition.play(Move.fromUci("e2e4"))
		assertEquals(Color.Black, vPosition.sideToMove)
		assertEquals(Piece(Color.White, PieceType.Pawn), vPosition.pieceAt(sq("e4")))
		assertEquals(listOf(Move.fromUci("e2e4")), vPosition.history)
		assertEquals(Move.fromUci("e2e4"), vPosition.undo())
		assertEquals(kStartFen, vPosition.fen())
		assertTrue(vPosition.history.isEmpty())
		assertFailsWith<IllegalStateException> { vPosition.undo() }
	}

	/** Vérifie le refus des coups illégaux et des FEN invalides. */
	@Test
	fun illegalMovesAndFens() {
		val vPosition = rules.positionFromFen(kStartFen)
		assertFalse(vPosition.isLegal(Move.fromUci("e2e5")))
		assertFalse(vPosition.isLegal(Move.fromUci("e7e5")))
		assertFailsWith<IllegalMoveException> { vPosition.play(Move.fromUci("e1e2")) }
		assertEquals(kStartFen, vPosition.fen())
		assertFailsWith<IllegalArgumentException> { rules.positionFromFen("pas une fen") }
		assertFailsWith<IllegalArgumentException> { rules.positionFromFen("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq") }
		assertFailsWith<IllegalArgumentException> { rules.positionFromFen("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR x KQkq - 0 1") }
	}

	/** Vérifie le petit et le grand roque, notés comme un déplacement du roi. */
	@Test
	fun castling() {
		val vPosition = rules.positionFromFen("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1")
		val vKingMoves = vPosition.legalMovesFrom(sq("e1")).map { it.toUci() }.toSet()
		assertTrue("e1g1" in vKingMoves)
		assertTrue("e1c1" in vKingMoves)
		vPosition.play(Move.fromUci("e1g1"))
		assertEquals(Piece(Color.White, PieceType.King), vPosition.pieceAt(sq("g1")))
		assertEquals(Piece(Color.White, PieceType.Rook), vPosition.pieceAt(sq("f1")))
		assertNull(vPosition.pieceAt(sq("h1")))
		vPosition.play(Move.fromUci("e8c8"))
		assertEquals(Piece(Color.Black, PieceType.King), vPosition.pieceAt(sq("c8")))
		assertEquals(Piece(Color.Black, PieceType.Rook), vPosition.pieceAt(sq("d8")))
		assertNull(vPosition.pieceAt(sq("a8")))
		vPosition.undo()
		vPosition.undo()
		assertEquals("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1", vPosition.fen())
	}

	/** Vérifie les interdictions du roque : case traversée attaquée, droit perdu. */
	@Test
	fun castlingRestrictions() {
		// La tour noire en f2 contrôle f1 : le petit roque est interdit, le grand reste possible.
		val vAttacked = rules.positionFromFen("r3k2r/8/8/8/8/8/5r2/R3K2R w KQkq - 0 1")
		assertFalse(vAttacked.isLegal(Move.fromUci("e1g1")))
		assertTrue(vAttacked.isLegal(Move.fromUci("e1c1")))
		// Sans le droit K, le petit roque est interdit.
		val vNoRight = rules.positionFromFen("r3k2r/8/8/8/8/8/8/R3K2R w Qkq - 0 1")
		assertFalse(vNoRight.isLegal(Move.fromUci("e1g1")))
		// Après un déplacement de la tour h1, le droit est perdu.
		val vRookMoved = rules.positionFromFen("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1")
		vRookMoved.playAll("h1h2 a8a7 h2h1 a7a8")
		assertFalse(vRookMoved.isLegal(Move.fromUci("e1g1")))
		assertTrue(vRookMoved.isLegal(Move.fromUci("e1c1")))
	}

	/** Vérifie la prise en passant, après un double pas joué ou indiqué dans la FEN. */
	@Test
	fun enPassant() {
		val vPlayed = rules.positionFromFen("4k3/3p4/8/4P3/8/8/8/4K3 b - - 0 1")
		vPlayed.play(Move.fromUci("d7d5"))
		assertTrue(vPlayed.isLegal(Move.fromUci("e5d6")))
		vPlayed.play(Move.fromUci("e5d6"))
		assertNull(vPlayed.pieceAt(sq("d5")))
		assertEquals(Piece(Color.White, PieceType.Pawn), vPlayed.pieceAt(sq("d6")))
		vPlayed.undo()
		assertEquals(Piece(Color.Black, PieceType.Pawn), vPlayed.pieceAt(sq("d5")))

		val vFromFen = rules.positionFromFen("4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 2")
		assertEquals(setOf("e5e6", "e5d6"), vFromFen.legalMovesFrom(sq("e5")).map { it.toUci() }.toSet())

		// Le droit de prise en passant disparaît après un autre coup.
		val vExpired = rules.positionFromFen("4k3/3p4/8/4P3/8/8/8/4K3 b - - 0 1")
		vExpired.playAll("d7d5 e1d1 e8d8")
		assertFalse(vExpired.isLegal(Move.fromUci("e5d6")))
	}

	/** Vérifie les quatre promotions possibles et la sous-promotion. */
	@Test
	fun promotion() {
		val vPosition = rules.positionFromFen("8/P7/8/8/8/8/8/k6K w - - 0 1")
		assertEquals(setOf("a7a8q", "a7a8r", "a7a8b", "a7a8n"), vPosition.legalMovesFrom(sq("a7")).map { it.toUci() }.toSet())
		assertFalse(vPosition.isLegal(Move.fromUci("a7a8")))
		vPosition.play(Move.fromUci("a7a8n"))
		assertEquals(Piece(Color.White, PieceType.Knight), vPosition.pieceAt(sq("a8")))
		vPosition.undo()
		assertEquals(Piece(Color.White, PieceType.Pawn), vPosition.pieceAt(sq("a7")))

		val vBlack = rules.positionFromFen("K7/8/8/8/8/8/6p1/7k b - - 0 1")
		vBlack.play(Move.fromUci("g2g1q"))
		assertEquals(Piece(Color.Black, PieceType.Queen), vBlack.pieceAt(sq("g1")))
	}

	/** Vérifie l'échec, le mat et le pat. */
	@Test
	fun checkMateAndStalemate() {
		val vCheck = rules.positionFromFen(kStartFen)
		vCheck.playAll("e2e4 f7f6 d1h5")
		assertEquals(GameStatus.Check, vCheck.status())

		val vMate = rules.positionFromFen(kStartFen)
		vMate.playAll("f2f3 e7e5 g2g4 d8h4")
		assertEquals(GameStatus.Checkmate, vMate.status())
		assertTrue(vMate.legalMoves().isEmpty())

		val vStalemate = rules.positionFromFen("7k/5Q2/6K1/8/8/8/8/8 b - - 0 1")
		assertEquals(GameStatus.Stalemate, vStalemate.status())
		assertTrue(vStalemate.legalMoves().isEmpty())
	}

	/** Vérifie qu'une pièce clouée ne peut pas quitter la ligne du clouage. */
	@Test
	fun pinnedPiece() {
		val vPosition = rules.positionFromFen("4k3/4r3/8/8/8/8/4N3/4K3 w - - 0 1")
		assertTrue(vPosition.legalMovesFrom(sq("e2")).isEmpty())
	}

	/** Vérifie que la copie est indépendante de l'original. */
	@Test
	fun copyIsIndependent() {
		val vPosition = rules.positionFromFen(kStartFen)
		vPosition.play(Move.fromUci("e2e4"))
		val vCopy = vPosition.copy()
		vCopy.play(Move.fromUci("e7e5"))
		assertEquals(1, vPosition.history.size)
		assertEquals(2, vCopy.history.size)
		assertNull(vPosition.pieceAt(sq("e5")))
	}
}
