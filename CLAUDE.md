# ChessPlus

Application Android d'entraînement aux problèmes d'échecs, hors connexion, à partir de la base Lichess.

- Spécification de référence : [docs/SPEC.md](docs/SPEC.md). À lire en entier avant toute tâche.
- État d'avancement et décisions : [docs/PROGRESS.md](docs/PROGRESS.md). À tenir à jour à chaque étape.
- Décisions de design (à partir de la phase 4) : `docs/DESIGN.md`.

## Commandes

- Build complet et tests : `./gradlew build`
- Tests d'un module : `./gradlew :core:allTests`, `./gradlew :data:allTests`, `./gradlew :puzzle-builder:test`, `./gradlew :app:testDebugUnitTest`
- APK debug : `./gradlew :app:assembleDebug`
- Génération de la base (manuel uniquement) : `./gradlew :puzzle-builder:run --args="<chemin du .csv.zst> [options]"`

## Règles essentielles

- Ne jamais passer à la phase suivante sans validation explicite de Mathieu.
- Conventions : constantes `kNom`, variables locales `vNom`, paramètres `inNom`, tabulations (largeur 4), KDoc sur chaque classe et fonction, commentaires et commits en français.
- `commonMain` ne dépend d'aucune bibliothèque Java ni API Android ; le code qui utilise chesslib va dans `jvmShared`.
- Aucune nouvelle dépendance sans validation.
- Ne pas lancer d'émulateur ni de fenêtre, ne pas installer sur le téléphone sans autorisation.
