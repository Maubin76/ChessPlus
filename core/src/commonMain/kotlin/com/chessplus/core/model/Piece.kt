package com.chessplus.core.model

/**
 * Type de pièce, indépendamment de sa couleur.
 *
 * @property fenChar lettre de la pièce en notation FEN pour les blancs (majuscule).
 */
enum class PieceType(val fenChar: Char) {
	Pawn('P'),
	Knight('N'),
	Bishop('B'),
	Rook('R'),
	Queen('Q'),
	King('K');

	/** Indique si un pion peut être promu en ce type de pièce. */
	val isPromotionTarget: Boolean
		get() = this == Knight || this == Bishop || this == Rook || this == Queen

	companion object {
		/**
		 * Retourne le type correspondant à une lettre FEN ou UCI, sans tenir compte de la casse,
		 * ou null si la lettre ne désigne aucune pièce.
		 */
		fun fromChar(inChar: Char): PieceType? {
			val vUpper = inChar.uppercaseChar()
			return entries.firstOrNull { it.fenChar == vUpper }
		}
	}
}

/**
 * Pièce posée sur l'échiquier : un type et une couleur.
 */
data class Piece(val color: Color, val type: PieceType) {
	/**
	 * Retourne la lettre FEN de la pièce : majuscule pour les blancs, minuscule pour les noirs.
	 */
	fun fenChar(): Char = if (color == Color.White) type.fenChar else type.fenChar.lowercaseChar()

	/**
	 * Représentation lisible, identique à la lettre FEN.
	 */
	override fun toString(): String = fenChar().toString()
}
