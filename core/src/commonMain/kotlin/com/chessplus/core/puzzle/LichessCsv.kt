package com.chessplus.core.puzzle

import com.chessplus.core.model.Move
import com.chessplus.core.model.ThemeSet

/**
 * Ligne brute du CSV des problèmes Lichess, avec les seules colonnes utiles à ChessPlus.
 *
 * @property id colonne `PuzzleId`.
 * @property fen colonne `FEN`.
 * @property moves colonne `Moves` (coups UCI séparés par des espaces).
 * @property rating colonne `Rating`.
 * @property ratingDeviation colonne `RatingDeviation`, pour le filtrage.
 * @property popularity colonne `Popularity` (de -100 à 100), pour le filtrage.
 * @property nbPlays colonne `NbPlays`, pour le filtrage.
 * @property themes colonne `Themes` (identifiants séparés par des espaces).
 */
data class LichessCsvRow(
	val id: String,
	val fen: String,
	val moves: String,
	val rating: Int,
	val ratingDeviation: Int,
	val popularity: Int,
	val nbPlays: Int,
	val themes: String,
) {
	/**
	 * Convertit la ligne en problème : décode les coups et les thèmes.
	 *
	 * @throws IllegalArgumentException si un coup est invalide ou si leur nombre est impair.
	 * @throws com.chessplus.core.model.UnknownThemeException si un thème est inconnu.
	 */
	fun toPuzzle(): Puzzle = Puzzle(
		id = id,
		fen = fen,
		moves = Move.listFromUci(moves),
		rating = rating,
		themes = ThemeSet.parse(themes),
	)
}

/**
 * Découpage des lignes du CSV des problèmes Lichess.
 *
 * Aucun champ ne contient de virgule ni de guillemet : un simple découpage sur la virgule
 * suffit, à condition de conserver les champs vides en fin de ligne.
 */
object LichessCsv {
	/** En-tête attendu en première ligne du fichier. */
	const val kHeader = "PuzzleId,FEN,Moves,Rating,RatingDeviation,Popularity,NbPlays,Themes,GameUrl,OpeningTags,DailyDate"

	/** Nombre de champs d'une ligne. */
	const val kFieldCount = 11

	/** Index de la colonne PuzzleId. */
	private const val kIdIndex = 0

	/** Index de la colonne FEN. */
	private const val kFenIndex = 1

	/** Index de la colonne Moves. */
	private const val kMovesIndex = 2

	/** Index de la colonne Rating. */
	private const val kRatingIndex = 3

	/** Index de la colonne RatingDeviation. */
	private const val kRatingDeviationIndex = 4

	/** Index de la colonne Popularity. */
	private const val kPopularityIndex = 5

	/** Index de la colonne NbPlays. */
	private const val kNbPlaysIndex = 6

	/** Index de la colonne Themes. */
	private const val kThemesIndex = 7

	/**
	 * Découpe une ligne de données.
	 *
	 * Le `split` de Kotlin conserve les champs vides finaux (`OpeningTags`, `DailyDate`),
	 * contrairement à `String.split` de Java.
	 *
	 * @throws CsvFormatException si la ligne ne compte pas exactement 11 champs, si un nombre est
	 * invalide ou si le nombre de coups est impair.
	 */
	fun parseLine(inLine: String): LichessCsvRow {
		val vFields = inLine.split(',')
		if (vFields.size != kFieldCount) {
			throw CsvFormatException("$kFieldCount champs attendus, ${vFields.size} trouvés", inLine)
		}
		val vMoves = vFields[kMovesIndex]
		val vMoveCount = vMoves.split(' ').count { it.isNotEmpty() }
		if (vMoveCount == 0 || vMoveCount % 2 != 0) {
			throw CsvFormatException("nombre de coups invalide ($vMoveCount)", inLine)
		}
		if (vFields[kIdIndex].isEmpty() || vFields[kFenIndex].isEmpty()) {
			throw CsvFormatException("identifiant ou FEN vide", inLine)
		}
		return LichessCsvRow(
			id = vFields[kIdIndex],
			fen = vFields[kFenIndex],
			moves = vMoves,
			rating = parseInt(vFields[kRatingIndex], "Rating", inLine),
			ratingDeviation = parseInt(vFields[kRatingDeviationIndex], "RatingDeviation", inLine),
			popularity = parseInt(vFields[kPopularityIndex], "Popularity", inLine),
			nbPlays = parseInt(vFields[kNbPlaysIndex], "NbPlays", inLine),
			themes = vFields[kThemesIndex],
		)
	}

	/**
	 * Lit un entier d'une colonne, ou lève une [CsvFormatException] qui nomme la colonne.
	 */
	private fun parseInt(inValue: String, inColumn: String, inLine: String): Int =
		inValue.toIntOrNull() ?: throw CsvFormatException("$inColumn n'est pas un entier : '$inValue'", inLine)
}

/**
 * Levée quand une ligne du CSV Lichess est mal formée.
 *
 * @property line ligne fautive.
 */
class CsvFormatException(inReason: String, val line: String) : IllegalArgumentException("Ligne CSV invalide ($inReason) : $line")
