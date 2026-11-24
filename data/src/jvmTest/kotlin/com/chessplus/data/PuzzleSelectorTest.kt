package com.chessplus.data

import com.chessplus.core.model.Theme
import com.chessplus.core.model.ThemeSet
import com.chessplus.core.puzzle.PuzzleResult
import com.chessplus.core.rating.EloRatingSystem
import com.chessplus.data.puzzle.PuzzleEntity
import com.chessplus.data.selection.PuzzleSelector
import com.chessplus.data.selection.SelectionCriteria
import com.chessplus.data.user.ProgressRepository
import kotlinx.coroutines.runBlocking
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests du choix du prochain problème.
 */
class PuzzleSelectorTest {
	/** Base des problèmes. */
	private val puzzles = TestDatabases.puzzles()

	/** Base utilisateur. */
	private val user = TestDatabases.user()

	/** Dépôt de progression, pour marquer des problèmes comme joués. */
	private val progress = ProgressRepository(user, EloRatingSystem())

	/** Sélecteur testé, avec un générateur déterministe. */
	private val selector = PuzzleSelector(puzzles.puzzleDao(), user.userDao(), Random(42))

	/**
	 * Ferme les bases après chaque test.
	 */
	@AfterTest
	fun closeDatabases() {
		puzzles.close()
		user.close()
	}

	/**
	 * Insère des problèmes factices de cotes données.
	 */
	private suspend fun insert(vararg inPuzzles: Pair<String, Int>) {
		puzzles.puzzleDao().insertAll(inPuzzles.map { PuzzleEntity.fromPuzzle(TestDatabases.puzzle(it.first, it.second)) })
	}

	/** Vérifie que le problème choisi est dans la tranche autour du joueur quand elle en contient. */
	@Test
	fun picksAroundPlayerRating() = runBlocking {
		insert("low" to 800, "mid1" to 1480, "mid2" to 1520, "high" to 2200)
		repeat(20) {
			val vPuzzle = selector.nextPuzzle(1500.0, SelectionCriteria(), null, 0L)
			assertTrue(vPuzzle!!.id in setOf("mid1", "mid2"))
		}
	}

	/** Vérifie l'élargissement progressif de la tranche, jusqu'à la limite. */
	@Test
	fun widensRangeWhenEmpty() = runBlocking {
		insert("far" to 1850, "tooFar" to 2200)
		assertEquals("far", selector.nextPuzzle(1500.0, SelectionCriteria(), null, 0L)?.id)
		assertEquals(listOf(1400..1600, 1300..1700, 1200..1800, 1100..1900, 1000..2000, 900..2100), selector.candidateRanges(1500, null))
		assertEquals(listOf(475..525, 375..625, 275..725, 175..825, 75..925, -25..1025), selector.candidateRanges(500, null, 25), "Survie : fenêtre resserrée")
	}

	/** Vérifie qu'un problème réussi n'est jamais reproposé et qu'un échoué revient après le délai. */
	@Test
	fun solvedAndFailedPuzzles() = runBlocking {
		insert("solved" to 1500, "failed" to 1500)
		progress.recordResult(TestDatabases.puzzle("solved", 1500), PuzzleResult.Success, 0, 0, true, 0L)
		progress.recordResult(TestDatabases.puzzle("failed", 1500), PuzzleResult.Failure, 1, 0, true, 0L)
		assertNull(selector.nextPuzzle(1500.0, SelectionCriteria(), null, 1_000L))
		val vLater = PuzzleSelector.kDefaultRetryDelayMs
		repeat(10) { assertEquals("failed", selector.nextPuzzle(1500.0, SelectionCriteria(), null, vLater)?.id) }
	}

	/** Vérifie l'exclusion du problème qui vient d'être joué. */
	@Test
	fun excludesCurrentPuzzle() = runBlocking {
		insert("a" to 1500, "b" to 1510)
		repeat(10) { assertEquals("b", selector.nextPuzzle(1500.0, SelectionCriteria(), "a", 0L)?.id) }
	}

	/** Vérifie l'exclusion des problèmes déjà vus dans une partie de défi. */
	@Test
	fun excludesSeenPuzzles() = runBlocking {
		insert("a" to 1500, "b" to 1510, "c" to 1490)
		repeat(10) { assertEquals("c", selector.nextPuzzle(1500.0, SelectionCriteria(), null, 0L, setOf("a", "b"))?.id) }
		assertNull(selector.nextPuzzle(1500.0, SelectionCriteria(), "c", 0L, setOf("a", "b")))
	}

	/** Vérifie le filtre par thème et par tranche imposée. */
	@Test
	fun themeAndImposedRange() = runBlocking {
		puzzles.puzzleDao().insertAll(
			listOf(
				TestDatabases.puzzle("fork", 1500, ThemeSet.of(Theme.Fork)),
				TestDatabases.puzzle("pin", 1500, ThemeSet.of(Theme.Pin)),
				TestDatabases.puzzle("hard", 2500, ThemeSet.of(Theme.Fork)),
			).map { PuzzleEntity.fromPuzzle(it) }
		)
		repeat(10) { assertEquals("fork", selector.nextPuzzle(1500.0, SelectionCriteria(themes = ThemeSet.of(Theme.Fork)), null, 0L)?.id) }
		assertEquals("hard", selector.nextPuzzle(1500.0, SelectionCriteria(ratingRange = 2400..2600), null, 0L)?.id)
		assertNull(selector.nextPuzzle(1500.0, SelectionCriteria(ratingRange = 3000..3200), null, 0L))
	}

	/** Vérifie que le tirage couvre tous les problèmes de la tranche. */
	@Test
	fun selectionCoversRange() = runBlocking {
		insert(*Array(10) { "p$it" to 1450 + it * 10 })
		val vSeen = mutableSetOf<String>()
		repeat(200) { vSeen.add(assertNotNull(selector.nextPuzzle(1500.0, SelectionCriteria(), null, 0L)).id) }
		assertEquals(10, vSeen.size)
	}
}
