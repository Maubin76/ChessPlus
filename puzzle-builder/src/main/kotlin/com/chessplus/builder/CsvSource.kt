package com.chessplus.builder

import com.chessplus.core.puzzle.LichessCsv
import com.github.luben.zstd.ZstdInputStream
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.nio.charset.StandardCharsets

/**
 * Lecture en flux du CSV Lichess compressé en zstd, sans le charger en mémoire ni le
 * décompresser sur disque.
 */
object CsvSource {
	/** Taille des tampons de lecture. */
	const val kBufferSize = 1 shl 16

	/**
	 * Ouvre le fichier, vérifie l'en-tête et passe les lignes de données à [inBlock].
	 * La fonction est `inline` pour que [inBlock] puisse appeler des fonctions suspendues.
	 *
	 * @throws IllegalArgumentException si le fichier est vide ou si l'en-tête est inattendu.
	 */
	inline fun <T> useDataLines(inFile: File, inBlock: (Sequence<String>) -> T): T {
		require(inFile.isFile) { "Fichier introuvable : $inFile" }
		val vStream = ZstdInputStream(BufferedInputStream(FileInputStream(inFile), kBufferSize))
		return vStream.bufferedReader(StandardCharsets.UTF_8).use { inReader ->
			val vHeader = inReader.readLine() ?: throw IllegalArgumentException("Fichier vide : $inFile")
			require(vHeader == LichessCsv.kHeader) { "En-tête inattendu : $vHeader" }
			inBlock(inReader.lineSequence())
		}
	}
}
