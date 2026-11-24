package com.chessplus.app

import android.app.Application
import com.chessplus.app.goal.GoalReminder
import com.chessplus.app.widget.ChessPlusWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Application ChessPlus : porte le conteneur de dépendances.
 */
class ChessPlusApplication : Application() {
	/** Dépendances partagées par les écrans. */
	lateinit var container: AppContainer
		private set

	/** Portée de l'application (suivi des réglages du rappel). */
	private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

	/**
	 * Crée le conteneur de dépendances (les bases sont ouvertes plus tard, hors du fil principal),
	 * le canal des rappels, reprogramme le rappel à chaque changement de ses réglages et met à jour le
	 * widget à chaque puzzle terminé ou changement d'objectif.
	 */
	override fun onCreate() {
		super.onCreate()
		container = AppContainer(this)
		GoalReminder.createChannel(this)
		scope.launch {
			val vDao = container.ready().second.userDao()
			combine(vDao.observeAttemptCount(), vDao.observeSolvedDailyDays(), container.settingsStore.settings.map { it.dailyGoal }) { _, _, _ -> }
				.collect { ChessPlusWidget.refresh(this@ChessPlusApplication) }
		}
		scope.launch {
			container.settingsStore.settings
				.map { it.reminder to it.reminderMinutes }
				.distinctUntilChanged()
				.collect { GoalReminder.schedule(this@ChessPlusApplication, container.settingsStore.settings.value) }
		}
	}
}
