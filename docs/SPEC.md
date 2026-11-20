# Prompt — ChessPlus

## Mode d'emploi

Ce document est le prompt de référence pour développer l'application ChessPlus avec un agent de code (Claude Code). Place-le à la racine du dépôt, par exemple dans `docs/SPEC.md`, référencé depuis `CLAUDE.md`.

**Rôle de l'agent :** tu es un développeur Kotlin senior, spécialisé Android et Kotlin Multiplatform. Tu développes l'application décrite ci-dessous phase par phase. Tu t'arrêtes à chaque point de validation pour présenter ton travail et attendre l'accord explicite de Mathieu avant de continuer.

L'interface n'est **pas encore définie** : une phase dédiée sert à s'accorder sur le design avant toute implémentation d'écran.

## Contexte et objectif

L'application s'appelle ChessPlus. Elle sert à s'entraîner à la résolution de problèmes d'échecs (tactiques) sur Android, principalement dans les transports en commun. Elle doit fonctionner **entièrement hors connexion**.

- **Source des problèmes :** la base publique de Lichess ([database.lichess.org](https://database.lichess.org/#puzzles)), sous licence CC0, environ 6 millions de lignes en CSV compressé `.zst`.
- **Usage cible :** des sessions courtes, à une main, avec des interruptions fréquentes (arrêt, correspondance, perte de réseau). L'état d'une session en cours ne doit jamais être perdu.
- **Plateformes :** Android d'abord. Une application desktop est envisagée plus tard : tout ce qui n'est pas propre à Android doit vivre dans des modules multiplateformes.
- **Hors périmètre de la v1 :** compte Lichess, synchronisation en ligne, moteur d'analyse (Stockfish), parties contre l'ordinateur.

## Stack technique et contraintes

| Domaine | Choix |
| --- | --- |
| Langage | Kotlin, partout (y compris l'outil de génération de la base) |
| Build | Gradle avec Kotlin DSL et version catalog (`gradle/libs.versions.toml`) |
| Modules partagés | Kotlin Multiplatform, cibles `androidTarget()` et `jvm()` |
| Interface Android | Jetpack Compose, en restant compatible avec Compose Multiplatform pour un futur desktop |
| Base de données | Room en version KMP (2.7 ou plus) avec `BundledSQLiteDriver` |
| Règles des échecs | chesslib (bhlangonijr) derrière une interface, ou implémentation Kotlin pure (voir plus bas) |
| Lecture du CSV | zstd-jni pour décompresser le `.zst` en flux, découpage CSV maison |
| Tests | `kotlin.test` dans `commonTest`, JUnit sur la JVM |

Contraintes :

- `commonMain` ne doit dépendre d'aucune bibliothèque Java ni d'aucune API Android.
- Aucune permission réseau n'est nécessaire en v1.
- Les versions de bibliothèques sont fixées dans le version catalog ; vérifie les dernières versions stables avant de les choisir, sans prendre une version publiée il y a moins de deux semaines.

## Architecture des modules

Le projet compte quatre modules en v1, plus deux prévus pour plus tard. Les dépendances vont toujours de l'application vers le cœur, jamais l'inverse.

| Module | Type | Dépend de | Contenu |
| --- | --- | --- | --- |
| `:core` | KMP (android + jvm) | aucun module | Modèle, codec, règles des échecs, logique des sessions |
| `:data` | KMP (android + jvm) | `:core` | Entités, DAO et `RoomDatabase` |
| `:puzzle-builder` | JVM (plugin `application`) | `:core`, `:data` | Génération de la base à partir du CSV Lichess |
| `:app` | Android | `:core`, `:data` | Écrans, navigation, ViewModels |
| `:ui` (plus tard) | KMP Compose | `:core`, `:data` | Composants partagés avec le desktop |
| `:desktop` (plus tard) | JVM Compose | `:ui` | Application desktop |

Règles :

- `:core` ne connaît ni Room, ni Compose, ni Android.
- Le code commun à Android et à la JVM qui utilise une bibliothèque Java (par exemple chesslib) va dans un source set intermédiaire partagé par les deux cibles (par exemple `jvmShared`), pour ne pas l'écrire deux fois.
- `:puzzle-builder` n'est jamais branché sur le build normal : aucune dépendance depuis `preBuild` ou une autre tâche automatique.

## Données : base Lichess et génération

L'application embarque une base SQLite filtrée d'environ 200 000 à 500 000 problèmes, pour un fichier de moins de 50 Mo, générée par `:puzzle-builder`.

### Colonnes du CSV

En-tête : `PuzzleId,FEN,Moves,Rating,RatingDeviation,Popularity,NbPlays,Themes,GameUrl,OpeningTags,DailyDate`.

| Colonne | Usage |
| --- | --- |
| `PuzzleId` | Conservée : clé primaire, lien `lichess.org/training/{id}` |
| `FEN` | Conservée : position de départ, **avant** le coup de l'adversaire |
| `Moves` | Conservée et encodée : premier coup = adversaire, la suite = solution |
| `Rating` | Conservée : difficulté (Glicko-2, d'environ 400 à plus de 3000) |
| `Themes` | Conservée et encodée : liste séparée par des espaces |
| `RatingDeviation`, `Popularity`, `NbPlays` | Filtrage uniquement, non stockées |
| `GameUrl`, `OpeningTags`, `DailyDate` | Ignorées |

### Exemple de données

Extrait réel du fichier, à utiliser comme données de test pour le parseur, le codec et le déroulement d'un problème :

```csv
PuzzleId,FEN,Moves,Rating,RatingDeviation,Popularity,NbPlays,Themes,GameUrl,OpeningTags,DailyDate
00sHx,q3k1nr/1pp1nQpp/3p4/1P2p3/4P3/B1PP1b2/B5PP/5K2 b k - 0 17,e8d7 a2e6 d7d8 f7f8,1760,80,83,72,mate mateIn2 middlegame short,https://lichess.org/yyznGmXs/black#34,Italian_Game Italian_Game_Classical_Variation,
00sJ9,r3r1k1/p4ppp/2p2n2/1p6/3P1qb1/2NQR3/PPB2PP1/R1B3K1 w - - 5 18,e3g3 e8e1 g1h2 e1c1 a1c1 f4h6 h2g1 h6c1,2671,105,87,325,advantage attraction fork middlegame sacrifice veryLong,https://lichess.org/gyFeQsOE#35,French_Defense French_Defense_Exchange_Variation,1607774862751
00sJb,Q1b2r1k/p2np2p/5bp1/q7/5P2/4B3/PPP3PP/2KR1B1R w - - 1 17,d1d7 a5e1 d7d1 e1e3 c1b1 e3b6,2235,76,97,64,advantage fork long,https://lichess.org/kiuvTFoE#33,Sicilian_Defense Sicilian_Defense_Dragon_Variation,
00sO1,1k1r4/pp3pp1/2p1p3/4b3/P3n1P1/8/KPP2PN1/3rBR1R b - - 2 31,b8c7 e1a5 b7b6 f1d1,998,85,94,293,advantage discoveredAttack master middlegame short,https://lichess.org/vsfFkG0s/black#62,,
```

Lecture du premier problème (`00sHx`) :

1. La FEN indique que les **noirs** ont le trait (`b`). Le premier coup, `e8d7`, est donc joué par les noirs : c'est le coup de l'adversaire, joué automatiquement.
2. Le joueur joue donc les **blancs** : son camp est toujours l'inverse du camp au trait dans la FEN, et le plateau est orienté de son côté.
3. Le joueur doit trouver `a2e6`, l'adversaire répond `d7d8`, puis le joueur conclut par `f7f8`, qui donne mat (thèmes `mate mateIn2`).

Points à respecter dans le parseur et le codec :

- **Champs vides en fin de ligne.** `OpeningTags` et `DailyDate` peuvent être vides, d'où des lignes qui se terminent par une ou deux virgules (`00sO1`). Le découpage doit conserver les champs vides finaux et vérifier que chaque ligne compte exactement 11 champs. Attention : `String.split` de Java les supprime, contrairement au `split` de Kotlin.
- **`DailyDate`** est soit vide, soit un horodatage en millisecondes (`1607774862751`). Il est ignoré.
- **Nombre de coups.** `Moves` contient toujours un nombre pair de coups, puisqu'il commence par le coup de l'adversaire et se termine par un coup du joueur. Un nombre impair est une donnée invalide.
- **Notation UCI.** Un coup fait 4 caractères (`e2e4`) ou 5 pour une promotion (`e7e8q`, pièce en minuscule). Le roque est noté comme un déplacement du roi (`e1g1`, `e8c8`).
- **Thèmes et ouvertures** sont séparés par des espaces à l'intérieur de leur champ ; aucun champ ne contient de virgule ni de guillemet.

Ces quatre problèmes doivent figurer dans les tests de `:core` : décodage, aller-retour du codec, et déroulement complet de chaque solution avec vérification du mat final pour `00sHx`.

### Filtrage

Les seuils sont des paramètres du builder (arguments ou fichier de config), avec des valeurs de départ à ajuster après un premier passage : écart de cote faible, popularité élevée, nombre de parties jouées suffisant. Le builder affiche en fin d'exécution le nombre de problèmes retenus par tranche de 200 points de cote et par thème, pour vérifier que la répartition reste équilibrée.

### Encodage

- **Coups :** chaque coup UCI tient sur 2 octets : case de départ (6 bits), case d'arrivée (6 bits), promotion (3 bits : aucune, cavalier, fou, tour, dame). La suite de coups est stockée en `BLOB`.
- **Thèmes :** une énumération Kotlin dans `:core` et un masque de bits. Le builder recense tous les thèmes présents dans le CSV ; s'il y en a plus de 64, le masque utilise deux colonnes `INTEGER`. Un thème inconnu fait échouer le builder plutôt que d'être ignoré en silence.
- **FEN :** stockée en texte.
- L'encodage et le décodage vivent dans `:core` et sont utilisés à la fois par le builder et par l'application. Ils sont couverts par des tests d'aller-retour.

### Le builder

- Lancement manuel : `./gradlew :puzzle-builder:run --args="<chemin du .csv.zst> [options]"` (`gradlew.bat` sous Windows).
- Lecture en flux du `.zst`, sans charger le fichier en mémoire ni le décompresser sur disque.
- La base est créée **via Room** avec les entités de `:data`, pour que le schéma soit identique par construction à celui attendu par l'application.
- Insertions par lots dans une seule transaction. Mesure l'effet des index sur la durée ; si besoin, supprime-les avant l'insertion et recrée-les à l'identique ensuite.
- Termine par un `VACUUM`, puis écrit le fichier dans `app/src/main/assets/puzzles.db`.
- Le CSV source n'est jamais copié dans le dépôt.

## Logique des échecs et des sessions (`:core`)

### Interface des règles

Les règles des échecs sont accessibles uniquement par une interface déclarée dans `commonMain`, exprimée avec les types du projet (`Square`, `Move`, `Piece`, `Color`…). Aucun type de bibliothèque externe ne doit apparaître en dehors de son implémentation. L'interface couvre :

- créer une position à partir d'une FEN, et exporter la FEN courante ;
- connaître la pièce présente sur une case ;
- lister les coups légaux depuis une case ;
- jouer un coup et l'annuler ;
- savoir si le camp au trait est en échec, mat ou pat.

**Implémentation :** chesslib dans le source set partagé `jvmShared` pour la v1. Les tests de `:core` sont écrits contre l'interface, pour pouvoir la remplacer plus tard par une implémentation Kotlin pure sans rien changer d'autre.

### Déroulement d'un problème

1. Charger la FEN, puis jouer automatiquement le premier coup de `Moves` (le coup de l'adversaire), avec une courte animation.
2. Le joueur joue un coup. S'il correspond au coup attendu, le coup suivant de l'adversaire est joué automatiquement.
3. Un coup différent qui donne **mat** est accepté comme correct, comme sur Lichess.
4. Tout autre coup est une erreur : le problème est compté comme échoué, le joueur peut réessayer ou voir la solution.
5. Le problème est réussi quand le dernier coup de la solution est joué.
6. Les promotions sont gérées : le joueur choisit la pièce, et le choix doit correspondre à la solution (sauf mat alternatif).

### Cote du joueur

Le joueur a une cote locale mise à jour après chaque problème (Glicko-2 ou un Elo simple, à décider pendant la phase de design). Un problème réussi après une erreur ou après avoir vu la solution compte comme un échec.

### Sélection des problèmes

La sélection se fait par tranche de cote autour du niveau du joueur, éventuellement filtrée par thème, sans `ORDER BY RANDOM()` sur toute la table. Un problème déjà réussi n'est pas reproposé ; un problème échoué peut revenir plus tard.

## Fonctionnalités

La v1 couvre l'entraînement hors ligne et le suivi de progression. La liste ci-dessous fixe le **quoi** ; le **comment** (écrans, navigation, visuel) est décidé pendant la phase de design.

**Indispensables en v1 :**

- jouer un problème : échiquier interactif (toucher-toucher et glisser-déposer), surlignage des coups légaux et du dernier coup joué, orientation du plateau selon le camp du joueur ;
- aide : voir la solution coup par coup, recommencer le problème ;
- mode d'entraînement continu, adapté à la cote du joueur ;
- filtre par thème et par tranche de difficulté ;
- historique : problèmes réussis, échoués, et possibilité de rejouer les échecs ;
- statistiques simples : évolution de la cote, taux de réussite par thème ;
- reprise exacte de l'état après une interruption (appel, mise en arrière-plan, redémarrage du processus) ;
- lien pour ouvrir le problème sur Lichess, quand le réseau est disponible ;
- thème clair et sombre.

**Envisagées après la v1 :** séries chronométrées, objectif quotidien, packs de problèmes téléchargeables par tranche de cote, application desktop.

L'historique de l'utilisateur est stocké dans des tables séparées de la base des problèmes, pour pouvoir remplacer celle-ci par une nouvelle version sans perdre la progression.

## Phase de design : s'accorder sur l'interface

Aucun écran n'est implémenté avant que Mathieu ait validé le design. Cette phase produit des propositions, des maquettes et un document de décisions, pas du code de production.

### Déroulement

1. **Questions préalables.** Avant de proposer quoi que ce soit, pose les questions dont les réponses changent le design (par exemple : usage à une main ou à deux, préférence d'ambiance, applications d'échecs appréciées ou rejetées). Pose-les en une fois, pas une par une.
2. **Inventaire des écrans et navigation.** Liste les écrans nécessaires aux fonctionnalités, avec un schéma de navigation entre eux. Fais valider avant d'aller plus loin.
3. **Deux ou trois directions visuelles contrastées.** Pour chacune : palette (claire et sombre), typographie, style du plateau et des pièces, ton général. Présente-les sur l'écran de jeu, qui est le plus important.
4. **Maquettes.** Une fois la direction choisie, réalise les maquettes de chaque écran sous forme de `@Preview` Compose statiques avec des données factices, consultables dans Android Studio. Ces composables jetables vivent dans un package `design.preview` et ne sont pas réutilisés tels quels.
5. **Itérations** jusqu'à validation explicite, écran par écran.
6. **Document de décisions.** Consigne chaque choix validé dans `docs/DESIGN.md` : navigation, palette avec ses valeurs, typographie, composants, comportements. Ce fichier fait foi pour la suite.

### Décisions à prendre pendant cette phase

- disposition de l'écran de jeu en portrait, avec les commandes à portée de pouce ;
- retour visuel en cas de bon coup, de mauvais coup et de problème réussi ; sons et vibrations (activables ou non) ;
- style des pièces et du plateau, en vérifiant la **licence** de tout jeu de pièces repris d'un projet existant ;
- écran d'accueil : reprise directe de l'entraînement ou tableau de bord ;
- présentation des thèmes : noms traduits en français, regroupements ;
- algorithme de cote du joueur ;
- prise en charge des tablettes et du mode paysage, ou non en v1.

## Conventions de code

Ces conventions s'appliquent à tout le code Kotlin du projet.

| Élément | Règle | Exemple |
| --- | --- | --- |
| Constantes (`const val`, constantes de `companion object` ou de premier niveau) | camelCase préfixé par `k` minuscule | `kMoveBits`, `kMaxRating` |
| Variables locales | préfixe `v` | `vParts`, `vResult`, `vLegalMoves` |
| Paramètres de fonction | préfixe `in` | `inFen`, `inPuzzleId` |
| Indentation | tabulations, largeur 4 | |
| Commentaires | un commentaire KDoc sur chaque classe et chaque fonction | |

Autres règles :

- Les commentaires et les noms de commits sont en français ; les identifiants restent en anglais.
- Configure `.editorconfig` pour les tabulations, afin que l'IDE et le formateur respectent la règle.
- Les règles spécifiques à Java (préfixe `f` des membres, format Whitesmith) ne s'appliquent pas à ce projet Kotlin.

## Plan de développement

Le développement suit six phases dans l'ordre. Chacune se termine par un point de validation : tu présentes ce qui a été fait, comment le vérifier, et tu attends l'accord de Mathieu.

1. **Mise en place du projet.** Modules, Gradle, version catalog, `.editorconfig`, source set `jvmShared`.
    - Validation : le projet compile, une application vide se lance, les tests (vides) passent.
2. **Cœur (`:core`).** Modèle, codec des coups et des thèmes, interface des règles et implémentation chesslib, logique d'une session de problème, calcul de cote.
    - Validation : tests d'aller-retour du codec, tests des règles sur des positions connues (roque, prise en passant, promotion, mat, pat), tests du déroulement d'un problème réel de la base, y compris un mat alternatif.
3. **Données et génération (`:data`, `:puzzle-builder`).** Entités, DAO, base Room, builder.
    - Validation : Mathieu lance le builder sur le vrai CSV. Tu lui fournis la durée d'exécution, la taille du fichier et la répartition des problèmes, et il fixe les seuils de filtrage définitifs.
4. **Design.** Déroulement décrit dans la section dédiée, jusqu'à un `docs/DESIGN.md` validé.
5. **Écrans.** Implémentation selon `docs/DESIGN.md`, en commençant par l'écran de jeu, puis l'entraînement continu, les filtres, l'historique et les statistiques. Une validation par écran.
6. **Finitions.** Reprise après interruption et mort du processus, mode avion, performance sur un appareil d'entrée de gamme, build release avec R8, vérification de la taille de l'APK.
    - Validation : test complet sur un vrai téléphone, hors connexion.

## Règles de travail pour l'agent

- Ne passe jamais à la phase suivante sans validation explicite de Mathieu.
- Quand une décision n'est pas couverte par ce document ou par `docs/DESIGN.md`, pose la question au lieu de choisir seul. Regroupe les questions plutôt que d'interrompre à chaque fois.
- Si une contrainte de ce document te semble mauvaise ou bloquante, dis-le et propose une alternative avant de t'en écarter.
- Écris les tests en même temps que le code, pas après. Lance-les avant chaque point de validation.
- Fais des commits petits et cohérents, un par étape logique.
- Tiens à jour un fichier `docs/PROGRESS.md` : phase en cours, ce qui est fait, ce qui reste, décisions prises. C'est ce fichier qui permet de reprendre le travail dans une nouvelle session.
- N'ajoute aucune dépendance qui ne figure pas dans ce document sans l'avoir proposée et fait valider.
