package com.chessplus.app.game

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chessplus.app.R
import com.chessplus.app.ui.board.ChessBoard
import com.chessplus.app.ui.board.rememberPiecePainter
import com.chessplus.app.ui.components.Chip
import com.chessplus.app.ui.components.IconButtonOutlined
import com.chessplus.app.ui.components.PrimaryButton
import com.chessplus.app.ui.components.SecondaryButton
import com.chessplus.app.ui.components.StatTiles
import com.chessplus.app.ui.components.SurfaceCard
import com.chessplus.app.ui.components.Tag
import com.chessplus.app.ui.components.TopBar
import com.chessplus.app.ui.components.formatClock
import com.chessplus.app.ui.components.formatDelta
import com.chessplus.app.ui.components.formatRating
import com.chessplus.app.ui.components.pressClickable
import com.chessplus.app.ui.components.rememberPressFeedback
import com.chessplus.app.stats.duration
import com.chessplus.app.stats.percent
import com.chessplus.app.ui.mainTheme
import com.chessplus.app.ui.theme.ChessPlusTheme
import com.chessplus.app.ui.theme.kMonoFamily
import com.chessplus.app.ui.themeLabel
import com.chessplus.core.model.Color
import com.chessplus.core.model.Piece
import com.chessplus.core.model.PieceType
import com.chessplus.core.model.Square
import com.chessplus.core.puzzle.PuzzleResult
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.roundToInt

/** Intervalle de rafraîchissement des chronomètres. */
private const val kTimerTickMs = 250L

/** Temps restant sous lequel le chronomètre du Rush passe en `danger`. */
private const val kTimerWarningMs = 30_000L

/** Côté du carré de couleur du camp au trait. */
private const val kSideSquare = 12

/** Nombre maximal d'éléments de notation affichés (les plus récents). */
private const val kMaxNotationTokens = 12

/**
 * Ton de la carte de retour sous l'échiquier.
 */
private enum class Tone {
	Neutral,
	Success,
	Danger,
}

/**
 * Retour affiché sous l'échiquier : titre facultatif en couleur, puis précision.
 *
 * @property title titre court (« Bon coup. »), ou null.
 * @property detail précision.
 * @property tone couleur de la bordure et du titre.
 */
private data class Feedback(val title: String?, val detail: String, val tone: Tone)

/**
 * Écran de jeu : échiquier bord à bord, retour immédiat sous les coups, actions à portée de pouce.
 */
@Composable
fun GameScreen(inViewModel: GameViewModel, inTitle: String, inVibration: Boolean, inCoordinates: Boolean, inOnBack: () -> Unit, inSounds: Boolean = false) {
	val vState by inViewModel.state.collectAsStateWithLifecycle()
	val vController = inViewModel.controller
	val vHaptics = LocalHapticFeedback.current
	val vSoundPlayer = rememberSoundPlayer()
	LaunchedEffect(vState.sound?.id) {
		if (inSounds) {
			vState.sound?.let { vSoundPlayer.play(it.kind) }
		}
	}
	LaunchedEffect(vState.haptic?.id) {
		if (inVibration) {
			vState.haptic?.let { vHaptics.performHapticFeedback(if (it.success) HapticFeedbackType.Confirm else HapticFeedbackType.Reject) }
		}
	}
	val vBack: () -> Unit = {
		vController.onLeave()
		inOnBack()
	}
	// Arcade et Entraînement : quitter une session où au moins un puzzle est terminé demande confirmation, puis affiche le récapitulatif.
	var vExitStep by rememberSaveable { mutableStateOf(ExitStep.None) }
	val vConfirmExit = vState.sessionRecap && vState.tally.played > 0
	val vRequestBack: () -> Unit = { if (vConfirmExit) vExitStep = ExitStep.Confirm else vBack() }
	BackHandler(enabled = vState.challenge != null || vConfirmExit, onBack = vRequestBack)
	GameContent(
		inState = vState,
		inTitle = inTitle,
		inCoordinates = inCoordinates,
		inOnBack = vRequestBack,
		inOnTap = vController::onSquareTap,
		inOnDragStart = vController::onDragStart,
		inOnDrop = vController::onDrop,
		inOnHint = vController::onHint,
		inOnSolution = vController::onSolution,
		inOnFlip = vController::onFlip,
		inOnSkip = vController::onSkip,
		inOnPromotion = vController::onPromotionChosen,
		inOnPromotionDismiss = vController::onPromotionDismissed,
		inOnConfirm = vController::onConfirmMove,
		inOnCancel = vController::onCancelMove,
		inOnPlayAgain = vController::onPlayAgain,
		inOnRestart = vController::onRestart,
		inOnDailyDay = vController::onDailyDay,
		inExitStep = vExitStep,
		inOnExitStep = { vExitStep = it },
		inOnLeave = vBack,
		inOnSolutionStep = vController::onSolutionStep,
		inOnSolutionJump = vController::onSolutionJump,
	)
}

