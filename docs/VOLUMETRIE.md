# Étude volumétrique : 20 puzzles par jour pendant 2 ans

Étude du 2026-11-24, sur `road-to-v2` (base utilisateur en version 5). Script reproductible : `python ci/volumetrie.py` (résultats dans `build/volumetrie/`).

## Méthode

La simulation rejoue 730 jours d'utilisation. Elle reprend :

- le vrai schéma de la base utilisateur (`data/schemas/.../UserDatabase/5.json`) ;
- la vraie base des puzzles (`puzzles.db`, 292 090 puzzles) ;
- l'algorithme de sélection de l'application (`PuzzleSelector`) ;
- les écritures de `ProgressRepository.recordResult` (tentative, bilan, révision, joueur).

Les temps de chargement et de calcul ont été mesurés sur PC, avec Room et `StatsCalculator`, sur les 15 707 tentatives simulées.

### Hypothèses d'utilisation

| Chaque jour | Puzzles |
|---|---|
| Puzzle du jour | 1 |
| Révision : puzzles arrivés à échéance, du plus ancien au plus récent | jusqu'à 4 |
| Survie, le samedi (jusqu'à 3 erreurs) | environ 26 |
| Rush, le dimanche | 14 |
| Arcade, pour compléter | le reste, jusqu'à 20 |

- **Joueur** : sa force réelle passe de 1 500 à 1 800 en deux ans. La réussite suit l'écart entre sa force et le classement du puzzle (bonus de 20 points de pourcentage en Révision, puzzle déjà vu).
- **Volume obtenu** : 15 707 puzzles joués (21,5 par jour en moyenne, à cause des Survies), dont :
  - Arcade 7 938 ;
  - Révision 2 916 ;
  - Survie 2 667 ;
  - Rush 1 456 ;
  - puzzle du jour 730.

## 1. Taille de la base utilisateur

| Table | Lignes après 2 ans | Taille (avec index) | Par ligne |
|---|---:|---:|---:|
| `attempt` (une ligne par puzzle joué) | 15 707 | 1,53 Mo | 98 o |
| `puzzle_state` (une ligne par puzzle distinct) | 12 415 | 0,75 Mo | 61 o |
| `review_item` (file de révision) | 5 180 | 0,31 Mo | 59 o |
| `daily_puzzle` | 730 | 0,05 Mo | — |
| `challenge_run` | 208 | 0,03 Mo | — |
| `player`, `active_session` | 1 + 1 | négligeable | — |
| **Fichier complet** | | **2,6 Mo** | ≈ 170 o par puzzle joué |

- **Croissance** : elle est linéaire, environ 1,3 Mo par an à ce rythme, soit environ 6,5 Mo à 5 ans et 13 Mo à 10 ans.
- **Journal WAL** : il occupe jusqu'à environ 4 Mo, de façon temporaire.
- **Sauvegarde exportée** : fichier de même taille que la base, 2,6 Mo.
- **Base des puzzles** : elle ne grossit jamais. Elle occupe 38,6 Mo une fois copiée sur le téléphone, et environ 20 Mo compressés dans l'APK.

**Conclusion : le stockage n'est pas un sujet.** Il faut 2,6 Mo à 2 ans, contre 38,6 Mo pour la base des puzzles.

## 2. Requêtes

Le tableau donne le temps médian sur PC, en SQLite natif.

| Requête | Temps | Commentaire |
|---|---:|---|
| `observeAllAttempts` (toutes les tentatives) | 60 ms en Python, **26 ms avec Room** (JVM) | lecture complète, en hausse linéaire |
| `getRatedAttempts` | 32 ms | lecture complète des tentatives notées |
| `observeRatingHistory` | 10 ms | |
| `observeSolvedDailyDays` (calendrier) | 1,2 ms | |
| `observeRecentAttempts(100)` (historique) | 0,4 ms | index sur `playedAt` |
| `countDueReviews`, `getDueReviewIds`, `countSuccessesSince`, `getState`, `getLatestAttempt`, `getUnsolvedIds` | < 0,3 ms | index utilisés |

**Calcul des statistiques** : `StatsCalculator.report` met 91 ms sur PC (JVM) pour 15 707 tentatives.

**Le seul point qui grossit vraiment est la lecture de toutes les tentatives suivie du calcul des statistiques.** Ce coût est payé :

- **à chaque puzzle terminé, par l'accueil** : `HomeViewModel` reste actif sous l'écran de jeu. Il relit deux fois toutes les tentatives : une fois pour la série de jours, avec le rapport complet de statistiques, et une fois pour déclencher le recalcul de l'objectif. Cela fait environ 145 ms de calcul sur PC à chaque puzzle, en tâche de fond. Sur le S24, on peut l'estimer entre 0,3 et 0,5 s, puis à environ 1 s à 5 ans (estimation, non mesurée). Pendant un Rush, un puzzle toutes les 10 s, c'est du processeur et de la batterie gaspillés, et des listes de 15 000 objets créées à chaque fois ;
- **à l'ouverture des statistiques** et d'une page de thème : une lecture et un calcul complets. C'est acceptable (environ 0,3 s estimée sur le téléphone à 2 ans), mais cela grandit indéfiniment.

## 3. Stock de puzzles

| Tranche | Puzzles vus en 2 ans | Puzzles disponibles | Part consommée |
|---|---:|---:|---:|
| 400 – 599 | 364 | 1 314 | 28 % |
| 600 – 1 399 | 1 829 | 120 000 | 2 % |
| 1 400 – 1 599 | 3 518 | 30 000 | 12 % |
| 1 600 – 1 799 | 5 666 | 30 000 | 19 % |
| 1 800 – 1 999 | 1 038 | 30 000 | 3 % |

- **Arcade, Rush, puzzle du jour** : aucun risque d'épuisement. Autour du classement, moins de 20 % des puzzles sont vus en deux ans, et un tirage n'échoue presque jamais (10 essais par tranche avant de l'élargir). À ce rythme, la tranche du joueur serait à moitié consommée au bout d'environ 5 ans s'il ne progressait pas.
- **Survie** : **la promesse « part de 400 » n'est pas tenue**, pour deux raisons :
  - la base ne compte qu'**1 puzzle entre 375 et 425**, 23 sous 500 et 1 314 sous 600 : les filtres de qualité du générateur écartent presque tous les puzzles faciles de Lichess ;
  - un puzzle réussi n'est jamais reproposé, en défi comme ailleurs.

  En pratique, le premier puzzle d'une Survie est entre 473 et 524 au début, puis **entre 604 et 623 après deux ans**, une fois les puzzles faciles épuisés. Les 3 ou 4 premiers paliers (400 à 550) ne sont donc pas réellement progressifs.

## 4. File de révision

C'est **le constat principal de l'étude**.

| Puzzles révisés par jour au plus | Puzzles maîtrisés en 2 ans | Puzzles en attente à 1 an | Puzzles en attente à 2 ans | Puzzles neufs joués (Arcade) |
|---:|---:|---:|---:|---:|
| 4 (hypothèse de base) | **5** | 2 616 | **5 158** | 7 938 |
| 10 | 260 | 1 667 | 3 327 | 4 713 |
| 20 (tout le temps de jeu) | 977 | 240 | 403 | 124 |

- **Ce qui entre** : environ 8 échecs par jour (Arcade 3 975 en deux ans, Rush 710, Survie 312, puzzle du jour 360, Révision 496). Tout échec, dans n'importe quel mode, entre en Révision.
- **Ce qui sort** : il faut 5 réussites à échéance d'affilée pour qu'un puzzle soit maîtrisé, et un échec le ramène au premier palier.
- **L'ordre « du plus ancien au plus récent » aggrave tout** : un puzzle réussi repart au bout de la file, derrière des milliers de puzzles plus anciens. Il n'est donc presque jamais revu.

Le compteur « Révision » de l'accueil afficherait plusieurs milliers de puzzles au bout d'un an. Le volume en base reste négligeable (0,3 Mo) : c'est l'usage qui pose problème.

## Recommandations

Retenues par Mathieu le 2026-11-24 et mises en œuvre le jour même (détail dans `docs/DESIGN.md` et `docs/PROGRESS.md`) : file de révision (défis exclus, échec qui fait redescendre d'un palier, plus avancés d'abord, plafond de 200, **3 réussites au lieu de 5**), accueil allégé, Survie qui accepte les puzzles déjà joués et générateur plus permissif sous 600. Simulation refaite avec ces règles (4 révisions par jour) : **528 puzzles maîtrisés en deux ans** (contre 5), file stable à 200 puzzles, dont environ 160 à réviser à un instant donné. Recommandations d'origine :

1. **File de révision**, à redéfinir. Pistes à combiner :
   - ne pas faire entrer les échecs des défis, puzzles joués vite et sous pression : environ 17 % des entrées ;
   - en Révision, un échec fait redescendre d'un palier au lieu de tout recommencer ;
   - servir d'abord les puzzles les plus avancés (les plus proches d'être maîtrisés), pour que la file se vide ;
   - plafonner la file, par exemple aux 200 échecs les plus récents, ou afficher un objectif de révision par jour plutôt que le total en attente.
2. **Accueil** : calculer la série de jours par une requête dédiée (jours distincts des tentatives récentes) au lieu du rapport complet, et déclencher l'objectif sur `observeAttemptCount` au lieu de `observeAllAttempts`. Le coût par puzzle devient alors constant.
3. **Survie** :
   - en défi, n'exclure que les puzzles déjà vus dans la partie, et non tous les puzzles réussis ;
   - faire partir la Survie de 600, où la base est assez fournie ;
   - ou régénérer la base avec des seuils assouplis sous 800.
4. **Statistiques** : rien d'urgent. Si l'historique dépasse plusieurs dizaines de milliers de puzzles, calculer les agrégats en SQL ou les mettre en cache par jour.
