package com.chessplus.app.game

import com.chessplus.app.ui.components.pressClickable
import com.chessplus.app.ui.components.rememberPressFeedback
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chessplus.app.R
import com.chessplus.app.ui.board.ChessBoard
import com.chessplus.app.ui.board.pieceDrawable
import com.chessplus.app.ui.components.PrimaryButton
import com.chessplus.app.ui.components.SecondaryButton
import com.chessplus.app.ui.components.formatDelta
import com.chessplus.app.ui.components.formatRating
import com.chessplus.app.ui.theme.ChessType
import com.chessplus.app.ui.theme.kLocalChessColors
import com.chessplus.app.ui.themeLabel
import com.chessplus.core.model.Color
import com.chessplus.core.model.Piece
import com.chessplus.core.model.PieceType
import com.chessplus.core.puzzle.PuzzleResult
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/** Intervalle de rafraîchissement du chronomètre du Rush. */
private const val kTimerTickMs = 250L

/** Temps restant sous lequel le chronomètre passe en rouge. */
private const val kTimerWarningMs = 30_000L

/**
 * Ton du message au-dessus de l'échiquier.
 */
private enum class Tone {
	Neutral,
	Success,
	Error,
}

/**
 * Écran de jeu : problème en cours, échiquier pleine largeur, commandes sous le pouce.
 */
@Composable
fun GameScreen(inViewModel: GameViewModel, inTitle: String, inVibration: Boolean, inCoordinates: Boolean, inOnBack: () -> Unit) {
	val vState by inViewModel.state.collectAsStateWithLifecycle()
	val vController = inViewModel.controller
	val vHaptics = LocalHapticFeedback.current
	LaunchedEffect(vState.haptic?.id) {
		if (inVibration) {
			vState.haptic?.let { vHaptics.performHapticFeedback(if (it.success) HapticFeedbackType.Confirm else HapticFeedbackType.Reject) }
		}
	}
	val vBack: () -> Unit = {
		vController.onLeave()
		inOnBack()
	}
	BackHandler(enabled = vState.challenge != null, onBack = vBack)
	GameContent(
		inState = vState,
		inTitle = inTitle,
		inCoordinates = inCoordinates,
		inOnBack = vBack,
		inOnTap = vController::onSquareTap,
		inOnDragStart = vController::onDragStart,
		inOnDrop = vController::onDrop,
		inOnRestart = vController::onRestart,
		inOnHint = vController::onHint,
		inOnNext = vController::onNext,
		inOnPromotion = vController::onPromotionChosen,
		inOnPromotionDismiss = vController::onPromotionDismissed,
		inOnAbandon = vController::onAbandon,
		inOnPlayAgain = vController::onPlayAgain,
	)
}

/**
 * Contenu de l'écran de jeu, sans état propre (prévisualisable).
 */
