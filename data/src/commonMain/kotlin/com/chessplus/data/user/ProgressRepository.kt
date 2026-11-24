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
 * @property dayStart début du jour local d'une date (les révisions tombent en début de journée).
 */
class ProgressRepository(
	private val database: UserDatabase,
	private val ratingSystem: RatingSystem,
	private val dayStart: (Long) -> Long = { it },
) {
	/** DAO de la base utilisateur. */
	private val dao: UserDao = database.userDao()

	/**
	 * Retourne la cote actuelle du joueur, ou la cote initiale avant le premier problème.
	 */
	suspend fun currentRating(): PlayerRating = dao.getPlayer()?.toRating() ?: ratingSystem.initialRating

	/**
	 * Enregistre le résultat d'un problème dans une seule transaction : tentative, bilan du
	 * problème, file de révision et, si [inRated], nouvelle cote du joueur. Retourne la cote après le problème.
	 * [inDurationMs] et [inFailedAtMove] alimentent les statistiques (null si inconnus).
	 */
	suspend fun recordResult(
		inPuzzle: Puzzle,
		inResult: PuzzleResult,
		inMistakeCount: Int,
		inRevealedCount: Int,
		inRated: Boolean,
		inNow: Long,
		inDurationMs: Long? = null,
		inFailedAtMove: Int? = null,
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
					durationMs = inDurationMs,
					failedAtMove = inFailedAtMove,
					playerDeviationAfter = if (inRated) vAfter.deviation else null,
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
			scheduleReview(inPuzzle.id, inResult, inNow)
			if (inRated) {
				dao.upsertPlayer(PlayerEntity(rating = vAfter.rating, deviation = vAfter.deviation, volatility = vAfter.volatility, updatedAt = inNow))
			}
			vAfter
		}
	}

	/**
	 * Met à jour la file de révision : un échec (re)place le puzzle au premier palier ; une réussite
	 * sur un puzzle à réviser le fait passer au palier suivant, ou le retire après le dernier.
	 * Une réussite avant l'échéance ne change rien.
	 */
	private suspend fun scheduleReview(inPuzzleId: String, inResult: PuzzleResult, inNow: Long) {
		val vItem = dao.getReviewItem(inPuzzleId)
		if (inResult == PuzzleResult.Failure) {
			dao.upsertReviewItem(ReviewItemEntity(inPuzzleId, 0, dueIn(inNow, 0), vItem?.addedAt ?: inNow))
			return
		}
		if (vItem == null || vItem.dueAt > inNow) {
			return
		}
		val vNext = vItem.level + 1
		if (vNext >= kReviewIntervalsDays.size) {
			dao.deleteReviewItem(inPuzzleId)
		} else {
			dao.upsertReviewItem(vItem.copy(level = vNext, dueAt = dueIn(inNow, vNext)))
		}
	}

	/**
	 * Retourne l'échéance du palier [inLevel] : début du jour local, plus l'intervalle du palier.
	 */
	private fun dueIn(inNow: Long, inLevel: Int): Long = dayStart(inNow) + kReviewIntervalsDays[inLevel] * kDayMs

	/**
	 * Convertit la ligne de cote en cote du modèle.
	 */
	private fun PlayerEntity.toRating(): PlayerRating = PlayerRating(rating, deviation, volatility)

	companion object {
		/** Intervalles de révision, en jours, palier par palier ; réussi au dernier, le puzzle est maîtrisé. */
		val kReviewIntervalsDays = listOf(1L, 3L, 7L, 14L, 30L)

		/** Durée d'un jour en millisecondes. */
		private const val kDayMs = 24L * 60 * 60 * 1000
	}
}
