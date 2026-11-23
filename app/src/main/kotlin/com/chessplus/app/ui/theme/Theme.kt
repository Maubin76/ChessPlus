package com.chessplus.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.chessplus.app.R

/**
 * Couleurs de ChessPlus (palette « Accent pleine largeur », version B verte, de docs/DESIGN.md).
 */
@Immutable
data class ChessColors(
	val background: Color,
	val surface: Color,
	val text: Color,
	val muted: Color,
	val primary: Color,
	val onPrimary: Color,
	val success: Color,
	val error: Color,
	val track: Color,
	val scrim: Color,
	val squareLight: Color,
	val squareDark: Color,
	val lastMove: Color,
	val selected: Color,
	val dot: Color,
	val coordOnLight: Color,
	val coordOnDark: Color,
)

/** Palette claire. */
val kLightChessColors = ChessColors(
	background = Color(0xFFF4F5F6),
	surface = Color(0xFFFFFFFF),
	text = Color(0xFF111111),
	muted = Color(0xFF8A8E95),
	primary = Color(0xFF2F7D4F),
	onPrimary = Color(0xFFFFFFFF),
	success = Color(0xFF3E8E5A),
	error = Color(0xFFC2483D),
	track = Color(0xFFE3E5E8),
	scrim = Color(0x55000000),
	squareLight = Color(0xFFE7EFE4),
	squareDark = Color(0xFF7FA98A),
	lastMove = Color(0x80F2E66B),
	selected = Color(0x3D111111),
	dot = Color(0x45111111),
	coordOnLight = Color(0x80557A61),
	coordOnDark = Color(0xB3FFFFFF),
)

/** Palette sombre. */
val kDarkChessColors = ChessColors(
	background = Color(0xFF0F1011),
	surface = Color(0xFF1C1D1F),
	text = Color(0xFFF2F3F4),
	muted = Color(0xFF8A8E95),
	primary = Color(0xFF5DB37C),
	onPrimary = Color(0xFF0F1011),
	success = Color(0xFF5DB37C),
	error = Color(0xFFE0675C),
	track = Color(0xFF2A2C2F),
	scrim = Color(0x77000000),
	squareLight = Color(0xFFA9C2AE),
	squareDark = Color(0xFF5F8768),
	lastMove = Color(0x80E8D85C),
	selected = Color(0x50FFFFFF),
	dot = Color(0x50111111),
	coordOnLight = Color(0x80405C49),
	coordOnDark = Color(0x99FFFFFF),
)

/** Couleurs courantes. */
val kLocalChessColors = staticCompositionLocalOf { kLightChessColors }

/**
 * Crée une graisse de la police variable Inter.
 */
private fun interWeight(inWeight: Int): Font =
	Font(R.font.inter, FontWeight(inWeight), variationSettings = FontVariation.Settings(FontVariation.weight(inWeight)))

/** Police Inter (variable), graisses 400 à 800. */
val kInter: FontFamily = FontFamily(listOf(400, 500, 600, 700, 800).map { interWeight(it) })

/**
 * Styles de texte de ChessPlus (tableau « Typographie » de docs/DESIGN.md).
 */
object ChessType {
	/** Très grand nombre. */
	val kNumber = style(44, 800)

	/** Titre d'écran. */
	val kScreenTitle = style(28, 800)

	/** Message de retour (« Bon coup ! »). */
	val kFeedback = style(26, 800)

	/** Titre de section ou de carte. */
	val kSection = style(17, 700)

	/** Titre de barre et boutons principaux. */
	val kBarTitle = style(16, 600)

	/** Corps de texte. */
	val kBody = style(14, 400)

	/** Corps de texte appuyé. */
	val kBodyStrong = style(14, 600)

	/** Cote du joueur. */
	val kRating = style(14, 700)

	/** Libellés des commandes. */
	val kLabel = style(11, 600)

	/** Petites mentions. */
	val kCaption = style(12, 500)

	/** Coordonnées dans les cases. */
	val kCoordinate = style(9, 600)

	/**
	 * Crée un style Inter de taille et de graisse données.
	 */
	private fun style(inSize: Int, inWeight: Int): TextStyle =
		TextStyle(fontFamily = kInter, fontSize = inSize.sp, fontWeight = FontWeight(inWeight))
}

/**
 * Thème Compose de ChessPlus, clair ou sombre selon le système. [inColors] remplace la palette
 * (comparaison de palettes dans les captures).
 */
@Composable
fun ChessPlusTheme(
	inDarkTheme: Boolean = isSystemInDarkTheme(),
	inColors: ChessColors? = null,
	inContent: @Composable () -> Unit,
) {
	val vColors = inColors ?: if (inDarkTheme) kDarkChessColors else kLightChessColors
	val vScheme = if (inDarkTheme) {
		darkColorScheme(background = vColors.background, surface = vColors.surface, primary = vColors.primary, onPrimary = vColors.onPrimary, onSurface = vColors.text)
	} else {
		lightColorScheme(background = vColors.background, surface = vColors.surface, primary = vColors.primary, onPrimary = vColors.onPrimary, onSurface = vColors.text)
	}
	CompositionLocalProvider(kLocalChessColors provides vColors) {
		MaterialTheme(colorScheme = vScheme, content = inContent)
	}
}
