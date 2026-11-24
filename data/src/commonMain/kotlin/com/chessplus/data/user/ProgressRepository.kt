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
	 * problème, file de révision (si [inReviewable] ; pas pour les défis) et, si [inRated], nouvelle
	 * cote du joueur. Retourne la cote après le problème.
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
		inReviewable: Boolean = true,
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
			if (inReviewable) {
				scheduleReview(inPuzzle.id, inResult, inNow)
			}
			if (inRated) {
				dao.upsertPlayer(PlayerEntity(rating = vAfter.rating, deviation = vAfter.deviation, volatility = vAfter.volatility, updatedAt = inNow))
			}
			vAfter
		}
	}

	/**
	 * Met à jour la file de révision. Un premier échec place le puzzle au premier palier ; un nouvel
	 * échec d'un puzzle déjà dans la file le fait redescendre d'un palier seulement. Dans les deux
	 * cas, il est à revoir le lendemain. Une réussite sur un puzzle arrivé à échéance le fait passer
	 * au palier suivant, ou le retire après le dernier ; une réussite avant l'échéance ne change rien.
	 * La file garde au plus [kReviewQueueLimit] puzzles : ceux des paliers les plus bas, ratés depuis le
	 * plus longtemps, en sortent.
	 */
	private suspend fun scheduleReview(inPuzzleId: String, inResult: PuzzleResult, inNow: Long) {
		val vItem = dao.getReviewItem(inPuzzleId)
		// Paliers d'avant le passage à trois réussites ramenés au dernier palier actuel.
		val vLevel = vItem?.level?.coerceAtMost(kReviewIntervalsDays.lastIndex)
		if (inResult == PuzzleResult.Failure) {
			val vDown = if (vLevel == null) 0 else maxOf(0, vLevel - 1)
			dao.upsertReviewItem(ReviewItemEntity(inPuzzleId, vDown, dueIn(inNow, 0), inNow))
			val vExcess = dao.countReviewItems() - kReviewQueueLimit
			if (vExcess > 0) {
				dao.trimReviewItems(vExcess)
			}
			return
		}
		if (vItem == null || vLevel == null || vItem.dueAt > inNow) {
			return
		}
		val vNext = vLevel + 1
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
		/**
		 * Intervalles de révision, en jours, palier par palier : trois réussites à échéance d'affilée
		 * (au lendemain de l'échec, une semaine plus tard, un mois plus tard) et le puzzle est maîtrisé.
		 */
		val kReviewIntervalsDays = listOf(1L, 7L, 30L)

		/** Nombre maximal de puzzles dans la file de révision. */
		const val kReviewQueueLimit = 200

		/** Durée d'un jour en millisecondes. */
		private const val kDayMs = 24L * 60 * 60 * 1000
	}
}
