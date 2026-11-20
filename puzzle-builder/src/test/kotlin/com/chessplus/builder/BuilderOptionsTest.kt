package com.chessplus.builder

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Tests de la lecture des arguments du builder.
 */
class BuilderOptionsTest {
	/** Vérifie les valeurs par défaut. */
	@Test
	fun defaults() {
		val vOptions = BuilderOptions.parse(arrayOf("puzzles.csv.zst"))
		assertEquals(File("puzzles.csv.zst"), vOptions.source)
		assertEquals(BuilderOptions.kDefaultMaxRatingDeviation, vOptions.maxRatingDeviation)
		assertEquals(File("app/src/main/assets/puzzles.db"), vOptions.output)
		assertTrue(vOptions.dropIndexes)
		assertTrue(vOptions.validate)
	}

	/** Vérifie la lecture de toutes les options. */
	@Test
	fun allOptions() {
		val vOptions = BuilderOptions.parse(
			arrayOf(
				"--max-rd=75", "x.csv.zst", "--min-popularity=-20", "--min-plays=1000", "--max-per-band=0",
				"--output=out.db", "--work-file=work.db", "--batch-size=500", "--keep-indexes", "--no-validate",
			)
		)
		assertEquals(BuilderOptions(File("x.csv.zst"), 75, -20, 1000, 0, File("out.db"), File("work.db"), 500, dropIndexes = false, validate = false), vOptions)
	}

	/** Vérifie le rejet des arguments invalides. */
	@Test
	fun invalidArguments() {
		assertFailsWith<IllegalArgumentException> { BuilderOptions.parse(emptyArray()) }
		assertFailsWith<IllegalArgumentException> { BuilderOptions.parse(arrayOf("a.zst", "b.zst")) }
		assertFailsWith<IllegalArgumentException> { BuilderOptions.parse(arrayOf("a.zst", "--unknown=1")) }
		assertFailsWith<IllegalArgumentException> { BuilderOptions.parse(arrayOf("a.zst", "--max-rd=abc")) }
		assertFailsWith<IllegalArgumentException> { BuilderOptions.parse(arrayOf("a.zst", "--max-rd")) }
		assertFailsWith<IllegalArgumentException> { BuilderOptions.parse(arrayOf("a.zst", "--min-popularity=101")) }
		assertFailsWith<IllegalArgumentException> { BuilderOptions.parse(arrayOf("a.zst", "--keep-indexes=yes")) }
		assertFailsWith<IllegalArgumentException> { BuilderOptions.parse(arrayOf("a.zst", "--output=same.db", "--work-file=same.db")) }
	}
}
