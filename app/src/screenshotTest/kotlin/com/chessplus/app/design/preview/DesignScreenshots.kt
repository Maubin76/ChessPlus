package com.chessplus.app.design.preview

import androidx.compose.runtime.Composable
import com.android.tools.screenshot.PreviewTest

/*
 * Captures des maquettes de la phase de design, générées par :
 * ./gradlew :app:updateDebugScreenshotTest
 * Chaque fonction rend une maquette de design.preview en clair et en sombre.
 */

/** Capture de [GameSelectionPreview]. */
@PreviewTest
@DesignPreviews
@Composable
fun GameSelectionShot() = GameSelectionPreview()

/** Capture de [GameCorrectPreview]. */
@PreviewTest
@DesignPreviews
@Composable
fun GameCorrectShot() = GameCorrectPreview()

/** Capture de [GameWrongPreview]. */
@PreviewTest
@DesignPreviews
@Composable
fun GameWrongShot() = GameWrongPreview()

/** Capture de [GamePromotionPreview]. */
@PreviewTest
@DesignPreviews
@Composable
fun GamePromotionShot() = GamePromotionPreview()

/** Capture de [GameSolvedPreview]. */
@PreviewTest
@DesignPreviews
@Composable
fun GameSolvedShot() = GameSolvedPreview()

/** Capture de [GameFailedPreview]. */
@PreviewTest
@DesignPreviews
@Composable
fun GameFailedShot() = GameFailedPreview()

/** Capture de [LevelChoicePreview]. */
@PreviewTest
@DesignPreviews
@Composable
fun LevelChoiceShot() = LevelChoicePreview()

/** Capture de [HomeResumePreview]. */
@PreviewTest
@DesignPreviews
@Composable
fun HomeResumeShot() = HomeResumePreview()

/** Capture de [HomePreview]. */
@PreviewTest
@DesignPreviews
@Composable
fun HomeShot() = HomePreview()

/** Capture de [TargetedTrainingPreview]. */
@PreviewTest
@DesignPreviews
@Composable
fun TargetedTrainingShot() = TargetedTrainingPreview()

/** Capture de [HistoryPreview]. */
@PreviewTest
@DesignPreviews
@Composable
fun HistoryShot() = HistoryPreview()

/** Capture de [StatsPreview]. */
@PreviewTest
@DesignPreviews
@Composable
fun StatsShot() = StatsPreview()

/** Capture de [SettingsPreview]. */
@PreviewTest
@DesignPreviews
@Composable
fun SettingsShot() = SettingsPreview()
