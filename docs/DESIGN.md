# Design de ChessPlus

Ce document fait foi pour l'implémentation des écrans (phase 5). Il ne contient que des choix validés par Mathieu ; les propositions en attente sont dans `docs/PROGRESS.md`.

## Design system ChessPlus (fourni par Mathieu le 2026-11-23, maquettes à valider sur l'appareil)

Source : dossier local `chessplus-design/` (non versionné, exclu par `.git/info/exclude`) : README, captures `screens/`, consignes `guidelines/`, jetons `tokens/tokens.json`, maquettes HTML `mockups/`. Il remplace l'ambiance « Calm », la direction « Accent pleine largeur », la palette verte B, la typographie Inter seule et les icônes Material Symbols décrites plus bas, qui restent comme historique.

- **Jetons** : `ui/theme/ChessPlusTokens.kt` est le fichier Kotlin généré du design system, repris tel quel (seul le paquet change ; il ne suit donc pas les conventions de nommage du projet). `ChessPlusTheme.colors / .typography / .spacing / .radius` ; aucun écran n'écrit de couleur en dur.
- **Rayons** : `tokens.json` (3 / 6 / 10 / 20 dp) fait foi. Les captures `screens/*.png` ont été rendues avec un `tokens.css` périmé (cartes à 4 px, barre de navigation sans arrondi, couleur `streak` absente) : l'application suit `tokens.json`.
- **Polices embarquées** (OFL) : Space Grotesk (titres), Inter (texte), JetBrains Mono (chiffres, coups, chronos) ; polices variables dans `res/font/`.
- **Icônes au trait** 1,6 sur grille 24 (`res/drawable/ic_ln_*.xml`), converties des tracés `kIcons` des maquettes ; check, close et rejouer dessinées dans le même style.
- **Navigation** : barre flottante à trois onglets, Accueil, Stats, Réglages. L'historique n'est plus un onglet : il s'ouvre depuis les statistiques (ligne « Historique »).
- **Ton** : tutoiement, « puzzle » au lieu de « problème », « classement » au lieu de « cote », retours factuels sans point d'exclamation.
- **Accueil** (maquette) : date, série de jours (flamme), puzzle du jour, liste des modes Arcade, Rush (3 minutes, comme la maquette), Survie, Entraînement (ex-ciblé ; valeur à droite : nombre de thèmes, 73), mention hors ligne. La carte « Reprendre » et le tableau de bord disparaissent : l'Arcade reprend le puzzle en cours.
- **Puzzle du jour** (modifié par Mathieu le 2026-11-24) : un puzzle par date, tiré à la première demande autour du classement (graine = date) puis conservé (table `daily_puzzle`, base utilisateur v4) ; noté s'il n'a jamais été joué. Les puzzles du jour **ne s'enchaînent pas** : ni « Passer », ni « Puzzle suivant », ni enchaînement automatique. Une fois le puzzle terminé, un **calendrier** remplace le bilan : un mois à la fois (12 mois en arrière au plus), jours résolus pleins en `accent`, aujourd'hui cerclé, jour affiché en `accentSoft`, jours à venir inactifs ; toucher un jour charge son puzzle. Un jour compte comme résolu quand son puzzle a été réussi au moins une fois. Sur l'accueil, la carte affiche « ✓ Résolu » et propose « Rejouer » (bouton secondaire) une fois le puzzle d'aujourd'hui réussi. Le puzzle du jour ne touche plus au puzzle en cours de l'Arcade.
- **Jeu** (maquette) : chrono du puzzle (Arcade, Entraînement, rejeu), compte à rebours (Rush) ou vies (Survie) en chip ; numéro et classement du puzzle ; camp au trait ; étiquette du thème principal affichée **seulement en Entraînement** (décision de Mathieu le 2026-11-24 : ailleurs, ce serait un indice ; la maquette la montrait en Arcade) ; notation SAN ; carte de retour à bordure `success` / `danger` ; bilan de la session (réussis, ratés, variation du classement) ; actions Indice (pièce à jouer cerclée d'`accent`), Solution (joue le coup), Retourner, Passer (un puzzle non terminé compte comme raté). « Recommencer » et le volet de bilan disparaissent ; une fois le puzzle terminé, bouton principal « Puzzle suivant ». Pas d'indice ni de solution en défi ; Passer y compte comme une erreur.
- **Objectif quotidien** (demandé par Mathieu le 2026-11-24) : nombre de puzzles à réussir par jour (3, 5, 10, 15, 20 ou 30 ; 5 par défaut), tous modes confondus, défis compris. Sur l'accueil, une carte « Objectif du jour » sous le puzzle du jour : « 3/5 » en mono, barre de progression (`success` une fois atteint) ; pas de sous-titre (retiré à la demande de Mathieu le 2026-11-24). Dans les réglages, section « Objectif quotidien » : puzzles par jour, rappel (désactivé par défaut ; l'activer demande l'autorisation des notifications), heure du rappel (20:00 par défaut, sélecteur 24 h). Le rappel est une alarme approximative (AlarmManager, à quelques minutes près, sans permission d'alarme exacte) : à son déclenchement, une notification n'est envoyée que si l'objectif n'est pas atteint ; elle est reprogrammée chaque jour, après un redémarrage, une mise à jour ou un changement d'heure.
- **Réglages** (maquette) : thèmes d'échiquier Sauge, Bois, Ardoise, Gris (variantes sombres assombries dans les proportions de la sauge), style des pièces (cburnett seul), coordonnées, confirmer les coups (Confirmer / Annuler avant de jouer), difficulté de l'Arcade (± 200 points), enchaîner automatiquement (1,2 s après une réussite), base hors ligne et « Gérer » (informations), apparence. Ajoutés dans la même charte : vibration, classement de départ, À propos. Le réglage Sons est retiré.
- **Statistiques** (maquette) : classement et variation sur 30 jours, courbe sans bande d'incertitude, périodes 7 j / 30 j / 1 an / Tout, tuiles Résolus, Réussite, Série, Chrono record (record du Rush), activité sur 12 semaines, réussite par thème ; les autres sections suivent dans la même charte.

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
- Indice en deux temps (modifié par Mathieu le 2026-11-23) : le premier appui met en surbrillance la pièce à jouer (case légèrement teintée de bleu, sans cadre, la couleur des indices dans les statistiques ; adoucie à la demande de Mathieu) avec « Indice / Jouez le fou en surbrillance » et la mention « Touchez encore Indice pour voir le coup » ; le second appui joue le coup comme avant. Les deux appuis sur un même coup comptent pour un seul indice ; le premier suffit à compter le problème comme échoué.

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
| 2 | Accueil | Tableau de bord (résolus, série, réussite, cote ; ouvre les statistiques) ; carte « Reprendre » si un problème est en cours ; modes de jeu en deux groupes (voir « Modes de jeu ») ; icône des réglages |
| 3 | Problème | Plein écran : échiquier, consigne, retour visuel, commandes en bas (Recommencer, Indice = pièce à jouer puis coup, Suivant) ; 3a promotion (choix de la pièce) ; 3b bilan en volet bas (résultat, cote et thèmes révélés, évolution de la cote, lien Lichess, Suivant) |
| 4 | Entraînement ciblé | Thèmes par familles ; difficulté relative (plus facile, à mon niveau, plus difficile) **ou** tranche précise ; bouton « Commencer » |
| 5 | Historique | Onglets Tous, Réussis, Échoués ; une ligne relance le problème, sans effet sur la cote |
| 6 | Statistiques | Sections détaillées ci-dessous ; une fiche par thème (en attente de validation sur `road-to-v2`) |
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

