package com.chessplus.app.level

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.chessplus.app.R
import com.chessplus.app.ui.components.Divider
import com.chessplus.app.ui.components.ListGroup
import com.chessplus.app.ui.components.ListRow
import com.chessplus.app.ui.components.Overline
import com.chessplus.app.ui.components.PageTitle
import com.chessplus.app.ui.components.PrimaryButton
import com.chessplus.app.ui.components.RowTexts
import com.chessplus.app.ui.components.Section
import com.chessplus.app.ui.components.TopBar
import com.chessplus.app.ui.components.formatRating
import com.chessplus.app.ui.components.kIconSize
import com.chessplus.app.ui.theme.ChessPlusTheme

/** Classement minimal accepté pour « Je connais mon classement ». */
private const val kMinRating = 400

/** Classement maximal accepté pour « Je connais mon classement ». */
private const val kMaxRating = 3000

/** Nombre maximal de chiffres saisis. */
private const val kMaxDigits = 4

/** Largeur du champ de saisie du classement. */
private const val kFieldWidth = 88

/** Hauteur du champ de saisie du classement. */
private const val kFieldHeight = 36

/** Niveaux proposés : titre, description, classement de départ. */
private val kLevels = listOf(
	Triple(R.string.lvl_beginner, R.string.lvl_beginner_detail, 800),
	Triple(R.string.lvl_intermediate, R.string.lvl_intermediate_detail, 1200),
	Triple(R.string.lvl_confirmed, R.string.lvl_confirmed_detail, 1600),
	Triple(R.string.lvl_advanced, R.string.lvl_advanced_detail, 2000),
)

/**
 * Choix du niveau : au premier lancement, ou depuis les réglages. [inOnChoose] reçoit le classement choisi.
 */
@Composable
fun LevelScreen(inFirstLaunch: Boolean, inOnBack: () -> Unit, inOnChoose: (Int) -> Unit) {
	val vColors = ChessPlusTheme.colors
	var vSelected by rememberSaveable { mutableIntStateOf(1) }
	var vCustom by rememberSaveable { mutableStateOf("") }
	var vSaving by remember { mutableStateOf(false) }
	val vCustomRating = vCustom.toIntOrNull()?.takeIf { it in kMinRating..kMaxRating }
	val vUseCustom = vSelected == kLevels.size
	val vRating = if (vUseCustom) vCustomRating else kLevels[vSelected].third
	Column(Modifier.fillMaxSize().background(vColors.surface).statusBarsPadding().imePadding()) {
		if (!inFirstLaunch) {
			TopBar(stringResource(R.string.lvl_back_title), inOnBack)
		}
		Column(
			Modifier
				.weight(1f)
				.verticalScroll(rememberScrollState())
				.padding(horizontal = ChessPlusTheme.spacing.space4)
				.padding(top = if (inFirstLaunch) ChessPlusTheme.spacing.space8 else ChessPlusTheme.spacing.space2, bottom = ChessPlusTheme.spacing.space4),
			verticalArrangement = Arrangement.spacedBy(ChessPlusTheme.spacing.space6),
		) {
			Column {
				if (inFirstLaunch) {
					Overline(stringResource(R.string.lvl_welcome))
					PageTitle(stringResource(R.string.lvl_title))
					Spacer(Modifier.height(ChessPlusTheme.spacing.space2))
				}
				Text(stringResource(R.string.lvl_detail), style = ChessPlusTheme.typography.body, color = vColors.inkMuted)
			}
			Section(stringResource(R.string.lvl_section)) {
				ListGroup(
					inRows = kLevels.mapIndexed<Triple<Int, Int, Int>, @Composable () -> Unit> { inIndex, (inTitle, inDetail, inLevelRating) ->
						{ LevelRow(stringResource(inTitle), stringResource(inDetail), inLevelRating, vSelected == inIndex) { vSelected = inIndex } }
					} + listOf<@Composable () -> Unit>(
						{
							CustomRow(vCustom, vUseCustom, { vSelected = kLevels.size }) { inText ->
								vCustom = inText.filter { it.isDigit() }.take(kMaxDigits)
								vSelected = kLevels.size
							}
						},
					),
				)
				if (vUseCustom && vCustom.isNotEmpty() && vCustomRating == null) {
					Text(stringResource(R.string.lvl_custom_invalid, kMinRating, kMaxRating), style = ChessPlusTheme.typography.small, color = vColors.danger)
				}
			}
		}
		Divider()
		PrimaryButton(
			stringResource(if (inFirstLaunch) R.string.lvl_start else R.string.lvl_save),
			{
				val vValue = vRating ?: return@PrimaryButton
				vSaving = true
				inOnChoose(vValue)
			},
			Modifier.fillMaxWidth().navigationBarsPadding().padding(ChessPlusTheme.spacing.space4),
			inEnabled = vRating != null && !vSaving,
		)
	}
}

