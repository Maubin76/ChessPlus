package com.chessplus.core.model

/**
 * Coup d'échecs : case de départ, case d'arrivée et éventuelle pièce de promotion.
 *
 * Le roque est représenté comme un déplacement du roi (`e1g1`), comme en notation UCI.
 */
data class Move(val from: Square, val to: Square, val promotion: PieceType? = null) {
	init {
		require(from != to) { "Un coup doit changer de case : $from" }
		require(promotion == null || promotion.isPromotionTarget) { "Promotion impossible en $promotion" }
	}

	/**
	 * Retourne le coup en notation UCI : `e2e4`, ou `e7e8q` pour une promotion.
	 */
	fun toUci(): String {
		val vBase = from.toAlgebraic() + to.toAlgebraic()
		return if (promotion == null) vBase else vBase + promotion.fenChar.lowercaseChar()
	}

	/**
	 * Représentation lisible, identique à la notation UCI.
	 */
	override fun toString(): String = toUci()

	companion object {
		/** Longueur d'un coup UCI sans promotion. */
		private const val kUciLength = 4

		/** Longueur d'un coup UCI avec promotion. */
		private const val kUciPromotionLength = 5

		/**
		 * Lit un coup en notation UCI (`e2e4`, `e7e8q`).
		 *
		 * @throws IllegalArgumentException si la notation n'est pas valide.
		 */
		fun fromUci(inUci: String): Move {
			require(inUci.length == kUciLength || inUci.length == kUciPromotionLength) { "Coup UCI invalide : '$inUci'" }
			val vFrom = Square.fromAlgebraic(inUci.substring(0, 2))
			val vTo = Square.fromAlgebraic(inUci.substring(2, 4))
			var vPromotion: PieceType? = null
			if (inUci.length == kUciPromotionLength) {
				val vChar = inUci[4]
				require(vChar.isLowerCase()) { "Pièce de promotion attendue en minuscule : '$inUci'" }
				vPromotion = PieceType.fromChar(vChar)
				require(vPromotion != null && vPromotion.isPromotionTarget) { "Pièce de promotion invalide : '$inUci'" }
			}
			return Move(vFrom, vTo, vPromotion)
		}

		/**
		 * Lit une suite de coups UCI séparés par des espaces.
		 *
		 * @throws IllegalArgumentException si l'un des coups n'est pas valide.
		 */
		fun listFromUci(inUciList: String): List<Move> =
			inUciList.split(' ').filter { it.isNotEmpty() }.map { fromUci(it) }
	}
}
