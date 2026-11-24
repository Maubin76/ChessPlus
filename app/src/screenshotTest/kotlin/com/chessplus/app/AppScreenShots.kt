package com.chessplus.app

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest


import com.chessplus.app.game.TrainingCriteria
import com.chessplus.app.history.HistoryContent
import com.chessplus.app.history.HistoryRow
import com.chessplus.app.home.DailyInfo
import com.chessplus.app.home.HomeContent
import com.chessplus.app.home.HomeUiState
import com.chessplus.app.level.LevelScreen
import com.chessplus.app.settings.Settings
import com.chessplus.app.settings.SettingsContent
import com.chessplus.app.training.TargetedContent
import com.chessplus.app.training.TargetedUiState
import com.chessplus.app.ui.theme.BoardTheme
import com.chessplus.app.ui.theme.ChessPlusTheme
import com.chessplus.core.model.Color
import com.chessplus.core.model.Square
import com.chessplus.core.model.Theme
import com.chessplus.core.model.ThemeSet
import com.chessplus.core.rules.defaultChessRules

/*
 * Captures des écrans réels (contenus sans état) avec des données factices, d'après les maquettes
 * chessplus-design/screens.
 */

/** Date de référence des captures (lundi 23 novembre 2026, 12 h). */
private const val kNow = 1_795_431_600_000L

/**
 * Retourne les pièces d'une position FEN.
 */
private fun piecesOf(inFen: String) = defaultChessRules().positionFromFen(inFen).let { inPosition ->
	Square.kAll.mapNotNull { vSquare -> inPosition.pieceAt(vSquare)?.let { vSquare to it } }.toMap()
}

/** État de l'accueil de la maquette : série de 9 jours, mat en 1 du jour, records 28 et 41. */
private val kHomeState = HomeUiState(
	rating = 1642,
	dayStreak = 9,
	daily = DailyInfo("20261123", "4821", piecesOf("6k1/5ppp/8/8/8/8/5PPP/3R2K1 w - - 0 1"), Color.White, 1210, Theme.MateIn1),
	survivalBest = 41,
	rushBest = 28,
	offlineCount = 12_400,
	goal = com.chessplus.app.goal.GoalProgress(3, 5),
)

/** Accueil. */
@PreviewTest
@DesignPreviews
@Composable
fun RealHomeShot() = ChessPlusTheme { HomeContent(kHomeState, kNow, {}, {}, {}, {}, {}) }

/** Accueil : puzzle du jour déjà résolu, à rejouer, et objectif atteint. */
@PreviewTest
@DesignPreviews
@Composable
fun RealHomeDailySolvedShot() = ChessPlusTheme {
	HomeContent(kHomeState.copy(solvedDays = setOf("20261123"), goal = com.chessplus.app.goal.GoalProgress(6, 5)), kNow, {}, {}, {}, {}, {})
}

/** Réglages avec le rappel de l'objectif activé. */
@PreviewTest
@DesignPreviews
@Composable
fun RealSettingsGoalShot() = ChessPlusTheme {
	SettingsContent(Settings(dailyGoal = 10, reminder = true, reminderMinutes = 19 * 60 + 30), {}, "1.0", com.chessplus.app.OfflineBaseInfo(12_400, 48_000_000), {}, {})
}

/** Accueil d'un nouveau joueur : pas de série, donc pas de flamme. */
@PreviewTest
@DesignPreviews
@Composable
fun RealHomeNoStreakShot() = ChessPlusTheme { HomeContent(kHomeState.copy(dayStreak = 0, survivalBest = null, rushBest = null), kNow, {}, {}, {}, {}, {}) }

/** Accueil avec une police agrandie de 30 % (réglage d'accessibilité du téléphone). */
@PreviewTest
@Preview(name = "Grande police", device = kDevice, showSystemUi = true, fontScale = 1.3f, uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
fun RealHomeLargeFontShot() = ChessPlusTheme(inDarkTheme = true) { HomeContent(kHomeState, kNow, {}, {}, {}, {}, {}) }

/** Réglages de la maquette. */
@PreviewTest
@DesignPreviews
@Composable
fun RealSettingsShot() = ChessPlusTheme { SettingsContent(Settings(), {}, "1.0", com.chessplus.app.OfflineBaseInfo(12_400, 48_000_000), {}, {}) }

/** Réglages avec le thème d'échiquier Bois. */
@PreviewTest
@DesignPreviews
@Composable
fun RealSettingsWoodShot() = ChessPlusTheme(inBoard = BoardTheme.Wood) {
	SettingsContent(Settings(boardTheme = BoardTheme.Wood, confirmMoves = true), {}, "1.0", com.chessplus.app.OfflineBaseInfo(12_400, 48_000_000), {}, {})
}

/** Premier lancement : choix du niveau. */
@PreviewTest
@DesignPreviews
@Composable
fun RealLevelShot() = ChessPlusTheme { LevelScreen(inFirstLaunch = true, inOnBack = {}, inOnChoose = {}) }

/** Entraînement avec des thèmes choisis. */
@PreviewTest
@DesignPreviews
@Composable
fun RealTargetedShot() = ChessPlusTheme {
	TargetedContent(TargetedUiState(TrainingCriteria(ThemeSet.of(Theme.Fork, Theme.MateIn2) + Theme.PawnEndgame + Theme.RookEndgame + Theme.BishopEndgame + Theme.KnightEndgame + Theme.QueenEndgame + Theme.QueenRookEndgame), 1520, 3869), {}, {}, {}, {}, {}, {}, {})
}

/** Entraînement avec une police agrandie de 30 %. */
@PreviewTest
@Preview(name = "Grande police", device = kDevice, showSystemUi = true, fontScale = 1.3f)
@Composable
fun RealTargetedLargeFontShot() = ChessPlusTheme(inDarkTheme = false) {
	TargetedContent(TargetedUiState(TrainingCriteria(ThemeSet.of(Theme.Fork, Theme.MateIn2)), 1520, 3869), {}, {}, {}, {}, {}, {}, {})
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
