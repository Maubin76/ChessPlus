package com.chessplus.app.game

/**
 * Défi chronométré ou à vies, sans effet sur la cote.
 *
 * @property key identifiant enregistré dans la base utilisateur.
 */
enum class ChallengeKind(val key: String) {
	/** Un maximum de problèmes de difficulté croissante, avec [ChallengeRun.kLives] vies. */
	Survival("survival"),

	/** Un maximum de problèmes autour de la cote du joueur en [ChallengeRun.kRushDurationMs]. */
	Rush("rush"),
}

/**
 * Raison de la fin d'une partie de défi.
 */
enum class ChallengeEnd {
	/** Survie : plus de vies. */
	NoLives,

	/** Rush : temps écoulé. */
	TimeUp,

	/** Le joueur a abandonné ou quitté l'écran. */
	Abandoned,

	/** Aucun problème disponible. */
	NoPuzzle,
}

/**
 * Déroulement d'une partie de défi (logique pure) : score, vies, difficulté des problèmes et
 * problèmes déjà vus.
 *
 * @property kind défi joué.
 * @property playerRating cote du joueur au début de la partie.
 * @property startedAt début de la partie (millisecondes).
 */
class ChallengeRun(val kind: ChallengeKind, val playerRating: Int, val startedAt: Long) {
	/** Nombre de problèmes réussis. */
	var score = 0
		private set

	/** Vies restantes (Survie). */
	var lives = kLives
		private set

	/** Nombre de problèmes proposés. */
	var served = 0
		private set

	/** Nombre de problèmes manqués ou passés. */
	var misses = 0
		private set

	/** Cote du problème le plus difficile réussi, ou null. */
	var bestPuzzleRating: Int? = null
		private set

	/** Problèmes déjà proposés dans la partie, à ne pas reproposer. */
	val playedIds: Set<String>
		get() = played

	/** Problèmes proposés (modifiable en interne). */
	private val played = mutableSetOf<String>()

	/** Fin de la partie, ou null tant qu'elle continue. */
	var end: ChallengeEnd? = null

	/**
	 * Retourne la cote visée pour le prochain problème : croissante en Survie, celle du joueur en Rush.
	 */
	fun targetRating(): Int = when (kind) {
		ChallengeKind.Survival -> maxOf(kMinRating, playerRating + kSurvivalStartOffset + served * kSurvivalStep)
		ChallengeKind.Rush -> playerRating
	}

	/**
	 * Note qu'un problème est proposé.
	 */
	fun onServed(inPuzzleId: String) {
		played.add(inPuzzleId)
		served++
	}

	/**
	 * Note un problème réussi.
	 */
	fun onSolved(inPuzzleRating: Int) {
		score++
		bestPuzzleRating = maxOf(bestPuzzleRating ?: inPuzzleRating, inPuzzleRating)
	}

	/**
	 * Note un problème manqué ; retourne vrai si la partie est perdue (Survie sans vie).
	 */
	fun onMissed(): Boolean {
		misses++
		if (kind == ChallengeKind.Survival) {
			lives = maxOf(0, lives - 1)
			return lives == 0
		}
		return false
	}

	companion object {
		/** Vies au début d'une partie de Survie. */
		const val kLives = 3

		/** Durée d'une partie de Rush : 3 minutes. */
		const val kRushDurationMs = 3L * 60 * 1000

		/** Écart entre la cote du joueur et le premier problème de Survie. */
		const val kSurvivalStartOffset = -300

		/** Hausse de la cote visée à chaque problème de Survie. */
		const val kSurvivalStep = 25

		/** Cote visée minimale. */
		const val kMinRating = 400
	}
}
