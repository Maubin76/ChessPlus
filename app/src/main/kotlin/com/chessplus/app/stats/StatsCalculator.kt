package com.chessplus.app.stats

import com.chessplus.app.ui.ThemeFamily
import com.chessplus.app.ui.themeFamily
import com.chessplus.core.model.Theme
import com.chessplus.core.model.ThemeSet
import com.chessplus.core.puzzle.PuzzleResult
import com.chessplus.core.rating.Glicko2RatingSystem
import com.chessplus.core.rating.PlayerRating
import com.chessplus.data.user.AttemptEntity
import java.util.Calendar
import java.util.TimeZone
import kotlin.math.roundToInt

/**
 * Point de l'évolution de la cote.
 *
 * @property time date de la tentative (millisecondes).
 * @property rating cote après la tentative.
 * @property deviation écart de cote après la tentative, ou null s'il n'est pas connu.
 */
data class RatingSample(val time: Long, val rating: Double, val deviation: Double?)

/**
 * Nombre de tentatives d'un jour du calendrier.
 *
 * @property dayStart début du jour (millisecondes, heure locale).
 * @property count nombre de problèmes joués ce jour-là.
 */
data class DayCount(val dayStart: Long, val count: Int)

/**
 * Volume d'une semaine.
 *
 * @property weekStart lundi de la semaine (millisecondes, heure locale).
 * @property attempts problèmes joués.
 * @property successes problèmes réussis.
 */
data class WeekVolume(val weekStart: Long, val attempts: Int, val successes: Int)

/**
 * Bilan d'un thème.
 *
 * @property theme thème.
 * @property attempts tentatives (toutes).
 * @property successes réussites.
 * @property rating cote du joueur sur ce thème (Glicko-2 rejoué sur les tentatives notées).
 * @property bestSolved cote du problème le plus difficile réussi sur ce thème, ou null.
 * @property averageDurationMs durée moyenne des réussites, ou null si inconnue.
 */
data class ThemeStat(
	val theme: Theme,
	val attempts: Int,
	val successes: Int,
	val rating: Int,
	val bestSolved: Int?,
	val averageDurationMs: Long?,
) {
	/** Taux de réussite, de 0 à 1. */
	val rate: Float
		get() = if (attempts == 0) 0f else successes.toFloat() / attempts
}

/**
 * Réussite d'un groupe de tentatives (famille, tranche, longueur, moment de la journée).
 *
 * @property key identifiant du groupe.
 * @property attempts tentatives.
 * @property successes réussites.
 */
data class GroupStat<K>(val key: K, val attempts: Int, val successes: Int) {
	/** Taux de réussite, de 0 à 1. */
	val rate: Float
		get() = if (attempts == 0) 0f else successes.toFloat() / attempts
}

/**
 * Moment de la journée.
 *
 * @property fromHour première heure incluse.
 * @property toHour dernière heure exclue.
 */
enum class DayPart(val fromHour: Int, val toHour: Int) {
	Morning(6, 12),
	Afternoon(12, 18),
	Evening(18, 24),
	Night(0, 6),
}

/**
 * Longueur d'un problème, d'après les thèmes de longueur de Lichess.
 *
 * @property theme thème correspondant.
 */
enum class PuzzleLength(val theme: Theme) {
	OneMove(Theme.OneMove),
	Short(Theme.Short),
	Long(Theme.Long),
	VeryLong(Theme.VeryLong),
}

/**
 * Répartition des tentatives selon la façon dont elles se sont terminées.
 *
 * @property clean réussies du premier coup, sans indice.
 * @property hint échouées sur un indice (sans erreur avant).
 * @property mistake échouées sur une erreur.
 */
data class ResolutionProfile(val clean: Int, val hint: Int, val mistake: Int) {
	/** Total des tentatives. */
	val total: Int
		get() = clean + hint + mistake
}

/**
 * Problème le plus difficile réussi.
 *
 * @property puzzleId identifiant Lichess.
 * @property rating cote du problème.
 * @property themes thèmes du problème.
 */
data class BestSolved(val puzzleId: String, val rating: Int, val themes: ThemeSet)

/**
 * Toutes les statistiques de l'écran Statistiques.
 */
