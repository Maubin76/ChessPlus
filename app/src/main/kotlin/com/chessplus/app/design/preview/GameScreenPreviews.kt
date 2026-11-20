package com.chessplus.app.design.preview

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.chessplus.app.R

/** Problème 00sHx après le coup de l'adversaire e8-d7. */
private const val kStartPlacement = "q5nr/1ppknQpp/3p4/1P2p3/4P3/B1PP1b2/B5PP/5K2"

/** Après le bon coup a2-e6. */
private const val kCorrectPlacement = "q5nr/1ppknQpp/3pB3/1P2p3/4P3/B1PP1b2/6PP/5K2"

/** Position finale : mat après f7-f8. */
private const val kMatePlacement = "q2k1Qnr/1pp1n1pp/3pB3/1P2p3/4P3/B1PP1b2/6PP/5K2"

/** Problème de promotion : le pion b7 doit être promu. */
private const val kPromotionPlacement = "8/1P6/8/7k/8/7p/8/2K5"

/**
 * Squelette de l'écran de jeu : barre, message, échiquier pleine largeur, zone libre, commandes.
 */
@Composable
private fun GameScreen(
	inHeaderTitle: String,
	inHeaderSubtitle: String,
	inTone: FeedbackTone,
	inPlacement: String,
	inMarks: BoardMarks,
	inNextEnabled: Boolean,
	inHint: String? = null,
) {
	val vColors = kLocalDesignColors.current
	Column(Modifier.fillMaxSize().statusBarsPadding()) {
		GameTopBar("Entraînement", "1 520")
		FeedbackHeader(inHeaderTitle, inHeaderSubtitle, inTone)
		PreviewBoard(inPlacement, inMarks, Modifier.fillMaxWidth())
		Box(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 24.dp), contentAlignment = Alignment.Center) {
			inHint?.let { Text(it, style = DesignType.kCaption, color = vColors.muted) }
		}
		GameControls(inNextEnabled)
	}
}

/** Joueur au trait : pièce sélectionnée, coups possibles marqués. */
@DesignPreviews
@Composable
fun GameSelectionPreview() = DesignTheme {
	GameScreen(
		"Les blancs jouent", "Trouvez le meilleur coup", FeedbackTone.Neutral, kStartPlacement,
		BoardMarks(lastMove = setOf("e8", "d7"), selected = "a2", dots = setOf("b1", "b3", "c4", "d5", "e6")),
		inNextEnabled = false, inHint = "Touchez une pièce puis sa case, ou faites-la glisser",
	)
}

/** Bon coup : message vert, pastille sur la case d'arrivée, l'adversaire va répondre. */
@DesignPreviews
@Composable
fun GameCorrectPreview() = DesignTheme {
	GameScreen(
		"Bon coup !", "Les noirs réfléchissent…", FeedbackTone.Success, kCorrectPlacement,
		BoardMarks(lastMove = setOf("a2", "e6"), badgeSquare = "e6"),
		inNextEnabled = false,
	)
}

/** Mauvais coup : message rouge, pastille ✕ sur la case visée, la pièce est revenue. */
@DesignPreviews
@Composable
fun GameWrongPreview() = DesignTheme {
	GameScreen(
		"Pas ce coup-là", "Réessayez, ou demandez un indice", FeedbackTone.Error, kStartPlacement,
		BoardMarks(lastMove = setOf("e8", "d7"), badgeSquare = "d5", badgeKind = BadgeKind.Error),
		inNextEnabled = false, inHint = "Le problème compte comme échoué, mais vous pouvez le terminer",
	)
}

/** Promotion : choix de la pièce par-dessus l'échiquier. */
@DesignPreviews
@Composable
fun GamePromotionPreview() = DesignTheme {
	val vColors = kLocalDesignColors.current
	Box(Modifier.fillMaxSize()) {
		GameScreen(
			"Les blancs jouent", "Trouvez le meilleur coup", FeedbackTone.Neutral, kPromotionPlacement,
			BoardMarks(lastMove = setOf("h4", "h3"), selected = "b7", dots = setOf("b8")),
			inNextEnabled = false,
		)
		Box(Modifier.fillMaxSize().background(Color(0x66000000)))
		Column(
			Modifier.align(Alignment.Center).padding(horizontal = 24.dp).fillMaxWidth().background(vColors.surface, RoundedCornerShape(28.dp)).padding(20.dp),
			horizontalAlignment = Alignment.CenterHorizontally,
		) {
			Text("Promotion", style = DesignType.kSection, color = vColors.text)
			Spacer(Modifier.height(4.dp))
			Text("Choisissez la nouvelle pièce", style = DesignType.kBody, color = vColors.muted)
			Spacer(Modifier.height(16.dp))
			Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
				for (vPiece in listOf(R.drawable.piece_wq, R.drawable.piece_wr, R.drawable.piece_wb, R.drawable.piece_wn)) {
					Box(Modifier.size(70.dp).background(vColors.squareLight, RoundedCornerShape(18.dp)), contentAlignment = Alignment.Center) {
						Image(painterResource(vPiece), contentDescription = null, modifier = Modifier.size(60.dp))
					}
				}
			}
		}
	}
}

