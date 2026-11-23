package com.chessplus.core.puzzle

import com.chessplus.core.SampleData
import com.chessplus.core.model.Move
import com.chessplus.core.model.Square
import com.chessplus.core.model.ThemeSet
import com.chessplus.core.rules.ChessRules
import com.chessplus.core.rules.GameStatus
import com.chessplus.core.rules.defaultChessRules
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests du déroulement d'un problème.
 */
class PuzzleSessionTest {
	/** Règles utilisées par les sessions. */
	private val rules: ChessRules = defaultChessRules()

	/** Mat en 1 avec deux solutions : Td8# (attendu) et Te8# (mat alternatif). */
	private val kTwoMatesPuzzle = Puzzle("twoMates", "7k/p5pp/8/8/8/8/5PPP/3RR1K1 b - - 0 1", Move.listFromUci("a7a6 d1d8"), 1000, ThemeSet.kEmpty)

	/** Promotion attendue en dame. */
	private val kPromotionPuzzle = Puzzle("promo", "8/1P6/8/7k/7p/8/8/2K5 b - - 0 1", Move.listFromUci("h4h3 b7b8q"), 1000, ThemeSet.kEmpty)

	/**
	 * Joue toute la solution d'un problème sans erreur et vérifie chaque étape.
	 */
	private fun solve(inPuzzle: Puzzle): PuzzleSession {
		val vSession = PuzzleSession.start(inPuzzle, rules)
		assertEquals(inPuzzle.fen, vSession.board.fen())
		assertNull(vSession.result)
		var vIndex = 0
		while (vIndex < inPuzzle.moves.size) {
			assertEquals(PuzzleStatus.OpponentToMove, vSession.status)
			assertEquals(inPuzzle.moves[vIndex], vSession.playOpponentMove())
			assertEquals(inPuzzle.moves[vIndex], vSession.lastMove)
			assertEquals(PuzzleStatus.PlayerToMove, vSession.status)
			assertEquals(inPuzzle.playerColor, vSession.board.sideToMove)
			val vVerdict = vSession.submitMove(inPuzzle.moves[vIndex + 1])
			val vLast = vIndex + 2 == inPuzzle.moves.size
			assertEquals(if (vLast) MoveVerdict.Solved(alternativeMate = false) else MoveVerdict.Correct, vVerdict)
			vIndex += 2
		}
		assertEquals(PuzzleStatus.Solved, vSession.status)
		assertEquals(PuzzleResult.Success, vSession.result)
		return vSession
	}

	/** Vérifie le déroulement complet des quatre problèmes de l'extrait. */
	@Test
	fun solveSamplePuzzles() {
		for (vPuzzle in SampleData.kPuzzles) {
			solve(vPuzzle)
		}
	}

	/** Vérifie que la solution de 00sHx se termine par un mat. */
	@Test
	fun firstPuzzleEndsInMate() {
		val vSession = solve(SampleData.puzzle("00sHx"))
		assertEquals(GameStatus.Checkmate, vSession.board.status())
		assertEquals(Move.fromUci("f7f8"), vSession.lastMove)
	}

	/** Vérifie qu'un mauvais coup n'est pas joué, compte comme un échec et permet de réessayer. */
	@Test
	fun wrongMoveThenRetry() {
		val vPuzzle = SampleData.puzzle("00sJ9")
		val vSession = PuzzleSession.start(vPuzzle, rules)
		vSession.playOpponentMove()
		val vFen = vSession.board.fen()
		assertEquals(MoveVerdict.Wrong, vSession.submitMove(Move.fromUci("a7a6")))
		assertEquals(vFen, vSession.board.fen())
		assertEquals(PuzzleStatus.PlayerToMove, vSession.status)
		assertEquals(PuzzleResult.Failure, vSession.result)
		assertEquals(1, vSession.mistakeCount)
		// Le joueur réessaie et termine le problème : il reste compté comme échoué.
		assertEquals(MoveVerdict.Correct, vSession.submitMove(vPuzzle.moves[1]))
		while (vSession.status != PuzzleStatus.Solved) {
			vSession.playOpponentMove()
			vSession.submitMove(vSession.expectedMove()!!)
		}
		assertEquals(PuzzleResult.Failure, vSession.result)
	}

