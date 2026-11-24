package com.chessplus.data.user

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
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
	 * Retourne la dernière tentative sur un problème, ou null.
	 */
	@Query("SELECT * FROM attempt WHERE puzzleId = :inPuzzleId ORDER BY playedAt DESC, id DESC LIMIT 1")
	suspend fun getLatestAttempt(inPuzzleId: String): AttemptEntity?

	/**
	 * Compte les problèmes réussis au moins une fois.
	 */
	@Query("SELECT COUNT(*) FROM puzzle_state WHERE solved = 1")
	fun observeSolvedCount(): Flow<Int>

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
	 * Retourne les identifiants des problèmes jamais réussis, du plus récent au plus ancien.
	 */
	@Query("SELECT puzzleId FROM puzzle_state WHERE solved = 0 ORDER BY lastPlayedAt DESC LIMIT :inLimit")
	suspend fun getUnsolvedIds(inLimit: Int): List<String>

	/**
	 * Observe l'évolution de la cote, dans l'ordre chronologique.
	 */
	@Query("SELECT playedAt, playerRatingAfter AS rating FROM attempt WHERE rated = 1 ORDER BY playedAt, id")
	fun observeRatingHistory(): Flow<List<RatingPoint>>

	/**
	 * Observe toutes les tentatives, dans l'ordre chronologique (statistiques).
	 */
	@Query("SELECT * FROM attempt ORDER BY playedAt, id")
	fun observeAllAttempts(): Flow<List<AttemptEntity>>

	/**
	 * Retourne toutes les tentatives notées, pour les statistiques par thème.
	 */
	@Query("SELECT * FROM attempt WHERE rated = 1")
	suspend fun getRatedAttempts(): List<AttemptEntity>

	/**
	 * Enregistre une partie de défi.
	 */
	@Insert
	suspend fun insertChallengeRun(inRun: ChallengeRunEntity): Long

	/**
	 * Retourne le meilleur score d'un défi, ou null s'il n'a jamais été joué.
	 */
	@Query("SELECT MAX(score) FROM challenge_run WHERE kind = :inKind")
	suspend fun getBestChallengeScore(inKind: String): Int?

	/**
	 * Observe le meilleur score de chaque défi joué.
	 */
	@Query("SELECT kind, MAX(score) AS score FROM challenge_run GROUP BY kind")
	fun observeChallengeBests(): Flow<List<ChallengeBest>>

	/**
	 * Retourne le puzzle du jour attribué à une date, ou null s'il n'a pas encore été tiré.
	 */
	@Query("SELECT * FROM daily_puzzle WHERE day = :inDay")
	suspend fun getDailyPuzzle(inDay: String): DailyPuzzleEntity?

	/**
	 * Enregistre le puzzle du jour d'une date (sans remplacer un tirage existant).
	 */
	@Insert(onConflict = OnConflictStrategy.IGNORE)
	suspend fun insertDailyPuzzle(inDaily: DailyPuzzleEntity)

	/**
	 * Retourne les puzzles déjà attribués à une date, pour ne pas les tirer deux fois.
	 */
	@Query("SELECT puzzleId FROM daily_puzzle")
	suspend fun getDailyPuzzleIds(): List<String>

	/**
	 * Retourne les jours dont le puzzle du jour a été réussi au moins une fois.
	 */
	@Query("SELECT d.day FROM daily_puzzle d INNER JOIN puzzle_state s ON s.puzzleId = d.puzzleId WHERE s.solved = 1")
	suspend fun getSolvedDailyDays(): List<String>

	/**
	 * Observe les jours dont le puzzle du jour a été réussi au moins une fois.
	 */
	@Query("SELECT d.day FROM daily_puzzle d INNER JOIN puzzle_state s ON s.puzzleId = d.puzzleId WHERE s.solved = 1")
	fun observeSolvedDailyDays(): Flow<List<String>>
}
