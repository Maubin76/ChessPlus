package com.chessplus.data.selection

import com.chessplus.core.model.ThemeSet
import com.chessplus.core.puzzle.Puzzle
import com.chessplus.core.puzzle.PuzzleResult
import com.chessplus.data.puzzle.PuzzleDao
import com.chessplus.data.user.UserDao
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * Critères de choix du prochain problème.
 *
 * @property themes thèmes acceptés (au moins un en commun) ; vide pour tous les thèmes.
 * @property ratingRange tranche de difficulté imposée, ou null pour suivre la cote du joueur.
 */
data class SelectionCriteria(
	val themes: ThemeSet = ThemeSet.kEmpty,
	val ratingRange: IntRange? = null,
)

/**
 * Choix du prochain problème, sans `ORDER BY RANDOM()` sur toute la table.
 *
 * Dans une tranche de cote (autour du joueur ou imposée), on compte les problèmes
 * correspondants, puis on lit celui d'un rang tiré au hasard grâce à l'index sur la cote.
 * Un problème déjà réussi n'est jamais reproposé ; un problème échoué revient après
 * [retryDelayMs]. Si la tranche autour du joueur ne fournit rien, elle est élargie.
 *
 * @property puzzleDao accès à la base des problèmes.
 * @property userDao accès à la base utilisateur.
 * @property random générateur aléatoire (injectable pour les tests).
 * @property retryDelayMs délai avant de reproposer un problème échoué.
 */
class PuzzleSelector(
	private val puzzleDao: PuzzleDao,
	private val userDao: UserDao,
	private val random: Random = Random.Default,
	private val retryDelayMs: Long = kDefaultRetryDelayMs,
) {
	/**
	 * Retourne le prochain problème, ou null si aucun problème ne correspond aux critères.
	 *
	 * @param inPlayerRating cote du joueur, centre de la tranche quand aucune n'est imposée.
	 * @param inCriteria thèmes et tranche de difficulté.
	 * @param inExcludeId problème à ne pas reproposer tout de suite (celui qui vient d'être joué).
	 * @param inNow date courante (millisecondes).
	 */
	suspend fun nextPuzzle(inPlayerRating: Double, inCriteria: SelectionCriteria, inExcludeId: String?, inNow: Long): Puzzle? {
		for (vRange in candidateRanges(inPlayerRating.roundToInt(), inCriteria.ratingRange)) {
			val vCount = puzzleDao.countInRange(vRange.first, vRange.last, inCriteria.themes.low, inCriteria.themes.high)
			if (vCount == 0) {
				continue
			}
			repeat(minOf(kTriesPerRange, vCount)) {
				val vOffset = random.nextInt(vCount)
				val vId = puzzleDao.getIdAtOffsetInRange(vRange.first, vRange.last, inCriteria.themes.low, inCriteria.themes.high, vOffset)
				if (vId != null && vId != inExcludeId && isEligible(vId, inNow)) {
					puzzleDao.getById(vId)?.let { return it.toPuzzle() }
				}
			}
		}
		return null
	}

	/**
	 * Retourne les tranches de cote à essayer dans l'ordre : la tranche imposée seule, ou des
	 * tranches de plus en plus larges autour de la cote du joueur.
	 */
	internal fun candidateRanges(inCenter: Int, inImposed: IntRange?): List<IntRange> {
		if (inImposed != null) {
			return listOf(inImposed)
		}
		return (kInitialHalfWidth..kMaxHalfWidth step kHalfWidthStep).map { inHalfWidth -> (inCenter - inHalfWidth)..(inCenter + inHalfWidth) }
	}

	/**
	 * Indique si un problème peut être proposé : jamais joué, ou échoué depuis assez longtemps.
	 */
	private suspend fun isEligible(inPuzzleId: String, inNow: Long): Boolean {
		val vState = userDao.getState(inPuzzleId) ?: return true
		if (vState.solved) {
			return false
		}
		return vState.lastResult == PuzzleResult.Failure && inNow - vState.lastPlayedAt >= retryDelayMs
	}

	companion object {
		/** Demi-largeur initiale de la tranche autour de la cote du joueur. */
		const val kInitialHalfWidth = 100

		/** Pas d'élargissement de la tranche. */
		const val kHalfWidthStep = 100

		/** Demi-largeur maximale de la tranche. */
		const val kMaxHalfWidth = 600

		/** Nombre de tirages par tranche avant de l'élargir. */
		const val kTriesPerRange = 10

		/** Délai par défaut avant de reproposer un problème échoué : 24 heures. */
		const val kDefaultRetryDelayMs = 24L * 60 * 60 * 1000
	}
}
