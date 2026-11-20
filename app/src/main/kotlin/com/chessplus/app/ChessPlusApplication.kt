package com.chessplus.app

import android.app.Application

/**
 * Application ChessPlus : porte le conteneur de dépendances.
 */
class ChessPlusApplication : Application() {
	/** Dépendances partagées par les écrans. */
	lateinit var container: AppContainer
		private set

	/**
	 * Crée le conteneur de dépendances (les bases sont ouvertes plus tard, hors du fil principal).
	 */
	override fun onCreate() {
		super.onCreate()
		container = AppContainer(this)
	}
}
