"""Étude volumétrique : 2 ans d'utilisation à 20 puzzles par jour, sur le vrai schéma de la base
utilisateur (Room, version 5) et la vraie base des puzzles, avec l'algorithme de sélection de
l'application (PuzzleSelector) et la répétition espacée (ProgressRepository.scheduleReview).

Hypothèses (détaillées dans docs/VOLUMETRIE.md) : chaque jour, le puzzle du jour, jusqu'à 4 puzzles
de Révision, une Survie le samedi, un Rush de 14 puzzles le dimanche, et l'Arcade pour compléter
à 20 puzzles ; force réelle du joueur de 1500 à 1800 en deux ans.
Lancer depuis la racine du dépôt : python ci/volumetrie.py (résultats dans build/volumetrie)."""
import bisect, json, math, os, random, shutil, sqlite3, time, csv

kOut = 'build/volumetrie/'
os.makedirs(kOut, exist_ok=True)
random.seed(42)

kDays = 730
kDayMs = 86_400_000
kStart = 1_735_689_600_000  # 2025-01-01
kIntervals = [1, 3, 7, 14, 30]
kRetryMs = 24 * 3600 * 1000

# Puzzles réels, triés par classement.
vSrc = sqlite3.connect('app/src/main/assets/puzzles.db')
vRows = vSrc.execute('SELECT rating, id, themesLow, themesHigh FROM puzzle ORDER BY rating, themesLow, themesHigh, id').fetchall()
kRatings = [r[0] for r in vRows]
kPuzzle = {r[1]: r for r in vRows}

state = {}      # puzzleId -> dict(solved, attempts, failures, lastResult, lastPlayedAt)
review = {}     # puzzleId -> dict(level, dueAt, addedAt)
attempts = []   # tuples dans l'ordre des colonnes
runs = []
dailies = []
dailyIds = set()
rating = 1500.0
deviation = 350.0
stats = {'fail': {}, 'mastered': 0, 'arcade': 0, 'daily': 0, 'review': 0, 'survival': 0, 'rush': 0, 'widened': 0, 'survivalFirst': [], 'survivalScores': [], 'reviewBacklog': []}


def strength(inDay):
	"""Force réelle du joueur : de 1500 à 1800 en deux ans."""
	return 1500 + 300 * inDay / kDays


def eligible(inId, inNow, inExclude):
	"""Règle de PuzzleSelector.isEligible, plus les exclusions."""
	if inId in inExclude:
		return False
	vState = state.get(inId)
	if vState is None:
		return True
	if vState['solved']:
		return False
	return vState['lastResult'] == 'Failure' and inNow - vState['lastPlayedAt'] >= kRetryMs


def select(inCenter, inNow, inExclude=frozenset(), inHalfWidth=100, inRng=random):
	"""Port de PuzzleSelector.nextPuzzle (tranches élargies de 100 jusqu'à ± 600, 10 tirages par tranche)."""
	vFirst = True
	for vHalf in range(inHalfWidth, 601, 100):
		vLo = bisect.bisect_left(kRatings, inCenter - vHalf)
		vHi = bisect.bisect_right(kRatings, inCenter + vHalf)
		vCount = vHi - vLo
		if vCount > 0:
			for _ in range(min(10, vCount)):
				vRow = vRows[vLo + inRng.randrange(vCount)]
				if eligible(vRow[1], inNow, inExclude):
					if not vFirst:
						stats['widened'] += 1
					return vRow
		vFirst = False
	return None


def success(inPuzzleRating, inDay, inBonus=0.0):
	"""Réussite tirée selon l'écart entre la force du joueur et le puzzle."""
	vP = 1 / (1 + 10 ** ((inPuzzleRating - strength(inDay)) / 400)) + inBonus
	return random.random() < min(0.97, vP)


def day_start(inNow):
	return inNow - (inNow - kStart) % kDayMs


