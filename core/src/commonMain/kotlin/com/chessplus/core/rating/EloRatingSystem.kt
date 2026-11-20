package com.chessplus.core.rating

import com.chessplus.core.puzzle.PuzzleResult
import kotlin.math.pow

/**
 * Cote Elo simple, avec un facteur K constant.
 *
 * L'écart de cote et la volatilité du joueur sont conservés tels quels.
 *
 * @property kFactor amplitude maximale d'une mise à jour.
 */
class EloRatingSystem(
	private val kFactor: Double = kDefaultKFactor,
) : RatingSystem {

	/** Cote de départ : 1500. */
	override val initialRating: PlayerRating = PlayerRating(kInitialRating, 0.0, 0.0)

	/**
	 * Applique la formule Elo : `R' = R + K (S - E)`, avec `E = 1 / (1 + 10^((Rp - R) / 400))`.
	 */
	override fun update(inPlayer: PlayerRating, inPuzzleRating: Int, inResult: PuzzleResult): PlayerRating {
		val vExpected = expectedScore(inPlayer.rating, inPuzzleRating.toDouble())
		return inPlayer.copy(rating = inPlayer.rating + kFactor * (inResult.score() - vExpected))
	}

	/**
	 * Retourne le score attendu d'un joueur de cote [inRating] face à une difficulté [inOpponentRating].
	 */
	fun expectedScore(inRating: Double, inOpponentRating: Double): Double =
		1.0 / (1.0 + 10.0.pow((inOpponentRating - inRating) / kScale))

	companion object {
		/** Cote de départ. */
		const val kInitialRating = 1500.0

		/** Facteur K par défaut. */
		const val kDefaultKFactor = 32.0

		/** Écart de cote pour lequel le score attendu est divisé par 10. */
		private const val kScale = 400.0
	}
}