/**
 * Contenu de l'écran de jeu, sans état propre (prévisualisable). [inClock] fournit l'heure des chronomètres.
 */
@Composable
fun GameContent(
	inState: GameUiState,
	inTitle: String,
	inCoordinates: Boolean,
	inOnBack: () -> Unit,
	inOnTap: (Square) -> Unit,
	inOnDragStart: (Square) -> Unit,
	inOnDrop: (Square, Square?) -> Unit,
	inOnHint: () -> Unit,
	inOnSolution: () -> Unit,
	inOnFlip: () -> Unit,
	inOnSkip: () -> Unit,
	inOnPromotion: (PieceType) -> Unit,
	inOnPromotionDismiss: () -> Unit,
	inOnConfirm: () -> Unit = {},
	inOnCancel: () -> Unit = {},
	inOnPlayAgain: () -> Unit = {},
	inOnRestart: () -> Unit = {},
	inOnDailyDay: (String) -> Unit = {},
	inExitStep: ExitStep = ExitStep.None,
	inOnExitStep: (ExitStep) -> Unit = {},
	inOnLeave: () -> Unit = inOnBack,
	inOnSolutionStep: (Int) -> Unit = {},
	inOnSolutionJump: (Boolean) -> Unit = {},
	inClock: () -> Long = System::currentTimeMillis,
) {
	val vColors = ChessPlusTheme.colors
	val vSpacing = ChessPlusTheme.spacing
	val vDaily = inState.daily
	// Puzzle du jour terminé : le calendrier remplace le bilan de session et l'enchaînement.
	val vShowCalendar = vDaily != null && inState.header is GameHeader.Finished
	Box(Modifier.fillMaxSize().background(vColors.surface)) {
		Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
			TopBar(vDaily?.let { dailyTitle(it) } ?: inTitle, inOnBack) { TopChip(inState, inClock) }
			PuzzleHeader(inState)
			ChessBoard(
				inPieces = inState.pieces,
				inOrientation = inState.orientation,
				inLastMove = inState.lastMove,
				inMoveId = inState.moveId,
				inAnimateMove = inState.animateMove,
				inSelected = inState.selected,
				inTargets = inState.targets,
				inHint = inState.hint,
				inBadge = inState.badge,
				inCoordinates = inCoordinates,
				inInteractive = inState.interactive,
				inOnTap = inOnTap,
				inOnDragStart = inOnDragStart,
				inOnDrop = inOnDrop,
				inModifier = Modifier.fillMaxWidth(),
			)
			if (vDaily != null && vShowCalendar) {
				// Le calendrier dépasse souvent l'espace sous l'échiquier : il s'ouvre défilé jusqu'en bas
				// (la valeur est ramenée au maximum à la mise en page).
				val vScroll = remember(vDaily.day) { ScrollState(Int.MAX_VALUE) }
				Column(
					Modifier.weight(1f).fillMaxWidth().verticalScroll(vScroll).padding(horizontal = vSpacing.space4, vertical = vSpacing.space3),
					verticalArrangement = Arrangement.spacedBy(vSpacing.space3),
				) {
					FeedbackCard(feedbackOf(inState))
					SolutionNavigator(inState, inOnSolutionStep, inOnSolutionJump)
					DailyCalendar(vDaily, inOnDailyDay)
				}
				Row(Modifier.fillMaxWidth().padding(start = vSpacing.space4, end = vSpacing.space4, bottom = vSpacing.space4), horizontalArrangement = Arrangement.spacedBy(vSpacing.space2)) {
					SecondaryButton(stringResource(R.string.action_play_again), inOnRestart, Modifier.weight(1f), inIcon = R.drawable.ic_ln_replay)
					IconButtonOutlined(R.drawable.ic_ln_flip, stringResource(R.string.action_flip), inOnFlip)
				}
			} else {
				Column(Modifier.weight(1f).fillMaxWidth().padding(horizontal = vSpacing.space4, vertical = vSpacing.space3), verticalArrangement = Arrangement.spacedBy(vSpacing.space3)) {
					Notation(inState.notation)
					FeedbackCard(feedbackOf(inState))
					if (vDaily == null) {
						SessionLine(inState)
					}
					Spacer(Modifier.weight(1f))
					SolutionNavigator(inState, inOnSolutionStep, inOnSolutionJump)
					Actions(inState, inOnBack, inOnHint, inOnSolution, inOnFlip, inOnSkip, inOnConfirm, inOnCancel, inOnPlayAgain)
				}
			}
		}
		inState.promotion?.let { PromotionDialog(it.color, inOnPromotion, inOnPromotionDismiss) }
		when (inExitStep) {
			ExitStep.Confirm -> ConfirmExitDialog(inState.keepsPuzzle, { inOnExitStep(ExitStep.None) }) { inOnExitStep(ExitStep.Recap) }
			ExitStep.Recap -> ArcadeRecapDialog(inState, inClock) {
				inOnExitStep(ExitStep.None)
				inOnLeave()
			}
			ExitStep.None -> Unit
		}
		val vResult = inState.challenge?.result
		val vKind = inState.challenge?.kind
		if (vKind != null && vResult != null) {
			// Récapitulatif à la fin d'une Survie ou d'un Rush, une fois par partie ; fermé, l'écran garde Accueil / Rejouer.
			var vRecapOpen by remember(vResult) { mutableStateOf(true) }
			if (vRecapOpen) {
				ChallengeRecapDialog(vKind, vResult, { vRecapOpen = false }, inOnPlayAgain)
			}
		}
	}
}