def record(inRow, inNow, inOk, inRated, inDay, inMode):
	"""Écritures de ProgressRepository.recordResult : tentative, bilan, révision, joueur."""
	global rating, deviation
	vRating, vId, vLow, vHigh = inRow
	vBefore = rating
	if inRated:
		vExpected = 1 / (1 + 10 ** ((vRating - rating) / 400))
		rating += 20 * ((1 if inOk else 0) - vExpected)
		deviation = max(60.0, deviation * 0.97)
	vResult = 'Success' if inOk else 'Failure'
	if not inOk:
		stats['fail'][inMode] = stats['fail'].get(inMode, 0) + 1
	vMistakes = 0 if inOk else random.choice([1, 1, 1, 2])
	vRevealed = 0 if inOk else random.choice([0, 0, 1])
	vFailedAt = None if inOk else random.choice([1, 1, 2, 3])
	attempts.append((len(attempts) + 1, vId, inNow, vResult, int(inRated), vRating, vLow, vHigh, vMistakes, vRevealed, vBefore, rating,
		random.randint(8_000, 120_000), vFailedAt, deviation if inRated else None))
	vState = state.get(vId)
	if vState is None:
		vState = state[vId] = {'solved': False, 'attempts': 0, 'failures': 0, 'lastResult': vResult, 'lastPlayedAt': inNow}
	vState['attempts'] += 1
	vState['failures'] += 0 if inOk else 1
	vState['solved'] = vState['solved'] or inOk
	vState['lastResult'] = vResult
	vState['lastPlayedAt'] = inNow
	vItem = review.get(vId)
	if not inOk:
		review[vId] = {'level': 0, 'dueAt': day_start(inNow) + kIntervals[0] * kDayMs, 'addedAt': vItem['addedAt'] if vItem else inNow}
	elif vItem is not None and vItem['dueAt'] <= inNow:
		vNext = vItem['level'] + 1
		if vNext >= len(kIntervals):
			del review[vId]
			stats['mastered'] += 1
		else:
			vItem['level'] = vNext
			vItem['dueAt'] = day_start(inNow) + kIntervals[vNext] * kDayMs


for vDay in range(kDays):
	vNow = kStart + vDay * kDayMs + 8 * 3600 * 1000
	vPlayed = 0

	def tick():
		global vNow
		vNow += random.randint(20_000, 120_000)

	# Puzzle du jour (graine = date), autour du classement, hors anciens puzzles du jour.
	vRow = select(round(rating), vNow, dailyIds, inRng=random.Random(vDay))
	dailies.append((f'D{vDay:05d}', vRow[1], vNow))
	dailyIds.add(vRow[1])
	record(vRow, vNow, success(vRow[0], vDay), vRow[1] not in state, vDay, 'daily'); tick(); vPlayed += 1; stats['daily'] += 1

	# Révision : jusqu'à 4 puzzles arrivés à échéance, du plus ancien au plus récent.
	vDue = sorted((v['dueAt'], k) for k, v in review.items() if v['dueAt'] <= vNow)
	stats['reviewBacklog'].append(len(vDue))
	for _, vId in vDue[:4]:
		vRow = kPuzzle[vId]
		record(vRow, vNow, success(vRow[0], vDay, 0.2), False, vDay, 'review'); tick(); vPlayed += 1; stats['review'] += 1

	# Défis : une Survie le samedi, un Rush le dimanche.
	if vDay % 7 == 5:
		vSeen, vServed, vMisses, vScore, vBegin = set(), 0, 0, 0, vNow
		while vMisses < 3:
			vRow = select(400 + vServed * 50, vNow, vSeen, inHalfWidth=25)
			if vRow is None:
				break
			if vServed == 0:
				stats['survivalFirst'].append(vRow[0])
			vSeen.add(vRow[1]); vServed += 1
			vOk = success(vRow[0], vDay)
			vScore += vOk; vMisses += not vOk
			record(vRow, vNow, vOk, False, vDay, 'survival'); tick(); vPlayed += 1; stats['survival'] += 1
		runs.append(('survival', vNow, vScore, None, vNow - vBegin))
		stats['survivalScores'].append(vScore)
	elif vDay % 7 == 6:
		vSeen, vScore, vBegin = set(), 0, vNow
		for _ in range(14):
			vRow = select(round(rating), vNow, vSeen)
			vSeen.add(vRow[1])
			vOk = success(vRow[0], vDay)
			vScore += vOk
			record(vRow, vNow, vOk, False, vDay, 'rush'); vNow += random.randint(8_000, 20_000); vPlayed += 1; stats['rush'] += 1
		runs.append(('rush', vNow, vScore, None, 180_000))

	# Arcade (et Entraînement) pour compléter les 20 puzzles du jour.
	while vPlayed < 20:
		vRow = select(round(rating), vNow, frozenset())
		vNew = vRow[1] not in state
		record(vRow, vNow, success(vRow[0], vDay), vNew, vDay, 'arcade'); tick(); vPlayed += 1; stats['arcade'] += 1

	if vDay in (90, 182, 365, 547, 729):
		stats.setdefault('snapshots', []).append((vDay + 1, len(attempts), len(state), len(review)))


