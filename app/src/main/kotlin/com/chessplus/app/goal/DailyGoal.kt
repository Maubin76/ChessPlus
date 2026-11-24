package com.chessplus.app.goal

import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Avancement de l'objectif quotidien.
 *
 * @property done puzzles réussis aujourd'hui.
 * @property target objectif du jour.
 */
data class GoalProgress(val done: Int, val target: Int) {
	/** Vrai si l'objectif est atteint. */
	val reached: Boolean
		get() = done >= target

	/** Puzzles restant à réussir. */
	val remaining: Int
		get() = (target - done).coerceAtLeast(0)

	/** Avancement, de 0 à 1. */
	val fraction: Float
		get() = if (target <= 0) 1f else (done.toFloat() / target).coerceIn(0f, 1f)
}

/**
 * Calculs de l'objectif quotidien et de son rappel (logique pure, testable).
 */
object DailyGoal {
	/** Objectifs proposés dans les réglages. */
	val kGoalOptions = listOf(3, 5, 10, 15, 20, 30)

	/** Objectif par défaut. */
	const val kDefaultGoal = 5

	/** Heure du rappel par défaut, en minutes depuis minuit (20 h). */
	const val kDefaultReminderMinutes = 20 * 60

	/** Minutes dans une heure. */
	private const val kMinutesPerHour = 60

	/**
	 * Retourne le début du jour local d'une date (millisecondes).
	 */
	fun dayStart(inNow: Long, inZone: ZoneId = ZoneId.systemDefault()): Long =
		Instant.ofEpochMilli(inNow).atZone(inZone).toLocalDate().atStartOfDay(inZone).toInstant().toEpochMilli()

	/**
	 * Retourne la prochaine heure du rappel strictement après [inNow] : aujourd'hui à
	 * [inMinutes] minutes après minuit si ce n'est pas passé, sinon demain.
	 */
	fun nextReminder(inNow: Long, inMinutes: Int, inZone: ZoneId = ZoneId.systemDefault()): Long {
		val vNow = Instant.ofEpochMilli(inNow).atZone(inZone)
		val vTime = LocalTime.of(inMinutes / kMinutesPerHour, inMinutes % kMinutesPerHour)
		var vNext: ZonedDateTime = vNow.toLocalDate().atTime(vTime).atZone(inZone)
		if (!vNext.isAfter(vNow)) {
			vNext = vNow.toLocalDate().plusDays(1).atTime(vTime).atZone(inZone)
		}
		return vNext.toInstant().toEpochMilli()
	}

	/**
	 * Formate une heure en minutes depuis minuit (« 20:00 »).
	 */
	fun formatTime(inMinutes: Int): String = "%02d:%02d".format(inMinutes / kMinutesPerHour, inMinutes % kMinutesPerHour)
}
