package com.chessplus.app.design.preview

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.chessplus.app.R

// ---------------------------------------------------------------------------
// 1. Choix du niveau (premier lancement)
// ---------------------------------------------------------------------------

/**
 * Carte de niveau du premier lancement.
 */
@Composable
private fun LevelCard(inTitle: String, inDescription: String, inRating: String, inSelected: Boolean) {
	val vColors = kLocalDesignColors.current
	Row(
		Modifier
			.fillMaxWidth()
			.clip(RoundedCornerShape(22.dp))
			.background(vColors.surface)
			.border(2.dp, if (inSelected) vColors.primary else vColors.surface, RoundedCornerShape(22.dp))
			.padding(18.dp),
		verticalAlignment = Alignment.CenterVertically,
	) {
		Column(Modifier.weight(1f)) {
			Text(inTitle, style = DesignType.kSection, color = vColors.text)
			Text(inDescription, style = DesignType.kCaption, color = vColors.muted)
		}
		Text(inRating, style = DesignType.kRating, color = if (inSelected) vColors.text else vColors.muted)
		if (inSelected) {
			Spacer(Modifier.width(10.dp))
			Icon(painterResource(R.drawable.ic_check_circle_fill1), contentDescription = null, tint = vColors.text, modifier = Modifier.size(22.dp))
		}
	}
}

/** Premier lancement : choix du niveau de départ. */
@DesignPreviews
@Composable
fun LevelChoicePreview() = DesignTheme {
	val vColors = kLocalDesignColors.current
	Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 20.dp)) {
		Spacer(Modifier.height(40.dp))
		Text("BIENVENUE", style = DesignType.kCaption, color = vColors.muted)
		Spacer(Modifier.height(6.dp))
		Text("Quel est votre niveau ?", style = DesignType.kScreenTitle, color = vColors.text)
		Spacer(Modifier.height(8.dp))
		Text("Votre cote de départ s'ajustera ensuite après chaque problème.", style = DesignType.kBody, color = vColors.muted)
		Spacer(Modifier.height(24.dp))
		Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
			LevelCard("Débutant", "Je connais les règles", "800", false)
			LevelCard("Intermédiaire", "Je joue de temps en temps", "1 200", true)
			LevelCard("Confirmé", "Je joue régulièrement", "1 600", false)
			LevelCard("Avancé", "Je joue en club ou en ligne", "2 000", false)
		}
		Spacer(Modifier.height(10.dp))
		Row(
			Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(vColors.surface).padding(horizontal = 18.dp, vertical = 14.dp),
			verticalAlignment = Alignment.CenterVertically,
		) {
			Text("Je connais ma cote", style = DesignType.kBodyStrong, color = vColors.text, modifier = Modifier.weight(1f))
			Box(Modifier.width(96.dp).height(40.dp).background(vColors.background, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
				Text("ex. 1 650", style = DesignType.kBody, color = vColors.muted)
			}
		}
		Spacer(Modifier.weight(1f))
		PrimaryButton("Commencer", Modifier.padding(bottom = 20.dp))
	}
}

// ---------------------------------------------------------------------------
// 2. Accueil
// ---------------------------------------------------------------------------

/**
 * Accueil : nombre de problèmes résolus, reprise éventuelle, entraînement.
 */
