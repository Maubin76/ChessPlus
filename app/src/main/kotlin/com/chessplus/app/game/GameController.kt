package com.chessplus.app.game

import com.chessplus.core.model.Move
import com.chessplus.core.model.Piece
import com.chessplus.core.model.PieceType
import com.chessplus.core.model.Square
import com.chessplus.core.puzzle.MoveVerdict
import com.chessplus.core.puzzle.Puzzle
import com.chessplus.core.puzzle.PuzzleResult
import com.chessplus.core.puzzle.PuzzleSession
import com.chessplus.core.puzzle.PuzzleStatus
import com.chessplus.core.rules.ChessRules
import com.chessplus.core.rules.GameStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Logique de l'écran de jeu, indépendante d'Android : enchaînement des problèmes, coups du
 * joueur et de l'adversaire, indice, enregistrement des résultats et sauvegarde pour la reprise.
 *
 * Toutes les méthodes publiques sont appelées depuis le fil principal.
 *
 * @property scope portée des tâches (celle du ViewModel).
 * @property mode mode de l'écran (continu, ciblé, rejeu).
 * @property rules règles des échecs.
 * @property puzzles source des problèmes.
 * @property progress enregistrement des résultats et cote.
 * @property sessions sauvegarde du problème en cours.
 * @property clock horloge (millisecondes).
 * @property opponentDelayMs pause avant la réponse de l'adversaire.
 * @property startDelayMs pause avant le premier coup de l'adversaire.
 * @property wrongMoveDelayMs durée d'affichage d'un mauvais coup avant son annulation.
 */
