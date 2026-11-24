package com.chessplus.app.ui.board

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.chessplus.app.R
import com.chessplus.app.game.BadgeKind
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import com.chessplus.app.ui.theme.ChessPlusTheme
import com.chessplus.core.model.Color
import com.chessplus.core.model.Move
import com.chessplus.core.model.Piece
import com.chessplus.core.model.PieceType
import com.chessplus.core.model.Square
import kotlin.math.abs
import kotlin.math.roundToInt

/** Durée de l'animation d'un coup. */
private const val kMoveAnimationMs = 300

/** Agrandissement de la pièce tenue sous le doigt. */
private const val kDragScale = 1.3f

/** Diamètre du point d'un coup possible, en fraction de la case (24 %). */
private const val kDotRatio = 0.24f

/** Retrait de l'anneau d'une prise possible, en fraction de la case (3 %). */
private const val kRingInsetRatio = 0.03f

/** Épaisseur de l'anneau d'une prise possible, en fraction de la case (≈ 9 %). */
private const val kRingWidthRatio = 0.088f

/** Taille des coordonnées, en fraction de la largeur de l'échiquier (2,2 %). */
private const val kCoordinateRatio = 0.022f

/** Marge des coordonnées, en fraction de la largeur de l'échiquier (0,6 %). */
private const val kCoordinateMarginRatio = 0.006f

/** Épaisseur du cadre de la pièce montrée par l'indice. */
private const val kHintStroke = 2

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
 * Animation d'un changement de position : pièces qui glissent (arrivée vers départ) et pièces qui
 * s'effacent (prises).
 *
 * @property motions départ de chaque pièce animée, indexé par sa case d'arrivée.
 * @property fading pièces qui disparaissent, par case.
 */
private data class MovePlan(val motions: Map<Square, Square>, val fading: Map<Square, Piece>) {
	/** Vrai s'il n'y a rien à animer. */
	val isEmpty: Boolean
		get() = motions.isEmpty() && fading.isEmpty()

	companion object {
		/** Aucune animation. */
		val kNone = MovePlan(emptyMap(), emptyMap())
	}
}

/**
 * Calcule l'animation entre deux positions : chaque pièce apparue est reliée à la pièce identique
 * disparue la plus proche (ou au pion de sa couleur pour une promotion) ; les pièces disparues sans
 * correspondance sont des prises. Couvre les coups simples, les prises, le roque et la promotion.
 */
private fun planMoves(inOld: Map<Square, Piece>, inNew: Map<Square, Piece>): MovePlan {
	val vAppeared = inNew.filter { inOld[it.key] != it.value }
	val vDisappeared = inOld.filter { inNew[it.key] != it.value }.toMutableMap()
	val vMotions = mutableMapOf<Square, Square>()
	for ((vTo, vPiece) in vAppeared) {
		var vFrom = vDisappeared.filter { it.value == vPiece }.keys.minByOrNull { distance(it, vTo) }
		if (vFrom == null && vPiece.type != PieceType.Pawn && vPiece.type != PieceType.King) {
			vFrom = vDisappeared.filter { it.value == Piece(vPiece.color, PieceType.Pawn) }.keys.minByOrNull { distance(it, vTo) }
		}
		if (vFrom != null) {
			vMotions[vTo] = vFrom
			vDisappeared.remove(vFrom)
		}
	}
	return MovePlan(vMotions, vDisappeared)
}

/**
 * Distance de Manhattan entre deux cases.
 */
private fun distance(inFirst: Square, inSecond: Square): Int = abs(inFirst.file - inSecond.file) + abs(inFirst.rank - inSecond.rank)

/**
 * Dernière position affichée, pour calculer l'animation de la suivante.
 *
 * @property pieces pièces affichées.
 * @property moveId identifiant du coup affiché.
 */
