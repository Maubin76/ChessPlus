package com.chessplus.app.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.chessplus.app.AppContainer
import kotlinx.coroutines.flow.StateFlow

/**
 * ViewModel de l'écran de jeu : porte un [GameController] dans la portée du ViewModel.
 *
 * L'état survit aux changements de configuration ; après la mort du processus, le contrôleur
 * reprend le problème sauvegardé dans la base utilisateur.
 */
class GameViewModel(inContainer: AppContainer, inMode: GameMode) : ViewModel() {
	/** Logique de l'écran. */
	val controller = GameController(
		scope = viewModelScope,
		mode = inMode,
		rules = inContainer.rules,
		puzzles = inContainer.puzzleProvider,
		progress = inContainer.progressStore,
		sessions = inContainer.sessionStore,
		challenges = inContainer.challengeStore,
		preferences = object : GamePreferences {
			override val confirmMoves: Boolean
				get() = inContainer.settingsStore.settings.value.confirmMoves
			override val autoNext: Boolean
				get() = inContainer.settingsStore.settings.value.autoNext
			override val difficultyOffset: Int
				get() = inContainer.settingsStore.settings.value.difficulty.offset
		},
		clock = System::currentTimeMillis,
	)

	/** État de l'écran. */
	val state: StateFlow<GameUiState> = controller.state

	init {
		controller.start()
	}

	companion object {
		/**
		 * Retourne la fabrique du ViewModel à partir des dépendances de l'application.
		 */
		fun factory(inContainer: AppContainer, inMode: GameMode): ViewModelProvider.Factory = viewModelFactory {
			initializer { GameViewModel(inContainer, inMode) }
		}
	}
}
