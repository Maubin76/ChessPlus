package com.chessplus.data

import android.content.Context
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.chessplus.data.puzzle.PuzzleDatabase
import com.chessplus.data.user.UserDatabase
import kotlinx.coroutines.Dispatchers
import java.io.File

/** Suffixe du fichier qui mémorise la version de la base des problèmes copiée sur l'appareil. */
private const val kVersionSuffix = ".version"

/**
 * Copie `assets/puzzles.db` dans le répertoire des bases si elle est absente ou si sa version
 * diffère de [PuzzleDatabase.kVersion]. À appeler hors du fil principal (copie de ~37 Mo).
 *
 * La copie est faite explicitement plutôt qu'avec `createFromAsset`, pour ne pas dépendre de la
 * prise en charge des bases préremplies par le driver SQLite intégré.
 */
fun installPuzzleDatabase(inContext: Context) {
	val vTarget = inContext.getDatabasePath(PuzzleDatabase.kFileName)
	val vMarker = File(vTarget.path + kVersionSuffix)
	val vExpected = PuzzleDatabase.kVersion.toString()
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
 * Ouvre la base des problèmes installée par [installPuzzleDatabase].
 */
fun createPuzzleDatabase(inContext: Context): PuzzleDatabase {
	val vContext = inContext.applicationContext
	return Room.databaseBuilder<PuzzleDatabase>(vContext, vContext.getDatabasePath(PuzzleDatabase.kFileName).absolutePath)
		.setDriver(BundledSQLiteDriver())
		.setQueryCoroutineContext(Dispatchers.IO)
		.build()
}

/**
 * Ouvre (ou crée) la base utilisateur.
 */
fun createUserDatabase(inContext: Context): UserDatabase {
	val vContext = inContext.applicationContext
	return Room.databaseBuilder<UserDatabase>(vContext, vContext.getDatabasePath(UserDatabase.kFileName).absolutePath)
		.setDriver(BundledSQLiteDriver())
		.setQueryCoroutineContext(Dispatchers.IO)
		.build()
}