@Composable
private fun HomeScreen(inResume: Boolean) {
	val vColors = kLocalDesignColors.current
	Column(Modifier.fillMaxSize().statusBarsPadding()) {
		ScreenTopBar("ChessPlus", inActionIcon = R.drawable.ic_settings)
		Column(Modifier.weight(1f).padding(horizontal = 20.dp)) {
			Spacer(Modifier.height(8.dp))
			SurfaceCard {
				Text("Problèmes résolus", style = DesignType.kCaption, color = vColors.muted)
				Text("128", style = DesignType.kNumber, color = vColors.text)
				Text("Votre cote : 1 520", style = DesignType.kBody, color = vColors.muted)
			}
			if (inResume) {
				Spacer(Modifier.height(12.dp))
				Row(
					Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(vColors.primary).padding(16.dp),
					verticalAlignment = Alignment.CenterVertically,
				) {
					Box(Modifier.size(64.dp).clip(RoundedCornerShape(10.dp))) {
						PreviewBoard("q5nr/1ppknQpp/3pB3/1P2p3/4P3/B1PP1b2/6PP/5K2", BoardMarks(coordinates = false), Modifier.fillMaxSize())
					}
					Spacer(Modifier.width(14.dp))
					Column(Modifier.weight(1f)) {
						Text("Reprendre le problème", style = DesignType.kSection, color = vColors.onPrimary)
						Text("Coup 2 sur 2 · il y a 3 min", style = DesignType.kCaption, color = vColors.muted)
					}
					Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = null, tint = vColors.onPrimary, modifier = Modifier.size(24.dp))
				}
			}
			Spacer(Modifier.weight(1f))
			SecondaryButton("Entraînement ciblé", R.drawable.ic_tune)
			Spacer(Modifier.height(12.dp))
			PrimaryButton("S'entraîner", inIcon = R.drawable.ic_bolt_fill1)
			Spacer(Modifier.height(20.dp))
		}
		BottomNavBar(NavTab.Home)
	}
}

/** Accueil avec un problème en cours. */
@DesignPreviews
@Composable
fun HomeResumePreview() = DesignTheme { HomeScreen(inResume = true) }

/** Accueil sans problème en cours. */
@DesignPreviews
@Composable
fun HomePreview() = DesignTheme { HomeScreen(inResume = false) }

// ---------------------------------------------------------------------------
// 4. Entraînement ciblé
// ---------------------------------------------------------------------------

/** Familles de thèmes et exemples de noms français (proposition, à valider). */
private val kThemeFamilies = listOf(
	"Motifs tactiques" to listOf("Fourchette", "Clouage", "Enfilade", "Attaque à la découverte", "Déviation", "Attraction", "Sacrifice", "Pièce en prise", "Coup intermédiaire"),
	"Mats" to listOf("Mat en 1", "Mat en 2", "Mat en 3", "Mat du couloir", "Mat étouffé", "Mat de l'épaulette"),
	"Finales" to listOf("Pions", "Tours", "Fous", "Cavaliers", "Dames"),
	"Phase de jeu" to listOf("Ouverture", "Milieu de partie", "Finale"),
	"Longueur" to listOf("Un coup", "Court", "Long", "Très long"),
)

/** Thèmes sélectionnés dans la maquette. */
private val kSelectedThemes = setOf("Fourchette", "Clouage", "Mat en 2")

/** Entraînement ciblé : difficulté et thèmes. */
@OptIn(ExperimentalLayoutApi::class)
@DesignPreviews
@Composable
fun TargetedTrainingPreview() = DesignTheme {
	val vColors = kLocalDesignColors.current
	Column(Modifier.fillMaxSize().statusBarsPadding()) {
		ScreenTopBar("Entraînement ciblé", inBack = true)
		Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
			Text("Difficulté", style = DesignType.kSection, color = vColors.text)
			Spacer(Modifier.height(10.dp))
			SegmentedControl(listOf("Plus facile", "Mon niveau", "Plus difficile"), 1)
			Spacer(Modifier.height(6.dp))
			SwitchRow("Tranche précise", "Par exemple 1 400 – 1 600", false)
			Spacer(Modifier.height(14.dp))
			Text("Thèmes", style = DesignType.kSection, color = vColors.text)
			for ((vFamily, vThemes) in kThemeFamilies) {
				Spacer(Modifier.height(14.dp))
				Text(vFamily.uppercase(), style = DesignType.kCaption, color = vColors.muted)
				Spacer(Modifier.height(8.dp))
				FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
					for (vTheme in vThemes) {
						SelectChip(vTheme, vTheme in kSelectedThemes)
					}
				}
			}
			Spacer(Modifier.height(16.dp))
		}
		Column(Modifier.background(vColors.background).navigationBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp)) {
			Text("3 869 problèmes correspondent", style = DesignType.kCaption, color = vColors.muted, modifier = Modifier.align(Alignment.CenterHorizontally))
			Spacer(Modifier.height(10.dp))
			PrimaryButton("Commencer", inIcon = R.drawable.ic_bolt_fill1)
		}
	}
}

// ---------------------------------------------------------------------------
// 5. Historique
// ---------------------------------------------------------------------------

