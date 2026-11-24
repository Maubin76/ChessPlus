package com.chessplus.app.game

import com.chessplus.core.model.Color
import com.chessplus.core.model.Move
import com.chessplus.core.model.PieceType
import com.chessplus.core.model.Square
import com.chessplus.core.model.ThemeSet
import com.chessplus.core.puzzle.LichessCsv
import com.chessplus.core.puzzle.Puzzle
import com.chessplus.core.puzzle.PuzzleResult
import com.chessplus.core.puzzle.PuzzleSnapshot
import com.chessplus.core.rating.EloRatingSystem
import com.chessplus.core.rating.PlayerRating
import com.chessplus.core.rules.defaultChessRules
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests de la logique de l'écran de jeu, avec des stockages en mémoire.
 */
class GameControllerTest {
	/** Mat en 2 (00sHx) : les noirs jouent e8d7, le joueur (blancs) trouve a2e6 puis f7f8. */
	private val kMateIn2 = LichessCsv.parseLine(
		"00sHx,q3k1nr/1pp1nQpp/3p4/1P2p3/4P3/B1PP1b2/B5PP/5K2 b k - 0 17,e8d7 a2e6 d7d8 f7f8,1760,80,83,72,mate mateIn2 middlegame short,https://lichess.org/yyznGmXs/black#34,,"
	).toPuzzle()

	/** Problème court (00sO1) : le joueur joue les blancs. */
	private val kShort = LichessCsv.parseLine(
		"00sO1,1k1r4/pp3pp1/2p1p3/4b3/P3n1P1/8/KPP2PN1/3rBR1R b - - 2 31,b8c7 e1a5 b7b6 f1d1,998,85,94,293,advantage discoveredAttack master middlegame short,https://lichess.org/vsfFkG0s/black#62,,"
	).toPuzzle()

	/** Promotion en dame attendue. */
	private val kPromotion = Puzzle("promo", "8/1P6/8/7k/7p/8/8/2K5 b - - 0 1", Move.listFromUci("h4h3 b7b8q"), 1000, ThemeSet.kEmpty)

	/**
	 * Source de problèmes en mémoire : propose les problèmes dans l'ordre, sauf l'exclu.
	 */
	private class FakePuzzles(val list: List<Puzzle>, val played: MutableSet<String> = mutableSetOf()) : PuzzleProvider {
		/** Recherche par identifiant. */
		override suspend fun byId(inId: String): Puzzle? = list.firstOrNull { it.id == inId }

		/** Modes demandés, dans l'ordre. */
		val modes = mutableListOf<GameMode>()

		/** Cotes visées des problèmes demandés, dans l'ordre. */
		val ratings = mutableListOf<Double>()

		/** Premier problème différent de l'exclu. */
		override suspend fun next(inRating: Double, inExcludeId: String?, inMode: GameMode): Puzzle? {
			modes.add(inMode)
			ratings.add(inRating)
			return list.firstOrNull { it.id != inExcludeId }
		}

		/** Nouveau si jamais joué. */
		override suspend fun isNew(inId: String): Boolean = inId !in played

		/** Cotes visées des problèmes de défi demandés, dans l'ordre. */
		val centers = mutableListOf<Int>()

		/** Premier problème pas encore vu dans la partie. */
		override suspend fun challengeNext(inCenter: Int, inExclude: Set<String>): Puzzle? {
			centers.add(inCenter)
			return list.firstOrNull { it.id !in inExclude }
		}
	}

	/**
	 * Parties de défi en mémoire.
	 */
	private class FakeChallenges(var bestScore: Int? = null) : ChallengeStore {
		/** Parties enregistrées (défi, score). */
		val saved = mutableListOf<Pair<ChallengeKind, Int>>()

		/** Meilleur score. */
		override suspend fun best(inKind: ChallengeKind): Int? = bestScore

		/** Enregistrement. */
		override suspend fun save(inKind: ChallengeKind, inScore: Int, inBestPuzzleRating: Int?, inDurationMs: Long) {
			saved.add(inKind to inScore)
		}
	}

	/**
	 * Progression en mémoire, cote Elo.
	 */
	private class FakeProgress : ProgressStore {
		/** Cote actuelle. */
		var rating = PlayerRating(1500.0, 0.0, 0.0)

