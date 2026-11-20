package com.chessplus.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.chessplus.app.ui.theme.ChessPlusTheme

/**
 * Activité unique de ChessPlus.
 *
 * Version provisoire de la phase 1 : affiche un écran vide. Les écrans réels sont
 * implémentés en phase 5, après validation du design.
 */
class MainActivity : ComponentActivity() {
	/**
	 * Installe le contenu Compose de l'activité.
	 */
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		setContent {
			ChessPlusTheme {
				PlaceholderScreen()
			}
		}
	}
}

/**
 * Écran provisoire affiché tant qu'aucun écran n'est implémenté.
 */
@Composable
fun PlaceholderScreen() {
	Scaffold(modifier = Modifier.fillMaxSize()) { inPadding ->
		Box(
			modifier = Modifier.fillMaxSize().padding(inPadding),
			contentAlignment = Alignment.Center,
		) {
			Text(text = stringResource(R.string.placeholder_message))
		}
	}
}

/**
 * Aperçu Android Studio de l'écran provisoire.
 */
@Preview(showBackground = true)
@Composable
private fun PlaceholderScreenPreview() {
	ChessPlusTheme {
		PlaceholderScreen()
	}
}
