package com.chessplus.data.puzzle

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

/**
 * Accès à la table des problèmes.
 *
 * Le filtre par thèmes est un masque sur deux mots : un problème est retenu s'il partage au
 * moins un thème avec le masque. Un masque nul (0, 0) désactive le filtre.
 */
@Dao
interface PuzzleDao {
	/**
	 * Insère un lot de problèmes (utilisé par le builder).
	 */
	@Insert
	suspend fun insertAll(inPuzzles: List<PuzzleEntity>)

	/**
	 * Retourne le problème d'identifiant donné, ou null.
	 */
	@Query("SELECT * FROM puzzle WHERE id = :inId")
	suspend fun getById(inId: String): PuzzleEntity?

	/**
	 * Compte les problèmes.
	 */
	@Query("SELECT COUNT(*) FROM puzzle")
	suspend fun count(): Int

	/**
	 * Compte les problèmes d'une tranche de cote, filtrés par thèmes.
	 */
	@Query(
		"""
		SELECT COUNT(*) FROM puzzle
		WHERE rating BETWEEN :inMinRating AND :inMaxRating
		AND ((:inThemesLow = 0 AND :inThemesHigh = 0) OR (themesLow & :inThemesLow) != 0 OR (themesHigh & :inThemesHigh) != 0)
		"""
	)
	suspend fun countInRange(inMinRating: Int, inMaxRating: Int, inThemesLow: Long, inThemesHigh: Long): Int

	/**
	 * Retourne l'identifiant du problème de rang [inOffset] (à partir de 0) parmi ceux d'une
	 * tranche de cote filtrés par thèmes, triés par cote. Associé à un rang aléatoire, cela évite
	 * un `ORDER BY RANDOM()` sur toute la table. Seul l'identifiant est lu, pour rester dans
	 * l'index de sélection.
	 */
	@Query(
		"""
		SELECT id FROM puzzle
		WHERE rating BETWEEN :inMinRating AND :inMaxRating
		AND ((:inThemesLow = 0 AND :inThemesHigh = 0) OR (themesLow & :inThemesLow) != 0 OR (themesHigh & :inThemesHigh) != 0)
		ORDER BY rating
		LIMIT 1 OFFSET :inOffset
		"""
	)
	suspend fun getIdAtOffsetInRange(inMinRating: Int, inMaxRating: Int, inThemesLow: Long, inThemesHigh: Long, inOffset: Int): String?
}
