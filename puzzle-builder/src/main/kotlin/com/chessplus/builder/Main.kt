package com.chessplus.builder

import com.chessplus.core.rules.defaultChessRules
import kotlinx.coroutines.runBlocking
import kotlin.system.exitProcess

/** Code de sortie en cas d'arguments invalides. */
private const val kExitUsage = 2

/** Code de sortie en cas d'échec de la génération. */
private const val kExitFailure = 1

/**
 * Point d'entrée du générateur de la base des problèmes.
 *
 * Usage : `./gradlew :puzzle-builder:run --args="<chemin du .csv.zst> [options]"`.
 */
fun main(inArgs: Array<String>) {
	val vOptions = try {
		BuilderOptions.parse(inArgs)
	} catch (vError: IllegalArgumentException) {
		System.err.println("Erreur : ${vError.message}")
		System.err.println(BuilderOptions.kUsage)
		exitProcess(kExitUsage)
	}
	try {
		val vReport = runBlocking { PuzzleImporter(vOptions, defaultChessRules()) { println(it) }.run() }
		println()
		println(vReport.format(vOptions))
	} catch (vError: Exception) {
		System.err.println("Échec de la génération : ${vError.message}")
		exitProcess(kExitFailure)
	}
}
