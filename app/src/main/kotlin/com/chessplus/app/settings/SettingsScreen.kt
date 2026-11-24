package com.chessplus.app.settings

import androidx.compose.foundation.Image
import androidx.compose.ui.window.DialogProperties
import com.chessplus.app.ui.board.PieceStyle
import com.chessplus.app.ui.board.rememberPiecePainter
import com.chessplus.core.model.Piece
import com.chessplus.core.model.PieceType
import com.chessplus.core.model.Color as Side
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chessplus.app.OfflineBaseInfo
import com.chessplus.app.R
import com.chessplus.app.goal.DailyGoal
import com.chessplus.app.ui.components.FloatingNavBar
import com.chessplus.app.ui.components.LinkRow
import com.chessplus.app.ui.components.ListGroup
import com.chessplus.app.ui.components.ListRow
import com.chessplus.app.ui.components.NavTab
import com.chessplus.app.ui.components.PageTitle
import com.chessplus.app.ui.components.PrimaryButton
import com.chessplus.app.ui.components.RowTexts
import com.chessplus.app.ui.components.Section
import com.chessplus.app.ui.components.SecondaryButton
import com.chessplus.app.ui.components.SegmentedControl
import com.chessplus.app.ui.components.SurfaceCard
import com.chessplus.app.ui.components.ToggleRow
import com.chessplus.app.ui.components.formatRating
import com.chessplus.app.ui.components.kIconSize
import com.chessplus.app.ui.theme.BoardTheme
import com.chessplus.app.ui.theme.ChessPlusTheme
import com.chessplus.app.ui.theme.kLocalDarkTheme
import kotlin.math.roundToInt

/** Écart entre les sections de la page (maquette : 20 dp). */
private const val kSectionGap = 20

/** Octets par mégaoctet. */
private const val kBytesPerMegabyte = 1_000_000.0

/** Côté d'une case de l'aperçu des styles de pièces. */
private const val kPieceSampleSize = 32

/** Hauteur du bouton compact « Gérer ». */
private const val kCompactButtonHeight = 32

/** Libellé de chaque thème d'échiquier. */
private val kBoardLabels = mapOf(
	BoardTheme.Sage to R.string.set_board_sage,
	BoardTheme.Wood to R.string.set_board_wood,
	BoardTheme.Slate to R.string.set_board_slate,
	BoardTheme.Grey to R.string.set_board_grey,
)

/**
 * Boîte de dialogue ouverte depuis les réglages.
 */
private enum class SettingsDialog {
	/** Style des pièces. */
	Pieces,

	/** Objectif quotidien. */
	Goal,

	/** Heure du rappel. */
	ReminderTime,

	/** Base hors ligne. */
	Base,
}

/**
 * Onglet Réglages : échiquier, puzzles, apparence, à propos.
 */
@Composable
fun SettingsScreen(
	inStore: SettingsStore,
	inVersion: String,
	inLoadBaseInfo: suspend () -> OfflineBaseInfo,
	inOnLevel: () -> Unit,
	inOnTab: (NavTab) -> Unit,
) {
	val vSettings by inStore.settings.collectAsStateWithLifecycle()
	var vBaseInfo by remember { mutableStateOf<OfflineBaseInfo?>(null) }
	LaunchedEffect(Unit) { vBaseInfo = inLoadBaseInfo() }
	val vContext = LocalContext.current
	// Activer le rappel demande d'abord l'autorisation d'envoyer des notifications ; refusée, le rappel reste désactivé.
	val vPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { inGranted ->
		if (inGranted) {
			inStore.update { it.copy(reminder = true) }
		}
	}
	val vOnReminderChange: (Boolean) -> Unit = { inOn ->
		if (inOn && vContext.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
			vPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
		} else {
			inStore.update { it.copy(reminder = inOn) }
		}
	}
	SettingsContent(vSettings, inStore::update, inVersion, vBaseInfo, inOnLevel, inOnTab, vOnReminderChange)
}

/**
 * Contenu des réglages, sans état métier (prévisualisable). [inBaseInfo] vaut null pendant la lecture de la base.
 */
