package com.chessplus.app.game

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.chessplus.app.R

/**
 * Lecture des sons de jeu (créations ChessPlus, res/raw) avec SoundPool : chargés une fois, joués
 * sans latence, mixés avec la musique du téléphone.
 *
 * @property context contexte de l'application.
 */
class SoundPlayer(context: Context) {
	/** Réserve de sons. */
	private val pool: SoundPool = SoundPool.Builder()
		.setMaxStreams(kMaxStreams)
		.setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
		.build()

	/** Identifiant de chaque son chargé. */
	private val soundIds: Map<SoundKind, Int> = SoundKind.entries.associateWith { vKind ->
		pool.load(
			context,
			when (vKind) {
				SoundKind.Move -> R.raw.sound_move
				SoundKind.Capture -> R.raw.sound_capture
				SoundKind.Error -> R.raw.sound_error
				SoundKind.Success -> R.raw.sound_success
			},
			1,
		)
	}

	/**
	 * Joue un son.
	 */
	fun play(inKind: SoundKind) {
		soundIds[inKind]?.let { pool.play(it, 1f, 1f, 1, 0, 1f) }
	}

	/**
	 * Libère les sons.
	 */
	fun release() = pool.release()

	companion object {
		/** Sons joués en même temps au plus (un coup et sa réponse). */
		private const val kMaxStreams = 2
	}
}

/**
 * Retourne un lecteur de sons lié à la composition (libéré quand l'écran disparaît).
 */
@Composable
fun rememberSoundPlayer(): SoundPlayer {
	val vContext = LocalContext.current
	val vPlayer = remember { SoundPlayer(vContext.applicationContext) }
	DisposableEffect(vPlayer) { onDispose { vPlayer.release() } }
	return vPlayer
}
