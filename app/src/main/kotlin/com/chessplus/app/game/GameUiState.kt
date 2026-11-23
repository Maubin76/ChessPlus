package com.chessplus.app.game

import com.chessplus.core.model.Color
import com.chessplus.core.model.Move
import com.chessplus.core.model.Piece
import com.chessplus.core.model.PieceType
import com.chessplus.core.model.Square
import com.chessplus.core.model.ThemeSet
import com.chessplus.core.puzzle.PuzzleResult

/**
 * Message affiché au-dessus de l'échiquier (textes dans docs/DESIGN.md).
 */
sealed interface GameHeader {
	/** Chargement du premier problème. */
	data object Loading : GameHeader

	/** Le joueur doit jouer. [afterCorrect] : l'adversaire vient de répondre à un bon coup. */
	data class ToMove(val color: Color, val afterCorrect: Boolean) : GameHeader

	/** Bon coup, l'adversaire va répondre. */
	data class Correct(val opponent: Color) : GameHeader

	/** Mauvais coup, non joué. */
	data object Wrong : GameHeader

	/** Premier appui sur « Indice » : la pièce à jouer, de type [piece], est en surbrillance. */
	data class HintPiece(val piece: PieceType) : GameHeader

	/** Second appui sur « Indice » : coup de la solution joué ; l'adversaire va répondre. */
	data class Hint(val move: Move, val opponent: Color) : GameHeader

	/** Problème terminé. [mate] : la position finale est un mat ; [hintsUsed] : au moins un indice utilisé. */
	data class Finished(val success: Boolean, val mate: Boolean, val hintsUsed: Boolean) : GameHeader

	/** Aucun problème disponible. */
	data object NoPuzzle : GameHeader

	/** Défi : problème réussi, le suivant arrive. */
	data object Solved : GameHeader

	/** Défi (Survie) : mauvais coup ; [move] est le coup qu'il fallait jouer. */
	data class Missed(val move: Move) : GameHeader

	/** Défi : partie terminée pour la raison [end]. */
	data class ChallengeOver(val end: ChallengeEnd) : GameHeader
}

/**
 * Bilan d'une partie de défi.
 *
 * @property end raison de la fin.
 * @property score problèmes réussis.
 * @property best meilleur score, partie comprise.
 * @property newRecord vrai si la partie bat le record précédent.
 * @property bestPuzzleRating cote du problème le plus difficile réussi, ou null.
 */
data class ChallengeResult(val end: ChallengeEnd, val score: Int, val best: Int, val newRecord: Boolean, val bestPuzzleRating: Int?)

/**
 * État d'une partie de défi affiché à l'écran.
 *
 * @property kind défi.
 * @property score problèmes réussis.
 * @property lives vies restantes (Survie).
 * @property endsAt fin du chronomètre (Rush, millisecondes), ou null s'il n'a pas démarré.
 * @property result bilan, une fois la partie terminée.
 */
data class ChallengeUi(val kind: ChallengeKind, val score: Int, val lives: Int, val endsAt: Long?, val result: ChallengeResult?)

/**
 * Pastille posée sur une case.
 */
enum class BadgeKind {
	Success,
	Error,
}

/**
 * Promotion en attente du choix de la pièce.
 *
 * @property from case de départ du pion.
 * @property to case d'arrivée.
 * @property color couleur du pion.
 */
data class PendingPromotion(val from: Square, val to: Square, val color: Color)

/**
 * Bilan affiché à la fin d'un problème.
 *
 * @property result résultat pris en compte.
 * @property mistakes nombre d'erreurs.
 * @property revealed nombre d'indices utilisés (un par coup au plus).
 * @property ratingBefore cote du joueur avant le problème.
 * @property ratingAfter cote du joueur après le problème.
 * @property rated vrai si le problème a modifié la cote.
 * @property puzzleRating cote du problème.
 * @property themes thèmes du problème.
 */
data class GameSummary(
	val result: PuzzleResult,
	val mistakes: Int,
	val revealed: Int,
	val ratingBefore: Double,
	val ratingAfter: Double,
	val rated: Boolean,
	val puzzleRating: Int,
	val themes: ThemeSet,
)

/**
 * Retour haptique ponctuel demandé à l'interface.
 *
 * @property id identifiant croissant, pour ne déclencher chaque retour qu'une fois.
 * @property success vrai pour un bon coup ou un problème réussi, faux pour une erreur.
 */
data class HapticEvent(val id: Int, val success: Boolean)

/**
 * État complet de l'écran de jeu.
 *
 * @property header message au-dessus de l'échiquier.
 * @property pieces pièces de la position affichée.
 * @property orientation camp affiché en bas (celui du joueur).
 * @property lastMove dernier coup joué, surligné.
 * @property moveId identifiant du dernier coup, qui déclenche son animation.
 * @property animateMove vrai si le dernier coup doit être animé (faux après un glisser-déposer).
 * @property selected case sélectionnée par le joueur.
 * @property hint case de la pièce à jouer montrée par l'indice, ou null.
 * @property targets cases d'arrivée possibles de la pièce sélectionnée.
 * @property badge pastille et sa case.
 * @property interactive vrai si le joueur peut jouer.
 * @property rating cote du joueur, arrondie.
 * @property hintEnabled vrai si « Indice » est disponible.
 * @property nextEnabled vrai si « Suivant » est disponible.
 * @property promotion promotion en attente.
 * @property summary bilan de fin de problème, affiché sous l'échiquier.
 * @property haptic dernier retour haptique demandé.
 * @property challenge partie de défi en cours, ou null hors défi.
 */
data class GameUiState(
	val header: GameHeader = GameHeader.Loading,
	val pieces: Map<Square, Piece> = emptyMap(),
	val orientation: Color = Color.White,
	val lastMove: Move? = null,
	val moveId: Int = 0,
	val animateMove: Boolean = false,
	val selected: Square? = null,
	val hint: Square? = null,
	val targets: Set<Square> = emptySet(),
	val badge: Pair<Square, BadgeKind>? = null,
	val interactive: Boolean = false,
	val rating: Int = 0,
	val hintEnabled: Boolean = false,
	val nextEnabled: Boolean = false,
	val promotion: PendingPromotion? = null,
	val summary: GameSummary? = null,
	val haptic: HapticEvent? = null,
	val challenge: ChallengeUi? = null,
)
