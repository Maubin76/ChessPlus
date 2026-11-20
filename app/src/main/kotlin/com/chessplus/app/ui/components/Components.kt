package com.chessplus.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.chessplus.app.R
import com.chessplus.app.ui.board.pieceDrawable
import com.chessplus.app.ui.theme.ChessType
import com.chessplus.app.ui.theme.kLocalChessColors
import com.chessplus.core.model.Color
import com.chessplus.core.model.Piece
import com.chessplus.core.model.PieceType
import com.chessplus.core.model.Square
import java.text.NumberFormat
import java.util.Locale

/**
 * Barre du haut des écrans : retour éventuel, titre à gauche, action éventuelle.
 */
@Composable
fun ScreenTopBar(inTitle: String, inOnBack: (() -> Unit)? = null, inActionIcon: Int? = null, inOnAction: () -> Unit = {}) {
	val vColors = kLocalChessColors.current
	Row(Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
		if (inOnBack != null) {
			Box(Modifier.size(44.dp).clip(CircleShape).clickable(onClick = inOnBack), contentAlignment = Alignment.Center) {
				Icon(painterResource(R.drawable.ic_arrow_back_ios_new), contentDescription = stringResource(R.string.action_back), tint = vColors.text, modifier = Modifier.size(20.dp))
			}
			Spacer(Modifier.width(4.dp))
		} else {
			Spacer(Modifier.width(8.dp))
		}
		Text(inTitle, style = ChessType.kScreenTitle, color = vColors.text, modifier = Modifier.weight(1f))
		inActionIcon?.let {
			Box(Modifier.size(44.dp).clip(CircleShape).background(vColors.surface).clickable(onClick = inOnAction), contentAlignment = Alignment.Center) {
				Icon(painterResource(it), contentDescription = null, tint = vColors.text, modifier = Modifier.size(22.dp))
			}
		}
	}
}

/**
 * Bouton principal pleine largeur, en pastille noire.
 */
@Composable
fun PrimaryButton(inText: String, inOnClick: () -> Unit, inModifier: Modifier = Modifier, inIcon: Int? = null, inEnabled: Boolean = true, inHeight: Int = 60) {
	val vColors = kLocalChessColors.current
	Row(
		inModifier.fillMaxWidth().height(inHeight.dp).clip(RoundedCornerShape(30.dp)).background(if (inEnabled) vColors.primary else vColors.track).clickable(enabled = inEnabled, onClick = inOnClick),
		horizontalArrangement = Arrangement.Center,
		verticalAlignment = Alignment.CenterVertically,
	) {
		val vInk = if (inEnabled) vColors.onPrimary else vColors.muted
		inIcon?.let {
			Icon(painterResource(it), contentDescription = null, tint = vInk, modifier = Modifier.size(22.dp))
			Spacer(Modifier.width(10.dp))
		}
		Text(inText, style = ChessType.kBarTitle, color = vInk)
	}
}

/**
 * Bouton secondaire pleine largeur, en pastille claire.
 */
@Composable
fun SecondaryButton(inText: String, inOnClick: () -> Unit, inIcon: Int? = null, inModifier: Modifier = Modifier, inHeight: Int = 56) {
	val vColors = kLocalChessColors.current
	Row(
		inModifier.fillMaxWidth().height(inHeight.dp).clip(RoundedCornerShape(28.dp)).background(vColors.surface).clickable(onClick = inOnClick),
		horizontalArrangement = Arrangement.Center,
		verticalAlignment = Alignment.CenterVertically,
	) {
		inIcon?.let {
			Icon(painterResource(it), contentDescription = null, tint = vColors.text, modifier = Modifier.size(20.dp))
			Spacer(Modifier.width(10.dp))
		}
		Text(inText, style = ChessType.kBodyStrong, color = vColors.text)
	}
}

/**
 * Carte blanche (surface) aux grands arrondis.
 */
@Composable
fun SurfaceCard(inModifier: Modifier = Modifier, inContent: @Composable ColumnScope.() -> Unit) {
	val vColors = kLocalChessColors.current
	Column(inModifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(vColors.surface).padding(20.dp), content = inContent)
}

/**
 * Puce de sélection.
 */
