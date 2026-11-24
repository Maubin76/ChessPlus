package com.chessplus.app

import android.content.Context
import com.chessplus.app.game.ChallengeKind
import com.chessplus.app.game.ChallengeStore
import com.chessplus.app.game.DailyDays
import com.chessplus.app.game.DailyStore
import com.chessplus.app.game.GameMode
import com.chessplus.app.goal.DailyGoal
import com.chessplus.app.goal.GoalProgress
import com.chessplus.app.game.ProgressStore
import com.chessplus.app.game.PuzzleProvider
import com.chessplus.app.game.RecordedResult
import com.chessplus.app.game.SavedSession
import com.chessplus.app.game.SessionStore
import com.chessplus.app.settings.SettingsStore
import com.chessplus.app.stats.StatsCalculator
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
import com.chessplus.data.user.ChallengeRunEntity
import com.chessplus.data.user.DailyPuzzleEntity
import com.chessplus.data.user.UserBackup
import com.chessplus.data.user.PlayerEntity
import com.chessplus.data.user.ProgressRepository
import com.chessplus.data.user.UserDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import kotlin.math.roundToInt
import kotlin.random.Random

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
		val vUser = userDatabase ?: withContext(Dispatchers.IO) {
			applyPendingImport()
			createUserDatabase(context)
		}.also { userDatabase = it }
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
			if (inMode == GameMode.Review) {
				val vId = vUser.userDao().getDueReviewIds(System.currentTimeMillis(), kReviewCandidates).firstOrNull { it != inExcludeId } ?: return null
				return vPuzzles.puzzleDao().getById(vId)?.toPuzzle()
			}
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

		/** Tire un problème de défi autour de la cote visée, hors des problèmes déjà vus dans la partie. */
		override suspend fun challengeNext(inCenter: Int, inHalfWidth: Int, inIncludeSeen: Boolean, inExclude: Set<String>): Puzzle? {
			val (vPuzzles, vUser) = ready()
			return PuzzleSelector(vPuzzles.puzzleDao(), vUser.userDao())
				.nextPuzzle(inCenter.toDouble(), SelectionCriteria(initialHalfWidth = inHalfWidth, includeSeen = inIncludeSeen), null, System.currentTimeMillis(), inExclude)
		}
	}

	/**
	 * Retourne le puzzle du jour d'aujourd'hui.
	 */
	suspend fun dailyPuzzle(inNow: Long = System.currentTimeMillis()): Puzzle? = dailyStore.puzzleFor(DailyDays.keyOf(inNow))

	/** Puzzles du jour : un par date, tiré une fois (graine = date) autour du classement, puis conservé. */
	val dailyStore: DailyStore = object : DailyStore {
		/** Lit ou tire le puzzle du jour [inDay]. */
		override suspend fun puzzleFor(inDay: String): Puzzle? {
			val (vPuzzles, vUser) = ready()
			val vDao = vUser.userDao()
			vDao.getDailyPuzzle(inDay)?.let { inDaily -> vPuzzles.puzzleDao().getById(inDaily.puzzleId)?.let { return it.toPuzzle() } }
			val vChosen = legacyDailyId(inDay)?.let { vPuzzles.puzzleDao().getById(it)?.toPuzzle() }
				?: PuzzleSelector(vPuzzles.puzzleDao(), vDao, Random(inDay.toLong())).nextPuzzle(
					ProgressRepository(vUser, ratingSystem).currentRating().rating, SelectionCriteria(), null, System.currentTimeMillis(), vDao.getDailyPuzzleIds().toSet(),
				)
				?: return null
			vDao.insertDailyPuzzle(DailyPuzzleEntity(inDay, vChosen.id, System.currentTimeMillis()))
			return vDao.getDailyPuzzle(inDay)?.let { vPuzzles.puzzleDao().getById(it.puzzleId)?.toPuzzle() } ?: vChosen
		}

		/** Jours dont le puzzle a été réussi. */
		override suspend fun solvedDays(): Set<String> = ready().second.userDao().getSolvedDailyDays().toSet()
	}

	/**
	 * Retourne le puzzle du jour mémorisé par la version précédente (préférences) pour ce jour, ou null.
	 */
	private fun legacyDailyId(inDay: String): String? {
		val vPreferences = context.getSharedPreferences(kDailyFile, Context.MODE_PRIVATE)
		return vPreferences.getString(kDailyIdKey, null).takeIf { vPreferences.getString(kDailyDayKey, null) == inDay }
	}

	/**
	 * Retourne l'avancement de l'objectif quotidien : puzzles réussis aujourd'hui (tous modes, défis
	 * compris, puisque chaque puzzle de défi est enregistré comme une tentative) et objectif des réglages.
	 */
	suspend fun goalProgress(inNow: Long = System.currentTimeMillis()): GoalProgress {
		val vDao = ready().second.userDao()
		val vFrom = DailyGoal.dayStart(inNow)
		return GoalProgress(vDao.countSuccessesSince(vFrom), settingsStore.settings.value.dailyGoal)
	}

	/**
	 * Exporte la progression (copie complète de la base utilisateur) dans [inOutput].
	 */
	suspend fun exportProgress(inOutput: OutputStream) {
		val vUser = ready().second
		withContext(Dispatchers.IO) {
			val vTemp = File(context.cacheDir, kExportFile).apply { delete() }
			try {
				UserBackup.export(vUser, vTemp.absolutePath)
				vTemp.inputStream().use { it.copyTo(inOutput) }
			} finally {
				vTemp.delete()
			}
		}
	}

	/**
	 * Prépare l'import d'une progression : vérifie le fichier puis le met de côté ; il remplacera la
	 * base au prochain démarrage (avant son ouverture). Retourne faux si le fichier ne convient pas.
	 */
	suspend fun stageImport(inInput: InputStream): Boolean = withContext(Dispatchers.IO) {
		val vTemp = File(context.cacheDir, kImportFile).apply { delete() }
		inInput.use { vSource -> vTemp.outputStream().use { vSource.copyTo(it) } }
		if (UserBackup.validate(vTemp.absolutePath) == null) {
			vTemp.delete()
			return@withContext false
		}
		val vPending = context.getDatabasePath(kPendingImport)
		vPending.delete()
		vTemp.copyTo(vPending)
		vTemp.delete()
		true
	}

	/**
	 * Remplace la base utilisateur par une progression importée, si un import est en attente (appelé
	 * avant l'ouverture de la base).
	 */
	private fun applyPendingImport() {
		val vPending = context.getDatabasePath(kPendingImport)
		if (!vPending.exists()) {
			return
		}
		val vTarget = context.getDatabasePath(UserDatabase.kFileName)
		for (vSuffix in listOf("", "-wal", "-shm", "-journal")) {
			File(vTarget.path + vSuffix).delete()
		}
		vPending.renameTo(vTarget)
	}

	/**
	 * Retourne le nombre de puzzles à réviser maintenant.
	 */
	suspend fun dueReviewCount(inNow: Long = System.currentTimeMillis()): Int = ready().second.userDao().countDueReviews(inNow)

	/**
	 * Retourne la série de jours consécutifs en cours, en ne lisant que les dates des tentatives
	 * récentes : la fenêtre double tant que la série la remplit, si bien que le coût ne grandit pas
	 * avec l'historique.
	 */
	suspend fun dayStreak(inNow: Long = System.currentTimeMillis()): Int {
		val vDao = ready().second.userDao()
		val vCalculator = StatsCalculator()
		var vWindowDays = kStreakWindowDays
		while (true) {
			val vStreak = vCalculator.dayStreak(vDao.getPlayedAtSince(inNow - vWindowDays * kDayMs), inNow)
			if (vStreak < vWindowDays - 1) {
				return vStreak
			}
			vWindowDays *= 2
		}
	}

	/**
	 * Retourne l'état de la base de puzzles hors ligne : nombre de puzzles et taille du fichier.
	 */
	suspend fun offlineBaseInfo(): OfflineBaseInfo {
		val vCount = ready().first.puzzleDao().count()
		val vSize = withContext(Dispatchers.IO) { context.getDatabasePath(PuzzleDatabase.kFileName).length() }
		return OfflineBaseInfo(vCount, vSize)
	}

	/** Parties des défis Survie et Rush. */
	val challengeStore: ChallengeStore = object : ChallengeStore {
		/** Lit le meilleur score du défi. */
		override suspend fun best(inKind: ChallengeKind): Int? = ready().second.userDao().getBestChallengeScore(inKind.key)

		/** Enregistre une partie terminée. */
		override suspend fun save(inKind: ChallengeKind, inScore: Int, inBestPuzzleRating: Int?, inDurationMs: Long) {
			ready().second.userDao().insertChallengeRun(
				ChallengeRunEntity(kind = inKind.key, playedAt = System.currentTimeMillis(), score = inScore, bestPuzzleRating = inBestPuzzleRating, durationMs = inDurationMs)
			)
		}
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
			inReviewable: Boolean,
		): RecordedResult {
			val vRepository = ProgressRepository(ready().second, ratingSystem) { DailyGoal.dayStart(it) }
			val vBefore = vRepository.currentRating().rating
			val vAfter = vRepository.recordResult(inPuzzle, inResult, inMistakes, inRevealed, inRated, System.currentTimeMillis(), inDurationMs, inFailedAtMove, inReviewable).rating
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
		/** Fichier temporaire de l'export. */
		private const val kExportFile = "export.db"

		/** Fichier temporaire de l'import, avant vérification. */
		private const val kImportFile = "import.db"

		/** Progression importée en attente du prochain démarrage (à côté des bases). */
		private const val kPendingImport = "user.db.import"

		/** Nombre de problèmes non réussis examinés pour le rejeu. */
		private const val kReplayCandidates = 50

		/** Nombre de puzzles à réviser lus pour choisir le suivant. */
		private const val kReviewCandidates = 20

		/** Fenêtre de départ de la lecture des tentatives pour la série de jours. */
		private const val kStreakWindowDays = 64L

		/** Durée d'un jour en millisecondes. */
		private const val kDayMs = 24L * 60 * 60 * 1000
	}
}

/** Fichier des préférences du puzzle du jour (version précédente, lu pour ne pas changer le puzzle du jour en cours). */
private const val kDailyFile = "daily"

/** Clé du jour du puzzle mémorisé. */
private const val kDailyDayKey = "day"

/** Clé de l'identifiant du puzzle mémorisé. */
private const val kDailyIdKey = "id"

/**
 * État de la base de puzzles embarquée.
 *
 * @property puzzleCount nombre de puzzles.
 * @property sizeBytes taille du fichier de la base.
 */
data class OfflineBaseInfo(val puzzleCount: Int, val sizeBytes: Long)
