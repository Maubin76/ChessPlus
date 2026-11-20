package com.chessplus.app.design.preview

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.chessplus.app.R

/*
 * Maquettes de la phase de design (docs/DESIGN.md, direction « Accent pleine largeur »).
 * Ce package est jetable : ses composables ne sont pas réutilisés tels quels en phase 5.
 */

/**
 * Couleurs de la direction « Accent pleine largeur ».
 */
data class DesignColors(
	val background: Color,
	val surface: Color,
	val text: Color,
	val muted: Color,
	val primary: Color,
	val onPrimary: Color,
	val success: Color,
	val error: Color,
	val track: Color,
	val squareLight: Color,
	val squareDark: Color,
	val lastMove: Color,
	val selected: Color,
	val dot: Color,
	val coordOnLight: Color,
	val coordOnDark: Color,
)

/** Palette claire. */
val kLightColors = DesignColors(
	background = Color(0xFFF4F5F6),
	surface = Color(0xFFFFFFFF),
	text = Color(0xFF111111),
	muted = Color(0xFF8A8E95),
	primary = Color(0xFF111111),
	onPrimary = Color(0xFFF4F5F6),
	success = Color(0xFF3E8E5A),
	error = Color(0xFFC2483D),
	track = Color(0xFFE3E5E8),
	squareLight = Color(0xFFEEF0F1),
	squareDark = Color(0xFFB5BAC0),
	lastMove = Color(0x3D3E8E5A),
	selected = Color(0x3D111111),
	dot = Color(0x45111111),
	coordOnLight = Color(0x45111111),
	coordOnDark = Color(0x96FFFFFF),
)

/** Palette sombre. */
val kDarkColors = DesignColors(
	background = Color(0xFF0F1011),
	surface = Color(0xFF1C1D1F),
	text = Color(0xFFF2F3F4),
	muted = Color(0xFF8A8E95),
	primary = Color(0xFFF2F3F4),
	onPrimary = Color(0xFF0F1011),
	success = Color(0xFF5DB37C),
	error = Color(0xFFE0675C),
	track = Color(0xFF2A2C2F),
	squareLight = Color(0xFFA3A8AE),
	squareDark = Color(0xFF696E75),
	lastMove = Color(0x505DB37C),
	selected = Color(0x50FFFFFF),
	dot = Color(0x50111111),
	coordOnLight = Color(0x50111111),
	coordOnDark = Color(0x6EFFFFFF),
)

/** Couleurs courantes, fournies par [DesignTheme]. */
val kLocalDesignColors = staticCompositionLocalOf { kLightColors }

/**
 * Crée une graisse de la police variable Inter.
 */
private fun interWeight(inWeight: Int): Font =
	Font(R.font.inter, FontWeight(inWeight), variationSettings = FontVariation.Settings(FontVariation.weight(inWeight)))

/** Police Inter (variable), graisses 400 à 800. */
val kInter: FontFamily = FontFamily(listOf(400, 500, 600, 700, 800).map { interWeight(it) })

/**
 * Styles de texte de la direction retenue (tableau « Typographie » de docs/DESIGN.md).
 */
object DesignType {
	/** Très grand nombre (compteurs, cote). */
	val kNumber = style(44, 800)

	/** Titre d'écran. */
	val kScreenTitle = style(28, 800)

	/** Message de retour (« Bon coup ! »). */
	val kFeedback = style(26, 800)

	/** Titre de section ou de carte. */
	val kSection = style(17, 700)

	/** Titre de barre. */
	val kBarTitle = style(16, 600)

	/** Corps de texte. */
	val kBody = style(14, 400)

	/** Corps de texte appuyé. */
	val kBodyStrong = style(14, 600)

	/** Cote du joueur dans la barre. */
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
 * Thème des maquettes : palette claire ou sombre selon le système, fond plein écran.
 */
@Composable
fun DesignTheme(
	inDark: Boolean = isSystemInDarkTheme(),
	inContent: @Composable () -> Unit,
) {
	val vColors = if (inDark) kDarkColors else kLightColors
	CompositionLocalProvider(kLocalDesignColors provides vColors) {
		Box(Modifier.fillMaxSize().background(vColors.background)) {
			inContent()
		}
	}
}

/** Appareil de référence : Galaxy S24 (1080 × 2340 px, 384 × 832 dp). */
const val kDevice = "spec:width=384dp,height=832dp,dpi=450"

/**
 * Prévisualise un écran en clair et en sombre sur l'appareil de référence.
 */
@Preview(name = "Clair", device = kDevice, showSystemUi = true, uiMode = Configuration.UI_MODE_NIGHT_NO or Configuration.UI_MODE_TYPE_NORMAL)
@Preview(name = "Sombre", device = kDevice, showSystemUi = true, uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
annotation class DesignPreviews
