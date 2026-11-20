package com.chessplus.builder

import androidx.room.useReaderConnection
import com.chessplus.core.model.Move
import com.chessplus.core.puzzle.LichessCsv
import com.chessplus.core.rules.defaultChessRules
import com.chessplus.data.createPuzzleDatabase
import com.github.luben.zstd.ZstdOutputStream
import kotlinx.coroutines.runBlocking
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests de bout en bout du builder sur un petit fichier `.csv.zst` généré.
 */
class PuzzleImporterTest {
	/** Répertoire temporaire du test. */
	private val directory: File = Files.createTempDirectory("chessplus-builder").toFile()

	/** Lignes valides de l'extrait de la spécification. */
	private val kSampleLines = listOf(
		"00sHx,q3k1nr/1pp1nQpp/3p4/1P2p3/4P3/B1PP1b2/B5PP/5K2 b k - 0 17,e8d7 a2e6 d7d8 f7f8,1760,80,83,72,mate mateIn2 middlegame short,https://lichess.org/yyznGmXs/black#34,Italian_Game Italian_Game_Classical_Variation,",
		"00sJ9,r3r1k1/p4ppp/2p2n2/1p6/3P1qb1/2NQR3/PPB2PP1/R1B3K1 w - - 5 18,e3g3 e8e1 g1h2 e1c1 a1c1 f4h6 h2g1 h6c1,2671,105,87,325,advantage attraction fork middlegame sacrifice veryLong,https://lichess.org/gyFeQsOE#35,French_Defense French_Defense_Exchange_Variation,1607774862751",
		"00sJb,Q1b2r1k/p2np2p/5bp1/q7/5P2/4B3/PPP3PP/2KR1B1R w - - 1 17,d1d7 a5e1 d7d1 e1e3 c1b1 e3b6,2235,76,97,64,advantage fork long,https://lichess.org/kiuvTFoE#33,Sicilian_Defense Sicilian_Defense_Dragon_Variation,",
		"00sO1,1k1r4/pp3pp1/2p1p3/4b3/P3n1P1/8/KPP2PN1/3rBR1R b - - 2 31,b8c7 e1a5 b7b6 f1d1,998,85,94,293,advantage discoveredAttack master middlegame short,https://lichess.org/vsfFkG0s/black#62,,",
	)

	/** Ligne au coup illégal (le premier coup déplace une case vide). */
	private val kInvalidLine = "bad01,q3k1nr/1pp1nQpp/3p4/1P2p3/4P3/B1PP1b2/B5PP/5K2 b k - 0 17,a1a2 a2e6,1500,70,90,500,mate,https://lichess.org/x,,"

	/**
	 * Supprime le répertoire temporaire.
	 */
	@AfterTest
	fun cleanUp() {
		directory.deleteRecursively()
	}

	/**
	 * Écrit un fichier `.csv.zst` avec l'en-tête Lichess et les lignes données.
	 */
	private fun writeCsv(inLines: List<String>, inHeader: String = LichessCsv.kHeader): File {
		val vFile = File(directory, "puzzles.csv.zst")
		ZstdOutputStream(vFile.outputStream()).bufferedWriter().use { inWriter ->
			inWriter.write(inHeader)
			inWriter.write("\n")
			inLines.forEach { inWriter.write(it); inWriter.write("\n") }
		}
		return vFile
	}

	/**
	 * Retourne des options qui acceptent tout, avec des fichiers dans le répertoire temporaire.
	 */
	private fun options(inSource: File) = BuilderOptions(
		source = inSource,
		maxRatingDeviation = 1000,
		minPopularity = -100,
		minPlays = 0,
		maxPerBand = 0,
		output = File(directory, "out/puzzles.db"),
		workFile = File(directory, "work/puzzles.db"),
		batchSize = 2,
		dropIndexes = false,
	)

