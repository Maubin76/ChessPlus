package com.chessplus.app.game

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Clés de jour des puzzles du jour (« yyyyMMdd », heure locale).
 */
object DailyDays {
	/** Format des clés. */
	private val kFormat: DateTimeFormatter = DateTimeFormatter.BASIC_ISO_DATE

	/**
	 * Retourne la clé du jour d'une date (millisecondes).
	 */
	fun keyOf(inTime: Long, inZone: ZoneId = ZoneId.systemDefault()): String = Instant.ofEpochMilli(inTime).atZone(inZone).toLocalDate().format(kFormat)

	/**
	 * Retourne la clé d'un jour.
	 */
	fun keyOf(inDate: LocalDate): String = inDate.format(kFormat)

	/**
	 * Retourne le jour d'une clé.
	 */
	fun dateOf(inKey: String): LocalDate = LocalDate.parse(inKey, kFormat)
}
