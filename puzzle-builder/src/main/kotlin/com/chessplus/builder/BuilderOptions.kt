package com.chessplus.builder

import java.io.File

/**
 * Paramètres du builder, lus sur la ligne de commande.
 *
 * @property source fichier `.csv.zst` de Lichess.
 * @property maxRatingDeviation écart de cote maximal d'un problème.
 * @property minPopularity popularité minimale (de -100 à 100).
 * @property minPlays nombre minimal de parties jouées.
 * @property maxPerBand nombre maximal de problèmes par tranche de 200 points (0 : pas de plafond).
 * @property output fichier final, copié dans les assets de l'application.
 * @property workFile fichier de travail dans lequel la base est construite.
 * @property batchSize nombre de problèmes par insertion.
 * @property dropIndexes supprime les index avant l'insertion et les recrée à l'identique ensuite
 *   (par défaut : environ 10 s gagnées sur 37 avec l'index de sélection couvrant).
 * @property validate rejoue chaque solution avec les règles des échecs pour écarter les problèmes invalides.
 */
data class BuilderOptions(
	val source: File,
	val maxRatingDeviation: Int = kDefaultMaxRatingDeviation,
	val minPopularity: Int = kDefaultMinPopularity,
	val minPlays: Int = kDefaultMinPlays,
	val maxPerBand: Int = kDefaultMaxPerBand,
	val output: File = File(kDefaultOutput),
	val workFile: File = File(kDefaultWorkFile),
	val batchSize: Int = kDefaultBatchSize,
	val dropIndexes: Boolean = true,
	val validate: Boolean = true,
) {
	companion object {
		/** Écart de cote maximal par défaut. */
		const val kDefaultMaxRatingDeviation = 80

		/** Popularité minimale par défaut. */
		const val kDefaultMinPopularity = 90

		/** Nombre minimal de parties par défaut. */
		const val kDefaultMinPlays = 250

		/** Plafond par tranche de 200 points par défaut. */
		const val kDefaultMaxPerBand = 30_000

		/** Fichier final par défaut, relatif à la racine du dépôt. */
		const val kDefaultOutput = "app/src/main/assets/puzzles.db"

		/** Fichier de travail par défaut, relatif à la racine du dépôt. */
		const val kDefaultWorkFile = "puzzle-builder/build/work/puzzles.db"

		/** Taille de lot par défaut. */
		const val kDefaultBatchSize = 1_000

		/** Aide affichée en cas d'erreur sur les arguments. */
		val kUsage = """
			Usage : puzzle-builder <chemin du .csv.zst> [options]
			  --max-rd=N          écart de cote maximal (défaut $kDefaultMaxRatingDeviation)
			  --min-popularity=N  popularité minimale, de -100 à 100 (défaut $kDefaultMinPopularity)
			  --min-plays=N       nombre minimal de parties jouées (défaut $kDefaultMinPlays)
			  --max-per-band=N    plafond par tranche de 200 points, 0 pour aucun (défaut $kDefaultMaxPerBand)
			  --output=FICHIER    base produite (défaut $kDefaultOutput)
			  --work-file=FICHIER fichier de travail (défaut $kDefaultWorkFile)
			  --batch-size=N      problèmes par insertion (défaut $kDefaultBatchSize)
			  --keep-indexes      garde les index pendant l'insertion (par défaut, ils sont
			                      supprimés puis recréés à l'identique, ce qui est plus rapide)
			  --no-validate       ne rejoue pas les solutions avec les règles des échecs
		""".trimIndent()

		/**
		 * Lit les arguments de la ligne de commande.
		 *
		 * @throws IllegalArgumentException si un argument est inconnu, manquant ou invalide.
		 */
		fun parse(inArgs: Array<String>): BuilderOptions {
			val vPositional = inArgs.filter { !it.startsWith("--") }
			require(vPositional.size == 1) { "Un seul chemin de fichier .csv.zst attendu (${vPositional.size} trouvés)" }
			var vOptions = BuilderOptions(source = File(vPositional[0]))
			for (vArg in inArgs.filter { it.startsWith("--") }) {
				val vName = vArg.substringBefore('=')
				val vValue = if (vArg.contains('=')) vArg.substringAfter('=') else null
				vOptions = when (vName) {
					"--max-rd" -> vOptions.copy(maxRatingDeviation = intValue(vName, vValue, 0..1000))
					"--min-popularity" -> vOptions.copy(minPopularity = intValue(vName, vValue, -100..100))
					"--min-plays" -> vOptions.copy(minPlays = intValue(vName, vValue, 0..Int.MAX_VALUE))
					"--max-per-band" -> vOptions.copy(maxPerBand = intValue(vName, vValue, 0..Int.MAX_VALUE))
					"--output" -> vOptions.copy(output = File(textValue(vName, vValue)))
					"--work-file" -> vOptions.copy(workFile = File(textValue(vName, vValue)))
					"--batch-size" -> vOptions.copy(batchSize = intValue(vName, vValue, 1..100_000))
					"--keep-indexes" -> vOptions.copy(dropIndexes = !flag(vName, vValue))
					"--no-validate" -> vOptions.copy(validate = !flag(vName, vValue))
					else -> throw IllegalArgumentException("Option inconnue : $vArg")
				}
			}
			require(vOptions.output.absoluteFile != vOptions.workFile.absoluteFile) { "Le fichier de travail doit différer du fichier produit" }
			return vOptions
		}

		/**
		 * Lit la valeur entière d'une option et vérifie son intervalle.
		 */
		private fun intValue(inName: String, inValue: String?, inRange: IntRange): Int {
			val vInt = inValue?.toIntOrNull() ?: throw IllegalArgumentException("$inName attend un entier : $inName=N")
			require(vInt in inRange) { "$inName hors limites ($inRange) : $vInt" }
			return vInt
		}

		/**
		 * Lit la valeur texte non vide d'une option.
		 */
		private fun textValue(inName: String, inValue: String?): String {
			require(!inValue.isNullOrEmpty()) { "$inName attend une valeur : $inName=..." }
			return inValue
		}

		/**
		 * Vérifie qu'une option booléenne n'a pas de valeur et retourne vrai.
		 */
		private fun flag(inName: String, inValue: String?): Boolean {
			require(inValue == null) { "$inName ne prend pas de valeur" }
			return true
		}
	}
}