	/** Vérifie qu'un coup illégal est refusé sans compter d'erreur. */
	@Test
	fun illegalMoveIsIgnored() {
		val vSession = PuzzleSession.start(SampleData.puzzle("00sHx"), rules)
		vSession.playOpponentMove()
		assertEquals(MoveVerdict.Illegal, vSession.submitMove(Move.fromUci("a2a8")))
		assertEquals(0, vSession.mistakeCount)
		assertNull(vSession.result)
	}

	/** Vérifie le mat alternatif sur un problème réel de la base (00Mgf : d8h8# au lieu de d8g8#). */
	@Test
	fun alternativeMateOnRealPuzzle() {
		val vPuzzle = LichessCsv.parseLine(SampleData.kLine00Mgf).toPuzzle()
		val vSession = PuzzleSession.start(vPuzzle, rules)
		vSession.playOpponentMove()
		assertEquals(MoveVerdict.Correct, vSession.submitMove(vPuzzle.moves[1]))
		vSession.playOpponentMove()
		assertEquals(Move.fromUci("d8g8"), vSession.expectedMove())
		assertEquals(MoveVerdict.Solved(alternativeMate = true), vSession.submitMove(Move.fromUci("d8h8")))
		assertEquals(GameStatus.Checkmate, vSession.board.status())
		assertEquals(PuzzleResult.Success, vSession.result)
		// La solution attendue reste valable.
		solve(vPuzzle)
	}

	/** Vérifie qu'un autre coup qui donne mat est accepté, comme sur Lichess. */
	@Test
	fun alternativeMateIsAccepted() {
		val vSession = PuzzleSession.start(kTwoMatesPuzzle, rules)
		vSession.playOpponentMove()
		assertEquals(MoveVerdict.Wrong, vSession.submitMove(Move.fromUci("e1e7")))
		val vOther = PuzzleSession.start(kTwoMatesPuzzle, rules)
		vOther.playOpponentMove()
		assertEquals(MoveVerdict.Solved(alternativeMate = true), vOther.submitMove(Move.fromUci("e1e8")))
		assertEquals(PuzzleStatus.Solved, vOther.status)
		assertEquals(PuzzleResult.Success, vOther.result)
		assertEquals(Move.fromUci("e1e8"), vOther.alternativeMate)
		assertEquals(GameStatus.Checkmate, vOther.board.status())
	}

	/** Vérifie la détection des promotions et l'exigence de la bonne pièce. */
	@Test
	fun promotionMustMatchSolution() {
		val vSession = PuzzleSession.start(kPromotionPuzzle, rules)
		vSession.playOpponentMove()
		assertTrue(vSession.isPromotion(Square.fromAlgebraic("b7"), Square.fromAlgebraic("b8")))
		assertFalse(vSession.isPromotion(Square.fromAlgebraic("c1"), Square.fromAlgebraic("c2")))
		assertEquals(MoveVerdict.Wrong, vSession.submitMove(Move.fromUci("b7b8n")))
		assertEquals(MoveVerdict.Solved(alternativeMate = false), vSession.submitMove(Move.fromUci("b7b8q")))
	}

	/** Vérifie la solution coup par coup : le problème est compté comme échoué. */
	@Test
	fun revealSolutionStepByStep() {
		val vPuzzle = SampleData.puzzle("00sJb")
		val vSession = PuzzleSession.start(vPuzzle, rules)
		vSession.playOpponentMove()
		assertEquals(vPuzzle.moves[1], vSession.expectedMove())
		assertEquals(vPuzzle.moves[1], vSession.revealNextMove())
		assertEquals(PuzzleResult.Failure, vSession.result)
		assertEquals(PuzzleStatus.OpponentToMove, vSession.status)
		assertNull(vSession.expectedMove())
		while (vSession.status != PuzzleStatus.Solved) {
			vSession.playOpponentMove()
			vSession.revealNextMove()
		}
		assertEquals(3, vSession.revealedCount)
		assertEquals(PuzzleResult.Failure, vSession.result)
	}

