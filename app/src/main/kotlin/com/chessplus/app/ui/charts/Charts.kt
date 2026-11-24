package com.chessplus.app.ui.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.chessplus.app.ui.components.ProgressBar
import com.chessplus.app.ui.theme.ChessPlusColors
import com.chessplus.app.ui.theme.ChessPlusTheme

/*
 * Graphiques des statistiques, d'après chessplus-design (statistiques.html) : courbe `accent` de
 * 2 dp aux jonctions arrondies, ligne de base `line`, barres de progression de 6 dp, calendrier
 * d'activité en cases `radiusXs` dont l'intensité suit l'opacité d'`accent`. Les valeurs sont
 * écrites dans la couleur du texte, en mono.
 */

/** Écart entre les cases du calendrier d'activité et entre les colonnes. */
private const val kHeatGap = 3

/**
 * Courbe d'une série avec sa ligne de base et un point final (8 dp).
 *
 * @param inValues valeurs, dans l'ordre chronologique (au moins une).
 * @param inRange bornes fixes de l'axe vertical, ou null pour s'adapter aux valeurs.
 */
@Composable
fun LineChart(inValues: List<Float>, inModifier: Modifier = Modifier, inRange: ClosedFloatingPointRange<Float>? = null) {
	val vColors = ChessPlusTheme.colors
	Canvas(inModifier) {
		val vBaseline = size.height - 1.dp.toPx()
		drawLine(vColors.line, Offset(0f, vBaseline), Offset(size.width, vBaseline), 1.dp.toPx())
		if (inValues.isEmpty()) {
			return@Canvas
		}
		val vValues = if (inValues.size == 1) listOf(inValues[0], inValues[0]) else inValues
		val vMin = inRange?.start ?: (vValues.min() - 10f)
		val vMax = inRange?.endInclusive ?: (vValues.max() + 10f)
		val vSpan = (vMax - vMin).takeIf { it > 0f } ?: 1f
		val vDot = 4.dp.toPx()
		val vWidth = size.width - vDot
		val vHeight = vBaseline - vDot
		fun x(inIndex: Int) = vWidth * inIndex / (vValues.size - 1)
		fun y(inValue: Float) = vDot + vHeight * (1f - (inValue.coerceIn(vMin, vMax) - vMin) / vSpan)
		val vLine = Path()
		vValues.forEachIndexed { inIndex, inValue -> if (inIndex == 0) vLine.moveTo(x(inIndex), y(inValue)) else vLine.lineTo(x(inIndex), y(inValue)) }
		drawPath(vLine, vColors.accent, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
		drawCircle(vColors.accent, vDot, Offset(x(vValues.lastIndex), y(vValues.last())))
	}
}

/**
 * Ligne de réussite : libellé et valeur en mono au-dessus, barre de progression dessous, précision
 * facultative (nombre de puzzles…).
 *
 * @param inFraction longueur de la barre, de 0 à 1.
 */
@Composable
fun BarRow(inLabel: String, inValue: String, inFraction: Float, inDetail: String? = null, inOnClick: (() -> Unit)? = null) {
	val vColors = ChessPlusTheme.colors
	Column(
		Modifier
			.fillMaxWidth()
			.clip(RoundedCornerShape(ChessPlusTheme.radius.radiusXs))
			.then(if (inOnClick != null) Modifier.clickable(onClick = inOnClick) else Modifier)
			.padding(vertical = 5.dp),
	) {
		Row(verticalAlignment = Alignment.Bottom) {
			Text(inLabel, style = ChessPlusTheme.typography.small, color = vColors.ink, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
			inDetail?.let {
				Text(it, style = ChessPlusTheme.typography.small, color = vColors.inkMuted, maxLines = 1)
				Spacer(Modifier.width(ChessPlusTheme.spacing.space3))
			}
			Text(inValue, style = ChessPlusTheme.typography.movesSmall, color = vColors.inkMuted, maxLines = 1, textAlign = TextAlign.End, modifier = Modifier.widthIn(min = 40.dp))
		}
		Spacer(Modifier.height(ChessPlusTheme.spacing.space1))
		ProgressBar(inFraction)
	}
}

/**
 * Colonne d'un graphique en barres verticales.
 *
 * @property label libellé sous la colonne.
 * @property fraction hauteur, de 0 à 1.
 * @property value valeur écrite au-dessus, ou null.
 * @property detail précision écrite sous le libellé (effectif…), ou null.
 */
data class ColumnItem(val label: String, val fraction: Float, val value: String?, val detail: String? = null)

/**
 * Graphique en barres verticales `accent` (coins `radiusXs` en haut), valeur au-dessus et libellé
 * dessous, en mono, sur une ligne de base `line`.
 */
@Composable
fun ColumnChart(inItems: List<ColumnItem>, inModifier: Modifier = Modifier) {
	val vColors = ChessPlusTheme.colors
	val vLabelStyle = ChessPlusTheme.typography.movesSmall.copy(fontSize = ChessPlusTheme.typography.coordinate.fontSize * 1.25f)
	Row(inModifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(kHeatGap.dp), verticalAlignment = Alignment.Bottom) {
		for (vItem in inItems) {
			Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
				vItem.value?.let { Text(it, style = vLabelStyle, color = vColors.ink, maxLines = 1) }
				Spacer(Modifier.height(2.dp))
				Box(Modifier.weight(1f).fillMaxWidth(0.7f), contentAlignment = Alignment.BottomCenter) {
					Canvas(Modifier.fillMaxWidth().fillMaxHeight()) {
						val vHeight = size.height * vItem.fraction.coerceIn(0f, 1f)
						if (vHeight > 0f) {
							val vRadius = minOf(3.dp.toPx(), size.width / 2, vHeight / 2)
							drawRoundRect(vColors.accent, Offset(0f, size.height - vHeight), Size(size.width, vHeight), CornerRadius(vRadius, vRadius))
							drawRect(vColors.accent, Offset(0f, size.height - minOf(vHeight, vRadius)), Size(size.width, minOf(vHeight, vRadius)))
						}
					}
				}
				Box(Modifier.fillMaxWidth().height(1.dp).background(vColors.line))
				Spacer(Modifier.height(ChessPlusTheme.spacing.space1))
				Text(vItem.label, style = vLabelStyle, color = vColors.inkMuted, maxLines = 1, textAlign = TextAlign.Center)
				vItem.detail?.let { Text(it, style = vLabelStyle, color = vColors.inkMuted.copy(alpha = 0.7f), maxLines = 1, textAlign = TextAlign.Center) }
			}
		}
	}
}

/**
 * Segment d'une barre empilée.
 *
 * @property label libellé de la légende.
 * @property count effectif.
 * @property color couleur du segment.
 */
data class StackSegment(val label: String, val count: Int, val color: Color)

/**
 * Barre empilée (partie d'un tout) de 6 dp avec 2 dp entre segments, et une légende qui donne
 * chaque libellé, son effectif et son pourcentage : la couleur n'est jamais seule à porter l'information.
 */
@Composable
fun StackedBar(inSegments: List<StackSegment>) {
	val vColors = ChessPlusTheme.colors
	val vTotal = inSegments.sumOf { it.count }.coerceAtLeast(1)
	Canvas(Modifier.fillMaxWidth().height(6.dp)) {
		val vGap = 2.dp.toPx()
		val vVisible = inSegments.filter { it.count > 0 }
		val vUsable = size.width - vGap * (vVisible.size - 1).coerceAtLeast(0)
		var vX = 0f
		vVisible.forEachIndexed { inIndex, inSegment ->
			val vWidth = vUsable * inSegment.count / vTotal
			val vRadius = CornerRadius(minOf(3.dp.toPx(), vWidth / 2), minOf(3.dp.toPx(), vWidth / 2))
			drawRoundRect(inSegment.color, Offset(vX, 0f), Size(vWidth, size.height), vRadius)
			vX += vWidth + if (inIndex < vVisible.lastIndex) vGap else 0f
		}
	}
	Spacer(Modifier.height(ChessPlusTheme.spacing.space3))
	for (vSegment in inSegments) {
		Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
			Box(Modifier.size(10.dp).clip(RoundedCornerShape(ChessPlusTheme.radius.radiusXs)).background(vSegment.color))
			Spacer(Modifier.width(ChessPlusTheme.spacing.space2))
			Text(vSegment.label, style = ChessPlusTheme.typography.small, color = vColors.ink, modifier = Modifier.weight(1f))
			Text("${vSegment.count}", style = ChessPlusTheme.typography.movesSmall, color = vColors.inkMuted)
			Spacer(Modifier.width(ChessPlusTheme.spacing.space3))
			Text("${(vSegment.count * 100f / vTotal).toInt()} %", style = ChessPlusTheme.typography.movesSmall, color = vColors.ink, textAlign = TextAlign.End, modifier = Modifier.width(48.dp))
		}
	}
}

