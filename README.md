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

## Intégration continue

`.github/workflows/ci.yml` (même organisation que PatrimoinePlus), à chaque push sur `main` :

1. tests JVM (`:core`, `:data`, `:puzzle-builder`, `:app`) et APK debug ;
2. génération de la base à partir du CSV Lichess (téléchargé une fois par mois, en cache), APK release (R8) installé et lancé sur un émulateur Android 16 (`ci/release-smoke-android.sh`) ;
3. numéro de version : celui de `gradle.properties` s'il n'est pas encore publié, sinon +0.1 (commit de la CI) ;
4. APK signé et release GitHub `vX.Y`.

La signature utilise trois secrets du dépôt (Settings › Secrets and variables › Actions) ; sans eux, la release échoue volontairement plutôt que d'être signée avec la clé de debug :

| Secret | Contenu |
| --- | --- |
| `KEYSTORE_BASE64` | fichier de clé encodé en base64 (`base64 -w0 chessplus.jks`) |
| `KEY_ALIAS` | alias de la clé |
| `KEY_PASSWORD` | mot de passe (identique pour le fichier et la clé) |

Création d'une clé : `keytool -genkeypair -v -keystore chessplus.jks -alias chessplus -keyalg RSA -keysize 4096 -validity 36500`.

## Ressources tierces

| Ressource | Auteur | Licence |
| --- | --- | --- |
| Problèmes d'échecs (base générée, non versionnée) | Lichess | CC0 |
| Pièces « cburnett », style Classique (`app/src/main/res/drawable/piece_w*.xml`, `piece_b*.xml`) | Colin M.L. Burnett, via lichess-org/lila | GPLv2+ / BSD |
| Pièces des styles Géométrique (`piece_geo_*.xml`) et Lettres | création ChessPlus | — |
| Sons (`app/src/main/res/raw/sound_*.wav`) | création ChessPlus (synthèse) | — |
| Police Inter (`app/src/main/res/font/inter.ttf`) | The Inter Project Authors | SIL Open Font License 1.1 |
| Police Space Grotesk (`app/src/main/res/font/space_grotesk.ttf`) | The Space Grotesk Project Authors | SIL Open Font License 1.1 |
| Police JetBrains Mono (`app/src/main/res/font/jetbrains_mono.ttf`) | The JetBrains Mono Project Authors | SIL Open Font License 1.1 |
| Icônes au trait (`app/src/main/res/drawable/ic_ln_*.xml`) | design system ChessPlus | — |
| chesslib (dépendance) | Ben-Hur Carlos Vieira Langoni Junior | Apache 2.0 |

Le code de ChessPlus n'est publié sous aucune licence : tous droits réservés.