@Composable
fun GameContent(
	inState: GameUiState,
	inTitle: String,
	inCoordinates: Boolean,
	inOnBack: () -> Unit,
	inOnTap: (com.chessplus.core.model.Square) -> Unit,
	inOnDragStart: (com.chessplus.core.model.Square) -> Unit,
	inOnDrop: (com.chessplus.core.model.Square, com.chessplus.core.model.Square?) -> Unit,
	inOnRestart: () -> Unit,
	inOnHint: () -> Unit,
	inOnNext: () -> Unit,
	inOnPromotion: (PieceType) -> Unit,
	inOnPromotionDismiss: () -> Unit,
	inOnAbandon: () -> Unit = {},
	inOnPlayAgain: () -> Unit = {},
	inClock: () -> Long = System::currentTimeMillis,
) {
	val vColors = kLocalChessColors.current
	val vChallenge = inState.challenge
	Box(Modifier.fillMaxSize().background(vColors.background)) {
		Column(Modifier.fillMaxSize().statusBarsPadding()) {
			TopBar(inTitle, inState.rating, inOnBack, vChallenge, inClock)
			Header(inState.header, inState.summary, vChallenge)
			ChessBoard(
				inPieces = inState.pieces,
				inOrientation = inState.orientation,
				inLastMove = inState.lastMove,
				inMoveId = inState.moveId,
				inAnimateMove = inState.animateMove,
				inSelected = inState.selected,
				inHint = inState.hint,
				inTargets = inState.targets,
				inBadge = inState.badge,
				inCoordinates = inCoordinates,
				inInteractive = inState.interactive,
				inOnTap = inOnTap,
				inOnDragStart = inOnDragStart,
				inOnDrop = inOnDrop,
				inModifier = Modifier.fillMaxWidth(),
			)
			val vSummary = inState.summary
			val vResult = vChallenge?.result
			if (vChallenge != null && vResult != null) {
				ChallengeResultPanel(vResult, Modifier.weight(1f))
				ChallengeOverControls(inOnBack, inOnPlayAgain)
			} else if (vChallenge != null) {
				Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
					ChallengeScore(vChallenge.score)
				}
				Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = 28.dp), horizontalArrangement = Arrangement.Center) {
					ControlTile(R.drawable.ic_flag_fill1, stringResource(R.string.action_abandon), true, false, inOnAbandon)
				}
			} else if (vSummary != null) {
				SummaryPanel(vSummary, Modifier.weight(1f))
				FinishedControls(inOnRestart, inOnNext)
			} else {
				Box(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 24.dp), contentAlignment = Alignment.Center) {
					BoardNote(inState)
				}
				Controls(inState, inOnRestart, inOnHint, inOnNext)
			}
		}
		inState.promotion?.let { PromotionDialog(it.color, inOnPromotion, inOnPromotionDismiss) }
	}
}

/**
 * Barre du haut : retour, titre, et cote du joueur (vies ou chronomètre en défi).
 */
@Composable
private fun TopBar(inTitle: String, inRating: Int, inOnBack: () -> Unit, inChallenge: ChallengeUi?, inClock: () -> Long) {
	val vColors = kLocalChessColors.current
	Box(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp)) {
		val vPress = rememberPressFeedback()
		Box(Modifier.align(Alignment.CenterStart).then(vPress.scale).size(48.dp).clip(CircleShape).pressClickable(vPress, inOnClick = inOnBack), contentAlignment = Alignment.Center) {
			Icon(painterResource(R.drawable.ic_arrow_back_ios_new), contentDescription = stringResource(R.string.action_back), tint = vColors.text, modifier = Modifier.size(20.dp))
		}
		Text(inTitle, style = ChessType.kBarTitle, color = vColors.text, modifier = Modifier.align(Alignment.Center))
		val vEndModifier = Modifier.align(Alignment.CenterEnd).padding(end = 8.dp)
		when {
			inChallenge?.kind == ChallengeKind.Survival -> Lives(inChallenge.lives, vEndModifier)
			inChallenge?.kind == ChallengeKind.Rush -> RushTimer(inChallenge, inClock, vEndModifier)
			inRating > 0 -> Text(formatRating(inRating), style = ChessType.kRating, color = vColors.text, modifier = vEndModifier)
		}
	}
}

/**
 * Vies de la Survie : cœurs pleins pour les vies restantes, grisés pour les vies perdues.
 */
@Composable
private fun Lives(inLives: Int, inModifier: Modifier) {
	val vColors = kLocalChessColors.current
	val vDescription = pluralStringResource(R.plurals.challenge_lives, inLives, inLives)
	Row(inModifier, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
		repeat(ChallengeRun.kLives) { vIndex ->
			Icon(
				painterResource(R.drawable.ic_favorite_fill1),
				contentDescription = if (vIndex == 0) vDescription else null,
				tint = if (vIndex < inLives) vColors.error else vColors.track,
				modifier = Modifier.size(22.dp),
			)
		}
	}
}

/**
 * Chronomètre du Rush (minutes:secondes), rouge dans les 30 dernières secondes.
 */
