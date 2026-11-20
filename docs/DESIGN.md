# Design de ChessPlus

Ce document fait foi pour l'implémentation des écrans (phase 5). Il ne contient que des choix validés par Mathieu ; les propositions en attente sont dans `docs/PROGRESS.md`.

## Contexte d'usage (validé le 2026-11-20)

- Usage à **une main**, au pouce, dans les transports : commandes en bas de l'écran, échiquier juste au-dessus.
- Appareil de référence : **Samsung Galaxy S24** (écran 6,2 pouces, 1080 × 2340 px, densité 2,8125, soit 384 × 832 dp).
- **Portrait uniquement**, téléphone uniquement en v1 (ni paysage, ni tablette).
- Application **personnelle** (pas de publication) : les licences des ressources reprises doivent permettre l'usage personnel ; on cite les auteurs dans l'application.
- Interface en **français uniquement** ; textes dans les ressources Android.

## Ambiance (validé le 2026-11-20)

- **Minimaliste, monochrome noir et blanc**, d'après la référence fournie par Mathieu (maquettes « Calm » de Nickelfox) :
	- fond gris très clair, cartes blanches à grands rayons d'angle, ombres très douces ;
	- boutons principaux pleins noirs, ronds ou en pastille ;
	- tuiles d'icônes carrées arrondies, noires (sélectionnées) ou gris clair ;
	- titres en police à empattements, élégante, mêlant graisse normale et grasse ; textes courants et libellés en sans-serif ; sous-titres gris, légèrement espacés ;
	- une variante de carte sombre (fond noir, contenu blanc) pour mettre en avant un élément.
- Applications appréciées : **Chess.com**.
- Thème clair ou sombre : **suit le système** par défaut.

## Direction visuelle retenue : « Accent pleine largeur » (validé le 2026-11-20)

Référence : `docs/design/suggestions/suggestion-10-pleine-largeur.png`.

- Monochrome, avec **une seule exception** : un vert pour la réussite et un rouge pour l'erreur, réservés au retour visuel (inspiration Chess.com).
- **Échiquier bord à bord** : toute la largeur de l'écran (384 dp sur le S24, cases de 48 dp), sans carte ni coins arrondis.
- Police unique : **Inter** (SIL OFL), graisses 400 à 800.
- Pièces : **cburnett** (Colin M.L. Burnett, GPLv2+), auteur cité dans les crédits.
- Icônes : **Material Symbols Rounded** (Apache 2.0).

### Palette

| Rôle | Clair | Sombre |
| --- | --- | --- |
| Fond | `#F4F5F6` | `#0F1011` |
| Surface (cartes, tuiles) | `#FFFFFF` | `#1C1D1F` |
| Texte | `#111111` | `#F2F3F4` |
| Texte secondaire | `#8A8E95` | `#8A8E95` |
| Bouton principal | `#111111`, icône `#F4F5F6` | `#F2F3F4`, icône `#0F1011` |
| Réussite (accent) | `#3E8E5A` | `#5DB37C` |
| Erreur | `#C2483D` | `#E0675C` |
| Case claire | `#EEF0F1` | `#A3A8AE` |
| Case foncée | `#B5BAC0` | `#696E75` |
| Dernier coup | accent à 24 % d'opacité | accent à 31 % |
| Pièce sélectionnée | `#111111` à 24 % | `#FFFFFF` à 31 % |
| Coups légaux (points) | `#111111` à 27 % | `#111111` à 31 % |
| Coordonnées | sur case claire `#111111` à 27 %, sur case foncée `#FFFFFF` à 59 % | `#111111` à 31 % / `#FFFFFF` à 43 % |

### Typographie (Inter)

| Usage | Taille | Graisse |
| --- | --- | --- |
| Message de retour (« Bon coup ! ») | 26 sp | 800 |
| Titre de barre | 16 sp | 600 |
| Corps, consigne | 14 sp | 400 |
| Cote du joueur | 14 sp | 700 |
| Libellés de commandes | 11 sp | 500 à 600 |
| Coordonnées | 9 sp | 600 |

