package com.chessplus.app.training

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
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
import com.chessplus.app.ui.components.PrimaryButton
import com.chessplus.app.ui.components.ScreenTopBar
import com.chessplus.app.ui.components.SegmentedControl
import com.chessplus.app.ui.components.SelectChip
import com.chessplus.app.ui.components.SwitchRow
import com.chessplus.app.ui.components.formatRating
import com.chessplus.app.ui.theme.ChessType
import com.chessplus.app.ui.theme.kLocalChessColors
import com.chessplus.app.ui.themeFamily
import com.chessplus.app.ui.themeLabel
import com.chessplus.core.model.Theme
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
 * État de l'entraînement ciblé.
 *
 * @property criteria critères choisis.
 * @property rating cote du joueur.
 * @property count nombre de problèmes correspondants, ou null pendant le calcul.
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
		inCriteria.copy(themes = com.chessplus.core.model.ThemeSet.of(vSelected))
	}

	/**
	 * Retire tous les thèmes (tous les problèmes).
	 */
	fun clearThemes() = change { it.copy(themes = com.chessplus.core.model.ThemeSet.kEmpty) }

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
 * Entraînement ciblé : difficulté (relative ou tranche précise) et thèmes par famille.
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TargetedScreen(inViewModel: TargetedViewModel, inOnBack: () -> Unit, inOnStart: (TrainingCriteria) -> Unit) {
	val vState by inViewModel.state.collectAsStateWithLifecycle()
	TargetedContent(vState, inViewModel::setDifficulty, inViewModel::setRange, inViewModel::toggleTheme, inViewModel::clearThemes, inOnBack, inOnStart)
}

/**
 * Contenu de l'entraînement ciblé, sans état propre (prévisualisable).
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TargetedContent(
	inState: TargetedUiState,
	inOnDifficulty: (Difficulty) -> Unit,
	inOnRange: (IntRange) -> Unit,
	inOnTheme: (Theme) -> Unit,
	inOnClearThemes: () -> Unit,
	inOnBack: () -> Unit,
	inOnStart: (TrainingCriteria) -> Unit,
) {
	val vState = inState
	val vColors = kLocalChessColors.current
	val vCriteria = vState.criteria
	val vRelative = listOf(Difficulty.Easier, Difficulty.Level, Difficulty.Harder)
	Column(Modifier.fillMaxSize().background(vColors.background).statusBarsPadding()) {
		ScreenTopBar(stringResource(R.string.targeted_title), inOnBack)
		Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
			Text(stringResource(R.string.targeted_difficulty), style = ChessType.kSection, color = vColors.text)
			Spacer(Modifier.height(10.dp))
			SegmentedControl(
				listOf(stringResource(R.string.targeted_easier), stringResource(R.string.targeted_level), stringResource(R.string.targeted_harder)),
				vRelative.indexOf(vCriteria.difficulty),
			) { inIndex -> inOnDifficulty(vRelative[inIndex]) }
			Spacer(Modifier.height(6.dp))
			SwitchRow(
				stringResource(R.string.targeted_range),
				stringResource(R.string.targeted_range_value, formatRating(vCriteria.range.first), formatRating(vCriteria.range.last)),
				vCriteria.difficulty == Difficulty.Range,
				{ inOn -> inOnDifficulty(if (inOn) Difficulty.Range else Difficulty.Level) },
			)
			if (vCriteria.difficulty == Difficulty.Range) {
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
					colors = SliderDefaults.colors(thumbColor = vColors.primary, activeTrackColor = vColors.primary, inactiveTrackColor = vColors.track, activeTickColor = vColors.primary, inactiveTickColor = vColors.track),
				)
			}
			Spacer(Modifier.height(14.dp))
			Text(stringResource(R.string.targeted_themes), style = ChessType.kSection, color = vColors.text)
			Spacer(Modifier.height(10.dp))
			SelectChip(stringResource(R.string.targeted_all_themes), vCriteria.themes.isEmpty, inOnClearThemes)
			for (vFamily in ThemeFamily.entries) {
				Spacer(Modifier.height(14.dp))
				Text(stringResource(vFamily.label).uppercase(), style = ChessType.kCaption, color = vColors.muted)
				Spacer(Modifier.height(8.dp))
				FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
					for (vTheme in Theme.entries.filter { themeFamily(it) == vFamily }) {
						SelectChip(stringResource(themeLabel(vTheme)), vTheme in vCriteria.themes) { inOnTheme(vTheme) }
					}
				}
			}
			Spacer(Modifier.height(16.dp))
		}
		Column(Modifier.background(vColors.background).navigationBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp)) {
			val vCount = vState.count
			Text(
				if (vCount == null) stringResource(R.string.targeted_counting) else pluralStringResource(R.plurals.targeted_count, vCount, formatRating(vCount)),
				style = ChessType.kCaption,
				color = vColors.muted,
				modifier = Modifier.align(Alignment.CenterHorizontally),
			)
			Spacer(Modifier.height(10.dp))
			PrimaryButton(stringResource(R.string.targeted_start), { inOnStart(vCriteria) }, inIcon = R.drawable.ic_bolt_fill1, inEnabled = vCount != 0)
		}
	}
}
