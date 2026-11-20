package com.chessplus.app.design.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.chessplus.app.R

/**
 * Barre du haut de l'écran de jeu : retour, titre centré, cote du joueur.
 */
@Composable
fun GameTopBar(inTitle: String, inRating: String) {
	val vColors = kLocalDesignColors.current
	Box(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 16.dp)) {
		Icon(painterResource(R.drawable.ic_arrow_back_ios_new), contentDescription = "Retour", tint = vColors.text, modifier = Modifier.align(Alignment.CenterStart).size(20.dp))
		Text(inTitle, style = DesignType.kBarTitle, color = vColors.text, modifier = Modifier.align(Alignment.Center))
		Text(inRating, style = DesignType.kRating, color = vColors.text, modifier = Modifier.align(Alignment.CenterEnd))
	}
}

/**
 * Barre du haut des écrans secondaires : retour éventuel, titre à gauche, action éventuelle.
 */
@Composable
fun ScreenTopBar(inTitle: String, inBack: Boolean = false, inActionIcon: Int? = null) {
	val vColors = kLocalDesignColors.current
	Row(Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
		if (inBack) {
			Icon(painterResource(R.drawable.ic_arrow_back_ios_new), contentDescription = "Retour", tint = vColors.text, modifier = Modifier.size(20.dp))
			Spacer(Modifier.width(16.dp))
		}
		Text(inTitle, style = DesignType.kScreenTitle, color = vColors.text, modifier = Modifier.weight(1f))
		inActionIcon?.let {
			Box(Modifier.size(40.dp).background(vColors.surface, CircleShape), contentAlignment = Alignment.Center) {
				Icon(painterResource(it), contentDescription = null, tint = vColors.text, modifier = Modifier.size(22.dp))
			}
		}
	}
}

/**
 * Ton du message affiché au-dessus de l'échiquier.
 */
enum class FeedbackTone {
	Neutral,
	Success,
	Error,
}

/**
 * Message d'état au-dessus de l'échiquier : consigne neutre, bon coup (vert) ou erreur (rouge).
 */
@Composable
fun FeedbackHeader(inTitle: String, inSubtitle: String, inTone: FeedbackTone) {
	val vColors = kLocalDesignColors.current
	val vColor = when (inTone) {
		FeedbackTone.Neutral -> vColors.text
		FeedbackTone.Success -> vColors.success
		FeedbackTone.Error -> vColors.error
	}
	Column(Modifier.fillMaxWidth().height(96.dp).padding(horizontal = 24.dp), verticalArrangement = Arrangement.Center) {
		Row(verticalAlignment = Alignment.CenterVertically) {
			when (inTone) {
				FeedbackTone.Success -> Icon(painterResource(R.drawable.ic_check_circle_fill1), contentDescription = null, tint = vColor, modifier = Modifier.size(30.dp).padding(end = 6.dp))
				FeedbackTone.Error -> Icon(painterResource(R.drawable.ic_cancel_fill1), contentDescription = null, tint = vColor, modifier = Modifier.size(30.dp).padding(end = 6.dp))
				FeedbackTone.Neutral -> Unit
			}
			Text(inTitle, style = DesignType.kFeedback, color = vColor)
		}
		Spacer(Modifier.height(4.dp))
		Text(inSubtitle, style = DesignType.kBody, color = vColors.muted)
	}
}

/**
 * Commandes du jeu sous le pouce : Recommencer, Indice (principal), Suivant.
 */
@Composable
fun GameControls(inNextEnabled: Boolean, inHintEnabled: Boolean = true) {
	Row(
		Modifier.fillMaxWidth().padding(horizontal = 42.dp).navigationBarsPadding().padding(bottom = 28.dp),
		horizontalArrangement = Arrangement.SpaceBetween,
		verticalAlignment = Alignment.Top,
	) {
		ControlTile(R.drawable.ic_refresh, "Recommencer", true)
		PrimaryRoundButton(R.drawable.ic_lightbulb, "Indice", inHintEnabled)
		ControlTile(R.drawable.ic_skip_next, "Suivant", inNextEnabled, inHighlighted = inNextEnabled)
	}
}

