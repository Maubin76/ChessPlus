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

		/** Premier problème différent de l'exclu. */
		override suspend fun next(inRating: Double, inExcludeId: String?, inMode: GameMode): Puzzle? {
			modes.add(inMode)
			return list.firstOrNull { it.id != inExcludeId }
		}

		/** Nouveau si jamais joué. */
		override suspend fun isNew(inId: String): Boolean = inId !in played
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
	) = GameController(this, inMode, defaultChessRules(), inPuzzles, inProgress, inSessions, clock = { 42L }, opponentDelayMs = 0, startDelayMs = 0, wrongMoveDelayMs = 0)

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
		val vController = GameController(this, GameMode.Continuous, defaultChessRules(), FakePuzzles(listOf(kMateIn2, kShort)), vProgress, FakeSessions(), clock = { vNow }, opponentDelayMs = 0, startDelayMs = 0, wrongMoveDelayMs = 0)
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