/**
 * Calendrier d'activité : une colonne par semaine (lundi en haut), une case par jour, intensité par
 * opacité d'`accent` (aucune activité : `surfaceSunken`). Les cases remplissent la largeur.
 *
 * @param inCounts nombre de puzzles par jour, du lundi le plus ancien à aujourd'hui.
 */
@Composable
fun CalendarHeatmap(inCounts: List<Int>, inModifier: Modifier = Modifier) {
	val vColors = ChessPlusTheme.colors
	Canvas(inModifier) {
		val vWeeks = (inCounts.size + 6) / 7
		val vGap = kHeatGap.dp.toPx()
		val vCellWidth = (size.width - vGap * (vWeeks - 1)) / vWeeks
		val vCellHeight = (size.height - vGap * 6) / 7
		val vRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
		inCounts.forEachIndexed { inIndex, inCount ->
			val vColumn = inIndex / 7
			val vRow = inIndex % 7
			drawRoundRect(chartLevelColor(vColors, inCount), Offset(vColumn * (vCellWidth + vGap), vRow * (vCellHeight + vGap)), Size(vCellWidth, vCellHeight), vRadius)
		}
	}
}

/**
 * Couleur d'une case du calendrier : `surfaceSunken` sans puzzle, puis `accent` d'opacité
 * 0,25 + niveau × 0,75 (niveaux 0,15 ; 0,4 ; 0,7 ; 1 pour 1-2, 3-5, 6-9, 10 puzzles et plus).
 */
