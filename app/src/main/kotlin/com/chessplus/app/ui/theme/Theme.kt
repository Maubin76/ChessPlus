package com.chessplus.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/**
 * Thème Compose de ChessPlus, clair ou sombre selon le système.
 *
 * Palette Material par défaut en attendant les décisions de la phase de design.
 */
@Composable
fun ChessPlusTheme(
	inDarkTheme: Boolean = isSystemInDarkTheme(),
	inContent: @Composable () -> Unit,
) {
	val vColorScheme = if (inDarkTheme) darkColorScheme() else lightColorScheme()
	MaterialTheme(colorScheme = vColorScheme, content = inContent)
}