# Écriture dans le vrai schéma (Room v5).
vSchema = json.load(open('data/schemas/com.chessplus.data.user.UserDatabase/5.json', encoding='utf-8'))['database']
vPath = kOut + 'user.db'
for vSuffix in ('', '-wal', '-shm'):
	if os.path.exists(vPath + vSuffix):
		os.remove(vPath + vSuffix)
vDb = sqlite3.connect(vPath)
for vEntity in vSchema['entities']:
	vDb.execute(vEntity['createSql'].replace('${TABLE_NAME}', vEntity['tableName']))
	for vIndex in vEntity.get('indices', []):
		vDb.execute(vIndex['createSql'].replace('${TABLE_NAME}', vEntity['tableName']))
for vSql in vSchema['setupQueries']:
	vDb.execute(vSql)
vDb.executemany('INSERT INTO attempt VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)', attempts)
vDb.executemany('INSERT INTO puzzle_state VALUES (?,?,?,?,?,?)', [(k, int(v['solved']), v['attempts'], v['failures'], v['lastResult'], v['lastPlayedAt']) for k, v in state.items()])
vDb.execute('INSERT INTO player VALUES (1, ?, ?, 0.06, ?)', (rating, deviation, vNow))
vDb.execute("INSERT INTO active_session VALUES (1, 'abc12', 2, NULL, 0, 0, 1, 0, ?)", (vNow,))
vDb.executemany('INSERT INTO challenge_run (kind, playedAt, score, bestPuzzleRating, durationMs) VALUES (?,?,?,?,?)', runs)
vDb.executemany('INSERT INTO daily_puzzle VALUES (?,?,?)', dailies)
vDb.executemany('INSERT INTO review_item VALUES (?,?,?,?)', [(k, v['level'], v['dueAt'], v['addedAt']) for k, v in review.items()])
vDb.commit()
vDb.close()
vRaw = os.path.getsize(vPath)
vDb = sqlite3.connect(vPath)
vDb.execute('VACUUM')
vDb.close()
vVacuum = os.path.getsize(vPath)

# Taille par table (avec ses index) : copie ne gardant que cette table, compactée.
vTables = {}
for vEntity in vSchema['entities']:
	vCopy = kOut + 'one.db'
	shutil.copy(vPath, vCopy)
	vOne = sqlite3.connect(vCopy)
	for vOther in vSchema['entities']:
		if vOther['tableName'] != vEntity['tableName']:
			vOne.execute(f"DROP TABLE `{vOther['tableName']}`")
	vOne.commit(); vOne.execute('VACUUM'); vOne.close()
	vEmpty = sqlite3.connect(kOut + 'empty.db')
	vEmpty.close()
	vTables[vEntity['tableName']] = os.path.getsize(vCopy)
	os.remove(vCopy)