class GameController(
	private val scope: CoroutineScope,
	private val mode: GameMode,
	private val rules: ChessRules,
	private val puzzles: PuzzleProvider,
	private val progress: ProgressStore,
	private val sessions: SessionStore,
	private val clock: () -> Long,
	private val opponentDelayMs: Long = kOpponentDelayMs,
	private val startDelayMs: Long = kStartDelayMs,
	private val wrongMoveDelayMs: Long = kWrongMoveDelayMs,
) {
	/** État publié. */
	private val mutableState = MutableStateFlow(GameUiState())

	/** État de l'écran, observé par l'interface. */
	val state: StateFlow<GameUiState> = mutableState.asStateFlow()

	/** Problème en cours. */
	private var session: PuzzleSession? = null

	/** Vrai si le problème en cours modifie la cote. */
	private var rated = false

	/** Vrai si le résultat du problème en cours est enregistré. */
	private var resultRecorded = false

	/** Résultat enregistré du problème en cours. */
	private var recorded: RecordedResult? = null

	/** Date de début du problème en cours. */
	private var startedAt = 0L

	/** Cote actuelle du joueur. */
	private var rating = 0.0

	/** Message courant. */
	private var header: GameHeader = GameHeader.Loading

	/** Case sélectionnée. */
	private var selected: Square? = null

	/** Pastille courante. */
	private var badge: Pair<Square, BadgeKind>? = null

	/** Promotion en attente. */
	private var promotion: PendingPromotion? = null

	/** Position affichée à la place de la vraie pendant qu'un mauvais coup est montré, ou null. */
	private var displayOverride: Map<Square, Piece>? = null

	/** Compteur des coups affichés (déclenche les animations). */
	private var moveId = 0

	/** Animation du dernier coup. */
	private var animate = false

	/** Dernier retour haptique. */
	private var haptic: HapticEvent? = null

	/** Coup de l'adversaire programmé, ou chargement en cours. */
	private var pending: Job? = null

	/** Vrai pendant un chargement ou en attendant le coup de l'adversaire : le joueur ne peut pas jouer. */
	private var busy = false

	/** Tâches d'enregistrement en cours. */
	private val persistJobs = mutableListOf<Job>()

	/** Sérialise les écritures (résultat puis sauvegarde). */
	private val persistMutex = Mutex()

	/**
	 * Démarre l'écran : en entraînement continu, reprend le problème sauvegardé s'il existe ;
	 * sinon (ou dans les autres modes) charge un nouveau problème.
	 */
	fun start() {
		busy = true
		pending = scope.launch {
			rating = progress.currentRating().rating
			val vRestored = mode == GameMode.Continuous && restore()
			if (!vRestored) {
				val vFirst = (mode as? GameMode.Replay)?.let { puzzles.byId(it.puzzleId) }
				if (vFirst != null) {
					load(vFirst)
				} else {
					loadNext(null)
				}
			}
		}
	}

	/**
	 * Gère un appui sur une case : sélection d'une pièce du joueur, ou coup vers une case possible.
	 */
	fun onSquareTap(inSquare: Square) {
		val vSession = session ?: return
		if (!isInteractive()) {
			return
		}
		val vFrom = selected
		if (vFrom != null && inSquare in targetsOf(vFrom)) {
			attemptMove(vFrom, inSquare, inAnimate = true)
			return
		}
		val vPiece = vSession.board.pieceAt(inSquare)
		selected = if (vPiece != null && vPiece.color == vSession.puzzle.playerColor && inSquare != vFrom) inSquare else null
		if (badge?.second == BadgeKind.Error) {
			badge = null
		}
		publish()
	}

	/**
	 * Gère le début d'un glisser : sélectionne la pièce si elle appartient au joueur.
	 */
	fun onDragStart(inSquare: Square) {
		val vSession = session ?: return
		if (!isInteractive()) {
			return
		}
		val vPiece = vSession.board.pieceAt(inSquare)
		if (vPiece != null && vPiece.color == vSession.puzzle.playerColor) {
			selected = inSquare
			if (badge?.second == BadgeKind.Error) {
				badge = null
			}
			publish()
		}
	}

	/**
	 * Gère un glisser-déposer d'une case à une autre (sans animation : la pièce est déjà sous le doigt).
	 */
	fun onDrop(inFrom: Square, inTo: Square?) {
		if (!isInteractive() || inTo == null || inTo == inFrom) {
			return
		}
		if (inTo in targetsOf(inFrom)) {
			attemptMove(inFrom, inTo, inAnimate = false)
		} else {
			selected = null
			publish()
		}
	}

	/**
	 * Termine une promotion avec la pièce choisie.
	 */
	fun onPromotionChosen(inType: PieceType) {
		val vPromotion = promotion ?: return
		promotion = null
		submit(Move(vPromotion.from, vPromotion.to, inType), inAnimate = true)
	}

	/**
	 * Annule une promotion.
	 */
	fun onPromotionDismissed() {
		promotion = null
		selected = null
		publish()
	}

	/**
	 * Montre le coup suivant de la solution (le problème compte alors comme échoué).
	 */
	fun onHint() {
		val vSession = session ?: return
		if (!isInteractive()) {
			return
		}
		val vMove = vSession.revealNextMove()
		showMove(inAnimate = true)
		badge = null
		selected = null
		if (vSession.status == PuzzleStatus.Solved) {
			finish()
		} else {
			header = GameHeader.Hint(vMove, vSession.puzzle.opponentColor)
			publish()
			persist()
			scheduleOpponent(opponentDelayMs)
		}
	}

	/**
	 * Recommence le problème depuis la position de départ (un échec reste un échec).
	 */
	fun onRestart() {
		val vSession = session ?: return
		if (header == GameHeader.Loading) {
			return
		}
		pending?.cancel()
		busy = false
		displayOverride = null
		vSession.restart()
		showMove(inAnimate = true)
		selected = null
		badge = null
		promotion = null
		header = GameHeader.ToMove(vSession.puzzle.playerColor, afterCorrect = false)
		publish()
		persist()
		scheduleOpponent(startDelayMs)
	}

	/**
	 * Passe au problème suivant (disponible quand le problème est terminé).
	 */
	fun onNext() {
		val vSession = session
		if (vSession == null && header != GameHeader.NoPuzzle) {
			return
		}
		if (vSession != null && vSession.status != PuzzleStatus.Solved) {
			return
		}
		pending?.cancel()
		busy = true
		displayOverride = null
		publish()
		pending = scope.launch { loadNext(vSession?.puzzle?.id) }
	}

	/**
	 * Attend la fin des tâches en cours (coup de l'adversaire, enregistrements). Pour les tests.
	 */
	suspend fun awaitIdle() {
		while (true) {
			val vJobs = listOfNotNull(pending) + synchronized(persistJobs) { persistJobs.toList() }
			if (vJobs.all { it.isCompleted }) {
				return
			}
			vJobs.joinAll()
		}
	}

	/**
	 * Reprend le problème sauvegardé ; retourne faux s'il n'y en a pas ou s'il est inutilisable.
	 */
	private suspend fun restore(): Boolean {
		val vSaved = sessions.load() ?: return false
		val vPuzzle = puzzles.byId(vSaved.snapshot.puzzleId) ?: return false
		val vSession = runCatching { PuzzleSession.restore(vPuzzle, rules, vSaved.snapshot) }.getOrNull() ?: return false
		busy = false
		session = vSession
		rated = vSaved.rated
		resultRecorded = vSaved.resultRecorded
		recorded = if (resultRecorded) progress.lastResult(vPuzzle.id) else null
		startedAt = vSaved.startedAt
		showMove(inAnimate = false)
		when (vSession.status) {
			PuzzleStatus.Solved -> {
				header = finishedHeader(vSession)
				publish()
			}
			PuzzleStatus.PlayerToMove -> {
				header = GameHeader.ToMove(vPuzzle.playerColor, afterCorrect = vSession.solutionPly > 1)
				publish()
			}
			PuzzleStatus.OpponentToMove -> {
				header = GameHeader.ToMove(vPuzzle.playerColor, afterCorrect = false)
				publish()
				scheduleOpponent(startDelayMs)
			}
		}
		return true
	}

	/**
	 * Charge le prochain problème du mode et programme le premier coup de l'adversaire.
	 */
	private suspend fun loadNext(inExcludeId: String?) {
		sessions.clear()
		val vPuzzle: Puzzle? = puzzles.next(rating, inExcludeId, mode)
		if (vPuzzle == null) {
			busy = false
			session = null
			header = GameHeader.NoPuzzle
			publish()
			return
		}
		load(vPuzzle)
	}

	/**
	 * Démarre un problème donné et programme le premier coup de l'adversaire.
	 */
	private suspend fun load(inPuzzle: Puzzle) {
		sessions.clear()
		val vPuzzle = inPuzzle
		busy = false
		session = PuzzleSession.start(vPuzzle, rules)
		rated = mode !is GameMode.Replay && puzzles.isNew(vPuzzle.id)
		resultRecorded = false
		recorded = null
		startedAt = clock()
		selected = null
		badge = null
		promotion = null
		displayOverride = null
		showMove(inAnimate = false)
		header = GameHeader.ToMove(vPuzzle.playerColor, afterCorrect = false)
		publish()
		persist()
		scheduleOpponent(startDelayMs)
	}

	/**
	 * Programme le coup de l'adversaire après une pause, pour laisser le temps de voir la position.
	 */
	private fun scheduleOpponent(inDelayMs: Long) {
		busy = true
		publish()
		pending = scope.launch {
			delay(inDelayMs)
			busy = false
			val vSession = session ?: return@launch
			if (vSession.status != PuzzleStatus.OpponentToMove) {
				publish()
				return@launch
			}
			vSession.playOpponentMove()
			showMove(inAnimate = true)
			if (header !is GameHeader.Wrong) {
				badge = null
			}
			header = GameHeader.ToMove(vSession.puzzle.playerColor, afterCorrect = vSession.solutionPly > 1)
			publish()
			persist()
		}
	}

	/**
	 * Joue un coup du joueur, ou ouvre le choix de la pièce pour une promotion.
	 */
	private fun attemptMove(inFrom: Square, inTo: Square, inAnimate: Boolean) {
		val vSession = session ?: return
		if (vSession.isPromotion(inFrom, inTo)) {
			promotion = PendingPromotion(inFrom, inTo, vSession.puzzle.playerColor)
			selected = inFrom
			publish()
			return
		}
		submit(Move(inFrom, inTo), inAnimate)
	}

	/**
	 * Soumet un coup à la session et met à jour l'affichage selon le verdict.
	 */
	private fun submit(inMove: Move, inAnimate: Boolean) {
		val vSession = session ?: return
		selected = null
		when (val vVerdict = vSession.submitMove(inMove)) {
			MoveVerdict.Illegal -> publish()
			MoveVerdict.Correct -> {
				showMove(inAnimate)
				badge = inMove.to to BadgeKind.Success
				header = GameHeader.Correct(vSession.puzzle.opponentColor)
				requestHaptic(true)
				publish()
				persist()
				scheduleOpponent(opponentDelayMs)
			}
			is MoveVerdict.Solved -> {
				showMove(inAnimate)
				badge = inMove.to to BadgeKind.Success
				requestHaptic(vSession.result == PuzzleResult.Success || vVerdict.alternativeMate)
				finish()
			}
			MoveVerdict.Wrong -> showWrongMove(inMove, inAnimate)
		}
	}

	/**
	 * Montre un mauvais coup : la pièce va sur la case visée avec une pastille d'erreur, puis
	 * revient à sa place. Le joueur ne peut pas jouer pendant ce temps.
	 */
	private fun showWrongMove(inMove: Move, inAnimate: Boolean) {
		val vSession = session ?: return
		val vShown = boardPieces(vSession).toMutableMap()
		vShown.remove(inMove.from)?.let { inPiece -> vShown[inMove.to] = inMove.promotion?.let { Piece(inPiece.color, it) } ?: inPiece }
		displayOverride = vShown
		showMove(inAnimate)
		badge = inMove.to to BadgeKind.Error
		header = GameHeader.Wrong
		requestHaptic(false)
		busy = true
		publish()
		persist()
		pending = scope.launch {
			delay(wrongMoveDelayMs)
			displayOverride = null
			badge = null
			showMove(inAnimate = true)
			busy = false
			publish()
		}
	}

	/**
	 * Retourne les pièces de la position réelle de la session.
	 */
	private fun boardPieces(inSession: PuzzleSession): Map<Square, Piece> =
		Square.kAll.mapNotNull { vSquare -> inSession.board.pieceAt(vSquare)?.let { vSquare to it } }.toMap()

	/**
	 * Termine le problème : message final, bilan, enregistrement.
	 */
	private fun finish() {
		val vSession = session ?: return
		header = finishedHeader(vSession)
		publish()
		persist()
	}

	/**
	 * Retourne le message final d'un problème terminé.
	 */
	private fun finishedHeader(inSession: PuzzleSession): GameHeader = GameHeader.Finished(
		success = inSession.result == PuzzleResult.Success,
		mate = inSession.board.status() == GameStatus.Checkmate,
		solutionShown = inSession.revealedCount > 0,
	)

	/**
	 * Enregistre le résultat dès qu'il est connu (une seule fois), puis sauvegarde le problème.
	 */
	private fun persist() {
		val vSession = session ?: return
		val vJob = scope.launch {
			persistMutex.withLock {
				val vResult = vSession.result
				if (!resultRecorded && vResult != null) {
					val vRecorded = progress.record(vSession.puzzle, vResult, vSession.mistakeCount, vSession.revealedCount, rated)
					recorded = vRecorded
					resultRecorded = true
					rating = vRecorded.ratingAfter
					publish()
				}
				sessions.save(SavedSession(vSession.snapshot(), rated, resultRecorded, startedAt))
			}
		}
		synchronized(persistJobs) {
			persistJobs.removeAll { it.isCompleted }
			persistJobs.add(vJob)
		}
	}

	/**
	 * Signale qu'un nouveau coup est affiché.
	 */
	private fun showMove(inAnimate: Boolean) {
		moveId++
		animate = inAnimate
	}

	/**
	 * Demande un retour haptique.
	 */
	private fun requestHaptic(inSuccess: Boolean) {
		haptic = HapticEvent((haptic?.id ?: 0) + 1, inSuccess)
	}

	/**
	 * Indique si le joueur peut jouer maintenant.
	 */
	private fun isInteractive(): Boolean {
		val vSession = session ?: return false
		return vSession.status == PuzzleStatus.PlayerToMove && promotion == null && !busy
	}

	/**
	 * Retourne les cases d'arrivée possibles d'une pièce.
	 */
	private fun targetsOf(inFrom: Square): Set<Square> = session?.board?.legalMovesFrom(inFrom)?.map { it.to }?.toSet() ?: emptySet()

	/**
	 * Publie l'état courant.
	 */
	private fun publish() {
		val vSession = session
		val vRecorded = recorded
		val vSummary = if (vSession != null && vSession.status == PuzzleStatus.Solved && vRecorded != null) {
			GameSummary(
				result = vRecorded.result,
				mistakes = vSession.mistakeCount,
				revealed = vSession.revealedCount,
				ratingBefore = vRecorded.ratingBefore,
				ratingAfter = vRecorded.ratingAfter,
				rated = rated,
				puzzleRating = vSession.puzzle.rating,
				themes = vSession.puzzle.themes,
				lichessUrl = vSession.puzzle.lichessUrl,
			)
		} else {
			null
		}
		mutableState.value = GameUiState(
			header = header,
			pieces = displayOverride ?: vSession?.let { boardPieces(it) } ?: emptyMap(),
			orientation = vSession?.puzzle?.playerColor ?: com.chessplus.core.model.Color.White,
			lastMove = vSession?.lastMove,
			moveId = moveId,
			animateMove = animate,
			selected = selected,
			targets = selected?.let { targetsOf(it) } ?: emptySet(),
			badge = badge,
			interactive = isInteractive(),
			rating = kotlin.math.round(rating).toInt(),
			hintEnabled = isInteractive(),
			nextEnabled = vSession?.status == PuzzleStatus.Solved || header == GameHeader.NoPuzzle,
			promotion = promotion,
			summary = vSummary,
			haptic = haptic,
		)
	}

	companion object {
		/** Pause avant la réponse de l'adversaire. */
		const val kOpponentDelayMs = 600L

		/** Pause avant le premier coup de l'adversaire. */
		const val kStartDelayMs = 700L

		/** Durée d'affichage d'un mauvais coup avant qu'il soit annulé. */
		const val kWrongMoveDelayMs = 900L
	}
}
