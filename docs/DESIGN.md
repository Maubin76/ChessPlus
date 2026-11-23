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

- Monochrome à l'origine, avec un vert pour la réussite et un rouge pour l'erreur. **Version B verte** (choisie par Mathieu le 2026-11-23 pour s'accorder à l'icône, `docs/design/vert/propositions-vert.png`) : boutons principaux, carte « Reprendre », capsule de navigation, bouton « Indice », puces sélectionnées et interrupteurs en vert ; échiquier aux tons verts, dernier coup surligné en jaune ; fonds et cartes inchangés.
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
| Bouton principal (vert) | `#2F7D4F`, texte et icône `#FFFFFF` | `#5DB37C`, texte et icône `#0F1011` |
| Réussite (accent) | `#3E8E5A` | `#5DB37C` |
| Erreur | `#C2483D` | `#E0675C` |
| Case claire | `#E7EFE4` | `#A9C2AE` |
| Case foncée | `#7FA98A` | `#5F8768` |
| Dernier coup | jaune `#F2E66B` à 50 % | jaune `#E8D85C` à 50 % |
| Pièce sélectionnée | `#111111` à 24 % | `#FFFFFF` à 31 % |
| Coups légaux (points) | `#111111` à 27 % | `#111111` à 31 % |
| Coordonnées | sur case claire `#557A61` à 50 %, sur case foncée `#FFFFFF` à 70 % | `#405C49` à 50 % / `#FFFFFF` à 60 % |
| Sous-titre de la carte « Reprendre » | texte de la carte à 80 % | texte de la carte à 80 % |

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

## Maquettes (validées le 2026-11-20)

- Sources : `app/src/main/kotlin/com/chessplus/app/design/preview/` (jetables, ne pas réutiliser tels quels).
- Captures : `./gradlew :app:updateDebugScreenshotTest` (plugin `com.android.compose.screenshot`, alpha, développement uniquement), planches dans `docs/design/maquettes/` (`maquettes-jeu.png`, `maquettes-ecrans.png`).

### Écran de jeu

| État | Message (titre / sous-titre) | Échiquier | Commandes |
| --- | --- | --- | --- |
| Au trait | « Les blancs jouent » (ou noirs) / « Trouvez le meilleur coup », en couleur de texte | dernier coup surligné, pièce sélectionnée, points sur les cases possibles ; aide discrète sous l'échiquier : « Touchez une pièce puis sa case, ou faites-la glisser » | Suivant grisé |
| Bon coup | ✓ « Bon coup ! » en vert / « Les noirs réfléchissent… » | pastille ✓ verte sur la case d'arrivée, coup surligné en vert | Suivant grisé |
| Mauvais coup | ✕ « Pas ce coup-là » en rouge / « Réessayez, ou demandez un indice » | la pièce va sur la case visée avec une pastille ✕ rouge, puis revient en glissant après 0,9 s (modifié par Mathieu le 2026-11-20) ; mention : « Le problème compte comme échoué, mais vous pouvez le terminer » | Suivant grisé |
| Promotion | inchangé | voile sombre et carte centrée « Promotion — Choisissez la nouvelle pièce » : dame, tour, fou, cavalier | — |
| Terminé | ✓ « Mat ! » ou « Problème terminé » | position finale | Suivant actif (tuile noire) |

