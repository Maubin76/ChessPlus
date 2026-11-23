package com.chessplus.data

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import com.chessplus.core.puzzle.PuzzleResult
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Test de la migration de la base utilisateur de la version 1 (v1.0 publiée) à la version 2.
 */
class UserMigrationTest {
	/**
	 * Retourne les requêtes de création du schéma exporté par Room, avec le vrai nom de chaque table
	 * à la place du paramètre `${TABLE_NAME}`.
	 */
	private fun createStatements(inSchema: String): List<String> {
		val vResult = mutableListOf<String>()
		var vTable = ""
		for (vMatch in Regex("\"(tableName|createSql)\": \"([^\"]+)\"").findAll(inSchema)) {
			if (vMatch.groupValues[1] == "tableName") {
				vTable = vMatch.groupValues[2]
			} else {
				vResult.add(vMatch.groupValues[2].replace("\${TABLE_NAME}", vTable))
			}
		}
		return vResult
	}

	/** Vérifie qu'une base de la version 1 garde ses tentatives et reçoit les nouvelles colonnes. */
	@Test
	fun migrateFromVersion1() = runBlocking<Unit> {
		val vDirectory = Files.createTempDirectory("chessplus-migration").toFile()
		val vFile = File(vDirectory, "user.db")
		try {
			val vSchema = File("schemas/com.chessplus.data.user.UserDatabase/1.json").readText()
			val vIdentity = Regex("\"identityHash\": \"([0-9a-f]+)\"").find(vSchema)!!.groupValues[1]
			val vConnection = BundledSQLiteDriver().open(vFile.absolutePath)
			createStatements(vSchema).forEach { vConnection.execSQL(it) }
			vConnection.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
			vConnection.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '$vIdentity')")
			vConnection.execSQL(
				"INSERT INTO attempt (puzzleId, playedAt, result, rated, puzzleRating, themesLow, themesHigh, mistakeCount, revealedCount, playerRatingBefore, playerRatingAfter) " +
					"VALUES ('00sHx', 1000, 'Success', 1, 1760, 0, 0, 0, 0, 1500.0, 1512.0)"
			)
			vConnection.execSQL("PRAGMA user_version = 1")
			vConnection.close()

			val vDatabase = createUserDatabase(vFile)
			try {
				val vAttempts = vDatabase.userDao().observeAllAttempts().first()
				assertEquals(1, vAttempts.size)
				assertEquals("00sHx", vAttempts[0].puzzleId)
				assertEquals(PuzzleResult.Success, vAttempts[0].result)
				assertNull(vAttempts[0].durationMs)
				assertNull(vAttempts[0].failedAtMove)
				assertNull(vAttempts[0].playerDeviationAfter)
			} finally {
				vDatabase.close()
			}
		} finally {
			vDirectory.deleteRecursively()
		}
	}
}
