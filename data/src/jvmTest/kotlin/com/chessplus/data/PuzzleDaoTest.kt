package com.chessplus.data

import com.chessplus.core.model.Move
import com.chessplus.core.model.Theme
import com.chessplus.core.model.ThemeSet
import com.chessplus.core.puzzle.Puzzle
import com.chessplus.data.puzzle.PuzzleEntity
import kotlinx.coroutines.runBlocking
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Tests de la table des problèmes.
 */
class PuzzleDaoTest {
	/** Base testée. */
	private val database = TestDatabases.puzzles()

	/** DAO testé. */
	private val dao = database.puzzleDao()

	/**
	 * Ferme la base après chaque test.
	 */
	@AfterTest
	fun closeDatabase() {
		database.close()
	}

	/** Vérifie l'aller-retour complet d'un problème réel, thèmes du mot haut compris. */
	@Test
	fun insertAndReadBack() = runBlocking {
		val vPuzzle = Puzzle(
			id = "00sHx",
			fen = "q3k1nr/1pp1nQpp/3p4/1P2p3/4P3/B1PP1b2/B5PP/5K2 b k - 0 17",
			moves = Move.listFromUci("e8d7 a2e6 d7d8 f7f8"),
			rating = 1760,
			themes = ThemeSet.parse("mate mateIn2 middlegame short superGM zugzwang"),
		)
		dao.insertAll(listOf(PuzzleEntity.fromPuzzle(vPuzzle)))
		assertEquals(vPuzzle, dao.getById("00sHx")?.toPuzzle())
		assertNull(dao.getById("absent"))
		assertEquals(1, dao.count())
	}

	/** Vérifie le comptage et l'accès par rang dans une tranche de cote, avec et sans thèmes. */
	@Test
	fun rangeQueries() = runBlocking {
		val vPuzzles = listOf(
			TestDatabases.puzzle("a", 1000, ThemeSet.of(Theme.Fork)),
			TestDatabases.puzzle("b", 1100, ThemeSet.of(Theme.Pin)),
			TestDatabases.puzzle("c", 1200, ThemeSet.of(Theme.Fork, Theme.Zugzwang)),
			TestDatabases.puzzle("d", 1300, ThemeSet.of(Theme.Zugzwang)),
			TestDatabases.puzzle("e", 2000, ThemeSet.of(Theme.Fork)),
		)
		dao.insertAll(vPuzzles.map { PuzzleEntity.fromPuzzle(it) })
		assertEquals(4, dao.countInRange(1000, 1300, 0, 0))
		assertEquals(listOf("a", "b", "c", "d"), (0 until 4).map { dao.getIdAtOffsetInRange(1000, 1300, 0, 0, it) })
		assertNull(dao.getIdAtOffsetInRange(1000, 1300, 0, 0, 4))

		val vFork = ThemeSet.of(Theme.Fork)
		assertEquals(2, dao.countInRange(1000, 1300, vFork.low, vFork.high))
		assertEquals("c", dao.getIdAtOffsetInRange(1000, 1300, vFork.low, vFork.high, 1))

		// Thème du mot haut (bit 72).
		val vZugzwang = ThemeSet.of(Theme.Zugzwang)
		assertEquals(2, dao.countInRange(0, 3000, vZugzwang.low, vZugzwang.high))

		// Plusieurs thèmes : au moins un en commun.
		val vPinOrZugzwang = ThemeSet.of(Theme.Pin, Theme.Zugzwang)
		assertEquals(3, dao.countInRange(0, 3000, vPinOrZugzwang.low, vPinOrZugzwang.high))
	}
}
