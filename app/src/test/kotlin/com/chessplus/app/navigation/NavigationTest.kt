package com.chessplus.app.navigation

import androidx.lifecycle.SavedStateHandle
import com.chessplus.app.game.ChallengeKind
import com.chessplus.app.game.Difficulty
import com.chessplus.app.game.GameMode
import com.chessplus.app.game.TrainingCriteria
import com.chessplus.app.ui.components.NavTab
import com.chessplus.core.model.Theme
import com.chessplus.core.model.ThemeSet
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests de la navigation : encodage des écrans et pile.
 */
class NavigationTest {
	/** Vérifie l'aller-retour de l'encodage de toutes les destinations. */
	@Test
	fun codecRoundTrip() {
		val vDestinations = listOf(
			Destination.Level(firstLaunch = true),
			Destination.Level(firstLaunch = false),
			Destination.Home,
			Destination.History,
			Destination.Stats,
			Destination.ThemeStats(Theme.Fork),
			Destination.Settings,
			Destination.Targeted,
			Destination.Game(GameMode.Continuous),
			Destination.Game(GameMode.Replay("00sHx")),
			Destination.Game(GameMode.Daily("00sJ9")),
			Destination.Game(GameMode.Challenge(ChallengeKind.Survival)),
			Destination.Game(GameMode.Challenge(ChallengeKind.Rush)),
			Destination.Game(GameMode.Targeted(TrainingCriteria(ThemeSet.parse("fork zugzwang smotheredMate"), Difficulty.Range, 900..1300))),
		)
		for (vDestination in vDestinations) {
			assertEquals(vDestination, DestinationCodec.decode(DestinationCodec.encode(vDestination)))
		}
		assertNull(DestinationCodec.decode("inconnu"))
		assertNull(DestinationCodec.decode("game:targeted:x"))
	}

	/** Vérifie l'ouverture, le retour et le remplacement d'écrans. */
	@Test
	fun backStack() {
		val vNavigation = NavigationViewModel(SavedStateHandle())
		assertFalse(vNavigation.isInitialized)
		vNavigation.initialize(Destination.Level(firstLaunch = true))
		vNavigation.replace(Destination.Home)
		assertEquals(listOf<Destination>(Destination.Home), vNavigation.backStack.map { it.destination })
		vNavigation.navigate(Destination.Game(GameMode.Continuous))
		assertTrue(vNavigation.back())
		assertFalse(vNavigation.back())
		assertEquals(Destination.Home, vNavigation.backStack.single().destination)
	}

	/** Vérifie les onglets : le retour depuis un onglet ramène à l'accueil. */
	@Test
	fun tabs() {
		val vNavigation = NavigationViewModel(SavedStateHandle())
		vNavigation.initialize(Destination.Home)
		vNavigation.navigate(Destination.Targeted)
		vNavigation.selectTab(NavTab.Stats)
		assertEquals(listOf(Destination.Home, Destination.Stats), vNavigation.backStack.map { it.destination })
		vNavigation.selectTab(NavTab.Settings)
		assertEquals(listOf(Destination.Home, Destination.Settings), vNavigation.backStack.map { it.destination })
		vNavigation.selectTab(NavTab.Home)
		assertEquals(listOf<Destination>(Destination.Home), vNavigation.backStack.map { it.destination })
	}

	/** Vérifie la reprise de la pile après la mort du processus. */
	@Test
	fun restoreAfterProcessDeath() {
		val vState = SavedStateHandle()
		val vFirst = NavigationViewModel(vState)
		vFirst.initialize(Destination.Home)
		vFirst.navigate(Destination.Game(GameMode.Replay("abc")))
		val vRestored = NavigationViewModel(vState)
		assertTrue(vRestored.isInitialized)
		assertEquals(listOf(Destination.Home, Destination.Game(GameMode.Replay("abc"))), vRestored.backStack.map { it.destination })
	}
}
