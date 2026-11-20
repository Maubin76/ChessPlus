package com.chessplus.data

import androidx.room.immediateTransaction
import androidx.room.useWriterConnection
import com.chessplus.core.model.Theme
import com.chessplus.core.model.ThemeSet
import com.chessplus.core.puzzle.PuzzleResult
import com.chessplus.core.rating.Glicko2RatingSystem
import com.chessplus.data.user.ActiveSessionEntity
import com.chessplus.data.user.AttemptEntity
import com.chessplus.data.user.ProgressRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests de l'enregistrement des résultats dans la base utilisateur.
 */
class ProgressRepositoryTest {
	/** Base testée. */
	private val database = TestDatabases.user()

	/** DAO de la base testée. */
	private val dao = database.userDao()

	/** Algorithme de cote. */
	private val ratingSystem = Glicko2RatingSystem()

	/** Dépôt testé. */
	private val repository = ProgressRepository(database, ratingSystem)

	/**
	 * Ferme la base après chaque test.
	 */
	@AfterTest
	fun closeDatabase() {
		database.close()
	}

	/** Vérifie la cote initiale puis l'enregistrement d'une réussite et d'un échec. */
	@Test
	fun recordResults() = runBlocking {
		assertEquals(ratingSystem.initialRating, repository.currentRating())
		val vPuzzle = TestDatabases.puzzle("p1", 1500, ThemeSet.of(Theme.Fork, Theme.Zugzwang))

		val vAfterFailure = repository.recordResult(vPuzzle, PuzzleResult.Failure, 1, 0, inRated = true, inNow = 1_000L)
		assertTrue(vAfterFailure.rating < ratingSystem.initialRating.rating)
		val vState = dao.getState("p1")!!
		assertEquals(false, vState.solved)
		assertEquals(1, vState.failureCount)

		val vAfterSuccess = repository.recordResult(vPuzzle, PuzzleResult.Success, 0, 0, inRated = true, inNow = 2_000L)
		assertTrue(vAfterSuccess.rating > vAfterFailure.rating)
		assertEquals(vAfterSuccess, repository.currentRating())
		val vSolved = dao.getState("p1")!!
		assertEquals(true, vSolved.solved)
		assertEquals(2, vSolved.attemptCount)
		assertEquals(1, vSolved.failureCount)
		assertEquals(PuzzleResult.Success, vSolved.lastResult)

		val vAttempts = dao.observeRecentAttempts(10).first()
		assertEquals(listOf(2_000L, 1_000L), vAttempts.map { it.playedAt })
		assertEquals(ThemeSet.of(Theme.Fork, Theme.Zugzwang), ThemeSet(vAttempts[0].themesLow, vAttempts[0].themesHigh))
		assertEquals(listOf(vAfterFailure.rating, vAfterSuccess.rating), dao.observeRatingHistory().first().map { it.rating })
	}

	/** Vérifie qu'une tentative non notée ne change pas la cote mais reste dans l'historique. */
	@Test
	fun unratedAttemptKeepsRating() = runBlocking {
		val vBefore = repository.currentRating()
		repository.recordResult(TestDatabases.puzzle("p2", 1800), PuzzleResult.Success, 0, 0, inRated = false, inNow = 5L)
		assertEquals(vBefore, repository.currentRating())
		assertNull(dao.getPlayer())
		assertEquals(1, dao.observeRecentAttempts(10).first().size)
		assertTrue(dao.observeRatingHistory().first().isEmpty())
		assertTrue(dao.getRatedAttempts().isEmpty())
	}

	/** Vérifie la liste des problèmes jamais réussis. */
	@Test
	fun unsolvedList() = runBlocking {
		repository.recordResult(TestDatabases.puzzle("ok", 1500), PuzzleResult.Success, 0, 0, true, 1L)
		repository.recordResult(TestDatabases.puzzle("ko1", 1500), PuzzleResult.Failure, 1, 0, true, 2L)
		repository.recordResult(TestDatabases.puzzle("ko2", 1500), PuzzleResult.Failure, 0, 1, true, 3L)
		assertEquals(listOf("ko2", "ko1"), dao.observeUnsolved(10).first().map { it.puzzleId })
	}

	/** Vérifie que les appels DAO faits dans la transaction d'écriture sont annulés en cas d'erreur. */
	@Test
	fun transactionRollsBackDaoCalls() = runBlocking {
		val vAttempt = AttemptEntity(
			puzzleId = "x", playedAt = 1L, result = PuzzleResult.Success, rated = true, puzzleRating = 1500,
			themesLow = 0L, themesHigh = 0L, mistakeCount = 0, revealedCount = 0, playerRatingBefore = 1500.0, playerRatingAfter = 1510.0,
		)
		assertFailsWith<IllegalStateException> {
			database.useWriterConnection { inTransactor ->
				inTransactor.immediateTransaction {
					dao.insertAttempt(vAttempt)
					error("échec simulé")
				}
			}
		}
		assertTrue(dao.observeRecentAttempts(10).first().isEmpty())
	}

	/** Vérifie l'enregistrement et l'effacement du problème en cours. */
	@Test
	fun activeSession() = runBlocking {
		assertNull(dao.getActiveSession())
		val vSession = ActiveSessionEntity(puzzleId = "p", solutionPly = 3, alternativeMateUci = null, mistakeCount = 1, revealedCount = 0, rated = true, resultRecorded = true, startedAt = 9L)
		dao.upsertActiveSession(vSession)
		assertEquals(vSession, dao.getActiveSession())
		dao.upsertActiveSession(vSession.copy(solutionPly = 4))
		assertEquals(4, dao.getActiveSession()!!.solutionPly)
		dao.clearActiveSession()
		assertNull(dao.getActiveSession())
	}
}