@Composable
fun SelectChip(inText: String, inSelected: Boolean, inOnClick: () -> Unit) {
	val vColors = kLocalChessColors.current
	Box(
		Modifier
			.clip(RoundedCornerShape(18.dp))
			.background(if (inSelected) vColors.primary else vColors.surface)
			.border(1.dp, if (inSelected) vColors.primary else vColors.track, RoundedCornerShape(18.dp))
			.clickable(onClick = inOnClick)
			.padding(horizontal = 14.dp, vertical = 8.dp),
	) {
		Text(inText, style = ChessType.kCaption, color = if (inSelected) vColors.onPrimary else vColors.text)
	}
}

/**
 * Contrôle segmenté (onglets, choix exclusif).
 */
@Composable
fun SegmentedControl(inOptions: List<String>, inSelected: Int, inOnSelect: (Int) -> Unit) {
	val vColors = kLocalChessColors.current
	Row(Modifier.fillMaxWidth().height(44.dp).background(vColors.track, RoundedCornerShape(22.dp)).padding(4.dp)) {
		inOptions.forEachIndexed { inIndex, inOption ->
			Box(
				Modifier
					.weight(1f)
					.height(36.dp)
					.clip(RoundedCornerShape(18.dp))
					.background(if (inIndex == inSelected) vColors.surface else vColors.track)
					.clickable { inOnSelect(inIndex) },
				contentAlignment = Alignment.Center,
			) {
				Text(inOption, style = ChessType.kCaption, color = if (inIndex == inSelected) vColors.text else vColors.muted, textAlign = TextAlign.Center)
			}
		}
	}
}

/**
 * Ligne de réglage avec un interrupteur.
 */
@Composable
fun SwitchRow(inTitle: String, inSubtitle: String?, inChecked: Boolean, inOnChange: (Boolean) -> Unit, inEnabled: Boolean = true) {
	val vColors = kLocalChessColors.current
	Row(Modifier.fillMaxWidth().clickable(enabled = inEnabled) { inOnChange(!inChecked) }.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
		Column(Modifier.weight(1f)) {
			Text(inTitle, style = ChessType.kBodyStrong, color = if (inEnabled) vColors.text else vColors.muted)
			inSubtitle?.let { Text(it, style = ChessType.kCaption, color = vColors.muted) }
		}
		Switch(
			checked = inChecked,
			onCheckedChange = inOnChange,
			enabled = inEnabled,
			colors = SwitchDefaults.colors(
				checkedThumbColor = vColors.onPrimary,
				checkedTrackColor = vColors.primary,
				uncheckedThumbColor = vColors.muted,
				uncheckedTrackColor = vColors.track,
				uncheckedBorderColor = vColors.track,
			),
		)
	}
}

/**
 * Ligne de navigation vers un autre écran (titre, valeur, chevron).
 */
@Composable
fun NavigationRow(inTitle: String, inValue: String?, inOnClick: () -> Unit) {
	val vColors = kLocalChessColors.current
	Row(Modifier.fillMaxWidth().clickable(onClick = inOnClick).padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
		Text(inTitle, style = ChessType.kBodyStrong, color = vColors.text, modifier = Modifier.weight(1f))
		inValue?.let { Text(it, style = ChessType.kBody, color = vColors.muted) }
		Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = null, tint = vColors.muted, modifier = Modifier.size(22.dp))
	}
}

/**
 * Onglets de la barre de navigation du bas.
 */
enum class NavTab {
	Home,
	History,
	Stats,
}

/**
 * Navigation du bas : pilule flottante centrée (forme reprise de PatrimoinePlus). Au repos, un
 * onglet n'affiche que son icône en gris ; l'onglet actif devient une capsule à l'encre qui
 * s'élargit pour afficher son nom. Ombre douce en clair, liseré en sombre.
 */
