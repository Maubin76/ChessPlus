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
import androidx.compose.foundation.shape.CircleShape
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
import com.chessplus.app.ui.theme.ChessColors
import com.chessplus.app.ui.theme.ChessType
import com.chessplus.app.ui.theme.kLocalChessColors

/*
 * Graphiques des statistiques. Règles (recommandations de visualisation) : une seule teinte pour une
 * mesure, traits fins, extrémités arrondies posées sur la ligne de base, piste grise derrière les
 * barres, valeurs écrites dans la couleur du texte, jamais dans celle de la marque.
 */

/**
 * Courbe d'une série, avec une bande facultative (par exemple la cote ± son écart Glicko-2) et un
 * point sur la dernière valeur.
 *
 * @param inValues valeurs, dans l'ordre chronologique (au moins une).
 * @param inBand bornes basse et haute de la bande pour chaque valeur, ou null.
 * @param inRange bornes fixes de l'axe vertical, ou null pour s'adapter aux valeurs.
 */
@Composable
fun LineChart(inValues: List<Float>, inModifier: Modifier = Modifier, inBand: List<Pair<Float, Float>>? = null, inRange: ClosedFloatingPointRange<Float>? = null) {
	val vColors = kLocalChessColors.current
	Canvas(inModifier) {
		if (inValues.isEmpty()) {
			return@Canvas
		}
		val vValues = if (inValues.size == 1) listOf(inValues[0], inValues[0]) else inValues
		val vBand = inBand?.let { if (it.size == 1) listOf(it[0], it[0]) else it }
		val vAll = vValues + (vBand?.flatMap { listOf(it.first, it.second) } ?: emptyList())
		val vMin = inRange?.start ?: (vAll.min() - 5f)
		val vMax = inRange?.endInclusive ?: (vAll.max() + 5f)
		val vSpan = (vMax - vMin).takeIf { it > 0f } ?: 1f
		val vInset = 6.dp.toPx()
		val vWidth = size.width - 2 * vInset
		val vHeight = size.height - 2 * vInset
		fun x(inIndex: Int) = vInset + vWidth * inIndex / (vValues.size - 1)
		fun y(inValue: Float) = vInset + vHeight * (1f - (inValue.coerceIn(vMin, vMax) - vMin) / vSpan)
		if (vBand != null && vBand.size == vValues.size) {
			val vArea = Path()
			vBand.forEachIndexed { inIndex, inPair -> if (inIndex == 0) vArea.moveTo(x(inIndex), y(inPair.second)) else vArea.lineTo(x(inIndex), y(inPair.second)) }
			for (vIndex in vBand.indices.reversed()) {
				vArea.lineTo(x(vIndex), y(vBand[vIndex].first))
			}
			vArea.close()
			drawPath(vArea, vColors.primary.copy(alpha = 0.14f))
		}
		val vLine = Path()
		vValues.forEachIndexed { inIndex, inValue -> if (inIndex == 0) vLine.moveTo(x(inIndex), y(inValue)) else vLine.lineTo(x(inIndex), y(inValue)) }
		drawPath(vLine, vColors.primary, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
		val vLast = Offset(x(vValues.lastIndex), y(vValues.last()))
		drawCircle(vColors.surface, 6.dp.toPx(), vLast)
		drawCircle(vColors.primary, 4.dp.toPx(), vLast)
	}
}

/**
 * Ligne d'une liste de barres horizontales : libellé et valeur au-dessus, barre sur sa piste
 * dessous, précision facultative (nombre de problèmes…).
 *
 * @param inFraction longueur de la barre, de 0 à 1.
 */
@Composable
fun BarRow(inLabel: String, inValue: String, inFraction: Float, inDetail: String? = null, inOnClick: (() -> Unit)? = null) {
	val vColors = kLocalChessColors.current
	Column(
		Modifier
			.fillMaxWidth()
			.clip(RoundedCornerShape(10.dp))
			.then(if (inOnClick != null) Modifier.clickable(onClick = inOnClick) else Modifier)
			.padding(vertical = 6.dp),
	) {
		Row(verticalAlignment = Alignment.Bottom) {
			Text(inLabel, style = ChessType.kBody, color = vColors.text, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
			inDetail?.let {
				Text(it, style = ChessType.kCaption, color = vColors.muted, maxLines = 1)
				Spacer(Modifier.width(10.dp))
			}
			Text(inValue, style = ChessType.kBodyStrong, color = vColors.text, maxLines = 1, textAlign = TextAlign.End, modifier = Modifier.widthIn(min = 44.dp))
		}
		Spacer(Modifier.height(5.dp))
		Box(Modifier.fillMaxWidth().height(8.dp).background(vColors.track, CircleShape)) {
			if (inFraction > 0f) {
				Box(Modifier.fillMaxWidth(inFraction.coerceIn(0.02f, 1f)).fillMaxHeight().background(vColors.primary, CircleShape))
			}
		}
	}
}

/**
 * Colonne d'un graphique en barres verticales.
 *
 * @property label libellé sous la colonne.
 * @property fraction hauteur, de 0 à 1.
 * @property value valeur écrite au-dessus, ou null.
 */
data class ColumnItem(val label: String, val fraction: Float, val value: String?)

/**
 * Graphique en barres verticales, extrémités arrondies posées sur la ligne de base, 2 dp entre
 * colonnes, valeur au-dessus et libellé dessous.
 */
@Composable
fun ColumnChart(inItems: List<ColumnItem>, inModifier: Modifier = Modifier) {
	val vColors = kLocalChessColors.current
	Row(inModifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.Bottom) {
		for (vItem in inItems) {
			Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
				vItem.value?.let { Text(it, style = ChessType.kCoordinate, color = vColors.text, maxLines = 1) }
				Spacer(Modifier.height(2.dp))
				Box(Modifier.weight(1f).fillMaxWidth(0.72f), contentAlignment = Alignment.BottomCenter) {
					Canvas(Modifier.fillMaxWidth().fillMaxHeight()) {
						val vHeight = size.height * vItem.fraction.coerceIn(0f, 1f)
						if (vHeight > 0f) {
							val vRadius = minOf(4.dp.toPx(), size.width / 2, vHeight / 2)
							drawRoundRect(vColors.primary, Offset(0f, size.height - vHeight), Size(size.width, vHeight), CornerRadius(vRadius, vRadius))
							drawRect(vColors.primary, Offset(0f, size.height - minOf(vHeight, vRadius)), Size(size.width, minOf(vHeight, vRadius)))
						}
					}
				}
				Box(Modifier.fillMaxWidth().height(1.dp).background(vColors.track))
				Spacer(Modifier.height(4.dp))
				Text(vItem.label, style = ChessType.kCoordinate, color = vColors.muted, maxLines = 1, textAlign = TextAlign.Center)
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
 * Barre empilée (partie d'un tout) avec 2 dp entre segments et une légende qui donne chaque
 * libellé, son effectif et son pourcentage : la couleur n'est jamais seule à porter l'information.
 */
@Composable
fun StackedBar(inSegments: List<StackSegment>) {
	val vColors = kLocalChessColors.current
	val vTotal = inSegments.sumOf { it.count }.coerceAtLeast(1)
	Canvas(Modifier.fillMaxWidth().height(14.dp)) {
		val vGap = 2.dp.toPx()
		val vVisible = inSegments.filter { it.count > 0 }
		val vUsable = size.width - vGap * (vVisible.size - 1).coerceAtLeast(0)
		var vX = 0f
		vVisible.forEachIndexed { inIndex, inSegment ->
			val vWidth = vUsable * inSegment.count / vTotal
			val vRadius = CornerRadius(minOf(4.dp.toPx(), vWidth / 2), minOf(4.dp.toPx(), vWidth / 2))
			drawRoundRect(inSegment.color, Offset(vX, 0f), Size(vWidth, size.height), vRadius)
			vX += vWidth + if (inIndex < vVisible.lastIndex) vGap else 0f
		}
	}
	Spacer(Modifier.height(10.dp))
	for (vSegment in inSegments) {
		Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
			Box(Modifier.size(10.dp).background(vSegment.color, CircleShape))
			Spacer(Modifier.width(8.dp))
			Text(vSegment.label, style = ChessType.kBody, color = vColors.text, modifier = Modifier.weight(1f))
			Text("${vSegment.count}", style = ChessType.kCaption, color = vColors.muted)
			Spacer(Modifier.width(10.dp))
			Text("${(vSegment.count * 100f / vTotal).toInt()} %", style = ChessType.kBodyStrong, color = vColors.text, textAlign = TextAlign.End, modifier = Modifier.width(48.dp))
		}
	}
}

/**
 * Calendrier d'activité façon GitHub : une colonne par semaine (lundi en haut), une case par jour,
 * dans une seule teinte du clair au foncé selon le nombre de problèmes joués.
 *
 * @param inCounts nombre de problèmes par jour, du lundi le plus ancien à aujourd'hui.
 */
@Composable
fun CalendarHeatmap(inCounts: List<Int>, inModifier: Modifier = Modifier) {
	val vColors = kLocalChessColors.current
	Canvas(inModifier) {
		val vWeeks = (inCounts.size + 6) / 7
		val vGap = 2.dp.toPx()
		val vCell = minOf((size.width - vGap * (vWeeks - 1)) / vWeeks, (size.height - vGap * 6) / 7)
		val vRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
		inCounts.forEachIndexed { inIndex, inCount ->
			val vColumn = inIndex / 7
			val vRow = inIndex % 7
			drawRoundRect(chartLevelColor(vColors, inCount), Offset(vColumn * (vCell + vGap), vRow * (vCell + vGap)), Size(vCell, vCell), vRadius)
		}
	}
}

/**
 * Couleur d'une case du calendrier d'activité : piste grise sans problème, puis la teinte principale
 * de plus en plus opaque (1-2, 3-5, 6-9, 10 problèmes et plus).
 */
fun chartLevelColor(inColors: ChessColors, inCount: Int): Color = when {
	inCount <= 0 -> inColors.track
	inCount <= 2 -> inColors.primary.copy(alpha = 0.3f)
	inCount <= 5 -> inColors.primary.copy(alpha = 0.55f)
	inCount <= 9 -> inColors.primary.copy(alpha = 0.8f)
	else -> inColors.primary
}

/**
 * Légende du calendrier d'activité : « Moins », les cinq niveaux, « Plus ».
 */
@Composable
fun HeatmapLegend(inLess: String, inMore: String) {
	val vColors = kLocalChessColors.current
	Row(verticalAlignment = Alignment.CenterVertically) {
		Text(inLess, style = ChessType.kCaption, color = vColors.muted)
		Spacer(Modifier.width(6.dp))
		for (vCount in listOf(0, 1, 3, 6, 10)) {
			Box(Modifier.padding(horizontal = 1.dp).size(10.dp).background(chartLevelColor(vColors, vCount), RoundedCornerShape(2.dp)))
		}
		Spacer(Modifier.width(6.dp))
		Text(inMore, style = ChessType.kCaption, color = vColors.muted)
	}
}
