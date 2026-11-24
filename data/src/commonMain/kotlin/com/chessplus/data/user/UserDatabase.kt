package com.chessplus.data.user

import androidx.room.AutoMigration
import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor

/**
 * Base utilisateur : historique, bilans par problème, cote, problème en cours et parties des défis.
 *
 * Elle est séparée de la base des problèmes pour que celle-ci puisse être remplacée sans
 * perdre la progression ; les deux bases ne sont liées que par l'identifiant Lichess.
 */
@Database(
	entities = [AttemptEntity::class, PuzzleStateEntity::class, PlayerEntity::class, ActiveSessionEntity::class, ChallengeRunEntity::class, DailyPuzzleEntity::class],
	version = UserDatabase.kVersion,
	exportSchema = true,
	// Version 2 : durée, coup de l'échec et écart de cote des tentatives (colonnes facultatives).
	// Version 3 : parties des défis Survie et Rush (nouvelle table).
	// Version 4 : puzzles du jour attribués à chaque date (nouvelle table).
	autoMigrations = [AutoMigration(from = 1, to = 2), AutoMigration(from = 2, to = 3), AutoMigration(from = 3, to = 4)],
)
@ConstructedBy(UserDatabaseConstructor::class)
abstract class UserDatabase : RoomDatabase() {
	/**
	 * Retourne le DAO de la base utilisateur.
	 */
	abstract fun userDao(): UserDao

	companion object {
		/** Version du schéma ; toute évolution après la première version publiée exige une migration. */
		const val kVersion = 4

		/** Nom du fichier de la base sur l'appareil. */
		const val kFileName = "user.db"
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
