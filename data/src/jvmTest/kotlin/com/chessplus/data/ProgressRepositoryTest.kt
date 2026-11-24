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
import com.chessplus.data.user.ReviewItemEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
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

	/** Vérifie la file de révision : entrée sur un échec, paliers 1, 3, 7, 14, 30 jours, retour au début sur un échec, sortie une fois maîtrisé. */
	@Test
	fun reviewSchedule() = runBlocking {
		val vDay = 24L * 60 * 60 * 1000
		val vPuzzle = TestDatabases.puzzle("r1", 1500, ThemeSet.of(Theme.Fork))
		repository.recordResult(vPuzzle, PuzzleResult.Failure, 1, 0, inRated = true, inNow = 10 * vDay)
		assertEquals(11 * vDay, dao.getReviewItem("r1")?.dueAt, "à réviser le lendemain")
		repository.recordResult(vPuzzle, PuzzleResult.Success, 0, 0, inRated = false, inNow = 10 * vDay + 1)
		assertEquals(0, dao.getReviewItem("r1")?.level, "une réussite avant l'échéance ne compte pas")
		assertEquals(listOf("r1"), dao.getDueReviewIds(11 * vDay, 10))
		assertEquals(0, dao.countDueReviews(10 * vDay + 1))
		var vNow = 11 * vDay
		for (vExpected in listOf(7L, 30L)) {
			repository.recordResult(vPuzzle, PuzzleResult.Success, 0, 0, inRated = false, inNow = vNow)
			assertEquals(vNow + vExpected * vDay, dao.getReviewItem("r1")?.dueAt)
			vNow += vExpected * vDay
		}
		repository.recordResult(vPuzzle, PuzzleResult.Failure, 1, 0, inRated = false, inNow = vNow)
		assertEquals(1, dao.getReviewItem("r1")?.level, "un échec fait redescendre d'un palier seulement")
		assertEquals(vNow + vDay, dao.getReviewItem("r1")?.dueAt, "et le puzzle est à revoir le lendemain")
		vNow += vDay
		repository.recordResult(vPuzzle, PuzzleResult.Success, 0, 0, inRated = false, inNow = vNow)
		vNow += 30 * vDay
		repository.recordResult(vPuzzle, PuzzleResult.Success, 0, 0, inRated = false, inNow = vNow)
		assertNull(dao.getReviewItem("r1"), "maîtrisé après trois réussites (le dernier palier)")
	}

	/** Vérifie que les défis n'alimentent pas la file de révision. */
	@Test
	fun challengesAreNotReviewed() = runBlocking<Unit> {
		val vPuzzle = TestDatabases.puzzle("c1", 1500, ThemeSet.of(Theme.Fork))
		repository.recordResult(vPuzzle, PuzzleResult.Failure, 1, 0, inRated = false, inNow = 1_000L, inReviewable = false)
		assertNull(dao.getReviewItem("c1"))
		assertEquals(1, dao.getState("c1")?.failureCount, "le bilan est quand même tenu")
	}

	/** Vérifie l'ordre de la file (les plus avancés d'abord) et son plafond. */
	@Test
	fun reviewQueueOrderAndLimit() = runBlocking<Unit> {
		val vDay = 24L * 60 * 60 * 1000
		dao.upsertReviewItem(ReviewItemEntity("bas", 0, vDay, 0L))
		dao.upsertReviewItem(ReviewItemEntity("haut", 2, 2 * vDay, 0L))
		dao.upsertReviewItem(ReviewItemEntity("ancien", 4, vDay, 0L))
		assertEquals(listOf("ancien", "haut", "bas"), dao.getDueReviewIds(3 * vDay, 10))
		repository.recordResult(TestDatabases.puzzle("ancien", 1500), PuzzleResult.Success, 0, 0, inRated = false, inNow = 3 * vDay)
		assertNull(dao.getReviewItem("ancien"), "un ancien palier 4 est maîtrisé à la réussite suivante")
		for (vIndex in 1..ProgressRepository.kReviewQueueLimit + 5) {
			repository.recordResult(TestDatabases.puzzle("p$vIndex", 1500), PuzzleResult.Failure, 1, 0, inRated = false, inNow = 10 * vDay + vIndex)
		}
		assertEquals(ProgressRepository.kReviewQueueLimit, dao.countReviewItems())
		assertNull(dao.getReviewItem("bas"), "les puzzles ratés depuis le plus longtemps sortent de la file")
		assertNotNull(dao.getReviewItem("haut"), "sauf ceux des paliers avancés, protégés")
		assertNull(dao.getReviewItem("p5"))
		assertNotNull(dao.getReviewItem("p${ProgressRepository.kReviewQueueLimit + 5}"))
	}

	/** Vérifie le comptage de l'objectif quotidien : réussites depuis une date, notées ou non. */
	@Test
	fun dailyGoalCounts() = runBlocking {
		val vPuzzle = TestDatabases.puzzle("p1", 1500, ThemeSet.of(Theme.Fork))
		val vOther = TestDatabases.puzzle("p2", 1500, ThemeSet.of(Theme.Pin))
		repository.recordResult(vPuzzle, PuzzleResult.Success, 0, 0, inRated = true, inNow = 1_000L)
		repository.recordResult(vOther, PuzzleResult.Failure, 1, 0, inRated = true, inNow = 5_000L)
		repository.recordResult(vOther, PuzzleResult.Success, 0, 0, inRated = false, inNow = 6_000L)
		assertEquals(2, dao.countSuccessesSince(0L))
		assertEquals(1, dao.countSuccessesSince(2_000L))
		assertEquals(0, dao.countSuccessesSince(7_000L))
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
