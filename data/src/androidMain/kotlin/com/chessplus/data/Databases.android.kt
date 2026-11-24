package com.chessplus.data

import android.content.Context
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.chessplus.data.puzzle.PuzzleDatabase
import com.chessplus.data.puzzle.PuzzleDatabaseConstructor
import com.chessplus.data.user.UserDatabase
import com.chessplus.data.user.UserDatabaseConstructor
import kotlinx.coroutines.Dispatchers
import java.io.File

/** Suffixe du fichier qui mémorise la version de la base des problèmes copiée sur l'appareil. */
private const val kVersionSuffix = ".version"

/**
 * Copie `assets/puzzles.db` dans le répertoire des bases si elle est absente, si sa version
 * diffère de [PuzzleDatabase.kVersion] ou si l'application a été installée ou mise à jour depuis la
 * dernière copie : la base embarquée change avec l'application (régénérée chaque mois par la CI,
 * nouvelles règles du générateur) sans que son schéma change. À appeler hors du fil principal
 * (copie de ~40 Mo, une fois par mise à jour).
 *
 * La copie est faite explicitement plutôt qu'avec `createFromAsset`, pour ne pas dépendre de la
 * prise en charge des bases préremplies par le driver SQLite intégré.
 */
fun installPuzzleDatabase(inContext: Context) {
	val vTarget = inContext.getDatabasePath(PuzzleDatabase.kFileName)
	val vMarker = File(vTarget.path + kVersionSuffix)
	val vInstalledAt = inContext.packageManager.getPackageInfo(inContext.packageName, 0).lastUpdateTime
	val vExpected = "${PuzzleDatabase.kVersion}@$vInstalledAt"
	if (vTarget.isFile && vMarker.isFile && vMarker.readText() == vExpected) {
		return
	}
	vTarget.parentFile?.mkdirs()
	val vTemporary = File(vTarget.path + ".tmp")
	inContext.assets.open(PuzzleDatabase.kFileName).use { inInput ->
		vTemporary.outputStream().use { inOutput -> inInput.copyTo(inOutput) }
	}
	for (vSuffix in listOf("-journal", "-wal", "-shm")) {
		File(vTarget.path + vSuffix).delete()
	}
	check(vTemporary.renameTo(vTarget) || (vTarget.delete() && vTemporary.renameTo(vTarget))) { "Impossible d'installer ${vTarget.path}" }
	vMarker.writeText(vExpected)
}

/**
 * Ouvre la base des problèmes installée par [installPuzzleDatabase]. Le constructeur généré est
 * passé explicitement, pour ne pas dépendre de la réflexion (version release optimisée par R8).
 */
fun createPuzzleDatabase(inContext: Context): PuzzleDatabase {
	val vContext = inContext.applicationContext
	return Room.databaseBuilder<PuzzleDatabase>(vContext, vContext.getDatabasePath(PuzzleDatabase.kFileName).absolutePath, factory = { PuzzleDatabaseConstructor.initialize() })
		.setDriver(BundledSQLiteDriver())
		.setQueryCoroutineContext(Dispatchers.IO)
		.build()
}

/**
 * Ouvre (ou crée) la base utilisateur.
 */
fun createUserDatabase(inContext: Context): UserDatabase {
	val vContext = inContext.applicationContext
	return Room.databaseBuilder<UserDatabase>(vContext, vContext.getDatabasePath(UserDatabase.kFileName).absolutePath, factory = { UserDatabaseConstructor.initialize() })
		.setDriver(BundledSQLiteDriver())
		.setQueryCoroutineContext(Dispatchers.IO)
		.build()
}
