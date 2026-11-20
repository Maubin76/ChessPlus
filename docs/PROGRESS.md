# Avancement de ChessPlus

## Phase en cours

**Phase 1 — Mise en place du projet** : terminée, en attente de validation par Mathieu.

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

## Reste à faire

- Phase 2 : cœur (`:core`).
- Phase 3 : données et génération (`:data`, `:puzzle-builder`).
- Phase 4 : design.
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
- **SDK** : `compileSdk` 37 (exigé par core-ktx 1.19 et Compose 1.12), `targetSdk` 36, `minSdk` 26 — à confirmer.
- **JVM** : toolchain 21, bytecode cible 17.
- **Package** : `com.chessplus` (`.core`, `.data`, `.builder`, `.app`) — à confirmer.
- **Spécification** : le prompt d'origine a été déplacé dans `docs/SPEC.md`, référencé depuis `CLAUDE.md`.

## Questions ouvertes

- Confirmer `minSdk` 26, `targetSdk` 36 et l'identifiant `com.chessplus.app`.
- Paramètres des fonctions redéfinies (`onCreate(inSavedInstanceState)`) : le préfixe `in` provoque un avertissement Kotlin (nom différent du supertype). Garder le préfixe ou conserver le nom d'origine pour les `override` ?
