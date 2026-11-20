package com.chessplus.app.settings

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Choix du thème clair ou sombre.
 */
enum class ThemeMode {
	System,
	Light,
	Dark,
}

/**
 * Réglages de l'application (valeurs par défaut de docs/DESIGN.md).
 *
 * @property themeMode thème clair, sombre, ou celui du système.
 * @property vibration courte vibration à chaque coup.
 * @property sounds sons (désactivés par défaut).
 * @property coordinates coordonnées dans les cases.
 */
data class Settings(
	val themeMode: ThemeMode = ThemeMode.System,
	val vibration: Boolean = true,
	val sounds: Boolean = false,
	val coordinates: Boolean = true,
)

/**
 * Stockage des réglages dans les préférences partagées d'Android.
 *
 * @property context contexte de l'application.
 */
class SettingsStore(context: Context) {
	/** Préférences de l'application. */
	private val preferences = context.getSharedPreferences(kFileName, Context.MODE_PRIVATE)

	/** Réglages courants. */
	private val mutableSettings = MutableStateFlow(read())

	/** Réglages observables. */
	val settings: StateFlow<Settings> = mutableSettings.asStateFlow()

	/**
	 * Modifie les réglages et les enregistre.
	 */
	fun update(inTransform: (Settings) -> Settings) {
		val vNew = inTransform(mutableSettings.value)
		preferences.edit()
			.putString(kThemeKey, vNew.themeMode.name)
			.putBoolean(kVibrationKey, vNew.vibration)
			.putBoolean(kSoundsKey, vNew.sounds)
			.putBoolean(kCoordinatesKey, vNew.coordinates)
			.apply()
		mutableSettings.value = vNew
	}

	/**
	 * Lit les réglages enregistrés.
	 */
	private fun read(): Settings {
		val vDefaults = Settings()
		return Settings(
			themeMode = runCatching { ThemeMode.valueOf(preferences.getString(kThemeKey, null) ?: "") }.getOrDefault(vDefaults.themeMode),
			vibration = preferences.getBoolean(kVibrationKey, vDefaults.vibration),
			sounds = preferences.getBoolean(kSoundsKey, vDefaults.sounds),
			coordinates = preferences.getBoolean(kCoordinatesKey, vDefaults.coordinates),
		)
	}

	companion object {
		/** Nom du fichier de préférences. */
		private const val kFileName = "settings"

		/** Clé du thème. */
		private const val kThemeKey = "theme"

		/** Clé de la vibration. */
		private const val kVibrationKey = "vibration"

		/** Clé des sons. */
		private const val kSoundsKey = "sounds"

		/** Clé des coordonnées. */
		private const val kCoordinatesKey = "coordinates"
	}
}