### Écran de jeu

- Haut : retour, titre « Entraînement », cote du joueur.
- Message d'état : consigne (« Les blancs jouent »), puis retour visuel en grand (« Bon coup ! » en vert avec icône, erreur en rouge).
- Échiquier pleine largeur ; bon coup signalé par une **pastille ✓ verte** sur la case d'arrivée et le dernier coup surligné en vert.
- Bas : tuile « Recommencer », gros bouton rond « Indice », tuile « Suivant » (grisée tant que le problème n'est pas terminé).

## Échiquier et pièces (validé le 2026-11-20)

- Pièces de style **classique Staunton**.
- **Coordonnées** affichées dans les cases (lettres sur la rangée du bas, chiffres sur la colonne de gauche), en fondu, discrètes.
- Plateau orienté du côté du joueur.

## Pendant un problème (validé le 2026-11-20)

- Cote et thèmes du problème **masqués** jusqu'à la fin du problème.
- **Vibration courte activée** par défaut, **sons désactivés** par défaut ; les deux réglables.
- Après un problème terminé : **bouton « Suivant »**, pas d'enchaînement automatique.

## Écrans et navigation (validé le 2026-11-20)

| # | Écran | Contenu |
| --- | --- | --- |
| 1 | Choix du niveau | Premier lancement : Débutant 800, Intermédiaire 1200, Confirmé 1600, Avancé 2000, ou « Je connais ma cote » (saisie libre) ; modifiable dans les réglages |
| 2 | Accueil | Tableau de bord : nombre de problèmes résolus ; carte « Reprendre le problème » si un problème est en cours ; bouton « S'entraîner » ; accès à l'entraînement ciblé ; icône des réglages |
| 3 | Problème | Plein écran : échiquier, consigne, retour visuel, commandes en bas (Recommencer, Indice = solution coup par coup, Suivant) ; 3a promotion (choix de la pièce) ; 3b bilan en volet bas (résultat, cote et thèmes révélés, évolution de la cote, lien Lichess, Suivant) |
| 4 | Entraînement ciblé | Thèmes par familles ; difficulté relative (plus facile, à mon niveau, plus difficile) **ou** tranche précise ; bouton « Commencer » |
| 5 | Historique | Onglets Tous, Réussis, Échoués ; une ligne relance le problème, sans effet sur la cote |
| 6 | Statistiques | Courbe de la cote, taux de réussite par thème (écran séparé, l'accueil reste sobre) |
| 7 | Réglages | Thème, vibration, sons, coordonnées, niveau, crédits |

- **Barre de navigation en bas** à trois onglets : Accueil, Historique, Statistiques. Elle est masquée sur l'écran Problème (plein écran), le choix du niveau et les réglages.
- Réglages et entraînement ciblé s'ouvrent depuis l'accueil.
- L'écran Problème est commun à l'entraînement continu, à l'entraînement ciblé (filtres rappelés en haut) et au rejeu depuis l'historique ; « Suivant » reste dans le même mode.
- Retour depuis un problème : retour à l'accueil, le problème est conservé (carte « Reprendre »).
- Reprise : après une fermeture par le système, retour direct sur le problème au coup près ; après un relancement volontaire, accueil avec « Reprendre ».

### Détails

- Écran d'accueil : **tableau de bord** destiné à accueillir des statistiques plus tard ; en v1, il affiche seulement le **nombre de problèmes résolus**.
- Premier lancement : l'utilisateur **choisit son niveau** de départ.
- Thèmes : noms en français, regroupés en familles (motifs tactiques, mats, finales, phase de jeu, longueur).

## Cote (validé le 2026-11-20)

- Algorithme **Glicko-2** (`Glicko2RatingSystem`).
- Un problème **déjà joué** ne modifie plus la cote quand il est rejoué (tentative non notée).
