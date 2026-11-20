package com.chessplus.data

import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.chessplus.data.puzzle.PuzzleDatabase
import com.chessplus.data.user.UserDatabase
import kotlinx.coroutines.Dispatchers
import java.io.File

/**
 * Ouvre (ou crée) une base des problèmes dans un fichier.
 *
 * Le journal est en mode `TRUNCATE` plutôt que WAL : le fichier produit par le builder est
 * ainsi autonome, sans fichier `-wal` à côté.
 */
fun createPuzzleDatabase(inFile: File): PuzzleDatabase =
	Room.databaseBuilder<PuzzleDatabase>(inFile.absolutePath)
		.setDriver(BundledSQLiteDriver())
		.setJournalMode(RoomDatabase.JournalMode.TRUNCATE)
		.setQueryCoroutineContext(Dispatchers.IO)
		.build()

/**
 * Ouvre (ou crée) une base utilisateur dans un fichier (future application desktop).
 */
fun createUserDatabase(inFile: File): UserDatabase =
	Room.databaseBuilder<UserDatabase>(inFile.absolutePath)
		.setDriver(BundledSQLiteDriver())
		.setQueryCoroutineContext(Dispatchers.IO)
		.build()
