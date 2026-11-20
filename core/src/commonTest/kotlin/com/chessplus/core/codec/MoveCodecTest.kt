package com.chessplus.core.codec

import com.chessplus.core.SampleData
import com.chessplus.core.model.Move
import com.chessplus.core.model.PieceType
import com.chessplus.core.model.Square
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Tests du codec des coups sur 2 octets.
 */
class MoveCodecTest {
	/** Vérifie l'encodage exact de coups connus. */
	@Test
	fun knownEncodings() {
		// e2e4 : départ 12, arrivée 28 -> 12 | 28 << 6 = 0x070C.
		assertEquals(0x070C, MoveCodec.encodeMove(Move.fromUci("e2e4")))
		assertContentEquals(byteArrayOf(0x07, 0x0C), MoveCodec.encode(listOf(Move.fromUci("e2e4"))))
		// e7e8q : départ 52, arrivée 60, dame (4) -> 52 | 60 << 6 | 4 << 12 = 0x4F34.
		assertEquals(0x4F34, MoveCodec.encodeMove(Move.fromUci("e7e8q")))
	}

	/** Vérifie l'aller-retour des coups des quatre problèmes de l'extrait. */
	@Test
	fun samplePuzzlesRoundTrip() {
		for (vPuzzle in SampleData.kPuzzles) {
			val vBytes = MoveCodec.encode(vPuzzle.moves)
			assertEquals(vPuzzle.moves.size * MoveCodec.kBytesPerMove, vBytes.size)
			assertEquals(vPuzzle.moves, MoveCodec.decode(vBytes))
		}
	}

	/** Vérifie l'aller-retour de toutes les combinaisons de cases et de promotions. */
	@Test
	fun allMovesRoundTrip() {
		val vPromotions = listOf(null, PieceType.Knight, PieceType.Bishop, PieceType.Rook, PieceType.Queen)
		val vMoves = mutableListOf<Move>()
		for (vFrom in Square.kAll) {
			for (vTo in Square.kAll) {
				if (vFrom == vTo) {
					continue
				}
				for (vPromotion in vPromotions) {
					val vMove = Move(vFrom, vTo, vPromotion)
					assertEquals(vMove, MoveCodec.decodeMove(MoveCodec.encodeMove(vMove)))
					vMoves.add(vMove)
				}
			}
		}
		assertEquals(vMoves, MoveCodec.decode(MoveCodec.encode(vMoves)))
	}

	/** Vérifie le rejet des données invalides. */
	@Test
	fun invalidData() {
		assertFailsWith<IllegalArgumentException> { MoveCodec.decode(byteArrayOf(0x07)) }
		// Code de promotion 5, inexistant.
		assertFailsWith<IllegalArgumentException> { MoveCodec.decodeMove(0x5F34) }
		// Bit 15 positionné.
		assertFailsWith<IllegalArgumentException> { MoveCodec.decodeMove(0x870C) }
		// Départ et arrivée identiques.
		assertFailsWith<IllegalArgumentException> { MoveCodec.decodeMove(0) }
		assertEquals(emptyList(), MoveCodec.decode(ByteArray(0)))
	}
}
