package com.chessplus.app.game

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.chessplus.app.DesignPreviews
import com.chessplus.app.kDevice
import com.chessplus.app.ui.board.PieceStyle
import com.chessplus.app.ui.theme.ChessPlusTheme
import com.chessplus.core.model.Color
import com.chessplus.core.model.Move
import com.chessplus.core.model.PieceType
import com.chessplus.core.model.Square
import com.chessplus.core.model.ThemeSet
import com.chessplus.core.puzzle.PuzzleResult
import com.chessplus.core.rules.defaultChessRules

/*
 * Captures de l'écran de jeu réel (GameContent), d'après la maquette chessplus-design/screens/jeu-*.png.
 */

/** Horloge fixe des captures. */
private const val kClock = 1_000_000L

/** Position de la maquette : les blancs jouent leur 9e coup après 8… Fg4. */
private const val kMockupFen = "r2q1rk1/ppp2ppp/2np1n2/2b1p1B1/2B1P1b1/2NP1N2/PPP2PPP/R2Q1RK1 w - - 0 9"

/**
 * Retourne les pièces d'une position FEN.
 */
private fun piecesOf(inFen: String) = defaultChessRules().positionFromFen(inFen).let { inPosition ->
	Square.kAll.mapNotNull { vSquare -> inPosition.pieceAt(vSquare)?.let { vSquare to it } }.toMap()
}

/**
 * Raccourci : cases à partir de leurs noms.
 */
private fun squares(vararg inNames: String) = inNames.map { Square.fromAlgebraic(it) }.toSet()

/** État de base reproduisant la maquette (puzzle #4821, fourchette, session 7 / 2 / +18, 0:42). */
private val kMockupState = GameUiState(
	header = GameHeader.ToMove(Color.White, afterCorrect = true),
	pieces = piecesOf(kMockupFen),
	lastMove = Move.fromUci("c8g4"),
	selected = Square.fromAlgebraic("f3"),
	targets = squares("e1", "h4", "d4", "d2", "e5"),
	interactive = true,
	rating = 1642,
	hintEnabled = true,
	solutionEnabled = true,
	skipEnabled = true,
	puzzleId = "4821",
	puzzleRating = 1580,
	puzzleThemes = ThemeSet.parse("fork middlegame short"),
	notation = listOf(
		NotationToken("8…", NotationKind.Past),
		NotationToken("Bg4", NotationKind.Past),
		NotationToken("9.", NotationKind.Current),
		NotationToken("?", NotationKind.Pending),
	),
	remainingMoves = 1,
	clockStart = kClock - 42_000,
	tally = SessionTally(7, 2, 18),
)

/**
 * Affiche l'écran de jeu pour un état donné, sans actions ; [inExitStep] montre une fenêtre de sortie.
 */
@Composable
private fun Shot(inState: GameUiState, inTitle: String = "Arcade", inExitStep: ExitStep = ExitStep.None, inPieces: PieceStyle = PieceStyle.Classic) = ChessPlusTheme(inPieces = inPieces) {
	GameContent(inState, inTitle, true, {}, {}, {}, { _, _ -> }, {}, {}, {}, {}, {}, {}, inExitStep = inExitStep, inClock = { kClock })
}

/** Style de pièces Géométrique. */
@PreviewTest
@DesignPreviews
@Composable
fun RealGameGeometricShot() = Shot(kMockupState, inPieces = PieceStyle.Geometric)

/** Style de pièces Lettres. */
@PreviewTest
@DesignPreviews
@Composable
fun RealGameLettersShot() = Shot(kMockupState, inPieces = PieceStyle.Letters)

/** Arcade : confirmation avant de quitter la session. */
@PreviewTest
@DesignPreviews
@Composable
fun RealGameExitConfirmShot() = Shot(kMockupState.copy(sessionRecap = true, keepsPuzzle = true), inExitStep = ExitStep.Confirm)

