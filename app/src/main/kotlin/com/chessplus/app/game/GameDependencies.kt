package com.chessplus.app.game

import com.chessplus.core.puzzle.Puzzle
import com.chessplus.core.puzzle.PuzzleResult
import com.chessplus.core.puzzle.PuzzleSnapshot
import com.chessplus.core.rating.PlayerRating

/**
 * Source des problèmes pour l'écran de jeu.
 */
interface PuzzleProvider {
	/**
	 * Retourne le problème d'identifiant donné, ou null s'il n'existe pas (base remplacée).
	 */
	suspend fun byId(inId: String): Puzzle?

	/**
	 * Retourne le prochain problème du mode donné, ou null si aucun ne convient.
	 */
	suspend fun next(inRating: Double, inExcludeId: String?, inMode: GameMode): Puzzle?

	/**
	 * Indique si le problème n'a jamais été joué (seul un premier essai modifie la cote).
	 */
	suspend fun isNew(inId: String): Boolean

	/**
	 * Retourne un problème de défi autour de la cote [inCenter] (première tranche de demi-largeur
	 * [inHalfWidth], élargie s'il le faut), hors des problèmes [inExclude] déjà vus dans la partie,
	 * ou null si aucun ne convient. Si [inIncludeSeen], les problèmes déjà joués hors de la partie
	 * peuvent revenir.
	 */
	suspend fun challengeNext(inCenter: Int, inHalfWidth: Int, inIncludeSeen: Boolean, inExclude: Set<String>): Puzzle?
}

/**
 * Réglages qui modifient le déroulement d'un puzzle, lus à chaque usage.
 */
interface GamePreferences {
	/** Vrai si un coup doit être confirmé avant d'être joué. */
	val confirmMoves: Boolean

	/** Vrai pour passer au puzzle suivant après une réussite. */
	val autoNext: Boolean

	/** Écart entre la difficulté visée en Arcade et le classement du joueur. */
	val difficultyOffset: Int

	companion object {
		/** Réglages neutres : ni confirmation, ni enchaînement, difficulté normale. */
		val kDefault = object : GamePreferences {
			override val confirmMoves = false
			override val autoNext = false
			override val difficultyOffset = 0
		}
	}
}

/**
 * Puzzles du jour : un puzzle attribué à chaque date, et les jours déjà résolus.
 */
interface DailyStore {
	/**
	 * Retourne le puzzle du jour [inDay] (« yyyyMMdd »), tiré et conservé à la première demande.
	 */
	suspend fun puzzleFor(inDay: String): Puzzle?

	/**
	 * Retourne les jours dont le puzzle a été réussi au moins une fois.
	 */
	suspend fun solvedDays(): Set<String>

	companion object {
		/** Aucun puzzle du jour (tests des autres modes). */
		val kNone = object : DailyStore {
			override suspend fun puzzleFor(inDay: String): Puzzle? = null
			override suspend fun solvedDays(): Set<String> = emptySet()
		}
	}
}

/**
 * Enregistrement des parties de défi.
 */
interface ChallengeStore {
	/**
	 * Retourne le meilleur score du défi, ou null s'il n'a jamais été joué.
	 */
	suspend fun best(inKind: ChallengeKind): Int?

	/**
	 * Enregistre une partie terminée.
	 */
	suspend fun save(inKind: ChallengeKind, inScore: Int, inBestPuzzleRating: Int?, inDurationMs: Long)
}

/**
 * Résultat enregistré d'un problème : cote du joueur avant et après.
 *
 * @property result résultat pris en compte.
 * @property ratingBefore cote avant le problème.
 * @property ratingAfter cote après le problème (égale à la précédente si non noté).
 */
data class RecordedResult(val result: PuzzleResult, val ratingBefore: Double, val ratingAfter: Double)

/**
 * Enregistrement de la progression du joueur.
 */
interface ProgressStore {
	/**
	 * Retourne la cote actuelle du joueur.
	 */
	suspend fun currentRating(): PlayerRating

	/**
	 * Enregistre le résultat d'un problème et retourne les cotes avant et après. [inDurationMs] et
	 * [inFailedAtMove] alimentent les statistiques (null si inconnus) ; [inReviewable] est faux pour
	 * les défis, qui n'alimentent pas la file de révision.
	 */
	suspend fun record(
		inPuzzle: Puzzle,
		inResult: PuzzleResult,
		inMistakes: Int,
		inRevealed: Int,
		inRated: Boolean,
		inDurationMs: Long?,
		inFailedAtMove: Int?,
		inReviewable: Boolean = true,
	): RecordedResult

	/**
	 * Retourne le dernier résultat enregistré pour un problème, ou null.
	 */
	suspend fun lastResult(inPuzzleId: String): RecordedResult?
}

/**
 * Problème en cours tel qu'il est sauvegardé pour la reprise.
 *
 * @property snapshot état de la session de problème.
 * @property rated vrai si le résultat modifie la cote.
 * @property resultRecorded vrai si le résultat est déjà enregistré.
 * @property startedAt date de début (millisecondes).
 */
data class SavedSession(
	val snapshot: PuzzleSnapshot,
	val rated: Boolean,
	val resultRecorded: Boolean,
	val startedAt: Long,
)

/**
 * Sauvegarde du problème en cours.
 */
interface SessionStore {
	/**
	 * Retourne le problème en cours sauvegardé, ou null.
	 */
	suspend fun load(): SavedSession?

	/**
	 * Sauvegarde le problème en cours.
	 */
	suspend fun save(inSession: SavedSession)

	/**
	 * Efface le problème en cours.
	 */
	suspend fun clear()
}
