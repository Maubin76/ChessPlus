package com.chessplus.core.puzzle

import com.chessplus.core.model.Color
import com.chessplus.core.model.Move
import com.chessplus.core.model.ThemeSet

/**
 * Problème d'échecs issu de la base Lichess.
 *
 * @property id identifiant Lichess (`lichess.org/training/{id}`).
 * @property fen position de départ, **avant** le coup de l'adversaire.
 * @property moves coups du problème : le premier est celui de l'adversaire, la suite est la solution.
 * @property rating difficulté (Glicko-2 Lichess).
 * @property themes thèmes du problème.
 */
data class Puzzle(
	val id: String,
	val fen: String,
	val moves: List<Move>,
	val rating: Int,
	val themes: ThemeSet,
) {
	init {
		require(id.isNotEmpty()) { "Identifiant de problème vide" }
		require(moves.size >= 2 && moves.size % 2 == 0) { "Le problème $id doit compter un nombre pair de coups (${moves.size})" }
	}

	/** Camp au trait dans la FEN, c'est-à-dire celui de l'adversaire. */
	val opponentColor: Color
		get() = sideToMoveInFen(fen)

	/** Camp du joueur : toujours l'inverse du camp au trait dans la FEN. */
	val playerColor: Color
		get() = opponentColor.opposite()

	/** Nombre de coups que le joueur doit trouver. */
	val playerMoveCount: Int
		get() = moves.size / 2

	/** Adresse du problème sur Lichess. */
	val lichessUrl: String
		get() = "$kLichessTrainingUrl$id"

	companion object {
		/** Préfixe de l'adresse d'un problème sur Lichess. */
		const val kLichessTrainingUrl = "https://lichess.org/training/"

		/**
		 * Retourne le camp au trait indiqué par le deuxième champ d'une FEN.
		 *
		 * @throws IllegalArgumentException si la FEN ne contient pas de camp au trait valide.
		 */
		fun sideToMoveInFen(inFen: String): Color {
			val vFields = inFen.trim().split(' ')
			require(vFields.size >= 2) { "FEN sans camp au trait : '$inFen'" }
			return when (vFields[1]) {
				"w" -> Color.White
				"b" -> Color.Black
				else -> throw IllegalArgumentException("Camp au trait invalide dans la FEN : '$inFen'")
			}
		}
	}
}
