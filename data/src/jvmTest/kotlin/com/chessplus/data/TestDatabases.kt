package com.chessplus.data

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.chessplus.core.model.Move
import com.chessplus.core.model.ThemeSet
import com.chessplus.core.puzzle.Puzzle
import com.chessplus.data.puzzle.PuzzleDatabase
import com.chessplus.data.user.UserDatabase
import kotlinx.coroutines.Dispatchers

/**
 * Bases en mémoire et données factices pour les tests de :data.
 */
object TestDatabases {
	/**
	 * Crée une base des problèmes vide en mémoire.
	 */
	fun puzzles(): PuzzleDatabase = Room.inMemoryDatabaseBuilder<PuzzleDatabase>()
		.setDriver(BundledSQLiteDriver())
		.setQueryCoroutineContext(Dispatchers.IO)
		.build()

	/**
	 * Crée une base utilisateur vide en mémoire.
	 */
	fun user(): UserDatabase = Room.inMemoryDatabaseBuilder<UserDatabase>()
		.setDriver(BundledSQLiteDriver())
		.setQueryCoroutineContext(Dispatchers.IO)
		.build()

	/**
	 * Crée un problème factice (position initiale, deux coups) de cote et de thèmes donnés.
	 */
	fun puzzle(inId: String, inRating: Int, inThemes: ThemeSet = ThemeSet.kEmpty): Puzzle = Puzzle(
		id = inId,
		fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
		moves = Move.listFromUci("e2e4 e7e5"),
		rating = inRating,
		themes = inThemes,
	)
}
