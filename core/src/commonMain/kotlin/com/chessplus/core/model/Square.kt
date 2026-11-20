package com.chessplus.core.model

import kotlin.jvm.JvmInline

/**
 * Case de l'échiquier, identifiée par un index de 0 (a1) à 63 (h8).
 *
 * L'index vaut `rangée * 8 + colonne`, avec la colonne a = 0 et la rangée 1 = 0.
 * Il tient sur 6 bits, ce qu'exploite le codec des coups.
 */
@JvmInline
value class Square(val index: Int) {
	init {
		require(index in 0 until kCount) { "Index de case hors limites : $index" }
	}

	/** Colonne de la case, de 0 (a) à 7 (h). */
	val file: Int
		get() = index % kSide

	/** Rangée de la case, de 0 (rangée 1) à 7 (rangée 8). */
	val rank: Int
		get() = index / kSide

	/** Indique si la case est claire (h1 est claire, a1 est sombre). */
	val isLight: Boolean
		get() = (file + rank) % 2 == 1

	/**
	 * Retourne le nom algébrique de la case, par exemple `e4`.
	 */
	fun toAlgebraic(): String = "${'a' + file}${'1' + rank}"

	/**
	 * Représentation lisible, identique au nom algébrique.
	 */
	override fun toString(): String = toAlgebraic()

	companion object {
		/** Nombre de cases de l'échiquier. */
		const val kCount = 64

		/** Nombre de cases d'un côté de l'échiquier. */
		const val kSide = 8

		/** Toutes les cases, de a1 à h8. */
		val kAll: List<Square> = List(kCount) { Square(it) }

		/**
		 * Retourne la case située à la colonne et à la rangée données (de 0 à 7).
		 */
		fun of(inFile: Int, inRank: Int): Square {
			require(inFile in 0 until kSide && inRank in 0 until kSide) { "Case hors limites : $inFile, $inRank" }
			return Square(inRank * kSide + inFile)
		}

		/**
		 * Retourne la case correspondant à un nom algébrique (`e4`).
		 *
		 * @throws IllegalArgumentException si le nom n'est pas valide.
		 */
		fun fromAlgebraic(inName: String): Square {
			require(inName.length == 2) { "Nom de case invalide : '$inName'" }
			val vFile = inName[0] - 'a'
			val vRank = inName[1] - '1'
			require(vFile in 0 until kSide && vRank in 0 until kSide) { "Nom de case invalide : '$inName'" }
			return of(vFile, vRank)
		}
	}
}