data class StatsReport(
	val attempts: Int,
	val successes: Int,
	val currentRating: Int?,
	val ratingDelta30Days: Int?,
	val bestRating: Int?,
	val ratingHistory: List<RatingSample>,
	val dayStreak: Int,
	val bestDayStreak: Int,
	val bestSuccessRun: Int,
	val calendar: List<DayCount>,
	val weeklyVolume: List<WeekVolume>,
	val themes: List<ThemeStat>,
	val families: List<GroupStat<ThemeFamily>>,
	val difficulty: List<GroupStat<Int>>,
	val profile: ResolutionProfile,
	val lengths: List<GroupStat<PuzzleLength>>,
	val dayParts: List<GroupStat<DayPart>>,
	val averageDurationMs: Long?,
	val weeklyDurationMs: List<Pair<Long, Long>>,
	val failedAtMove: List<Int>,
	val bestSolved: BestSolved?,
	val seenThemes: Int,
	val unseenThemes: List<Theme>,
	val weakest: List<ThemeStat>,
) {
	/** Taux de réussite global, de 0 à 1, ou null sans tentative. */
	val successRate: Float?
		get() = if (attempts == 0) null else successes.toFloat() / attempts
}

/**
 * Tentative d'un thème, pour sa fiche détaillée.
 *
 * @property puzzleId identifiant Lichess.
 * @property playedAt date.
 * @property success vrai si réussie.
 * @property puzzleRating cote du problème.
 */
data class ThemeAttempt(val puzzleId: String, val playedAt: Long, val success: Boolean, val puzzleRating: Int)

/**
 * Fiche détaillée d'un thème.
 *
 * @property stat bilan du thème.
 * @property ratingHistory cote du joueur sur ce thème après chaque tentative notée.
 * @property rollingRate taux de réussite glissant (fenêtre de [StatsCalculator.kRollingWindow] tentatives).
 * @property recent dernières tentatives, de la plus récente à la plus ancienne.
 */
data class ThemeDetail(
	val stat: ThemeStat,
	val ratingHistory: List<RatingSample>,
	val rollingRate: List<Float>,
	val recent: List<ThemeAttempt>,
)

/**
 * Calcul des statistiques à partir des tentatives enregistrées (fonctions pures, testables).
 *
 * @property zone fuseau horaire des jours, semaines et heures.
 */
class StatsCalculator(private val zone: TimeZone = TimeZone.getDefault()) {
	/** Algorithme de cote, rejoué par thème. */
	private val ratingSystem = Glicko2RatingSystem()