private class BoardMemory(var pieces: Map<Square, Piece>, var moveId: Int)

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
	inHint: Square? = null,
	inBadge: Pair<Square, BadgeKind>?,
	inCoordinates: Boolean,
	inInteractive: Boolean,
	inOnTap: (Square) -> Unit,
	inOnDragStart: (Square) -> Unit,
	inOnDrop: (Square, Square?) -> Unit,
	inModifier: Modifier = Modifier,
) {
	val vColors = ChessPlusTheme.colors
	val vTextMeasurer = rememberTextMeasurer()
	val vMemory = remember { BoardMemory(inPieces, inMoveId) }
	val vPlan = remember(inMoveId) {
		if (inAnimateMove && inMoveId != vMemory.moveId) planMoves(vMemory.pieces, inPieces) else MovePlan.kNone
	}
	SideEffect {
		vMemory.pieces = inPieces
		vMemory.moveId = inMoveId
	}
	val vProgress = remember(inMoveId) { Animatable(if (vPlan.isEmpty) 1f else 0f) }
	var vDragFrom by remember { mutableStateOf<Square?>(null) }
	var vDragPosition by remember { mutableStateOf(Offset.Zero) }
	val vPieces by rememberUpdatedState(inPieces)
	val vInteractive by rememberUpdatedState(inInteractive)
	val vOnTap by rememberUpdatedState(inOnTap)
	val vOnDragStart by rememberUpdatedState(inOnDragStart)
	val vOnDrop by rememberUpdatedState(inOnDrop)

	LaunchedEffect(inMoveId) {
		if (!vPlan.isEmpty) {
			vProgress.animateTo(1f, tween(kMoveAnimationMs, easing = FastOutSlowInEasing))
		}
	}

	BoxWithConstraints(inModifier.aspectRatio(1f).clip(RoundedCornerShape(ChessPlusTheme.radius.radiusSm))) {
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
				drawRect(if (vLight) vColors.boardLight else vColors.boardDark, vTopLeft, vSize)
				if (inLastMove != null && (vSquare == inLastMove.from || vSquare == inLastMove.to)) {
					drawRect(vColors.boardLast, vTopLeft, vSize)
				}
				if (vSquare == inSelected) {
					drawRect(vColors.boardSelect, vTopLeft, vSize)
				}
			}
			inHint?.let { inSquare ->
				// Indice : cadre `accent` à l'intérieur de la case de la pièce à jouer, discret.
				val vStroke = kHintStroke.dp.toPx()
				drawRect(vColors.accent, vGeometry.topLeft(inSquare) + Offset(vStroke / 2, vStroke / 2), Size(vSquarePx - vStroke, vSquarePx - vStroke), style = Stroke(width = vStroke))
			}
			if (inCoordinates) {
				// Coordonnées en mono, dans le coin de la case, de la couleur de la case opposée.
				val vBoardPx = vSquarePx * 8
				val vMargin = vBoardPx * kCoordinateMarginRatio
				val vFontSize = (vBoardPx * kCoordinateRatio / density / fontScale).sp
				for (vSquare in Square.kAll) {
					val vTopLeft = vGeometry.topLeft(vSquare)
					val vColor = if (vSquare.isLight) vColors.boardDark else vColors.boardLight
					val vStyle = ChessPlusTheme.typography.coordinate.copy(color = vColor, fontSize = vFontSize, lineHeight = vFontSize)
					val vBottomRow = vTopLeft.y >= vSquarePx * 7 - 1
					val vLeftColumn = vTopLeft.x <= 1
					if (vLeftColumn) {
						drawText(vTextMeasurer, "${vSquare.rank + 1}", Offset(vTopLeft.x + vMargin, vTopLeft.y + vMargin), vStyle)
					}
					if (vBottomRow) {
						val vLayout = vTextMeasurer.measure("${'a' + vSquare.file}", vStyle)
						drawText(vLayout, topLeft = Offset(vTopLeft.x + vSquarePx - vLayout.size.width - vMargin, vTopLeft.y + vSquarePx - vLayout.size.height - vMargin))
					}
				}
			}
		}

		// Pièces : prises qui s'effacent, pièces déplacées qui glissent, sauf celle tenue sous le doigt.
		// L'avancement de l'animation n'est lu qu'au placement et au dessin, pour ne pas recomposer
		// l'échiquier à chaque image.
		for ((vSquare, vPiece) in vPlan.fading) {
			val vPosition = vGeometry.topLeft(vSquare)
			Image(
				painterResource(pieceDrawable(vPiece)),
				contentDescription = null,
				modifier = Modifier
					.size(vSquareDp)
					.offset { IntOffset(vPosition.x.roundToInt(), vPosition.y.roundToInt()) }
					.graphicsLayer { alpha = 1f - vProgress.value },
			)
		}
		for ((vSquare, vPiece) in inPieces) {
			if (vSquare == vDragFrom) {
				continue
			}
			val vTarget = vGeometry.topLeft(vSquare)
			val vStart = vPlan.motions[vSquare]?.let { vGeometry.topLeft(it) }
			Image(
				painterResource(pieceDrawable(vPiece)),
				contentDescription = null,
				modifier = Modifier
					.size(vSquareDp)
					.offset {
						val vPosition = if (vStart != null) vStart + (vTarget - vStart) * vProgress.value else vTarget
						IntOffset(vPosition.x.roundToInt(), vPosition.y.roundToInt())
					}
					.zIndex(if (vStart != null) 1f else 0f),
			)
		}

		// Coups possibles : petit point rond (24 % de la case) sur une case vide, anneau fin sur une prise.
		Canvas(Modifier.fillMaxSize()) {
			for (vTarget in inTargets) {
				val vCenter = vGeometry.topLeft(vTarget) + Offset(vSquarePx / 2, vSquarePx / 2)
				if (inPieces.containsKey(vTarget)) {
					val vRing = vSquarePx * kRingWidthRatio
					drawCircle(vColors.boardHint, radius = vSquarePx * (0.5f - kRingInsetRatio) - vRing / 2, center = vCenter, style = Stroke(width = vRing))
				} else {
					drawCircle(vColors.boardHint, radius = vSquarePx * kDotRatio / 2, center = vCenter)
				}
			}
		}

		vDragFrom?.let { inFrom ->
			inPieces[inFrom]?.let { inPiece ->
				Image(
					painterResource(pieceDrawable(inPiece)),
					contentDescription = null,
					modifier = Modifier
						.offset { IntOffset((vDragPosition.x - vSquarePx / 2).roundToInt(), (vDragPosition.y - vSquarePx * 0.8f).roundToInt()) }
						.zIndex(3f)
						.size(vSquareDp)
						.scale(kDragScale),
				)
			}
		}
	}
}