	/** Vérifie l'indice en deux temps : la pièce, puis le coup, comptés comme un seul indice. */
	@Test
	fun hintPieceThenMove() {
		val vPuzzle = SampleData.puzzle("00sJb")
		val vSession = PuzzleSession.start(vPuzzle, rules)
		vSession.playOpponentMove()
		assertNull(vSession.hintedSquare)
		assertEquals(vPuzzle.moves[1].from, vSession.hintPiece())
		assertEquals(vPuzzle.moves[1].from, vSession.hintedSquare)
		assertEquals(PuzzleResult.Failure, vSession.result)
		assertEquals(PuzzleStatus.PlayerToMove, vSession.status)
		assertEquals(1, vSession.solutionPly)
		vSession.hintPiece()
		assertEquals(1, vSession.revealedCount)
		assertEquals(vPuzzle.moves[1], vSession.revealNextMove())
		assertEquals(1, vSession.revealedCount)
		assertNull(vSession.hintedSquare)
		vSession.playOpponentMove()
		assertNull(vSession.hintedSquare)
		vSession.hintPiece()
		assertEquals(2, vSession.revealedCount)
		vSession.restart()
		vSession.playOpponentMove()
		assertNull(vSession.hintedSquare)
	}

	/** Vérifie que recommencer revient à la FEN de départ en gardant l'échec. */
	@Test
	fun restartKeepsFailure() {
		val vPuzzle = SampleData.puzzle("00sO1")
		val vSession = PuzzleSession.start(vPuzzle, rules)
		vSession.playOpponentMove()
		vSession.revealNextMove()
		vSession.restart()
		assertEquals(vPuzzle.fen, vSession.board.fen())
		assertEquals(0, vSession.solutionPly)
		assertNull(vSession.lastMove)
		assertEquals(PuzzleStatus.OpponentToMove, vSession.status)
		assertEquals(PuzzleResult.Failure, vSession.result)
	}

	/** Vérifie qu'on ne peut pas jouer hors de son tour. */
	@Test
	fun turnOrderIsEnforced() {
		val vSession = PuzzleSession.start(SampleData.puzzle("00sHx"), rules)
		assertFailsWith<IllegalStateException> { vSession.submitMove(Move.fromUci("a2e6")) }
		assertFailsWith<IllegalStateException> { vSession.revealNextMove() }
		assertFailsWith<IllegalStateException> { vSession.hintPiece() }
		vSession.playOpponentMove()
		assertFailsWith<IllegalStateException> { vSession.playOpponentMove() }
	}

	/** Vérifie la reprise exacte d'une session à partir de son état sérialisé. */
	@Test
	fun snapshotAndRestore() {
		val vPuzzle = SampleData.puzzle("00sJ9")
		val vSession = PuzzleSession.start(vPuzzle, rules)
		vSession.playOpponentMove()
		vSession.submitMove(Move.fromUci("a7a6"))
		vSession.submitMove(vPuzzle.moves[1])
		vSession.playOpponentMove()
		val vSnapshot = vSession.snapshot()
		assertEquals(PuzzleSnapshot("00sJ9", 3, null, 1, 0), vSnapshot)

		val vRestored = PuzzleSession.restore(vPuzzle, rules, vSnapshot)
		assertEquals(vSession.board.fen(), vRestored.board.fen())
		assertEquals(vSession.status, vRestored.status)
		assertEquals(vSession.lastMove, vRestored.lastMove)
		assertEquals(PuzzleResult.Failure, vRestored.result)
		assertEquals(vSnapshot, vRestored.snapshot())

		assertFailsWith<IllegalArgumentException> { PuzzleSession.restore(SampleData.puzzle("00sHx"), rules, vSnapshot) }
		assertFailsWith<IllegalArgumentException> { PuzzleSession.restore(vPuzzle, rules, vSnapshot.copy(solutionPly = 9)) }
	}

	/** Vérifie la reprise d'un problème terminé par un mat alternatif. */
	@Test
	fun restoreAlternativeMate() {
		val vSession = PuzzleSession.start(kTwoMatesPuzzle, rules)
		vSession.playOpponentMove()
		vSession.submitMove(Move.fromUci("e1e8"))
		val vRestored = PuzzleSession.restore(kTwoMatesPuzzle, rules, vSession.snapshot())
		assertEquals(PuzzleStatus.Solved, vRestored.status)
		assertEquals(vSession.board.fen(), vRestored.board.fen())
		assertFailsWith<IllegalArgumentException> {
			PuzzleSession.restore(kTwoMatesPuzzle, rules, vSession.snapshot().copy(alternativeMateUci = "e1e7"))
		}
	}
}