/**
 * Confirmation avant de quitter une session d'Arcade ou d'Entraînement : « Continuer » ou « Terminer ».
 * [inKeepsPuzzle] précise si le puzzle en cours est gardé (Arcade) ou abandonné (Entraînement).
 */
@Composable
private fun ConfirmExitDialog(inKeepsPuzzle: Boolean, inOnCancel: () -> Unit, inOnConfirm: () -> Unit) {
	val vColors = ChessPlusTheme.colors
	Dialog(onDismissRequest = inOnCancel, properties = DialogProperties(usePlatformDefaultWidth = false)) {
		SurfaceCard(Modifier.padding(horizontal = ChessPlusTheme.spacing.space6)) {
			Text(stringResource(R.string.exit_title), style = ChessPlusTheme.typography.heading, color = vColors.ink)
			Text(stringResource(if (inKeepsPuzzle) R.string.exit_detail else R.string.exit_detail_training), style = ChessPlusTheme.typography.small, color = vColors.inkMuted)
			Spacer(Modifier.height(ChessPlusTheme.spacing.space4))
			Row(horizontalArrangement = Arrangement.spacedBy(ChessPlusTheme.spacing.space2)) {
				SecondaryButton(stringResource(R.string.exit_continue), inOnCancel, Modifier.weight(1f))
				PrimaryButton(stringResource(R.string.exit_confirm), inOnConfirm, Modifier.weight(1f))
			}
		}
	}
}

/**
 * Récapitulatif d'une session d'Arcade ou d'Entraînement : classement et sa variation, durée de la session, puzzles
 * joués, réussis et ratés, taux de réussite, temps moyen ; « Accueil » quitte l'écran.
 */
@Composable
private fun ArcadeRecapDialog(inState: GameUiState, inClock: () -> Long, inOnLeave: () -> Unit) {
	val vColors = ChessPlusTheme.colors
	val vTally = inState.tally
	// Durée figée à l'ouverture du récapitulatif (au moment où le joueur a choisi « Terminer »).
	val vDurationMs = remember(inState.sessionStartedAt) { inState.sessionStartedAt?.let { inClock() - it } }
	val vDelta = vTally.ratingDelta ?: 0
	Dialog(onDismissRequest = inOnLeave, properties = DialogProperties(usePlatformDefaultWidth = false)) {
		SurfaceCard(Modifier.padding(horizontal = ChessPlusTheme.spacing.space6)) {
			Text(stringResource(R.string.session_title), style = ChessPlusTheme.typography.heading, color = vColors.ink)
			Text(
				buildAnnotatedString {
					append(stringResource(R.string.session_rating))
					append(" ")
					withStyle(SpanStyle(fontFamily = kMonoFamily, color = vColors.ink)) { append(formatRating(inState.rating)) }
					append(" ")
					withStyle(SpanStyle(fontFamily = kMonoFamily, color = if (vDelta >= 0) vColors.success else vColors.danger)) { append(formatDelta(vDelta)) }
				},
				style = ChessPlusTheme.typography.small,
				color = vColors.inkMuted,
			)
			Spacer(Modifier.height(ChessPlusTheme.spacing.space4))
			StatTiles(
				listOf(
					stringResource(R.string.session_duration) to (vDurationMs?.let { sessionDuration(it) } ?: "–"),
					stringResource(R.string.session_played) to formatRating(vTally.played),
					stringResource(R.string.recap_solved) to formatRating(vTally.solved),
					stringResource(R.string.recap_failed) to formatRating(vTally.failed),
					stringResource(R.string.session_success) to (if (vTally.played > 0) percent(vTally.solved.toFloat() / vTally.played) else "–"),
					stringResource(R.string.recap_average) to (vTally.averageDurationMs?.let { duration(it) } ?: "–"),
				)
			)
			Spacer(Modifier.height(ChessPlusTheme.spacing.space4))
			PrimaryButton(stringResource(R.string.action_home), inOnLeave, Modifier.fillMaxWidth(), inIcon = R.drawable.ic_ln_home)
		}
	}
}