	/**
	 * Lit les noms des index de la table des problèmes dans la base produite.
	 */
	private fun indexNames(inFile: File): List<String> = runBlocking {
		val vDatabase = createPuzzleDatabase(inFile)
		try {
			vDatabase.useReaderConnection { inConnection ->
				inConnection.usePrepared("SELECT name FROM sqlite_master WHERE type = 'index' AND tbl_name = 'puzzle' AND sql IS NOT NULL") { inStatement ->
					buildList { while (inStatement.step()) add(inStatement.getText(0)) }
				}
			}
		} finally {
			vDatabase.close()
		}
	}

	/** Vérifie la génération complète : problèmes valides insérés, invalide écarté, base lisible par Room. */
	@Test
	fun buildsDatabase() = runBlocking {
		val vOptions = options(writeCsv(kSampleLines + kInvalidLine))
		val vReport = PuzzleImporter(vOptions, defaultChessRules()).run()
		assertEquals(5, vReport.rowCount)
		assertEquals(4, vReport.retainedCount)
		assertEquals(1, vReport.rejections[RejectReason.Invalid])
		assertTrue(vReport.invalidExamples.single().startsWith("bad01"))
		assertTrue(vOptions.output.isFile)

		val vDatabase = createPuzzleDatabase(vOptions.output)
		try {
			val vDao = vDatabase.puzzleDao()
			assertEquals(4, vDao.count())
			val vPuzzle = vDao.getById("00sHx")!!.toPuzzle()
			assertEquals(Move.listFromUci("e8d7 a2e6 d7d8 f7f8"), vPuzzle.moves)
			assertEquals("mate mateIn2 middlegame short", vPuzzle.themes.toKeys())
			assertNull(vDao.getById("bad01"))
		} finally {
			vDatabase.close()
		}
		assertEquals(listOf("index_puzzle_selection"), indexNames(vOptions.output))
		assertTrue(vReport.format(vOptions).contains("Problèmes retenus : 4"))
	}

	/** Vérifie que les index supprimés pendant l'insertion sont recréés à l'identique. */
	@Test
	fun dropAndRecreateIndexes() = runBlocking {
		val vOptions = options(writeCsv(kSampleLines)).copy(dropIndexes = true)
		PuzzleImporter(vOptions, defaultChessRules()).run()
		assertEquals(listOf("index_puzzle_selection"), indexNames(vOptions.output))
	}

	/** Vérifie les seuils et le plafond par tranche dans la génération. */
	@Test
	fun appliesThresholdsAndCap() = runBlocking {
		// RD <= 90 écarte 00sJ9 (105) ; plafond 1 par tranche : 00sHx (1600-1799), 00sJb (2200-2399), 00sO1 (800-999).
		val vOptions = options(writeCsv(kSampleLines)).copy(maxRatingDeviation = 90, maxPerBand = 1)
		val vReport = PuzzleImporter(vOptions, defaultChessRules()).run()
		assertEquals(3, vReport.retainedCount)
		assertEquals(1, vReport.rejections[RejectReason.RatingDeviation])
		assertEquals(0, vReport.rejections[RejectReason.BandCap])
	}

	/** Vérifie qu'un thème inconnu fait échouer la génération, même sur une ligne rejetée par les seuils. */
	@Test
	fun unknownThemeFails() {
		val vLine = kSampleLines[0].replace("mate mateIn2", "mate brandNewTheme")
		val vOptions = options(writeCsv(listOf(vLine))).copy(minPlays = 1_000_000)
		val vError = assertFailsWith<IllegalStateException> { runBlocking { PuzzleImporter(vOptions, defaultChessRules()).run() } }
		assertTrue(vError.message!!.contains("brandNewTheme"))
		assertTrue(vError.message!!.contains("Ligne 2"))
	}

	/** Vérifie le rejet d'une ligne mal formée et d'un en-tête inattendu. */
	@Test
	fun malformedInputFails() {
		val vBadLine = options(writeCsv(listOf(kSampleLines[3].dropLast(1))))
		assertFailsWith<IllegalStateException> { runBlocking { PuzzleImporter(vBadLine, defaultChessRules()).run() } }
		val vBadHeader = options(writeCsv(kSampleLines, inHeader = "PuzzleId,FEN"))
		assertFailsWith<IllegalArgumentException> { runBlocking { PuzzleImporter(vBadHeader, defaultChessRules()).run() } }
	}
}
