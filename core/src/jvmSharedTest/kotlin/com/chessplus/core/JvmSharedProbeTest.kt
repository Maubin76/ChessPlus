package com.chessplus.core

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Test de mise en place du source set jvmShared (phase 1).
 */
class JvmSharedProbeTest {
	/** Vérifie que chesslib est accessible depuis jvmShared sur Android et sur la JVM. */
	@Test
	fun chesslibGivesStartingPosition() {
		assertEquals("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1", JvmSharedProbe.startingFen())
	}
}