/**
 * Volet de bilan affiché en bas de l'écran à la fin d'un problème.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SummarySheet(
	inSuccess: Boolean,
	inDetail: String,
	inRating: String,
	inDelta: String,
) {
	val vColors = kLocalDesignColors.current
	val vColor = if (inSuccess) vColors.success else vColors.error
	Column(
		Modifier.fillMaxWidth().background(vColors.surface, RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)).navigationBarsPadding().padding(horizontal = 24.dp, vertical = 12.dp),
	) {
		Box(Modifier.align(Alignment.CenterHorizontally).width(40.dp).height(4.dp).background(vColors.track, RoundedCornerShape(2.dp)))
		Spacer(Modifier.height(18.dp))
		Row(verticalAlignment = Alignment.CenterVertically) {
			Icon(painterResource(if (inSuccess) R.drawable.ic_check_circle_fill1 else R.drawable.ic_cancel_fill1), contentDescription = null, tint = vColor, modifier = Modifier.size(28.dp))
			Spacer(Modifier.width(8.dp))
			Text(if (inSuccess) "Problème réussi" else "Problème échoué", style = DesignType.kFeedback, color = vColor)
		}
		Text(inDetail, style = DesignType.kBody, color = vColors.muted, modifier = Modifier.padding(top = 4.dp))
		Spacer(Modifier.height(18.dp))
		Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
			Column(Modifier.weight(1f).background(vColors.background, RoundedCornerShape(18.dp)).padding(14.dp)) {
				Text("Ma cote", style = DesignType.kCaption, color = vColors.muted)
				Row(verticalAlignment = Alignment.Bottom) {
					Text(inRating, style = DesignType.kSection, color = vColors.text)
					Spacer(Modifier.width(6.dp))
					Text(inDelta, style = DesignType.kBodyStrong, color = vColor)
				}
			}
			Column(Modifier.weight(1f).background(vColors.background, RoundedCornerShape(18.dp)).padding(14.dp)) {
				Text("Cote du problème", style = DesignType.kCaption, color = vColors.muted)
				Text("1 760", style = DesignType.kSection, color = vColors.text)
			}
		}
		Spacer(Modifier.height(14.dp))
		Text("Thèmes", style = DesignType.kCaption, color = vColors.muted)
		Spacer(Modifier.height(8.dp))
		FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
			for (vTheme in listOf("Mat", "Mat en 2", "Milieu de partie", "Court")) {
				SelectChip(vTheme, false)
			}
		}
		Spacer(Modifier.height(14.dp))
		Row(Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
			Icon(painterResource(R.drawable.ic_open_in_new), contentDescription = null, tint = vColors.text, modifier = Modifier.size(18.dp))
			Spacer(Modifier.width(8.dp))
			Text("Voir sur Lichess", style = DesignType.kBodyStrong, color = vColors.text)
			Spacer(Modifier.width(6.dp))
			Text("(si le réseau est disponible)", style = DesignType.kCaption, color = vColors.muted)
		}
		Spacer(Modifier.height(12.dp))
		PrimaryButton("Suivant", inIcon = R.drawable.ic_skip_next)
		Spacer(Modifier.height(12.dp))
	}
}

/** Problème réussi : position finale (mat) et bilan. */
@DesignPreviews
@Composable
fun GameSolvedPreview() = DesignTheme {
	Box(Modifier.fillMaxSize()) {
		GameScreen(
			"Mat !", "Problème terminé", FeedbackTone.Success, kMatePlacement,
			BoardMarks(lastMove = setOf("f7", "f8"), badgeSquare = "f8"),
			inNextEnabled = true,
		)
		Box(Modifier.fillMaxSize().background(Color(0x33000000)))
		Box(Modifier.align(Alignment.BottomCenter)) {
			SummarySheet(true, "Trouvé du premier coup, sans indice", "1 532", "+12")
		}
	}
}

/** Problème échoué : bilan après une erreur. */
@DesignPreviews
@Composable
fun GameFailedPreview() = DesignTheme {
	Box(Modifier.fillMaxSize()) {
		GameScreen(
			"Problème terminé", "Solution affichée", FeedbackTone.Neutral, kMatePlacement,
			BoardMarks(lastMove = setOf("f7", "f8")),
			inNextEnabled = true,
		)
		Box(Modifier.fillMaxSize().background(Color(0x33000000)))
		Box(Modifier.align(Alignment.BottomCenter)) {
			SummarySheet(false, "Une erreur au premier coup, solution vue ensuite", "1 511", "−9")
		}
	}
}