# Requêtes de l'application, temps médian sur 30 exécutions (SQLite natif sur PC).
vDb = sqlite3.connect(vPath)
vQueries = {
	'observeAllAttempts (toutes les tentatives)': ('SELECT * FROM attempt ORDER BY playedAt, id', ()),
	'getRatedAttempts': ('SELECT * FROM attempt WHERE rated = 1', ()),
	'observeRatingHistory': ('SELECT playedAt, playerRatingAfter AS rating FROM attempt WHERE rated = 1 ORDER BY playedAt, id', ()),
	'countSuccessesSince (objectif)': ("SELECT COUNT(*) FROM attempt WHERE result = 'Success' AND playedAt >= ?", (vNow - kDayMs,)),
	'observeAttemptCount (widget)': ('SELECT COUNT(*) FROM attempt', ()),
	'observeRecentAttempts(100) (historique)': ('SELECT * FROM attempt ORDER BY playedAt DESC, id DESC LIMIT 100', ()),
	'getState (sélection, par tirage)': ('SELECT * FROM puzzle_state WHERE puzzleId = ?', (attempts[-1][1],)),
	'getLatestAttempt': ('SELECT * FROM attempt WHERE puzzleId = ? ORDER BY playedAt DESC, id DESC LIMIT 1', (attempts[-1][1],)),
	'getUnsolvedIds(50) (rejeu)': ('SELECT puzzleId FROM puzzle_state WHERE solved = 0 ORDER BY lastPlayedAt DESC LIMIT 50', ()),
	'getDueReviewIds(20)': ('SELECT puzzleId FROM review_item WHERE dueAt <= ? ORDER BY dueAt, puzzleId LIMIT 20', (vNow,)),
	'countDueReviews': ('SELECT COUNT(*) FROM review_item WHERE dueAt <= ?', (vNow,)),
	'observeSolvedDailyDays (calendrier)': ('SELECT d.day FROM daily_puzzle d INNER JOIN puzzle_state s ON s.puzzleId = d.puzzleId WHERE s.solved = 1', ()),
	'observeChallengeRuns': ('SELECT * FROM challenge_run ORDER BY playedAt, id', ()),
}
vTimes = {}
for vName, (vSql, vArgs) in vQueries.items():
	vSamples = []
	for _ in range(30):
		vT = time.perf_counter()
		vDb.execute(vSql, vArgs).fetchall()
		vSamples.append((time.perf_counter() - vT) * 1000)
	vSamples.sort()
	vTimes[vName] = vSamples[15]
vDb.close()

# Tentatives pour le banc d'essai du calcul des statistiques (JVM).
with open(kOut + 'attempts.csv', 'w', newline='', encoding='utf-8') as vFile:
	csv.writer(vFile).writerows(attempts)

vReport = {
	'attempts': len(attempts), 'puzzle_state': len(state), 'solved': sum(v['solved'] for v in state.values()),
	'review_item': len(review), 'challenge_run': len(runs), 'daily_puzzle': len(dailies),
	'mix': {k: stats[k] for k in ('daily', 'review', 'survival', 'rush', 'arcade')},
	'failures': stats['fail'], 'mastered': stats['mastered'], 'widened': stats['widened'], 'sizeRaw': vRaw, 'sizeVacuum': vVacuum, 'tables': vTables, 'times': vTimes,
	'finalRating': rating, 'snapshots': stats['snapshots'],
	'reviewBacklog': [stats['reviewBacklog'][d] for d in (30, 90, 182, 365, 547, 729)],
	'survivalFirstFirst10': stats['survivalFirst'][:10], 'survivalFirstLast10': stats['survivalFirst'][-10:],
	'survivalScoresAvg': sum(stats['survivalScores']) / len(stats['survivalScores']),
	'distinctPerBand': {},
}
for vLo in range(400, 2800, 200):
	vIn = [k for k in state if vLo <= kPuzzle[k][0] < vLo + 200]
	vTotal = bisect.bisect_left(kRatings, vLo + 200) - bisect.bisect_left(kRatings, vLo)
	vReport['distinctPerBand'][f'{vLo}-{vLo + 199}'] = (len(vIn), vTotal)
json.dump(vReport, open(kOut + 'report.json', 'w', encoding='utf-8'), indent=1, ensure_ascii=False)
print(json.dumps(vReport, indent=1, ensure_ascii=False))
