package com.chessplus.app.game

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Retours haptiques du jeu, joués par le vibreur (plus nets que le retour haptique d'interface) :
 * un clic franc quand le dernier coup d'un puzzle est trouvé, deux impulsions sèches sur un mauvais coup.
 *
 * @property context contexte de l'application.
 */
class GameHaptics(context: Context) {
	/** Vibreur par défaut du téléphone. */
	private val vibrator: Vibrator = context.getSystemService(VibratorManager::class.java).defaultVibrator

	/**
	 * Joue le retour d'une réussite ([inSuccess]) ou d'une erreur.
	 */
	fun play(inSuccess: Boolean) {
		if (!vibrator.hasVibrator()) {
			return
		}
		vibrator.vibrate(if (inSuccess) successEffect() else errorEffect())
	}

	/**
	 * Clic franc de la réussite.
	 */
	private fun successEffect(): VibrationEffect = VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK)

	/**
	 * Deux impulsions sèches de l'erreur (pleine intensité si le vibreur la règle).
	 */
	private fun errorEffect(): VibrationEffect =
		if (vibrator.hasAmplitudeControl()) VibrationEffect.createWaveform(kErrorTimings, kErrorAmplitudes, -1)
		else VibrationEffect.createWaveform(kErrorTimings, -1)

	companion object {
		/** Rythme de l'erreur : attente, impulsion, pause, impulsion (millisecondes). */
		private val kErrorTimings = longArrayOf(0, 45, 70, 45)

		/** Intensité de chaque segment du rythme de l'erreur. */
		private val kErrorAmplitudes = intArrayOf(0, 255, 0, 255)
	}
}

/**
 * Retourne les retours haptiques du jeu.
 */
@Composable
fun rememberGameHaptics(): GameHaptics {
	val vContext = LocalContext.current
	return remember { GameHaptics(vContext.applicationContext) }
}
