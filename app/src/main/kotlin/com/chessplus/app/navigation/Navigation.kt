package com.chessplus.app.navigation

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import com.chessplus.app.game.ChallengeKind
import com.chessplus.app.game.Difficulty
import com.chessplus.app.game.GameMode
import com.chessplus.app.game.TrainingCriteria
import com.chessplus.app.ui.components.NavTab
import com.chessplus.core.model.Theme
import com.chessplus.core.model.ThemeSet

/**
 * Écran de l'application.
 */
sealed interface Destination {
	/** Choix du niveau ; [firstLaunch] : premier lancement (pas de retour possible). */
	data class Level(val firstLaunch: Boolean) : Destination

	/** Accueil. */
	data object Home : Destination

	/** Historique. */
	data object History : Destination

	/** Statistiques. */
	data object Stats : Destination

	/** Fiche de statistiques d'un thème. */
	data class ThemeStats(val theme: Theme) : Destination

	/** Réglages. */
	data object Settings : Destination

	/** Entraînement ciblé (choix des critères). */
	data object Targeted : Destination

	/** Écran de jeu dans un mode donné. */
	data class Game(val mode: GameMode) : Destination
}

/**
 * Retourne l'onglet de la barre du bas d'une destination, ou null si elle n'en affiche pas.
 */
fun Destination.tab(): NavTab? = when (this) {
	Destination.Home -> NavTab.Home
	Destination.History -> NavTab.History
	Destination.Stats -> NavTab.Stats
	else -> null
}

/**
 * Conversion des destinations en texte, pour la sauvegarde de la pile après la mort du processus.
 */
object DestinationCodec {
	/** Séparateur des champs. */
	private const val kSeparator = ":"

	/**
	 * Encode une destination.
	 */
	fun encode(inDestination: Destination): String = when (inDestination) {
		is Destination.Level -> "level$kSeparator${inDestination.firstLaunch}"
		Destination.Home -> "home"
		Destination.History -> "history"
		Destination.Stats -> "stats"
		is Destination.ThemeStats -> "themestats$kSeparator${inDestination.theme.key}"
		Destination.Settings -> "settings"
		Destination.Targeted -> "targeted"
		is Destination.Game -> when (val vMode = inDestination.mode) {
			GameMode.Continuous -> "game${kSeparator}continuous"
			is GameMode.Replay -> "game${kSeparator}replay$kSeparator${vMode.puzzleId}"
			is GameMode.Challenge -> "game${kSeparator}challenge$kSeparator${vMode.kind.key}"
			is GameMode.Targeted -> listOf(
				"game", "targeted", vMode.criteria.themes.low, vMode.criteria.themes.high, vMode.criteria.difficulty.name,
				vMode.criteria.range.first, vMode.criteria.range.last,
			).joinToString(kSeparator)
		}
	}

	/**
	 * Décode une destination, ou retourne null si le texte est invalide.
	 */
	fun decode(inText: String): Destination? = runCatching {
		val vParts = inText.split(kSeparator)
		when (vParts[0]) {
			"level" -> Destination.Level(vParts[1].toBoolean())
			"home" -> Destination.Home
			"history" -> Destination.History
			"stats" -> Destination.Stats
			"themestats" -> Theme.fromKey(vParts[1])?.let { Destination.ThemeStats(it) }
			"settings" -> Destination.Settings
			"targeted" -> Destination.Targeted
			"game" -> when (vParts[1]) {
				"continuous" -> Destination.Game(GameMode.Continuous)
				"replay" -> Destination.Game(GameMode.Replay(vParts[2]))
				"challenge" -> ChallengeKind.entries.firstOrNull { it.key == vParts[2] }?.let { Destination.Game(GameMode.Challenge(it)) }
				"targeted" -> Destination.Game(
					GameMode.Targeted(
						TrainingCriteria(ThemeSet(vParts[2].toLong(), vParts[3].toLong()), Difficulty.valueOf(vParts[4]), vParts[5].toInt()..vParts[6].toInt())
					)
				)
				else -> null
			}
			else -> null
		}
	}.getOrNull()
}

