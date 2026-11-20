package com.chessplus.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chessplus.app.game.GameScreen
import com.chessplus.app.game.GameViewModel
import com.chessplus.app.ui.theme.ChessPlusTheme

/**
 * Activité unique de ChessPlus.
 *
 * Phase 5, premier écran : l'application s'ouvre directement sur l'entraînement. L'accueil et la
 * navigation arrivent avec les écrans suivants.
 */
class MainActivity : ComponentActivity() {
	/**
	 * Installe l'écran de jeu.
	 */
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		val vContainer = (application as ChessPlusApplication).container
		setContent {
			ChessPlusTheme {
				val vViewModel: GameViewModel = viewModel(factory = GameViewModel.factory(vContainer))
				GameScreen(vViewModel, inOnBack = { finish() })
			}
		}
	}
}