/**
 * Formate la durée d'une session, courte pour tenir dans une tuile : « 45 s », « 12 min », « 1 h 05 ».
 */
@Composable
private fun sessionDuration(inMs: Long): String {
	val vSeconds = (inMs / 1000).coerceAtLeast(0)
	return when {
		vSeconds < 60 -> stringResource(R.string.session_duration_seconds, vSeconds.toInt())
		vSeconds < 3600 -> stringResource(R.string.session_duration_minutes, (vSeconds / 60).toInt())
		else -> stringResource(R.string.session_duration_hours, (vSeconds / 3600).toInt(), ((vSeconds % 3600) / 60).toInt())
	}
}

/**
 * Récapitulatif d'une partie de Survie ou de Rush : puzzles réussis et ratés, temps moyen, record absolu.
 */
@Composable
private fun ChallengeRecapDialog(inKind: ChallengeKind, inResult: ChallengeResult, inOnClose: () -> Unit, inOnPlayAgain: () -> Unit) {
	val vColors = ChessPlusTheme.colors
	Dialog(onDismissRequest = inOnClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
		SurfaceCard(Modifier.padding(horizontal = ChessPlusTheme.spacing.space6)) {
			Text(
				stringResource(if (inKind == ChallengeKind.Survival) R.string.recap_title else R.string.recap_title_rush),
				style = ChessPlusTheme.typography.heading,
				color = vColors.ink,
			)
			Text(
				stringResource(
					when {
						inResult.newRecord -> R.string.recap_new_record
						inResult.end == ChallengeEnd.NoPuzzle -> R.string.recap_no_puzzle
						inKind == ChallengeKind.Rush -> R.string.recap_subtitle_rush
						else -> R.string.recap_subtitle
					}
				),
				style = ChessPlusTheme.typography.small,
				color = if (inResult.newRecord) vColors.success else vColors.inkMuted,
			)
			Spacer(Modifier.height(ChessPlusTheme.spacing.space4))
			StatTiles(
				listOf(
					stringResource(R.string.recap_solved) to formatRating(inResult.score),
					stringResource(R.string.recap_failed) to formatRating(inResult.failed),
					stringResource(R.string.recap_average) to (inResult.averageDurationMs?.let { duration(it) } ?: "–"),
					stringResource(R.string.recap_best) to formatRating(inResult.best),
				)
			)
			Spacer(Modifier.height(ChessPlusTheme.spacing.space4))
			Row(horizontalArrangement = Arrangement.spacedBy(ChessPlusTheme.spacing.space2)) {
				SecondaryButton(stringResource(R.string.recap_close), inOnClose, Modifier.weight(1f))
				PrimaryButton(stringResource(R.string.action_play_again), {
					inOnClose()
					inOnPlayAgain()
				}, Modifier.weight(1f), inIcon = R.drawable.ic_ln_replay)
			}
		}
	}
}

/**
 * Chip de la barre du haut : vies (Survie), compte à rebours (Rush) ou chronomètre du puzzle.
 */
@Composable
private fun TopChip(inState: GameUiState, inClock: () -> Long) {
	val vColors = ChessPlusTheme.colors
	val vChallenge = inState.challenge
	if (vChallenge?.kind == ChallengeKind.Survival) {
		Chip("${vChallenge.lives}", inIcon = R.drawable.ic_ln_heart, inIconTint = vColors.danger)
		return
	}
	val vRunning = if (vChallenge != null) vChallenge.endsAt != null && vChallenge.result == null else inState.clockStart != null && inState.clockEnd == null
	var vNow by remember { mutableLongStateOf(inClock()) }
	LaunchedEffect(vRunning, inState.clockStart, vChallenge?.endsAt) {
		vNow = inClock()
		while (vRunning) {
			delay(kTimerTickMs)
			vNow = inClock()
		}
	}
	if (vChallenge != null) {
		val vRemaining = when {
			vChallenge.result?.end == ChallengeEnd.TimeUp -> 0L
			vChallenge.endsAt == null -> ChallengeRun.kRushDurationMs
			else -> (vChallenge.endsAt - vNow).coerceAtLeast(0L)
		}
		val vColor = if (vRemaining < kTimerWarningMs) vColors.danger else vColors.ink
		Chip(formatClock(vRemaining + 999), inIcon = R.drawable.ic_ln_clock, inIconTint = vColor, inTextColor = vColor)
	} else {
		val vStart = inState.clockStart ?: vNow
		Chip(formatClock((inState.clockEnd ?: vNow) - vStart), inIcon = R.drawable.ic_ln_clock)
	}
}

