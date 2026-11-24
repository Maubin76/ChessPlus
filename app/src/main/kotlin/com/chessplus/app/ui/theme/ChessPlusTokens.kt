package com.chessplus.app.ui.theme

// Généré depuis tokens/tokens.json (design system ChessPlus). Ne pas modifier à la main : régénérer.

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Immutable
data class ChessPlusColors(
    val surface: Color,
    val surfaceRaised: Color,
    val surfaceSunken: Color,
    val ink: Color,
    val inkMuted: Color,
    val line: Color,
    val lineStrong: Color,
    val accent: Color,
    val accentInk: Color,
    val accentSoft: Color,
    val boardLight: Color,
    val boardDark: Color,
    val boardLast: Color,
    val boardSelect: Color,
    val boardHint: Color,
    val pieceWhite: Color,
    val pieceBlack: Color,
    val success: Color,
    val danger: Color,
    val streak: Color,
)

val LightChessPlusColors = ChessPlusColors(
    surface = Color(0xFFF4F2EE),
    surfaceRaised = Color(0xFFFFFFFF),
    surfaceSunken = Color(0xFFEAE7E1),
    ink = Color(0xFF16181A),
    inkMuted = Color(0xFF5F6368),
    line = Color(0xFFDCD8D0),
    lineStrong = Color(0xFF8C8F93),
    accent = Color(0xFF1F6F5C),
    accentInk = Color(0xFFFFFFFF),
    accentSoft = Color(0xFFDCEBE6),
    boardLight = Color(0xFFE9E4D8),
    boardDark = Color(0xFF8FA094),
    boardLast = Color(214, 178, 62, alpha = 115),
    boardSelect = Color(31, 111, 92, alpha = 115),
    boardHint = Color(0, 0, 0, alpha = 36),
    pieceWhite = Color(0xFFFBFAF7),
    pieceBlack = Color(0xFF17181A),
    success = Color(0xFF2E7D4F),
    danger = Color(0xFFB3261E),
    streak = Color(0xFFD9600F),
)

val DarkChessPlusColors = ChessPlusColors(
    surface = Color(0xFF121314),
    surfaceRaised = Color(0xFF1B1D1F),
    surfaceSunken = Color(0xFF0C0D0E),
    ink = Color(0xFFECEAE5),
    inkMuted = Color(0xFF9A9DA1),
    line = Color(0xFF2A2D30),
    lineStrong = Color(0xFF6B6F74),
    accent = Color(0xFF5CBFA5),
    accentInk = Color(0xFF0D1F1A),
    accentSoft = Color(0xFF173029),
    boardLight = Color(0xFFC9C3B5),
    boardDark = Color(0xFF6F7D72),
    boardLast = Color(214, 178, 62, alpha = 128),
    boardSelect = Color(92, 191, 165, alpha = 128),
    boardHint = Color(0, 0, 0, alpha = 51),
    pieceWhite = Color(0xFFFBFAF7),
    pieceBlack = Color(0xFF17181A),
    success = Color(0xFF6CC98F),
    danger = Color(0xFFF2867D),
    streak = Color(0xFFF28A3C),
)

object ChessPlusSpacing {
    val space1 = 4.dp // Écart icône/texte, entre chiffre et unité.
    val space2 = 8.dp // Écart entre éléments d'un même groupe.
    val space3 = 12.dp // Padding vertical des lignes de liste.
    val space4 = 16.dp // Marges latérales d'écran, padding des cartes.
    val space6 = 24.dp // Écart entre sections.
    val space8 = 32.dp // Respiration au-dessus des titres de page.
}

object ChessPlusRadius {
    val radius0 = 0.dp // Cases de l'échiquier (le plateau lui-même est arrondi par radius-sm).
    val radiusXs = 3.dp // Petits éléments : curseur de toggle, cellules de heatmap, barres de progression.
    val radiusSm = 6.dp // Chips, tags, toggles, contrôles segmentés, tuiles d'icônes, échiquier.
    val radiusMd = 10.dp // Boutons, cartes, listes, champs.
    val radiusLg = 20.dp // Feuilles modales et cadre d'appareil des maquettes. Jamais de pilule complète.
}
