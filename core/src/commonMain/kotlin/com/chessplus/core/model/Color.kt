package com.chessplus.core.model

/**
 * Camp aux échecs.
 */
enum class Color {
	White,
	Black;

	/**
	 * Retourne le camp adverse.
	 */
	fun opposite(): Color = if (this == White) Black else White
}