- Bilan (modifié par Mathieu le 2026-11-20 : il ne doit pas cacher l'échiquier) : le résultat remplace le message au-dessus de l'échiquier (« Problème réussi » en vert ou « Problème échoué » en rouge, avec la phrase de détail) ; sous l'échiquier, deux encadrés « Ma cote » (valeur et variation colorée, ou « inchangée ») et « Cote du problème », les thèmes en puces sur une ligne défilante ; pas de lien Lichess (retiré par Mathieu le 2026-11-20, alors que la spécification le prévoyait) ; en bas, tuile « Recommencer » et gros bouton noir « Suivant ».
- Animations des pièces (demandées par Mathieu le 2026-11-20) : 300 ms, accélération puis ralentissement ; toutes les pièces qui changent de case glissent (coups du joueur et de l'adversaire, indice, roque, promotion, recommencer, annulation d'un mauvais coup) ; une pièce prise s'efface en fondu ; pas d'animation après un glisser-déposer (la pièce est déjà sous le doigt).

### Autres écrans

- **Choix du niveau** : « BIENVENUE », titre « Quel est votre niveau ? », phrase « Votre cote de départ s'ajustera ensuite après chaque problème. » ; cartes Débutant (« Je connais les règles », 800), Intermédiaire (« Je joue de temps en temps », 1 200), Confirmé (« Je joue régulièrement », 1 600), Avancé (« Je joue en club ou en ligne », 2 000), carte sélectionnée bordée de noir avec ✓ ; ligne « Je connais ma cote » avec saisie ; bouton « Commencer ».
- **Accueil** : titre « ChessPlus » et bouton réglages ; carte « Problèmes résolus » (grand nombre, « Votre cote : … ») ; carte noire « Reprendre le problème » avec vignette de la position et « Coup x sur y · il y a … » si un problème est en cours ; en bas, « Entraînement ciblé » (bouton clair) et « S'entraîner » (bouton noir) ; barre de navigation.
- **Entraînement ciblé** : « Difficulté » en contrôle segmenté (Plus facile, Mon niveau, Plus difficile) et interrupteur « Tranche précise » ; « Thèmes » en puces par famille (sélection en noir), avec à droite du nom de chaque famille un bouton « Tout sélectionner » / « Tout désélectionner » (ajouté à la demande de Mathieu le 2026-11-20) ; en bas, nombre de problèmes correspondants et bouton « Commencer ».
- **Historique** : onglets Tous, Réussis, Échoués ; groupes par jour (« AUJOURD'HUI », « HIER ») ; lignes avec vignette, thèmes, cote et date, icône ✓/✕ et variation de cote ; mention « Touchez un problème pour le rejouer (sans effet sur la cote) ».
- **Statistiques** : carte « Ma cote » (grand nombre, variation sur la période, courbe, période 7 jours / 30 jours / Tout) ; carte « Réussite par thème » avec barres et pourcentages.
- **Réglages** : Thème (Système, Clair, Sombre) ; Vibration, Sons, Coordonnées ; Niveau de départ ; Crédits (base Lichess CC0, pièces cburnett GPLv2+, Inter OFL, Material Symbols Apache 2.0) ; numéro de version.

### Noms français des thèmes (73)

La famille « Évaluation et origine » s'ajoute aux cinq familles validées pour les thèmes qui n'entrent dans aucune.

| Famille | Thèmes |
| --- | --- |
| **Motifs tactiques** (29) | Fourchette (`fork`), Clouage (`pin`), Enfilade (`skewer`), Attaque à la découverte (`discoveredAttack`), Échec à la découverte (`discoveredCheck`), Échec double (`doubleCheck`), Déviation (`deflection`), Attraction (`attraction`), Sacrifice (`sacrifice`), Pièce en prise (`hangingPiece`), Coup intermédiaire (`intermezzo`), Élimination du défenseur (`capturingDefender`), Dégagement (`clearance`), Interception (`interference`), Attaque en rayons X (`xRayAttack`), Pièce piégée (`trappedPiece`), Coup tranquille (`quietMove`), Coup défensif (`defensiveMove`), Zugzwang (`zugzwang`), Roi exposé (`exposedKing`), Attaque à l'aile roi (`kingsideAttack`), Attaque à l'aile dame (`queensideAttack`), Attaque sur f2/f7 (`attackingF2F7`), Pion avancé (`advancedPawn`), Promotion (`promotion`), Sous-promotion (`underPromotion`), Roque (`castling`), Prise en passant (`enPassant`), Coup colinéaire (`collinearMove`) |
| **Mats** (25) | Mat (`mate`), Mat en 1 (`mateIn1`), Mat en 2 (`mateIn2`), Mat en 3 (`mateIn3`), Mat en 4 (`mateIn4`), Mat en 5 ou plus (`mateIn5`), Mat du couloir (`backRankMate`), Mat étouffé (`smotheredMate`), Mat d'Anastasia (`anastasiaMate`), Mat arabe (`arabianMate`), Mat de Balestra (`balestraMate`), Mat des cochons aveugles (`blindSwineMate`), Mat de Boden (`bodenMate`), Mat du coin (`cornerMate`), Mat des deux fous (`doubleBishopMate`), Mat en queue d'aronde (`dovetailMate`), Mat de l'épaulette (`epauletteMate`), Mat du crochet (`hookMate`), Mat de la boîte (`killBoxMate`), Mat de Morphy (`morphysMate`), Mat de l'Opéra (`operaMate`), Mat de Pillsbury (`pillsburysMate`), Mat en queue d'hirondelle (`swallowstailMate`), Mat du triangle (`triangleMate`), Mat de Vuković (`vukovicMate`) |
| **Finales** (6) | Finale de pions (`pawnEndgame`), Finale de tours (`rookEndgame`), Finale de fous (`bishopEndgame`), Finale de cavaliers (`knightEndgame`), Finale de dames (`queenEndgame`), Finale dame et tour (`queenRookEndgame`) |
| **Phase de jeu** (3) | Ouverture (`opening`), Milieu de partie (`middlegame`), Finale (`endgame`) |
| **Longueur** (4) | Un coup (`oneMove`), Court (`short`), Long (`long`), Très long (`veryLong`) |
| **Évaluation et origine** (6) | Avantage (`advantage`), Gain décisif (`crushing`), Égalisation (`equality`), Partie de maître (`master`), Maître contre maître (`masterVsMaster`), Super grand maître (`superGM`) |

## Icône de l'application (validée le 2026-11-23)

- Proposition 9 « Halo » (`docs/design/icones/icone-09.png`) : fond en dégradé radial vert (`#4FA06B` au centre, `#1F4D33` au bord), cavalier cburnett blanc au contour vert foncé (`#1E4A2E`) avec une ombre douce, roi et dame en silhouettes vertes (`#2E6B48`) inclinées de 12° vers l'extérieur.
- Icône adaptative (`res/mipmap-anydpi/ic_launcher.xml`) : fond et premier plan en PNG pour chaque densité, contenu dans la zone visible de 72 dp ; calque monochrome (silhouette du cavalier) pour les icônes à thème d'Android.

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
- Forme de la navigation (modifiée par Mathieu le 2026-11-20, d'après PatrimoinePlus) : pilule flottante centrée, 12 dp au-dessus de la barre système, hauteur 64 dp, 6 dp de marge interne, 8 dp entre onglets ; au repos, icône seule en gris ; l'onglet actif devient une capsule à l'encre qui s'élargit pour afficher son nom ; ombre douce en clair, liseré de 1 dp en sombre.
- Accueil : boutons « Ciblé » et « S'entraîner » côte à côte, hauteur 48 dp, texte en 14 sp sur une ligne, le bouton principal plus large ; carte « Reprendre » (titre court) avec la progression dessous (modifiés par Mathieu le 2026-11-20 : libellés trop longs avec la police agrandie du téléphone).
- Textes : tous les libellés de boutons et de commandes tiennent sur une ligne, vérifiés avec une police agrandie de 30 %.
- Transitions (demandées par Mathieu le 2026-11-20) : entre onglets, fondu ; sinon glissement d'un quart de largeur et fondu (280 ms), vers la gauche en avançant, vers la droite en revenant.
- Clics : onde et léger enfoncement animé (échelle 0,95, ressort) sur les boutons, tuiles, cartes, puces et onglets.
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