@Composable
private fun RushTimer(inChallenge: ChallengeUi, inClock: () -> Long, inModifier: Modifier) {
	val vColors = kLocalChessColors.current
	var vNow by remember { mutableLongStateOf(inClock()) }
	val vRunning = inChallenge.endsAt != null && inChallenge.result == null
	LaunchedEffect(inChallenge.endsAt, vRunning) {
		while (vRunning) {
			vNow = inClock()
			delay(kTimerTickMs)
		}
	}
	val vRemaining = when {
		inChallenge.result?.end == ChallengeEnd.TimeUp -> 0L
		inChallenge.endsAt == null -> ChallengeRun.kRushDurationMs
		else -> (inChallenge.endsAt - vNow).coerceAtLeast(0L)
	}
	val vSeconds = ((vRemaining + 999) / 1000).toInt()
	val vColor = if (vRemaining < kTimerWarningMs) vColors.error else vColors.text
	Row(inModifier, verticalAlignment = Alignment.CenterVertically) {
		Icon(painterResource(R.drawable.ic_timer_fill1), contentDescription = null, tint = vColor, modifier = Modifier.size(20.dp))
		Spacer(Modifier.width(4.dp))
		Text("%d:%02d".format(vSeconds / 60, vSeconds % 60), style = ChessType.kRating, color = vColor)
	}
}

/**
 * Score de la partie de défi en cours, sous l'échiquier.
 */
@Composable
private fun ChallengeScore(inScore: Int) {
	val vColors = kLocalChessColors.current
	Column(horizontalAlignment = Alignment.CenterHorizontally) {
		Text("$inScore", style = ChessType.kNumber, color = vColors.text)
		Text(pluralStringResource(R.plurals.challenge_solved, inScore), style = ChessType.kCaption, color = vColors.muted)
	}
}

/**
 * Bilan d'une partie de défi : score, record, problème le plus difficile réussi.
 */
@Composable
private fun ChallengeResultPanel(inResult: ChallengeResult, inModifier: Modifier) {
	val vColors = kLocalChessColors.current
	Column(inModifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp)) {
		Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(vColors.surface).padding(20.dp)) {
			Row(verticalAlignment = Alignment.Bottom) {
				Text("${inResult.score}", style = ChessType.kNumber, color = vColors.text)
				Spacer(Modifier.width(10.dp))
				Text(pluralStringResource(R.plurals.challenge_solved, inResult.score), style = ChessType.kBody, color = vColors.muted, modifier = Modifier.padding(bottom = 10.dp))
			}
			if (inResult.newRecord) {
				Text(stringResource(R.string.challenge_new_record), style = ChessType.kBodyStrong, color = vColors.success)
			} else {
				Text(stringResource(R.string.challenge_record, inResult.best), style = ChessType.kBodyStrong, color = vColors.text)
			}
			inResult.bestPuzzleRating?.let {
				Spacer(Modifier.height(4.dp))
				Text(stringResource(R.string.challenge_best_puzzle, formatRating(it)), style = ChessType.kBody, color = vColors.muted)
			}
		}
	}
}

/**
 * Commandes de fin de partie de défi : Accueil, et Rejouer sous le pouce.
 */
@Composable
private fun ChallengeOverControls(inOnHome: () -> Unit, inOnPlayAgain: () -> Unit) {
	Row(
		Modifier.fillMaxWidth().padding(horizontal = 20.dp).navigationBarsPadding().padding(bottom = 20.dp),
		horizontalArrangement = Arrangement.spacedBy(12.dp),
	) {
		SecondaryButton(stringResource(R.string.action_home), inOnHome, R.drawable.ic_home, Modifier.weight(1f), inHeight = 60)
		PrimaryButton(stringResource(R.string.action_play_again), inOnPlayAgain, Modifier.weight(1.4f), inIcon = R.drawable.ic_refresh)
	}
}

