package com.chessplus.data.user

import androidx.room.useWriterConnection
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver

/**
 * Export et vérification d'une sauvegarde de la progression (base utilisateur complète).
 */
object UserBackup {
	/** Tables qu'une base utilisateur de ChessPlus contient dans toutes ses versions. */
	private val kRequiredTables = setOf("attempt", "puzzle_state", "player")

	/**
	 * Écrit une copie cohérente de la base ouverte dans [inTargetPath] (`VACUUM INTO` : journal WAL
	 * compris, sans fermer la base). Le fichier cible ne doit pas exister.
	 */
	suspend fun export(inDatabase: UserDatabase, inTargetPath: String) {
		inDatabase.useWriterConnection { inConnection ->
			inConnection.usePrepared("VACUUM INTO ?") { inStatement ->
				inStatement.bindText(1, inTargetPath)
				inStatement.step()
			}
		}
	}

	/**
	 * Vérifie qu'un fichier est une base utilisateur de ChessPlus d'une version connue ; retourne sa
	 * version (les plus anciennes seront migrées à l'ouverture), ou null si le fichier ne convient pas.
	 */
	fun validate(inPath: String): Int? = runCatching {
		val vConnection = BundledSQLiteDriver().open(inPath)
		try {
			val vVersion = vConnection.longQuery("PRAGMA user_version").toInt()
			val vTables = mutableSetOf<String>()
			val vStatement = vConnection.prepare("SELECT name FROM sqlite_master WHERE type = 'table'")
			try {
				while (vStatement.step()) {
					vTables.add(vStatement.getText(0))
				}
			} finally {
				vStatement.close()
			}
			vVersion.takeIf { it in 1..UserDatabase.kVersion && vTables.containsAll(kRequiredTables) }
		} finally {
			vConnection.close()
		}
	}.getOrNull()

	/**
	 * Exécute une requête qui retourne un seul entier.
	 */
	private fun SQLiteConnection.longQuery(inSql: String): Long {
		val vStatement = prepare(inSql)
		try {
			return if (vStatement.step()) vStatement.getLong(0) else 0L
		} finally {
			vStatement.close()
		}
	}
}