@Composable
fun SettingsContent(
	inSettings: Settings,
	inOnUpdate: ((Settings) -> Settings) -> Unit,
	inVersion: String,
	inBaseInfo: OfflineBaseInfo?,
	inOnLevel: () -> Unit,
	inOnTab: (NavTab) -> Unit,
	inOnReminderChange: (Boolean) -> Unit = { inOn -> inOnUpdate { it.copy(reminder = inOn) } },
) {
	val vColors = ChessPlusTheme.colors
	var vDialog by rememberSaveable { mutableStateOf<SettingsDialog?>(null) }
	Column(Modifier.fillMaxSize().background(vColors.surface).statusBarsPadding()) {
		Column(
			Modifier
				.weight(1f)
				.verticalScroll(rememberScrollState())
				.padding(horizontal = ChessPlusTheme.spacing.space4)
				.padding(top = ChessPlusTheme.spacing.space2, bottom = ChessPlusTheme.spacing.space4),
			verticalArrangement = Arrangement.spacedBy(kSectionGap.dp),
		) {
			PageTitle(stringResource(R.string.set_title))
			Section(stringResource(R.string.set_section_board)) {
				Row(horizontalArrangement = Arrangement.spacedBy(ChessPlusTheme.spacing.space2)) {
					for (vTheme in BoardTheme.entries) {
						BoardPreview(vTheme, vTheme == inSettings.boardTheme, Modifier.weight(1f)) { inOnUpdate { it.copy(boardTheme = vTheme) } }
					}
				}
				ListGroup(
					inRows = listOf(
						{ LinkRow(stringResource(R.string.set_pieces), pieceStyleLabel(inSettings.pieceStyle), { vDialog = SettingsDialog.Pieces }) },
						{ ToggleRow(stringResource(R.string.set_coordinates), null, inSettings.coordinates, { inOn -> inOnUpdate { it.copy(coordinates = inOn) } }) },
						{
							ToggleRow(
								stringResource(R.string.set_confirm), stringResource(R.string.set_confirm_detail), inSettings.confirmMoves,
								{ inOn -> inOnUpdate { it.copy(confirmMoves = inOn) } },
							)
						},
						{
							ToggleRow(
								stringResource(R.string.set_vibration), stringResource(R.string.set_vibration_detail), inSettings.vibration,
								{ inOn -> inOnUpdate { it.copy(vibration = inOn) } },
							)
						},
					),
				)
			}
			Section(stringResource(R.string.set_section_puzzles)) {
				SegmentedControl(
					listOf(stringResource(R.string.set_difficulty_easier), stringResource(R.string.set_difficulty_normal), stringResource(R.string.set_difficulty_harder)),
					inSettings.difficulty.ordinal,
				) { inIndex -> inOnUpdate { it.copy(difficulty = PuzzleDifficulty.entries[inIndex]) } }
				ListGroup(
					inRows = listOf(
						{
							ToggleRow(
								stringResource(R.string.set_auto_next), stringResource(R.string.set_auto_next_detail), inSettings.autoNext,
								{ inOn -> inOnUpdate { it.copy(autoNext = inOn) } },
							)
						},
						{ BaseRow(inBaseInfo) { vDialog = SettingsDialog.Base } },
						{ LinkRow(stringResource(R.string.set_level), null, inOnLevel, stringResource(R.string.set_level_detail)) },
					),
				)
			}
				Section(stringResource(R.string.set_section_goal)) {
				val vRows = mutableListOf<@Composable () -> Unit>(
					{ LinkRow(stringResource(R.string.set_goal), "${inSettings.dailyGoal}", { vDialog = SettingsDialog.Goal }) },
					{ ToggleRow(stringResource(R.string.set_reminder), stringResource(R.string.set_reminder_detail), inSettings.reminder, inOnReminderChange) },
				)
				if (inSettings.reminder) {
					vRows.add { LinkRow(stringResource(R.string.set_reminder_time), DailyGoal.formatTime(inSettings.reminderMinutes), { vDialog = SettingsDialog.ReminderTime }) }
				}
				ListGroup(inRows = vRows)
			}
			Section(stringResource(R.string.set_section_appearance)) {
				SegmentedControl(
					listOf(stringResource(R.string.set_theme_system), stringResource(R.string.set_theme_light), stringResource(R.string.set_theme_dark)),
					inSettings.themeMode.ordinal,
				) { inIndex -> inOnUpdate { it.copy(themeMode = ThemeMode.entries[inIndex]) } }
			}
			Section(stringResource(R.string.set_section_about)) {
				ListGroup(
					inRows = listOf(
						{
							ListRow {
								RowTexts(stringResource(R.string.set_version), null)
								Text(inVersion, style = ChessPlusTheme.typography.movesSmall, color = vColors.inkMuted)
							}
						},
						{
							ListRow {
								RowTexts(stringResource(R.string.set_credits), stringResource(R.string.set_credits_detail))
							}
						},
					),
				)
			}
		}
		FloatingNavBar(NavTab.Settings, inOnTab)
	}
	when (vDialog) {
		SettingsDialog.Pieces -> PiecesDialog(inSettings.pieceStyle, { inStyle -> inOnUpdate { it.copy(pieceStyle = inStyle) } }) { vDialog = null }
		SettingsDialog.Base -> InfoDialog(stringResource(R.string.set_base_dialog_title), baseDialogText(inBaseInfo), inOnDismiss = { vDialog = null })
		SettingsDialog.Goal -> GoalDialog(inSettings.dailyGoal, { inGoal -> inOnUpdate { it.copy(dailyGoal = inGoal) } }) { vDialog = null }
		SettingsDialog.ReminderTime -> ReminderTimeDialog(inSettings.reminderMinutes, { inMinutes -> inOnUpdate { it.copy(reminderMinutes = inMinutes) } }) { vDialog = null }
		null -> Unit
	}
}