/** Entraînement : confirmation avant de quitter (le puzzle en cours n'est pas gardé). */
@PreviewTest
@DesignPreviews
@Composable
fun RealGameTrainingExitShot() = Shot(kMockupState.copy(sessionRecap = true, showThemeTag = true), "Entraînement", ExitStep.Confirm)

/** Arcade : récapitulatif de la session. */
@PreviewTest
@DesignPreviews
@Composable
fun RealGameSessionRecapShot() = Shot(kMockupState.copy(sessionRecap = true, keepsPuzzle = true, tally = SessionTally(7, 2, 18, 41_000L), sessionStartedAt = kClock - 12 * 60_000L), inExitStep = ExitStep.Recap)

/** Maquette : bon coup, il reste un coup, fou f3 sélectionné et coups possibles. */
@PreviewTest
@DesignPreviews
@Composable
fun RealGameShot() = Shot(kMockupState)

/** Entraînement : seul mode où le thème principal est affiché pendant le puzzle. */
@PreviewTest
@DesignPreviews
@Composable
fun RealGameTrainingShot() = Shot(kMockupState.copy(showThemeTag = true), "Entraînement")

/** Indice : la pièce à jouer est cerclée d'`accent`. */
@PreviewTest
@DesignPreviews
@Composable
fun RealGameHintPieceShot() = Shot(
	kMockupState.copy(
		header = GameHeader.HintPiece(PieceType.Knight),
		selected = null,
		targets = emptySet(),
		hint = Square.fromAlgebraic("f3"),
		hintEnabled = false,
	)
)

/** Mauvais coup : retour `danger`. */
@PreviewTest
@DesignPreviews
@Composable
fun RealGameWrongShot() = Shot(
	kMockupState.copy(header = GameHeader.Wrong, selected = null, targets = emptySet(), interactive = false, hintEnabled = false, solutionEnabled = false)
)

/** Puzzle résolu : classement mis à jour, bouton « Puzzle suivant ». */
@PreviewTest
@DesignPreviews
@Composable
fun RealGameSolvedShot() = Shot(
	kMockupState.copy(
		header = GameHeader.Finished(success = true, mate = false, hintsUsed = false),
		selected = null,
		targets = emptySet(),
		interactive = false,
		notation = kMockupState.notation.dropLast(1).map { it.copy(kind = NotationKind.Past) } + NotationToken("Nxe5", NotationKind.Past),
		clockEnd = kClock,
		summary = GameSummary(PuzzleResult.Success, 0, 0, 1630.0, 1642.0, true, 1580, ThemeSet.parse("fork middlegame short")),
		tally = SessionTally(8, 2, 30),
	)
)

/** Puzzle du jour résolu : calendrier des jours résolus à la place de l'enchaînement. */
@PreviewTest
@DesignPreviews
@Composable
fun RealGameDailyShot() = Shot(
	kMockupState.copy(
		header = GameHeader.Finished(success = true, mate = false, hintsUsed = false),
		selected = null,
		targets = emptySet(),
		interactive = false,
		clockEnd = kClock,
		summary = GameSummary(PuzzleResult.Success, 0, 0, 1630.0, 1642.0, true, 1580, ThemeSet.parse("fork middlegame short")),
		daily = DailyUi("20261123", "20261123", setOf("20261102", "20261103", "20261105", "20261110", "20261111", "20261112", "20261117", "20261120", "20261123")),
	),
	"Puzzle du jour",
)

/** Réglage « Confirmer les coups » : coup en attente. */
@PreviewTest
@DesignPreviews
@Composable
fun RealGameConfirmShot() = Shot(kMockupState.copy(selected = null, targets = emptySet(), interactive = false, pendingConfirm = true))

/** Survie : deux vies, sept puzzles résolus. */
@PreviewTest
@DesignPreviews
@Composable
fun RealGameSurvivalShot() = Shot(
	kMockupState.copy(
		header = GameHeader.ToMove(Color.White, afterCorrect = false),
		selected = null,
		targets = emptySet(),
		hintEnabled = false,
		solutionEnabled = false,
		challenge = ChallengeUi(ChallengeKind.Survival, 7, 2, null, null),
		challengeBest = 41,
		tally = SessionTally(7, 1, null),
	),
	"Survie",
)