/**
 * En-tête du puzzle : numéro et classement, camp au trait, thème principal.
 */
@Composable
private fun PuzzleHeader(inState: GameUiState) {
	val vColors = ChessPlusTheme.colors
	val vSpacing = ChessPlusTheme.spacing
	Row(Modifier.fillMaxWidth().padding(start = vSpacing.space4, end = vSpacing.space4, bottom = vSpacing.space3), verticalAlignment = Alignment.Bottom) {
		Column(Modifier.weight(1f)) {
			val vId = inState.puzzleId
			val vRating = inState.puzzleRating
			Text(
				buildAnnotatedString {
					if (vId != null) {
						append(stringResource(R.string.game_puzzle_number, vId).uppercase(Locale.FRANCE))
						if (vRating != null) {
							append(" · ")
							withStyle(SpanStyle(fontFamily = kMonoFamily)) { append(formatRating(vRating)) }
						}
					}
				},
				style = ChessPlusTheme.typography.overline,
				color = vColors.inkMuted,
				maxLines = 1,
			)
			Row(verticalAlignment = Alignment.CenterVertically) {
				val vWhite = inState.sideToMove == Color.White
				Box(Modifier.size(kSideSquare.dp).background(if (vWhite) vColors.pieceWhite else vColors.pieceBlack).border(1.5.dp, vColors.pieceBlack))
				Spacer(Modifier.width(vSpacing.space2))
				Text(
					stringResource(if (vWhite) R.string.game_white_to_move else R.string.game_black_to_move),
					style = ChessPlusTheme.typography.heading,
					color = vColors.ink,
					maxLines = 1,
				)
			}
		}
		// Le thème n'est montré qu'en Entraînement, où il a été choisi : ailleurs, ce serait un indice.
		if (inState.showThemeTag) {
			mainTheme(inState.puzzleThemes)?.let { Tag(stringResource(themeLabel(it))) }
		}
	}
}

/**
 * Notation des coups en mono : coups joués atténués, numéro du coup à trouver, « ? » souligné d'`accent`.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Notation(inTokens: List<NotationToken>) {
	val vColors = ChessPlusTheme.colors
	FlowRow(horizontalArrangement = Arrangement.spacedBy(ChessPlusTheme.spacing.space3)) {
		// En revue de la solution, toute la ligne ; sinon, les derniers coups seulement.
		val vReviewing = inTokens.any { it.kind == NotationKind.Selected || it.kind == NotationKind.Upcoming }
		for (vToken in if (vReviewing) inTokens else inTokens.takeLast(kMaxNotationTokens)) {
			Text(
				vToken.text,
				style = ChessPlusTheme.typography.moves,
				color = when (vToken.kind) {
					NotationKind.Past -> vColors.inkMuted
					NotationKind.Upcoming -> vColors.inkMuted.copy(alpha = 0.5f)
					else -> vColors.ink
				},
				modifier = if (vToken.kind == NotationKind.Pending || vToken.kind == NotationKind.Selected) Modifier.drawBehind {
					val vStroke = 2.dp.toPx()
					drawLine(vColors.accent, Offset(0f, size.height - vStroke / 2), Offset(size.width, size.height - vStroke / 2), vStroke)
				} else Modifier,
			)
		}
	}
}

/**
 * Revue de la solution d'un puzzle terminé : début, coup précédent, coup suivant, fin, et numéro du
 * coup affiché. Absente tant que la revue n'est pas disponible.
 */