		/** Résultats enregistrés (problème, résultat, noté). */
		val records = mutableListOf<Triple<String, PuzzleResult, Boolean>>()

		/** Durée et coup de l'échec de chaque enregistrement. */
		val details = mutableListOf<Pair<Long?, Int?>>()

		/** Dernier résultat par problème. */
		private val last = mutableMapOf<String, RecordedResult>()

		/** Cote actuelle. */
		override suspend fun currentRating(): PlayerRating = rating

		/** Enregistre et met à jour la cote si le problème est noté. */
		override suspend fun record(inPuzzle: Puzzle, inResult: PuzzleResult, inMistakes: Int, inRevealed: Int, inRated: Boolean, inDurationMs: Long?, inFailedAtMove: Int?): RecordedResult {
			details.add(inDurationMs to inFailedAtMove)
			val vBefore = rating.rating
			if (inRated) {
				rating = EloRatingSystem().update(rating, inPuzzle.rating, inResult)
			}
			records.add(Triple(inPuzzle.id, inResult, inRated))
			return RecordedResult(inResult, vBefore, rating.rating).also { last[inPuzzle.id] = it }
		}

		/** Dernier résultat. */
		override suspend fun lastResult(inPuzzleId: String): RecordedResult? = last[inPuzzleId]
	}

	/**
	 * Sauvegarde en mémoire.
	 */
	private class FakeSessions(var saved: SavedSession? = null) : SessionStore {
		/** Lecture. */
		override suspend fun load(): SavedSession? = saved

		/** Écriture. */
		override suspend fun save(inSession: SavedSession) {
			saved = inSession
		}

		/** Effacement. */
		override suspend fun clear() {
			saved = null
		}
	}

	/**
	 * Crée un contrôleur sans pauses, dans la portée du test.
	 */
	private fun CoroutineScope.controller(
		inPuzzles: PuzzleProvider,
		inProgress: ProgressStore = FakeProgress(),
		inSessions: SessionStore = FakeSessions(),
		inMode: GameMode = GameMode.Continuous,
		inChallenges: ChallengeStore = FakeChallenges(),
		inRushDurationMs: Long = ChallengeRun.kRushDurationMs,
		inPreferences: GamePreferences = GamePreferences.kDefault,
		inDaily: DailyStore = DailyStore.kNone,
	) = GameController(
		this, inMode, defaultChessRules(), inPuzzles, inProgress, inSessions, inChallenges, clock = { 42L }, preferences = inPreferences, daily = inDaily,
		opponentDelayMs = 0, startDelayMs = 0, wrongMoveDelayMs = 0, challengeNextDelayMs = 0, solutionDelayMs = 0, rushDurationMs = inRushDurationMs,
		autoNextDelayMs = 0,
	)

	/**
	 * Puzzles du jour en mémoire : un puzzle par jour ; un jour est résolu si son puzzle a été réussi.
	 */
	private class FakeDaily(val byDay: Map<String, Puzzle>, val progress: FakeProgress) : DailyStore {
		/** Puzzle du jour. */
		override suspend fun puzzleFor(inDay: String): Puzzle? = byDay[inDay]

		/** Jours dont le puzzle a été réussi. */
		override suspend fun solvedDays(): Set<String> =
			byDay.filter { (_, vPuzzle) -> progress.records.any { it.first == vPuzzle.id && it.second == PuzzleResult.Success } }.keys
	}

	/**
	 * Réglages de jeu modifiables pour les tests.
	 */
	private class FakePreferences(
		override var confirmMoves: Boolean = false,
		override var autoNext: Boolean = false,
		override var difficultyOffset: Int = 0,
	) : GamePreferences

	/**
	 * Retourne des copies de [kMateIn2] d'identifiants différents, pour enchaîner les problèmes d'un défi.
	 */
	private fun mateCopies(inCount: Int) = (1..inCount).map { kMateIn2.copy(id = "m$it") }

	/**
	 * Raccourci : case à partir de son nom.
	 */
	private fun sq(inName: String) = Square.fromAlgebraic(inName)

	/**
	 * Joue un coup par deux appuis et attend la fin des tâches.
	 */
	private suspend fun GameController.tapMove(inFrom: String, inTo: String) {
		onSquareTap(sq(inFrom))
		onSquareTap(sq(inTo))
		awaitIdle()
	}

