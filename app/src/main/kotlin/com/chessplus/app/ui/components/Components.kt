package com.chessplus.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.chessplus.app.R
import com.chessplus.app.ui.board.rememberPiecePainter
import com.chessplus.app.ui.theme.ChessPlusTheme
import com.chessplus.app.ui.theme.kLocalDarkTheme
import com.chessplus.core.model.Piece
import com.chessplus.core.model.PieceType
import com.chessplus.core.model.Square
import java.text.NumberFormat
import java.util.Locale
import com.chessplus.core.model.Color as Side

/*
 * Contrôles réutilisables du design system (chessplus-design/mockups/controles.html) : boutons,
 * chips, tags, toggles carrés, contrôles segmentés, barres de progression, listes à séparateurs,
 * barre de navigation flottante.
 */

/** Hauteur tactile minimale des boutons. */
private const val kTouchHeight = 44

/** Hauteur de la barre de navigation flottante. */
private const val kNavHeight = 60

/** Élévation de l'ombre de la barre de navigation (shadow-float). */
private const val kNavElevation = 6

/** Largeur de l'interrupteur carré. */
private const val kToggleWidth = 40f

/** Hauteur de l'interrupteur carré. */
private const val kToggleHeight = 22f

/** Côté du curseur de l'interrupteur. */
private const val kToggleThumb = 14f

/** Écart entre le curseur et le bord extérieur de l'interrupteur (bordure comprise). */
private const val kToggleInset = 4f

/** Hauteur d'une barre du haut d'écran secondaire. */
private const val kTopBarHeight = 52

/** Taille des icônes au trait. */
val kIconSize = 20.dp

/** Espace fine insécable, séparateur des milliers et des unités. */
const val kThinSpace = '\u202F'

/**
 * Titre de page (style `title`), en haut des écrans principaux.
 */
@Composable
fun PageTitle(inTitle: String, inModifier: Modifier = Modifier) {
	Text(inTitle, style = ChessPlusTheme.typography.title, color = ChessPlusTheme.colors.ink, modifier = inModifier)
}

/**
 * En-tête de section en capitales espacées (style `overline`).
 */
@Composable
fun Overline(inText: String, inModifier: Modifier = Modifier, inColor: Color = ChessPlusTheme.colors.inkMuted) {
	Text(inText.uppercase(Locale.FRANCE), style = ChessPlusTheme.typography.overline, color = inColor, modifier = inModifier, maxLines = 1, overflow = TextOverflow.Ellipsis)
}

/**
 * Section : en-tête `overline` puis contenu, séparés de 8 dp.
 */
@Composable
fun Section(inTitle: String, inModifier: Modifier = Modifier, inContent: @Composable ColumnScope.() -> Unit) {
	Column(inModifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(ChessPlusTheme.spacing.space2)) {
		Overline(inTitle)
		inContent()
	}
}

/**
 * Barre du haut d'un écran secondaire : retour, titre, élément facultatif à droite.
 */
