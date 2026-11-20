package com.chessplus.data

import android.content.Context
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.chessplus.data.puzzle.PuzzleDatabase
import com.chessplus.data.user.UserDatabase
import kotlinx.coroutines.Dispatchers

/**
 * Ouvre la base des problèmes, copiée depuis `assets/puzzles.db` au premier lancement.
 *
 * Quand [PuzzleDatabase.kVersion] change, l'ancienne copie est supprimée et la nouvelle base
 * embarquée est recopiée ; la base utilisateur n'est pas touchée.
 */
fun createPuzzleDatabase(inContext: Context): PuzzleDatabase {
	val vContext = inContext.applicationContext
	return Room.databaseBuilder<PuzzleDatabase>(vContext, vContext.getDatabasePath(PuzzleDatabase.kFileName).absolutePath)
		.createFromAsset(PuzzleDatabase.kFileName)
		.fallbackToDestructiveMigration(dropAllTables = true)
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
