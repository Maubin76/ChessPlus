package com.chessplus.app.settings

import android.content.Context
import com.chessplus.app.goal.DailyGoal
import com.chessplus.app.ui.board.PieceStyle
import com.chessplus.app.ui.theme.BoardTheme
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
 * Difficulté des puzzles Arcade, relative au classement du joueur.
 *
 * @property offset écart entre la difficulté visée et le classement.
 */
enum class PuzzleDifficulty(val offset: Int) {
	/** Plus facile : 200 points sous le classement. */
	Easier(-200),

	/** Normal : autour du classement. */
	Normal(0),

	/** Plus dur : 200 points au-dessus. */
	Harder(200),
}

/**
 * Réglages de l'application (valeurs par défaut des maquettes chessplus-design/reglages).
 *
 * @property themeMode apparence : système, clair ou sombre.
 * @property vibration courte vibration à chaque coup.
 * @property sounds sons des coups, des erreurs et des réussites.
 * @property coordinates coordonnées dans les cases.
 * @property boardTheme couleurs de l'échiquier.
 * @property pieceStyle style des pièces.
 * @property confirmMoves un coup doit être confirmé avant d'être joué.
 * @property difficulty difficulté des puzzles Arcade.
 * @property autoNext puzzle suivant automatiquement après une réussite.
 * @property dailyGoal objectif quotidien : nombre de puzzles à réussir.
 * @property reminder rappel par notification si l'objectif n'est pas atteint à l'heure choisie.
 * @property reminderMinutes heure du rappel, en minutes depuis minuit.
 */
data class Settings(
	val themeMode: ThemeMode = ThemeMode.System,
	val vibration: Boolean = true,
	val sounds: Boolean = true,
	val coordinates: Boolean = true,
	val boardTheme: BoardTheme = BoardTheme.Sage,
	val pieceStyle: PieceStyle = PieceStyle.Classic,
	val confirmMoves: Boolean = false,
	val difficulty: PuzzleDifficulty = PuzzleDifficulty.Normal,
	val autoNext: Boolean = true,
	val dailyGoal: Int = DailyGoal.kDefaultGoal,
	val reminder: Boolean = false,
	val reminderMinutes: Int = DailyGoal.kDefaultReminderMinutes,
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
			.putString(kBoardKey, vNew.boardTheme.name)
			.putString(kPiecesKey, vNew.pieceStyle.name)
			.putBoolean(kConfirmKey, vNew.confirmMoves)
			.putString(kDifficultyKey, vNew.difficulty.name)
			.putBoolean(kAutoNextKey, vNew.autoNext)
			.putInt(kGoalKey, vNew.dailyGoal)
			.putBoolean(kReminderKey, vNew.reminder)
			.putInt(kReminderMinutesKey, vNew.reminderMinutes)
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
			boardTheme = runCatching { BoardTheme.valueOf(preferences.getString(kBoardKey, null) ?: "") }.getOrDefault(vDefaults.boardTheme),
			pieceStyle = runCatching { PieceStyle.valueOf(preferences.getString(kPiecesKey, null) ?: "") }.getOrDefault(vDefaults.pieceStyle),
			confirmMoves = preferences.getBoolean(kConfirmKey, vDefaults.confirmMoves),
			difficulty = runCatching { PuzzleDifficulty.valueOf(preferences.getString(kDifficultyKey, null) ?: "") }.getOrDefault(vDefaults.difficulty),
			autoNext = preferences.getBoolean(kAutoNextKey, vDefaults.autoNext),
			dailyGoal = preferences.getInt(kGoalKey, vDefaults.dailyGoal),
			reminder = preferences.getBoolean(kReminderKey, vDefaults.reminder),
			reminderMinutes = preferences.getInt(kReminderMinutesKey, vDefaults.reminderMinutes),
		)
	}

	companion object {
		/** Nom du fichier de préférences. */
		private const val kFileName = "settings"

		/** Clé du thème. */
		private const val kThemeKey = "theme"

		/** Clé de la vibration. */
		private const val kVibrationKey = "vibration"

		/** Clé des sons (nouvelle clé : l'ancien réglage « Bientôt disponible » est ignoré). */
		private const val kSoundsKey = "gameSounds"

		/** Clé des coordonnées. */
		private const val kCoordinatesKey = "coordinates"

		/** Clé du thème d'échiquier. */
		private const val kBoardKey = "board"

		/** Clé du style des pièces. */
		private const val kPiecesKey = "pieces"

		/** Clé de la confirmation des coups. */
		private const val kConfirmKey = "confirmMoves"

		/** Clé de la difficulté. */
		private const val kDifficultyKey = "difficulty"

		/** Clé de l'enchaînement automatique. */
		private const val kAutoNextKey = "autoNext"

		/** Clé de l'objectif quotidien. */
		private const val kGoalKey = "dailyGoal"

		/** Clé du rappel. */
		private const val kReminderKey = "reminder"

		/** Clé de l'heure du rappel. */
		private const val kReminderMinutesKey = "reminderMinutes"
	}
}