/**
 * Tuile de commande secondaire (60 dp) avec son libellé.
 */
@Composable
fun ControlTile(inIcon: Int, inLabel: String, inEnabled: Boolean, inHighlighted: Boolean = false) {
	val vColors = kLocalDesignColors.current
	Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(84.dp).padding(top = 8.dp)) {
		Box(
			Modifier.size(60.dp).background(if (inHighlighted) vColors.primary else vColors.surface, RoundedCornerShape(20.dp)),
			contentAlignment = Alignment.Center,
		) {
			Icon(painterResource(inIcon), contentDescription = null, tint = if (inHighlighted) vColors.onPrimary else if (inEnabled) vColors.text else vColors.muted, modifier = Modifier.size(26.dp))
		}
		Spacer(Modifier.height(8.dp))
		Text(inLabel, style = DesignType.kLabel, color = if (inEnabled) vColors.text else vColors.muted)
	}
}

/**
 * Gros bouton rond principal (76 dp) avec son libellé.
 */
@Composable
fun PrimaryRoundButton(inIcon: Int, inLabel: String, inEnabled: Boolean) {
	val vColors = kLocalDesignColors.current
	Column(horizontalAlignment = Alignment.CenterHorizontally) {
		Box(Modifier.size(76.dp).background(if (inEnabled) vColors.primary else vColors.track, CircleShape), contentAlignment = Alignment.Center) {
			Icon(painterResource(inIcon), contentDescription = null, tint = if (inEnabled) vColors.onPrimary else vColors.muted, modifier = Modifier.size(30.dp))
		}
		Spacer(Modifier.height(8.dp))
		Text(inLabel, style = DesignType.kLabel, color = if (inEnabled) vColors.text else vColors.muted)
	}
}

/**
 * Bouton principal pleine largeur, en pastille noire.
 */
@Composable
fun PrimaryButton(inText: String, inModifier: Modifier = Modifier, inIcon: Int? = null) {
	val vColors = kLocalDesignColors.current
	Row(
		inModifier.fillMaxWidth().height(60.dp).background(vColors.primary, RoundedCornerShape(30.dp)),
		horizontalArrangement = Arrangement.Center,
		verticalAlignment = Alignment.CenterVertically,
	) {
		inIcon?.let {
			Icon(painterResource(it), contentDescription = null, tint = vColors.onPrimary, modifier = Modifier.size(22.dp))
			Spacer(Modifier.width(10.dp))
		}
		Text(inText, style = DesignType.kBarTitle, color = vColors.onPrimary)
	}
}

/**
 * Bouton secondaire pleine largeur, en pastille claire.
 */
@Composable
fun SecondaryButton(inText: String, inIcon: Int? = null) {
	val vColors = kLocalDesignColors.current
	Row(
		Modifier.fillMaxWidth().height(56.dp).background(vColors.surface, RoundedCornerShape(28.dp)),
		horizontalArrangement = Arrangement.Center,
		verticalAlignment = Alignment.CenterVertically,
	) {
		inIcon?.let {
			Icon(painterResource(it), contentDescription = null, tint = vColors.text, modifier = Modifier.size(20.dp))
			Spacer(Modifier.width(10.dp))
		}
		Text(inText, style = DesignType.kBodyStrong, color = vColors.text)
	}
}

/**
 * Carte blanche (surface) aux grands arrondis.
 */
@Composable
fun SurfaceCard(inModifier: Modifier = Modifier, inContent: @Composable ColumnScope.() -> Unit) {
	val vColors = kLocalDesignColors.current
	Column(inModifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(vColors.surface).padding(20.dp), content = inContent)
}

/**
 * Puce de sélection (thème, filtre).
 */
@Composable
fun SelectChip(inText: String, inSelected: Boolean) {
	val vColors = kLocalDesignColors.current
	Box(
		Modifier
			.clip(RoundedCornerShape(18.dp))
			.background(if (inSelected) vColors.primary else vColors.surface)
			.border(1.dp, if (inSelected) vColors.primary else vColors.track, RoundedCornerShape(18.dp))
			.padding(horizontal = 14.dp, vertical = 8.dp),
	) {
		Text(inText, style = DesignType.kCaption, color = if (inSelected) vColors.onPrimary else vColors.text)
	}
}

