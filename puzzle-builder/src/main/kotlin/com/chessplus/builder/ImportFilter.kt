package com.chessplus.builder

import com.chessplus.core.puzzle.LichessCsvRow

/**
 * Motif de rejet d'une ligne du CSV.
 *
 * @property label libellé affiché dans le rapport.
 */
enum class RejectReason(val label: String) {
	RatingDeviation("écart de cote trop élevé"),
	Popularity("popularité insuffisante"),
	Plays("trop peu de parties"),
	BandCap("plafond de la tranche atteint"),
	Invalid("solution invalide"),
}

/**
 * Tranches de cote de 200 points, utilisées pour le plafond et le rapport.
 */
object RatingBands {
	/** Largeur d'une tranche. */
	const val kBandWidth = 200

	/**
	 * Retourne le numéro de tranche d'une cote (0 pour 0-199, 1 pour 200-399…).
	 */
	fun bandOf(inRating: Int): Int = maxOf(0, inRating) / kBandWidth

	/**
	 * Retourne le libellé d'une tranche, par exemple `1400-1599`.
	 */
	fun label(inBand: Int): String = "${inBand * kBandWidth}-${inBand * kBandWidth + kBandWidth - 1}"
}

/**
 * Seuils de qualité appliqués à chaque ligne. Sous [lowRatingLimit], des seuils plus permissifs
 * s'appliquent : les problèmes faciles, moins joués, ont un écart de cote plus élevé, et les seuils
 * généraux n'en gardaient presque aucun (23 sous 500 points).
 *
 * @property maxRatingDeviation écart de cote maximal.
 * @property minPopularity popularité minimale.
 * @property minPlays nombre minimal de parties jouées.
 * @property lowRatingLimit cote sous laquelle s'appliquent les seuils des problèmes faciles (0 : jamais).
 * @property lowMaxRatingDeviation écart de cote maximal d'un problème facile.
 * @property lowMinPopularity popularité minimale d'un problème facile.
 * @property lowMinPlays nombre minimal de parties d'un problème facile.
 */
data class QualityThresholds(
	val maxRatingDeviation: Int,
	val minPopularity: Int,
	val minPlays: Int,
	val lowRatingLimit: Int = 0,
	val lowMaxRatingDeviation: Int = maxRatingDeviation,
	val lowMinPopularity: Int = minPopularity,
	val lowMinPlays: Int = minPlays,
) {
	/**
	 * Retourne le premier seuil non respecté par la ligne, ou null si elle les respecte tous.
	 */
	fun check(inRow: LichessCsvRow): RejectReason? {
		val vLow = inRow.rating < lowRatingLimit
		return when {
			inRow.ratingDeviation > (if (vLow) lowMaxRatingDeviation else maxRatingDeviation) -> RejectReason.RatingDeviation
			inRow.popularity < (if (vLow) lowMinPopularity else minPopularity) -> RejectReason.Popularity
			inRow.nbPlays < (if (vLow) lowMinPlays else minPlays) -> RejectReason.Plays
			else -> null
		}
	}
}

/**
 * Première passe du plafond par tranche : compte les lignes de qualité suffisante par
 * tranche et par popularité, pour garder ensuite les plus populaires de chaque tranche.
 *
 * @property maxPerBand plafond par tranche.
 */
class BandCapPlanner(private val maxPerBand: Int) {
	/** Comptes par tranche, indexés par popularité + 100. */
	private val histograms = mutableMapOf<Int, IntArray>()

	/**
	 * Compte une ligne qui respecte les seuils de qualité.
	 */
	fun count(inRow: LichessCsvRow) {
		val vHistogram = histograms.getOrPut(RatingBands.bandOf(inRow.rating)) { IntArray(kPopularityValues) }
		vHistogram[inRow.popularity.coerceIn(kMinPopularity, kMaxPopularity) - kMinPopularity]++
	}

	/**
	 * Calcule, pour chaque tranche, la popularité de coupure et le nombre de lignes à garder
	 * à cette popularité exacte (les premières rencontrées).
	 */
	fun plan(): BandCaps {
		val vCutoffs = mutableMapOf<Int, BandCutoff>()
		for ((vBand, vHistogram) in histograms) {
			var vRemaining = maxPerBand
			for (vPopularity in kMaxPopularity downTo kMinPopularity) {
				val vCount = vHistogram[vPopularity - kMinPopularity]
				if (vCount > vRemaining) {
					vCutoffs[vBand] = BandCutoff(vPopularity, vRemaining)
					break
				}
				vRemaining -= vCount
			}
		}
		return BandCaps(vCutoffs)
	}

	companion object {
		/** Popularité minimale possible dans le CSV. */
		private const val kMinPopularity = -100

		/** Popularité maximale possible dans le CSV. */
		private const val kMaxPopularity = 100

		/** Nombre de valeurs de popularité possibles. */
		private const val kPopularityValues = kMaxPopularity - kMinPopularity + 1
	}
}

/**
 * Coupure d'une tranche : on garde tout au-dessus de [popularity], et seulement
 * [quotaAtCutoff] lignes à cette popularité exacte.
 *
 * @property popularity popularité de coupure.
 * @property quotaAtCutoff nombre de lignes acceptées à la popularité de coupure.
 */
data class BandCutoff(val popularity: Int, val quotaAtCutoff: Int)

/**
 * Seconde passe du plafond par tranche : décide ligne par ligne selon les coupures.
 * Une tranche sans coupure n'atteint pas le plafond et garde tout.
 *
 * @property cutoffs coupures par tranche.
 */
class BandCaps(private val cutoffs: Map<Int, BandCutoff>) {
	/** Quotas restants à la popularité de coupure, par tranche. */
	private val remaining = cutoffs.mapValues { it.value.quotaAtCutoff }.toMutableMap()

	/**
	 * Indique si la ligne entre dans le plafond de sa tranche, et consomme le quota si besoin.
	 */
	fun tryAccept(inRow: LichessCsvRow): Boolean {
		val vBand = RatingBands.bandOf(inRow.rating)
		val vCutoff = cutoffs[vBand] ?: return true
		if (inRow.popularity > vCutoff.popularity) {
			return true
		}
		if (inRow.popularity < vCutoff.popularity) {
			return false
		}
		val vLeft = remaining.getValue(vBand)
		if (vLeft == 0) {
			return false
		}
		remaining[vBand] = vLeft - 1
		return true
	}

	companion object {
		/** Absence de plafond : tout est accepté. */
		val kNone = BandCaps(emptyMap())
	}
}
