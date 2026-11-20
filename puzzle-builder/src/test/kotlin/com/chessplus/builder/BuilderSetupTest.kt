package com.chessplus.builder

import com.chessplus.data.DataInfo
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Test de mise en place du module :puzzle-builder (phase 1).
 */
class BuilderSetupTest {
	/** Vérifie que le builder voit :data et :core. */
	@Test
	fun builderSeesDataAndCore() {
		assertEquals("data", DataInfo.kModuleName)
		assertEquals(73, DataInfo.coreThemeCount())
	}
}
