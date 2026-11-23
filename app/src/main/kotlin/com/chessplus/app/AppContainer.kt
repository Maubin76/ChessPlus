package com.chessplus.app

import android.content.Context
import com.chessplus.app.game.GameMode
import com.chessplus.app.game.ProgressStore
import com.chessplus.app.game.PuzzleProvider
import com.chessplus.app.game.RecordedResult
import com.chessplus.app.game.SavedSession
import com.chessplus.app.game.SessionStore
import com.chessplus.app.settings.SettingsStore
import com.chessplus.core.puzzle.Puzzle
import com.chessplus.core.puzzle.PuzzleResult
import com.chessplus.core.puzzle.PuzzleSnapshot
import com.chessplus.core.rating.Glicko2RatingSystem
import com.chessplus.core.rating.PlayerRating
import com.chessplus.core.rules.ChessRules
import com.chessplus.core.rules.defaultChessRules
import com.chessplus.data.createPuzzleDatabase
import com.chessplus.data.createUserDatabase
import com.chessplus.data.installPuzzleDatabase
import com.chessplus.data.puzzle.PuzzleDatabase
import com.chessplus.data.selection.PuzzleSelector
import com.chessplus.data.selection.SelectionCriteria
import com.chessplus.data.user.ActiveSessionEntity
import com.chessplus.data.user.PlayerEntity
import com.chessplus.data.user.ProgressRepository
import com.chessplus.data.user.UserDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/**
 * Dépendances de l'application, créées une fois pour toutes (injection manuelle).
 *
 * Les bases sont ouvertes à la première utilisation, après l'installation de la base des
 * problèmes depuis les assets (copie de ~37 Mo au premier lancement).
 *
 * @property context contexte de l'application.
 */
class AppContainer(private val context: Context) {
	/** Règles des échecs. */
	val rules: ChessRules = defaultChessRules()

	/** Algorithme de cote retenu (docs/DESIGN.md). */
	val ratingSystem = Glicko2RatingSystem()

	/** Réglages de l'application. */
	val settingsStore = SettingsStore(context)

	/** Protège l'ouverture des bases. */
	private val openMutex = Mutex()

	/** Base des problèmes, ouverte par [ready]. */
	private var puzzleDatabase: PuzzleDatabase? = null

	/** Base utilisateur, ouverte par [ready]. */
	private var userDatabase: UserDatabase? = null

	/**
	 * Installe et ouvre les bases si besoin, puis retourne la paire (problèmes, utilisateur).
	 */
	suspend fun ready(): Pair<PuzzleDatabase, UserDatabase> = openMutex.withLock {
		val vPuzzles = puzzleDatabase ?: withContext(Dispatchers.IO) {
			installPuzzleDatabase(context)
			createPuzzleDatabase(context)
		}.also { puzzleDatabase = it }
		val vUser = userDatabase ?: createUserDatabase(context).also { userDatabase = it }
		vPuzzles to vUser
	}

	/** Source des problèmes. */
	val puzzleProvider: PuzzleProvider = object : PuzzleProvider {
		/** Lit le problème dans la base des problèmes. */
		override suspend fun byId(inId: String): Puzzle? = ready().first.puzzleDao().getById(inId)?.toPuzzle()

		/**
		 * Tire le prochain problème : autour de la cote (continu), selon les critères (ciblé), ou
		 * le prochain problème jamais réussi de l'historique (rejeu).
		 */
		override suspend fun next(inRating: Double, inExcludeId: String?, inMode: GameMode): Puzzle? {
			val (vPuzzles, vUser) = ready()
			if (inMode is GameMode.Replay) {
				val vId = vUser.userDao().getUnsolvedIds(kReplayCandidates).firstOrNull { it != inExcludeId } ?: return null
				return vPuzzles.puzzleDao().getById(vId)?.toPuzzle()
			}
			val vCriteria = when (inMode) {
				is GameMode.Targeted -> SelectionCriteria(inMode.criteria.themes, inMode.criteria.ratingRange(inRating.roundToInt()))
				else -> SelectionCriteria()
			}
			return PuzzleSelector(vPuzzles.puzzleDao(), vUser.userDao()).nextPuzzle(inRating, vCriteria, inExcludeId, System.currentTimeMillis())
		}

		/** Nouveau si la base utilisateur n'a aucun bilan pour ce problème. */
		override suspend fun isNew(inId: String): Boolean = ready().second.userDao().getState(inId) == null
	}

