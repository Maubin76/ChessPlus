package com.chessplus.app

import android.content.Intent
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
import com.chessplus.app.widget.ChessPlusWidget
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Activité unique de ChessPlus.
 */
class MainActivity : ComponentActivity() {
	/** Écran demandé par le widget (« daily »), en attente d'être ouvert, ou null. */
	private val pendingOpen = MutableStateFlow<String?>(null)

	/**
	 * Reçoit une nouvelle intention (widget touché alors que l'application est ouverte).
	 */
	override fun onNewIntent(inIntent: Intent) {
		super.onNewIntent(inIntent)
		pendingOpen.value = inIntent.getStringExtra(ChessPlusWidget.kExtraOpen)
	}

	/**
	 * Redémarre l'application (après l'import d'une progression, installé avant l'ouverture de la base).
	 */
	private fun restart() {
		val vIntent = packageManager.getLaunchIntentForPackage(packageName)?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK) ?: return
		startActivity(vIntent)
		Runtime.getRuntime().exit(0)
	}

	/**
	 * Installe l'application Compose, avec le thème choisi dans les réglages.
	 */
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		if (savedInstanceState == null) {
			pendingOpen.value = intent?.getStringExtra(ChessPlusWidget.kExtraOpen)
		}
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
				val vOpen by pendingOpen.collectAsStateWithLifecycle()
				ChessPlusApp(vContainer, vVersion, inOnExit = { finish() }, inOnRestart = ::restart, inOpen = vOpen, inOnOpened = { pendingOpen.value = null })
			}
		}
	}
}
