package com.chessplus.app.game

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chessplus.app.R
import com.chessplus.app.ui.board.ChessBoard
import com.chessplus.app.ui.board.pieceDrawable
import com.chessplus.app.ui.components.formatDelta
import com.chessplus.app.ui.components.formatRating
import com.chessplus.app.ui.theme.ChessType
import com.chessplus.app.ui.theme.kLocalChessColors
import com.chessplus.app.ui.themeLabel
import com.chessplus.core.model.Color
import com.chessplus.core.model.Piece
import com.chessplus.core.model.PieceType
import com.chessplus.core.puzzle.PuzzleResult
import kotlin.math.roundToInt

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
	GameContent(
		inState = vState,
		inTitle = inTitle,
		inCoordinates = inCoordinates,
		inOnBack = inOnBack,
		inOnTap = vController::onSquareTap,
		inOnDragStart = vController::onDragStart,
		inOnDrop = vController::onDrop,
		inOnRestart = vController::onRestart,
		inOnHint = vController::onHint,
		inOnNext = vController::onNext,
		inOnPromotion = vController::onPromotionChosen,
		inOnPromotionDismiss = vController::onPromotionDismissed,
		inOnSummaryVisible = vController::onSummaryVisible,
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
	inOnSummaryVisible: (Boolean) -> Unit,
) {
	val vColors = kLocalChessColors.current
	Box(Modifier.fillMaxSize().background(vColors.background)) {
		Column(Modifier.fillMaxSize().statusBarsPadding()) {
			TopBar(inTitle, inState.rating, inOnBack)
			Header(inState.header)
			ChessBoard(
				inPieces = inState.pieces,
				inOrientation = inState.orientation,
				inLastMove = inState.lastMove,
				inMoveId = inState.moveId,
				inAnimateMove = inState.animateMove,
				inSelected = inState.selected,
				inTargets = inState.targets,
				inBadge = inState.badge,
				inCoordinates = inCoordinates,
				inInteractive = inState.interactive,
				inOnTap = inOnTap,
				inOnDragStart = inOnDragStart,
				inOnDrop = inOnDrop,
				inModifier = Modifier.fillMaxWidth(),
			)
			Box(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 24.dp), contentAlignment = Alignment.Center) {
				BoardNote(inState, inOnSummaryVisible)
			}
			Controls(inState, inOnRestart, inOnHint, inOnNext)
		}
		inState.promotion?.let { PromotionDialog(it.color, inOnPromotion, inOnPromotionDismiss) }
		if (inState.summaryVisible) {
			inState.summary?.let { SummaryOverlay(it, inOnNext, { inOnSummaryVisible(false) }) }
		}
	}
}

/**
 * Barre du haut : retour, titre, cote du joueur.
 */
@Composable
private fun TopBar(inTitle: String, inRating: Int, inOnBack: () -> Unit) {
	val vColors = kLocalChessColors.current
	Box(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp)) {
		Box(Modifier.align(Alignment.CenterStart).size(48.dp).clip(CircleShape).clickable(onClick = inOnBack), contentAlignment = Alignment.Center) {
			Icon(painterResource(R.drawable.ic_arrow_back_ios_new), contentDescription = stringResource(R.string.action_back), tint = vColors.text, modifier = Modifier.size(20.dp))
		}
		Text(inTitle, style = ChessType.kBarTitle, color = vColors.text, modifier = Modifier.align(Alignment.Center))
		if (inRating > 0) {
			Text(formatRating(inRating), style = ChessType.kRating, color = vColors.text, modifier = Modifier.align(Alignment.CenterEnd).padding(end = 8.dp))
		}
	}
}

/**
 * Message au-dessus de l'échiquier.
 */
