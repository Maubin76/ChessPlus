#!/usr/bin/env bash
# Test de fumée de l'APK release (R8, réduit) sur un émulateur, lancé par la CI (même principe
# que PatrimoinePlus) : installation, lancement, choix du niveau du premier lancement
# (« Commencer »), accueil (modes de jeu), un puzzle d'Arcade (« Indice »), retour à l'accueil,
# onglet Stats puis Historique (en bas de la page), onglet Réglages ; aucun plantage (journal
# « crash » vide, processus vivant). L'appli est pilotée par son arbre d'accessibilité (uiautomator), sans
# dépendre de son code : c'est bien l'APK produit qui est vérifié.
# Usage : ci/release-smoke-android.sh chemin/vers/app-release.apk
set -euo pipefail

kApk="$1"																	# APK à vérifier
kPackage="com.chessplus.app"												# appli
kDump="/sdcard/ui.xml"														# arbre de l'écran

# Arbre de l'écran courant (texte et descriptions des éléments).
screen()
{
	adb shell uiautomator dump "$kDump" > /dev/null 2>&1 || true
	adb shell cat "$kDump" 2> /dev/null || true
}

# Attend qu'un motif apparaisse dans l'arbre de l'écran ; échoue au bout de 90 s (la première
# ouverture copie la base des problèmes).
# $1 : motif attendu (par exemple text="Commencer", pour un texte exact)
wait_for()
{
	for _ in $(seq 1 90)
	do
		if screen | grep -q -- "$1"; then return 0; fi
		sleep 1
	done
	echo "::error::Introuvable à l'écran : $1"
	screen | sed 's/></>\n</g' | grep -o 'text="[^"]*"\|content-desc="[^"]*"' | grep -v '=""' | head -40
	return 1
}

# Touche le centre de l'élément qui porte un texte ou une description.
# $1 : texte ou description de l'élément
tap()
{
	local vNode vBounds vX1 vY1 vX2 vY2
	vNode=$(screen | sed 's/></>\n</g' | grep -- "text=\"$1\"\|content-desc=\"$1\"" | head -1)
	vBounds=$(echo "$vNode" | grep -o 'bounds="\[[0-9]*,[0-9]*\]\[[0-9]*,[0-9]*\]"' | grep -o '[0-9]*' | tr '\n' ' ')
	read -r vX1 vY1 vX2 vY2 <<< "$vBounds"
	if [ -z "${vX2:-}" ]; then echo "::error::Impossible de toucher : $1"; return 1; fi
	adb shell input tap $(( (vX1 + vX2) / 2 )) $(( (vY1 + vY2) / 2 ))
}

# Échoue si l'appli a planté ou n'est plus lancée.
check_alive()
{
	if adb logcat -d -b crash | grep -q "$kPackage"
	then
		echo "::error::Plantage de l'appli"
		adb logcat -d -b crash | tail -60
		return 1
	fi
	adb shell pidof "$kPackage" > /dev/null || { echo "::error::L'appli n'est plus lancée"; adb logcat -d | tail -80; return 1; }
}

# Fait défiler l'écran vers le bas jusqu'à ce qu'un texte exact apparaisse (12 gestes au plus).
# $1 : texte exact
scroll_to()
{
	local vSize vWidth vHeight vTry
	vSize=$(adb shell wm size | grep -o '[0-9]*x[0-9]*' | tail -1)
	vWidth=${vSize%x*}
	vHeight=${vSize#*x}
	for vTry in $(seq 1 12); do
		if screen | grep -q -- "text=\"$1\""; then return 0; fi
		adb shell input swipe $(( vWidth / 2 )) $(( vHeight * 7 / 10 )) $(( vWidth / 2 )) $(( vHeight * 3 / 10 )) 300
		sleep 1
	done
	echo "::error::Introuvable après défilement : $1"
	return 1
}

adb install -r "$kApk"
adb logcat -c
adb shell am start -W -n "$kPackage/.MainActivity"
# Début du titre seulement : une espace insécable précède le point d'interrogation.
wait_for 'text="Quel est ton niveau'
check_alive
tap "Commencer"
wait_for 'text="Puzzles adaptés à ton classement"'
check_alive
tap "Arcade"
wait_for 'text="Indice"'
check_alive
adb shell input keyevent KEYCODE_BACK
wait_for 'text="Puzzles adaptés à ton classement"'
tap "Stats"
wait_for 'text="Statistiques"'
check_alive
scroll_to "Historique"
tap "Historique"
wait_for 'text="Tous"'
check_alive
adb shell input keyevent KEYCODE_BACK
wait_for 'text="Statistiques"'
tap "Réglages"
wait_for 'text="Réglages"'
sleep 2
check_alive
echo "APK release vérifié : lancement, choix du niveau, accueil, puzzle, statistiques, historique et réglages sans plantage."
