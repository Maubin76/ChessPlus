package com.chessplus.app.level

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import com.chessplus.app.ui.components.PrimaryButton
import com.chessplus.app.ui.components.ScreenTopBar
import com.chessplus.app.ui.components.formatRating
import com.chessplus.app.ui.theme.ChessType
import com.chessplus.app.ui.theme.kLocalChessColors

/** Cote minimale acceptée pour « Je connais ma cote ». */
private const val kMinRating = 400

/** Cote maximale acceptée pour « Je connais ma cote ». */
private const val kMaxRating = 3000

/** Niveaux proposés : titre, description, cote de départ. */
private val kLevels = listOf(
	Triple(R.string.level_beginner, R.string.level_beginner_detail, 800),
	Triple(R.string.level_intermediate, R.string.level_intermediate_detail, 1200),
	Triple(R.string.level_confirmed, R.string.level_confirmed_detail, 1600),
	Triple(R.string.level_advanced, R.string.level_advanced_detail, 2000),
)

/**
 * Choix du niveau : au premier lancement, ou depuis les réglages. [inOnChoose] reçoit la cote choisie.
 */
@Composable
fun LevelScreen(inFirstLaunch: Boolean, inOnBack: () -> Unit, inOnChoose: (Int) -> Unit) {
	val vColors = kLocalChessColors.current
	var vSelected by rememberSaveable { mutableIntStateOf(1) }
	var vCustom by rememberSaveable { mutableStateOf("") }
	var vSaving by remember { mutableStateOf(false) }
	val vCustomRating = vCustom.toIntOrNull()?.takeIf { it in kMinRating..kMaxRating }
	val vUseCustom = vSelected == kLevels.size
	val vRating = if (vUseCustom) vCustomRating else kLevels[vSelected].third
	Column(Modifier.fillMaxSize().background(vColors.background).statusBarsPadding().navigationBarsPadding().imePadding()) {
		if (inFirstLaunch) {
			Spacer(Modifier.height(40.dp))
		} else {
			ScreenTopBar("", inOnBack)
		}
		Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
			Text(stringResource(R.string.level_welcome), style = ChessType.kCaption, color = vColors.muted)
			Spacer(Modifier.height(6.dp))
			Text(stringResource(R.string.level_title), style = ChessType.kScreenTitle, color = vColors.text)
			Spacer(Modifier.height(8.dp))
			Text(stringResource(R.string.level_detail), style = ChessType.kBody, color = vColors.muted)
			Spacer(Modifier.height(24.dp))
			Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
				kLevels.forEachIndexed { inIndex, (inTitle, inDetail, inLevelRating) ->
					LevelCard(stringResource(inTitle), stringResource(inDetail), formatRating(inLevelRating), vSelected == inIndex) { vSelected = inIndex }
				}
				Row(
					Modifier
						.fillMaxWidth()
						.clip(RoundedCornerShape(22.dp))
						.background(vColors.surface)
						.border(2.dp, if (vUseCustom) vColors.primary else vColors.surface, RoundedCornerShape(22.dp))
						.clickable { vSelected = kLevels.size }
						.padding(horizontal = 18.dp, vertical = 14.dp),
					verticalAlignment = Alignment.CenterVertically,
				) {
					Text(stringResource(R.string.level_custom), style = ChessType.kBodyStrong, color = vColors.text, modifier = Modifier.weight(1f))
					Box(Modifier.width(96.dp).height(40.dp).background(vColors.background, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
						if (vCustom.isEmpty()) {
							Text(stringResource(R.string.level_custom_hint), style = ChessType.kBody, color = vColors.muted)
						}
						BasicTextField(
							value = vCustom,
							onValueChange = { inText ->
								vCustom = inText.filter { it.isDigit() }.take(4)
								vSelected = kLevels.size
							},
							singleLine = true,
							textStyle = ChessType.kBody.copy(color = vColors.text, textAlign = TextAlign.Center),
							cursorBrush = SolidColor(vColors.text),
							keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
							modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
						)
					}
				}
				if (vUseCustom && vCustom.isNotEmpty() && vCustomRating == null) {
					Text(stringResource(R.string.level_custom_invalid, kMinRating, kMaxRating), style = ChessType.kCaption, color = vColors.error)
				}
			}
			Spacer(Modifier.height(16.dp))
		}
		PrimaryButton(
			stringResource(R.string.level_start),
			{
				val vValue = vRating ?: return@PrimaryButton
				vSaving = true
				inOnChoose(vValue)
			},
			Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
			inEnabled = vRating != null && !vSaving,
		)
	}
}

/**
 * Carte de niveau.
 */
@Composable
private fun LevelCard(inTitle: String, inDescription: String, inRating: String, inSelected: Boolean, inOnClick: () -> Unit) {
	val vColors = kLocalChessColors.current
	Row(
		Modifier
			.fillMaxWidth()
			.clip(RoundedCornerShape(22.dp))
			.background(vColors.surface)
			.border(2.dp, if (inSelected) vColors.primary else vColors.surface, RoundedCornerShape(22.dp))
			.clickable(onClick = inOnClick)
			.padding(18.dp),
		verticalAlignment = Alignment.CenterVertically,
	) {
		Column(Modifier.weight(1f)) {
			Text(inTitle, style = ChessType.kSection, color = vColors.text)
			Text(inDescription, style = ChessType.kCaption, color = vColors.muted)
		}
		Text(inRating, style = ChessType.kRating, color = if (inSelected) vColors.text else vColors.muted)
		if (inSelected) {
			Spacer(Modifier.width(10.dp))
			Icon(painterResource(R.drawable.ic_check_circle_fill1), contentDescription = null, tint = vColors.text, modifier = Modifier.size(22.dp))
		}
	}
}
