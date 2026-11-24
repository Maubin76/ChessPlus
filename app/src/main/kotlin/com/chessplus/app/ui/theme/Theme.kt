package com.chessplus.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.chessplus.app.R
import com.chessplus.app.ui.board.PieceStyle
import com.chessplus.app.ui.board.kLocalPieceStyle

/*
 * Thème de ChessPlus d'après le design system (chessplus-design/, docs/DESIGN.md) : couleurs générées
 * dans ChessPlusTokens.kt, trois familles de polices embarquées, espacements et rayons.
 */

/**
 * Thème d'échiquier choisi dans les réglages : couleurs des cases en clair et en sombre.
 *
 * @property light cases claires, thème clair.
 * @property dark cases foncées, thème clair.
 * @property lightNight cases claires, thème sombre.
 * @property darkNight cases foncées, thème sombre.
 */
enum class BoardTheme(val light: Color, val dark: Color, val lightNight: Color, val darkNight: Color) {
	/** Sauge : valeurs du design system. */
	Sage(Color(0xFFE9E4D8), Color(0xFF8FA094), Color(0xFFC9C3B5), Color(0xFF6F7D72)),

	/** Bois ; variante sombre assombrie dans les mêmes proportions que la sauge. */
	Wood(Color(0xFFECE6DA), Color(0xFFB58863), Color(0xFFCAC5BA), Color(0xFF8C6A4D)),

	/** Ardoise. */
	Slate(Color(0xFFDEE3E6), Color(0xFF8CA2AD), Color(0xFFBEC2C5), Color(0xFF6D7E86)),

	/** Gris. */
	Grey(Color(0xFFF0F0F0), Color(0xFF9A9A9A), Color(0xFFCDCDCD), Color(0xFF787878)),
}

/**
 * Crée les graisses d'une police variable.
 */
private fun variableFamily(inFont: Int, inWeights: List<Int>): FontFamily =
	FontFamily(inWeights.map { Font(inFont, FontWeight(it), variationSettings = FontVariation.Settings(FontVariation.weight(it))) })

/** Space Grotesk : titres. */
val kDisplayFamily: FontFamily = variableFamily(R.font.space_grotesk, listOf(500, 600, 700))

/** Inter : texte. */
val kSansFamily: FontFamily = variableFamily(R.font.inter, listOf(400, 500, 600))

/** JetBrains Mono : chiffres, coups, chronos. */
val kMonoFamily: FontFamily = variableFamily(R.font.jetbrains_mono, listOf(500, 600))

/**
 * Styles de texte du design system.
 */
object ChessPlusTypography {
	/** Grand chiffre de classement. */
	val display = TextStyle(fontFamily = kDisplayFamily, fontWeight = FontWeight(700), fontSize = 40.sp, lineHeight = 44.sp, letterSpacing = (-0.02).em)

	/** Titre de page. */
	val title = TextStyle(fontFamily = kDisplayFamily, fontWeight = FontWeight(600), fontSize = 24.sp, lineHeight = 30.sp, letterSpacing = (-0.01).em)

	/** Titre de carte ou de mode. */
	val heading = TextStyle(fontFamily = kDisplayFamily, fontWeight = FontWeight(600), fontSize = 17.sp, lineHeight = 24.sp)

	/** Titre de ligne de mode (heading réduit des maquettes). */
	val headingSmall = heading.copy(fontSize = 15.sp, lineHeight = 20.sp)

	/** Texte courant. */
	val body = TextStyle(fontFamily = kSansFamily, fontWeight = FontWeight(400), fontSize = 15.sp, lineHeight = 22.sp)

	/** Texte de bouton. */
	val button = TextStyle(fontFamily = kSansFamily, fontWeight = FontWeight(600), fontSize = 14.sp, lineHeight = 14.sp)

	/** Descriptions sous un titre. */
	val small = TextStyle(fontFamily = kSansFamily, fontWeight = FontWeight(400), fontSize = 13.sp, lineHeight = 18.sp)

	/** En-têtes de section, à écrire en capitales. */
	val overline = TextStyle(fontFamily = kSansFamily, fontWeight = FontWeight(600), fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.08.em)

	/** Étiquette de thème, à écrire en capitales. */
	val tag = TextStyle(fontFamily = kSansFamily, fontWeight = FontWeight(600), fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 0.04.em)

	/** Notation des coups, chronos, classements en ligne. */
	val moves = TextStyle(fontFamily = kMonoFamily, fontWeight = FontWeight(500), fontSize = 14.sp, lineHeight = 20.sp)

	/** Chiffres en ligne de petite taille (valeurs des modes, pourcentages). */
	val movesSmall = moves.copy(fontSize = 12.sp, lineHeight = 16.sp)

	/** Valeurs des tuiles de statistiques. */
	val stat = TextStyle(fontFamily = kMonoFamily, fontWeight = FontWeight(600), fontSize = 22.sp, lineHeight = 28.sp)

	/** Coordonnées de l'échiquier. */
	val coordinate = TextStyle(fontFamily = kMonoFamily, fontWeight = FontWeight(600), fontSize = 8.sp, lineHeight = 8.sp)
}

/** Couleurs courantes, fournies par [ChessPlusTheme]. */
val kLocalChessPlusColors = staticCompositionLocalOf { LightChessPlusColors }

/** Vrai en thème sombre, fourni par [ChessPlusTheme]. */
val kLocalDarkTheme = staticCompositionLocalOf { false }

/**
 * Accès au thème courant : couleurs, typographie, espacements et rayons.
 */
object ChessPlusTheme {
	/** Couleurs du thème courant. */
	val colors: ChessPlusColors
		@Composable
		@ReadOnlyComposable
		get() = kLocalChessPlusColors.current

	/** Styles de texte. */
	val typography = ChessPlusTypography

	/** Espacements. */
	val spacing = ChessPlusSpacing

	/** Rayons. */
	val radius = ChessPlusRadius
}

/**
 * Thème Compose de ChessPlus, clair ou sombre, avec le thème d'échiquier et le style des pièces choisis. Un `MaterialTheme`
 * minimal dérivé garde les composants Material cohérents.
 */
@Composable
fun ChessPlusTheme(
	inDarkTheme: Boolean = isSystemInDarkTheme(),
	inBoard: BoardTheme = BoardTheme.Sage,
	inPieces: PieceStyle = PieceStyle.Classic,
	inContent: @Composable () -> Unit,
) {
	val vBase = if (inDarkTheme) DarkChessPlusColors else LightChessPlusColors
	val vColors = vBase.copy(
		boardLight = if (inDarkTheme) inBoard.lightNight else inBoard.light,
		boardDark = if (inDarkTheme) inBoard.darkNight else inBoard.dark,
	)
	val vScheme = if (inDarkTheme) {
		darkColorScheme(
			primary = vColors.accent, onPrimary = vColors.accentInk, background = vColors.surface, surface = vColors.surfaceRaised,
			onSurface = vColors.ink, onBackground = vColors.ink, outline = vColors.lineStrong, error = vColors.danger,
		)
	} else {
		lightColorScheme(
			primary = vColors.accent, onPrimary = vColors.accentInk, background = vColors.surface, surface = vColors.surfaceRaised,
			onSurface = vColors.ink, onBackground = vColors.ink, outline = vColors.lineStrong, error = vColors.danger,
		)
	}
	CompositionLocalProvider(kLocalChessPlusColors provides vColors, kLocalDarkTheme provides inDarkTheme, kLocalPieceStyle provides inPieces) {
		MaterialTheme(colorScheme = vScheme, content = inContent)
	}
}