	/**
	 * Calcule le rapport complet ; [inAttempts] dans l'ordre chronologique.
	 */
	fun report(inAttempts: List<AttemptEntity>, inNow: Long): StatsReport {
		val vRated = inAttempts.filter { it.rated }
		val vHistory = vRated.map { RatingSample(it.playedAt, it.playerRatingAfter, it.playerDeviationAfter) }
		val vCurrent = vHistory.lastOrNull()?.rating
		val vMonthAgo = inNow - kMonthMs
		val vReference = vRated.lastOrNull { it.playedAt < vMonthAgo }?.playerRatingAfter ?: vRated.firstOrNull { it.playedAt >= vMonthAgo }?.playerRatingBefore
		val vThemes = themeStats(inAttempts)
		val vDays = inAttempts.map { dayStart(it.playedAt) }.toSortedSet()
		val vDurations = inAttempts.filter { it.result == PuzzleResult.Success }.mapNotNull { vAttempt -> vAttempt.durationMs?.let { vAttempt.playedAt to it } }
		return StatsReport(
			attempts = inAttempts.size,
			successes = inAttempts.count { it.result == PuzzleResult.Success },
			currentRating = vCurrent?.roundToInt(),
			ratingDelta30Days = if (vCurrent != null && vReference != null) (vCurrent - vReference).roundToInt() else null,
			bestRating = vHistory.maxOfOrNull { it.rating }?.roundToInt(),
			ratingHistory = vHistory,
			dayStreak = currentDayStreak(vDays, inNow),
			bestDayStreak = bestDayStreak(vDays),
			bestSuccessRun = bestSuccessRun(inAttempts),
			calendar = calendar(inAttempts, inNow),
			weeklyVolume = weeklyVolume(inAttempts, inNow),
			themes = vThemes,
			families = ThemeFamily.entries.map { vFamily ->
				val vMatching = inAttempts.filter { vAttempt -> themesOf(vAttempt).any { themeFamily(it) == vFamily } }
				GroupStat(vFamily, vMatching.size, vMatching.count { it.result == PuzzleResult.Success })
			},
			difficulty = inAttempts.groupBy { it.puzzleRating / kBandWidth * kBandWidth }.toSortedMap()
				.map { (vBand, vList) -> GroupStat(vBand, vList.size, vList.count { it.result == PuzzleResult.Success }) },
			profile = ResolutionProfile(
				clean = inAttempts.count { it.result == PuzzleResult.Success },
				hint = inAttempts.count { it.result == PuzzleResult.Failure && it.mistakeCount == 0 },
				mistake = inAttempts.count { it.result == PuzzleResult.Failure && it.mistakeCount > 0 },
			),
			lengths = PuzzleLength.entries.map { vLength ->
				val vMatching = inAttempts.filter { vLength.theme in themesOf(it) }
				GroupStat(vLength, vMatching.size, vMatching.count { it.result == PuzzleResult.Success })
			},
			dayParts = DayPart.entries.map { vPart ->
				val vMatching = inAttempts.filter { hourOf(it.playedAt) in vPart.fromHour until vPart.toHour }
				GroupStat(vPart, vMatching.size, vMatching.count { it.result == PuzzleResult.Success })
			},
			averageDurationMs = vDurations.map { it.second }.takeIf { it.isNotEmpty() }?.average()?.toLong(),
			weeklyDurationMs = vDurations.groupBy { weekStart(it.first) }.toSortedMap()
				.map { (vWeek, vList) -> vWeek to median(vList.map { it.second }) },
			failedAtMove = List(kFailedMoveBuckets) { vIndex ->
				inAttempts.count { vAttempt ->
					val vMove = vAttempt.failedAtMove ?: return@count false
					if (vIndex == kFailedMoveBuckets - 1) vMove >= kFailedMoveBuckets else vMove == vIndex + 1
				}
			},
			bestSolved = inAttempts.filter { it.result == PuzzleResult.Success }.maxByOrNull { it.puzzleRating }
				?.let { BestSolved(it.puzzleId, it.puzzleRating, themesOf(it)) },
			seenThemes = vThemes.size,
			unseenThemes = Theme.entries.filter { vTheme -> vThemes.none { it.theme == vTheme } },
			weakest = vThemes.filter { it.attempts >= kMinAttemptsForWeak && themeFamily(it.theme) in kTrainableFamilies }
				.sortedWith(compareBy<ThemeStat> { it.rate }.thenByDescending { it.attempts })
				.take(kWeakestCount),
		)
	}

	/**
	 * Calcule la fiche détaillée d'un thème ; [inAttempts] dans l'ordre chronologique.
	 */
	fun themeDetail(inAttempts: List<AttemptEntity>, inTheme: Theme): ThemeDetail? {
		val vStat = themeStats(inAttempts).firstOrNull { it.theme == inTheme } ?: return null
		val vMatching = inAttempts.filter { inTheme in themesOf(it) }
		var vRating = ratingSystem.initialRating
		val vHistory = mutableListOf<RatingSample>()
		for (vAttempt in vMatching.filter { it.rated }) {
			vRating = ratingSystem.update(vRating, vAttempt.puzzleRating, vAttempt.result)
			vHistory.add(RatingSample(vAttempt.playedAt, vRating.rating, vRating.deviation))
		}
		val vResults = vMatching.map { it.result == PuzzleResult.Success }
		val vRolling = vResults.indices.map { vIndex ->
			val vWindow = vResults.subList(maxOf(0, vIndex - kRollingWindow + 1), vIndex + 1)
			vWindow.count { it }.toFloat() / vWindow.size
		}
		return ThemeDetail(
			stat = vStat,
			ratingHistory = vHistory,
			rollingRate = vRolling,
			recent = vMatching.takeLast(kRecentCount).reversed().map { ThemeAttempt(it.puzzleId, it.playedAt, it.result == PuzzleResult.Success, it.puzzleRating) },
		)
	}

