package com.chessplus.app

import com.chessplus.core.model.Theme
import com.chessplus.data.puzzle.PuzzleDatabase
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Test de câblage du module :app vers :data et :core.
 */
class AppSetupTest {
	/** Vérifie que l'application voit :data et :core. */
	@Test
	fun appSeesDataAndCore() {
		assertEquals("puzzles.db", PuzzleDatabase.kFileName)
		assertEquals(73, Theme.entries.size)
	}
}
