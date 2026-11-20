package com.chessplus.app.ui.board

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.chessplus.app.R
import com.chessplus.app.game.BadgeKind
import com.chessplus.app.ui.theme.ChessType
import com.chessplus.app.ui.theme.kLocalChessColors
import com.chessplus.core.model.Color
import com.chessplus.core.model.Move
import com.chessplus.core.model.Piece
import com.chessplus.core.model.PieceType
import com.chessplus.core.model.Square
import kotlin.math.roundToInt

/** Durée de l'animation d'un coup. */
private const val kMoveAnimationMs = 220

/** Agrandissement de la pièce tenue sous le doigt. */
private const val kDragScale = 1.3f

/**
 * Retourne la ressource cburnett d'une pièce.
 */
fun pieceDrawable(inPiece: Piece): Int {
	val vWhite = inPiece.color == Color.White
	return when (inPiece.type) {
		PieceType.King -> if (vWhite) R.drawable.piece_wk else R.drawable.piece_bk
		PieceType.Queen -> if (vWhite) R.drawable.piece_wq else R.drawable.piece_bq
		PieceType.Rook -> if (vWhite) R.drawable.piece_wr else R.drawable.piece_br
		PieceType.Bishop -> if (vWhite) R.drawable.piece_wb else R.drawable.piece_bb
		PieceType.Knight -> if (vWhite) R.drawable.piece_wn else R.drawable.piece_bn
		PieceType.Pawn -> if (vWhite) R.drawable.piece_wp else R.drawable.piece_bp
	}
}

/**
 * Géométrie de l'échiquier affiché : conversion entre cases et positions selon l'orientation.
 *
 * @property squarePx côté d'une case en pixels.
 * @property orientation camp affiché en bas.
 */
private class BoardGeometry(val squarePx: Float, val orientation: Color) {
	/**
	 * Retourne la position (coin haut gauche) d'une case.
	 */
	fun topLeft(inSquare: Square): Offset {
		val vColumn = if (orientation == Color.White) inSquare.file else 7 - inSquare.file
		val vRow = if (orientation == Color.White) 7 - inSquare.rank else inSquare.rank
		return Offset(vColumn * squarePx, vRow * squarePx)
	}

	/**
	 * Retourne la case sous un point, ou null hors de l'échiquier.
	 */
	fun squareAt(inPoint: Offset): Square? {
		val vColumn = (inPoint.x / squarePx).toInt()
		val vRow = (inPoint.y / squarePx).toInt()
		if (inPoint.x < 0 || inPoint.y < 0 || vColumn !in 0..7 || vRow !in 0..7) {
			return null
		}
		val vFile = if (orientation == Color.White) vColumn else 7 - vColumn
		val vRank = if (orientation == Color.White) 7 - vRow else vRow
		return Square.of(vFile, vRank)
	}
}

/**
 * Échiquier interactif, carré, qui occupe toute la largeur disponible.
 *
 * Le joueur peut toucher une pièce puis sa case d'arrivée, ou la faire glisser. L'échiquier
 * n'a pas d'état métier : il signale les gestes et affiche ce qu'on lui donne.
 */
