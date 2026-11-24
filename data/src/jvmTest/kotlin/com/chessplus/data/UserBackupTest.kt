package com.chessplus.data

import com.chessplus.core.model.Theme
import com.chessplus.core.model.ThemeSet
import com.chessplus.core.puzzle.PuzzleResult
import com.chessplus.core.rating.Glicko2RatingSystem
import com.chessplus.data.user.ProgressRepository
import com.chessplus.data.user.UserBackup
import com.chessplus.data.user.UserDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Tests de l'export et de la vérification d'une sauvegarde de la progression.
 */
class UserBackupTest {
	/** Vérifie qu'une base exportée est valide et contient la progression, et qu'un autre fichier est refusé. */
	@Test
	fun exportAndValidate() = runBlocking<Unit> {
		val vDirectory = Files.createTempDirectory("chessplus-backup").toFile()
		try {
			val vSource = createUserDatabase(File(vDirectory, "user.db"))
			val vRepository = ProgressRepository(vSource, Glicko2RatingSystem())
			vRepository.recordResult(TestDatabases.puzzle("p1", 1500, ThemeSet.of(Theme.Fork)), PuzzleResult.Success, 0, 0, inRated = true, inNow = 1_000L)
			val vExport = File(vDirectory, "export.db")
			UserBackup.export(vSource, vExport.absolutePath)
			vSource.close()
			assertEquals(UserDatabase.kVersion, UserBackup.validate(vExport.absolutePath))
			val vRestored = createUserDatabase(vExport)
			try {
				assertEquals(listOf("p1"), vRestored.userDao().observeAllAttempts().first().map { it.puzzleId })
				assertEquals(1, vRestored.userDao().countSuccessesSince(0L))
			} finally {
				vRestored.close()
			}
			val vText = File(vDirectory, "notes.txt").apply { writeText("pas une base") }
			assertNull(UserBackup.validate(vText.absolutePath), "un fichier texte est refusé")
			val vPuzzles = createPuzzleDatabase(File(vDirectory, "puzzles.db"))
			vPuzzles.puzzleDao().count()
			vPuzzles.close()
			assertNull(UserBackup.validate(File(vDirectory, "puzzles.db").absolutePath), "la base des puzzles est refusée")
		} finally {
			vDirectory.deleteRecursively()
		}
	}
}
