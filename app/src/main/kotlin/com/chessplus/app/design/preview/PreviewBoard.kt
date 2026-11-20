package com.chessplus.app.design.preview

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.chessplus.app.R

/**
 * Nature de la pastille posée sur une case après un coup.
 */
enum class BadgeKind {
	Success,
	Error,
}

/**
 * Marques affichées sur l'échiquier factice.
 *
 * @property lastMove cases du dernier coup, surlignées.
 * @property selected case de la pièce sélectionnée.
 * @property dots cases d'arrivée possibles de la pièce sélectionnée.
 * @property badgeSquare case portant une pastille, ou null.
 * @property badgeKind nature de la pastille.
 * @property coordinates affiche les coordonnées dans les cases.
 */
data class BoardMarks(
	val lastMove: Set<String> = emptySet(),
	val selected: String? = null,
	val dots: Set<String> = emptySet(),
	val badgeSquare: String? = null,
	val badgeKind: BadgeKind = BadgeKind.Success,
	val coordinates: Boolean = true,
)

/**
 * Retourne les pièces d'une position, à partir du premier champ d'une FEN, indexées par case (`e4` -> `wP`).
 */
fun parsePlacement(inPlacement: String): Map<String, String> {
	val vResult = mutableMapOf<String, String>()
	inPlacement.split('/').forEachIndexed { inRankIndex, inRow ->
		var vFile = 0
		for (vChar in inRow) {
			if (vChar.isDigit()) {
				vFile += vChar.digitToInt()
			} else {
				val vColor = if (vChar.isUpperCase()) "w" else "b"
				vResult["${'a' + vFile}${8 - inRankIndex}"] = vColor + vChar.uppercaseChar()
				vFile++
			}
		}
	}
	return vResult
}

/**
 * Retourne la ressource de la pièce cburnett correspondant à un code (`wK`, `bP`…).
 */
private fun pieceDrawable(inCode: String): Int = when (inCode) {
	"wK" -> R.drawable.piece_wk
	"wQ" -> R.drawable.piece_wq
	"wR" -> R.drawable.piece_wr
	"wB" -> R.drawable.piece_wb
	"wN" -> R.drawable.piece_wn
	"wP" -> R.drawable.piece_wp
	"bK" -> R.drawable.piece_bk
	"bQ" -> R.drawable.piece_bq
	"bR" -> R.drawable.piece_br
	"bB" -> R.drawable.piece_bb
	"bN" -> R.drawable.piece_bn
	else -> R.drawable.piece_bp
}

/**
 * Échiquier factice, carré, orienté avec les blancs en bas, qui occupe toute la largeur disponible.
 */
@Composable
fun PreviewBoard(
	inPlacement: String,
	inMarks: BoardMarks = BoardMarks(),
	inModifier: Modifier = Modifier,
) {
	val vColors = kLocalDesignColors.current
	val vPieces = parsePlacement(inPlacement)
	BoxWithConstraints(inModifier.aspectRatio(1f)) {
		val vSquare: Dp = maxWidth / 8
		Column {
			for (vRankIndex in 0 until 8) {
				Row {
					for (vFile in 0 until 8) {
						val vRank = 8 - vRankIndex
						val vName = "${'a' + vFile}$vRank"
						val vLight = (vFile + vRank) % 2 == 0
						Box(Modifier.size(vSquare).background(if (vLight) vColors.squareLight else vColors.squareDark)) {
							if (vName in inMarks.lastMove) {
								Box(Modifier.fillMaxSize().background(vColors.lastMove))
							}
							if (vName == inMarks.selected) {
								Box(Modifier.fillMaxSize().background(vColors.selected))
							}
							if (inMarks.coordinates) {
								val vCoordColor = if (vLight) vColors.coordOnLight else vColors.coordOnDark
								if (vFile == 0) {
									Text("$vRank", style = DesignType.kCoordinate, color = vCoordColor, modifier = Modifier.align(Alignment.TopStart).padding(start = 3.dp, top = 1.dp))
								}
								if (vRank == 1) {
									Text("${'a' + vFile}", style = DesignType.kCoordinate, color = vCoordColor, modifier = Modifier.align(Alignment.BottomEnd).padding(end = 3.dp))
								}
							}
							vPieces[vName]?.let { inCode ->
								Image(painterResource(pieceDrawable(inCode)), contentDescription = null, modifier = Modifier.fillMaxSize())
							}
							if (vName in inMarks.dots) {
								Box(Modifier.align(Alignment.Center).size(vSquare * 0.32f).background(vColors.dot, CircleShape))
							}
						}
					}
				}
			}
		}
		inMarks.badgeSquare?.let { inSquare ->
			val vFile = inSquare[0] - 'a'
			val vRankIndex = 8 - inSquare[1].digitToInt()
			val vRadius = vSquare * 0.24f
			val vColor = if (inMarks.badgeKind == BadgeKind.Success) vColors.success else vColors.error
			Box(
				Modifier
					.offset(x = vSquare * (vFile + 1) - vSquare * 0.12f - vRadius, y = vSquare * vRankIndex + vSquare * 0.12f - vRadius)
					.size(vRadius * 2)
					.background(vColor, CircleShape)
					.border(1.5.dp, vColors.background, CircleShape),
				contentAlignment = Alignment.Center,
			) {
				val vIcon = if (inMarks.badgeKind == BadgeKind.Success) R.drawable.ic_check else R.drawable.ic_close
				Icon(painterResource(vIcon), contentDescription = null, tint = vColors.onPrimary, modifier = Modifier.size(vRadius * 1.4f))
			}
		}
	}
}