fun chartLevelColor(inColors: ChessPlusColors, inCount: Int): Color {
	val vLevel = when {
		inCount <= 0 -> return inColors.surfaceSunken
		inCount <= 2 -> 0.15f
		inCount <= 5 -> 0.4f
		inCount <= 9 -> 0.7f
		else -> 1f
	}
	return inColors.accent.copy(alpha = 0.25f + vLevel * 0.75f)
}

/**
 * Légende du calendrier : « Moins », les cinq niveaux, « Plus ».
 */
@Composable
fun HeatmapLegend(inLess: String, inMore: String) {
	val vColors = ChessPlusTheme.colors
	Row(verticalAlignment = Alignment.CenterVertically) {
		Text(inLess, style = ChessPlusTheme.typography.small, color = vColors.inkMuted)
		Spacer(Modifier.width(6.dp))
		for (vCount in listOf(0, 1, 3, 6, 10)) {
			Box(Modifier.padding(horizontal = 1.dp).size(10.dp).clip(RoundedCornerShape(ChessPlusTheme.radius.radiusXs)).background(chartLevelColor(vColors, vCount)))
		}
		Spacer(Modifier.width(6.dp))
		Text(inMore, style = ChessPlusTheme.typography.small, color = vColors.inkMuted)
	}
}