	/** Enregistrement de la progression. */
	val progressStore: ProgressStore = object : ProgressStore {
		/** Lit la cote du joueur. */
		override suspend fun currentRating(): PlayerRating = ProgressRepository(ready().second, ratingSystem).currentRating()

		/** Enregistre le résultat et retourne les cotes avant et après. */
		override suspend fun record(
			inPuzzle: Puzzle,
			inResult: PuzzleResult,
			inMistakes: Int,
			inRevealed: Int,
			inRated: Boolean,
			inDurationMs: Long?,
			inFailedAtMove: Int?,
		): RecordedResult {
			val vRepository = ProgressRepository(ready().second, ratingSystem)
			val vBefore = vRepository.currentRating().rating
			val vAfter = vRepository.recordResult(inPuzzle, inResult, inMistakes, inRevealed, inRated, System.currentTimeMillis(), inDurationMs, inFailedAtMove).rating
			return RecordedResult(inResult, vBefore, vAfter)
		}

		/** Lit la dernière tentative sur le problème. */
		override suspend fun lastResult(inPuzzleId: String): RecordedResult? =
			ready().second.userDao().getLatestAttempt(inPuzzleId)?.let { RecordedResult(it.result, it.playerRatingBefore, it.playerRatingAfter) }
	}

	/**
	 * Fixe la cote du joueur (choix du niveau) ; l'écart de cote maximal la laisse s'ajuster vite.
	 */
	suspend fun setLevel(inRating: Int) {
		val vInitial = ratingSystem.initialRating
		ready().second.userDao().upsertPlayer(PlayerEntity(rating = inRating.toDouble(), deviation = vInitial.deviation, volatility = vInitial.volatility, updatedAt = System.currentTimeMillis()))
	}

	/**
	 * Indique si le joueur a déjà une cote (sinon, premier lancement : choix du niveau).
	 */
	suspend fun hasPlayer(): Boolean = ready().second.userDao().getPlayer() != null

	/** Sauvegarde du problème en cours. */
	val sessionStore: SessionStore = object : SessionStore {
		/** Lit le problème en cours. */
		override suspend fun load(): SavedSession? = ready().second.userDao().getActiveSession()?.let {
			SavedSession(PuzzleSnapshot(it.puzzleId, it.solutionPly, it.alternativeMateUci, it.mistakeCount, it.revealedCount), it.rated, it.resultRecorded, it.startedAt)
		}

		/** Écrit le problème en cours. */
		override suspend fun save(inSession: SavedSession) {
			val vSnapshot = inSession.snapshot
			ready().second.userDao().upsertActiveSession(
				ActiveSessionEntity(
					puzzleId = vSnapshot.puzzleId,
					solutionPly = vSnapshot.solutionPly,
					alternativeMateUci = vSnapshot.alternativeMateUci,
					mistakeCount = vSnapshot.mistakeCount,
					revealedCount = vSnapshot.revealedCount,
					rated = inSession.rated,
					resultRecorded = inSession.resultRecorded,
					startedAt = inSession.startedAt,
				)
			)
		}

		/** Efface le problème en cours. */
		override suspend fun clear() = ready().second.userDao().clearActiveSession()
	}

	companion object {
		/** Nombre de problèmes non réussis examinés pour le rejeu. */
		private const val kReplayCandidates = 50
	}
}