	/** Vérifie le démarrage : premier coup de l'adversaire joué, orientation, sauvegarde. */
	@Test
	fun startPlaysOpponentFirstMove() = runBlocking<Unit> {
		val vSessions = FakeSessions()
		val vController = controller(FakePuzzles(listOf(kMateIn2)), inSessions = vSessions)
		vController.start()
		vController.awaitIdle()
		val vState = vController.state.value
		assertEquals(GameHeader.ToMove(Color.White, afterCorrect = false), vState.header)
		assertEquals(Color.White, vState.orientation)
		assertFalse(vState.showThemeTag, "pas de thème en Arcade")
		assertEquals(Move.fromUci("e8d7"), vState.lastMove)
		assertTrue(vState.animateMove)
		assertTrue(vState.interactive)
		assertEquals(1500, vState.rating)
		assertEquals(1, vSessions.saved?.snapshot?.solutionPly)
	}

	/** Vérifie la sélection : seules les pièces du joueur, avec leurs cases d'arrivée. */
	@Test
	fun selection() = runBlocking<Unit> {
		val vController = controller(FakePuzzles(listOf(kMateIn2)))
		vController.start()
		vController.awaitIdle()
		vController.onSquareTap(sq("d7"))
		assertNull(vController.state.value.selected)
		vController.onSquareTap(sq("a2"))
		assertEquals(sq("a2"), vController.state.value.selected)
		assertEquals(setOf("b1", "b3", "c4", "d5", "e6"), vController.state.value.targets.map { it.toAlgebraic() }.toSet())
		vController.onSquareTap(sq("a2"))
		assertNull(vController.state.value.selected)
	}

	/** Vérifie la résolution complète, le bilan et l'enregistrement d'une seule réussite notée. */
	@Test
	fun solvePuzzle() = runBlocking<Unit> {
		val vProgress = FakeProgress()
		val vController = controller(FakePuzzles(listOf(kMateIn2, kShort)), vProgress)
		vController.start()
		vController.awaitIdle()
		vController.onSquareTap(sq("a2"))
		vController.onSquareTap(sq("e6"))
		assertEquals(GameHeader.Correct(Color.Black), vController.state.value.header)
		assertEquals(sq("e6") to BadgeKind.Success, vController.state.value.badge)
		vController.awaitIdle()
		assertEquals(GameHeader.ToMove(Color.White, afterCorrect = true), vController.state.value.header)
		assertEquals(Move.fromUci("d7d8"), vController.state.value.lastMove)
		vController.tapMove("f7", "f8")
		val vState = vController.state.value
		assertEquals(GameHeader.Finished(success = true, mate = true, hintsUsed = false), vState.header)
		assertTrue(vState.nextEnabled)
		assertFalse(vState.interactive)
		val vSummary = assertNotNull(vState.summary)
		assertEquals(PuzzleResult.Success, vSummary.result)
		assertTrue(vSummary.ratingAfter > vSummary.ratingBefore)
		assertEquals(listOf(Triple("00sHx", PuzzleResult.Success, true)), vProgress.records)
		assertEquals(vSummary.ratingAfter.toInt(), vState.rating)
	}

	/** Vérifie qu'une erreur est signalée, enregistrée tout de suite comme échec, une seule fois. */
	@Test
	fun wrongMoveRecordsFailureOnce() = runBlocking<Unit> {
		val vProgress = FakeProgress()
		val vController = controller(FakePuzzles(listOf(kMateIn2)), vProgress)
		vController.start()
		vController.awaitIdle()
		vController.onSquareTap(sq("a2"))
		vController.onSquareTap(sq("b3"))
		// Le mauvais coup est d'abord montré (pièce sur la case visée, pastille d'erreur)…
		assertEquals(GameHeader.Wrong, vController.state.value.header)
		assertEquals(sq("b3") to BadgeKind.Error, vController.state.value.badge)
		assertNotNull(vController.state.value.pieces[sq("b3")])
		assertNull(vController.state.value.pieces[sq("a2")])
		assertFalse(vController.state.value.interactive)
		vController.awaitIdle()
		// … puis annulé : la pièce revient, le joueur peut réessayer.
		assertEquals(GameHeader.Wrong, vController.state.value.header)
		assertNull(vController.state.value.badge)
		assertNotNull(vController.state.value.pieces[sq("a2")])
		assertNull(vController.state.value.pieces[sq("b3")])
		assertTrue(vController.state.value.animateMove)
		assertEquals(listOf(Triple("00sHx", PuzzleResult.Failure, true)), vProgress.records)
		assertTrue(vController.state.value.interactive)
		vController.tapMove("a2", "e6")
		vController.tapMove("f7", "f8")
		assertEquals(1, vProgress.records.size)
		assertEquals(PuzzleResult.Failure, vController.state.value.summary?.result)
		assertEquals(1, vController.state.value.summary?.mistakes)
	}

