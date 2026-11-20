package com.chessplus.core.rating

import com.chessplus.core.puzzle.PuzzleResult
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.sqrt

/**
 * Résultat d'une rencontre pour le calcul Glicko-2.
 *
 * @property opponent cote de l'adversaire (ici, le problème).
 * @property score 1 pour une victoire, 0,5 pour une nulle, 0 pour une défaite.
 */
data class Glicko2Game(val opponent: PlayerRating, val score: Double)

/**
 * Cote Glicko-2 (Mark Glickman, « Example of the Glicko-2 system », 2022), comme sur Lichess.
 *
 * Chaque problème est traité comme une période de notation d'une seule partie contre un
 * adversaire de cote [puzzleDeviation] fixe, puisque l'écart de cote des problèmes n'est
 * pas stocké en base.
 *
 * @property tau contrainte sur l'évolution de la volatilité (0,3 à 1,2).
 * @property puzzleDeviation écart de cote attribué aux problèmes.
 * @property minDeviation écart de cote minimal du joueur, pour que la cote continue d'évoluer.
 * @property maxDeviation écart de cote maximal du joueur.
 */
class Glicko2RatingSystem(
	private val tau: Double = kDefaultTau,
	private val puzzleDeviation: Double = kDefaultPuzzleDeviation,
	private val minDeviation: Double = kDefaultMinDeviation,
	private val maxDeviation: Double = kDefaultMaxDeviation,
) : RatingSystem {

	/** Cote de départ : 1500, écart 350, volatilité 0,06. */
	override val initialRating: PlayerRating = PlayerRating(kInitialRating, kDefaultMaxDeviation, kInitialVolatility)

	/**
	 * Met à jour la cote après un problème, avec un écart de cote borné.
	 */
	override fun update(inPlayer: PlayerRating, inPuzzleRating: Int, inResult: PuzzleResult): PlayerRating {
		val vPuzzle = PlayerRating(inPuzzleRating.toDouble(), puzzleDeviation, kInitialVolatility)
		val vRated = rate(inPlayer, listOf(Glicko2Game(vPuzzle, inResult.score())))
		return vRated.copy(deviation = vRated.deviation.coerceIn(minDeviation, maxDeviation))
	}

	/**
	 * Calcule la cote d'un joueur après une période de notation, sans borner l'écart de cote.
	 *
	 * Sans partie, seul l'écart de cote augmente (étape 6 de l'algorithme).
	 */
	fun rate(inPlayer: PlayerRating, inGames: List<Glicko2Game>): PlayerRating {
		val vMu = (inPlayer.rating - kInitialRating) / kScale
		val vPhi = inPlayer.deviation / kScale
		val vSigma = inPlayer.volatility
		if (inGames.isEmpty()) {
			val vPhiStar = sqrt(vPhi * vPhi + vSigma * vSigma)
			return inPlayer.copy(deviation = vPhiStar * kScale)
		}

		// Étapes 3 et 4 : variance estimée v et amélioration estimée delta.
		var vInverseV = 0.0
		var vScoreSum = 0.0
		for (vGame in inGames) {
			val vMuJ = (vGame.opponent.rating - kInitialRating) / kScale
			val vPhiJ = vGame.opponent.deviation / kScale
			val vG = g(vPhiJ)
			val vE = expected(vMu, vMuJ, vG)
			vInverseV += vG * vG * vE * (1.0 - vE)
			vScoreSum += vG * (vGame.score - vE)
		}
		val vV = 1.0 / vInverseV
		val vDelta = vV * vScoreSum

		// Étape 5 : nouvelle volatilité.
		val vNewSigma = newVolatility(vPhi, vSigma, vV, vDelta)

		// Étapes 6 et 7 : nouvel écart et nouvelle cote.
		val vPhiStar = sqrt(vPhi * vPhi + vNewSigma * vNewSigma)
		val vNewPhi = 1.0 / sqrt(1.0 / (vPhiStar * vPhiStar) + 1.0 / vV)
		val vNewMu = vMu + vNewPhi * vNewPhi * vScoreSum

		// Étape 8 : retour à l'échelle Glicko.
		return PlayerRating(
			rating = vNewMu * kScale + kInitialRating,
			deviation = vNewPhi * kScale,
			volatility = vNewSigma,
		)
	}

	/**
	 * Fonction g de Glicko-2 : réduit l'influence d'un adversaire incertain.
	 */
	private fun g(inPhi: Double): Double = 1.0 / sqrt(1.0 + 3.0 * inPhi * inPhi / (PI * PI))

	/**
	 * Score attendu contre un adversaire.
	 */
	private fun expected(inMu: Double, inMuJ: Double, inG: Double): Double = 1.0 / (1.0 + exp(-inG * (inMu - inMuJ)))

	/**
	 * Calcule la nouvelle volatilité par la méthode d'Illinois (étape 5 de l'algorithme).
	 */
	private fun newVolatility(inPhi: Double, inSigma: Double, inV: Double, inDelta: Double): Double {
		val vA = ln(inSigma * inSigma)
		val vPhi2 = inPhi * inPhi
		val vDelta2 = inDelta * inDelta
		val vF = { inX: Double ->
			val vEx = exp(inX)
			val vDenominator = vPhi2 + inV + vEx
			vEx * (vDelta2 - vPhi2 - inV - vEx) / (2.0 * vDenominator * vDenominator) - (inX - vA) / (tau * tau)
		}

		var vLow = vA
		var vHigh: Double
		if (vDelta2 > vPhi2 + inV) {
			vHigh = ln(vDelta2 - vPhi2 - inV)
		} else {
			var vK = 1
			while (vF(vA - vK * tau) < 0) {
				vK++
			}
			vHigh = vA - vK * tau
		}

		var vFLow = vF(vLow)
		var vFHigh = vF(vHigh)
		while (abs(vHigh - vLow) > kConvergence) {
			val vC = vLow + (vLow - vHigh) * vFLow / (vFHigh - vFLow)
			val vFC = vF(vC)
			if (vFC * vFHigh <= 0) {
				vLow = vHigh
				vFLow = vFHigh
			} else {
				vFLow /= 2.0
			}
			vHigh = vC
			vFHigh = vFC
		}
		return exp(vLow / 2.0)
	}

	companion object {
		/** Cote de départ. */
		const val kInitialRating = 1500.0

		/** Volatilité de départ. */
		const val kInitialVolatility = 0.06

		/** Facteur de conversion entre l'échelle Glicko et l'échelle Glicko-2. */
		const val kScale = 173.7178

		/** Valeur de tau par défaut (celle de Lichess). */
		const val kDefaultTau = 0.75

		/** Écart de cote attribué aux problèmes par défaut. */
		const val kDefaultPuzzleDeviation = 75.0

		/** Écart de cote minimal du joueur par défaut. */
		const val kDefaultMinDeviation = 45.0

		/** Écart de cote maximal du joueur par défaut. */
		const val kDefaultMaxDeviation = 350.0

		/** Précision de la recherche de la volatilité. */
		private const val kConvergence = 0.000001
	}
}
