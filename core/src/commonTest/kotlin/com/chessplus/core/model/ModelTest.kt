package com.chessplus.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests des types de base : cases, pièces et coups.
 */
class ModelTest {
	/** Vérifie la numérotation des cases et leur nom algébrique. */
	@Test
	fun squareIndexAndName() {
		assertEquals(0, Square.fromAlgebraic("a1").index)
		assertEquals(63, Square.fromAlgebraic("h8").index)
		assertEquals(28, Square.fromAlgebraic("e4").index)
		assertEquals("e4", Square(28).toAlgebraic())
		assertEquals(4, Square(28).file)
		assertEquals(3, Square(28).rank)
		assertEquals(Square.kCount, Square.kAll.size)
		Square.kAll.forEach { assertEquals(it, Square.fromAlgebraic(it.toAlgebraic())) }
	}

	/** Vérifie la couleur des cases : a1 sombre, h1 claire. */
	@Test
	fun squareColor() {
		assertFalse(Square.fromAlgebraic("a1").isLight)
		assertTrue(Square.fromAlgebraic("h1").isLight)
		assertFalse(Square.fromAlgebraic("h8").isLight)
	}

	/** Vérifie le rejet des cases invalides. */
	@Test
	fun invalidSquares() {
		assertFailsWith<IllegalArgumentException> { Square(64) }
		assertFailsWith<IllegalArgumentException> { Square(-1) }
		assertFailsWith<IllegalArgumentException> { Square.fromAlgebraic("i1") }
		assertFailsWith<IllegalArgumentException> { Square.fromAlgebraic("a9") }
		assertFailsWith<IllegalArgumentException> { Square.fromAlgebraic("e") }
	}

	/** Vérifie la lecture et l'écriture des coups UCI, roque et promotion compris. */
	@Test
	fun uciMoves() {
		val vMove = Move.fromUci("e2e4")
		assertEquals(Square.fromAlgebraic("e2"), vMove.from)
		assertEquals(Square.fromAlgebraic("e4"), vMove.to)
		assertNull(vMove.promotion)
		assertEquals("e2e4", vMove.toUci())
		assertEquals("e1g1", Move.fromUci("e1g1").toUci())
		val vPromotion = Move.fromUci("e7e8q")
		assertEquals(PieceType.Queen, vPromotion.promotion)
		assertEquals("e7e8q", vPromotion.toUci())
		assertEquals(PieceType.Knight, Move.fromUci("b2b1n").promotion)
		assertEquals(4, Move.listFromUci("e8d7 a2e6 d7d8 f7f8").size)
	}

	/** Vérifie le rejet des coups UCI invalides. */
	@Test
	fun invalidUciMoves() {
		assertFailsWith<IllegalArgumentException> { Move.fromUci("e2") }
		assertFailsWith<IllegalArgumentException> { Move.fromUci("e2e4e") }
		assertFailsWith<IllegalArgumentException> { Move.fromUci("e7e8k") }
		assertFailsWith<IllegalArgumentException> { Move.fromUci("e7e8p") }
		assertFailsWith<IllegalArgumentException> { Move.fromUci("e7e8Q") }
		assertFailsWith<IllegalArgumentException> { Move.fromUci("e2e2") }
		assertFailsWith<IllegalArgumentException> { Move.fromUci("e2e4q5") }
	}

	/** Vérifie les lettres FEN des pièces. */
	@Test
	fun pieceLetters() {
		assertEquals('K', Piece(Color.White, PieceType.King).fenChar())
		assertEquals('n', Piece(Color.Black, PieceType.Knight).fenChar())
		assertEquals(PieceType.Rook, PieceType.fromChar('r'))
		assertNull(PieceType.fromChar('x'))
		assertEquals(Color.Black, Color.White.opposite())
	}
}
