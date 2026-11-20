package com.chessplus.core

import com.github.bhlangonijr.chesslib.Board

/**
 * Sonde de la phase 1 : vérifie que le source set jvmShared est compilé pour Android et la JVM
 * et qu'il voit chesslib. Supprimée en phase 2 au profit de l'implémentation des règles.
 */
internal object JvmSharedProbe {
	/**
	 * Retourne la FEN de la position initiale calculée par chesslib.
	 */
	fun startingFen(): String {
		val vBoard = Board()
		return vBoard.fen
	}
}