	/** Vérifie l'indice en deux temps : pièce en surbrillance, puis coup joué ; échec enregistré. */
	@Test
	fun hint() = runBlocking<Unit> {
		val vProgress = FakeProgress()
		val vController = controller(FakePuzzles(listOf(kMateIn2)), vProgress)
		vController.start()
		vController.awaitIdle()
		vController.onHint()
		vController.awaitIdle()
		assertEquals(GameHeader.HintPiece(PieceType.Bishop), vController.state.value.header)
		assertEquals(sq("a2"), vController.state.value.hint)
		assertEquals(Move.fromUci("e8d7"), vController.state.value.lastMove)
		assertTrue(vController.state.value.interactive)
		assertEquals(PuzzleResult.Failure, vProgress.records.single().second)
		vController.onHint()
		assertEquals(sq("a2"), vController.state.value.hint, "un second Indice ne fait rien")
		assertFalse(vController.state.value.hintEnabled)
		vController.onSolution()
		assertIs<GameHeader.Hint>(vController.state.value.header)
		assertNull(vController.state.value.hint)
		vController.awaitIdle()
		assertEquals(Move.fromUci("d7d8"), vController.state.value.lastMove)
		vController.onHint()
		assertEquals(sq("f7"), vController.state.value.hint)
		vController.tapMove("f7", "f8")
		assertEquals(GameHeader.Finished(success = false, mate = true, hintsUsed = true), vController.state.value.header)
		assertNull(vController.state.value.hint)
		assertEquals(2, vController.state.value.summary?.revealed)
		assertEquals(1, vProgress.records.size)
	}

	/** Vérifie la notation (« 17… Kd7 18. ? ») et le bilan de la session. */
	@Test
	fun notationAndTally() = runBlocking<Unit> {
		val vController = controller(FakePuzzles(listOf(kMateIn2, kShort)))
		vController.start()
		vController.awaitIdle()
		assertEquals(listOf("17…", "Kd7", "18.", "?"), vController.state.value.notation.map { it.text })
		assertEquals(listOf(NotationKind.Past, NotationKind.Past, NotationKind.Current, NotationKind.Pending), vController.state.value.notation.map { it.kind })
		assertEquals(2, vController.state.value.remainingMoves)
		vController.tapMove("a2", "e6")
		assertEquals(listOf("17…", "Kd7", "18.", "Be6+", "Kd8", "19.", "?"), vController.state.value.notation.map { it.text })
		vController.tapMove("f7", "f8")
		assertEquals(SessionTally(1, 0, vController.state.value.tally.ratingDelta, 0L), vController.state.value.tally)
		assertTrue(vController.state.value.sessionRecap, "l'Arcade demande confirmation avant de quitter")
		assertTrue(vController.state.value.keepsPuzzle)
		assertEquals(42L, vController.state.value.sessionStartedAt, "début de la session à l'ouverture de l'écran")
		assertTrue((vController.state.value.tally.ratingDelta ?: 0) > 0)
	}

	/** Vérifie Passer : un puzzle non terminé compte comme raté, puis le suivant arrive. */
	@Test
	fun skipRecordsFailure() = runBlocking<Unit> {
		val vProgress = FakeProgress()
		val vController = controller(FakePuzzles(listOf(kMateIn2, kShort)), vProgress)
		vController.start()
		vController.awaitIdle()
		vController.onSkip()
		vController.awaitIdle()
		assertEquals(listOf(Triple("00sHx", PuzzleResult.Failure, true)), vProgress.records)
		assertEquals("00sO1", vController.state.value.puzzleId)
		assertEquals(SessionTally(0, 1, vController.state.value.tally.ratingDelta, 0L), vController.state.value.tally)
	}

