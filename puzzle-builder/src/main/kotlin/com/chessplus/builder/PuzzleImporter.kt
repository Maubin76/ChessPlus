package com.chessplus.builder

import androidx.room.execSQL
import androidx.room.immediateTransaction
import androidx.room.useWriterConnection
import com.chessplus.core.model.ThemeSet
import com.chessplus.core.puzzle.LichessCsv
import com.chessplus.core.puzzle.LichessCsvRow
import com.chessplus.core.rules.ChessRules
import com.chessplus.data.createPuzzleDatabase
import com.chessplus.data.puzzle.PuzzleDao
import com.chessplus.data.puzzle.PuzzleDatabase
import com.chessplus.data.puzzle.PuzzleEntity
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * Génère la base des problèmes à partir du CSV Lichess.
 *
 * Étapes : première passe pour calculer les plafonds par tranche (si demandés), création de
 * la base via Room, seconde passe qui filtre, valide et insère par lots dans une seule
 * transaction, recréation éventuelle des index, `VACUUM`, puis copie vers le fichier final.
 *
 * @property options paramètres du builder.
 * @property rules règles des échecs pour valider les solutions.
 * @property log affichage des messages de progression.
 */
class PuzzleImporter(
	private val options: BuilderOptions,
	rules: ChessRules,
	private val log: (String) -> Unit = {},
) {
	/** Validation des solutions. */
	private val validator = PuzzleValidator(rules)

	/** Seuils de qualité. */
	private val thresholds = QualityThresholds(options.maxRatingDeviation, options.minPopularity, options.minPlays)

	/**
	 * Exécute la génération complète et retourne le rapport.
	 *
	 * @throws IllegalStateException si une ligne est mal formée ou contient un thème inconnu.
	 */
	suspend fun run(): ImportReport {
		val vReport = ImportReport()
		val vCaps = if (options.maxPerBand > 0) {
			vReport.measure("passe 1 (plafonds par tranche)") { planCaps() }
		} else {
			BandCaps.kNone
		}
		prepareWorkFile()
		val vDatabase = createPuzzleDatabase(options.workFile)
		try {
			val vIndexes = if (options.dropIndexes) vReport.measure("suppression des index") { dropIndexes(vDatabase) } else emptyList()
			vReport.measure("passe 2 (lecture, validation, insertion)") { insertAll(vDatabase, vCaps, vReport) }
			if (vIndexes.isNotEmpty()) {
				vReport.measure("recréation des index") { recreateIndexes(vDatabase, vIndexes) }
			}
			vReport.measure("VACUUM") { vDatabase.useWriterConnection { it.execSQL("VACUUM") } }
		} finally {
			vDatabase.close()
		}
		vReport.measure("copie vers ${options.output.path}") { copyToOutput() }
		vReport.outputFile = options.output
		return vReport
	}

	/**
	 * Première passe : compte les lignes de qualité suffisante par tranche et par popularité.
	 */
	private fun planCaps(): BandCaps {
		log("Passe 1 : calcul des plafonds par tranche…")
		val vPlanner = BandCapPlanner(options.maxPerBand)
		CsvSource.useDataLines(options.source) { inLines ->
			for ((vIndex, vLine) in inLines.withIndex()) {
				val vRow = parse(vLine, vIndex)
				if (thresholds.check(vRow) == null) {
					vPlanner.count(vRow)
				}
			}
		}
		return vPlanner.plan()
	}

	/**
	 * Seconde passe : filtre, valide et insère les problèmes par lots, dans une seule transaction.
	 */
	private suspend fun insertAll(inDatabase: PuzzleDatabase, inCaps: BandCaps, inReport: ImportReport) {
		log("Passe 2 : lecture, validation et insertion…")
		val vDao = inDatabase.puzzleDao()
		inDatabase.useWriterConnection { inTransactor ->
			inTransactor.immediateTransaction {
				val vBatch = ArrayList<PuzzleEntity>(options.batchSize)
				CsvSource.useDataLines(options.source) { inLines ->
					for ((vIndex, vLine) in inLines.withIndex()) {
						inReport.rowCount++
						if (inReport.rowCount % kProgressStep == 0L) {
							log("  ${inReport.rowCount} lignes lues, ${inReport.retainedCount} retenues")
						}
						val vEntity = select(parse(vLine, vIndex), vIndex, inCaps, inReport) ?: continue
						vBatch.add(vEntity)
						if (vBatch.size == options.batchSize) {
							flush(vDao, vBatch)
						}
					}
				}
				flush(vDao, vBatch)
			}
		}
	}

	/**
	 * Applique les filtres à une ligne et retourne l'entité à insérer, ou null si elle est rejetée.
	 * Les thèmes sont lus pour toutes les lignes : un thème inconnu fait échouer la génération.
	 */
	private fun select(inRow: LichessCsvRow, inIndex: Int, inCaps: BandCaps, inReport: ImportReport): PuzzleEntity? {
		val vThemes = try {
			ThemeSet.parse(inRow.themes)
		} catch (vError: IllegalArgumentException) {
			throw IllegalStateException("Ligne ${inIndex + 2} : ${vError.message}", vError)
		}
		val vReason = thresholds.check(inRow) ?: if (inCaps.tryAccept(inRow)) null else RejectReason.BandCap
		if (vReason != null) {
			inReport.reject(vReason)
			return null
		}
		val vPuzzle = try {
			inRow.toPuzzle()
		} catch (vError: IllegalArgumentException) {
			throw IllegalStateException("Ligne ${inIndex + 2} : ${vError.message}", vError)
		}
		if (options.validate) {
			val vProblem = validator.problem(vPuzzle)
			if (vProblem != null) {
				inReport.reject(RejectReason.Invalid)
				if (inReport.invalidExamples.size < ImportReport.kMaxInvalidExamples) {
					inReport.invalidExamples.add("${vPuzzle.id} : $vProblem")
				}
				return null
			}
		}
		inReport.retain(vPuzzle.rating, vThemes)
		return PuzzleEntity.fromPuzzle(vPuzzle)
	}

	/**
	 * Insère le lot courant puis le vide.
	 */
	private suspend fun flush(inDao: PuzzleDao, inBatch: MutableList<PuzzleEntity>) {
		if (inBatch.isNotEmpty()) {
			inDao.insertAll(inBatch.toList())
			inBatch.clear()
		}
	}

	/**
	 * Découpe une ligne en signalant son numéro (en-tête = ligne 1) en cas d'erreur.
	 */
	private fun parse(inLine: String, inIndex: Int): LichessCsvRow = try {
		LichessCsv.parseLine(inLine)
	} catch (vError: IllegalArgumentException) {
		throw IllegalStateException("Ligne ${inIndex + 2} : ${vError.message}", vError)
	}

	/**
	 * Supprime les index de la table des problèmes et retourne leur SQL de création, pour les
	 * recréer à l'identique.
	 */
	private suspend fun dropIndexes(inDatabase: PuzzleDatabase): List<String> = inDatabase.useWriterConnection { inTransactor ->
		val vIndexes = inTransactor.usePrepared(
			"SELECT name, sql FROM sqlite_master WHERE type = 'index' AND tbl_name = '${PuzzleEntity.kTableName}' AND sql IS NOT NULL"
		) { inStatement ->
			buildList { while (inStatement.step()) add(inStatement.getText(0) to inStatement.getText(1)) }
		}
		vIndexes.forEach { inTransactor.execSQL("DROP INDEX `${it.first}`") }
		vIndexes.map { it.second }
	}

	/**
	 * Recrée les index à partir de leur SQL d'origine.
	 */
	private suspend fun recreateIndexes(inDatabase: PuzzleDatabase, inSql: List<String>) {
		inDatabase.useWriterConnection { inTransactor -> inSql.forEach { inTransactor.execSQL(it) } }
	}

	/**
	 * Supprime un éventuel fichier de travail précédent et ses journaux.
	 */
	private fun prepareWorkFile() {
		options.workFile.absoluteFile.parentFile.mkdirs()
		for (vSuffix in listOf("", "-journal", "-wal", "-shm")) {
			File(options.workFile.path + vSuffix).delete()
		}
	}

	/**
	 * Copie la base terminée vers le fichier final.
	 */
	private fun copyToOutput() {
		options.output.absoluteFile.parentFile.mkdirs()
		Files.copy(options.workFile.toPath(), options.output.toPath(), StandardCopyOption.REPLACE_EXISTING)
	}

	companion object {
		/** Nombre de lignes entre deux messages de progression. */
		private const val kProgressStep = 500_000L
	}
}