	/**
	 * Calcule le bilan de chaque thème rencontré, du plus joué au moins joué.
	 */
	private fun themeStats(inAttempts: List<AttemptEntity>): List<ThemeStat> {
		val vRatings = mutableMapOf<Theme, PlayerRating>()
		val vCounts = mutableMapOf<Theme, IntArray>()
		val vBest = mutableMapOf<Theme, Int>()
		val vDurations = mutableMapOf<Theme, MutableList<Long>>()
		for (vAttempt in inAttempts) {
			val vSuccess = vAttempt.result == PuzzleResult.Success
			for (vTheme in themesOf(vAttempt)) {
				val vCount = vCounts.getOrPut(vTheme) { IntArray(2) }
				vCount[0]++
				if (vSuccess) {
					vCount[1]++
					vBest[vTheme] = maxOf(vBest[vTheme] ?: 0, vAttempt.puzzleRating)
					vAttempt.durationMs?.let { vDurations.getOrPut(vTheme) { mutableListOf() }.add(it) }
				}
				if (vAttempt.rated) {
					vRatings[vTheme] = ratingSystem.update(vRatings[vTheme] ?: ratingSystem.initialRating, vAttempt.puzzleRating, vAttempt.result)
				}
			}
		}
		return vCounts.map { (vTheme, vCount) ->
			ThemeStat(
				theme = vTheme,
				attempts = vCount[0],
				successes = vCount[1],
				rating = (vRatings[vTheme] ?: ratingSystem.initialRating).rating.roundToInt(),
				bestSolved = vBest[vTheme],
				averageDurationMs = vDurations[vTheme]?.average()?.toLong(),
			)
		}.sortedByDescending { it.attempts }
	}

	/**
	 * Retourne les thèmes d'une tentative.
	 */
	private fun themesOf(inAttempt: AttemptEntity): ThemeSet = ThemeSet(inAttempt.themesLow, inAttempt.themesHigh)

	/**
	 * Retourne la série de jours consécutifs en cours (aujourd'hui ou hier compris) à partir des
	 * seules dates des tentatives (accueil : sans calculer tout le rapport).
	 */
	fun dayStreak(inPlayedAt: Collection<Long>, inNow: Long): Int = currentDayStreak(inPlayedAt.mapTo(HashSet()) { dayStart(it) }, inNow)

	/**
	 * Retourne la série de jours consécutifs en cours (aujourd'hui ou hier compris).
	 */
	private fun currentDayStreak(inDays: Set<Long>, inNow: Long): Int {
		var vDay = dayStart(inNow)
		if (vDay !in inDays) {
			vDay = previousDay(vDay)
		}
		var vCount = 0
		while (vDay in inDays) {
			vCount++
			vDay = previousDay(vDay)
		}
		return vCount
	}

	/**
	 * Retourne la plus longue série de jours consécutifs.
	 */
	private fun bestDayStreak(inDays: Set<Long>): Int {
		var vBest = 0
		var vCurrent = 0
		var vPrevious: Long? = null
		for (vDay in inDays.sorted()) {
			vCurrent = if (vPrevious != null && previousDay(vDay) == vPrevious) vCurrent + 1 else 1
			vBest = maxOf(vBest, vCurrent)
			vPrevious = vDay
		}
		return vBest
	}

	/**
	 * Retourne la plus longue suite de problèmes réussis d'affilée.
	 */
	private fun bestSuccessRun(inAttempts: List<AttemptEntity>): Int {
		var vBest = 0
		var vCurrent = 0
		for (vAttempt in inAttempts) {
			vCurrent = if (vAttempt.result == PuzzleResult.Success) vCurrent + 1 else 0
			vBest = maxOf(vBest, vCurrent)
		}
		return vBest
	}

	/**
	 * Retourne le calendrier des [kCalendarWeeks] dernières semaines, du lundi le plus ancien à
	 * aujourd'hui.
	 */
	private fun calendar(inAttempts: List<AttemptEntity>, inNow: Long): List<DayCount> {
		val vCounts = inAttempts.groupingBy { dayStart(it.playedAt) }.eachCount()
		val vToday = dayStart(inNow)
		var vDay = weekStart(inNow)
		repeat(kCalendarWeeks - 1) { vDay = addDays(vDay, -7) }
		val vResult = mutableListOf<DayCount>()
		while (vDay <= vToday) {
			vResult.add(DayCount(vDay, vCounts[vDay] ?: 0))
			vDay = addDays(vDay, 1)
		}
		return vResult
	}

