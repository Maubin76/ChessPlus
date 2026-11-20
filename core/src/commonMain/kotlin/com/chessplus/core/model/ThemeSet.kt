package com.chessplus.core.model

/**
 * Ensemble de thèmes stocké sous forme de masque de bits sur deux entiers de 64 bits.
 *
 * [low] contient les bits 0 à 63, [high] les bits 64 à 127. Ces deux valeurs sont
 * stockées telles quelles dans deux colonnes `INTEGER` de la base.
 */
data class ThemeSet(val low: Long = 0L, val high: Long = 0L) : Iterable<Theme> {
	/**
	 * Indique si l'ensemble contient le thème donné.
	 */
	operator fun contains(inTheme: Theme): Boolean {
		val vMask = bitMask(inTheme.bit)
		val vWord = if (inTheme.bit < kWordBits) low else high
		return vWord and vMask != 0L
	}

	/**
	 * Retourne un nouvel ensemble augmenté du thème donné.
	 */
	operator fun plus(inTheme: Theme): ThemeSet {
		val vMask = bitMask(inTheme.bit)
		return if (inTheme.bit < kWordBits) copy(low = low or vMask) else copy(high = high or vMask)
	}

	/**
	 * Indique si l'ensemble partage au moins un thème avec l'ensemble donné.
	 */
	fun intersects(inOther: ThemeSet): Boolean = (low and inOther.low) != 0L || (high and inOther.high) != 0L

	/** Indique si l'ensemble est vide. */
	val isEmpty: Boolean
		get() = low == 0L && high == 0L

	/**
	 * Parcourt les thèmes de l'ensemble dans l'ordre des bits.
	 */
	override fun iterator(): Iterator<Theme> = Theme.entries.filter { it in this }.iterator()

	/**
	 * Retourne les identifiants Lichess des thèmes, séparés par des espaces.
	 */
	fun toKeys(): String = joinToString(" ") { it.key }

	companion object {
		/** Nombre de bits d'un mot du masque. */
		private const val kWordBits = 64

		/** Ensemble vide. */
		val kEmpty = ThemeSet()

		/**
		 * Retourne le masque d'un bit dans son mot de 64 bits.
		 */
		private fun bitMask(inBit: Int): Long = 1L shl (inBit % kWordBits)

		/**
		 * Construit un ensemble à partir d'une liste de thèmes.
		 */
		fun of(inThemes: Iterable<Theme>): ThemeSet = inThemes.fold(kEmpty) { inSet, inTheme -> inSet + inTheme }

		/**
		 * Construit un ensemble à partir de thèmes.
		 */
		fun of(vararg inThemes: Theme): ThemeSet = of(inThemes.asIterable())

		/**
		 * Lit le champ `Themes` du CSV Lichess (identifiants séparés par des espaces).
		 *
		 * @throws UnknownThemeException si un identifiant est inconnu : un thème ne doit
		 * jamais être ignoré en silence.
		 */
		fun parse(inKeys: String): ThemeSet {
			var vResult = kEmpty
			for (vKey in inKeys.split(' ')) {
				if (vKey.isEmpty()) {
					continue
				}
				val vTheme = Theme.fromKey(vKey) ?: throw UnknownThemeException(vKey)
				vResult += vTheme
			}
			return vResult
		}
	}
}

/**
 * Levée quand le CSV contient un thème absent de l'énumération [Theme].
 *
 * @property key identifiant du thème inconnu.
 */
class UnknownThemeException(val key: String) : IllegalArgumentException("Thème Lichess inconnu : '$key'")