/**
 * Aperçu 2 × 2 d'un thème d'échiquier : le choix actif est entouré d'un contour `accent` de 2 dp
 * décalé de 2 dp, les autres d'un filet `line`.
 */
@Composable
private fun BoardPreview(inTheme: BoardTheme, inActive: Boolean, inModifier: Modifier, inOnClick: () -> Unit) {
	val vColors = ChessPlusTheme.colors
	val vDark = kLocalDarkTheme.current
	val vLight = if (vDark) inTheme.lightNight else inTheme.light
	val vDarkSquare = if (vDark) inTheme.darkNight else inTheme.dark
	val vRadius = ChessPlusTheme.radius.radiusSm
	val vShape = RoundedCornerShape(vRadius)
	Column(inModifier.clickable(onClick = inOnClick), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(ChessPlusTheme.spacing.space1)) {
		Box(
			Modifier
				.fillMaxWidth()
				.aspectRatio(1f)
				.drawBehind {
					if (inActive) {
						val vStroke = 2.dp.toPx()
						val vOutset = 2.dp.toPx() + vStroke / 2
						drawRoundRect(
							vColors.accent,
							Offset(-vOutset, -vOutset),
							Size(size.width + 2 * vOutset, size.height + 2 * vOutset),
							CornerRadius(vRadius.toPx() + vOutset, vRadius.toPx() + vOutset),
							style = Stroke(vStroke),
						)
					}
				}
				.clip(vShape)
				.then(if (inActive) Modifier else Modifier.border(1.dp, vColors.line, vShape)),
		) {
			Column {
				Row(Modifier.weight(1f)) {
					Box(Modifier.weight(1f).fillMaxSize().background(vLight))
					Box(Modifier.weight(1f).fillMaxSize().background(vDarkSquare))
				}
				Row(Modifier.weight(1f)) {
					Box(Modifier.weight(1f).fillMaxSize().background(vDarkSquare))
					Box(Modifier.weight(1f).fillMaxSize().background(vLight))
				}
			}
		}
		Text(
			stringResource(kBoardLabels.getValue(inTheme)),
			style = ChessPlusTheme.typography.small.copy(fontWeight = if (inActive) FontWeight(600) else null),
			color = if (inActive) vColors.ink else vColors.inkMuted,
			maxLines = 1,
		)
	}
}

