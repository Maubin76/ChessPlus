# Avancement de ChessPlus

## Phase en cours

**Phase 4 — Design** : étapes 1 et 2 validées (`docs/DESIGN.md`) ; étape 3, trois directions visuelles (`docs/design/direction-*.png`), en attente du choix de Mathieu.

## Fait

### Phase 1

- Wrapper Gradle 9.8.2, Kotlin DSL, version catalog `gradle/libs.versions.toml`.
- `.editorconfig` (tabulations, largeur 4), `.gitignore` (exclut `*.csv` et `*.csv.zst`).
- Modules :
	- `:core` : KMP, cibles Android et JVM, source set intermédiaire `jvmShared` (Android + JVM) qui dépend de chesslib.
	- `:data` : KMP, cibles Android et JVM, Room KMP + KSP + `sqlite-bundled`, schémas exportés dans `data/schemas`.
	- `:puzzle-builder` : JVM, plugin `application`, dépend de `:core`, `:data` et zstd-jni ; jamais branché sur le build normal.
	- `:app` : Android, Compose Material 3, écran provisoire, thème clair/sombre suivant le système.
- Un test de câblage par module ; le test `jvmShared` (chesslib) tourne à la fois sur la JVM et en test hôte Android.
- `./gradlew build` réussit : tests, lint, APK debug et APK release minifié par R8.
- Validée par Mathieu le 2026-11-20.

### Phase 2