	/** Vérifie Retourner : l'orientation change, pas le camp au trait. */
	@Test
	fun flip() = runBlocking<Unit> {
		val vController = controller(FakePuzzles(listOf(kMateIn2)))
		vController.start()
		vController.awaitIdle()
		vController.onFlip()
		assertEquals(Color.Black, vController.state.value.orientation)
		assertEquals(Color.White, vController.state.value.sideToMove)
	}

	/** Vérifie la confirmation des coups : rien n'est joué avant « Confirmer », « Annuler » rend la main. */
	@Test
	fun confirmMoves() = runBlocking<Unit> {
		val vProgress = FakeProgress()
		val vController = controller(FakePuzzles(listOf(kMateIn2)), vProgress, inPreferences = FakePreferences(confirmMoves = true))
		vController.start()
		vController.awaitIdle()
		vController.tapMove("f7", "f6")
		assertTrue(vController.state.value.pendingConfirm)
		assertFalse(vController.state.value.interactive)
		assertTrue(vProgress.records.isEmpty())
		vController.onCancelMove()
		assertFalse(vController.state.value.pendingConfirm)
		assertTrue(vController.state.value.interactive)
		vController.tapMove("a2", "e6")
		vController.onConfirmMove()
		vController.awaitIdle()
		assertEquals(Move.fromUci("d7d8"), vController.state.value.lastMove)
		assertTrue(vProgress.records.isEmpty())
	}

	/** Vérifie l'enchaînement automatique après une réussite et la difficulté de l'Arcade. */
	@Test
	fun autoNextAndDifficulty() = runBlocking<Unit> {
		val vPuzzles = FakePuzzles(listOf(kMateIn2, kShort))
		val vController = controller(vPuzzles, inPreferences = FakePreferences(autoNext = true, difficultyOffset = 200))
		vController.start()
		vController.awaitIdle()
		vController.tapMove("a2", "e6")
		vController.tapMove("f7", "f8")
		vController.awaitIdle()
		assertEquals("00sO1", vController.state.value.puzzleId, "puzzle suivant sans appui")
		assertEquals(2, vPuzzles.ratings.size)
		assertEquals(1700.0, vPuzzles.ratings[0], "classement 1 500 + 200 (plus dur)")
		assertTrue(vPuzzles.ratings[1] > 1700.0, "le classement a monté après la réussite")
	}

	/** Vérifie le puzzle du jour : pas d'enchaînement, calendrier à jour, choix d'un autre jour, Arcade intacte. */
	@Test
	fun dailyPuzzle() = runBlocking<Unit> {
		val vProgress = FakeProgress()
		val vSessions = FakeSessions()
		val vDaily = FakeDaily(mapOf("20261124" to kMateIn2, "20261120" to kShort), vProgress)
		val vController = controller(
			FakePuzzles(listOf(kMateIn2, kShort)), vProgress, vSessions, GameMode.Daily("20261124", "00sHx"),
			inPreferences = FakePreferences(autoNext = true), inDaily = vDaily,
		)
		vController.start()
		vController.awaitIdle()
		assertEquals("20261124", vController.state.value.daily?.day)
		assertTrue(vController.state.value.daily?.solvedDays.orEmpty().isEmpty())
		assertFalse(vController.state.value.skipEnabled)
		vController.tapMove("a2", "e6")
		vController.tapMove("f7", "f8")
		vController.awaitIdle()
		assertIs<GameHeader.Finished>(vController.state.value.header, "pas d'enchaînement automatique")
		assertEquals("00sHx", vController.state.value.puzzleId)
		assertEquals(setOf("20261124"), vController.state.value.daily?.solvedDays)
		vController.onNext()
		vController.onSkip()
		vController.awaitIdle()
		assertEquals("00sHx", vController.state.value.puzzleId, "ni Suivant ni Passer pour le puzzle du jour")
		vController.onDailyDay("20261120")
		vController.awaitIdle()
		assertEquals("00sO1", vController.state.value.puzzleId)
		assertEquals("20261120", vController.state.value.daily?.day)
		assertNull(vSessions.saved, "le puzzle en cours de l'Arcade n'est pas touché")
	}

