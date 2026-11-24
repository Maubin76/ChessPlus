"""Visuels du README (docs/images) : écrans de l'application posés sur un fond gris
dégradé, dans des cadres de téléphone, à la manière des présentations produit.

Entrées : les captures de référence des tests de captures d'écran
(app/src/screenshotTestDebug/reference), régénérées par
./gradlew :app:updateDebugScreenshotTest ; l'icône de l'application (mipmap-xxxhdpi).
Lancer depuis la racine du dépôt : python ci/vitrines.py (Pillow requis)."""
import glob

from PIL import Image, ImageDraw, ImageFilter, ImageFont

kShots = 'app/src/screenshotTestDebug/reference/com/chessplus/app/'
kIcon  = 'app/src/main/res/mipmap-xxxhdpi/ic_launcher_'
kOut   = 'docs/images/'
kFont  = 'C:/Windows/Fonts/seguisb.ttf'


def shot(inName):
	"""Capture de référence dont le nom commence par inName (ex. 'game/GameScreenShotsKt/RealGameShot_Clair')."""
	vMatches = glob.glob(kShots + inName + '*.png')
	assert len(vMatches) == 1, (inName, vMatches)
	return Image.open(vMatches[0]).convert('RGB')


def tall(inName, inTop, inRatio=2288 / 1056):
	"""Fenêtre de la hauteur d'un écran dans une capture longue, à partir de inTop."""
	vImg = shot(inName)
	return vImg.crop((0, inTop, vImg.width, inTop + int(vImg.width * inRatio)))


def background(inW, inH):
	"""Fond gris dégradé, plus clair en haut à gauche."""
	vImg = Image.new('RGB', (inW, inH))
	vPx  = vImg.load()
	for vY in range(inH):
		for vX in range(inW):
			vT = (vX / inW) * 0.35 + (vY / inH) * 0.65
			vC = int(158 - 46 * vT)
			vPx[vX, vY] = (vC, vC, vC + 2)
	return vImg


def rounded_mask(inSize, inRadius):
	"""Masque d'un rectangle arrondi (anti-crénelé par sur-échantillonnage)."""
	vScale = 4
	vBig   = Image.new('L', (inSize[0] * vScale, inSize[1] * vScale), 0)
	ImageDraw.Draw(vBig).rounded_rectangle((0, 0, vBig.width - 1, vBig.height - 1), inRadius * vScale, fill=255)
	return vBig.resize(inSize, Image.LANCZOS)


def shadow(inCanvas, inBox, inRadius, inBlur=60, inOpacity=110, inOffset=(0, 40)):
	"""Ombre douce sous un élément."""
	vLayer = Image.new('L', inCanvas.size, 0)
	vBox   = (inBox[0] + inOffset[0], inBox[1] + inOffset[1], inBox[2] + inOffset[0], inBox[3] + inOffset[1])
	ImageDraw.Draw(vLayer).rounded_rectangle(vBox, inRadius, fill=inOpacity)
	vLayer = vLayer.filter(ImageFilter.GaussianBlur(inBlur))
	inCanvas.paste(Image.new('RGB', inCanvas.size, (20, 20, 22)), (0, 0), vLayer)