## Modes de jeu (demandés par Mathieu le 2026-11-23, maquettes à valider)

- Accueil en deux groupes de cartes : **Entraînement** = Arcade (l'entraînement continu, adapté à la cote, carte verte) et Ciblé ; **Défis** = Survie et Rush, avec leur record en haut à droite de la carte. Le tableau de bord tient en une carte (résolus, série, réussite, cote) pour que tous les modes soient visibles sans défiler ; l'écran défile si la police est agrandie.
- **Survie** : 3 vies ; le premier problème est 300 points sous la cote du joueur, puis chaque problème monte de 25 points. Un mauvais coup retire une vie, montre le coup attendu 1,2 s (« Il fallait jouer a2 → e6 »), puis passe au suivant ; à zéro vie, la partie s'arrête. Vies en cœurs dans la barre du haut.
- **Rush** : 5 minutes ; problèmes autour de la cote du joueur (± 100, élargi si besoin). Le chronomètre part au premier problème et passe en rouge dans les 30 dernières secondes. Un mauvais coup passe directement au problème suivant.
- Dans les deux défis : un problème réussi enchaîne sur le suivant après 0,5 s ; pas d'indice ni de « Recommencer » ; score sous l'échiquier et bouton « Abandonner ». Aucun problème n'est reproposé dans une même partie.
- Fin d'une session d'Arcade (demandé par Mathieu le 2026-11-24) : dès qu'au moins un puzzle est terminé, le retour (flèche ou geste) ouvre « Mettre fin à la session ? » (« Continuer » / « Terminer » ; le puzzle en cours reste gardé pour la reprise), puis le récapitulatif « Session terminée » : classement et variation de la session, tuiles réussis, ratés, temps moyen, réussite ; « Accueil » quitte l'écran. Sans puzzle terminé, le retour quitte directement.
- Fin de Survie (demandé par Mathieu le 2026-11-24) : fenêtre récapitulative « Survie terminée » avec quatre tuiles, puzzles réussis, ratés, temps moyen d'un puzzle terminé (réussi ou raté, à partir du moment où le joueur peut jouer) et record absolu ; « Nouveau record. » le cas échéant ; boutons « Fermer » et « Rejouer ». Une fois fermée, l'écran garde « Accueil » et « Rejouer ».
- Fin de partie : titre (« Plus de vies », « Temps écoulé », « Partie terminée »), score, « Nouveau record ! » ou le record, problème le plus difficile réussi ; boutons « Accueil » et « Rejouer ».
- Les défis ne modifient pas le classement (choix de Mathieu) ; depuis le 2026-11-24, **chaque puzzle de défi compte** dans les statistiques, l'historique et l'objectif quotidien (tentative non notée : réussi, ou raté sur une erreur ou « Passer » ; un puzzle interrompu par la fin du temps ou un abandon ne compte pas). Chaque partie est aussi enregistrée à part pour les records (table `challenge_run`, base utilisateur v3). Quitter une partie en cours compte comme un abandon ; une partie abandonnée sans aucun problème réussi n'est pas enregistrée.

## Statistiques (demandées par Mathieu le 2026-11-23, maquettes à valider)

Maquettes : captures `app/src/screenshotTestDebug/reference/com/chessplus/app/StatsScreenShotsKt/` (historique factice de quatre mois, calculé par le vrai `StatsCalculator`).

- **Écran Statistiques**, de haut en bas :
  - quatre tuiles : cote et variation sur 30 jours, résolus sur le total, série de jours et record, réussite ;
  - courbe de la cote avec la bande d'incertitude Glicko-2 (cote ± écart), sur 7 jours, 30 jours ou tout ;
  - calendrier d'activité façon GitHub (17 semaines, lundi en haut, cinq niveaux), série en cours, plus longue série, réussites d'affilée ;
  - problèmes par semaine (8 semaines) ;
  - forces et faiblesses : motifs, mats et finales joués au moins 3 fois, triés par réussite ou par cote du thème ; un thème ouvre sa fiche ;
  - à travailler : les trois thèmes les plus faibles (au moins 5 problèmes) et « S'entraîner sur ces thèmes » (entraînement ciblé à mon niveau) ;
  - réussite par famille (motifs, mats, finales, phase de jeu), par tranche de difficulté de 200 points (au moins 3 problèmes), selon la longueur de la solution et selon le moment de la journée ;
  - fin des problèmes en barre empilée : réussi du premier coup, indice ou solution, erreur ;
  - temps de résolution : moyenne des réussites, thèmes le plus rapide et le plus lent, médiane par semaine ;
  - coup de la première erreur ou du premier indice (1er, 2e, 3e et plus) ;
  - records (meilleure cote, plus longue série, réussites d'affilée, problème le plus difficile réussi) et couverture des 73 thèmes.
- **Fiche d'un thème** : cote du thème (Glicko-2 rejoué sur les seules tentatives du thème), réussite, nombre de problèmes, meilleur réussi ; courbe de la cote du thème avec sa bande ; réussite glissante sur 10 problèmes ; temps moyen ; dix derniers problèmes (un toucher les rejoue) ; bouton « S'entraîner sur ce thème ».
- **Accueil** : tuiles « Série » et « Réussite » sous le compteur de problèmes résolus ; elles ouvrent l'onglet Statistiques.
- **Graphiques** (recommandations de visualisation) : une seule teinte (la couleur principale) pour une mesure ; traits de 2 dp ; extrémités arrondies posées sur la ligne de base ; 2 dp entre barres et segments ; valeurs dans la couleur du texte ; pas de radar. Seule la barre empilée utilise trois couleurs (vert `#3E8E5A`, bleu `#3D6FB6`, rouge `#C2483D` en clair ; `#4FA06B`, `#6D8FD8`, `#E0675C` en sombre), vérifiées pour les daltoniens et toujours accompagnées d'une légende chiffrée.
- **Nouvelles données** (base utilisateur v2, migration automatique) : durée d'une tentative (du premier coup de l'adversaire à la fin, ignorée au-delà de 15 min), coup de la première erreur ou du premier indice, écart de cote après la tentative. Les tentatives antérieures n'ont pas ces données : les cartes concernées l'indiquent.

## Cote (validé le 2026-11-20)

- Algorithme **Glicko-2** (`Glicko2RatingSystem`).
- Un problème **déjà joué** ne modifie plus la cote quand il est rejoué (tentative non notée).
