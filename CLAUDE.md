# ChessPlus

Application Android d'entraînement aux puzzles d'échecs, hors connexion, à partir de la base Lichess. Version publiée : 2.0.0 (V1 en 1.0).

- Spécification de référence : [docs/SPEC.md](docs/SPEC.md). À lire en entier avant toute tâche.
- État d'avancement et décisions : [docs/PROGRESS.md](docs/PROGRESS.md). À tenir à jour à chaque étape.
- Décisions de design : [docs/DESIGN.md](docs/DESIGN.md) (section « Design system ChessPlus », puis les décisions datées).

## Commandes

- Build complet et tests (vérifie aussi les captures d'écran de référence) : `./gradlew build`
- Tests d'un module : `./gradlew :core:allTests`, `./gradlew :data:allTests`, `./gradlew :puzzle-builder:test`, `./gradlew :app:testDebugUnitTest`
- Captures d'écran de référence (`app/src/screenshotTestDebug/reference`) : `./gradlew :app:updateDebugScreenshotTest` après tout changement visuel voulu
- Installation sur le téléphone (sur autorisation) : `./gradlew :app:installRelease`, jamais `installDebug` (le téléphone porte la release, signée par la clé décrite dans `~/.gradle/gradle.properties` ; ne jamais afficher ces mots de passe ni versionner la clé)
- Génération de la base (manuel uniquement) : `./gradlew :puzzle-builder:run --args="<chemin du .csv.zst> [options]"`
- Visuels du README (`docs/images`) : `python ci/vitrines.py`, à partir des captures de référence

## Branches et publication

- Travailler sur une branche dédiée (jamais directement sur `main`), et y pousser.
- Toute mise à jour de `main`, même forcée, publie une release par la CI : ne rien fusionner dans `main` ni publier sans l'accord explicite de Mathieu.
- Version dans `gradle.properties` (`chessplus.version`, `chessplus.versionCode`) : publiée telle quelle si son tag n'existe pas, sinon augmentée par la CI (2.0.0 → 2.1.0).

## Design system

- Les maquettes de référence sont dans `chessplus-design/`, dossier local non versionné (exclu par `.git/info/exclude`) : à respecter scrupuleusement, ne jamais le commiter.
- Couleurs, typographie, espacements et rayons passent par `ChessPlusTheme` (`.colors`, `.typography`, `.spacing`, `.radius`) et les composants de `ui/components/Components.kt`, jamais par des valeurs en dur.
- `ChessPlusTokens.kt` est généré et copié tel quel depuis les maquettes : il est exempté des conventions de nommage.

## Règles essentielles

- Conventions : constantes `kNom`, variables locales `vNom`, paramètres `inNom`, tabulations (largeur 4), KDoc sur chaque classe et fonction, commentaires et commits en français.
- Textes de l'application : tutoiement ; « puzzle » (pas « problème ») et « classement » (pas « cote ») ; espace fine insécable (U+202F) avant `%` et dans les nombres ; pas de point d'exclamation.
- `commonMain` ne dépend d'aucune bibliothèque Java ni API Android ; le code qui utilise chesslib va dans `jvmShared`.
- Aucune nouvelle dépendance sans validation.
- Ne pas lancer d'émulateur ni de fenêtre, ne pas contrôler la souris, ne pas installer sur le téléphone sans autorisation.
