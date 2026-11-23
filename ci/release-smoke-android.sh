#!/usr/bin/env bash
# Test de fumée de l'APK release (R8, réduit) sur un émulateur, lancé par la CI (même principe
# que PatrimoinePlus) : installation, lancement, choix du niveau du premier lancement
# (« Commencer »), accueil (« Problèmes résolus »), un problème (« … jouent »), retour à
# l'accueil, onglets Historique et Statistiques ; aucun plantage (journal « crash » vide,
# processus vivant). L'appli est pilotée par son arbre d'accessibilité (uiautomator), sans
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

adb install -r "$kApk"
adb logcat -c
adb shell am start -W -n "$kPackage/.MainActivity"
wait_for 'text="Quel est votre niveau ?"'
check_alive
tap "Commencer"
wait_for 'text="Problèmes résolus"'
check_alive
tap "S’entraîner"
wait_for 'jouent"'
check_alive
adb shell input keyevent KEYCODE_BACK
wait_for 'text="Problèmes résolus"'
tap "Historique"
wait_for 'text="Tous"'
tap "Statistiques"
wait_for 'text="Statistiques"'
sleep 2
check_alive
echo "APK release vérifié : lancement, choix du niveau, accueil, problème, historique et statistiques sans plantage."