- `model` : `Color`, `PieceType`, `Piece`, `Square` (index 0 = a1 à 63 = h8), `Move` (lecture/écriture UCI), `Theme` (73 thèmes à bit fixe), `ThemeSet` (masque sur deux `Long` : `low` bits 0-63, `high` bits 64-127).
- `codec.MoveCodec` : 2 octets gros-boutistes par coup (départ 6 bits, arrivée 6 bits, promotion 3 bits).
- `puzzle.LichessCsv` : découpage strict (11 champs, champs vides finaux conservés, nombre pair de coups) ; `LichessCsvRow.toPuzzle()` échoue sur un thème inconnu.
- `puzzle.Puzzle` : camp du joueur = inverse du trait de la FEN, URL Lichess.
- `rules` : interfaces `ChessRules`, `PositionView`, `ChessPosition` (types du projet uniquement), `defaultChessRules()` en `expect`/`actual` ; implémentation chesslib dans `jvmShared`.
- `puzzle.PuzzleSession` : déroulement d'un problème (voir décisions), état sérialisable `PuzzleSnapshot` pour la reprise.
- `rating` : interface `RatingSystem`, implémentations `Glicko2RatingSystem` et `EloRatingSystem` (choix en phase de design).
- Tests (`commonTest`, exécutés sur JVM et en test hôte Android) : 50 tests par cible ; 104 tests au total sur le projet, tous verts.
	- codec : encodages connus, aller-retour des 4 problèmes de l'extrait et de toutes les combinaisons case/case/promotion, données invalides ;
	- thèmes : correspondance exacte avec les 73 thèmes du CSV réel, bits uniques, mots haut et bas ;
	- CSV : 4 lignes de l'extrait, champs vides finaux, 10/12 champs, coups impairs, nombres et thèmes invalides ;
	- règles : position initiale, export FEN, roque (et ses interdictions), prise en passant (jouée, depuis la FEN, expirée), promotions et sous-promotion, échec, mat, pat, clouage, annulation, copie ;
	- sessions : déroulement complet des 4 problèmes, mat final de 00sHx, mat alternatif sur un problème réel (00Mgf : d8h8# au lieu de d8g8#), erreur puis nouvel essai, coup illégal, promotion, solution coup par coup, recommencer, ordre des tours, reprise depuis l'état ;
	- cote : exemple de l'article de Glickman, sens des mises à jour, bornes de l'écart de cote, formule Elo.
- Validée par Mathieu le 2026-11-20.

### Phase 3

- `:data`, deux bases Room séparées (liées seulement par l'identifiant Lichess) :
	- `PuzzleDatabase` (`puzzles.db`, embarquée dans les assets, lecture seule) : table `puzzle` (id, fen, moves BLOB, rating, themesLow, themesHigh) et index de sélection couvrant `(rating, themesLow, themesHigh, id)` ;
	- `UserDatabase` (`user.db`) : `attempt` (historique, avec cote et thèmes du problème recopiés pour les statistiques), `puzzle_state` (bilan par problème), `player` (cote), `active_session` (problème en cours, pour la reprise) ;
	- `ProgressRepository.recordResult` : tentative, bilan et cote dans une seule transaction ;
	- `PuzzleSelector` : comptage dans une tranche de cote puis tirage d'un rang aléatoire (pas d'`ORDER BY RANDOM()`), tranche élargie de ±100 à ±600 si besoin, problèmes réussis exclus, échoués reproposés après 24 h ;
	- fabriques `createPuzzleDatabase` / `createUserDatabase` pour Android (copie depuis les assets, recopie si la version change) et pour la JVM.
- `:puzzle-builder` : lecture en flux du `.zst`, en-tête vérifié, seuils de qualité, plafond par tranche de 200 points en deux passes (garde les plus populaires), validation de chaque solution par les règles, insertion Room par lots dans une seule transaction, index supprimés puis recréés à l'identique, `VACUUM`, copie dans `app/src/main/assets/puzzles.db`. Rapport : rejets par motif, répartition par tranche et par thème, durées, taille.
- Tests : 13 pour `:data` (JVM, base en mémoire, dont l'annulation d'une transaction), 12 pour le builder (options, plafond, génération de bout en bout sur un `.zst` créé par le test, index recréés, thème inconnu, ligne mal formée). 126 tests au total, tous verts.

- Validée par Mathieu le 2026-11-20 : seuils de départ retenus comme définitifs, délai de 24 h validé.

### Résultat sur le vrai CSV (seuils définitifs)

Commande : `./gradlew :puzzle-builder:run --args="C:/Users/mau/Downloads/lichess_db_puzzle.csv.zst"`

- Seuils : RD ≤ 80, popularité ≥ 90, parties ≥ 250, plafond 30 000 par tranche.
- 6 193 530 lignes lues → **292 090 problèmes retenus** ; rejets : RD 2 600 916, popularité 1 421 356, parties 221 531, plafond 1 657 637, solution invalide 0.
- Répartition : 30 000 par tranche de 600 à 2399 ; 1 314 en 400-599 ; 20 214 en 2400-2599 ; 562 en 2600-2799.
- Les 73 thèmes sont présents ; les plus rares : balestraMate 28, underPromotion 58, vukovicMate 67, superGM 71, castling 88.
- **Durée : 27 à 37 s** selon la charge de la machine (passe 1 ≈ 7 s, passe 2 ≈ 19 à 27 s).
- **Fichier : 36,8 Mo** ; compressé à 20,3 Mo dans l'APK release (25,3 Mo au total).
- Effet des index sur la passe 2 : avec l'index simple sur `rating`, négligeable (< 1 s) ; avec l'index couvrant, 30,2 s en le gardant contre 18,9 s en le supprimant puis recréant (0,3 s) → suppression par défaut, `--keep-indexes` pour l'autre mode.
- Sélection mesurée sur PC (Python/SQLite) dans la tranche 1400-1600 : 2 ms sans filtre, 4 ms avec un filtre de thème (200 ms avec l'index simple, d'où l'index couvrant, +4,4 Mo).

### Statistiques du CSV Lichess (fichier du 2026-11-20)

- 6 193 530 problèmes, aucune ligne mal formée, aucun nombre de coups impair.
- Cotes de 399 à 3330 ; solutions de 2 à 34 demi-coups (2 : 921 479, 4 : 3 132 447, 6 : 1 628 493, 8 : 364 028, au-delà : < 150 000).
- 73 thèmes : masque sur deux colonnes `INTEGER`.

## Reste à faire

- Phase 4 : design (questions préalables, inventaire des écrans, directions visuelles, maquettes `@Preview`, `docs/DESIGN.md`).
- Phase 5 : écrans.
- Phase 6 : finitions.

## Décisions prises

### Versions (choisies le 2026-11-20, stables et publiées depuis au moins deux semaines)

| Élément | Version | Remarque |
| --- | --- | --- |
| Gradle | 9.8.2 | 9.9.x trop récent |
| Android Gradle Plugin | 9.5.1 | |
| Kotlin | 2.4.21 | |
| KSP | 2.3.13 | |
| Room | 2.8.5 | |
| androidx.sqlite (bundled) | 2.7.1 | |
| Coroutines | 1.11.0 | |
| Compose BOM | 2026.09.00 | 2026.11.01 trop récent |
| activity-compose | 1.13.0 | |
| lifecycle | 2.11.0 | |
| core-ktx | 1.19.1 | |
| chesslib | 1.3.7 | publié uniquement sur JitPack, licence Apache-2.0 |
| zstd-jni | 1.5.7-25 | |
| JUnit | 4.13.2 | |

### Choix techniques de la phase 1

- **Plugin Android des modules KMP** : AGP 9 n'accepte plus `com.android.library` avec le plugin Kotlin Multiplatform. `:core` et `:data` utilisent donc le plugin `com.android.kotlin.multiplatform.library`, dont la cible se déclare avec `kotlin { android { … } }` au lieu de `androidTarget()`. Le résultat est le même (cibles Android et JVM).
- **Source set `jvmShared`** : créé par le template de hiérarchie Kotlin, avec un filtre sur les compilations JVM et Android (`withAndroidTarget()` ne reconnaît pas la cible du nouveau plugin). Tests associés dans `jvmSharedTest`.
- **Kotlin dans `:app`** : AGP 9 intègre Kotlin, le plugin `org.jetbrains.kotlin.android` n'est pas appliqué.
- **JitPack** : déclaré dans `settings.gradle.kts` en `exclusiveContent`, limité au groupe `com.github.bhlangonijr`.
- **SDK** : application pour Android 16 uniquement (décision de Mathieu) : `minSdk` 36, `targetSdk` 36. `compileSdk` 37, exigé par core-ktx 1.19 et Compose 1.12 (sans effet sur les appareils visés).
- **JVM** : toolchain 21, bytecode cible 17.
- **Console du builder** : la tâche `run` force `stdout.encoding`/`stderr.encoding` en UTF-8, sinon les accents sont corrompus sous Windows.
- **Package** : `com.chessplus` (`.core`, `.data`, `.builder`, `.app`), identifiant `com.chessplus.app` (validé).
- **Spécification** : le prompt d'origine a été déplacé dans `docs/SPEC.md`, référencé depuis `CLAUDE.md`.
- **Paramètres des `override`** : ils gardent le nom du supertype (validé) ; le préfixe `in` s'applique à toutes les autres fonctions.
- **Constantes** : les `val` immuables de `companion object` prennent aussi le préfixe `k` (`Square.kAll`, `ThemeSet.kEmpty`).

### Outils de la phase de design

- Les planches `docs/design/direction-*.png` sont générées par un script Python jetable (Pillow, hors dépôt) : rendu des SVG des pièces Lichess, polices OFL (Playfair Display, Inter, DM Serif Display, DM Sans, Manrope), icônes Material Symbols (Apache 2.0). Les maquettes définitives seront des `@Preview` Compose (étape 4).

### Choix techniques de la phase 3

- **Deux fichiers de base** plutôt que deux groupes de tables dans un même fichier : la base des problèmes est remplacée en bloc depuis les assets (incrémenter `PuzzleDatabase.kVersion`), la base utilisateur n'est jamais touchée.
- **Plafond par tranche** : ajouté parce que les seuils seuls donnent soit des millions de problèmes, soit une répartition très déséquilibrée.
- **Validation par les règles** dans le builder : chaque solution retenue est rejouée avec chesslib (aucun rejet sur le CSV actuel).
- **Thèmes inconnus** : vérifiés sur toutes les lignes, y compris celles rejetées par les seuils.
- **Journal SQLite** en mode `TRUNCATE` pour le builder : fichier autonome, sans `-wal`.
- `puzzles.db` est exclu de git en attendant la décision de Mathieu.
- `-Xexpect-actual-classes` activé dans `:data` (constructeurs Room en `expect object`).

### Choix techniques de la phase 2

- **Thèmes** : chaque thème a un bit fixe déclaré explicitement (pas l'ordinal) ; on n'en change jamais, on en ajoute.
- **Session de problème** :
	- la session ne joue jamais le coup de l'adversaire d'elle-même : l'interface appelle `playOpponentMove()` quand le statut vaut `OpponentToMove`, après son animation ;
	- un mauvais coup légal n'est pas joué sur l'échiquier (l'interface peut l'animer puis le retirer) ; le joueur réessaie depuis la même position ;
	- un coup illégal est refusé sans compter d'erreur ;
	- un autre coup légal qui donne mat termine le problème avec succès ;
	- `result` passe à `Failure` dès la première erreur ou aide et n'en change plus ; `Success` seulement si le problème est terminé sans erreur ni aide ;
	- « recommencer » revient à la FEN de départ en gardant l'échec ;
	- la reprise rejoue les coups à partir de `PuzzleSnapshot` (id, coups joués, mat alternatif, compteurs).
- **Glicko-2** : chaque problème est une période de notation d'une partie ; écart de cote des problèmes fixé à 75 (non stocké en base), tau 0,75, écart du joueur borné entre 45 et 350. L'écart se stabilise vers 60 en pratique. Valeurs provisoires, à confirmer en phase de design avec le choix de l'algorithme.

## Questions ouvertes

- `app/src/main/assets/puzzles.db` reste hors de git (à régénérer avec le builder) tant que Mathieu n'a pas demandé autre chose.
- Paramètres Glicko-2 (tau 0,75, écart des problèmes 75, écart du joueur entre 45 et 350) : à confirmer.
- La copie depuis les assets (`createFromAsset` avec le driver SQLite intégré) n'est pas encore vérifiée sur appareil : premier test en phase 5.