@Composable
fun ChessBoard(
	inPieces: Map<Square, Piece>,
	inOrientation: Color,
	inLastMove: Move?,
	inMoveId: Int,
	inAnimateMove: Boolean,
	inSelected: Square?,
	inTargets: Set<Square>,
	inBadge: Pair<Square, BadgeKind>?,
	inCoordinates: Boolean,
	inInteractive: Boolean,
	inOnTap: (Square) -> Unit,
	inOnDragStart: (Square) -> Unit,
	inOnDrop: (Square, Square?) -> Unit,
	inModifier: Modifier = Modifier,
) {
	val vColors = kLocalChessColors.current
	val vTextMeasurer = rememberTextMeasurer()
	val vProgress = remember { Animatable(1f) }
	var vAnimatedMoveId by remember { mutableIntStateOf(inMoveId) }
	var vDragFrom by remember { mutableStateOf<Square?>(null) }
	var vDragPosition by remember { mutableStateOf(Offset.Zero) }
	val vPieces by rememberUpdatedState(inPieces)
	val vInteractive by rememberUpdatedState(inInteractive)
	val vOnTap by rememberUpdatedState(inOnTap)
	val vOnDragStart by rememberUpdatedState(inOnDragStart)
	val vOnDrop by rememberUpdatedState(inOnDrop)

	LaunchedEffect(inMoveId) {
		if (inAnimateMove && inLastMove != null && inMoveId != vAnimatedMoveId) {
			vProgress.snapTo(0f)
			vAnimatedMoveId = inMoveId
			vProgress.animateTo(1f, tween(kMoveAnimationMs))
		} else {
			vAnimatedMoveId = inMoveId
			vProgress.snapTo(1f)
		}
	}

	BoxWithConstraints(inModifier.aspectRatio(1f)) {
		val vSquareDp = maxWidth / 8
		val vSquarePx = with(LocalDensity.current) { vSquareDp.toPx() }
		val vGeometry = BoardGeometry(vSquarePx, inOrientation)
		val vGeometryState by rememberUpdatedState(vGeometry)

		Canvas(
			Modifier
				.fillMaxSize()
				.pointerInput(Unit) {
					detectTapGestures(onTap = { inPoint -> vGeometryState.squareAt(inPoint)?.let(vOnTap) })
				}
				.pointerInput(Unit) {
					detectDragGestures(
						onDragStart = { inPoint ->
							val vSquare = vGeometryState.squareAt(inPoint)
							if (vInteractive && vSquare != null && vPieces[vSquare]?.color == vGeometryState.orientation) {
								vDragFrom = vSquare
								vDragPosition = inPoint
								vOnDragStart(vSquare)
							}
						},
						onDrag = { inChange, inAmount ->
							if (vDragFrom != null) {
								inChange.consume()
								vDragPosition += inAmount
							}
						},
						onDragEnd = {
							vDragFrom?.let { inFrom -> vOnDrop(inFrom, vGeometryState.squareAt(vDragPosition)) }
							vDragFrom = null
						},
						onDragCancel = { vDragFrom = null },
					)
				},
		) {
			val vSize = Size(vSquarePx, vSquarePx)
			for (vSquare in Square.kAll) {
				val vTopLeft = vGeometry.topLeft(vSquare)
				val vLight = vSquare.isLight
				drawRect(if (vLight) vColors.squareLight else vColors.squareDark, vTopLeft, vSize)
				if (inLastMove != null && (vSquare == inLastMove.from || vSquare == inLastMove.to)) {
					drawRect(vColors.lastMove, vTopLeft, vSize)
				}
				if (vSquare == inSelected) {
					drawRect(vColors.selected, vTopLeft, vSize)
				}
			}
			if (inCoordinates) {
				for (vSquare in Square.kAll) {
					val vTopLeft = vGeometry.topLeft(vSquare)
					val vColor = if (vSquare.isLight) vColors.coordOnLight else vColors.coordOnDark
					val vStyle = ChessType.kCoordinate.copy(color = vColor)
					val vBottomRow = vTopLeft.y >= vSquarePx * 7 - 1
					val vLeftColumn = vTopLeft.x <= 1
					if (vLeftColumn) {
						drawText(vTextMeasurer, "${vSquare.rank + 1}", Offset(vTopLeft.x + 3.dp.toPx(), vTopLeft.y + 1.dp.toPx()), vStyle)
					}
					if (vBottomRow) {
						val vLayout = vTextMeasurer.measure("${'a' + vSquare.file}", vStyle)
						drawText(vLayout, topLeft = Offset(vTopLeft.x + vSquarePx - vLayout.size.width - 3.dp.toPx(), vTopLeft.y + vSquarePx - vLayout.size.height))
					}
				}
			}
		}

		// Pièces, avec animation du dernier coup et pièce tenue sous le doigt.
		val vMoving = inLastMove?.to
		val vProgressValue = if (inAnimateMove && inMoveId != vAnimatedMoveId) 0f else vProgress.value
		for ((vSquare, vPiece) in inPieces) {
			if (vSquare == vDragFrom) {
				continue
			}
			var vPosition = vGeometry.topLeft(vSquare)
			if (vSquare == vMoving && vProgressValue < 1f) {
				val vFrom = vGeometry.topLeft(inLastMove.from)
				vPosition = vFrom + (vPosition - vFrom) * vProgressValue
			}
			Image(
				painterResource(pieceDrawable(vPiece)),
				contentDescription = null,
				modifier = Modifier
					.size(vSquareDp)
					.offset { IntOffset(vPosition.x.roundToInt(), vPosition.y.roundToInt()) }
					.zIndex(if (vSquare == vMoving) 1f else 0f),
			)
		}

		// Cases d'arrivée possibles : point sur une case vide, anneau sur une prise.
		Canvas(Modifier.fillMaxSize()) {
			for (vTarget in inTargets) {
				val vCenter = vGeometry.topLeft(vTarget) + Offset(vSquarePx / 2, vSquarePx / 2)
				if (inPieces.containsKey(vTarget)) {
					drawCircle(vColors.dot, radius = vSquarePx * 0.46f, center = vCenter, style = Stroke(width = vSquarePx * 0.08f))
				} else {
					drawCircle(vColors.dot, radius = vSquarePx * 0.16f, center = vCenter)
				}
			}
		}

		inBadge?.let { (inSquare, inKind) ->
			val vTopLeft = vGeometry.topLeft(inSquare)
			val vRadius = vSquareDp * 0.24f
			val vRadiusPx = vSquarePx * 0.24f
			val vX = vTopLeft.x + vSquarePx * 0.88f - vRadiusPx
			val vY = vTopLeft.y + vSquarePx * 0.12f - vRadiusPx
			Box(
				Modifier
					.offset { IntOffset(vX.roundToInt(), vY.roundToInt()) }
					.zIndex(2f)
					.size(vRadius * 2)
					.background(if (inKind == BadgeKind.Success) vColors.success else vColors.error, CircleShape)
					.border(1.5.dp, vColors.background, CircleShape),
				contentAlignment = Alignment.Center,
			) {
				Icon(
					painterResource(if (inKind == BadgeKind.Success) R.drawable.ic_check else R.drawable.ic_close),
					contentDescription = null,
					tint = vColors.onPrimary,
					modifier = Modifier.size(vRadius * 1.4f),
				)
			}
		}

		vDragFrom?.let { inFrom ->
			inPieces[inFrom]?.let { inPiece ->
				val vX = vDragPosition.x - vSquarePx / 2
				val vY = vDragPosition.y - vSquarePx * 0.8f
				Image(
					painterResource(pieceDrawable(inPiece)),
					contentDescription = null,
					modifier = Modifier
						.offset { IntOffset(vX.roundToInt(), vY.roundToInt()) }
						.zIndex(3f)
						.size(vSquareDp)
						.scale(kDragScale),
				)
			}
		}
	}
}
