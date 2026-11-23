package com.chessplus.data.user

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.chessplus.core.puzzle.PuzzleResult

/**
 * Tentative sur un problème, une ligne par problème terminé ou abandonné.
 *
 * La cote et les thèmes du problème sont recopiés ici, pour calculer les statistiques sans
 * dépendre de la base des problèmes (qui peut être remplacée).
 *
 * @property id identifiant technique.
 * @property puzzleId identifiant Lichess du problème.
 * @property playedAt date de la tentative (millisecondes depuis l'époque Unix).
 * @property result résultat pris en compte.
 * @property rated vrai si la tentative a modifié la cote du joueur.
 * @property puzzleRating difficulté du problème.
 * @property themesLow bits 0 à 63 des thèmes du problème.
 * @property themesHigh bits 64 à 127 des thèmes du problème.
 * @property mistakeCount nombre de mauvais coups.
 * @property revealedCount nombre de coups de la solution montrés.
 * @property playerRatingBefore cote du joueur avant la tentative.
 * @property playerRatingAfter cote du joueur après la tentative.
 * @property durationMs durée de résolution (du premier coup possible au résultat), ou null si inconnue
 *   (tentatives d'avant la version 2 de la base, problème repris après une interruption).
 * @property failedAtMove numéro du coup du joueur (à partir de 1) où le problème a échoué (erreur ou
 *   indice), ou null s'il est réussi ou si l'information manque.
 * @property playerDeviationAfter écart de cote du joueur après la tentative (Glicko-2), ou null si
 *   la tentative n'est pas notée ou si l'information manque.
 */
@Entity(
	tableName = "attempt",
	indices = [Index("puzzleId"), Index("playedAt")],
)
data class AttemptEntity(
	@PrimaryKey(autoGenerate = true) val id: Long = 0,
	val puzzleId: String,
	val playedAt: Long,
	val result: PuzzleResult,
	val rated: Boolean,
	val puzzleRating: Int,
	val themesLow: Long,
	val themesHigh: Long,
	val mistakeCount: Int,
	val revealedCount: Int,
	val playerRatingBefore: Double,
	val playerRatingAfter: Double,
	val durationMs: Long? = null,
	val failedAtMove: Int? = null,
	val playerDeviationAfter: Double? = null,
)

/**
 * Bilan d'un problème déjà joué, pour la sélection et l'historique.
 *
 * @property puzzleId identifiant Lichess du problème.
 * @property solved vrai si le problème a déjà été réussi : il n'est plus reproposé.
 * @property attemptCount nombre de tentatives.
 * @property failureCount nombre de tentatives échouées.
 * @property lastResult résultat de la dernière tentative.
 * @property lastPlayedAt date de la dernière tentative (millisecondes).
 */
@Entity(
	tableName = "puzzle_state",
	indices = [Index("solved", "lastPlayedAt")],
)
data class PuzzleStateEntity(
	@PrimaryKey val puzzleId: String,
	val solved: Boolean,
	val attemptCount: Int,
	val failureCount: Int,
	val lastResult: PuzzleResult,
	val lastPlayedAt: Long,
)

/**
 * Cote du joueur (une seule ligne, d'identifiant [kSingleId]).
 *
 * @property id toujours [kSingleId].
 * @property rating cote.
 * @property deviation écart de cote (Glicko-2).
 * @property volatility volatilité (Glicko-2).
 * @property updatedAt date de la dernière mise à jour (millisecondes).
 */
@Entity(tableName = "player")
data class PlayerEntity(
	@PrimaryKey val id: Int = kSingleId,
	val rating: Double,
	val deviation: Double,
	val volatility: Double,
	val updatedAt: Long,
) {
	companion object {
		/** Identifiant de l'unique ligne. */
		const val kSingleId = 1
	}
}

/**
 * Problème en cours, enregistré à chaque coup pour une reprise exacte après interruption
 * (une seule ligne, d'identifiant [PlayerEntity.kSingleId]).
 *
 * @property id toujours [PlayerEntity.kSingleId].
 * @property puzzleId identifiant du problème en cours.
 * @property solutionPly nombre de coups de la solution déjà joués.
 * @property alternativeMateUci mat alternatif qui a terminé le problème, ou null.
 * @property mistakeCount nombre de mauvais coups.
 * @property revealedCount nombre de coups de la solution montrés.
 * @property rated vrai si le résultat doit modifier la cote.
 * @property resultRecorded vrai si le résultat a déjà été enregistré (évite un double comptage à la reprise).
 * @property startedAt date de début du problème (millisecondes).
 */
@Entity(tableName = "active_session")
data class ActiveSessionEntity(
	@PrimaryKey val id: Int = PlayerEntity.kSingleId,
	val puzzleId: String,
	val solutionPly: Int,
	val alternativeMateUci: String?,
	val mistakeCount: Int,
	val revealedCount: Int,
	val rated: Boolean,
	val resultRecorded: Boolean,
	val startedAt: Long,
)

/**
 * Point de l'évolution de la cote du joueur.
 *
 * @property playedAt date de la tentative (millisecondes).
 * @property rating cote du joueur après la tentative.
 */
data class RatingPoint(val playedAt: Long, val rating: Double)
