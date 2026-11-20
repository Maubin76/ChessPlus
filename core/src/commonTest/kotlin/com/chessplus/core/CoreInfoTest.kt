package com.chessplus.core

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Test de mise en place du module :core (phase 1).
 */
class CoreInfoTest {
	/** Vérifie que les tests communs sont exécutés sur chaque cible. */
	@Test
	fun moduleNameIsCore() {
		assertEquals("core", CoreInfo.kModuleName)
	}
}