	/** Vérifie la Survie : difficulté croissante, une vie par erreur, fin à zéro vie, rien dans la progression. */
	@Test
	fun survival() = runBlocking<Unit> {
		val vPuzzles = FakePuzzles(mateCopies(4))
		val vProgress = FakeProgress()
		val vSessions = FakeSessions()
		val vChallenges = FakeChallenges(bestScore = 5)
		val vController = controller(vPuzzles, vProgress, vSessions, GameMode.Challenge(ChallengeKind.Survival), vChallenges)
		vController.start()
		vController.awaitIdle()
		assertEquals(ChallengeUi(ChallengeKind.Survival, 0, 3, null, null), vController.state.value.challenge)
		assertFalse(vController.state.value.hintEnabled)
		assertFalse(vController.state.value.showThemeTag, "pas de thème en Survie")
		assertFalse(vController.state.value.sessionRecap, "la Survie a son propre récapitulatif")
		vController.tapMove("a2", "e6")
		vController.tapMove("f7", "f8")
		assertEquals(1, vController.state.value.challenge?.score)
		assertEquals(listOf(1200, 1225), vPuzzles.centers)
		vController.tapMove("f7", "f6")
		assertEquals(2, vController.state.value.challenge?.lives)
		assertEquals(3, vPuzzles.centers.size)
		vController.tapMove("f7", "f6")
		vController.tapMove("f7", "f6")
		val vState = vController.state.value
		assertEquals(GameHeader.ChallengeOver(ChallengeEnd.NoLives), vState.header)
		assertEquals(ChallengeResult(ChallengeEnd.NoLives, 1, 5, false, 1760, failed = 3, averageDurationMs = 0L), vState.challenge?.result)
		assertEquals(Move.fromUci("a2e6"), vState.lastMove)
		assertFalse(vState.interactive)
		assertEquals(listOf(ChallengeKind.Survival to 1), vChallenges.saved)
		assertEquals(
			listOf(Triple("m1", PuzzleResult.Success, false), Triple("m2", PuzzleResult.Failure, false), Triple("m3", PuzzleResult.Failure, false), Triple("m4", PuzzleResult.Failure, false)),
			vProgress.records,
			"chaque puzzle compte dans les statistiques, sans effet sur le classement",
		)
		assertEquals(listOf<Int?>(null, 1, 1, 1), vProgress.details.map { it.second }, "coup de l'échec")
		assertNull(vSessions.saved)
	}

	/** Vérifie le Rush : une erreur passe au problème suivant sans vie perdue, fin au bout du temps. */
	@Test
	fun rush() = runBlocking<Unit> {
		val vPuzzles = FakePuzzles(mateCopies(3))
		val vChallenges = FakeChallenges()
		val vProgress = FakeProgress()
		val vController = controller(vPuzzles, vProgress, inMode = GameMode.Challenge(ChallengeKind.Rush), inChallenges = vChallenges, inRushDurationMs = 300)
		vController.start()
		vController.awaitIdle()
		assertEquals(342L, vController.state.value.challenge?.endsAt)
		vController.tapMove("f7", "f6")
		assertEquals(listOf(1500, 1500), vPuzzles.centers)
		assertEquals(3, vController.state.value.challenge?.lives)
		vController.tapMove("a2", "e6")
		vController.tapMove("f7", "f8")
		assertEquals(1, vController.state.value.challenge?.score)
		delay(500)
		vController.awaitIdle()
		assertEquals(GameHeader.ChallengeOver(ChallengeEnd.TimeUp), vController.state.value.header)
		assertEquals(ChallengeResult(ChallengeEnd.TimeUp, 1, 1, true, 1760, failed = 1, averageDurationMs = 0L), vController.state.value.challenge?.result)
		assertEquals(listOf(ChallengeKind.Rush to 1), vChallenges.saved)
		assertEquals(listOf(Triple("m1", PuzzleResult.Failure, false), Triple("m2", PuzzleResult.Success, false)), vProgress.records)
	}

