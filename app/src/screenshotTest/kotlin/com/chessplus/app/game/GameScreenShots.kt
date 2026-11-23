package com.chessplus.app.game

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.chessplus.app.design.preview.kDevice
import com.android.tools.screenshot.PreviewTest
import com.chessplus.app.design.preview.DesignPreviews
import com.chessplus.app.ui.theme.ChessPlusTheme
import com.chessplus.core.model.Color
import com.chessplus.core.model.Move
import com.chessplus.core.model.PieceType
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

/** Horloge fixe des captures du Rush. */
private const val kClock = 1_000_000L

/**
 * Affiche l'écran de jeu pour un état donné, sans actions.
 */
@Composable
private fun Shot(inState: GameUiState, inTitle: String = "Arcade") = ChessPlusTheme {
	GameContent(inState, inTitle, true, {}, {}, {}, { _, _ -> }, {}, {}, {}, {}, {}, inClock = { kClock })
}

/** Survie : deux vies, sept problèmes résolus. */
@PreviewTest
@DesignPreviews
@Composable
fun RealGameSurvivalShot() = Shot(
	GameUiState(
		header = GameHeader.ToMove(Color.White, afterCorrect = false),
		pieces = piecesOf("q5nr/1ppknQpp/3p4/1P2p3/4P3/B1PP1b2/B5PP/5K2 w - - 1 18"),
		lastMove = Move.fromUci("e8d7"),
		interactive = true,
		challenge = ChallengeUi(ChallengeKind.Survival, 7, 2, null, null),
	),
	"Survie",
)

/** Survie : erreur, le coup attendu est montré. */
@PreviewTest
@DesignPreviews
@Composable
fun RealGameSurvivalMissedShot() = Shot(
	GameUiState(
		header = GameHeader.Missed(Move.fromUci("a2e6")),
		pieces = piecesOf("q5nr/1ppknQpp/3pB3/1P2p3/4P3/B1PP1b2/6PP/5K2 b - - 0 18"),
		lastMove = Move.fromUci("a2e6"),
		challenge = ChallengeUi(ChallengeKind.Survival, 7, 1, null, null),
	),
	"Survie",
)

/** Rush : 3 min 12 s restantes, douze problèmes résolus. */
@PreviewTest
@DesignPreviews
@Composable
fun RealGameRushShot() = Shot(
	GameUiState(
		header = GameHeader.Solved,
		pieces = piecesOf("q5nr/1ppkQ1pp/3pB3/1P2p3/4P3/B1PP1b2/6PP/5K2 b - - 0 19"),
		lastMove = Move.fromUci("f7e7"),
		badge = Square.fromAlgebraic("e7") to BadgeKind.Success,
		challenge = ChallengeUi(ChallengeKind.Rush, 12, 3, kClock + 192_000, null),
	),
	"Rush",
)

/** Rush terminé : bilan et nouveau record. */
@PreviewTest
@DesignPreviews
@Composable
fun RealGameChallengeOverShot() = Shot(
	GameUiState(
		header = GameHeader.ChallengeOver(ChallengeEnd.TimeUp),
		pieces = piecesOf("q5nr/1ppkQ1pp/3pB3/1P2p3/4P3/B1PP1b2/6PP/5K2 b - - 0 19"),
		lastMove = Move.fromUci("f7e7"),
		challenge = ChallengeUi(ChallengeKind.Rush, 24, 3, kClock - 1, ChallengeResult(ChallengeEnd.TimeUp, 24, 24, true, 1890)),
	),
	"Rush",
)

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

/** Premier appui sur « Indice » : le fou a2 est en surbrillance. */
@PreviewTest
@DesignPreviews
@Composable
fun RealGameHintPieceShot() = Shot(
	GameUiState(
		header = GameHeader.HintPiece(PieceType.Bishop),
		pieces = piecesOf("q5nr/1ppknQpp/3p4/1P2p3/4P3/B1PP1b2/B5PP/5K2 w - - 1 18"),
		lastMove = Move.fromUci("e8d7"),
		hint = Square.fromAlgebraic("a2"),
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
		header = GameHeader.Finished(success = true, mate = true, hintsUsed = false),
		pieces = piecesOf("q2k1Qnr/1pp1n1pp/3pB3/1P2p3/4P3/B1PP1b2/6PP/5K2 b - - 3 19"),
		lastMove = Move.fromUci("f7f8"),
		badge = Square.fromAlgebraic("f8") to BadgeKind.Success,
		rating = 1532,
		nextEnabled = true,
		summary = GameSummary(PuzzleResult.Success, 0, 0, 1520.0, 1532.0, true, 1760, ThemeSet.parse("mate mateIn2 middlegame short")),
	)
)

/** Mauvais coup montré : le fou est allé en b3, pastille d'erreur, avant de revenir en a2. */
@PreviewTest
@DesignPreviews
@Composable
fun RealGameWrongShot() = Shot(
	GameUiState(
		header = GameHeader.Wrong,
		pieces = piecesOf("q5nr/1ppknQpp/3p4/1P2p3/4P3/BBPP1b2/6PP/5K2 w - - 1 18"),
		lastMove = Move.fromUci("e8d7"),
		badge = Square.fromAlgebraic("b3") to BadgeKind.Error,
		rating = 1520,
	)
)

/** Joueur au trait avec une police agrandie de 30 %. */
@PreviewTest
@Preview(name = "Grande police", device = kDevice, showSystemUi = true, fontScale = 1.3f)
@Composable
fun RealGameLargeFontShot() = Shot(
	GameUiState(
		header = GameHeader.ToMove(Color.White, afterCorrect = false),
		pieces = piecesOf("q5nr/1ppknQpp/3p4/1P2p3/4P3/B1PP1b2/B5PP/5K2 w - - 1 18"),
		lastMove = Move.fromUci("e8d7"),
		interactive = true,
		rating = 1431,
		hintEnabled = true,
	)
)

/** Bilan avec une police agrandie de 30 %. */
@PreviewTest
@Preview(name = "Grande police", device = kDevice, showSystemUi = true, fontScale = 1.3f)
@Composable
fun RealSummaryLargeFontShot() = Shot(
	GameUiState(
		header = GameHeader.Finished(success = false, mate = true, hintsUsed = true),
		pieces = piecesOf("q2k1Qnr/1pp1n1pp/3pB3/1P2p3/4P3/B1PP1b2/6PP/5K2 b - - 3 19"),
		lastMove = Move.fromUci("f7f8"),
		rating = 1431,
		nextEnabled = true,
		summary = GameSummary(PuzzleResult.Failure, 1, 1, 1440.0, 1431.0, true, 1760, ThemeSet.parse("mate mateIn2 middlegame short")),
	)
)
