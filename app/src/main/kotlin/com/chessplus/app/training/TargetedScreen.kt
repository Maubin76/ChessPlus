package com.chessplus.app.training

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.chessplus.app.AppContainer
import com.chessplus.app.R
import com.chessplus.app.game.Difficulty
import com.chessplus.app.game.TrainingCriteria
import com.chessplus.app.ui.ThemeFamily
import com.chessplus.app.ui.components.Divider
import com.chessplus.app.ui.components.ListGroup
import com.chessplus.app.ui.components.ListRow
import com.chessplus.app.ui.components.Overline
import com.chessplus.app.ui.components.PrimaryButton
import com.chessplus.app.ui.components.Section
import com.chessplus.app.ui.components.SegmentedControl
import com.chessplus.app.ui.components.SelectChip
import com.chessplus.app.ui.components.ToggleRow
import com.chessplus.app.ui.components.TopBar
import com.chessplus.app.ui.components.formatRating
import com.chessplus.app.ui.theme.ChessPlusTheme
import com.chessplus.app.ui.themeFamily
import com.chessplus.app.ui.themeLabel
import com.chessplus.core.model.Theme
import com.chessplus.core.model.ThemeSet
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** Bornes du curseur de tranche précise. */
private val kSliderBounds = 400f..3000f

/** Pas du curseur de tranche précise. */
private const val kSliderStep = 100

/**
 * État de l'écran Entraînement.
 *
 * @property criteria critères choisis.
 * @property rating classement du joueur.
 * @property count nombre de puzzles correspondants, ou null pendant le calcul.
 */
data class TargetedUiState(
	val criteria: TrainingCriteria = TrainingCriteria(),
	val rating: Int = 0,
	val count: Int? = null,
)

/**
 * ViewModel de l'entraînement ciblé : critères et nombre de problèmes correspondants.
 *
 * @property container dépendances de l'application.
 */
class TargetedViewModel(private val container: AppContainer) : ViewModel() {
	/** État publié. */
	private val mutableState = MutableStateFlow(TargetedUiState())

	/** État de l'écran. */
	val state: StateFlow<TargetedUiState> = mutableState.asStateFlow()

	/** Calcul du nombre de problèmes en cours. */
	private var countJob: Job? = null

	init {
		viewModelScope.launch {
			val vRating = container.progressStore.currentRating().rating.roundToInt()
			mutableState.update { it.copy(rating = vRating) }
			recount()
		}
	}

	/**
	 * Ajoute ou retire un thème.
	 */
	fun toggleTheme(inTheme: Theme) = change { inCriteria ->
		val vSelected = inCriteria.themes.toMutableSet()
		if (!vSelected.add(inTheme)) {
			vSelected.remove(inTheme)
		}
		inCriteria.copy(themes = ThemeSet.of(vSelected))
	}

	/**
	 * Retire tous les thèmes (tous les problèmes).
	 */
	fun clearThemes() = change { it.copy(themes = ThemeSet.kEmpty) }

	/**
	 * Sélectionne toute une famille de thèmes, ou la désélectionne si elle l'est déjà entièrement.
	 */
	fun toggleFamily(inFamily: ThemeFamily) = change { it.copy(themes = it.themes.toggledFamily(inFamily)) }

	/**
	 * Choisit la difficulté.
	 */
	fun setDifficulty(inDifficulty: Difficulty) = change { it.copy(difficulty = inDifficulty) }

	/**
	 * Choisit la tranche précise.
	 */
	fun setRange(inRange: IntRange) = change { it.copy(range = inRange) }

	/**
	 * Applique une modification aux critères et recalcule le nombre de problèmes.
	 */
	private fun change(inTransform: (TrainingCriteria) -> TrainingCriteria) {
		mutableState.update { it.copy(criteria = inTransform(it.criteria)) }
		recount()
	}