/**
 * Ligne de l'historique : vignette, thèmes, cote, résultat.
 */
@Composable
private fun HistoryRow(inPlacement: String, inThemes: String, inDetail: String, inSuccess: Boolean, inDelta: String) {
	val vColors = kLocalDesignColors.current
	Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
		Box(Modifier.size(52.dp).clip(RoundedCornerShape(10.dp))) {
			PreviewBoard(inPlacement, BoardMarks(coordinates = false), Modifier.fillMaxSize())
		}
		Spacer(Modifier.width(14.dp))
		Column(Modifier.weight(1f)) {
			Text(inThemes, style = DesignType.kBodyStrong, color = vColors.text)
			Text(inDetail, style = DesignType.kCaption, color = vColors.muted)
		}
		Icon(
			painterResource(if (inSuccess) R.drawable.ic_check_circle_fill1 else R.drawable.ic_cancel_fill1),
			contentDescription = null, tint = if (inSuccess) vColors.success else vColors.error, modifier = Modifier.size(20.dp),
		)
		Spacer(Modifier.width(6.dp))
		Text(inDelta, style = DesignType.kBodyStrong, color = if (inSuccess) vColors.success else vColors.error, modifier = Modifier.width(34.dp))
	}
}

/** Historique : tentatives récentes, filtrables. */
@DesignPreviews
@Composable
fun HistoryPreview() = DesignTheme {
	val vColors = kLocalDesignColors.current
	Column(Modifier.fillMaxSize().statusBarsPadding()) {
		ScreenTopBar("Historique")
		Column(Modifier.weight(1f).padding(horizontal = 20.dp)) {
			SegmentedControl(listOf("Tous", "Réussis", "Échoués"), 0)
			Spacer(Modifier.height(14.dp))
			Text("AUJOURD'HUI", style = DesignType.kCaption, color = vColors.muted)
			SurfaceCard(Modifier.padding(top = 8.dp)) {
				HistoryRow("q2k1Qnr/1pp1n1pp/3pB3/1P2p3/4P3/B1PP1b2/6PP/5K2", "Mat en 2 · Milieu de partie", "1 760 · il y a 5 min", true, "+12")
				HistoryRow("r3r1k1/p4ppp/2p2n2/1p6/3P1qb1/2NQ2R1/PPB2PP1/R1B3K1", "Fourchette · Sacrifice", "2 671 · il y a 12 min", false, "−9")
				HistoryRow("Q1b2r1k/p2Rp2p/5bp1/8/5P2/4B3/PPP3PP/2K4R", "Fourchette · Long", "2 235 · il y a 20 min", true, "+15")
			}
			Spacer(Modifier.height(14.dp))
			Text("HIER", style = DesignType.kCaption, color = vColors.muted)
			SurfaceCard(Modifier.padding(top = 8.dp)) {
				HistoryRow("1k1r4/pp3pp1/2p1p3/4b3/P3n1P1/8/KPP2PN1/3rBR1R", "Attaque à la découverte", "998 · hier, 18:42", true, "+6")
				HistoryRow("2RQ4/p4pp1/4p1kp/8/6PP/q3PPK1/1r6/8", "Mat en 2 · Finale", "1 124 · hier, 18:30", false, "−11")
			}
			Spacer(Modifier.height(8.dp))
			Text("Touchez un problème pour le rejouer (sans effet sur la cote)", style = DesignType.kCaption, color = vColors.muted, modifier = Modifier.align(Alignment.CenterHorizontally))
		}
		BottomNavBar(NavTab.History)
	}
}

// ---------------------------------------------------------------------------
// 6. Statistiques
// ---------------------------------------------------------------------------

/** Évolution factice de la cote sur 30 jours. */
private val kRatingSeries = listOf(1484, 1490, 1478, 1495, 1502, 1497, 1510, 1505, 1499, 1512, 1520, 1514, 1526, 1519, 1532)

/** Taux de réussite factices par thème. */
private val kThemeRates = listOf("Fourchette" to 0.78f, "Mat en 2" to 0.71f, "Clouage" to 0.64f, "Finale de tours" to 0.52f, "Sacrifice" to 0.45f)