/** Rush : 2:12 restantes, puzzle réussi. */
@PreviewTest
@DesignPreviews
@Composable
fun RealGameRushShot() = Shot(
	kMockupState.copy(
		header = GameHeader.Solved,
		selected = null,
		targets = emptySet(),
		interactive = false,
		hintEnabled = false,
		solutionEnabled = false,
		challenge = ChallengeUi(ChallengeKind.Rush, 12, 3, kClock + 132_000, null),
		challengeBest = 28,
		tally = SessionTally(12, 3, null),
	),
	"Rush",
)

/** Rush terminé : nouveau record, Accueil et Rejouer. */
@PreviewTest
@DesignPreviews
@Composable
fun RealGameChallengeOverShot() = Shot(
	kMockupState.copy(
		header = GameHeader.ChallengeOver(ChallengeEnd.TimeUp),
		selected = null,
		targets = emptySet(),
		interactive = false,
		challenge = ChallengeUi(ChallengeKind.Rush, 31, 3, kClock - 1, ChallengeResult(ChallengeEnd.TimeUp, 31, 31, true, 1890)),
		challengeBest = 31,
		tally = SessionTally(31, 4, null),
	),
	"Rush",
)

/** Fin de Survie : récapitulatif (réussis, ratés, temps moyen, record). */
@PreviewTest
@DesignPreviews
@Composable
fun RealGameSurvivalRecapShot() = Shot(
	kMockupState.copy(
		header = GameHeader.ChallengeOver(ChallengeEnd.NoLives),
		selected = null,
		targets = emptySet(),
		interactive = false,
		challenge = ChallengeUi(ChallengeKind.Survival, 17, 0, null, ChallengeResult(ChallengeEnd.NoLives, 17, 41, false, 1745, failed = 3, averageDurationMs = 38_000L)),
		challengeBest = 41,
		tally = SessionTally(17, 3, null),
	),
	"Survie",
)

/** Fin de Rush : récapitulatif, nouveau record. */
@PreviewTest
@DesignPreviews
@Composable
fun RealGameRushRecapShot() = Shot(
	kMockupState.copy(
		header = GameHeader.ChallengeOver(ChallengeEnd.TimeUp),
		selected = null,
		targets = emptySet(),
		interactive = false,
		challenge = ChallengeUi(ChallengeKind.Rush, 31, 3, kClock - 1, ChallengeResult(ChallengeEnd.TimeUp, 31, 31, true, 1890, failed = 4, averageDurationMs = 5_000L)),
		challengeBest = 31,
		tally = SessionTally(31, 4, null, 5_000L),
	),
	"Rush",
)

/** Joueur noir : échiquier retourné, trait aux noirs. */
@PreviewTest
@DesignPreviews
@Composable
fun RealGameBlackShot() = Shot(
	kMockupState.copy(
		header = GameHeader.ToMove(Color.Black, afterCorrect = false),
		pieces = piecesOf("r5k1/p4ppp/2p2n2/1p6/3P1qb1/2NQ2R1/PPB2PP1/R1B1r1K1 w - - 0 19"),
		orientation = Color.Black,
		sideToMove = Color.Black,
		lastMove = Move.fromUci("e8e1"),
		selected = null,
		targets = emptySet(),
		notation = listOf(NotationToken("18…", NotationKind.Past), NotationToken("Re1+", NotationKind.Past), NotationToken("19.", NotationKind.Past), NotationToken("Rxe1", NotationKind.Past), NotationToken("19…", NotationKind.Current), NotationToken("?", NotationKind.Pending)),
		puzzleThemes = ThemeSet.parse("backRankMate mate mateIn2"),
	)
)

/** Maquette avec une police agrandie de 30 %. */
@PreviewTest
@Preview(name = "Grande police", device = kDevice, showSystemUi = true, fontScale = 1.3f)
@Composable
fun RealGameLargeFontShot() = Shot(kMockupState)
