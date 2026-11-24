package com.chessplus.app.game

import com.chessplus.core.model.ThemeSet

/**
 * Difficulté de l'entraînement ciblé.
 */
enum class Difficulty {
	/** De 400 à 100 points sous la cote du joueur. */
	Easier,

	/** Autour de la cote du joueur (tranche élargie si besoin). */
	Level,

	/** De 100 à 400 points au-dessus de la cote du joueur. */
	Harder,

	/** Tranche précise choisie par le joueur. */
	Range,
}

/**
 * Critères de l'entraînement ciblé.
 *
 * @property themes thèmes acceptés (au moins un en commun), vide pour tous.
 * @property difficulty difficulté relative ou tranche précise.
 * @property range tranche précise, utilisée avec [Difficulty.Range].
 */
data class TrainingCriteria(
	val themes: ThemeSet = ThemeSet.kEmpty,
	val difficulty: Difficulty = Difficulty.Level,
	val range: IntRange = kDefaultRange,
) {
	/**
	 * Retourne la tranche de cote imposée pour une cote de joueur, ou null pour suivre la cote.
	 */
	fun ratingRange(inPlayerRating: Int): IntRange? = when (difficulty) {
		Difficulty.Easier -> (inPlayerRating - kOffsetFar)..(inPlayerRating - kOffsetNear)
		Difficulty.Level -> null
		Difficulty.Harder -> (inPlayerRating + kOffsetNear)..(inPlayerRating + kOffsetFar)
		Difficulty.Range -> range
	}

	companion object {
		/** Écart proche pour « plus facile » et « plus difficile ». */
		const val kOffsetNear = 100

		/** Écart lointain pour « plus facile » et « plus difficile ». */
		const val kOffsetFar = 400

		/** Tranche précise par défaut. */
		val kDefaultRange = 1400..1600
	}
}

/**
 * Mode de l'écran de jeu.
 */
sealed interface GameMode {
	/** Entraînement continu, adapté à la cote ; reprend le problème en cours. */
	data object Continuous : GameMode

	/** Entraînement ciblé par thèmes et difficulté. */
	data class Targeted(val criteria: TrainingCriteria) : GameMode

	/** Rejeu d'un problème de l'historique (sans effet sur la cote), puis des autres problèmes non réussis. */
	data class Replay(val puzzleId: String) : GameMode

	/** Puzzle du jour [day] (« yyyyMMdd ») ; les puzzles du jour ne s'enchaînent pas, un calendrier permet d'en choisir un autre. */
	data class Daily(val day: String, val puzzleId: String) : GameMode

	/** Défi Survie ou Rush : score à part, sans effet sur la cote ni sur l'historique. */
	data class Challenge(val kind: ChallengeKind) : GameMode
}