/**
 * Ligne « Base hors ligne » : nombre de puzzles, taille, état, et bouton compact « Gérer ».
 */
@Composable
private fun BaseRow(inInfo: OfflineBaseInfo?, inOnManage: () -> Unit) {
	ListRow {
		RowTexts(
			stringResource(R.string.set_base),
			if (inInfo == null) stringResource(R.string.set_base_loading) else stringResource(R.string.set_base_detail, formatRating(inInfo.puzzleCount), megabytes(inInfo.sizeBytes)),
		)
		SecondaryButton(
			stringResource(R.string.set_base_manage),
			inOnManage,
			inHeight = kCompactButtonHeight.dp,
			inStyle = ChessPlusTheme.typography.button.copy(fontSize = ChessPlusTheme.typography.small.fontSize),
			inHorizontalPadding = ChessPlusTheme.spacing.space3,
		)
	}
}

/**
 * Formate une taille en mégaoctets (« 48 Mo »).
 */
@Composable
private fun megabytes(inBytes: Long): String = stringResource(R.string.set_base_size, formatRating((inBytes / kBytesPerMegabyte).roundToInt()))

/**
 * Texte de la boîte de dialogue de la base hors ligne.
 */
@Composable
private fun baseDialogText(inInfo: OfflineBaseInfo?): String =
	if (inInfo == null) stringResource(R.string.set_base_loading) else stringResource(R.string.set_base_dialog_detail, formatRating(inInfo.puzzleCount), megabytes(inInfo.sizeBytes))

/**
 * Boîte de dialogue sobre : titre, texte, bouton « Fermer ».
 */
@Composable
private fun InfoDialog(inTitle: String, inText: String, inOnDismiss: () -> Unit, inExtra: (@Composable () -> Unit)? = null) {
	Dialog(onDismissRequest = inOnDismiss) {
		SurfaceCard {
			Text(inTitle, style = ChessPlusTheme.typography.heading, color = ChessPlusTheme.colors.ink)
			Spacer(Modifier.height(ChessPlusTheme.spacing.space2))
			inExtra?.invoke()
			Text(inText, style = ChessPlusTheme.typography.small, color = ChessPlusTheme.colors.inkMuted)
			Spacer(Modifier.height(ChessPlusTheme.spacing.space4))
			SecondaryButton(stringResource(R.string.set_close), inOnDismiss, Modifier.fillMaxWidth())
		}
	}
}

/**
 * Choix de l'objectif quotidien : liste des objectifs proposés, le choix actif en `accentSoft`.
 */
@Composable
private fun GoalDialog(inGoal: Int, inOnChoose: (Int) -> Unit, inOnDismiss: () -> Unit) {
	val vColors = ChessPlusTheme.colors
	Dialog(onDismissRequest = inOnDismiss) {
		SurfaceCard {
			Text(stringResource(R.string.set_goal), style = ChessPlusTheme.typography.heading, color = vColors.ink)
			Text(stringResource(R.string.set_goal_detail), style = ChessPlusTheme.typography.small, color = vColors.inkMuted)
			Spacer(Modifier.height(ChessPlusTheme.spacing.space3))
			ListGroup(
				inRows = DailyGoal.kGoalOptions.map { vOption ->
					{
						val vOn = vOption == inGoal
						ListRow(Modifier.background(if (vOn) vColors.accentSoft else vColors.surfaceRaised), inOnClick = {
							inOnChoose(vOption)
							inOnDismiss()
						}) {
							Text(pluralStringResource(R.plurals.set_goal_option, vOption, vOption), style = ChessPlusTheme.typography.body, color = if (vOn) vColors.accent else vColors.ink, modifier = Modifier.weight(1f))
							if (vOn) {
								Icon(painterResource(R.drawable.ic_ln_check), contentDescription = null, tint = vColors.accent, modifier = Modifier.size(kIconSize))
							}
						}
					}
				},
			)
		}
	}
}

