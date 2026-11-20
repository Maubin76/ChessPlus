# Design de ChessPlus

Ce document fait foi pour l'implémentation des écrans (phase 5). Il ne contient que des choix validés par Mathieu ; les propositions en attente sont dans `docs/PROGRESS.md`.

## Contexte d'usage (validé le 2026-11-20)

- Usage à **une main**, au pouce, dans les transports : commandes en bas de l'écran, échiquier juste au-dessus.
- Appareil de référence : **Samsung Galaxy S24** (écran 6,2 pouces, 1080 × 2340, environ 360 × 780 dp).
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

## Échiquier et pièces (validé le 2026-11-20)

- Pièces de style **classique Staunton**.
- **Coordonnées** affichées dans les cases (lettres sur la rangée du bas, chiffres sur la colonne de gauche), en fondu, discrètes.
- Plateau orienté du côté du joueur.

## Pendant un problème (validé le 2026-11-20)

- Cote et thèmes du problème **masqués** jusqu'à la fin du problème.
- **Vibration courte activée** par défaut, **sons désactivés** par défaut ; les deux réglables.
- Après un problème terminé : **bouton « Suivant »**, pas d'enchaînement automatique.

## Écrans (validé le 2026-11-20)

- Écran d'accueil : **tableau de bord** destiné à accueillir des statistiques plus tard ; en v1, il affiche seulement le **nombre de problèmes résolus**.
- Premier lancement : l'utilisateur **choisit son niveau** de départ.
- Thèmes : noms en français, regroupés en familles (motifs tactiques, mats, finales, phase de jeu, longueur).

## Cote (validé le 2026-11-20)

- Algorithme **Glicko-2** (`Glicko2RatingSystem`).
- Un problème **déjà joué** ne modifie plus la cote quand il est rejoué (tentative non notée).
