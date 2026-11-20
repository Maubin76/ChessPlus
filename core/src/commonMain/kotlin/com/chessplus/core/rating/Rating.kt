package com.chessplus.core.rating

import com.chessplus.core.puzzle.PuzzleResult

/**
 * Cote d'un joueur.
 *
 * @property rating cote, sur l'échelle Glicko/Elo (1500 au départ).
 * @property deviation écart de cote (incertitude), utilisé par Glicko-2 seulement.
 * @property volatility volatilité, utilisée par Glicko-2 seulement.
 */
data class PlayerRating(
	val rating: Double,
	val deviation: Double,
	val volatility: Double,
)

/**
 * Algorithme de mise à jour de la cote du joueur après chaque problème.
 *
 * Le choix définitif (Glicko-2 ou Elo) est fait pendant la phase de design.
 */
interface RatingSystem {
	/** Cote d'un nouveau joueur. */
	val initialRating: PlayerRating

	/**
	 * Retourne la cote du joueur après un problème de difficulté [inPuzzleRating].
	 *
	 * Un problème réussi après une erreur ou une aide est passé comme [PuzzleResult.Failure].
	 */
	fun update(inPlayer: PlayerRating, inPuzzleRating: Int, inResult: PuzzleResult): PlayerRating
}

/**
 * Retourne le score d'un résultat : 1 pour une réussite, 0 pour un échec.
 */
internal fun PuzzleResult.score(): Double = if (this == PuzzleResult.Success) 1.0 else 0.0
