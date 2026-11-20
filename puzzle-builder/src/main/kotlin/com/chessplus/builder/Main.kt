package com.chessplus.builder

import com.chessplus.data.DataInfo
import kotlin.system.exitProcess

/** Code de sortie en cas d'arguments invalides. */
private const val kExitUsage = 2

/**
 * Point d'entrée du générateur de base de problèmes.
 *
 * Version provisoire de la phase 1 : vérifie les arguments et le câblage des modules.
 * La lecture du CSV et l'écriture de la base arrivent en phase 3.
 */
fun main(inArgs: Array<String>) {
	if (inArgs.isEmpty()) {
		System.err.println("Usage : puzzle-builder <chemin du .csv.zst> [options]")
		exitProcess(kExitUsage)
	}
	println("puzzle-builder : module ${DataInfo.kModuleName} disponible, ${DataInfo.coreThemeCount()} thèmes connus.")
	println("Fichier source : ${inArgs[0]} (génération non implémentée en phase 1)")
}
