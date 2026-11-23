package com.chessplus.app

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.chessplus.app.design.preview.kDevice
import com.android.tools.screenshot.PreviewTest
import com.chessplus.app.design.preview.DesignPreviews
import com.chessplus.app.history.HistoryContent
import com.chessplus.app.history.HistoryRow
import com.chessplus.app.home.HomeContent
import com.chessplus.app.home.HomeUiState
import com.chessplus.app.home.ResumeInfo
import com.chessplus.app.level.LevelScreen
import com.chessplus.app.settings.Settings
import com.chessplus.app.settings.SettingsContent
import com.chessplus.app.training.TargetedContent
import com.chessplus.app.training.TargetedUiState
import com.chessplus.app.game.TrainingCriteria
import com.chessplus.app.ui.theme.ChessPlusTheme
import com.chessplus.core.model.Color
import com.chessplus.core.model.Square
import com.chessplus.core.model.Theme
import com.chessplus.core.model.ThemeSet
import com.chessplus.core.rules.defaultChessRules

/*
 * Captures des écrans réels (contenus sans état) avec des données factices.
 */

/** Date de référence des captures (20 novembre 2026, 12 h). */
private const val kNow = 1_795_172_400_000L

/**
 * Retourne les pièces d'une position FEN.
 */
private fun piecesOf(inFen: String) = defaultChessRules().positionFromFen(inFen).let { inPosition ->
	Square.kAll.mapNotNull { vSquare -> inPosition.pieceAt(vSquare)?.let { vSquare to it } }.toMap()
}

/** Premier lancement : choix du niveau. */
@PreviewTest
@DesignPreviews
@Composable
fun RealLevelShot() = ChessPlusTheme { LevelScreen(inFirstLaunch = true, inOnBack = {}, inOnChoose = {}) }

/** Accueil avec un problème en cours. */
@PreviewTest
@DesignPreviews
@Composable
fun RealHomeShot() = ChessPlusTheme {
	HomeContent(
		HomeUiState(
			solvedCount = 128,
			rating = 1520,
			resume = ResumeInfo(piecesOf("q5nr/1ppknQpp/3pB3/1P2p3/4P3/B1PP1b2/6PP/5K2 b - - 0 1"), Color.White, 2, 2, false, kNow - 3 * 60_000),
			dayStreak = 4,
			successRate = 0.62f,
			survivalBest = 23,
		),
		kNow, {}, {}, {}, {},
	)
}

/** Entraînement ciblé avec deux thèmes choisis. */
@PreviewTest
@DesignPreviews
@Composable
fun RealTargetedShot() = ChessPlusTheme {
	TargetedContent(TargetedUiState(TrainingCriteria(ThemeSet.of(Theme.Fork, Theme.MateIn2) + Theme.PawnEndgame + Theme.RookEndgame + Theme.BishopEndgame + Theme.KnightEndgame + Theme.QueenEndgame + Theme.QueenRookEndgame), 1520, 3869), {}, {}, {}, {}, {}, {}, {})
}

/** Historique. */
@PreviewTest
@DesignPreviews
@Composable
fun RealHistoryShot() = ChessPlusTheme {
	val vPieces = piecesOf("q5nr/1ppknQpp/3p4/1P2p3/4P3/B1PP1b2/B5PP/5K2 w - - 0 1")
	HistoryContent(
		listOf(
			HistoryRow(3, "00sHx", vPieces, Color.White, ThemeSet.parse("mate mateIn2 middlegame"), 1760, kNow - 5 * 60_000, true, 12),
			HistoryRow(2, "00sJ9", vPieces, Color.White, ThemeSet.parse("fork sacrifice"), 2671, kNow - 12 * 60_000, false, -9),
			HistoryRow(1, "00sO1", vPieces, Color.White, ThemeSet.parse("discoveredAttack"), 998, kNow - 26 * 60 * 60_000, true, null),
		),
		kNow, {}, {},
	)
}

/** Réglages. */
@PreviewTest
@DesignPreviews
@Composable
fun RealSettingsShot() = ChessPlusTheme { SettingsContent(Settings(), {}, "0.1.0", {}, {}) }

/** Accueil avec une police agrandie de 30 % (réglage d'accessibilité du téléphone). */
@PreviewTest
@Preview(name = "Grande police", device = kDevice, showSystemUi = true, fontScale = 1.3f, uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
fun RealHomeLargeFontShot() = ChessPlusTheme(inDarkTheme = true) {
	HomeContent(
		HomeUiState(
			solvedCount = 1,
			rating = 1431,
			resume = ResumeInfo(piecesOf("q5nr/1ppknQpp/3pB3/1P2p3/4P3/B1PP1b2/6PP/5K2 b - - 0 1"), Color.White, 2, 2, true, kNow - 60 * 60_000),
		),
		kNow, {}, {}, {}, {},
	)
}

/** Entraînement ciblé avec une police agrandie de 30 %. */
@PreviewTest
@Preview(name = "Grande police", device = kDevice, showSystemUi = true, fontScale = 1.3f)
@Composable
fun RealTargetedLargeFontShot() = ChessPlusTheme(inDarkTheme = false) {
	TargetedContent(TargetedUiState(TrainingCriteria(ThemeSet.of(Theme.Fork, Theme.MateIn2)), 1520, 3869), {}, {}, {}, {}, {}, {}, {})
}