@Composable
private fun SolutionNavigator(inState: GameUiState, inOnStep: (Int) -> Unit, inOnJump: (Boolean) -> Unit) {
	val vLength = inState.solutionLength
	if (vLength == 0) {
		return
	}
	val vColors = ChessPlusTheme.colors
	val vPly = inState.solutionPly ?: vLength
	Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(ChessPlusTheme.spacing.space2)) {
		Column(Modifier.weight(1f)) {
			Text(stringResource(R.string.solution_title), style = ChessPlusTheme.typography.small, color = vColors.inkMuted, maxLines = 1)
			Text(stringResource(R.string.solution_ply, vPly, vLength), style = ChessPlusTheme.typography.moves, color = vColors.ink, maxLines = 1)
		}
		IconButtonOutlined(R.drawable.ic_ln_first, stringResource(R.string.solution_first), { inOnJump(false) }, inEnabled = vPly > 0)
		IconButtonOutlined(R.drawable.ic_ln_back, stringResource(R.string.solution_previous), { inOnStep(-1) }, inEnabled = vPly > 0)
		IconButtonOutlined(R.drawable.ic_ln_chevron, stringResource(R.string.solution_next), { inOnStep(1) }, inEnabled = vPly < vLength)
		IconButtonOutlined(R.drawable.ic_ln_last, stringResource(R.string.solution_last), { inOnJump(true) }, inEnabled = vPly < vLength)
	}
}

/**
 * Carte de retour : bordure `success`, `danger` ou `line`, jamais de pop-up.
 */
@Composable
private fun FeedbackCard(inFeedback: Feedback) {
	val vColors = ChessPlusTheme.colors
	val vBorder = when (inFeedback.tone) {
		Tone.Neutral -> vColors.line
		Tone.Success -> vColors.success
		Tone.Danger -> vColors.danger
	}
	val vShape = RoundedCornerShape(ChessPlusTheme.radius.radiusMd)
	Row(
		Modifier.fillMaxWidth().clip(vShape).background(vColors.surfaceRaised).border(1.dp, vBorder, vShape).padding(horizontal = 12.dp, vertical = 10.dp),
		verticalAlignment = Alignment.CenterVertically,
	) {
		inFeedback.title?.let {
			Text(
				it,
				style = ChessPlusTheme.typography.button.copy(lineHeight = ChessPlusTheme.typography.moves.lineHeight),
				color = when (inFeedback.tone) {
					Tone.Neutral -> vColors.ink
					Tone.Success -> vColors.success
					Tone.Danger -> vColors.danger
				},
				maxLines = 1,
			)
			Spacer(Modifier.width(10.dp))
		}
		Text(inFeedback.detail, style = ChessPlusTheme.typography.small, color = vColors.inkMuted, maxLines = 2, overflow = TextOverflow.Ellipsis)
	}
}

/**
 * Retourne le retour à afficher pour l'état de l'écran.
 */
@Composable
private fun feedbackOf(inState: GameUiState): Feedback {
	val vChallenge = inState.challenge
	val vSide = stringResource(if (inState.sideToMove == Color.White) R.string.game_side_white else R.string.game_side_black)
	return when (val vHeader = inState.header) {
		GameHeader.Loading -> Feedback(null, stringResource(R.string.game_loading), Tone.Neutral)
		is GameHeader.ToMove -> if (vHeader.afterCorrect) {
			Feedback(stringResource(R.string.game_good_move), pluralStringResource(R.plurals.game_moves_left, inState.remainingMoves, inState.remainingMoves), Tone.Success)
		} else {
			Feedback(null, stringResource(R.string.game_find_best, vSide), Tone.Neutral)
		}
		is GameHeader.Correct -> Feedback(
			stringResource(R.string.game_good_move),
			stringResource(if (vHeader.opponent == Color.White) R.string.game_white_thinking else R.string.game_black_thinking),
			Tone.Success,
		)
		GameHeader.Wrong -> Feedback(
			stringResource(R.string.game_wrong),
			stringResource(
				when (vChallenge?.kind) {
					ChallengeKind.Survival -> R.string.challenge_life_lost
					ChallengeKind.Rush -> R.string.challenge_next
					null -> R.string.game_wrong_detail
				}
			),
			Tone.Danger,
		)
		is GameHeader.HintPiece -> Feedback(stringResource(R.string.game_hint), stringResource(pieceHint(vHeader.piece)), Tone.Neutral)
		is GameHeader.Hint -> Feedback(
			stringResource(R.string.game_solution),
			stringResource(R.string.game_solution_detail, vHeader.move.from.toAlgebraic(), vHeader.move.to.toAlgebraic()),
			Tone.Neutral,
		)
		is GameHeader.Finished -> {
			val vSummary = inState.summary
			val vDetail = when {
				vSummary == null -> stringResource(R.string.game_saving)
				!vSummary.rated -> stringResource(R.string.game_unrated)
				else -> stringResource(
					R.string.game_rating_after,
					formatRating(vSummary.ratingAfter.roundToInt()),
					formatDelta(vSummary.ratingAfter.roundToInt() - vSummary.ratingBefore.roundToInt()),
				)
			}
			val vSuccess = vSummary?.result?.let { it == PuzzleResult.Success } ?: vHeader.success
			Feedback(stringResource(if (vSuccess) R.string.game_solved else R.string.game_failed), vDetail, if (vSuccess) Tone.Success else Tone.Danger)
		}
		GameHeader.NoPuzzle -> if (inState.isReview) {
			Feedback(stringResource(R.string.review_done), stringResource(R.string.review_done_detail), Tone.Success)
		} else {
			Feedback(stringResource(R.string.game_no_puzzle), stringResource(R.string.game_no_puzzle_detail), Tone.Neutral)
		}
		GameHeader.Solved -> Feedback(stringResource(R.string.game_good_move), stringResource(R.string.challenge_next), Tone.Success)
		is GameHeader.Missed -> Feedback(
			stringResource(R.string.game_wrong),
			stringResource(R.string.challenge_missed_detail, vHeader.move.from.toAlgebraic(), vHeader.move.to.toAlgebraic()),
			Tone.Danger,
		)
		is GameHeader.ChallengeOver -> {
			val vResult = vChallenge?.result
			val vTitle = stringResource(
				when (vHeader.end) {
					ChallengeEnd.NoLives -> R.string.challenge_over_lives
					ChallengeEnd.TimeUp -> R.string.challenge_over_time
					ChallengeEnd.Abandoned -> R.string.challenge_over_abandoned
					ChallengeEnd.NoPuzzle -> R.string.challenge_over_no_puzzle
				}
			)
			val vDetail = if (vResult == null) "" else {
				val vScore = pluralStringResource(R.plurals.challenge_score, vResult.score, vResult.score)
				if (vResult.newRecord) stringResource(R.string.challenge_new_record, vScore) else stringResource(R.string.challenge_record, vScore, vResult.best)
			}
			Feedback(vTitle, vDetail, if (vResult?.newRecord == true) Tone.Success else Tone.Neutral)
		}
	}
}

