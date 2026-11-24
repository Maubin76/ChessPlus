package com.chessplus.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chessplus.app.settings.ThemeMode
import com.chessplus.app.ui.theme.ChessPlusTheme

/**
 * Activité unique de ChessPlus.
 */
class MainActivity : ComponentActivity() {
	/**
	 * Installe l'application Compose, avec le thème choisi dans les réglages.
	 */
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		val vContainer = (application as ChessPlusApplication).container
		val vVersion = packageManager.getPackageInfo(packageName, 0).versionName ?: ""
		setContent {
			val vSettings by vContainer.settingsStore.settings.collectAsStateWithLifecycle()
			val vDark = when (vSettings.themeMode) {
				ThemeMode.System -> isSystemInDarkTheme()
				ThemeMode.Light -> false
				ThemeMode.Dark -> true
			}
			LaunchedEffect(vDark) {
				val vStyle = if (vDark) SystemBarStyle.dark(Color.TRANSPARENT) else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
				enableEdgeToEdge(statusBarStyle = vStyle, navigationBarStyle = vStyle)
			}
			ChessPlusTheme(inDarkTheme = vDark, inBoard = vSettings.boardTheme, inPieces = vSettings.pieceStyle) {
				ChessPlusApp(vContainer, vVersion, inOnExit = { finish() })
			}
		}
	}
}