def add_status(inScreen):
	"""Barre d'état dessinée (9:41, Wi-Fi, batterie) au-dessus d'un écran rendu sans barre :
	une bande de la couleur du fond, le contenu descend d'autant. L'encre suit le fond."""
	vW    = inScreen.width
	vBand = int(vW * 0.115)
	vImg  = Image.new('RGB', (vW, inScreen.height + vBand))
	vTop  = inScreen.getpixel((2, 2))
	vImg.paste(Image.new('RGB', (vW, vBand), vTop), (0, 0))
	vImg.paste(inScreen, (0, vBand))
	vDark = sum(vTop) / 3 < 128
	vDraw = ImageDraw.Draw(vImg)
	vInk  = (245, 245, 245) if vDark else (20, 20, 20)
	vFont = ImageFont.truetype(kFont, int(vW * 0.034))
	vDraw.text((int(vW * 0.075), int(vW * 0.042)), '9:41', font=vFont, fill=vInk)
	vBx = int(vW * 0.855); vBy = int(vW * 0.048); vBw = int(vW * 0.062); vBh = int(vW * 0.029)
	vDraw.rounded_rectangle((vBx, vBy, vBx + vBw, vBy + vBh), vBh // 3, fill=vInk)
	vDraw.rectangle((vBx + vBw + 3, vBy + vBh // 3, vBx + vBw + 7, vBy + vBh - vBh // 3), fill=vInk)
	vCx = int(vW * 0.80); vCy = vBy + vBh
	for vR in (int(vW * 0.030), int(vW * 0.020)):
		vDraw.arc((vCx - vR, vCy - vR, vCx + vR, vCy + vR), 225, 315, fill=vInk, width=4)
	vDraw.ellipse((vCx - 5, vCy - 9, vCx + 5, vCy + 1), fill=vInk)
	return vImg


def phone(inScreen, inWidth):
	"""Téléphone : écran arrondi dans un cadre sombre à liseré métallique, caméra en haut."""
	vScreen = add_status(inScreen)
	vSw     = inWidth
	vSh     = int(vScreen.height * inWidth / vScreen.width)
	vScreen = vScreen.resize((vSw, vSh), Image.LANCZOS)
	vBezel  = int(inWidth * 0.032)
	vEdge   = max(3, int(inWidth * 0.008))
	vW      = vSw + 2 * (vBezel + vEdge)
	vH      = vSh + 2 * (vBezel + vEdge)
	vR      = int(inWidth * 0.135)
	vBody   = Image.new('RGBA', (vW, vH), (0, 0, 0, 0))
	vMetal  = Image.new('RGB', (vW, vH))
	vMd     = ImageDraw.Draw(vMetal)
	for vY in range(vH):
		vC = int(118 - 70 * vY / vH)
		vMd.line((0, vY, vW, vY), fill=(vC + 14, vC + 14, vC + 18))
	vBody.paste(vMetal, (0, 0), rounded_mask((vW, vH), vR))
	vInner = rounded_mask((vW - 2 * vEdge, vH - 2 * vEdge), vR - vEdge)
	vBody.paste(Image.new('RGB', (vW - 2 * vEdge, vH - 2 * vEdge), (12, 12, 13)), (vEdge, vEdge), vInner)
	vBody.paste(vScreen, (vEdge + vBezel, vEdge + vBezel), rounded_mask((vSw, vSh), vR - vEdge - vBezel))
	vD  = ImageDraw.Draw(vBody)
	vCr = int(vSw * 0.022)
	vCx = vW // 2; vCy = vEdge + vBezel + int(vSw * 0.055)
	vD.ellipse((vCx - vCr, vCy - vCr, vCx + vCr, vCy + vCr), fill=(0, 0, 0))
	return vBody


def flat(inScreen, inWidth):
	"""Écran seul, sans cadre, coins arrondis."""
	vScreen = add_status(inScreen)
	vH      = int(vScreen.height * inWidth / vScreen.width)
	vScreen = vScreen.resize((inWidth, vH), Image.LANCZOS)
	vOut    = Image.new('RGBA', (inWidth, vH), (0, 0, 0, 0))
	vOut.paste(vScreen, (0, 0), rounded_mask((inWidth, vH), int(inWidth * 0.11)))
	return vOut


def place(inCanvas, inItem, inX, inY, inRadius, inBlur=55):
	"""Pose un élément avec son ombre."""
	shadow(inCanvas, (inX, inY, inX + inItem.width, inY + inItem.height), inRadius, inBlur)
	inCanvas.paste(inItem, (inX, inY), inItem)


def save(inCanvas, inName, inWidth=1600):
	"""Enregistre un visuel à la largeur du README."""
	vH = int(inCanvas.height * inWidth / inCanvas.width)
	inCanvas.resize((inWidth, vH), Image.LANCZOS).save(kOut + inName, optimize=True)
	print(inName, (inWidth, vH))


def trio(inScreens, inName):
	"""Trois téléphones côte à côte, celui du milieu légèrement plus haut."""
	vCanvas = background(3200, 2150)
	for vIndex, vScreen in enumerate(inScreens):
		vItem = phone(vScreen, 820)
		place(vCanvas, vItem, 190 + vIndex * 1000, 140 + (0 if vIndex == 1 else 60), 110)
	save(vCanvas, inName)


def logo():
	"""Icône de l'application (fond et premier plan de l'icône adaptative) en carré arrondi."""
	vBack  = Image.open(kIcon + 'background.png').convert('RGBA')
	vFront = Image.open(kIcon + 'foreground.png').convert('RGBA')
	vIcon  = Image.alpha_composite(vBack, vFront)
	vSize  = vIcon.width
	vOut   = Image.new('RGBA', (vSize, vSize), (0, 0, 0, 0))
	vOut.paste(vIcon, (0, 0), rounded_mask((vSize, vSize), int(vSize * 0.23)))
	vOut.resize((224, 224), Image.LANCZOS).save(kOut + 'logo.png', optimize=True)
	print('logo.png', (224, 224))


kGame  = 'game/GameScreenShotsKt/'
kApp   = 'AppScreenShotsKt/'
kStats = 'StatsScreenShotsKt/'

logo()

# 1. Couverture : accueil à plat à gauche, un puzzle en cours dans un téléphone à droite.
vCanvas = background(3200, 2000)
place(vCanvas, flat(shot(kApp + 'RealHomeShot_Clair'), 760), 330, 300, 80)
place(vCanvas, phone(shot(kGame + 'RealGameShot_Sombre'), 1180), 1520, 40, 160, 70)
save(vCanvas, 'vitrine-telephone.png')

# 2. Résolution : mauvais coup, indice (la pièce à jouer), revue de la solution coup par coup.
trio([shot(kGame + 'RealGameWrongShot_Clair'), shot(kGame + 'RealGameHintPieceShot_Sombre'), shot(kGame + 'RealGameSolutionReviewShot_Clair')], 'telephone-resolution.png')

# 3. Défis et sessions : Rush en cours, fin de Survie, récapitulatif de session.
trio([shot(kGame + 'RealGameRushShot_Sombre'), shot(kGame + 'RealGameSurvivalRecapShot_Clair'), shot(kGame + 'RealGameSessionRecapShot_Sombre')], 'telephone-defis.png')

# 4. Progression : statistiques, graphiques détaillés, page d'un thème.
trio([shot(kStats + 'StatsTopShot_Clair'), tall(kStats + 'StatsFullDarkShot_SombreLong', 4300), tall(kStats + 'ThemeStatsShot_ClairLong', 0)], 'telephone-progression.png')

# 5. Sur mesure : puzzle du jour et son calendrier, entraînement ciblé, réglages de l'échiquier.
trio([shot(kGame + 'RealGameDailyShot_Clair'), shot(kApp + 'RealTargetedShot_Sombre'), shot(kApp + 'RealSettingsWoodShot_Clair')], 'telephone-sur-mesure.png')