	/** Vérifie l'abandon sans score (non enregistré) et la nouvelle partie. */
	@Test
	fun abandonAndPlayAgain() = runBlocking<Unit> {
		val vChallenges = FakeChallenges()
		val vController = controller(FakePuzzles(mateCopies(2)), inMode = GameMode.Challenge(ChallengeKind.Survival), inChallenges = vChallenges)
		vController.start()
		vController.awaitIdle()
		vController.onAbandon()
		vController.awaitIdle()
		assertEquals(GameHeader.ChallengeOver(ChallengeEnd.Abandoned), vController.state.value.header)
		assertTrue(vChallenges.saved.isEmpty())
		vController.onPlayAgain()
		vController.awaitIdle()
		assertEquals(ChallengeUi(ChallengeKind.Survival, 0, 3, null, null), vController.state.value.challenge)
		assertTrue(vController.state.value.interactive)
	}

	/** Vérifie le glisser-déposer et la promotion avec choix de la pièce. */
	@Test
	fun dragAndPromotion() = runBlocking<Unit> {
		val vController = controller(FakePuzzles(listOf(kPromotion)))
		vController.start()
		vController.awaitIdle()
		vController.onDragStart(sq("b7"))
		vController.onDrop(sq("b7"), sq("b8"))
		val vPromotion = assertNotNull(vController.state.value.promotion)
		assertEquals(Color.White, vPromotion.color)
		assertFalse(vController.state.value.interactive)
		vController.onPromotionChosen(PieceType.Queen)
		vController.awaitIdle()
		assertIs<GameHeader.Finished>(vController.state.value.header)
		assertEquals(PuzzleResult.Success, vController.state.value.summary?.result)
	}

	/** Vérifie la reprise exacte d'un problème sauvegardé au milieu de la solution. */
	@Test
	fun restoreSavedSession() = runBlocking<Unit> {
		val vSessions = FakeSessions(SavedSession(PuzzleSnapshot("00sHx", 3, null, 1, 0), rated = true, resultRecorded = true, startedAt = 7L))
		val vController = controller(FakePuzzles(listOf(kMateIn2)), inSessions = vSessions)
		vController.start()
		vController.awaitIdle()
		val vState = vController.state.value
		assertEquals(GameHeader.ToMove(Color.White, afterCorrect = true), vState.header)
		assertEquals(Move.fromUci("d7d8"), vState.lastMove)
		assertTrue(vState.interactive)
		assertNotNull(vState.pieces[sq("e6")])
	}

	/** Vérifie le passage au problème suivant et un problème déjà joué, non noté. */
	@Test
	fun nextAndUnratedReplay() = runBlocking<Unit> {
		val vProgress = FakeProgress()
		val vController = controller(FakePuzzles(listOf(kShort, kMateIn2), played = mutableSetOf("00sHx")), vProgress)
		vController.start()
		vController.awaitIdle()
		vController.onNext()
		vController.awaitIdle()
		assertEquals(Move.fromUci("b8c7"), vController.state.value.lastMove, "Suivant est ignoré tant que le problème n'est pas terminé")
		vController.tapMove("e1", "a5")
		vController.tapMove("f1", "d1")
		vController.onNext()
		vController.awaitIdle()
		assertEquals(Move.fromUci("e8d7"), vController.state.value.lastMove)
		vController.tapMove("a2", "e6")
		vController.tapMove("f7", "f8")
		assertEquals(Triple("00sHx", PuzzleResult.Success, false), vProgress.records.last())
		assertEquals(false, vController.state.value.summary?.rated)
	}

	/** Vérifie que recommencer revient au départ et rejoue le coup de l'adversaire. */
	@Test
	fun restart() = runBlocking<Unit> {
		val vController = controller(FakePuzzles(listOf(kMateIn2)))
		vController.start()
		vController.awaitIdle()
		vController.tapMove("a2", "e6")
		vController.onRestart()
		vController.awaitIdle()
		assertEquals(Move.fromUci("e8d7"), vController.state.value.lastMove)
		assertNotNull(vController.state.value.pieces[sq("a2")])
		assertTrue(vController.state.value.interactive)
	}

