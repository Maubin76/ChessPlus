package com.chessplus.core.puzzle

import com.chessplus.core.model.Move
import com.chessplus.core.model.Square
import com.chessplus.core.rules.ChessPosition
import com.chessplus.core.rules.ChessRules
import com.chessplus.core.rules.GameStatus
import com.chessplus.core.rules.PositionView

/**
 * Étape d'un problème en cours.
 */
enum class PuzzleStatus {
	/** L'adversaire doit jouer : l'interface appelle [PuzzleSession.playOpponentMove] après son animation. */
	OpponentToMove,

	/** Le joueur doit trouver le coup suivant. */
	PlayerToMove,

	/** Le problème est terminé : dernier coup de la solution joué, ou mat alternatif. */
	Solved,
}

/**
 * Résultat d'un problème, pris en compte une seule fois pour la cote du joueur.
 */
enum class PuzzleResult {
	/** Résolu sans erreur et sans voir la solution. */
	Success,

	/** Au moins une erreur, ou solution consultée (même si le problème est terminé ensuite). */
	Failure,
}

/**
 * Réponse de la session à un coup proposé par le joueur.
 */
sealed interface MoveVerdict {
	/** Coup illégal : refusé, rien ne change et ce n'est pas compté comme une erreur. */
	data object Illegal : MoveVerdict

	/** Bon coup : joué, la réponse de l'adversaire est en attente. */
	data object Correct : MoveVerdict

	/**
	 * Bon coup qui termine le problème.
	 *
	 * @property alternativeMate vrai si le coup diffère de la solution mais donne mat.
	 */
	data class Solved(val alternativeMate: Boolean) : MoveVerdict

	/** Mauvais coup : non joué, le problème est compté comme échoué, le joueur peut réessayer. */
	data object Wrong : MoveVerdict
}

/**
 * État sérialisable d'une session, pour la reprise exacte après une interruption
 * ou la mort du processus.
 *
 * @property puzzleId identifiant du problème.
 * @property solutionPly nombre de coups de [Puzzle.moves] déjà joués sur l'échiquier.
 * @property alternativeMateUci coup de mat alternatif qui a terminé le problème, en UCI, ou null.
 * @property mistakeCount nombre de mauvais coups joués.
 * @property revealedCount nombre d'indices utilisés (un par coup du joueur au plus).
 */
data class PuzzleSnapshot(
	val puzzleId: String,
	val solutionPly: Int,
	val alternativeMateUci: String?,
	val mistakeCount: Int,
	val revealedCount: Int,
)

/**
 * Déroulement d'un problème : coups de l'adversaire, vérification des coups du joueur,
 * aide et reprise.
 *
 * La session ne joue jamais un coup de l'adversaire d'elle-même : l'interface appelle
 * [playOpponentMove] quand le statut vaut [PuzzleStatus.OpponentToMove], ce qui lui laisse
 * le temps d'animer.
 */
