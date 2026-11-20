package com.chessplus.core.codec

import com.chessplus.core.model.Move
import com.chessplus.core.model.PieceType
import com.chessplus.core.model.Square

/**
 * Encodage compact des coups, utilisé pour la colonne `BLOB` de la base.
 *
 * Chaque coup tient sur 2 octets (gros-boutiste) :
 * - bits 0 à 5 : case de départ ;
 * - bits 6 à 11 : case d'arrivée ;
 * - bits 12 à 14 : promotion (0 aucune, 1 cavalier, 2 fou, 3 tour, 4 dame) ;
 * - bit 15 : toujours 0.
 */
object MoveCodec {
	/** Nombre d'octets par coup. */
	const val kBytesPerMove = 2

	/** Nombre de bits d'une case. */
	private const val kSquareBits = 6

	/** Masque d'une case sur 6 bits. */
	private const val kSquareMask = 0x3F

	/** Décalage du champ promotion. */
	private const val kPromotionShift = 12

	/** Masque du champ promotion sur 3 bits. */
	private const val kPromotionMask = 0x7

	/** Masque d'un octet. */
	private const val kByteMask = 0xFF

	/** Nombre de bits d'un octet. */
	private const val kByteBits = 8

	/** Pièces de promotion, dans l'ordre de leur code (le code 0 signifie « aucune »). */
	private val kPromotionPieces = listOf(PieceType.Knight, PieceType.Bishop, PieceType.Rook, PieceType.Queen)

	/**
	 * Encode un coup sur 16 bits.
	 */
	fun encodeMove(inMove: Move): Int {
		val vPromotionCode = if (inMove.promotion == null) 0 else kPromotionPieces.indexOf(inMove.promotion) + 1
		return inMove.from.index or (inMove.to.index shl kSquareBits) or (vPromotionCode shl kPromotionShift)
	}

	/**
	 * Décode un coup encodé sur 16 bits.
	 *
	 * @throws IllegalArgumentException si la valeur n'est pas un coup valide.
	 */
	fun decodeMove(inCode: Int): Move {
		require(inCode in 0..0x7FFF) { "Code de coup hors limites : $inCode" }
		val vFrom = Square(inCode and kSquareMask)
		val vTo = Square((inCode shr kSquareBits) and kSquareMask)
		val vPromotionCode = (inCode shr kPromotionShift) and kPromotionMask
		require(vPromotionCode <= kPromotionPieces.size) { "Code de promotion invalide : $vPromotionCode" }
		val vPromotion = if (vPromotionCode == 0) null else kPromotionPieces[vPromotionCode - 1]
		return Move(vFrom, vTo, vPromotion)
	}

	/**
	 * Encode une suite de coups en tableau d'octets.
	 */
	fun encode(inMoves: List<Move>): ByteArray {
		val vBytes = ByteArray(inMoves.size * kBytesPerMove)
		inMoves.forEachIndexed { inIndex, inMove ->
			val vCode = encodeMove(inMove)
			vBytes[inIndex * kBytesPerMove] = (vCode shr kByteBits).toByte()
			vBytes[inIndex * kBytesPerMove + 1] = (vCode and kByteMask).toByte()
		}
		return vBytes
	}

	/**
	 * Décode un tableau d'octets en suite de coups.
	 *
	 * @throws IllegalArgumentException si la taille est impaire ou si un coup est invalide.
	 */
	fun decode(inBytes: ByteArray): List<Move> {
		require(inBytes.size % kBytesPerMove == 0) { "Taille de suite de coups invalide : ${inBytes.size} octets" }
		return List(inBytes.size / kBytesPerMove) { inIndex ->
			val vHigh = inBytes[inIndex * kBytesPerMove].toInt() and kByteMask
			val vLow = inBytes[inIndex * kBytesPerMove + 1].toInt() and kByteMask
			decodeMove((vHigh shl kByteBits) or vLow)
		}
	}
}