	/**
	 * Retourne le volume des [kVolumeWeeks] dernières semaines.
	 */
	private fun weeklyVolume(inAttempts: List<AttemptEntity>, inNow: Long): List<WeekVolume> {
		val vByWeek = inAttempts.groupBy { weekStart(it.playedAt) }
		var vWeek = weekStart(inNow)
		val vWeeks = mutableListOf<Long>()
		repeat(kVolumeWeeks) {
			vWeeks.add(0, vWeek)
			vWeek = addDays(vWeek, -7)
		}
		return vWeeks.map { vStart ->
			val vList = vByWeek[vStart].orEmpty()
			WeekVolume(vStart, vList.size, vList.count { it.result == PuzzleResult.Success })
		}
	}

	/**
	 * Retourne le début du jour local d'une date.
	 */
	fun dayStart(inTime: Long): Long = calendarAt(inTime).apply {
		set(Calendar.HOUR_OF_DAY, 0)
		set(Calendar.MINUTE, 0)
		set(Calendar.SECOND, 0)
		set(Calendar.MILLISECOND, 0)
	}.timeInMillis

	/**
	 * Retourne le lundi de la semaine locale d'une date.
	 */
	fun weekStart(inTime: Long): Long {
		val vCalendar = calendarAt(dayStart(inTime))
		val vOffset = (vCalendar.get(Calendar.DAY_OF_WEEK) + 5) % 7
		vCalendar.add(Calendar.DAY_OF_YEAR, -vOffset)
		return vCalendar.timeInMillis
	}

	/**
	 * Ajoute des jours à un début de jour (en tenant compte des changements d'heure).
	 */
	private fun addDays(inDayStart: Long, inDays: Int): Long = calendarAt(inDayStart).apply { add(Calendar.DAY_OF_YEAR, inDays) }.timeInMillis

	/**
	 * Retourne le début du jour précédent.
	 */
	private fun previousDay(inDayStart: Long): Long = addDays(inDayStart, -1)

	/**
	 * Retourne l'heure locale d'une date (0 à 23).
	 */
	private fun hourOf(inTime: Long): Int = calendarAt(inTime).get(Calendar.HOUR_OF_DAY)

	/**
	 * Retourne un calendrier positionné sur une date, dans le fuseau du calculateur.
	 */
	private fun calendarAt(inTime: Long): Calendar = Calendar.getInstance(zone).apply { timeInMillis = inTime }

	/**
	 * Retourne la médiane d'une liste non vide.
	 */
	private fun median(inValues: List<Long>): Long {
		val vSorted = inValues.sorted()
		return if (vSorted.size % 2 == 1) vSorted[vSorted.size / 2] else (vSorted[vSorted.size / 2 - 1] + vSorted[vSorted.size / 2]) / 2
	}

	companion object {
		/** Largeur des tranches de difficulté. */
		const val kBandWidth = 200

		/** Nombre de semaines du calendrier d'activité. */
		const val kCalendarWeeks = 12

		/** Nombre de semaines du graphique de volume. */
		const val kVolumeWeeks = 8

		/** Nombre minimal de tentatives pour classer un thème parmi les plus faibles. */
		const val kMinAttemptsForWeak = 5

		/** Nombre de thèmes faibles proposés. */
		const val kWeakestCount = 3

		/** Nombre de catégories du coup de l'échec (1, 2, 3 et plus). */
		const val kFailedMoveBuckets = 3

		/** Fenêtre du taux de réussite glissant d'un thème. */
		const val kRollingWindow = 10

		/** Nombre de dernières tentatives affichées dans la fiche d'un thème. */
		const val kRecentCount = 10

		/** Familles dont les thèmes peuvent être proposés « à travailler » (motifs, mats, finales). */
		val kTrainableFamilies = setOf(ThemeFamily.Tactics, ThemeFamily.Mates, ThemeFamily.Endgames)

		/** Durée de 30 jours en millisecondes. */
		private const val kMonthMs = 30L * 24 * 60 * 60 * 1000
	}
}