class PuzzleSession private constructor(
	val puzzle: Puzzle,
	private val rules: ChessRules,
) {
	/** Position courante. */
	private var position: ChessPosition = rules.positionFromFen(puzzle.fen)

	/** Nombre de coups de [Puzzle.moves] déjà joués sur l'échiquier. */
	var solutionPly: Int = 0
		private set

	/** Coup de mat alternatif qui a terminé le problème, ou null. */
	var alternativeMate: Move? = null
		private set

	/** Nombre de mauvais coups joués. */
	var mistakeCount: Int = 0
		private set

	/** Nombre d'indices utilisés : un par coup du joueur au plus (pièce montrée, puis coup joué). */
	var revealedCount: Int = 0
		private set

	/**
	 * Coup de la solution dont la pièce a été montrée, ou null. Non sauvegardé : après une reprise,
	 * l'indice repart de la pièce.
	 */
	private var hintedPly: Int? = null

	/** Case de la pièce à jouer montrée par l'indice pour le coup en cours, ou null. */
	val hintedSquare: Square?
		get() = if (hintedPly == solutionPly && status == PuzzleStatus.PlayerToMove) puzzle.moves[solutionPly].from else null

	/** Position courante, en lecture seule, pour l'affichage. */
	val board: PositionView
		get() = position

	/** Dernier coup joué sur l'échiquier, pour le surlignage, ou null au départ. */
	val lastMove: Move?
		get() = position.history.lastOrNull()

	/** Étape courante du problème. */
	val status: PuzzleStatus
		get() = when {
			alternativeMate != null || solutionPly == puzzle.moves.size -> PuzzleStatus.Solved
			solutionPly % 2 == 0 -> PuzzleStatus.OpponentToMove
			else -> PuzzleStatus.PlayerToMove
		}

	/**
	 * Résultat à prendre en compte pour la cote : [PuzzleResult.Failure] dès la première erreur
	 * ou aide, [PuzzleResult.Success] si le problème est terminé proprement, null tant que
	 * rien n'est joué.
	 */
	val result: PuzzleResult?
		get() = when {
			mistakeCount > 0 || revealedCount > 0 -> PuzzleResult.Failure
			status == PuzzleStatus.Solved -> PuzzleResult.Success
			else -> null
		}

	/**
	 * Joue le coup attendu de l'adversaire et le retourne.
	 *
	 * @throws IllegalStateException si ce n'est pas à l'adversaire de jouer.
	 */
	fun playOpponentMove(): Move {
		check(status == PuzzleStatus.OpponentToMove) { "Ce n'est pas à l'adversaire de jouer ($status)" }
		return playSolutionMove()
	}

	/**
	 * Examine un coup du joueur.
	 *
	 * Le coup attendu (promotion comprise) est joué. Un autre coup légal qui donne mat est
	 * accepté et termine le problème. Tout autre coup légal est une erreur et n'est pas joué.
	 *
	 * @throws IllegalStateException si ce n'est pas au joueur de jouer.
	 */
	fun submitMove(inMove: Move): MoveVerdict {
		check(status == PuzzleStatus.PlayerToMove) { "Ce n'est pas au joueur de jouer ($status)" }
		if (!position.isLegal(inMove)) {
			return MoveVerdict.Illegal
		}
		if (inMove == puzzle.moves[solutionPly]) {
			playSolutionMove()
			return if (status == PuzzleStatus.Solved) MoveVerdict.Solved(alternativeMate = false) else MoveVerdict.Correct
		}
		if (givesMate(inMove)) {
			position.play(inMove)
			alternativeMate = inMove
			return MoveVerdict.Solved(alternativeMate = true)
		}
		mistakeCount++
		return MoveVerdict.Wrong
	}

	/**
	 * Retourne le prochain coup attendu du joueur sans le jouer, ou null si ce n'est pas à lui.
	 */
	fun expectedMove(): Move? = if (status == PuzzleStatus.PlayerToMove) puzzle.moves[solutionPly] else null

	/**
	 * Premier niveau de l'indice : retourne la case de la pièce à jouer, sans jouer le coup.
	 * Le problème est alors compté comme échoué.
	 *
	 * @throws IllegalStateException si ce n'est pas au joueur de jouer.
	 */
	fun hintPiece(): Square {
		check(status == PuzzleStatus.PlayerToMove) { "Ce n'est pas au joueur de jouer ($status)" }
		if (hintedPly != solutionPly) {
			revealedCount++
			hintedPly = solutionPly
		}
		return puzzle.moves[solutionPly].from
	}

	/**
	 * Second niveau de l'indice : joue le prochain coup attendu du joueur et le retourne. Le problème
	 * est alors compté comme échoué ; si la pièce de ce coup a déjà été montrée, l'indice n'est pas
	 * compté une seconde fois.
	 *
	 * @throws IllegalStateException si ce n'est pas au joueur de jouer.
	 */
	fun revealNextMove(): Move {
		check(status == PuzzleStatus.PlayerToMove) { "Ce n'est pas au joueur de jouer ($status)" }
		if (hintedPly != solutionPly) {
			revealedCount++
		}
		hintedPly = null
		return playSolutionMove()
	}

	/**
	 * Recommence le problème depuis la FEN de départ. Les erreurs et aides déjà comptées
	 * sont conservées : un problème échoué le reste.
	 */
	fun restart() {
		position = rules.positionFromFen(puzzle.fen)
		solutionPly = 0
		alternativeMate = null
		hintedPly = null
	}

	/**
	 * Indique si le déplacement d'une case à une autre est une promotion légale, pour que
	 * l'interface demande la pièce choisie avant d'appeler [submitMove].
	 */
	fun isPromotion(inFrom: Square, inTo: Square): Boolean =
		position.legalMovesFrom(inFrom).any { it.to == inTo && it.promotion != null }

	/**
	 * Retourne l'état sérialisable de la session.
	 */
	fun snapshot(): PuzzleSnapshot = PuzzleSnapshot(
		puzzleId = puzzle.id,
		solutionPly = solutionPly,
		alternativeMateUci = alternativeMate?.toUci(),
		mistakeCount = mistakeCount,
		revealedCount = revealedCount,
	)

	/**
	 * Joue le coup suivant de la solution et le retourne.
	 */
	private fun playSolutionMove(): Move {
		val vMove = puzzle.moves[solutionPly]
		position.play(vMove)
		solutionPly++
		return vMove
	}

	/**
	 * Indique si un coup légal donne mat, sans modifier la position courante.
	 */
	private fun givesMate(inMove: Move): Boolean {
		position.play(inMove)
		val vMate = position.status() == GameStatus.Checkmate
		position.undo()
		return vMate
	}

	companion object {
		/**
		 * Démarre un problème : la FEN est chargée et le premier coup de l'adversaire est en attente.
		 *
		 * @throws IllegalArgumentException si la FEN est invalide.
		 */
		fun start(inPuzzle: Puzzle, inRules: ChessRules): PuzzleSession = PuzzleSession(inPuzzle, inRules)

		/**
		 * Reconstruit une session à partir de son état sérialisé, en rejouant les coups.
		 *
		 * @throws IllegalArgumentException si l'état ne correspond pas au problème.
		 */
		fun restore(inPuzzle: Puzzle, inRules: ChessRules, inSnapshot: PuzzleSnapshot): PuzzleSession {
			require(inSnapshot.puzzleId == inPuzzle.id) { "État du problème ${inSnapshot.puzzleId} appliqué au problème ${inPuzzle.id}" }
			require(inSnapshot.solutionPly in 0..inPuzzle.moves.size) { "Nombre de coups joués invalide : ${inSnapshot.solutionPly}" }
			require(inSnapshot.mistakeCount >= 0 && inSnapshot.revealedCount >= 0) { "Compteurs négatifs dans l'état" }
			val vSession = PuzzleSession(inPuzzle, inRules)
			repeat(inSnapshot.solutionPly) { vSession.playSolutionMove() }
			vSession.mistakeCount = inSnapshot.mistakeCount
			vSession.revealedCount = inSnapshot.revealedCount
			if (inSnapshot.alternativeMateUci != null) {
				val vMate = Move.fromUci(inSnapshot.alternativeMateUci)
				require(vSession.status == PuzzleStatus.PlayerToMove && vSession.position.isLegal(vMate) && vSession.givesMate(vMate)) {
					"Mat alternatif invalide dans l'état : ${inSnapshot.alternativeMateUci}"
				}
				vSession.position.play(vMate)
				vSession.alternativeMate = vMate
			}
			return vSession
		}
	}
}