/**
 * Message au-dessus de l'échiquier ; à la fin du problème, résultat du bilan.
 */
@Composable
private fun Header(inHeader: GameHeader, inSummary: GameSummary?, inChallenge: ChallengeUi?) {
	val vColors = kLocalChessColors.current
	val (vTitle, vSubtitle, vTone) = if (inSummary != null) summaryTexts(inSummary) else headerTexts(inHeader, inChallenge?.kind)
	val vColor = when (vTone) {
		Tone.Neutral -> vColors.text
		Tone.Success -> vColors.success
		Tone.Error -> vColors.error
	}
	Column(Modifier.fillMaxWidth().height(96.dp).padding(horizontal = 24.dp), verticalArrangement = Arrangement.Center) {
		Row(verticalAlignment = Alignment.CenterVertically) {
			when (vTone) {
				Tone.Success -> Icon(painterResource(R.drawable.ic_check_circle_fill1), contentDescription = null, tint = vColor, modifier = Modifier.size(30.dp).padding(end = 6.dp))
				Tone.Error -> Icon(painterResource(R.drawable.ic_cancel_fill1), contentDescription = null, tint = vColor, modifier = Modifier.size(30.dp).padding(end = 6.dp))
				Tone.Neutral -> Unit
			}
			Text(vTitle, style = ChessType.kFeedback, color = vColor)
		}
		if (vSubtitle.isNotEmpty()) {
			Spacer(Modifier.height(4.dp))
			Text(vSubtitle, style = ChessType.kBody, color = vColors.muted)
		}
	}
}

/**
 * Retourne le titre, le sous-titre et le ton du message de fin de problème.
 */
@Composable
private fun summaryTexts(inSummary: GameSummary): Triple<String, String, Tone> {
	val vSuccess = inSummary.result == PuzzleResult.Success
	return Triple(
		stringResource(if (vSuccess) R.string.summary_success else R.string.summary_failure),
		summaryDetail(inSummary),
		if (vSuccess) Tone.Success else Tone.Error,
	)
}

/**
 * Retourne le titre, le sous-titre et le ton d'un message.
 */
@Composable
private fun headerTexts(inHeader: GameHeader, inChallenge: ChallengeKind?): Triple<String, String, Tone> = when (inHeader) {
	GameHeader.Loading -> Triple(stringResource(R.string.game_loading), stringResource(R.string.game_loading_detail), Tone.Neutral)
	is GameHeader.ToMove -> Triple(
		stringResource(if (inHeader.color == Color.White) R.string.game_white_to_move else R.string.game_black_to_move),
		stringResource(if (inHeader.afterCorrect) R.string.game_find_next else R.string.game_find_best),
		Tone.Neutral,
	)
	is GameHeader.Correct -> Triple(
		stringResource(R.string.game_correct),
		stringResource(if (inHeader.opponent == Color.White) R.string.game_white_thinking else R.string.game_black_thinking),
		Tone.Success,
	)
	GameHeader.Wrong -> Triple(
		stringResource(R.string.game_wrong),
		stringResource(
			when (inChallenge) {
				ChallengeKind.Survival -> R.string.challenge_life_lost
				ChallengeKind.Rush -> R.string.challenge_next
				null -> R.string.game_wrong_detail
			}
		),
		Tone.Error,
	)
	GameHeader.Solved -> Triple(stringResource(R.string.challenge_solved_title), stringResource(R.string.challenge_next), Tone.Success)
	is GameHeader.Missed -> Triple(
		stringResource(R.string.game_wrong),
		stringResource(R.string.challenge_missed_detail, inHeader.move.from.toAlgebraic(), inHeader.move.to.toAlgebraic()),
		Tone.Error,
	)
	is GameHeader.ChallengeOver -> Triple(
		stringResource(
			when (inHeader.end) {
				ChallengeEnd.NoLives -> R.string.challenge_over_lives
				ChallengeEnd.TimeUp -> R.string.challenge_over_time
				ChallengeEnd.Abandoned -> R.string.challenge_over_abandoned
				ChallengeEnd.NoPuzzle -> R.string.challenge_over_no_puzzle
			}
		),
		stringResource(if (inChallenge == ChallengeKind.Rush) R.string.challenge_over_rush else R.string.challenge_over_survival),
		Tone.Neutral,
	)
	is GameHeader.HintPiece -> Triple(
		stringResource(R.string.game_hint),
		stringResource(
			when (inHeader.piece) {
				PieceType.King -> R.string.game_hint_king
				PieceType.Queen -> R.string.game_hint_queen
				PieceType.Rook -> R.string.game_hint_rook
				PieceType.Bishop -> R.string.game_hint_bishop
				PieceType.Knight -> R.string.game_hint_knight
				PieceType.Pawn -> R.string.game_hint_pawn
			}
		),
		Tone.Neutral,
	)
	is GameHeader.Hint -> Triple(
		stringResource(R.string.game_hint),
		stringResource(R.string.game_hint_detail, inHeader.move.from.toAlgebraic(), inHeader.move.to.toAlgebraic()),
		Tone.Neutral,
	)
	is GameHeader.Finished -> Triple(
		stringResource(if (inHeader.mate) R.string.game_mate else R.string.game_finished),
		when {
			inHeader.hintsUsed -> stringResource(R.string.game_hints_used)
			inHeader.mate -> stringResource(R.string.game_finished)
			else -> ""
		},
		if (inHeader.success) Tone.Success else Tone.Neutral,
	)
	GameHeader.NoPuzzle -> Triple(stringResource(R.string.game_no_puzzle), stringResource(R.string.game_no_puzzle_detail), Tone.Neutral)
}

