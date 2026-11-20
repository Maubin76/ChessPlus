package com.chessplus.app.game

import androidx.compose.runtime.Composable
import com.android.tools.screenshot.PreviewTest
import com.chessplus.app.design.preview.DesignPreviews
import com.chessplus.app.ui.theme.ChessPlusTheme
import com.chessplus.core.model.Color
import com.chessplus.core.model.Move
import com.chessplus.core.model.Square
import com.chessplus.core.model.ThemeSet
import com.chessplus.core.puzzle.PuzzleResult
import com.chessplus.core.rules.defaultChessRules

/*
 * Captures de l'écran de jeu réel (GameContent), alimenté par des positions calculées par les règles.
 */

/**
 * Retourne les pièces d'une position FEN.
 */
private fun piecesOf(inFen: String) = defaultChessRules().positionFromFen(inFen).let { inPosition ->
	Square.kAll.mapNotNull { vSquare -> inPosition.pieceAt(vSquare)?.let { vSquare to it } }.toMap()
}

/**
 * Affiche l'écran de jeu pour un état donné, sans actions.
 */
@Composable
private fun Shot(inState: GameUiState) = ChessPlusTheme {
	GameContent(inState, "Entraînement", true, {}, {}, {}, { _, _ -> }, {}, {}, {}, {}, {}, {})
}

/** Joueur au trait, fou a2 sélectionné. */
@PreviewTest
@DesignPreviews
@Composable
fun RealGameSelectionShot() = Shot(
	GameUiState(
		header = GameHeader.ToMove(Color.White, afterCorrect = false),
		pieces = piecesOf("q5nr/1ppknQpp/3p4/1P2p3/4P3/B1PP1b2/B5PP/5K2 w - - 1 18"),
		lastMove = Move.fromUci("e8d7"),
		selected = Square.fromAlgebraic("a2"),
		targets = listOf("b1", "b3", "c4", "d5", "e6").map { Square.fromAlgebraic(it) }.toSet(),
		interactive = true,
		rating = 1520,
		hintEnabled = true,
	)
)

/** Joueur noir (échiquier retourné), bon coup. */
@PreviewTest
@DesignPreviews
@Composable
fun RealGameBlackCorrectShot() = Shot(
	GameUiState(
		header = GameHeader.Correct(Color.White),
		pieces = piecesOf("r5k1/p4ppp/2p2n2/1p6/3P1qb1/2NQ2R1/PPB2PP1/R1B1r1K1 w - - 0 19"),
		orientation = Color.Black,
		lastMove = Move.fromUci("e8e1"),
		badge = Square.fromAlgebraic("e1") to BadgeKind.Success,
		rating = 1520,
	)
)

/** Problème terminé avec le bilan. */
@PreviewTest
@DesignPreviews
@Composable
fun RealGameSummaryShot() = Shot(
	GameUiState(
		header = GameHeader.Finished(success = true, mate = true, solutionShown = false),
		pieces = piecesOf("q2k1Qnr/1pp1n1pp/3pB3/1P2p3/4P3/B1PP1b2/6PP/5K2 b - - 3 19"),
		lastMove = Move.fromUci("f7f8"),
		badge = Square.fromAlgebraic("f8") to BadgeKind.Success,
		rating = 1532,
		nextEnabled = true,
		summary = GameSummary(PuzzleResult.Success, 0, 0, 1520.0, 1532.0, true, 1760, ThemeSet.parse("mate mateIn2 middlegame short"), "https://lichess.org/training/00sHx"),
		summaryVisible = true,
	)
)