/**
 * Choix de l'heure du rappel : sélecteur d'heure Material sur 24 heures, puis « Annuler » / « Valider ».
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderTimeDialog(inMinutes: Int, inOnChoose: (Int) -> Unit, inOnDismiss: () -> Unit) {
	val vState = rememberTimePickerState(initialHour = inMinutes / 60, initialMinute = inMinutes % 60, is24Hour = true)
	Dialog(onDismissRequest = inOnDismiss) {
		SurfaceCard {
			Text(stringResource(R.string.set_reminder_time), style = ChessPlusTheme.typography.heading, color = ChessPlusTheme.colors.ink)
			Spacer(Modifier.height(ChessPlusTheme.spacing.space4))
			TimePicker(vState, Modifier.align(Alignment.CenterHorizontally))
			Row(horizontalArrangement = Arrangement.spacedBy(ChessPlusTheme.spacing.space2)) {
				SecondaryButton(stringResource(R.string.set_cancel), inOnDismiss, Modifier.weight(1f))
				PrimaryButton(stringResource(R.string.set_validate), {
					inOnChoose(vState.hour * 60 + vState.minute)
					inOnDismiss()
				}, Modifier.weight(1f))
			}
		}
	}
}

/**
 * Retourne le nom d'un style de pièces.
 */
@Composable
private fun pieceStyleLabel(inStyle: PieceStyle): String = stringResource(
	when (inStyle) {
		PieceStyle.Classic -> R.string.set_pieces_classic
		PieceStyle.Geometric -> R.string.set_pieces_geometric
		PieceStyle.Letters -> R.string.set_pieces_letters
	}
)

/**
 * Choix du style des pièces : chaque style avec un aperçu (roi blanc, dame noire, cavalier blanc,
 * fou noir), le choix actif en `accentSoft` ; crédits en dessous.
 */
@Composable
private fun PiecesDialog(inStyle: PieceStyle, inOnChoose: (PieceStyle) -> Unit, inOnDismiss: () -> Unit) {
	val vColors = ChessPlusTheme.colors
	val vSample = listOf(Piece(Side.White, PieceType.King), Piece(Side.Black, PieceType.Queen), Piece(Side.White, PieceType.Knight), Piece(Side.Black, PieceType.Bishop))
	Dialog(onDismissRequest = inOnDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
		SurfaceCard(Modifier.padding(horizontal = ChessPlusTheme.spacing.space6)) {
			Text(stringResource(R.string.set_pieces), style = ChessPlusTheme.typography.heading, color = vColors.ink)
			Spacer(Modifier.height(ChessPlusTheme.spacing.space3))
			ListGroup(
				inRows = PieceStyle.entries.map { vStyle ->
					{
						val vOn = vStyle == inStyle
						ListRow(Modifier.background(if (vOn) vColors.accentSoft else vColors.surfaceRaised), inOnClick = {
							inOnChoose(vStyle)
							inOnDismiss()
						}) {
							Column(Modifier.weight(1f)) {
								Text(pieceStyleLabel(vStyle), style = ChessPlusTheme.typography.body, color = if (vOn) vColors.accent else vColors.ink)
								Spacer(Modifier.height(ChessPlusTheme.spacing.space1))
								Row(Modifier.clip(RoundedCornerShape(ChessPlusTheme.radius.radiusXs))) {
									vSample.forEachIndexed { inIndex, inPiece ->
										Box(Modifier.size(kPieceSampleSize.dp).background(if (inIndex % 2 == 0) vColors.boardLight else vColors.boardDark)) {
											Image(rememberPiecePainter(inPiece, vStyle), contentDescription = null, modifier = Modifier.fillMaxSize())
										}
									}
								}
							}
							if (vOn) {
								Icon(painterResource(R.drawable.ic_ln_check), contentDescription = null, tint = vColors.accent, modifier = Modifier.size(kIconSize))
							}
						}
					}
				},
			)
			Spacer(Modifier.height(ChessPlusTheme.spacing.space3))
			Text(stringResource(R.string.set_pieces_dialog_detail), style = ChessPlusTheme.typography.small, color = vColors.inkMuted)
		}
	}
}