/**
 * Mention discrète sous l'échiquier : aide, ou rappel après une erreur.
 */
@Composable
private fun BoardNote(inState: GameUiState) {
	val vColors = kLocalChessColors.current
	when {
		inState.header is GameHeader.Wrong -> Text(stringResource(R.string.game_wrong_note), style = ChessType.kCaption, color = vColors.muted, textAlign = TextAlign.Center)
		inState.hint != null -> Text(stringResource(R.string.game_hint_again), style = ChessType.kCaption, color = vColors.muted, textAlign = TextAlign.Center)
		inState.header is GameHeader.ToMove && !inState.header.afterCorrect && inState.interactive ->
			Text(stringResource(R.string.game_touch_help), style = ChessType.kCaption, color = vColors.muted, textAlign = TextAlign.Center)
		else -> Unit
	}
}

/**
 * Commandes sous le pouce : Recommencer, Indice (principal), Suivant.
 */
@Composable
private fun Controls(inState: GameUiState, inOnRestart: () -> Unit, inOnHint: () -> Unit, inOnNext: () -> Unit) {
	Row(
		Modifier.fillMaxWidth().padding(horizontal = 28.dp).navigationBarsPadding().padding(bottom = 28.dp),
		horizontalArrangement = Arrangement.SpaceBetween,
		verticalAlignment = Alignment.Top,
	) {
		ControlTile(R.drawable.ic_refresh, stringResource(R.string.action_restart), inState.pieces.isNotEmpty(), false, inOnRestart)
		PrimaryRoundButton(R.drawable.ic_lightbulb, stringResource(R.string.action_hint), inState.hintEnabled, inOnHint)
		ControlTile(R.drawable.ic_skip_next, stringResource(R.string.action_next), inState.nextEnabled, inState.nextEnabled, inOnNext)
	}
}

/**
 * Tuile de commande secondaire avec son libellé.
 */
