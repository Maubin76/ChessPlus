package com.chessplus.app.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chessplus.app.R
import com.chessplus.app.ui.components.NavigationRow
import com.chessplus.app.ui.components.ScreenTopBar
import com.chessplus.app.ui.components.SegmentedControl
import com.chessplus.app.ui.components.SurfaceCard
import com.chessplus.app.ui.components.SwitchRow
import com.chessplus.app.ui.theme.ChessType
import com.chessplus.app.ui.theme.kLocalChessColors

/**
 * Réglages : thème, vibration, sons, coordonnées, niveau, crédits.
 */
@Composable
fun SettingsScreen(inStore: SettingsStore, inVersion: String, inOnBack: () -> Unit, inOnLevel: () -> Unit) {
	val vSettings by inStore.settings.collectAsStateWithLifecycle()
	SettingsContent(vSettings, inStore::update, inVersion, inOnBack, inOnLevel)
}

/**
 * Contenu des réglages, sans état propre (prévisualisable).
 */
@Composable
fun SettingsContent(inSettings: Settings, inOnUpdate: ((Settings) -> Settings) -> Unit, inVersion: String, inOnBack: () -> Unit, inOnLevel: () -> Unit) {
	val vSettings = inSettings
	val vColors = kLocalChessColors.current
	Column(Modifier.fillMaxSize().background(vColors.background).statusBarsPadding().verticalScroll(rememberScrollState())) {
		ScreenTopBar(stringResource(R.string.settings_title), inOnBack)
		Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
			SurfaceCard {
				Text(stringResource(R.string.settings_theme), style = ChessType.kBodyStrong, color = vColors.text)
				Spacer(Modifier.height(10.dp))
				SegmentedControl(
					listOf(stringResource(R.string.settings_theme_system), stringResource(R.string.settings_theme_light), stringResource(R.string.settings_theme_dark)),
					vSettings.themeMode.ordinal,
				) { inIndex -> inOnUpdate { it.copy(themeMode = ThemeMode.entries[inIndex]) } }
			}
			SurfaceCard {
				SwitchRow(stringResource(R.string.settings_vibration), stringResource(R.string.settings_vibration_detail), vSettings.vibration, { inOn -> inOnUpdate { it.copy(vibration = inOn) } })
				SwitchRow(stringResource(R.string.settings_sounds), stringResource(R.string.settings_sounds_detail), false, {}, inEnabled = false)
				SwitchRow(stringResource(R.string.settings_coordinates), stringResource(R.string.settings_coordinates_detail), vSettings.coordinates, { inOn -> inOnUpdate { it.copy(coordinates = inOn) } })
			}
			SurfaceCard {
				NavigationRow(stringResource(R.string.settings_level), null, inOnLevel)
			}
			SurfaceCard {
				Text(stringResource(R.string.settings_credits), style = ChessType.kBodyStrong, color = vColors.text)
				Spacer(Modifier.height(6.dp))
				Text(stringResource(R.string.settings_credits_detail), style = ChessType.kCaption, color = vColors.muted)
			}
			Text(
				stringResource(R.string.settings_version, inVersion),
				style = ChessType.kCaption,
				color = vColors.muted,
				modifier = Modifier.align(Alignment.CenterHorizontally).navigationBarsPadding().padding(bottom = 16.dp),
			)
		}
	}
}
