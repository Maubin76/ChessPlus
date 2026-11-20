package com.chessplus.core.rules

import com.chessplus.core.model.Color
import com.chessplus.core.model.Move
import com.chessplus.core.model.Piece
import com.chessplus.core.model.PieceType
import com.chessplus.core.model.Square
import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.Side
import com.github.bhlangonijr.chesslib.move.Move as LibMove
import com.github.bhlangonijr.chesslib.Piece as LibPiece
import com.github.bhlangonijr.chesslib.PieceType as LibPieceType
import com.github.bhlangonijr.chesslib.Square as LibSquare

/**
 * Implémentation des règles des échecs avec chesslib (bhlangonijr).
 */
actual fun defaultChessRules(): ChessRules = ChesslibRules

/**
 * Règles des échecs fournies par chesslib. Les types chesslib ne sortent pas de ce fichier.
 */
internal object ChesslibRules : ChessRules {
	/** Nombre de champs d'une FEN complète. */
	private const val kFenFieldCount = 6

	/**
	 * Crée une position chesslib à partir d'une FEN, après une validation minimale que
	 * chesslib ne fait pas (il accepte silencieusement des FEN incomplètes).
	 */
	override fun positionFromFen(inFen: String): ChessPosition {
		val vFields = inFen.trim().split(' ')
		require(vFields.size == kFenFieldCount) { "FEN invalide (6 champs attendus) : '$inFen'" }
		require(vFields[1] == "w" || vFields[1] == "b") { "FEN invalide (camp au trait) : '$inFen'" }
		val vRows = vFields[0].split('/')
		require(vRows.size == Square.kSide) { "FEN invalide (8 rangées attendues) : '$inFen'" }
		val vBoard = Board()
		try {
			vBoard.loadFromFen(inFen.trim())
		} catch (vError: RuntimeException) {
			throw IllegalArgumentException("FEN invalide : '$inFen'", vError)
		}
		return ChesslibPosition(vBoard, mutableListOf())
	}
}

/**
 * Position adossée à un `Board` chesslib.
 *
 * L'historique est tenu ici plutôt que lu dans chesslib, pour ne dépendre que des types du projet.
 */
private class ChesslibPosition(
	private val board: Board,
	private val playedMoves: MutableList<Move>,
) : ChessPosition {

	/** Camp au trait, lu dans chesslib. */
	override val sideToMove: Color
		get() = board.sideToMove.toColor()

	/** Copie de l'historique des coups joués. */
	override val history: List<Move>
		get() = playedMoves.toList()

	/**
	 * Retourne la FEN produite par chesslib.
	 */
	override fun fen(): String = board.fen

	/**
	 * Retourne la pièce de la case, ou null si elle est vide.
	 */
	override fun pieceAt(inSquare: Square): Piece? = board.getPiece(inSquare.toLib()).toPiece()

	/**
	 * Filtre les coups légaux chesslib sur leur case de départ.
	 */
	override fun legalMovesFrom(inSquare: Square): List<Move> {
		val vFrom = inSquare.toLib()
		return board.legalMoves().filter { it.from == vFrom }.map { it.toMove() }
	}

	/**
	 * Convertit tous les coups légaux chesslib.
	 */
	override fun legalMoves(): List<Move> = board.legalMoves().map { it.toMove() }

	/**
	 * Déduit l'état de la partie de l'échec au roi et de l'existence de coups légaux.
	 */
	override fun status(): GameStatus {
		val vHasMoves = board.legalMoves().isNotEmpty()
		val vInCheck = board.isKingAttacked
		return when {
			vInCheck && !vHasMoves -> GameStatus.Checkmate
			!vHasMoves -> GameStatus.Stalemate
			vInCheck -> GameStatus.Check
			else -> GameStatus.Ongoing
		}
	}

	/**
	 * Cherche le coup parmi les coups légaux chesslib.
	 */
	override fun isLegal(inMove: Move): Boolean = board.legalMoves().contains(inMove.toLib(board.sideToMove))

	/**
	 * Vérifie la légalité du coup avant de le jouer : chesslib ne le fait pas par défaut.
	 */
	override fun play(inMove: Move) {
		val vLibMove = inMove.toLib(board.sideToMove)
		if (!board.legalMoves().contains(vLibMove)) {
			throw IllegalMoveException(inMove, fen())
		}
		board.doMove(vLibMove)
		playedMoves.add(inMove)
	}

	/**
	 * Annule le dernier coup dans chesslib et dans l'historique.
	 */
	override fun undo(): Move {
		check(playedMoves.isNotEmpty()) { "Aucun coup à annuler" }
		board.undoMove()
		return playedMoves.removeAt(playedMoves.lastIndex)
	}

	/**
	 * Clone le plateau chesslib et l'historique.
	 */
	override fun copy(): ChessPosition = ChesslibPosition(board.clone(), playedMoves.toMutableList())
}

/**
 * Convertit une case du projet en case chesslib (même numérotation, de a1 à h8).
 */
private fun Square.toLib(): LibSquare = LibSquare.squareAt(index)

/**
 * Convertit une case chesslib en case du projet.
 */
private fun LibSquare.toSquare(): Square = Square(ordinal)

/**
 * Convertit un camp chesslib en camp du projet.
 */
private fun Side.toColor(): Color = if (this == Side.WHITE) Color.White else Color.Black

/**
 * Convertit un type de pièce du projet en type chesslib.
 */
private fun PieceType.toLib(): LibPieceType = when (this) {
	PieceType.Pawn -> LibPieceType.PAWN
	PieceType.Knight -> LibPieceType.KNIGHT
	PieceType.Bishop -> LibPieceType.BISHOP
	PieceType.Rook -> LibPieceType.ROOK
	PieceType.Queen -> LibPieceType.QUEEN
	PieceType.King -> LibPieceType.KING
}

/**
 * Convertit un type de pièce chesslib en type du projet, ou null pour `NONE`.
 */
private fun LibPieceType?.toPieceType(): PieceType? = when (this) {
	LibPieceType.PAWN -> PieceType.Pawn
	LibPieceType.KNIGHT -> PieceType.Knight
	LibPieceType.BISHOP -> PieceType.Bishop
	LibPieceType.ROOK -> PieceType.Rook
	LibPieceType.QUEEN -> PieceType.Queen
	LibPieceType.KING -> PieceType.King
	else -> null
}

/**
 * Convertit une pièce chesslib en pièce du projet, ou null pour une case vide.
 */
private fun LibPiece.toPiece(): Piece? {
	if (this == LibPiece.NONE) {
		return null
	}
	val vType = pieceType.toPieceType() ?: return null
	return Piece(pieceSide.toColor(), vType)
}

/**
 * Convertit un coup du projet en coup chesslib ; la pièce de promotion prend la couleur du camp au trait.
 */
private fun Move.toLib(inSide: Side): LibMove {
	val vPromotion = promotion?.let { LibPiece.make(inSide, it.toLib()) } ?: LibPiece.NONE
	return LibMove(from.toLib(), to.toLib(), vPromotion)
}

/**
 * Convertit un coup chesslib en coup du projet.
 */
private fun LibMove.toMove(): Move {
	val vPromotion = if (promotion == null || promotion == LibPiece.NONE) null else promotion.pieceType.toPieceType()
	return Move(from.toSquare(), to.toSquare(), vPromotion)
}
