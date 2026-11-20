package com.chessplus.data

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Test de mise en place du module :data (phase 1).
 */
class DataInfoTest {
	/** Vérifie que :data voit :core. */
	@Test
	fun dataSeesCore() {
		assertEquals("core", DataInfo.coreModuleName())
	}
}
