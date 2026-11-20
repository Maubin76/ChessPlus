package com.chessplus.builder

import com.chessplus.core.puzzle.Puzzle
import com.chessplus.core.rules.ChessRules

/**
 * Vérifie qu'un problème est jouable : FEN valide et coups tous légaux dans l'ordre.
 *
 * @property rules règles des échecs utilisées pour rejouer la solution.
 */
class PuzzleValidator(private val rules: ChessRules) {
	/**
	 * Retourne la raison de l'invalidité du problème, ou null s'il est valide.
	 */
	fun problem(inPuzzle: Puzzle): String? {
		val vPosition = try {
			rules.positionFromFen(inPuzzle.fen)
		} catch (vError: IllegalArgumentException) {
			return "FEN invalide (${vError.message})"
		}
		for ((vIndex, vMove) in inPuzzle.moves.withIndex()) {
			if (!vPosition.isLegal(vMove)) {
				return "coup ${vIndex + 1} illégal : $vMove"
			}
			vPosition.play(vMove)
		}
		return null
	}
}
