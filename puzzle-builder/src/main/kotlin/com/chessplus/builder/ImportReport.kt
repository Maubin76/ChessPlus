package com.chessplus.builder

import com.chessplus.core.model.Theme
import com.chessplus.core.model.ThemeSet
import java.io.File

/**
 * Compteurs et durées d'une génération, et leur mise en forme.
 */
class ImportReport {
	/** Nombre de lignes de données lues (en-tête exclu). */
	var rowCount: Long = 0

	/** Nombre de problèmes retenus. */
	var retainedCount: Long = 0

	/** Rejets par motif. */
	val rejections: MutableMap<RejectReason, Long> = RejectReason.entries.associateWith { 0L }.toMutableMap()

	/** Exemples de problèmes invalides (identifiant et raison), limités à [kMaxInvalidExamples]. */
	val invalidExamples: MutableList<String> = mutableListOf()

	/** Problèmes retenus par tranche de cote. */
	val retainedByBand: MutableMap<Int, Long> = sortedMapOf()

	/** Problèmes retenus par thème. */
	val retainedByTheme: MutableMap<Theme, Long> = mutableMapOf()

	/** Durées des étapes, dans l'ordre d'exécution (libellé, millisecondes). */
	val durations: MutableList<Pair<String, Long>> = mutableListOf()

	/** Fichier produit, renseigné à la fin. */
	var outputFile: File? = null

	/**
	 * Compte un rejet.
	 */
	fun reject(inReason: RejectReason) {
		rejections[inReason] = rejections.getValue(inReason) + 1
	}

	/**
	 * Compte un problème retenu dans sa tranche et ses thèmes.
	 */
	fun retain(inRating: Int, inThemes: ThemeSet) {
		retainedCount++
		val vBand = RatingBands.bandOf(inRating)
		retainedByBand[vBand] = (retainedByBand[vBand] ?: 0) + 1
		for (vTheme in inThemes) {
			retainedByTheme[vTheme] = (retainedByTheme[vTheme] ?: 0) + 1
		}
	}

	/**
	 * Exécute une étape en mesurant sa durée.
	 */
	inline fun <T> measure(inLabel: String, inBlock: () -> T): T {
		val vStart = System.nanoTime()
		val vResult = inBlock()
		durations.add(inLabel to (System.nanoTime() - vStart) / kNanosPerMilli)
		return vResult
	}

	/**
	 * Met en forme le rapport complet : rejets, répartition par tranche et par thème, durées, fichier.
	 */
	fun format(inOptions: BuilderOptions): String = buildString {
		appendLine("=== Génération de la base des problèmes ===")
		appendLine("Source : ${inOptions.source}")
		appendLine(
			"Seuils : RD <= ${inOptions.maxRatingDeviation}, popularité >= ${inOptions.minPopularity}, " +
				"parties >= ${inOptions.minPlays}, plafond par tranche ${if (inOptions.maxPerBand == 0) "aucun" else group(inOptions.maxPerBand.toLong())}, " +
				"validation ${if (inOptions.validate) "oui" else "non"}, index supprimés pendant l'insertion ${if (inOptions.dropIndexes) "oui" else "non"}"
		)
		appendLine()
		appendLine("Lignes lues : ${group(rowCount)}")
		for ((vReason, vCount) in rejections) {
			appendLine("  rejet, ${vReason.label} : ${group(vCount)}")
		}
		appendLine("Problèmes retenus : ${group(retainedCount)}")
		if (invalidExamples.isNotEmpty()) {
			appendLine("Exemples de solutions invalides :")
			invalidExamples.forEach { appendLine("  $it") }
		}
		appendLine()
		appendLine("Répartition par tranche de cote :")
		val vMaxBand = retainedByBand.values.maxOrNull() ?: 1L
		for ((vBand, vCount) in retainedByBand) {
			val vBar = "#".repeat(((vCount * kBarWidth) / vMaxBand).toInt())
			appendLine("  ${RatingBands.label(vBand).padStart(9)} : ${group(vCount).padStart(9)}  $vBar")
		}
		appendLine()
		appendLine("Répartition par thème (un problème a plusieurs thèmes) :")
		for ((vTheme, vCount) in retainedByTheme.entries.sortedByDescending { it.value }) {
			val vPercent = if (retainedCount == 0L) 0.0 else vCount * 100.0 / retainedCount
			appendLine("  ${vTheme.key.padEnd(20)} ${group(vCount).padStart(9)}  ${"%.1f".format(vPercent)} %")
		}
		val vMissing = Theme.entries.filter { it !in retainedByTheme }
		if (vMissing.isNotEmpty()) {
			appendLine("  absents : ${vMissing.joinToString(" ") { it.key }}")
		}
		appendLine()
		appendLine("Durées :")
		for ((vLabel, vMillis) in durations) {
			appendLine("  ${vLabel.padEnd(32)} ${"%.1f".format(vMillis / 1000.0)} s")
		}
		appendLine("  ${"total".padEnd(32)} ${"%.1f".format(durations.sumOf { it.second } / 1000.0)} s")
		outputFile?.let { inFile ->
			appendLine()
			appendLine("Fichier : ${inFile.path} (${"%.1f".format(inFile.length() / kBytesPerMegabyte)} Mo)")
		}
	}

	/**
	 * Formate un entier avec des espaces entre les milliers.
	 */
	private fun group(inValue: Long): String = inValue.toString().reversed().chunked(3).joinToString(" ").reversed()

	companion object {
		/** Nombre maximal d'exemples de problèmes invalides conservés. */
		const val kMaxInvalidExamples = 10

		/** Largeur maximale des barres de l'histogramme. */
		private const val kBarWidth = 40

		/** Nanosecondes par milliseconde. */
		const val kNanosPerMilli = 1_000_000L

		/** Octets par mégaoctet. */
		private const val kBytesPerMegabyte = 1024.0 * 1024.0
	}
}
