package com.chessplus.app.game

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.chessplus.app.R
import com.chessplus.app.ui.components.Overline
import com.chessplus.app.ui.components.kIconSize
import com.chessplus.app.ui.theme.ChessPlusTheme
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Hauteur d'une case du calendrier. */
private const val kDayCellHeight = 28

/** Nombre de mois du passé accessibles depuis le mois courant. */
private const val kMonthsBack = 12

/** Format du mois affiché (« novembre 2026 »). */
private val kMonthFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("LLLL yyyy", Locale.FRANCE)

/** Format d'un jour pour l'accessibilité (« 12 novembre »). */
private val kDayFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM", Locale.FRANCE)

/**
 * Calendrier des puzzles du jour : un mois à la fois (lundi en premier), jours résolus pleins en
 * `accent`, aujourd'hui cerclé, jour affiché en `accentSoft`, jours à venir inactifs. Toucher un jour
 * passé ou aujourd'hui charge son puzzle.
 */
@Composable
fun DailyCalendar(inDaily: DailyUi, inOnDay: (String) -> Unit) {
	val vColors = ChessPlusTheme.colors
	val vToday = DailyDays.dateOf(inDaily.today)
	val vCurrentMonth = YearMonth.from(vToday)
	// Décalage du mois affiché par rapport au mois du jour affiché (0 : son mois).
	var vOffset by rememberSaveable(inDaily.day) { mutableIntStateOf(0) }
	val vMonth = YearMonth.from(DailyDays.dateOf(inDaily.day)).plusMonths(vOffset.toLong())
	val vCanGoBack = vMonth.isAfter(vCurrentMonth.minusMonths(kMonthsBack.toLong()))
	val vCanGoForward = vMonth.isBefore(vCurrentMonth)
	Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(ChessPlusTheme.spacing.space2)) {
		Row(verticalAlignment = Alignment.CenterVertically) {
			Column(Modifier.weight(1f)) {
				Overline(stringResource(R.string.daily_calendar))
				Text(
					vMonth.format(kMonthFormat).replaceFirstChar { it.titlecase(Locale.FRANCE) },
					style = ChessPlusTheme.typography.headingSmall,
					color = vColors.ink,
				)
			}
			MonthButton(R.string.daily_previous_month, vCanGoBack, inMirror = true) { vOffset-- }
			MonthButton(R.string.daily_next_month, vCanGoForward, inMirror = false) { vOffset++ }
		}
		Row(Modifier.fillMaxWidth()) {
			for (vLetter in stringResource(R.string.daily_weekdays).split(' ')) {
				Text(vLetter, style = ChessPlusTheme.typography.overline, color = vColors.inkMuted, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
			}
		}
		val vFirst = vMonth.atDay(1)
		val vLeading = vFirst.dayOfWeek.value - 1
		val vCells = List(vLeading) { null } + (1..vMonth.lengthOfMonth()).map { vMonth.atDay(it) }
		for (vWeek in vCells.chunked(7)) {
			Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
				for (vIndex in 0 until 7) {
					val vDate = vWeek.getOrNull(vIndex)
					Box(Modifier.weight(1f).height(kDayCellHeight.dp)) {
						if (vDate != null) {
							val vKey = DailyDays.keyOf(vDate)
							DayCell(
								inNumber = vDate.dayOfMonth,
								inLabel = vDate.format(kDayFormat),
								inSolved = vKey in inDaily.solvedDays,
								inToday = vDate == vToday,
								inShown = vKey == inDaily.day,
								inEnabled = !vDate.isAfter(vToday),
							) { inOnDay(vKey) }
						}
					}
				}
			}
		}
		Text(stringResource(R.string.daily_calendar_hint), style = ChessPlusTheme.typography.small, color = vColors.inkMuted)
	}
}

/**
 * Bouton de changement de mois (chevron).
 */
@Composable
private fun MonthButton(inDescription: Int, inEnabled: Boolean, inMirror: Boolean, inOnClick: () -> Unit) {
	val vColors = ChessPlusTheme.colors
	val vDescription = stringResource(inDescription)
	Box(
		Modifier
			.size(40.dp)
			.clip(RoundedCornerShape(ChessPlusTheme.radius.radiusMd))
			.clickable(enabled = inEnabled, onClick = inOnClick)
			.semantics { contentDescription = vDescription },
		contentAlignment = Alignment.Center,
	) {
		Icon(
			painterResource(R.drawable.ic_ln_chevron),
			contentDescription = null,
			tint = if (inEnabled) vColors.ink else vColors.line,
			modifier = Modifier.size(kIconSize).then(if (inMirror) Modifier.rotate(180f) else Modifier),
		)
	}
}

/**
 * Case d'un jour : numéro en mono ; plein `accent` si résolu, `accentSoft` si c'est le jour affiché,
 * bordure `accent` pour aujourd'hui, atténuée pour les jours à venir.
 */
@Composable
private fun DayCell(inNumber: Int, inLabel: String, inSolved: Boolean, inToday: Boolean, inShown: Boolean, inEnabled: Boolean, inOnClick: () -> Unit) {
	val vColors = ChessPlusTheme.colors
	val vShape = RoundedCornerShape(ChessPlusTheme.radius.radiusSm)
	val vDescription = when {
		inSolved -> stringResource(R.string.daily_day_solved, inLabel)
		inToday -> stringResource(R.string.daily_day_today, inLabel)
		else -> inLabel
	}
	val vBackground = when {
		inSolved -> vColors.accent
		inShown -> vColors.accentSoft
		else -> vColors.surfaceRaised
	}
	Box(
		Modifier
			.fillMaxSize()
			.clip(vShape)
			.background(vBackground)
			.border(if (inToday || inShown) 1.5.dp else 1.dp, if (inToday || inShown) vColors.accent else vColors.line, vShape)
			.clickable(enabled = inEnabled, onClick = inOnClick)
			.semantics { contentDescription = vDescription },
		contentAlignment = Alignment.Center,
	) {
		Text(
			"$inNumber",
			style = ChessPlusTheme.typography.movesSmall,
			color = when {
				inSolved -> vColors.accentInk
				!inEnabled -> vColors.inkMuted.copy(alpha = 0.5f)
				else -> vColors.ink
			},
		)
	}
}

/**
 * Retourne le titre de l'écran pour un puzzle du jour : « Puzzle du jour » aujourd'hui, sinon
 * « Puzzle du 12 novembre ».
 */
@Composable
fun dailyTitle(inDaily: DailyUi): String =
	if (inDaily.day == inDaily.today) stringResource(R.string.game_title_daily)
	else stringResource(R.string.game_title_daily_past, DailyDays.dateOf(inDaily.day).format(kDayFormat))