	/** Vérifie qu'un entraînement ciblé ignore le problème sauvegardé et transmet ses critères. */
	@Test
	fun targetedModeStartsFresh() = runBlocking<Unit> {
		val vCriteria = TrainingCriteria(ThemeSet.parse("fork"), Difficulty.Harder)
		val vPuzzles = FakePuzzles(listOf(kShort, kMateIn2))
		val vSessions = FakeSessions(SavedSession(PuzzleSnapshot("00sHx", 1, null, 0, 0), rated = true, resultRecorded = false, startedAt = 1L))
		val vController = controller(vPuzzles, inSessions = vSessions, inMode = GameMode.Targeted(vCriteria))
		vController.start()
		vController.awaitIdle()
		assertEquals(Move.fromUci("b8c7"), vController.state.value.lastMove)
		assertEquals(listOf<GameMode>(GameMode.Targeted(vCriteria)), vPuzzles.modes)
		assertTrue(vController.state.value.showThemeTag, "thème affiché en Entraînement")
		assertTrue(vController.state.value.sessionRecap, "l'Entraînement demande aussi confirmation et récapitule la session")
		assertFalse(vController.state.value.keepsPuzzle, "l'Entraînement ne garde pas le puzzle en cours")
	}

	/** Vérifie la Révision : le mode est transmis à la source, récapitulatif de session, message quand tout est révisé. */
	@Test
	fun reviewMode() = runBlocking<Unit> {
		val vPuzzles = FakePuzzles(listOf(kMateIn2))
		val vController = controller(vPuzzles, inMode = GameMode.Review)
		vController.start()
		vController.awaitIdle()
		assertEquals(listOf<GameMode>(GameMode.Review), vPuzzles.modes)
		assertTrue(vController.state.value.isReview)
		assertTrue(vController.state.value.sessionRecap)
		assertFalse(vController.state.value.keepsPuzzle, "la Révision ne touche pas au puzzle en cours de l'Arcade")
		val vEmpty = controller(FakePuzzles(emptyList()), inMode = GameMode.Review)
		vEmpty.start()
		vEmpty.awaitIdle()
		assertEquals(GameHeader.NoPuzzle, vEmpty.state.value.header)
	}

	/** Vérifie que le rejeu charge le problème demandé, sans effet sur la cote. */
	@Test
	fun replayModeIsUnrated() = runBlocking<Unit> {
		val vProgress = FakeProgress()
		val vController = controller(FakePuzzles(listOf(kShort, kMateIn2)), vProgress, inMode = GameMode.Replay("00sHx"))
		vController.start()
		vController.awaitIdle()
		assertEquals(Move.fromUci("e8d7"), vController.state.value.lastMove)
		vController.tapMove("a2", "e6")
		vController.tapMove("f7", "f8")
		assertEquals(Triple("00sHx", PuzzleResult.Success, false), vProgress.records.single())
		assertEquals(1500.0, vProgress.rating.rating)
	}

	/** Vérifie les tranches de cote des difficultés relatives. */
	@Test
	fun difficultyRanges() {
		assertEquals(1100..1400, TrainingCriteria(difficulty = Difficulty.Easier).ratingRange(1500))
		assertNull(TrainingCriteria(difficulty = Difficulty.Level).ratingRange(1500))
		assertEquals(1600..1900, TrainingCriteria(difficulty = Difficulty.Harder).ratingRange(1500))
		assertEquals(800..1000, TrainingCriteria(difficulty = Difficulty.Range, range = 800..1000).ratingRange(1500))
	}

	/** Vérifie l'enregistrement de la durée et du coup de l'échec. */
	@Test
	fun durationAndFailedMove() = runBlocking<Unit> {
		var vNow = 1_000L
		val vProgress = FakeProgress()
		val vController = GameController(this, GameMode.Continuous, defaultChessRules(), FakePuzzles(listOf(kMateIn2, kShort)), vProgress, FakeSessions(), FakeChallenges(), clock = { vNow }, opponentDelayMs = 0, startDelayMs = 0, wrongMoveDelayMs = 0)
		vController.start()
		vController.awaitIdle()
		vNow += 4_000
		vController.tapMove("a2", "e6")
		vController.tapMove("f7", "f6")
		assertEquals(4_000L to 2, vProgress.details.single(), "échec au 2e coup, 4 s après le premier coup possible")
		vController.tapMove("f7", "f8")
		vController.onNext()
		vController.awaitIdle()
		vNow += 7_000
		vController.tapMove("e1", "a5")
		vController.tapMove("f1", "d1")
		assertEquals(7_000L to null, vProgress.details.last(), "réussite : durée, pas de coup d'échec")
	}
}
