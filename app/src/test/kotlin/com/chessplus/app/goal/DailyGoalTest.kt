package com.chessplus.app.goal

import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests de l'objectif quotidien et de l'heure de son rappel.
 */
class DailyGoalTest {
	/** Fuseau des tests. */
	private val zone = ZoneId.of("Europe/Paris")

	/**
	 * Retourne l'instant d'une date et heure locales (millisecondes).
	 */
	private fun at(inYear: Int, inMonth: Int, inDay: Int, inHour: Int, inMinute: Int = 0): Long =
		ZonedDateTime.of(inYear, inMonth, inDay, inHour, inMinute, 0, 0, zone).toInstant().toEpochMilli()

	/** Vérifie le prochain rappel : aujourd'hui s'il n'est pas passé, sinon demain, y compris au changement d'heure. */
	@Test
	fun nextReminder() {
		assertEquals(at(2026, 11, 24, 20), DailyGoal.nextReminder(at(2026, 11, 24, 9), 20 * 60, zone))
		assertEquals(at(2026, 11, 25, 20), DailyGoal.nextReminder(at(2026, 11, 24, 20), 20 * 60, zone), "pile à l'heure : le lendemain")
		assertEquals(at(2026, 11, 25, 7, 30), DailyGoal.nextReminder(at(2026, 11, 24, 21), 7 * 60 + 30, zone))
		assertEquals(at(2027, 3, 28, 20), DailyGoal.nextReminder(at(2027, 3, 27, 22), 20 * 60, zone), "passage à l'heure d'été")
	}

	/** Vérifie le début du jour local et l'avancement. */
	@Test
	fun progress() {
		assertEquals(at(2026, 11, 24, 0), DailyGoal.dayStart(at(2026, 11, 24, 18, 45), zone))
		val vHalf = GoalProgress(2, 5)
		assertFalse(vHalf.reached)
		assertEquals(3, vHalf.remaining)
		assertEquals(0.4f, vHalf.fraction)
		val vDone = GoalProgress(7, 5)
		assertTrue(vDone.reached)
		assertEquals(0, vDone.remaining)
		assertEquals(1f, vDone.fraction)
		assertEquals("07:05", DailyGoal.formatTime(7 * 60 + 5))
	}
}