/**
 * Retourne la phrase d'indice pour la pièce à jouer.
 */
private fun pieceHint(inPiece: PieceType): Int = when (inPiece) {
	PieceType.King -> R.string.game_hint_king
	PieceType.Queen -> R.string.game_hint_queen
	PieceType.Rook -> R.string.game_hint_rook
	PieceType.Bishop -> R.string.game_hint_bishop
	PieceType.Knight -> R.string.game_hint_knight
	PieceType.Pawn -> R.string.game_hint_pawn
}

/**
 * Bilan de la session : réussis en `success`, ratés en `danger`, variation du classement (ou record en défi).
 */
@Composable
private fun SessionLine(inState: GameUiState) {
	val vColors = ChessPlusTheme.colors
	val vMono = ChessPlusTheme.typography.moves.copy(fontSize = ChessPlusTheme.typography.small.fontSize, lineHeight = ChessPlusTheme.typography.small.lineHeight)
	val vTally = inState.tally
	Row(horizontalArrangement = Arrangement.spacedBy(ChessPlusTheme.spacing.space4), verticalAlignment = Alignment.CenterVertically) {
		Text(stringResource(if (inState.challenge != null) R.string.game_tally_run else R.string.game_tally_session), style = ChessPlusTheme.typography.small, color = vColors.inkMuted)
		Text(pluralStringResource(R.plurals.game_tally_solved, vTally.solved, vTally.solved), style = vMono, color = vColors.success)
		Text(pluralStringResource(R.plurals.game_tally_failed, vTally.failed, vTally.failed), style = vMono, color = vColors.danger)
		Spacer(Modifier.weight(1f))
		val vRight = when {
			inState.challenge != null -> inState.challengeBest?.let { stringResource(R.string.game_tally_record, it) }
			vTally.ratingDelta != null && (vTally.solved + vTally.failed) > 0 -> formatDelta(vTally.ratingDelta)
			else -> null
		}
		vRight?.let { Text(it, style = vMono, color = vColors.ink) }
	}
}

/**
 * Actions à portée de pouce : Indice, Solution, Retourner, Passer ; Confirmer / Annuler pour un coup en
 * attente ; Puzzle suivant une fois le puzzle terminé ; Accueil / Rejouer à la fin d'un défi.
 */