	/**
	 * Recalcule le nombre de problèmes correspondants (autour de la cote pour « Mon niveau »).
	 */
	private fun recount() {
		countJob?.cancel()
		countJob = viewModelScope.launch {
			mutableState.update { it.copy(count = null) }
			val vState = mutableState.value
			val vRange = vState.criteria.ratingRange(vState.rating) ?: (vState.rating - TrainingCriteria.kOffsetNear)..(vState.rating + TrainingCriteria.kOffsetNear)
			val vThemes = vState.criteria.themes
			val vCount = container.ready().first.puzzleDao().countInRange(vRange.first, vRange.last, vThemes.low, vThemes.high)
			mutableState.update { it.copy(count = vCount) }
		}
	}

	companion object {
		/**
		 * Retourne la fabrique du ViewModel.
		 */
		fun factory(inContainer: AppContainer): ViewModelProvider.Factory = viewModelFactory { initializer { TargetedViewModel(inContainer) } }
	}
}

/**
 * Entraînement : difficulté (relative ou tranche précise) et thèmes par famille.
 */
@Composable
fun TargetedScreen(inViewModel: TargetedViewModel, inOnBack: () -> Unit, inOnStart: (TrainingCriteria) -> Unit) {
	val vState by inViewModel.state.collectAsStateWithLifecycle()
	TargetedContent(vState, inViewModel::setDifficulty, inViewModel::setRange, inViewModel::toggleTheme, inViewModel::toggleFamily, inViewModel::clearThemes, inOnBack, inOnStart)
}

