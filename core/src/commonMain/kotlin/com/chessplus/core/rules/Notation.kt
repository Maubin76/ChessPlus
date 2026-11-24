package com.chessplus.core.rules

import com.chessplus.core.model.Color
import com.chessplus.core.model.Move
import com.chessplus.core.model.PieceType
import kotlin.math.abs

/** Indice du champ « numéro de coup » dans une FEN. */
private const val kFullmoveField = 5

/** Points de suspension qui suivent le numéro d'un coup noir. */
private const val kEllipsis = '…'

/**
 * Coup numéroté d'une suite, pour l'affichage.
 *
 * @property number numéro du coup (champ « fullmove » de la FEN).
 * @property color camp qui a joué le coup.
 * @property san notation algébrique abrégée du coup.
 */
data class NumberedMove(val number: Int, val color: Color, val san: String)

/**
 * Retourne la notation algébrique abrégée (SAN, lettres anglaises) du coup légal [inMove], avant
 * qu'il soit joué. La position n'est pas modifiée.
 */
fun ChessPosition.san(inMove: Move): String {
	val vPiece = pieceAt(inMove.from) ?: return inMove.toUci()
	val vBuilder = StringBuilder()
	if (vPiece.type == PieceType.King && abs(inMove.to.file - inMove.from.file) == 2) {
		vBuilder.append(if (inMove.to.file > inMove.from.file) "O-O" else "O-O-O")
	} else {
		// Une prise en passant arrive sur une case vide : seul le changement de colonne du pion la révèle.
		val vCapture = pieceAt(inMove.to) != null || (vPiece.type == PieceType.Pawn && inMove.from.file != inMove.to.file)
		if (vPiece.type == PieceType.Pawn) {
			if (vCapture) {
				vBuilder.append('a' + inMove.from.file).append('x')
			}
			vBuilder.append(inMove.to.toAlgebraic())
			inMove.promotion?.let { vBuilder.append('=').append(it.fenChar.uppercaseChar()) }
		} else {
			vBuilder.append(vPiece.type.fenChar.uppercaseChar())
			vBuilder.append(disambiguation(inMove, vPiece.type))
			if (vCapture) {
				vBuilder.append('x')
			}
			vBuilder.append(inMove.to.toAlgebraic())
		}
	}
	val vAfter = copy()
	vAfter.play(inMove)
	when (vAfter.status()) {
		GameStatus.Checkmate -> vBuilder.append('#')
		GameStatus.Check -> vBuilder.append('+')
		else -> Unit
	}
	return vBuilder.toString()
}

/**
 * Retourne la désambiguïsation minimale d'un coup de pièce : rien, la colonne, la rangée, ou la case
 * de départ, selon les autres pièces de même type qui peuvent aller sur la même case.
 */
private fun ChessPosition.disambiguation(inMove: Move, inType: PieceType): String {
	val vRivals = legalMoves()
		.filter { it.to == inMove.to && it.from != inMove.from && pieceAt(it.from)?.type == inType }
		.map { it.from }
		.distinct()
	if (vRivals.isEmpty()) {
		return ""
	}
	val vFile = "${'a' + inMove.from.file}"
	val vRank = "${'1' + inMove.from.rank}"
	return when {
		vRivals.none { it.file == inMove.from.file } -> vFile
		vRivals.none { it.rank == inMove.from.rank } -> vRank
		else -> vFile + vRank
	}
}

/**
 * Joue la suite [inMoves] depuis [inFen] et retourne chaque coup numéroté, avec le camp qui l'a joué.
 * Le numéro part du champ « fullmove » de la FEN et augmente après chaque coup noir.
 */
fun sanLine(inRules: ChessRules, inFen: String, inMoves: List<Move>): List<NumberedMove> {
	val vPosition = inRules.positionFromFen(inFen)
	var vNumber = inFen.trim().split(Regex("\\s+")).getOrNull(kFullmoveField)?.toIntOrNull() ?: 1
	val vResult = mutableListOf<NumberedMove>()
	for (vMove in inMoves) {
		val vColor = vPosition.sideToMove
		vResult.add(NumberedMove(vNumber, vColor, vPosition.san(vMove)))
		vPosition.play(vMove)
		if (vColor == Color.Black) {
			vNumber++
		}
	}
	return vResult
}

/**
 * Formate le numéro d'un coup : « 9. » pour un coup blanc, « 8… » pour un coup noir.
 */
fun formatMoveNumber(inMove: NumberedMove): String =
	if (inMove.color == Color.White) "${inMove.number}." else "${inMove.number}$kEllipsis"
