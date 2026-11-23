package com.chessplus.app

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.android.tools.screenshot.PreviewTest
import com.chessplus.app.design.preview.DesignPreviews
import com.chessplus.app.game.BadgeKind
import com.chessplus.app.game.GameContent
import com.chessplus.app.game.GameHeader
import com.chessplus.app.game.GameUiState
import com.chessplus.app.home.HomeContent
import com.chessplus.app.home.HomeUiState
import com.chessplus.app.home.ResumeInfo
import com.chessplus.app.ui.theme.ChessColors
import com.chessplus.app.ui.theme.ChessPlusTheme
import com.chessplus.app.ui.theme.kDarkChessColors
import com.chessplus.app.ui.theme.kLightChessColors
import com.chessplus.core.model.Move
import com.chessplus.core.model.Square
import com.chessplus.core.rules.defaultChessRules

/*
 * Propositions de vert dans l'application, pour l'accorder à l'icône « Halo » (captures de comparaison).
 * A : accents verts. B : A + échiquier vert. C : B + fonds teintés de vert.
 */

/** Date de référence des captures. */
private const val kNow = 1_795_172_400_000L

/** A, clair : accents verts. */
private val kGreenALight = kLightChessColors.copy(primary = Color(0xFF2F7D4F), onPrimary = Color(0xFFFFFFFF))

/** A, sombre : accents verts. */
private val kGreenADark = kDarkChessColors.copy(primary = Color(0xFF5DB37C), onPrimary = Color(0xFF0F1011))

/** B, clair : A + échiquier vert, dernier coup en jaune. */
private val kGreenBLight = kGreenALight.copy(
	squareLight = Color(0xFFE7EFE4),
	squareDark = Color(0xFF7FA98A),
	lastMove = Color(0x80F2E66B),
	coordOnLight = Color(0x80557A61),
	coordOnDark = Color(0xB3FFFFFF),
)

/** B, sombre : A + échiquier vert. */
private val kGreenBDark = kGreenADark.copy(
	squareLight = Color(0xFFA9C2AE),
	squareDark = Color(0xFF5F8768),
	lastMove = Color(0x80E8D85C),
	coordOnLight = Color(0x80405C49),
	coordOnDark = Color(0x99FFFFFF),
)

/** C, clair : B + fonds teintés de vert. */
private val kGreenCLight = kGreenBLight.copy(background = Color(0xFFEDF3EE), track = Color(0xFFD9E5DC), onPrimary = Color(0xFFFFFFFF))

/** C, sombre : B + fonds teintés de vert. */
private val kGreenCDark = kGreenBDark.copy(background = Color(0xFF0C1511), surface = Color(0xFF15221B), track = Color(0xFF22352A))

/**
 * Retourne les pièces d'une position FEN.
 */
private fun piecesOf(inFen: String) = defaultChessRules().positionFromFen(inFen).let { inPosition ->
	Square.kAll.mapNotNull { vSquare -> inPosition.pieceAt(vSquare)?.let { vSquare to it } }.toMap()
}

/**
 * Accueil dans une palette donnée.
 */
@Composable
private fun Home(inColors: ChessColors) = ChessPlusTheme(inColors = inColors) {
	HomeContent(
		HomeUiState(128, 1520, ResumeInfo(piecesOf("q5nr/1ppknQpp/3pB3/1P2p3/4P3/B1PP1b2/6PP/5K2 b - - 0 1"), com.chessplus.core.model.Color.White, 2, 2, false, kNow - 3 * 60_000)),
		kNow, {}, {}, {}, {},
	)
}

/**
 * Écran de jeu (bon coup) dans une palette donnée.
 */
@Composable
private fun Game(inColors: ChessColors) = ChessPlusTheme(inColors = inColors) {
	GameContent(
		GameUiState(
			header = GameHeader.Correct(com.chessplus.core.model.Color.Black),
			pieces = piecesOf("q5nr/1ppknQpp/3pB3/1P2p3/4P3/B1PP1b2/6PP/5K2 b - - 0 1"),
			lastMove = Move.fromUci("a2e6"),
			badge = Square.fromAlgebraic("e6") to BadgeKind.Success,
			rating = 1520,
			hintEnabled = true,
		),
		"Entraînement", true, {}, {}, {}, { _, _ -> }, {}, {}, {}, {}, {},
	)
}

/** A, accueil. */
@PreviewTest
@DesignPreviews
@Composable
fun GreenAHomeShot() = Home(if (isSystemInDarkTheme()) kGreenADark else kGreenALight)

/** A, jeu. */
@PreviewTest
@DesignPreviews
@Composable
fun GreenAGameShot() = Game(if (isSystemInDarkTheme()) kGreenADark else kGreenALight)

/** B, accueil. */
@PreviewTest
@DesignPreviews
@Composable
fun GreenBHomeShot() = Home(if (isSystemInDarkTheme()) kGreenBDark else kGreenBLight)

/** B, jeu. */
@PreviewTest
@DesignPreviews
@Composable
fun GreenBGameShot() = Game(if (isSystemInDarkTheme()) kGreenBDark else kGreenBLight)

/** C, accueil. */
@PreviewTest
@DesignPreviews
@Composable
fun GreenCHomeShot() = Home(if (isSystemInDarkTheme()) kGreenCDark else kGreenCLight)

/** C, jeu. */
@PreviewTest
@DesignPreviews
@Composable
fun GreenCGameShot() = Game(if (isSystemInDarkTheme()) kGreenCDark else kGreenCLight)