@Composable
private fun Actions(
	inState: GameUiState,
	inOnHome: () -> Unit,
	inOnHint: () -> Unit,
	inOnSolution: () -> Unit,
	inOnFlip: () -> Unit,
	inOnSkip: () -> Unit,
	inOnConfirm: () -> Unit,
	inOnCancel: () -> Unit,
	inOnPlayAgain: () -> Unit,
) {
	val vSpacing = ChessPlusTheme.spacing
	Row(Modifier.fillMaxWidth().padding(bottom = vSpacing.space4), horizontalArrangement = Arrangement.spacedBy(vSpacing.space2)) {
		val vChallenge = inState.challenge
		val vFinished = inState.header is GameHeader.Finished || inState.header == GameHeader.NoPuzzle
		when {
			vChallenge?.result != null -> {
				SecondaryButton(stringResource(R.string.action_home), inOnHome, Modifier.weight(1f), inIcon = R.drawable.ic_ln_home)
				PrimaryButton(stringResource(R.string.action_play_again), inOnPlayAgain, Modifier.weight(1f), inIcon = R.drawable.ic_ln_replay)
			}
			inState.pendingConfirm -> {
				SecondaryButton(stringResource(R.string.action_cancel), inOnCancel, Modifier.weight(1f), inIcon = R.drawable.ic_ln_close)
				PrimaryButton(stringResource(R.string.action_confirm), inOnConfirm, Modifier.weight(1f), inIcon = R.drawable.ic_ln_check)
			}
			vFinished && vChallenge == null -> {
				PrimaryButton(stringResource(R.string.action_next), inOnSkip, Modifier.weight(1f), inIcon = R.drawable.ic_ln_skip, inEnabled = inState.skipEnabled)
				IconButtonOutlined(R.drawable.ic_ln_flip, stringResource(R.string.action_flip), inOnFlip)
			}
			inState.daily != null -> {
				SecondaryButton(stringResource(R.string.action_hint), inOnHint, Modifier.weight(1f), inIcon = R.drawable.ic_ln_bulb, inEnabled = inState.hintEnabled)
				SecondaryButton(stringResource(R.string.action_solution), inOnSolution, Modifier.weight(1f), inIcon = R.drawable.ic_ln_eye, inEnabled = inState.solutionEnabled)
				IconButtonOutlined(R.drawable.ic_ln_flip, stringResource(R.string.action_flip), inOnFlip)
			}
			else -> {
				SecondaryButton(stringResource(R.string.action_hint), inOnHint, Modifier.weight(1f), inIcon = R.drawable.ic_ln_bulb, inEnabled = inState.hintEnabled, inHorizontalPadding = vSpacing.space2)
				SecondaryButton(stringResource(R.string.action_solution), inOnSolution, Modifier.weight(1f), inIcon = R.drawable.ic_ln_eye, inEnabled = inState.solutionEnabled, inHorizontalPadding = vSpacing.space2)
				IconButtonOutlined(R.drawable.ic_ln_flip, stringResource(R.string.action_flip), inOnFlip)
				IconButtonOutlined(R.drawable.ic_ln_skip, stringResource(R.string.action_skip), inOnSkip, inEnabled = inState.skipEnabled)
			}
		}
	}
}

/**
 * Choix de la pièce de promotion, par-dessus l'échiquier.
 */
@Composable
private fun PromotionDialog(inColor: Color, inOnChoose: (PieceType) -> Unit, inOnDismiss: () -> Unit) {
	val vColors = ChessPlusTheme.colors
	val vShape = RoundedCornerShape(ChessPlusTheme.radius.radiusMd)
	Box(
		Modifier.fillMaxSize().background(vColors.ink.copy(alpha = 0.4f)).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = inOnDismiss),
		contentAlignment = Alignment.Center,
	) {
		Column(
			Modifier
				.padding(horizontal = ChessPlusTheme.spacing.space4)
				.fillMaxWidth()
				.clip(vShape)
				.background(vColors.surfaceRaised)
				.border(1.dp, vColors.line, vShape)
				.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
				.padding(ChessPlusTheme.spacing.space4),
			horizontalAlignment = Alignment.CenterHorizontally,
		) {
			Text(stringResource(R.string.promotion_title), style = ChessPlusTheme.typography.heading, color = vColors.ink)
			Text(stringResource(R.string.promotion_detail), style = ChessPlusTheme.typography.small, color = vColors.inkMuted)
			Spacer(Modifier.height(ChessPlusTheme.spacing.space4))
			Row(horizontalArrangement = Arrangement.spacedBy(ChessPlusTheme.spacing.space2)) {
				for (vType in listOf(PieceType.Queen, PieceType.Rook, PieceType.Bishop, PieceType.Knight)) {
					val vPress = rememberPressFeedback()
					Box(
						Modifier.then(vPress.scale).size(64.dp).clip(RoundedCornerShape(ChessPlusTheme.radius.radiusSm)).background(vColors.boardLight).pressClickable(vPress) { inOnChoose(vType) },
						contentAlignment = Alignment.Center,
					) {
						Image(rememberPiecePainter(Piece(inColor, vType)), contentDescription = null, modifier = Modifier.size(58.dp))
					}
				}
			}
		}
	}
}