/**
 * Contenu de l'entraînement, sans état propre (prévisualisable).
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TargetedContent(
	inState: TargetedUiState,
	inOnDifficulty: (Difficulty) -> Unit,
	inOnRange: (IntRange) -> Unit,
	inOnTheme: (Theme) -> Unit,
	inOnFamily: (ThemeFamily) -> Unit,
	inOnClearThemes: () -> Unit,
	inOnBack: () -> Unit,
	inOnStart: (TrainingCriteria) -> Unit,
) {
	val vColors = ChessPlusTheme.colors
	val vCriteria = inState.criteria
	val vRelative = listOf(Difficulty.Easier, Difficulty.Level, Difficulty.Harder)
	val vRangeOn = vCriteria.difficulty == Difficulty.Range
	Column(Modifier.fillMaxSize().background(vColors.surface).statusBarsPadding()) {
		TopBar(stringResource(R.string.train_title), inOnBack)
		Column(
			Modifier
				.weight(1f)
				.verticalScroll(rememberScrollState())
				.padding(horizontal = ChessPlusTheme.spacing.space4)
				.padding(top = ChessPlusTheme.spacing.space2, bottom = ChessPlusTheme.spacing.space4),
			verticalArrangement = Arrangement.spacedBy(ChessPlusTheme.spacing.space6),
		) {
			Section(stringResource(R.string.train_section_difficulty)) {
				SegmentedControl(
					listOf(stringResource(R.string.train_easier), stringResource(R.string.train_level), stringResource(R.string.train_harder)),
					vRelative.indexOf(vCriteria.difficulty),
				) { inIndex -> inOnDifficulty(vRelative[inIndex]) }
				val vRows = mutableListOf<@Composable () -> Unit>(
					{
						ToggleRow(
							stringResource(R.string.train_range),
							stringResource(R.string.train_range_value, formatRating(vCriteria.range.first), formatRating(vCriteria.range.last)),
							vRangeOn,
							{ inOn -> inOnDifficulty(if (inOn) Difficulty.Range else Difficulty.Level) },
						)
					},
				)
				if (vRangeOn) {
					vRows.add {
						ListRow {
							RangeSlider(
								value = vCriteria.range.first.toFloat()..vCriteria.range.last.toFloat(),
								onValueChange = { inValue ->
									val vLow = (inValue.start / kSliderStep).roundToInt() * kSliderStep
									val vHigh = (inValue.endInclusive / kSliderStep).roundToInt() * kSliderStep
									if (vHigh > vLow) {
										inOnRange(vLow..vHigh)
									}
								},
								valueRange = kSliderBounds,
								steps = ((kSliderBounds.endInclusive - kSliderBounds.start) / kSliderStep).toInt() - 1,
								colors = SliderDefaults.colors(
									thumbColor = vColors.accent,
									activeTrackColor = vColors.accent,
									inactiveTrackColor = vColors.surfaceSunken,
									activeTickColor = Color.Transparent,
									inactiveTickColor = Color.Transparent,
								),
								modifier = Modifier.weight(1f),
							)
						}
					}
				}
				ListGroup(inRows = vRows)
			}
			Section(stringResource(R.string.train_section_themes)) {
				SelectChip(stringResource(R.string.train_all_themes), vCriteria.themes.isEmpty, inOnClearThemes)
			}
			for (vFamily in ThemeFamily.entries) {
				val vFamilyThemes = familyThemes(vFamily)
				val vAllSelected = vFamilyThemes.all { it in vCriteria.themes }
				Column(verticalArrangement = Arrangement.spacedBy(ChessPlusTheme.spacing.space2)) {
					Row(verticalAlignment = Alignment.CenterVertically) {
						Overline(stringResource(vFamily.label), Modifier.weight(1f))
						Text(
							stringResource(if (vAllSelected) R.string.train_family_none else R.string.train_family_all),
							style = ChessPlusTheme.typography.small.copy(fontWeight = FontWeight(600)),
							color = vColors.ink,
							maxLines = 1,
							modifier = Modifier
								.clip(RoundedCornerShape(ChessPlusTheme.radius.radiusSm))
								.clickable { inOnFamily(vFamily) }
								.padding(horizontal = ChessPlusTheme.spacing.space2, vertical = ChessPlusTheme.spacing.space1),
						)
					}
					FlowRow(horizontalArrangement = Arrangement.spacedBy(ChessPlusTheme.spacing.space2), verticalArrangement = Arrangement.spacedBy(ChessPlusTheme.spacing.space2)) {
						for (vTheme in vFamilyThemes) {
							SelectChip(stringResource(themeLabel(vTheme)), vTheme in vCriteria.themes) { inOnTheme(vTheme) }
						}
					}
				}
			}
		}
		Divider()
		Column(
			Modifier.background(vColors.surface).navigationBarsPadding().padding(ChessPlusTheme.spacing.space4),
			verticalArrangement = Arrangement.spacedBy(ChessPlusTheme.spacing.space3),
		) {
			val vCount = inState.count
			Text(
				if (vCount == null) stringResource(R.string.train_counting) else pluralStringResource(R.plurals.train_count, vCount, formatRating(vCount)),
				style = ChessPlusTheme.typography.small,
				color = vColors.inkMuted,
				modifier = Modifier.align(Alignment.CenterHorizontally),
			)
			PrimaryButton(stringResource(R.string.train_start), { inOnStart(vCriteria) }, Modifier.fillMaxWidth(), inEnabled = vCount != 0)
		}
	}
}

/**
 * Retourne les thèmes d'une famille, dans l'ordre de l'énumération.
 */
fun familyThemes(inFamily: ThemeFamily): List<Theme> = Theme.entries.filter { themeFamily(it) == inFamily }

/**
 * Ajoute tous les thèmes d'une famille, ou les retire tous si la famille est déjà entièrement choisie.
 */
fun ThemeSet.toggledFamily(inFamily: ThemeFamily): ThemeSet {
	val vFamily = familyThemes(inFamily)
	val vSelected = toMutableSet()
	if (vFamily.all { it in vSelected }) {
		vSelected.removeAll(vFamily.toSet())
	} else {
		vSelected.addAll(vFamily)
	}
	return ThemeSet.of(vSelected)
}
