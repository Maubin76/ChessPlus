package com.chessplus.core.rules

import com.chessplus.core.model.Color
import com.chessplus.core.model.Move
import com.chessplus.core.model.Piece
import com.chessplus.core.model.Square

/**
 * Point d'entrée des règles des échecs.
 *
 * Seule cette interface et [ChessPosition] sont visibles du reste du projet : aucune
 * bibliothèque externe n'apparaît en dehors de l'implémentation, qui peut être remplacée
 * par une version Kotlin pure sans autre changement.
 */
interface ChessRules {
	/**
	 * Crée une position à partir d'une FEN.
	 *
	 * @throws IllegalArgumentException si la FEN n'est pas valide.
	 */
	fun positionFromFen(inFen: String): ChessPosition
}

/**
 * État d'une partie du point de vue du camp au trait.
 */
enum class GameStatus {
	/** La partie continue, le camp au trait n'est pas en échec. */
	Ongoing,

	/** Le camp au trait est en échec mais peut jouer. */
	Check,

	/** Le camp au trait est mat. */
	Checkmate,

	/** Le camp au trait est pat. */
	Stalemate,
}

/**
 * Vue en lecture seule d'une position, destinée à l'affichage.
 */
interface PositionView {
	/** Camp au trait. */
	val sideToMove: Color

	/**
	 * Retourne la FEN de la position courante.
	 */
	fun fen(): String

	/**
	 * Retourne la pièce posée sur une case, ou null si la case est vide.
	 */
	fun pieceAt(inSquare: Square): Piece?

	/**
	 * Liste les coups légaux du camp au trait depuis une case (vide si aucun).
	 */
	fun legalMovesFrom(inSquare: Square): List<Move>

	/**
	 * Liste tous les coups légaux du camp au trait.
	 */
	fun legalMoves(): List<Move>

	/**
	 * Retourne l'état de la partie pour le camp au trait.
	 */
	fun status(): GameStatus
}

/**
 * Position modifiable : on peut y jouer et y annuler des coups.
 */
interface ChessPosition : PositionView {
	/** Coups joués depuis la position de départ, du plus ancien au plus récent. */
	val history: List<Move>

	/**
	 * Indique si un coup est légal pour le camp au trait.
	 */
	fun isLegal(inMove: Move): Boolean

	/**
	 * Joue un coup légal.
	 *
	 * @throws IllegalMoveException si le coup n'est pas légal.
	 */
	fun play(inMove: Move)

	/**
	 * Annule le dernier coup joué et le retourne.
	 *
	 * @throws IllegalStateException si aucun coup n'a été joué.
	 */
	fun undo(): Move

	/**
	 * Retourne une copie indépendante de la position, historique compris.
	 */
	fun copy(): ChessPosition
}

/**
 * Levée quand on tente de jouer un coup illégal.
 *
 * @property move coup refusé.
 * @property fen position dans laquelle le coup a été refusé.
 */
class IllegalMoveException(val move: Move, val fen: String) : IllegalArgumentException("Coup illégal $move dans la position $fen")
