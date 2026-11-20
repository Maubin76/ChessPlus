package com.chessplus.data.user

import androidx.room.immediateTransaction
import androidx.room.useWriterConnection
import com.chessplus.core.puzzle.Puzzle
import com.chessplus.core.puzzle.PuzzleResult
import com.chessplus.core.rating.PlayerRating
import com.chessplus.core.rating.RatingSystem

/**
 * Enregistrement des résultats et lecture de la cote du joueur.
 *
 * @property database base utilisateur.
 * @property ratingSystem algorithme de cote.
 */
class ProgressRepository(
	private val database: UserDatabase,
	private val ratingSystem: RatingSystem,
) {
	/** DAO de la base utilisateur. */
	private val dao: UserDao = database.userDao()

	/**
	 * Retourne la cote actuelle du joueur, ou la cote initiale avant le premier problème.
	 */
	suspend fun currentRating(): PlayerRating = dao.getPlayer()?.toRating() ?: ratingSystem.initialRating

	/**
	 * Enregistre le résultat d'un problème dans une seule transaction : tentative, bilan du
	 * problème et, si [inRated], nouvelle cote du joueur. Retourne la cote après le problème.
	 */
	suspend fun recordResult(
		inPuzzle: Puzzle,
		inResult: PuzzleResult,
		inMistakeCount: Int,
		inRevealedCount: Int,
		inRated: Boolean,
		inNow: Long,
	): PlayerRating = database.useWriterConnection { inTransactor ->
		inTransactor.immediateTransaction {
			val vBefore = currentRating()
			val vAfter = if (inRated) ratingSystem.update(vBefore, inPuzzle.rating, inResult) else vBefore
			dao.insertAttempt(
				AttemptEntity(
					puzzleId = inPuzzle.id,
					playedAt = inNow,
					result = inResult,
					rated = inRated,
					puzzleRating = inPuzzle.rating,
					themesLow = inPuzzle.themes.low,
					themesHigh = inPuzzle.themes.high,
					mistakeCount = inMistakeCount,
					revealedCount = inRevealedCount,
					playerRatingBefore = vBefore.rating,
					playerRatingAfter = vAfter.rating,
				)
			)
			val vState = dao.getState(inPuzzle.id)
			val vFailure = if (inResult == PuzzleResult.Failure) 1 else 0
			dao.upsertState(
				PuzzleStateEntity(
					puzzleId = inPuzzle.id,
					solved = (vState?.solved ?: false) || inResult == PuzzleResult.Success,
					attemptCount = (vState?.attemptCount ?: 0) + 1,
					failureCount = (vState?.failureCount ?: 0) + vFailure,
					lastResult = inResult,
					lastPlayedAt = inNow,
				)
			)
			if (inRated) {
				dao.upsertPlayer(PlayerEntity(rating = vAfter.rating, deviation = vAfter.deviation, volatility = vAfter.volatility, updatedAt = inNow))
			}
			vAfter
		}
	}

	/**
	 * Convertit la ligne de cote en cote du modèle.
	 */
	private fun PlayerEntity.toRating(): PlayerRating = PlayerRating(rating, deviation, volatility)
}
