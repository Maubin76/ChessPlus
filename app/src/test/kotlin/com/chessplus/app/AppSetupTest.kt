package com.chessplus.app

import com.chessplus.data.DataInfo
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Test de mise en place du module :app (phase 1).
 */
class AppSetupTest {
	/** Vérifie que l'application voit :data et :core. */
	@Test
	fun appSeesDataAndCore() {
		assertEquals("data", DataInfo.kModuleName)
		assertEquals("core", DataInfo.coreModuleName())
	}
}