/**
 * Ligne d'un niveau : titre, description, classement en mono ; le niveau choisi est sur `accentSoft`.
 */
@Composable
private fun LevelRow(inTitle: String, inDescription: String, inRating: Int, inSelected: Boolean, inOnClick: () -> Unit) {
	val vColors = ChessPlusTheme.colors
	ListRow(Modifier.background(if (inSelected) vColors.accentSoft else vColors.surfaceRaised), inOnClick = inOnClick, inMinHeight = 64.dp) {
		RowTexts(inTitle, inDescription, if (inSelected) vColors.accent else vColors.ink)
		Text(formatRating(inRating), style = ChessPlusTheme.typography.moves, color = if (inSelected) vColors.accent else vColors.inkMuted)
		SelectionMark(inSelected)
	}
}

/**
 * Ligne « Je connais mon classement » avec son champ de saisie en mono.
 */
@Composable
private fun CustomRow(inValue: String, inSelected: Boolean, inOnSelect: () -> Unit, inOnValue: (String) -> Unit) {
	val vColors = ChessPlusTheme.colors
	val vShape = RoundedCornerShape(ChessPlusTheme.radius.radiusSm)
	ListRow(Modifier.background(if (inSelected) vColors.accentSoft else vColors.surfaceRaised), inOnClick = inOnSelect, inMinHeight = 64.dp) {
		RowTexts(stringResource(R.string.lvl_custom), stringResource(R.string.lvl_custom_detail, kMinRating, kMaxRating), if (inSelected) vColors.accent else vColors.ink)
		Box(
			Modifier
				.size(kFieldWidth.dp, kFieldHeight.dp)
				.clip(vShape)
				.background(vColors.surfaceSunken)
				.border(1.dp, if (inSelected) vColors.accent else vColors.lineStrong, vShape),
			contentAlignment = Alignment.Center,
		) {
			if (inValue.isEmpty()) {
				Text(stringResource(R.string.lvl_custom_hint), style = ChessPlusTheme.typography.moves, color = vColors.inkMuted)
			}
			BasicTextField(
				value = inValue,
				onValueChange = inOnValue,
				singleLine = true,
				textStyle = ChessPlusTheme.typography.moves.copy(color = vColors.ink, textAlign = TextAlign.Center),
				cursorBrush = SolidColor(vColors.ink),
				keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
				modifier = Modifier.fillMaxWidth().padding(horizontal = ChessPlusTheme.spacing.space2),
			)
		}
		SelectionMark(inSelected)
	}
}

/**
 * Coche `accent` du choix actif, ou espace réservé de même largeur.
 */
@Composable
private fun SelectionMark(inSelected: Boolean) {
	if (inSelected) {
		Icon(painterResource(R.drawable.ic_ln_check), contentDescription = null, tint = ChessPlusTheme.colors.accent, modifier = Modifier.size(kIconSize))
	} else {
		Spacer(Modifier.width(kIconSize))
	}
}
