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
 * Rôle d'un élément de la notation des coups.
 */
enum class NotationKind {
	/** Coup déjà joué (atténué). */
	Past,

	/** Numéro du coup à trouver. */
	Current,

	/** Coup à trouver (« ? » souligné). */
	Pending,
}

/**
 * Élément de la notation affichée sous l'échiquier (« 8… », « Bg4 », « 9. », « ? »).
 *
 * @property text texte.
 * @property kind rôle.
 */
data class NotationToken(val text: String, val kind: NotationKind)

/**
 * Bilan de la session de jeu (ou de la partie de défi).
 *
 * @property solved puzzles réussis.
 * @property failed puzzles ratés.
 * @property ratingDelta variation du classement, ou null en défi.
 */
data class SessionTally(val solved: Int, val failed: Int, val ratingDelta: Int?)

/**
 * Puzzle du jour en cours et calendrier des jours résolus.
 *
 * @property day jour du puzzle affiché (« yyyyMMdd »).
 * @property today aujourd'hui (« yyyyMMdd »).
 * @property solvedDays jours dont le puzzle a été réussi.
 */
data class DailyUi(val day: String, val today: String, val solvedDays: Set<String>)

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
 * @property sideToMove camp du joueur, au trait.
 * @property puzzleId identifiant Lichess du puzzle, ou null.
 * @property puzzleRating classement du puzzle, ou null.
 * @property puzzleThemes thèmes du puzzle.
 * @property notation coups joués et coup à trouver.
 * @property remainingMoves coups du joueur restant à trouver.
 * @property clockStart début du chronomètre du puzzle, ou null.
 * @property clockEnd fin du chronomètre (puzzle terminé), ou null.
 * @property tally bilan de la session.
 * @property pendingConfirm vrai si un coup attend d'être confirmé.
 * @property solutionEnabled vrai si « Solution » est disponible.
 * @property skipEnabled vrai si « Passer » est disponible.
 * @property challengeBest record du défi, ou null.
 * @property daily puzzle du jour et calendrier, ou null hors puzzle du jour.
 * @property showThemeTag vrai si le thème principal est affiché pendant le puzzle (Entraînement seulement).
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
	val sideToMove: Color = Color.White,
	val puzzleId: String? = null,
	val puzzleRating: Int? = null,
	val puzzleThemes: ThemeSet = ThemeSet.kEmpty,
	val notation: List<NotationToken> = emptyList(),
	val remainingMoves: Int = 0,
	val clockStart: Long? = null,
	val clockEnd: Long? = null,
	val tally: SessionTally = SessionTally(0, 0, 0),
	val pendingConfirm: Boolean = false,
	val solutionEnabled: Boolean = false,
	val skipEnabled: Boolean = false,
	val challengeBest: Int? = null,
	val daily: DailyUi? = null,
	val showThemeTag: Boolean = false,
)
