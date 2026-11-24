package com.chessplus.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.chessplus.app.ChessPlusApplication
import com.chessplus.app.MainActivity
import com.chessplus.app.R
import com.chessplus.app.game.DailyDays
import com.chessplus.app.ui.mainTheme
import com.chessplus.app.ui.themeLabel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Widget de l'écran d'accueil du téléphone : objectif du jour et puzzle du jour. Toucher le puzzle
 * l'ouvre directement ; toucher le reste ouvre l'application.
 */
class ChessPlusWidget : AppWidgetProvider() {
	/**
	 * Mise à jour périodique ou à l'ajout du widget : lit les données hors du fil principal.
	 */
	override fun onUpdate(inContext: Context, inManager: AppWidgetManager, inWidgetIds: IntArray) {
		val vPending = goAsync()
		kScope.launch {
			try {
				render(inContext, inManager, inWidgetIds)
			} finally {
				vPending.finish()
			}
		}
	}

	companion object {
		/** Extra de l'intention d'ouverture : écran à afficher. */
		const val kExtraOpen = "com.chessplus.app.OPEN"

		/** Valeur de [kExtraOpen] : puzzle du jour. */
		const val kOpenDaily = "daily"

		/** Portée des lectures en base. */
		private val kScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

		/**
		 * Met à jour tous les widgets posés (appelé quand un puzzle est terminé ou l'objectif modifié).
		 */
		fun refresh(inContext: Context) {
			val vManager = AppWidgetManager.getInstance(inContext)
			val vIds = vManager.getAppWidgetIds(ComponentName(inContext, ChessPlusWidget::class.java))
			if (vIds.isNotEmpty()) {
				kScope.launch { render(inContext, vManager, vIds) }
			}
		}

		/**
		 * Construit et applique la vue des widgets.
		 */
		private suspend fun render(inContext: Context, inManager: AppWidgetManager, inWidgetIds: IntArray) {
			val vContainer = (inContext.applicationContext as ChessPlusApplication).container
			val vGoal = vContainer.goalProgress()
			val vToday = DailyDays.keyOf(System.currentTimeMillis())
			val vPuzzle = vContainer.dailyStore.puzzleFor(vToday)
			val vSolved = vToday in vContainer.dailyStore.solvedDays()
			val vViews = RemoteViews(inContext.packageName, R.layout.widget_daily)
			vViews.setTextViewText(R.id.widget_goal_value, inContext.getString(R.string.goal_value, vGoal.done, vGoal.target))
			vViews.setTextColor(R.id.widget_goal_value, inContext.getColor(if (vGoal.reached) R.color.widget_success else R.color.widget_ink))
			vViews.setProgressBar(R.id.widget_goal_bar, kProgressMax, (vGoal.fraction * kProgressMax).toInt(), false)
			vViews.setTextViewText(R.id.widget_daily_title, vPuzzle?.let { mainTheme(it.themes) }?.let { inContext.getString(themeLabel(it)) } ?: inContext.getString(R.string.home_daily_default))
			vViews.setTextViewText(R.id.widget_daily_state, inContext.getString(if (vSolved) R.string.widget_daily_solved else R.string.widget_daily_todo))
			vViews.setTextColor(R.id.widget_daily_state, inContext.getColor(if (vSolved) R.color.widget_success else R.color.widget_accent))
			vViews.setOnClickPendingIntent(R.id.widget_root, openIntent(inContext, null, kRequestApp))
			vViews.setOnClickPendingIntent(R.id.widget_daily_row, openIntent(inContext, kOpenDaily, kRequestDaily))
			inManager.updateAppWidget(inWidgetIds, vViews)
		}

		/**
		 * Retourne l'intention qui ouvre l'application, éventuellement sur un écran ([inOpen]).
		 */
		private fun openIntent(inContext: Context, inOpen: String?, inRequest: Int): PendingIntent {
			val vIntent = Intent(inContext, MainActivity::class.java)
				.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
			inOpen?.let { vIntent.putExtra(kExtraOpen, it) }
			return PendingIntent.getActivity(inContext, inRequest, vIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
		}

		/** Graduation de la barre de l'objectif. */
		private const val kProgressMax = 100

		/** Code de l'intention d'ouverture de l'application. */
		private const val kRequestApp = 10

		/** Code de l'intention d'ouverture du puzzle du jour. */
		private const val kRequestDaily = 11
	}
}