@Composable
private fun ControlTile(inIcon: Int, inLabel: String, inEnabled: Boolean, inHighlighted: Boolean, inOnClick: () -> Unit) {
	val vColors = kLocalChessColors.current
	val vPress = rememberPressFeedback()
	Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(100.dp).padding(top = 8.dp)) {
		Box(
			Modifier
				.then(vPress.scale)
				.size(60.dp)
				.clip(RoundedCornerShape(20.dp))
				.background(if (inHighlighted) vColors.primary else vColors.surface)
				.pressClickable(vPress, inEnabled, inOnClick),
			contentAlignment = Alignment.Center,
		) {
			Icon(painterResource(inIcon), contentDescription = inLabel, tint = if (inHighlighted) vColors.onPrimary else if (inEnabled) vColors.text else vColors.muted, modifier = Modifier.size(26.dp))
		}
		Spacer(Modifier.height(8.dp))
		Text(inLabel, style = ChessType.kLabel, color = if (inEnabled) vColors.text else vColors.muted, maxLines = 1, softWrap = false)
	}
}

/**
 * Gros bouton rond principal avec son libellé.
 */
@Composable
private fun PrimaryRoundButton(inIcon: Int, inLabel: String, inEnabled: Boolean, inOnClick: () -> Unit) {
	val vColors = kLocalChessColors.current
	val vPress = rememberPressFeedback()
	Column(horizontalAlignment = Alignment.CenterHorizontally) {
		Box(
			Modifier.then(vPress.scale).size(76.dp).clip(CircleShape).background(if (inEnabled) vColors.primary else vColors.track).pressClickable(vPress, inEnabled, inOnClick),
			contentAlignment = Alignment.Center,
		) {
			Icon(painterResource(inIcon), contentDescription = inLabel, tint = if (inEnabled) vColors.onPrimary else vColors.muted, modifier = Modifier.size(30.dp))
		}
		Spacer(Modifier.height(8.dp))
		Text(inLabel, style = ChessType.kLabel, color = if (inEnabled) vColors.text else vColors.muted)
	}
}

/**
 * Choix de la pièce de promotion, par-dessus l'échiquier.
 */
@Composable
private fun PromotionDialog(inColor: Color, inOnChoose: (PieceType) -> Unit, inOnDismiss: () -> Unit) {
	val vColors = kLocalChessColors.current
	Box(
		Modifier.fillMaxSize().background(vColors.scrim).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = inOnDismiss),
		contentAlignment = Alignment.Center,
	) {
		Column(
			Modifier
				.padding(horizontal = 24.dp)
				.fillMaxWidth()
				.clip(RoundedCornerShape(28.dp))
				.background(vColors.surface)
				.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
				.padding(20.dp),
			horizontalAlignment = Alignment.CenterHorizontally,
		) {
			Text(stringResource(R.string.promotion_title), style = ChessType.kSection, color = vColors.text)
			Spacer(Modifier.height(4.dp))
			Text(stringResource(R.string.promotion_detail), style = ChessType.kBody, color = vColors.muted)
			Spacer(Modifier.height(16.dp))
			Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
				for (vType in listOf(PieceType.Queen, PieceType.Rook, PieceType.Bishop, PieceType.Knight)) {
					val vPress = rememberPressFeedback()
					Box(
						Modifier.then(vPress.scale).size(70.dp).clip(RoundedCornerShape(18.dp)).background(vColors.squareLight).pressClickable(vPress) { inOnChoose(vType) },
						contentAlignment = Alignment.Center,
					) {
						Image(painterResource(pieceDrawable(Piece(inColor, vType))), contentDescription = null, modifier = Modifier.size(60.dp))
					}
				}
			}
		}
	}
}

/**
 * Bilan sous l'échiquier, qui laisse la position finale visible : cotes et thèmes.
 */