@Composable
private fun Header(inHeader: GameHeader) {
	val vColors = kLocalChessColors.current
	val (vTitle, vSubtitle, vTone) = headerTexts(inHeader)
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
 * Retourne le titre, le sous-titre et le ton d'un message.
 */
@Composable
private fun headerTexts(inHeader: GameHeader): Triple<String, String, Tone> = when (inHeader) {
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
	GameHeader.Wrong -> Triple(stringResource(R.string.game_wrong), stringResource(R.string.game_wrong_detail), Tone.Error)
	is GameHeader.Hint -> Triple(
		stringResource(R.string.game_hint),
		stringResource(R.string.game_hint_detail, inHeader.move.from.toAlgebraic(), inHeader.move.to.toAlgebraic()),
		Tone.Neutral,
	)
	is GameHeader.Finished -> Triple(
		stringResource(if (inHeader.mate) R.string.game_mate else R.string.game_finished),
		when {
			inHeader.solutionShown -> stringResource(R.string.game_solution_shown)
			inHeader.mate -> stringResource(R.string.game_finished)
			else -> ""
		},
		if (inHeader.success) Tone.Success else Tone.Neutral,
	)
	GameHeader.NoPuzzle -> Triple(stringResource(R.string.game_no_puzzle), stringResource(R.string.game_no_puzzle_detail), Tone.Neutral)
}

/**
 * Mention discrète sous l'échiquier : aide, rappel après une erreur, ou accès au bilan replié.
 */
@Composable
private fun BoardNote(inState: GameUiState, inOnSummaryVisible: (Boolean) -> Unit) {
	val vColors = kLocalChessColors.current
	when {
		inState.summary != null && !inState.summaryVisible -> Text(
			stringResource(R.string.summary_success).takeIf { inState.summary.result == PuzzleResult.Success } ?: stringResource(R.string.summary_failure),
			style = ChessType.kBodyStrong,
			color = vColors.text,
			modifier = Modifier.clip(RoundedCornerShape(16.dp)).clickable { inOnSummaryVisible(true) }.background(vColors.surface).padding(horizontal = 16.dp, vertical = 10.dp),
		)
		inState.header is GameHeader.Wrong -> Text(stringResource(R.string.game_wrong_note), style = ChessType.kCaption, color = vColors.muted, textAlign = TextAlign.Center)
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
		Modifier.fillMaxWidth().padding(horizontal = 42.dp).navigationBarsPadding().padding(bottom = 28.dp),
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
	Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(84.dp).padding(top = 8.dp)) {
		Box(
			Modifier
				.size(60.dp)
				.clip(RoundedCornerShape(20.dp))
				.background(if (inHighlighted) vColors.primary else vColors.surface)
				.clickable(enabled = inEnabled, onClick = inOnClick),
			contentAlignment = Alignment.Center,
		) {
			Icon(painterResource(inIcon), contentDescription = inLabel, tint = if (inHighlighted) vColors.onPrimary else if (inEnabled) vColors.text else vColors.muted, modifier = Modifier.size(26.dp))
		}
		Spacer(Modifier.height(8.dp))
		Text(inLabel, style = ChessType.kLabel, color = if (inEnabled) vColors.text else vColors.muted)
	}
}

/**
 * Gros bouton rond principal avec son libellé.
 */
@Composable
private fun PrimaryRoundButton(inIcon: Int, inLabel: String, inEnabled: Boolean, inOnClick: () -> Unit) {
	val vColors = kLocalChessColors.current
	Column(horizontalAlignment = Alignment.CenterHorizontally) {
		Box(
			Modifier.size(76.dp).clip(CircleShape).background(if (inEnabled) vColors.primary else vColors.track).clickable(enabled = inEnabled, onClick = inOnClick),
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
					Box(
						Modifier.size(70.dp).clip(RoundedCornerShape(18.dp)).background(vColors.squareLight).clickable { inOnChoose(vType) },
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
 * Volet de bilan en bas de l'écran, sur un voile qui le replie quand on le touche.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SummaryOverlay(inSummary: GameSummary, inOnNext: () -> Unit, inOnHide: () -> Unit) {
	val vColors = kLocalChessColors.current
	val vSuccess = inSummary.result == PuzzleResult.Success
	val vColor = if (vSuccess) vColors.success else vColors.error
	val vUriHandler = LocalUriHandler.current
	Box(Modifier.fillMaxSize()) {
		Box(Modifier.fillMaxSize().background(vColors.scrim.copy(alpha = 0.2f)).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = inOnHide))
		Column(
			Modifier
				.align(Alignment.BottomCenter)
				.fillMaxWidth()
				.clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
				.background(vColors.surface)
				.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
				.navigationBarsPadding()
				.padding(horizontal = 24.dp, vertical = 12.dp),
		) {
			Box(Modifier.align(Alignment.CenterHorizontally).width(40.dp).height(4.dp).background(vColors.track, RoundedCornerShape(2.dp)).clickable(onClick = inOnHide))
			Spacer(Modifier.height(18.dp))
			Row(verticalAlignment = Alignment.CenterVertically) {
				Icon(painterResource(if (vSuccess) R.drawable.ic_check_circle_fill1 else R.drawable.ic_cancel_fill1), contentDescription = null, tint = vColor, modifier = Modifier.size(28.dp))
				Spacer(Modifier.width(8.dp))
				Text(stringResource(if (vSuccess) R.string.summary_success else R.string.summary_failure), style = ChessType.kFeedback, color = vColor)
			}
			Text(summaryDetail(inSummary), style = ChessType.kBody, color = vColors.muted, modifier = Modifier.padding(top = 4.dp))
			if (!inSummary.rated) {
				Text(stringResource(R.string.summary_unrated), style = ChessType.kCaption, color = vColors.muted, modifier = Modifier.padding(top = 2.dp))
			}
			Spacer(Modifier.height(18.dp))
			Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
				Column(Modifier.weight(1f).background(vColors.background, RoundedCornerShape(18.dp)).padding(14.dp)) {
					Text(stringResource(R.string.summary_my_rating), style = ChessType.kCaption, color = vColors.muted)
					Row(verticalAlignment = Alignment.Bottom) {
						Text(formatRating(inSummary.ratingAfter.roundToInt()), style = ChessType.kSection, color = vColors.text)
						if (inSummary.rated) {
							Spacer(Modifier.width(6.dp))
							Text(formatDelta(inSummary.ratingAfter.roundToInt() - inSummary.ratingBefore.roundToInt()), style = ChessType.kBodyStrong, color = vColor)
						}
					}
				}
				Column(Modifier.weight(1f).background(vColors.background, RoundedCornerShape(18.dp)).padding(14.dp)) {
					Text(stringResource(R.string.summary_puzzle_rating), style = ChessType.kCaption, color = vColors.muted)
					Text(formatRating(inSummary.puzzleRating), style = ChessType.kSection, color = vColors.text)
				}
			}
			Spacer(Modifier.height(14.dp))
			Text(stringResource(R.string.summary_themes), style = ChessType.kCaption, color = vColors.muted)
			Spacer(Modifier.height(8.dp))
			FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
				for (vTheme in inSummary.themes) {
					Box(Modifier.clip(RoundedCornerShape(18.dp)).background(vColors.background).padding(horizontal = 14.dp, vertical = 8.dp)) {
						Text(stringResource(themeLabel(vTheme)), style = ChessType.kCaption, color = vColors.text)
					}
				}
			}
			Spacer(Modifier.height(10.dp))
			Row(
				Modifier.clip(RoundedCornerShape(12.dp)).clickable { vUriHandler.openUri(inSummary.lichessUrl) }.padding(vertical = 10.dp, horizontal = 4.dp),
				verticalAlignment = Alignment.CenterVertically,
			) {
				Icon(painterResource(R.drawable.ic_open_in_new), contentDescription = null, tint = vColors.text, modifier = Modifier.size(18.dp))
				Spacer(Modifier.width(8.dp))
				Text(stringResource(R.string.summary_lichess), style = ChessType.kBodyStrong, color = vColors.text)
				Spacer(Modifier.width(6.dp))
				Text(stringResource(R.string.summary_lichess_note), style = ChessType.kCaption, color = vColors.muted)
			}
			Spacer(Modifier.height(10.dp))
			Row(
				Modifier.fillMaxWidth().height(60.dp).clip(RoundedCornerShape(30.dp)).background(vColors.primary).clickable(onClick = inOnNext),
				horizontalArrangement = Arrangement.Center,
				verticalAlignment = Alignment.CenterVertically,
			) {
				Icon(painterResource(R.drawable.ic_skip_next), contentDescription = null, tint = vColors.onPrimary, modifier = Modifier.size(22.dp))
				Spacer(Modifier.width(10.dp))
				Text(stringResource(R.string.action_next), style = ChessType.kBarTitle, color = vColors.onPrimary)
			}
			Spacer(Modifier.height(12.dp))
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