/**
 * Contrôle segmenté (onglets, choix exclusif).
 */
@Composable
fun SegmentedControl(inOptions: List<String>, inSelected: Int) {
	val vColors = kLocalDesignColors.current
	Row(Modifier.fillMaxWidth().height(44.dp).background(vColors.track, RoundedCornerShape(22.dp)).padding(4.dp)) {
		inOptions.forEachIndexed { inIndex, inOption ->
			Box(
				Modifier.weight(1f).height(36.dp).background(if (inIndex == inSelected) vColors.surface else vColors.track, RoundedCornerShape(18.dp)),
				contentAlignment = Alignment.Center,
			) {
				Text(inOption, style = DesignType.kCaption, color = if (inIndex == inSelected) vColors.text else vColors.muted, textAlign = TextAlign.Center)
			}
		}
	}
}

/**
 * Ligne de réglage avec un interrupteur.
 */
@Composable
fun SwitchRow(inTitle: String, inSubtitle: String?, inChecked: Boolean) {
	val vColors = kLocalDesignColors.current
	Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
		Column(Modifier.weight(1f)) {
			Text(inTitle, style = DesignType.kBodyStrong, color = vColors.text)
			inSubtitle?.let { Text(it, style = DesignType.kCaption, color = vColors.muted) }
		}
		Switch(
			checked = inChecked,
			onCheckedChange = null,
			colors = SwitchDefaults.colors(
				checkedThumbColor = vColors.onPrimary,
				checkedTrackColor = vColors.primary,
				uncheckedThumbColor = vColors.muted,
				uncheckedTrackColor = vColors.track,
				uncheckedBorderColor = vColors.track,
			),
		)
	}
}

/**
 * Ligne de navigation vers un autre écran (titre, valeur, chevron).
 */
@Composable
fun NavigationRow(inTitle: String, inValue: String?) {
	val vColors = kLocalDesignColors.current
	Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
		Text(inTitle, style = DesignType.kBodyStrong, color = vColors.text, modifier = Modifier.weight(1f))
		inValue?.let { Text(it, style = DesignType.kBody, color = vColors.muted) }
		Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = null, tint = vColors.muted, modifier = Modifier.size(22.dp))
	}
}

/**
 * Onglets de la barre de navigation du bas.
 */
enum class NavTab {
	Home,
	History,
	Stats,
}

/**
 * Barre de navigation du bas à trois onglets : Accueil, Historique, Statistiques.
 */
@Composable
fun BottomNavBar(inSelected: NavTab) {
	val vColors = kLocalDesignColors.current
	val vItems = listOf(
		Triple(NavTab.Home, "Accueil", R.drawable.ic_home to R.drawable.ic_home_fill1),
		Triple(NavTab.History, "Historique", R.drawable.ic_history to R.drawable.ic_history_fill1),
		Triple(NavTab.Stats, "Statistiques", R.drawable.ic_bar_chart to R.drawable.ic_bar_chart_fill1),
	)
	Row(Modifier.fillMaxWidth().background(vColors.surface).navigationBarsPadding().height(72.dp), verticalAlignment = Alignment.CenterVertically) {
		for ((vTab, vLabel, vIcons) in vItems) {
			val vActive = vTab == inSelected
			Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
				Box(
					Modifier.width(56.dp).height(30.dp).background(if (vActive) vColors.track else vColors.surface, RoundedCornerShape(15.dp)),
					contentAlignment = Alignment.Center,
				) {
					Icon(painterResource(if (vActive) vIcons.second else vIcons.first), contentDescription = null, tint = if (vActive) vColors.text else vColors.muted, modifier = Modifier.size(22.dp))
				}
				Spacer(Modifier.height(4.dp))
				Text(vLabel, style = DesignType.kLabel, color = if (vActive) vColors.text else vColors.muted)
			}
		}
	}
}
