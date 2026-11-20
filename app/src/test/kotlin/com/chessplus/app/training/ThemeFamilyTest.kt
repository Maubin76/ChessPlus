package com.chessplus.app.training

import com.chessplus.app.ui.ThemeFamily
import com.chessplus.core.model.Theme
import com.chessplus.core.model.ThemeSet
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests de la sélection d'une famille entière de thèmes.
 */
class ThemeFamilyTest {
	/** Vérifie l'ajout puis le retrait de toute une famille, sans toucher aux autres thèmes. */
	@Test
	fun toggleWholeFamily() {
		val vEndgames = familyThemes(ThemeFamily.Endgames)
		assertEquals(6, vEndgames.size)
		val vStart = ThemeSet.of(Theme.Fork, Theme.RookEndgame)
		val vAll = vStart.toggledFamily(ThemeFamily.Endgames)
		assertTrue(vEndgames.all { it in vAll })
		assertTrue(Theme.Fork in vAll)
		val vNone = vAll.toggledFamily(ThemeFamily.Endgames)
		assertEquals(ThemeSet.of(Theme.Fork), vNone)
	}

	/** Vérifie que chaque thème appartient à exactement une famille. */
	@Test
	fun everyThemeHasOneFamily() {
		assertEquals(Theme.entries.size, ThemeFamily.entries.sumOf { familyThemes(it).size })
	}
}