@Composable
fun BottomNavBar(inSelected: NavTab, inOnSelect: (NavTab) -> Unit) {
	val vColors = kLocalChessColors.current
	val vDark = vColors.background.luminance() < 0.5f
	val vItems = listOf(
		Triple(NavTab.Home, R.string.nav_home, R.drawable.ic_home to R.drawable.ic_home_fill1),
		Triple(NavTab.History, R.string.nav_history, R.drawable.ic_history to R.drawable.ic_history_fill1),
		Triple(NavTab.Stats, R.string.nav_stats, R.drawable.ic_bar_chart to R.drawable.ic_bar_chart_fill1),
	)
	Box(Modifier.fillMaxWidth().navigationBarsPadding().padding(top = 8.dp, bottom = 12.dp), contentAlignment = Alignment.Center) {
		Row(
			Modifier
				.then(if (vDark) Modifier else Modifier.shadow(10.dp, CircleShape, ambientColor = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.18f), spotColor = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.18f)))
				.clip(CircleShape)
				.background(vColors.surface)
				.then(if (vDark) Modifier.border(1.dp, vColors.track, CircleShape) else Modifier)
				.height(64.dp)
				.padding(6.dp),
			horizontalArrangement = Arrangement.spacedBy(8.dp),
			verticalAlignment = Alignment.CenterVertically,
		) {
			for ((vTab, vLabel, vIcons) in vItems) {
				val vActive = vTab == inSelected
				val vBackground by animateColorAsState(if (vActive) vColors.primary else vColors.surface, label = "fondOnglet")
				val vInk by animateColorAsState(if (vActive) vColors.onPrimary else vColors.muted, label = "encreOnglet")
				val vName = stringResource(vLabel)
				Row(
					Modifier
						.fillMaxHeight()
						.clip(CircleShape)
						.background(vBackground)
						.clickable(onClickLabel = vName) { inOnSelect(vTab) }
						.animateContentSize()
						.padding(horizontal = if (vActive) 18.dp else 14.dp),
					verticalAlignment = Alignment.CenterVertically,
				) {
					Icon(painterResource(if (vActive) vIcons.second else vIcons.first), contentDescription = vName, tint = vInk, modifier = Modifier.size(22.dp))
					if (vActive) {
						Spacer(Modifier.width(8.dp))
						Text(vName, style = ChessType.kBodyStrong, color = vInk, maxLines = 1)
					}
				}
			}
		}
	}
}

/**
 * Vignette d'échiquier non interactive, dessinée en une seule passe (listes, accueil).
 */
@Composable
fun MiniBoard(inPieces: Map<Square, Piece>, inOrientation: Color, inModifier: Modifier = Modifier) {
	val vColors = kLocalChessColors.current
	val vPainters: Map<Piece, Painter> = buildMap {
		for (vColor in Color.entries) {
			for (vType in PieceType.entries) {
				val vPiece = Piece(vColor, vType)
				put(vPiece, painterResource(pieceDrawable(vPiece)))
			}
		}
	}
	Canvas(inModifier.aspectRatio(1f)) {
		val vSquare = size.width / 8
		for (vSquareModel in Square.kAll) {
			val vColumn = if (inOrientation == Color.White) vSquareModel.file else 7 - vSquareModel.file
			val vRow = if (inOrientation == Color.White) 7 - vSquareModel.rank else vSquareModel.rank
			val vTopLeft = Offset(vColumn * vSquare, vRow * vSquare)
			drawRect(if (vSquareModel.isLight) vColors.squareLight else vColors.squareDark, vTopLeft, Size(vSquare, vSquare))
			inPieces[vSquareModel]?.let { inPiece ->
				translate(vTopLeft.x, vTopLeft.y) {
					with(vPainters.getValue(inPiece)) { draw(Size(vSquare, vSquare)) }
				}
			}
		}
	}
}

/**
 * Formate une durée écoulée en français, quelle que soit la langue du système
 * (« à l'instant », « il y a 3 min », « il y a 2 h », « hier », « il y a 4 jours »).
 */
@Composable
fun formatElapsed(inTime: Long, inNow: Long): String {
	val vMinutes = ((inNow - inTime) / kMinuteMs).coerceAtLeast(0)
	return when {
		vMinutes < 1 -> stringResource(R.string.elapsed_now)
		vMinutes < kMinutesPerHour -> stringResource(R.string.elapsed_minutes, vMinutes.toInt())
		vMinutes < kMinutesPerDay -> stringResource(R.string.elapsed_hours, (vMinutes / kMinutesPerHour).toInt())
		vMinutes < 2 * kMinutesPerDay -> stringResource(R.string.elapsed_yesterday)
		else -> stringResource(R.string.elapsed_days, (vMinutes / kMinutesPerDay).toInt())
	}
}

/** Durée d'une minute en millisecondes. */
private const val kMinuteMs = 60_000L

/** Minutes par heure. */
private const val kMinutesPerHour = 60L

/** Minutes par jour. */
private const val kMinutesPerDay = 24L * 60

/**
 * Formate une cote avec une espace entre les milliers (« 1 520 »).
 */
fun formatRating(inRating: Int): String = NumberFormat.getIntegerInstance(Locale.FRANCE).format(inRating)

/**
 * Formate une variation de cote avec son signe (« +12 », « −9 »).
 */
fun formatDelta(inDelta: Int): String = when {
	inDelta > 0 -> "+$inDelta"
	inDelta < 0 -> "−${-inDelta}"
	else -> "±0"
}
