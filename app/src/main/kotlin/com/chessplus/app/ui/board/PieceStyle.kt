package com.chessplus.app.ui.board

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import com.chessplus.app.R
import com.chessplus.app.ui.theme.ChessPlusTheme
import com.chessplus.app.ui.theme.kDisplayFamily
import com.chessplus.core.model.Piece
import com.chessplus.core.model.PieceType
import com.chessplus.core.model.Color as Side

/**
 * Style des pièces, choisi dans les réglages.
 */
enum class PieceStyle {
	/** Jeu « cburnett » de Colin M.L. Burnett. */
	Classic,

	/** Silhouettes géométriques (création ChessPlus). */
	Geometric,

	/** Disques portant l'initiale française de la pièce (création ChessPlus). */
	Letters,
}

/** Style des pièces courant, fourni par le thème. */
val kLocalPieceStyle = staticCompositionLocalOf { PieceStyle.Classic }

/** Rayon du disque d'une pièce du style Lettres, en fraction de la case. */
private const val kLetterDiscRatio = 0.42f

/** Épaisseur du cerne du disque, en fraction de la case. */
private const val kLetterStrokeRatio = 0.045f

/** Taille de l'initiale, en fraction de la case. */
private const val kLetterSizeRatio = 0.46f

/**
 * Retourne la ressource d'une pièce du style Géométrique.
 */
fun geometricDrawable(inPiece: Piece): Int {
	val vWhite = inPiece.color == Side.White
	return when (inPiece.type) {
		PieceType.King -> if (vWhite) R.drawable.piece_geo_wk else R.drawable.piece_geo_bk
		PieceType.Queen -> if (vWhite) R.drawable.piece_geo_wq else R.drawable.piece_geo_bq
		PieceType.Rook -> if (vWhite) R.drawable.piece_geo_wr else R.drawable.piece_geo_br
		PieceType.Bishop -> if (vWhite) R.drawable.piece_geo_wb else R.drawable.piece_geo_bb
		PieceType.Knight -> if (vWhite) R.drawable.piece_geo_wn else R.drawable.piece_geo_bn
		PieceType.Pawn -> if (vWhite) R.drawable.piece_geo_wp else R.drawable.piece_geo_bp
	}
}

/**
 * Retourne l'initiale française d'une pièce (roi R, dame D, tour T, fou F, cavalier C, pion P).
 */
fun frenchLetter(inType: PieceType): String = when (inType) {
	PieceType.King -> "R"
	PieceType.Queen -> "D"
	PieceType.Rook -> "T"
	PieceType.Bishop -> "F"
	PieceType.Knight -> "C"
	PieceType.Pawn -> "P"
}

/**
 * Retourne le dessin d'une pièce dans le style [inStyle] (par défaut, celui des réglages).
 */
@Composable
fun rememberPiecePainter(inPiece: Piece, inStyle: PieceStyle = kLocalPieceStyle.current): Painter = when (inStyle) {
	PieceStyle.Classic -> painterResource(pieceDrawable(inPiece))
	PieceStyle.Geometric -> painterResource(geometricDrawable(inPiece))
	PieceStyle.Letters -> {
		val vMeasurer = rememberTextMeasurer()
		val vColors = ChessPlusTheme.colors
		remember(inPiece, vMeasurer, vColors.pieceWhite, vColors.pieceBlack) { LetterPiecePainter(inPiece, vMeasurer, vColors.pieceWhite, vColors.pieceBlack) }
	}
}

/**
 * Pièce du style Lettres : disque clair cerné de foncé (blancs) ou disque foncé (noirs), avec
 * l'initiale française au centre en Space Grotesk.
 *
 * @property piece pièce dessinée.
 * @property measurer mesure du texte.
 * @property light couleur claire (pieceWhite).
 * @property dark couleur foncée (pieceBlack).
 */
private class LetterPiecePainter(
	private val piece: Piece,
	private val measurer: TextMeasurer,
	private val light: Color,
	private val dark: Color,
) : Painter() {
	/** Taille propre : aucune, le dessin remplit la case. */
	override val intrinsicSize: Size = Size.Unspecified

	/**
	 * Dessine le disque et l'initiale, centrés dans la zone.
	 */
	override fun DrawScope.onDraw() {
		val vSide = minOf(size.width, size.height)
		val vCenter = Offset(size.width / 2, size.height / 2)
		val vWhite = piece.color == Side.White
		val vRadius = vSide * kLetterDiscRatio
		drawCircle(if (vWhite) light else dark, vRadius, vCenter)
		drawCircle(dark, vRadius, vCenter, style = Stroke(width = vSide * kLetterStrokeRatio))
		val vFontSize = (vSide * kLetterSizeRatio / density / fontScale).sp
		val vLayout = measurer.measure(
			frenchLetter(piece.type),
			TextStyle(fontFamily = kDisplayFamily, fontWeight = FontWeight(700), fontSize = vFontSize, lineHeight = vFontSize, color = if (vWhite) dark else light),
		)
		drawText(vLayout, topLeft = Offset(vCenter.x - vLayout.size.width / 2f, vCenter.y - vLayout.size.height / 2f))
	}
}