@Composable
fun TopBar(inTitle: String, inOnBack: (() -> Unit)?, inTrailing: (@Composable () -> Unit)? = null) {
	Row(
		Modifier.fillMaxWidth().height(kTopBarHeight.dp).padding(horizontal = ChessPlusTheme.spacing.space4),
		verticalAlignment = Alignment.CenterVertically,
		horizontalArrangement = Arrangement.spacedBy(ChessPlusTheme.spacing.space3),
	) {
		if (inOnBack != null) {
			PlainIconButton(R.drawable.ic_ln_back, stringResource(R.string.action_back), inOnBack)
		}
		Text(inTitle, style = ChessPlusTheme.typography.heading, color = ChessPlusTheme.colors.ink, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
		inTrailing?.invoke()
	}
}

/**
 * Carte : fond `surfaceRaised`, filet `line` (ou [inBorder]), coins `radiusMd`.
 */
@Composable
fun SurfaceCard(inModifier: Modifier = Modifier, inBorder: Color = ChessPlusTheme.colors.line, inPadding: Dp = ChessPlusTheme.spacing.space4, inContent: @Composable ColumnScope.() -> Unit) {
	val vShape = RoundedCornerShape(ChessPlusTheme.radius.radiusMd)
	Column(
		inModifier.fillMaxWidth().clip(vShape).background(ChessPlusTheme.colors.surfaceRaised).border(1.dp, inBorder, vShape).padding(inPadding),
		content = inContent,
	)
}

/**
 * Liste à séparateurs : lignes sur `surfaceRaised`, séparées par des filets `line`.
 */
@Composable
fun ListGroup(inModifier: Modifier = Modifier, inRows: List<@Composable () -> Unit>) {
	val vShape = RoundedCornerShape(ChessPlusTheme.radius.radiusMd)
	Column(inModifier.fillMaxWidth().clip(vShape).background(ChessPlusTheme.colors.surfaceRaised).border(1.dp, ChessPlusTheme.colors.line, vShape)) {
		inRows.forEachIndexed { inIndex, inRow ->
			if (inIndex > 0) {
				Divider()
			}
			inRow()
		}
	}
}

/**
 * Filet horizontal de 1 dp.
 */
@Composable
fun Divider(inModifier: Modifier = Modifier) {
	Box(inModifier.fillMaxWidth().height(1.dp).background(ChessPlusTheme.colors.line))
}

/**
 * Ligne de liste : padding 12 × 16 dp, hauteur minimale [inMinHeight], éléments espacés de 12 dp.
 */
@Composable
fun ListRow(inModifier: Modifier = Modifier, inOnClick: (() -> Unit)? = null, inMinHeight: Dp = 48.dp, inContent: @Composable RowScope.() -> Unit) {
	Row(
		inModifier
			.fillMaxWidth()
			.heightIn(min = inMinHeight)
			.then(if (inOnClick != null) Modifier.clickable(onClick = inOnClick) else Modifier)
			.padding(horizontal = ChessPlusTheme.spacing.space4, vertical = ChessPlusTheme.spacing.space3),
		verticalAlignment = Alignment.CenterVertically,
		horizontalArrangement = Arrangement.spacedBy(ChessPlusTheme.spacing.space3),
		content = inContent,
	)
}

/**
 * Titre et description facultative d'une ligne de liste.
 */
@Composable
fun RowScope.RowTexts(inTitle: String, inSubtitle: String?, inTitleColor: Color = ChessPlusTheme.colors.ink) {
	Column(Modifier.weight(1f)) {
		Text(inTitle, style = ChessPlusTheme.typography.body, color = inTitleColor)
		inSubtitle?.let { Text(it, style = ChessPlusTheme.typography.small, color = ChessPlusTheme.colors.inkMuted) }
	}
}

/**
 * Ligne avec interrupteur carré.
 */
@Composable
fun ToggleRow(inTitle: String, inSubtitle: String?, inChecked: Boolean, inOnChange: (Boolean) -> Unit, inEnabled: Boolean = true) {
	ListRow(inOnClick = if (inEnabled) ({ inOnChange(!inChecked) }) else null) {
		RowTexts(inTitle, inSubtitle, if (inEnabled) ChessPlusTheme.colors.ink else ChessPlusTheme.colors.inkMuted)
		SquareToggle(inChecked, inEnabled)
	}
}

/**
 * Ligne de navigation : titre, valeur facultative, chevron.
 */
@Composable
fun LinkRow(inTitle: String, inValue: String?, inOnClick: () -> Unit, inSubtitle: String? = null) {
	ListRow(inOnClick = inOnClick) {
		RowTexts(inTitle, inSubtitle)
		inValue?.let { Text(it, style = ChessPlusTheme.typography.small, color = ChessPlusTheme.colors.inkMuted, maxLines = 1) }
		Icon(painterResource(R.drawable.ic_ln_chevron), contentDescription = null, tint = ChessPlusTheme.colors.inkMuted, modifier = Modifier.size(kIconSize))
	}
}

/**
 * Contenu commun des boutons : icône facultative et libellé.
 */
@Composable
private fun ButtonContent(inText: String, inIcon: Int?, inColor: Color, inStyle: TextStyle) {
	if (inIcon != null) {
		Icon(painterResource(inIcon), contentDescription = null, tint = inColor, modifier = Modifier.size(kIconSize))
		Spacer(Modifier.width(ChessPlusTheme.spacing.space2))
	}
	Text(inText, style = inStyle, color = inColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
}

/**
 * Bouton principal (`accent`) : un seul par écran.
 */
@Composable
fun PrimaryButton(inText: String, inOnClick: () -> Unit, inModifier: Modifier = Modifier, inIcon: Int? = null, inEnabled: Boolean = true, inHeight: Dp = kTouchHeight.dp) {
	val vColors = ChessPlusTheme.colors
	val vPress = rememberPressFeedback()
	val vShape = RoundedCornerShape(ChessPlusTheme.radius.radiusMd)
	Row(
		inModifier
			.then(vPress.scale)
			.height(inHeight)
			.clip(vShape)
			.background(if (inEnabled) vColors.accent else vColors.surfaceSunken)
			.pressClickable(vPress, inEnabled, inOnClick)
			.padding(horizontal = ChessPlusTheme.spacing.space4),
		horizontalArrangement = Arrangement.Center,
		verticalAlignment = Alignment.CenterVertically,
	) {
		ButtonContent(inText, inIcon, if (inEnabled) vColors.accentInk else vColors.inkMuted, ChessPlusTheme.typography.button)
	}
}

/**
 * Bouton secondaire : fond `surfaceRaised`, bordure `lineStrong`.
 */
@Composable
fun SecondaryButton(
	inText: String,
	inOnClick: () -> Unit,
	inModifier: Modifier = Modifier,
	inIcon: Int? = null,
	inEnabled: Boolean = true,
	inHeight: Dp = kTouchHeight.dp,
	inStyle: TextStyle = ChessPlusTheme.typography.button,
	inHorizontalPadding: Dp = ChessPlusTheme.spacing.space4,
) {
	val vColors = ChessPlusTheme.colors
	val vPress = rememberPressFeedback()
	val vShape = RoundedCornerShape(ChessPlusTheme.radius.radiusMd)
	Row(
		inModifier
			.then(vPress.scale)
			.height(inHeight)
			.clip(vShape)
			.background(vColors.surfaceRaised)
			.border(1.dp, if (inEnabled) vColors.lineStrong else vColors.line, vShape)
			.pressClickable(vPress, inEnabled, inOnClick)
			.padding(horizontal = inHorizontalPadding),
		horizontalArrangement = Arrangement.Center,
		verticalAlignment = Alignment.CenterVertically,
	) {
		ButtonContent(inText, inIcon, if (inEnabled) vColors.ink else vColors.inkMuted, inStyle)
	}
}

/**
 * Bouton discret : texte `inkMuted`, sans fond.
 */
@Composable
fun GhostButton(inText: String, inOnClick: () -> Unit, inModifier: Modifier = Modifier, inIcon: Int? = null) {
	val vPress = rememberPressFeedback()
	Row(
		inModifier
			.then(vPress.scale)
			.height(kTouchHeight.dp)
			.clip(RoundedCornerShape(ChessPlusTheme.radius.radiusMd))
			.pressClickable(vPress, inOnClick = inOnClick)
			.padding(horizontal = ChessPlusTheme.spacing.space4),
		horizontalArrangement = Arrangement.Center,
		verticalAlignment = Alignment.CenterVertically,
	) {
		ButtonContent(inText, inIcon, ChessPlusTheme.colors.inkMuted, ChessPlusTheme.typography.button)
	}
}

/**
 * Bouton icône 44 × 44 dp bordé de `lineStrong`.
 */
@Composable
fun IconButtonOutlined(inIcon: Int, inDescription: String, inOnClick: () -> Unit, inModifier: Modifier = Modifier, inEnabled: Boolean = true) {
	val vColors = ChessPlusTheme.colors
	val vPress = rememberPressFeedback()
	val vShape = RoundedCornerShape(ChessPlusTheme.radius.radiusMd)
	Box(
		inModifier
			.then(vPress.scale)
			.size(kTouchHeight.dp)
			.clip(vShape)
			.border(1.dp, if (inEnabled) vColors.lineStrong else vColors.line, vShape)
			.pressClickable(vPress, inEnabled, inOnClick),
		contentAlignment = Alignment.Center,
	) {
		Icon(painterResource(inIcon), contentDescription = inDescription, tint = if (inEnabled) vColors.ink else vColors.inkMuted, modifier = Modifier.size(kIconSize))
	}
}

/**
 * Bouton icône 40 × 40 dp sans bordure (retour).
 */
@Composable
fun PlainIconButton(inIcon: Int, inDescription: String, inOnClick: () -> Unit) {
	Box(
		Modifier.size(40.dp).clip(RoundedCornerShape(ChessPlusTheme.radius.radiusMd)).clickable(onClick = inOnClick),
		contentAlignment = Alignment.Center,
	) {
		Icon(painterResource(inIcon), contentDescription = inDescription, tint = ChessPlusTheme.colors.ink, modifier = Modifier.size(kIconSize))
	}
}

/**
 * Chip : icône facultative et valeur en mono, bordure `line`, hauteur 26 dp.
 */
@Composable
fun Chip(inText: String, inModifier: Modifier = Modifier, inIcon: Int? = null, inIconTint: Color = ChessPlusTheme.colors.ink, inTextColor: Color = ChessPlusTheme.colors.ink) {
	val vShape = RoundedCornerShape(ChessPlusTheme.radius.radiusSm)
	Row(
		inModifier.height(26.dp).clip(vShape).background(ChessPlusTheme.colors.surfaceRaised).border(1.dp, ChessPlusTheme.colors.line, vShape).padding(horizontal = ChessPlusTheme.spacing.space2),
		verticalAlignment = Alignment.CenterVertically,
		horizontalArrangement = Arrangement.spacedBy(ChessPlusTheme.spacing.space1),
	) {
		inIcon?.let { Icon(painterResource(it), contentDescription = null, tint = inIconTint, modifier = Modifier.size(kIconSize)) }
		Text(inText, style = ChessPlusTheme.typography.moves.copy(fontSize = ChessPlusTheme.typography.small.fontSize), color = inTextColor, maxLines = 1)
	}
}

/**
 * Puce sélectionnable (filtres) : `accentSoft` et `accent` une fois choisie, sinon `surfaceRaised` bordée de `line`.
 */
@Composable
fun SelectChip(inText: String, inSelected: Boolean, inOnClick: () -> Unit) {
	val vColors = ChessPlusTheme.colors
	val vShape = RoundedCornerShape(ChessPlusTheme.radius.radiusSm)
	val vBackground by animateColorAsState(if (inSelected) vColors.accentSoft else vColors.surfaceRaised, tween(150), label = "puce")
	Box(
		Modifier
			.heightIn(min = 32.dp)
			.clip(vShape)
			.background(vBackground)
			.border(1.dp, if (inSelected) vColors.accent else vColors.line, vShape)
			.clickable(onClick = inOnClick)
			.padding(horizontal = 10.dp, vertical = 6.dp),
		contentAlignment = Alignment.Center,
	) {
		Text(inText, style = ChessPlusTheme.typography.small.copy(fontWeight = if (inSelected) androidx.compose.ui.text.font.FontWeight(600) else null), color = if (inSelected) vColors.accent else vColors.ink)
	}
}

/**
 * Étiquette de thème : capitales sur `accentSoft`, texte `accent`.
 */
@Composable
fun Tag(inText: String, inModifier: Modifier = Modifier) {
	Text(
		inText.uppercase(Locale.FRANCE),
		style = ChessPlusTheme.typography.tag,
		color = ChessPlusTheme.colors.accent,
		maxLines = 1,
		modifier = inModifier.clip(RoundedCornerShape(ChessPlusTheme.radius.radiusSm)).background(ChessPlusTheme.colors.accentSoft).padding(horizontal = 6.dp, vertical = 2.dp),
	)
}

/**
 * Interrupteur rectangulaire 40 × 22 dp à curseur carré de 14 dp.
 */
@Composable
fun SquareToggle(inChecked: Boolean, inEnabled: Boolean = true) {
	val vColors = ChessPlusTheme.colors
	val vShape = RoundedCornerShape(ChessPlusTheme.radius.radiusSm)
	// Curseur à 4 dp des bords (bordure de 1 dp + 3 dp d'écart), centré verticalement : 4 dp éteint, 40 − 14 − 4 = 22 dp allumé.
	val vOffset by animateFloatAsState(if (inChecked) kToggleWidth - kToggleThumb - kToggleInset else kToggleInset, tween(150), label = "interrupteur")
	val vTrack = if (inChecked) vColors.accent else vColors.surfaceSunken
	val vBorder = if (inChecked) vColors.accent else vColors.lineStrong
	Box(
		Modifier.size(kToggleWidth.dp, kToggleHeight.dp).clip(vShape).background(if (inEnabled) vTrack else vColors.surfaceSunken).border(1.dp, if (inEnabled) vBorder else vColors.line, vShape),
		contentAlignment = Alignment.CenterStart,
	) {
		Box(
			Modifier
				.offset(x = vOffset.dp)
				.size(kToggleThumb.dp)
				.clip(RoundedCornerShape(ChessPlusTheme.radius.radiusXs))
				.background(if (inChecked && inEnabled) vColors.accentInk else vColors.lineStrong),
		)
	}
}

/**
 * Contrôle segmenté pour les choix exclusifs : bordure `lineStrong`, segments séparés par des filets,
 * segment choisi en `accentSoft` et `accent`.
 */
@Composable
fun SegmentedControl(inOptions: List<String>, inSelected: Int, inModifier: Modifier = Modifier, inOnSelect: (Int) -> Unit) {
	val vColors = ChessPlusTheme.colors
	val vShape = RoundedCornerShape(ChessPlusTheme.radius.radiusSm)
	Row(inModifier.fillMaxWidth().height(IntrinsicSize.Min).clip(vShape).border(1.dp, vColors.lineStrong, vShape)) {
		inOptions.forEachIndexed { inIndex, inLabel ->
			if (inIndex > 0) {
				Box(Modifier.width(1.dp).fillMaxHeight().background(vColors.line))
			}
			val vOn = inIndex == inSelected
			val vBackground by animateColorAsState(if (vOn) vColors.accentSoft else Color.Transparent, tween(150), label = "segment")
			Box(
				Modifier.weight(1f).background(vBackground).clickable { inOnSelect(inIndex) }.padding(vertical = ChessPlusTheme.spacing.space2),
				contentAlignment = Alignment.Center,
			) {
				Text(
					inLabel,
					style = ChessPlusTheme.typography.small.copy(fontWeight = androidx.compose.ui.text.font.FontWeight(if (vOn) 600 else 500)),
					color = if (vOn) vColors.accent else vColors.inkMuted,
					maxLines = 1,
				)
			}
		}
	}
}

/**
 * Barre de progression de 6 dp : piste `surfaceSunken`, remplissage `accent`.
 */
@Composable
fun ProgressBar(inFraction: Float, inModifier: Modifier = Modifier, inColor: Color = ChessPlusTheme.colors.accent) {
	val vShape = RoundedCornerShape(ChessPlusTheme.radius.radiusXs)
	Box(inModifier.fillMaxWidth().height(6.dp).clip(vShape).background(ChessPlusTheme.colors.surfaceSunken)) {
		if (inFraction > 0f) {
			Box(Modifier.fillMaxWidth(inFraction.coerceIn(0f, 1f)).fillMaxHeight().background(inColor))
		}
	}
}

/**
 * Grille de tuiles de chiffres (libellé `small`, valeur `stat`) séparées par des filets 1 dp.
 */
@Composable
fun StatTiles(inTiles: List<Pair<String, String>>, inModifier: Modifier = Modifier, inColumns: Int = 2) {
	val vColors = ChessPlusTheme.colors
	val vShape = RoundedCornerShape(ChessPlusTheme.radius.radiusMd)
	Column(inModifier.fillMaxWidth().clip(vShape).background(vColors.line).border(1.dp, vColors.line, vShape), verticalArrangement = Arrangement.spacedBy(1.dp)) {
		for (vRow in inTiles.chunked(inColumns)) {
			Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(1.dp)) {
				for (vIndex in 0 until inColumns) {
					val vTile = vRow.getOrNull(vIndex)
					Column(Modifier.weight(1f).fillMaxHeight().background(vColors.surfaceRaised).padding(horizontal = 14.dp, vertical = ChessPlusTheme.spacing.space3)) {
						if (vTile != null) {
							Text(vTile.first, style = ChessPlusTheme.typography.small, color = vColors.inkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
							Text(vTile.second, style = ChessPlusTheme.typography.stat, color = vColors.ink, maxLines = 1)
						}
					}
				}
			}
		}
	}
}

/**
 * Onglets de la barre de navigation.
 */
enum class NavTab {
	Home,
	Stats,
	Settings,
}

/**
 * Barre de navigation flottante : décollée des bords, `surfaceRaised`, filet `line`, coins `radiusLg`,
 * ombre `shadow-float`. L'onglet actif s'élargit et montre son libellé en `accent` sur `accentSoft`.
 */
@Composable
fun FloatingNavBar(inSelected: NavTab, inOnSelect: (NavTab) -> Unit) {
	val vColors = ChessPlusTheme.colors
	val vShape = RoundedCornerShape(ChessPlusTheme.radius.radiusLg)
	val vShadow = if (kLocalDarkTheme.current) Color.Black.copy(alpha = 0.45f) else Color(0xFF16181A).copy(alpha = 0.10f)
	Box(Modifier.fillMaxWidth().navigationBarsPadding().padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 12.dp)) {
		Row(
			Modifier
				.fillMaxWidth()
				.height(kNavHeight.dp)
				.shadow(kNavElevation.dp, vShape, ambientColor = vShadow, spotColor = vShadow)
				.clip(vShape)
				.background(vColors.surfaceRaised)
				.border(1.dp, vColors.line, vShape)
				.padding(horizontal = ChessPlusTheme.spacing.space2),
			verticalAlignment = Alignment.CenterVertically,
			horizontalArrangement = Arrangement.spacedBy(ChessPlusTheme.spacing.space1),
		) {
			for (vTab in NavTab.entries) {
				val vOn = vTab == inSelected
				val vWeight by animateFloatAsState(if (vOn) 1.8f else 1f, tween(200), label = "onglet")
				val vBackground by animateColorAsState(if (vOn) vColors.accentSoft else Color.Transparent, tween(200), label = "fond onglet")
				val vLabel = stringResource(
					when (vTab) {
						NavTab.Home -> R.string.nav_home
						NavTab.Stats -> R.string.nav_stats
						NavTab.Settings -> R.string.nav_settings
					}
				)
				Row(
					Modifier
						.weight(vWeight)
						.height(kTouchHeight.dp)
						.clip(RoundedCornerShape(ChessPlusTheme.radius.radiusMd))
						.background(vBackground)
						.clickable { inOnSelect(vTab) },
					horizontalArrangement = Arrangement.Center,
					verticalAlignment = Alignment.CenterVertically,
				) {
					Icon(
						painterResource(
							when (vTab) {
								NavTab.Home -> R.drawable.ic_ln_home
								NavTab.Stats -> R.drawable.ic_ln_stats
								NavTab.Settings -> R.drawable.ic_ln_settings
							}
						),
						contentDescription = vLabel,
						tint = if (vOn) vColors.accent else vColors.inkMuted,
						modifier = Modifier.size(kIconSize),
					)
					if (vOn) {
						Spacer(Modifier.width(ChessPlusTheme.spacing.space2))
						Text(vLabel, style = ChessPlusTheme.typography.button.copy(fontSize = ChessPlusTheme.typography.small.fontSize), color = vColors.accent, maxLines = 1)
					}
				}
			}
		}
	}
}

/**
 * Vignette d'échiquier non interactive, sans coordonnées, dessinée en une seule passe.
 */
@Composable
fun MiniBoard(inPieces: Map<Square, Piece>, inOrientation: Side, inModifier: Modifier = Modifier) {
	val vColors = ChessPlusTheme.colors
	val vPainters: Map<Piece, Painter> = buildMap {
		for (vColor in Side.entries) {
			for (vType in PieceType.entries) {
				val vPiece = Piece(vColor, vType)
				put(vPiece, rememberPiecePainter(vPiece))
			}
		}
	}
	Canvas(inModifier.aspectRatio(1f).clip(RoundedCornerShape(ChessPlusTheme.radius.radiusSm))) {
		val vSquare = size.width / 8
		for (vSquareModel in Square.kAll) {
			val vColumn = if (inOrientation == Side.White) vSquareModel.file else 7 - vSquareModel.file
			val vRow = if (inOrientation == Side.White) 7 - vSquareModel.rank else vSquareModel.rank
			val vTopLeft = Offset(vColumn * vSquare, vRow * vSquare)
			drawRect(if (vSquareModel.isLight) vColors.boardLight else vColors.boardDark, vTopLeft, Size(vSquare, vSquare))
			inPieces[vSquareModel]?.let { inPiece ->
				translate(vTopLeft.x, vTopLeft.y) {
					with(vPainters.getValue(inPiece)) { draw(Size(vSquare, vSquare)) }
				}
			}
		}
	}
}

/**
 * Formate une durée écoulée en français (« à l'instant », « il y a 3 min », « hier »…).
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
 * Formate un nombre avec une espace fine insécable entre les milliers (« 1 642 »).
 */
fun formatRating(inRating: Int): String =
	NumberFormat.getIntegerInstance(Locale.FRANCE).format(inRating).replace('\u00A0', kThinSpace).replace(' ', kThinSpace)

/**
 * Formate une variation avec son signe (« +12 », « −9 »).
 */
fun formatDelta(inDelta: Int): String = when {
	inDelta > 0 -> "+$inDelta"
	inDelta < 0 -> "−${-inDelta}"
	else -> "±0"
}

/**
 * Formate une durée de chronomètre en minutes et secondes (« 0:42 »).
 */
fun formatClock(inMs: Long): String {
	val vSeconds = (inMs.coerceAtLeast(0) / 1000).toInt()
	return "%d:%02d".format(vSeconds / 60, vSeconds % 60)
}