@Composable
private fun SummaryPanel(inSummary: GameSummary, inModifier: Modifier) {
	val vColors = kLocalChessColors.current
	val vSuccess = inSummary.result == PuzzleResult.Success
	Column(
		inModifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp),
		verticalArrangement = Arrangement.spacedBy(10.dp),
	) {
		Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
			Column(Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(vColors.surface).padding(horizontal = 14.dp, vertical = 10.dp)) {
				Text(stringResource(R.string.summary_my_rating), style = ChessType.kCaption, color = vColors.muted)
				Row(verticalAlignment = Alignment.Bottom) {
					Text(formatRating(inSummary.ratingAfter.roundToInt()), style = ChessType.kSection, color = vColors.text)
					Spacer(Modifier.width(6.dp))
					if (inSummary.rated) {
						Text(
							formatDelta(inSummary.ratingAfter.roundToInt() - inSummary.ratingBefore.roundToInt()),
							style = ChessType.kBodyStrong,
							color = if (vSuccess) vColors.success else vColors.error,
						)
					} else {
						Text(stringResource(R.string.summary_unrated_short), style = ChessType.kCaption, color = vColors.muted)
					}
				}
			}
			Column(Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(vColors.surface).padding(horizontal = 14.dp, vertical = 10.dp)) {
				Text(stringResource(R.string.summary_puzzle_rating), style = ChessType.kCaption, color = vColors.muted)
				Text(formatRating(inSummary.puzzleRating), style = ChessType.kSection, color = vColors.text)
			}
		}
		Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
			for (vTheme in inSummary.themes) {
				Box(Modifier.clip(RoundedCornerShape(16.dp)).background(vColors.surface).padding(horizontal = 12.dp, vertical = 7.dp)) {
					Text(stringResource(themeLabel(vTheme)), style = ChessType.kCaption, color = vColors.text)
				}
			}
		}
	}
}

/**
 * Commandes de fin de problème : Recommencer, et gros bouton « Suivant » sous le pouce.
 */
@Composable
private fun FinishedControls(inOnRestart: () -> Unit, inOnNext: () -> Unit) {
	val vColors = kLocalChessColors.current
	Row(
		Modifier.fillMaxWidth().padding(horizontal = 20.dp).navigationBarsPadding().padding(bottom = 20.dp),
		verticalAlignment = Alignment.CenterVertically,
	) {
		val vRestartPress = rememberPressFeedback()
		Box(
			Modifier.then(vRestartPress.scale).size(60.dp).clip(RoundedCornerShape(20.dp)).background(vColors.surface).pressClickable(vRestartPress, inOnClick = inOnRestart),
			contentAlignment = Alignment.Center,
		) {
			Icon(painterResource(R.drawable.ic_refresh), contentDescription = stringResource(R.string.action_restart), tint = vColors.text, modifier = Modifier.size(26.dp))
		}
		Spacer(Modifier.width(12.dp))
		val vNextPress = rememberPressFeedback()
		Row(
			Modifier.weight(1f).then(vNextPress.scale).height(60.dp).clip(RoundedCornerShape(30.dp)).background(vColors.primary).pressClickable(vNextPress, inOnClick = inOnNext),
			horizontalArrangement = Arrangement.Center,
			verticalAlignment = Alignment.CenterVertically,
		) {
			Icon(painterResource(R.drawable.ic_skip_next), contentDescription = null, tint = vColors.onPrimary, modifier = Modifier.size(22.dp))
			Spacer(Modifier.width(10.dp))
			Text(stringResource(R.string.action_next), style = ChessType.kBarTitle, color = vColors.onPrimary)
		}
	}
}

/**
 * Retourne la phrase de détail du bilan.
 */
@Composable
private fun summaryDetail(inSummary: GameSummary): String {
	if (inSummary.mistakes == 0 && inSummary.revealed == 0) {
		return stringResource(R.string.summary_clean)
	}
	val vParts = mutableListOf<String>()
	if (inSummary.mistakes > 0) {
		vParts.add(pluralStringResource(R.plurals.summary_mistakes, inSummary.mistakes, inSummary.mistakes))
	}
	if (inSummary.revealed > 0) {
		vParts.add(pluralStringResource(R.plurals.summary_hints, inSummary.revealed, inSummary.revealed))
	}
	return vParts.joinToString(", ")
}
