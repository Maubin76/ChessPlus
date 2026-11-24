package com.chessplus.app.goal

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.app.Notification
import com.chessplus.app.ChessPlusApplication
import com.chessplus.app.MainActivity
import com.chessplus.app.R
import com.chessplus.app.settings.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Rappel de l'objectif quotidien : une alarme approximative (AlarmManager, sans permission
 * d'alarme exacte) à l'heure choisie ; à son déclenchement, une notification n'est envoyée que si
 * l'objectif n'est pas atteint, puis l'alarme du lendemain est programmée.
 */
object GoalReminder {
	/** Identifiant du canal de notification des rappels. */
	const val kChannelId = "goal_reminder"

	/** Identifiant de la notification de rappel. */
	private const val kNotificationId = 1

	/** Code de la demande d'alarme. */
	private const val kRequestCode = 1

	/** Fenêtre de déclenchement accordée au système (10 minutes après l'heure choisie). */
	private const val kWindowMs = 10L * 60 * 1000

	/**
	 * Crée le canal de notification des rappels (sans effet s'il existe déjà).
	 */
	fun createChannel(inContext: Context) {
		val vChannel = NotificationChannel(kChannelId, inContext.getString(R.string.goal_channel), NotificationManager.IMPORTANCE_DEFAULT)
		vChannel.description = inContext.getString(R.string.goal_channel_detail)
		inContext.getSystemService(NotificationManager::class.java).createNotificationChannel(vChannel)
	}

	/**
	 * Programme la prochaine alarme selon les réglages, ou l'annule si le rappel est désactivé.
	 */
	fun schedule(inContext: Context, inSettings: Settings, inNow: Long = System.currentTimeMillis()) {
		val vAlarms = inContext.getSystemService(AlarmManager::class.java)
		val vIntent = alarmIntent(inContext)
		vAlarms.cancel(vIntent)
		if (inSettings.reminder) {
			vAlarms.setWindow(AlarmManager.RTC_WAKEUP, DailyGoal.nextReminder(inNow, inSettings.reminderMinutes), kWindowMs, vIntent)
		}
	}

	/**
	 * Envoie la notification de rappel si l'objectif n'est pas atteint et si les notifications sont autorisées.
	 */
	fun notifyIfNeeded(inContext: Context, inProgress: GoalProgress) {
		if (inProgress.reached || inContext.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
			return
		}
		val vOpen = PendingIntent.getActivity(
			inContext, 0, Intent(inContext, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP), PendingIntent.FLAG_IMMUTABLE,
		)
		val vText = inContext.resources.getQuantityString(R.plurals.goal_notification_text, inProgress.remaining, inProgress.remaining, inProgress.done, inProgress.target)
		val vNotification = Notification.Builder(inContext, kChannelId)
			.setSmallIcon(R.drawable.ic_stat_goal)
			.setContentTitle(inContext.getString(R.string.goal_notification_title))
			.setContentText(vText)
			.setContentIntent(vOpen)
			.setAutoCancel(true)
			.build()
		inContext.getSystemService(NotificationManager::class.java).notify(kNotificationId, vNotification)
	}

	/**
	 * Retourne l'intention de l'alarme, adressée à [GoalReminderReceiver].
	 */
	private fun alarmIntent(inContext: Context): PendingIntent =
		PendingIntent.getBroadcast(inContext, kRequestCode, Intent(inContext, GoalReminderReceiver::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
}

/**
 * Déclenchement du rappel : lit l'avancement de l'objectif, notifie si besoin, programme le lendemain.
 */
class GoalReminderReceiver : BroadcastReceiver() {
	/** Portée des lectures en base, hors du fil principal. */
	private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

	/**
	 * Traite l'alarme de manière asynchrone (goAsync) pour lire la base utilisateur.
	 */
	override fun onReceive(inContext: Context, inIntent: Intent) {
		val vPending = goAsync()
		val vContainer = (inContext.applicationContext as ChessPlusApplication).container
		scope.launch {
			try {
				GoalReminder.notifyIfNeeded(inContext, vContainer.goalProgress())
			} finally {
				GoalReminder.schedule(inContext, vContainer.settingsStore.settings.value)
				vPending.finish()
			}
		}
	}
}

/**
 * Reprogramme le rappel après un redémarrage, une mise à jour de l'application ou un changement d'heure.
 */
class GoalReminderRescheduler : BroadcastReceiver() {
	/**
	 * Reprogramme l'alarme selon les réglages courants.
	 */
	override fun onReceive(inContext: Context, inIntent: Intent) {
		val vContainer = (inContext.applicationContext as ChessPlusApplication).container
		GoalReminder.schedule(inContext, vContainer.settingsStore.settings.value)
	}
}
