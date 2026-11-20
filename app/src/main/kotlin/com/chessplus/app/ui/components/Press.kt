package com.chessplus.app.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

/** Échelle d'un élément pendant l'appui. */
private const val kPressedScale = 0.95f

/**
 * Retour visuel d'un appui : léger enfoncement animé, partagé avec l'onde du clic.
 *
 * L'échelle ([scale]) se place en tête de la chaîne de modificateurs, pour enfoncer aussi le fond ;
 * le clic ([pressClickable]) se place après la découpe, pour que l'onde suive la forme.
 *
 * @property source source des interactions, commune à l'échelle et au clic.
 * @property scale modificateur d'échelle.
 */
class PressFeedback(val source: MutableInteractionSource, val scale: Modifier)

/**
 * Crée le retour visuel d'appui d'un élément.
 */
@Composable
fun rememberPressFeedback(): PressFeedback {
	val vSource = remember { MutableInteractionSource() }
	val vPressed by vSource.collectIsPressedAsState()
	val vScale by animateFloatAsState(
		if (vPressed) kPressedScale else 1f,
		spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
		label = "appui",
	)
	return PressFeedback(vSource, Modifier.graphicsLayer {
		scaleX = vScale
		scaleY = vScale
	})
}

/**
 * Rend l'élément cliquable avec l'onde du clic et l'enfoncement de [inPress].
 */
@Composable
fun Modifier.pressClickable(inPress: PressFeedback, inEnabled: Boolean = true, inOnClick: () -> Unit): Modifier =
	clickable(interactionSource = inPress.source, indication = ripple(), enabled = inEnabled, onClick = inOnClick)
