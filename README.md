# ChessPlus

Application Android d'entraînement aux problèmes d'échecs (tactiques), entièrement hors connexion, à partir de la base publique de problèmes de Lichess.

- Spécification : [docs/SPEC.md](docs/SPEC.md)
- Design : [docs/DESIGN.md](docs/DESIGN.md)
- Avancement : [docs/PROGRESS.md](docs/PROGRESS.md)

## Modules

| Module | Rôle |
| --- | --- |
| `:core` | Kotlin Multiplatform (Android + JVM) : modèle, codec des coups et des thèmes, règles des échecs, déroulement d'un problème, cote Glicko-2 |
| `:data` | Kotlin Multiplatform : bases Room (problèmes, utilisateur), sélection des problèmes |
| `:puzzle-builder` | JVM : génère `app/src/main/assets/puzzles.db` à partir du CSV Lichess |
| `:app` | Android (Jetpack Compose) : écrans et navigation |

## Construire

Prérequis : JDK 21, SDK Android (compileSdk 37).

La base des problèmes n'est pas versionnée. Il faut d'abord la générer à partir du fichier `lichess_db_puzzle.csv.zst`, téléchargeable sur [database.lichess.org](https://database.lichess.org/#puzzles) :

```
./gradlew :puzzle-builder:run --args="<chemin>/lichess_db_puzzle.csv.zst"
```

Puis :

```
./gradlew build                 # tests et APK
./gradlew :app:installDebug     # installation sur un téléphone branché
```

## Ressources tierces

| Ressource | Auteur | Licence |
| --- | --- | --- |
| Problèmes d'échecs (base générée, non versionnée) | Lichess | CC0 |
| Pièces « cburnett » (`app/src/main/res/drawable/piece_*.xml`) | Colin M.L. Burnett, via lichess-org/lila | GPLv2+ |
| Police Inter (`app/src/main/res/font/inter.ttf`) | The Inter Project Authors | SIL Open Font License 1.1 |
| Icônes Material Symbols (`app/src/main/res/drawable/ic_*.xml`) | Google | Apache 2.0 |
| chesslib (dépendance) | Ben-Hur Carlos Vieira Langoni Junior | Apache 2.0 |

Le code de ChessPlus n'est publié sous aucune licence : tous droits réservés.