/** Statistiques : évolution de la cote et réussite par thème. */
@DesignPreviews
@Composable
fun StatsPreview() = DesignTheme {
	val vColors = kLocalDesignColors.current
	Column(Modifier.fillMaxSize().statusBarsPadding()) {
		ScreenTopBar("Statistiques")
		Column(Modifier.weight(1f).padding(horizontal = 20.dp)) {
			SurfaceCard {
				Text("Ma cote", style = DesignType.kCaption, color = vColors.muted)
				Row(verticalAlignment = Alignment.Bottom) {
					Text("1 532", style = DesignType.kNumber, color = vColors.text)
					Spacer(Modifier.width(10.dp))
					Text("+48 en 30 jours", style = DesignType.kBodyStrong, color = vColors.success, modifier = Modifier.padding(bottom = 10.dp))
				}
				Spacer(Modifier.height(8.dp))
				Canvas(Modifier.fillMaxWidth().height(110.dp)) {
					val vMin = kRatingSeries.min() - 10f
					val vMax = kRatingSeries.max() + 10f
					val vPath = Path()
					kRatingSeries.forEachIndexed { inIndex, inValue ->
						val vX = size.width * inIndex / (kRatingSeries.size - 1)
						val vY = size.height * (1f - (inValue - vMin) / (vMax - vMin))
						if (inIndex == 0) vPath.moveTo(vX, vY) else vPath.lineTo(vX, vY)
					}
					drawPath(vPath, vColors.text, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round))
					drawCircle(vColors.text, 4.dp.toPx(), Offset(size.width, size.height * (1f - (kRatingSeries.last() - vMin) / (vMax - vMin))))
				}
				Spacer(Modifier.height(12.dp))
				SegmentedControl(listOf("7 jours", "30 jours", "Tout"), 1)
			}
			Spacer(Modifier.height(12.dp))
			SurfaceCard {
				Text("Réussite par thème", style = DesignType.kSection, color = vColors.text)
				Spacer(Modifier.height(8.dp))
				for ((vTheme, vRate) in kThemeRates) {
					Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
						Text(vTheme, style = DesignType.kBody, color = vColors.text, modifier = Modifier.width(118.dp))
						Box(Modifier.weight(1f).height(8.dp).background(vColors.track, CircleShape)) {
							Box(Modifier.fillMaxWidth(vRate).fillMaxHeight().background(vColors.text, CircleShape))
						}
						Text("${(vRate * 100).toInt()} %", style = DesignType.kBodyStrong, color = vColors.text, modifier = Modifier.width(52.dp).padding(start = 10.dp))
					}
				}
			}
		}
		BottomNavBar(NavTab.Stats)
	}
}

// ---------------------------------------------------------------------------
// 7. Réglages
// ---------------------------------------------------------------------------

/** Réglages : apparence, retours, échiquier, niveau, crédits. */
@DesignPreviews
@Composable
fun SettingsPreview() = DesignTheme {
	val vColors = kLocalDesignColors.current
	Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState())) {
		ScreenTopBar("Réglages", inBack = true)
		Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
			SurfaceCard {
				Text("Thème", style = DesignType.kBodyStrong, color = vColors.text)
				Spacer(Modifier.height(10.dp))
				SegmentedControl(listOf("Système", "Clair", "Sombre"), 0)
			}
			SurfaceCard {
				SwitchRow("Vibration", "Courte vibration à chaque coup", true)
				SwitchRow("Sons", null, false)
				SwitchRow("Coordonnées", "Lettres et chiffres dans les cases", true)
			}
			SurfaceCard {
				NavigationRow("Niveau de départ", "Intermédiaire")
			}
			SurfaceCard {
				NavigationRow("Crédits", null)
				Text(
					"Problèmes : base Lichess (CC0). Pièces : cburnett, Colin M.L. Burnett (GPLv2+). Police : Inter (OFL). Icônes : Material Symbols (Apache 2.0).",
					style = DesignType.kCaption, color = vColors.muted,
				)
			}
			Text("ChessPlus 0.1.0", style = DesignType.kCaption, color = vColors.muted, modifier = Modifier.align(Alignment.CenterHorizontally).navigationBarsPadding().padding(bottom = 16.dp))
		}
	}
}
