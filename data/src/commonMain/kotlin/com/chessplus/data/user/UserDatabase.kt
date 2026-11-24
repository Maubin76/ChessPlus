package com.chessplus.data.user

import androidx.room.AutoMigration
import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.migration.AutoMigrationSpec
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/**
 * Base utilisateur : historique, bilans par problème, cote, problème en cours et parties des défis.
 *
 * Elle est séparée de la base des problèmes pour que celle-ci puisse être remplacée sans
 * perdre la progression ; les deux bases ne sont liées que par l'identifiant Lichess.
 */
@Database(
	entities = [AttemptEntity::class, PuzzleStateEntity::class, PlayerEntity::class, ActiveSessionEntity::class, ChallengeRunEntity::class, DailyPuzzleEntity::class, ReviewItemEntity::class],
	version = UserDatabase.kVersion,
	exportSchema = true,
	// Version 2 : durée, coup de l'échec et écart de cote des tentatives (colonnes facultatives).
	// Version 3 : parties des défis Survie et Rush (nouvelle table).
	// Version 4 : puzzles du jour attribués à chaque date (nouvelle table).
	// Version 5 : file de révision, remplie avec les puzzles ratés jamais réussis.
	autoMigrations = [
		AutoMigration(from = 1, to = 2),
		AutoMigration(from = 2, to = 3),
		AutoMigration(from = 3, to = 4),
		AutoMigration(from = 4, to = 5, spec = ReviewSeedMigration::class),
	],
)
@ConstructedBy(UserDatabaseConstructor::class)
abstract class UserDatabase : RoomDatabase() {
	/**
	 * Retourne le DAO de la base utilisateur.
	 */
	abstract fun userDao(): UserDao

	companion object {
		/** Version du schéma ; toute évolution après la première version publiée exige une migration. */
		const val kVersion = 5

		/** Nom du fichier de la base sur l'appareil. */
		const val kFileName = "user.db"
	}
}

/**
 * Migration vers la version 5 : les puzzles déjà ratés et jamais réussis entrent dans la file de
 * révision, à réviser le lendemain de leur dernière tentative (donc tout de suite s'il est passé).
 */
class ReviewSeedMigration : AutoMigrationSpec {
	/**
	 * Remplit la file de révision après la création de sa table.
	 */
	override fun onPostMigrate(connection: SQLiteConnection) {
		connection.execSQL(
			"INSERT OR IGNORE INTO review_item (puzzleId, level, dueAt, addedAt) " +
				"SELECT puzzleId, 0, lastPlayedAt + $kDayMs, lastPlayedAt FROM puzzle_state WHERE solved = 0"
		)
	}

	companion object {
		/** Durée d'un jour en millisecondes. */
		private const val kDayMs = 24L * 60 * 60 * 1000
	}
}

/**
 * Constructeur généré par Room pour chaque plateforme.
 */
@Suppress("KotlinNoActualForExpect")
expect object UserDatabaseConstructor : RoomDatabaseConstructor<UserDatabase> {
	/**
	 * Instancie l'implémentation générée de la base.
	 */
	override fun initialize(): UserDatabase
}