/**
 * Entrée de la pile de navigation : une destination et les ViewModels de son écran.
 *
 * @property destination écran affiché.
 * @property id identifiant unique de l'entrée.
 */
class BackStackEntry(val destination: Destination, val id: Long) : ViewModelStoreOwner {
	/** ViewModels de l'écran, libérés quand l'entrée quitte la pile. */
	override val viewModelStore = ViewModelStore()
}

/**
 * Pile de navigation de l'application, conservée lors des rotations (ViewModel) et après la mort
 * du processus (SavedStateHandle).
 *
 * @property savedState état sauvegardé par le système.
 */
class NavigationViewModel(private val savedState: SavedStateHandle) : ViewModel() {
	/** Compteur des identifiants d'entrées. */
	private var nextId = 0L

	/** Pile des écrans ; le dernier est affiché. */
	val backStack: SnapshotStateList<BackStackEntry> = mutableStateListOf()

	/** Écrans retirés de la pile, encore affichés pendant leur animation de sortie. */
	private val retired = mutableListOf<BackStackEntry>()

	/** Vrai quand la pile a été initialisée (reprise ou premier écran choisi). */
	val isInitialized: Boolean
		get() = backStack.isNotEmpty()

	init {
		savedState.get<ArrayList<String>>(kStackKey)?.mapNotNull { DestinationCodec.decode(it) }?.forEach { backStack.add(newEntry(it)) }
	}

	/**
	 * Initialise la pile avec le premier écran, si elle est vide.
	 */
	fun initialize(inStart: Destination) {
		if (backStack.isEmpty()) {
			backStack.add(newEntry(inStart))
			save()
		}
	}

	/**
	 * Ouvre un écran par-dessus le courant.
	 */
	fun navigate(inDestination: Destination) {
		backStack.add(newEntry(inDestination))
		save()
	}

	/**
	 * Remplace l'écran courant.
	 */
	fun replace(inDestination: Destination) {
		backStack.removeLastOrNull()?.let { retired.add(it) }
		navigate(inDestination)
	}

	/**
	 * Revient à l'écran précédent ; retourne faux s'il n'y en a pas.
	 */
	fun back(): Boolean {
		if (backStack.size <= 1) {
			return false
		}
		retired.add(backStack.removeAt(backStack.lastIndex))
		save()
		return true
	}

	/**
	 * Affiche un onglet de la barre du bas : l'accueil seul, ou l'accueil puis l'onglet (le retour
	 * depuis un onglet ramène à l'accueil).
	 */
	fun selectTab(inTab: NavTab) {
		val vTarget = when (inTab) {
			NavTab.Home -> Destination.Home
			NavTab.History -> Destination.History
			NavTab.Stats -> Destination.Stats
		}
		if (backStack.lastOrNull()?.destination == vTarget) {
			return
		}
		while (backStack.size > 1) {
			retired.add(backStack.removeAt(backStack.lastIndex))
		}
		if (backStack.firstOrNull()?.destination != Destination.Home) {
			backStack.removeLastOrNull()?.let { retired.add(it) }
			backStack.add(newEntry(Destination.Home))
		}
		if (vTarget != Destination.Home) {
			backStack.add(newEntry(vTarget))
		}
		save()
	}

	/**
	 * Libère les ViewModels des écrans retirés, une fois leur animation de sortie terminée.
	 */
	fun clearRetired() {
		retired.forEach { it.viewModelStore.clear() }
		retired.clear()
	}

	/**
	 * Libère les ViewModels de tous les écrans.
	 */
	override fun onCleared() {
		clearRetired()
		backStack.forEach { it.viewModelStore.clear() }
	}

	/**
	 * Crée une entrée de pile.
	 */
	private fun newEntry(inDestination: Destination) = BackStackEntry(inDestination, nextId++)

	/**
	 * Sauvegarde la pile.
	 */
	private fun save() {
		savedState[kStackKey] = ArrayList(backStack.map { DestinationCodec.encode(it.destination) })
	}

	companion object {
		/** Clé de la pile dans l'état sauvegardé. */
		private const val kStackKey = "backStack"
	}
}
