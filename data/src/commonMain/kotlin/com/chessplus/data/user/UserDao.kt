package com.chessplus.data.user

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/**
 * Accès aux tables de la base utilisateur.
 */
@Dao
interface UserDao {
	/**
	 * Enregistre une tentative et retourne son identifiant.
	 */
	@Insert
	suspend fun insertAttempt(inAttempt: AttemptEntity): Long

	/**
	 * Retourne le bilan d'un problème, ou null s'il n'a jamais été joué.
	 */
	@Query("SELECT * FROM puzzle_state WHERE puzzleId = :inPuzzleId")
	suspend fun getState(inPuzzleId: String): PuzzleStateEntity?

	/**
	 * Crée ou remplace le bilan d'un problème.
	 */
	@Upsert
	suspend fun upsertState(inState: PuzzleStateEntity)

	/**
	 * Retourne la cote du joueur, ou null avant le premier problème.
	 */
	@Query("SELECT * FROM player WHERE id = 1")
	suspend fun getPlayer(): PlayerEntity?

	/**
	 * Observe la cote du joueur.
	 */
	@Query("SELECT * FROM player WHERE id = 1")
	fun observePlayer(): Flow<PlayerEntity?>

	/**
	 * Crée ou remplace la cote du joueur.
	 */
	@Upsert
	suspend fun upsertPlayer(inPlayer: PlayerEntity)

	/**
	 * Retourne le problème en cours, ou null.
	 */
	@Query("SELECT * FROM active_session WHERE id = 1")
	suspend fun getActiveSession(): ActiveSessionEntity?

	/**
	 * Enregistre le problème en cours.
	 */
	@Upsert
	suspend fun upsertActiveSession(inSession: ActiveSessionEntity)

	/**
	 * Efface le problème en cours.
	 */
	@Query("DELETE FROM active_session")
	suspend fun clearActiveSession()

	/**
	 * Observe les dernières tentatives, de la plus récente à la plus ancienne.
	 */
	@Query("SELECT * FROM attempt ORDER BY playedAt DESC, id DESC LIMIT :inLimit")
	fun observeRecentAttempts(inLimit: Int): Flow<List<AttemptEntity>>

	/**
	 * Observe les problèmes jamais réussis, du plus récent au plus ancien, pour les rejouer.
	 */
	@Query("SELECT * FROM puzzle_state WHERE solved = 0 ORDER BY lastPlayedAt DESC LIMIT :inLimit")
	fun observeUnsolved(inLimit: Int): Flow<List<PuzzleStateEntity>>

	/**
	 * Observe l'évolution de la cote, dans l'ordre chronologique.
	 */
	@Query("SELECT playedAt, playerRatingAfter AS rating FROM attempt WHERE rated = 1 ORDER BY playedAt, id")
	fun observeRatingHistory(): Flow<List<RatingPoint>>

	/**
	 * Retourne toutes les tentatives notées, pour les statistiques par thème.
	 */
	@Query("SELECT * FROM attempt WHERE rated = 1")
	suspend fun getRatedAttempts(): List<AttemptEntity>
}
