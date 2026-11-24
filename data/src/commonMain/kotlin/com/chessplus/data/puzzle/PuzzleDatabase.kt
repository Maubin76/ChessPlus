package com.chessplus.data.puzzle

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor

/**
 * Base des problèmes, générée par `:puzzle-builder` et embarquée dans l'application
 * (`assets/puzzles.db`). Elle est en lecture seule pour l'application et peut être remplacée
 * par une nouvelle version sans toucher à la base utilisateur.
 */
@Database(entities = [PuzzleEntity::class], version = PuzzleDatabase.kVersion, exportSchema = true)
@ConstructedBy(PuzzleDatabaseConstructor::class)
abstract class PuzzleDatabase : RoomDatabase() {
	/**
	 * Retourne le DAO des problèmes.
	 */
	abstract fun puzzleDao(): PuzzleDao

	companion object {
		/** Version du schéma (la base embarquée est de toute façon recopiée à chaque mise à jour de l'application). */
		const val kVersion = 1

		/** Nom du fichier de la base, dans les assets comme sur l'appareil. */
		const val kFileName = "puzzles.db"
	}
}

/**
 * Constructeur généré par Room pour chaque plateforme.
 */
@Suppress("KotlinNoActualForExpect")
expect object PuzzleDatabaseConstructor : RoomDatabaseConstructor<PuzzleDatabase> {
	/**
	 * Instancie l'implémentation générée de la base.
	 */
	override fun initialize(): PuzzleDatabase
}
