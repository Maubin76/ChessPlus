package com.chessplus.data.puzzle

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.chessplus.core.codec.MoveCodec
import com.chessplus.core.model.ThemeSet
import com.chessplus.core.puzzle.Puzzle

/**
 * Problème stocké dans la base embarquée `puzzles.db`.
 *
 * L'index de sélection couvre la cote, les thèmes et l'identifiant : le comptage et le
 * tirage par rang dans une tranche se font dans l'index seul, sans lire la table
 * (environ 4 ms au lieu de 200 ms avec un filtre de thème, pour 4 Mo de plus).
 *
 * @property id identifiant Lichess.
 * @property fen position de départ, avant le coup de l'adversaire.
 * @property moves coups encodés par [MoveCodec] (2 octets par coup).
 * @property rating difficulté.
 * @property themesLow bits 0 à 63 du masque des thèmes.
 * @property themesHigh bits 64 à 127 du masque des thèmes.
 */
@Entity(
	tableName = PuzzleEntity.kTableName,
	indices = [Index(value = ["rating", "themesLow", "themesHigh", "id"], name = PuzzleEntity.kSelectionIndexName)],
)
data class PuzzleEntity(
	@PrimaryKey val id: String,
	val fen: String,
	@ColumnInfo(typeAffinity = ColumnInfo.BLOB) val moves: ByteArray,
	val rating: Int,
	val themesLow: Long,
	val themesHigh: Long,
) {
	/**
	 * Convertit l'entité en problème du modèle.
	 */
	fun toPuzzle(): Puzzle = Puzzle(
		id = id,
		fen = fen,
		moves = MoveCodec.decode(moves),
		rating = rating,
		themes = ThemeSet(themesLow, themesHigh),
	)

	/**
	 * Égalité sur le contenu du tableau de coups (un `data class` compare les tableaux par référence).
	 */
	override fun equals(other: Any?): Boolean {
		if (this === other) {
			return true
		}
		if (other !is PuzzleEntity) {
			return false
		}
		return id == other.id && fen == other.fen && moves.contentEquals(other.moves) && rating == other.rating &&
			themesLow == other.themesLow && themesHigh == other.themesHigh
	}

	/**
	 * Hachage cohérent avec [equals].
	 */
	override fun hashCode(): Int {
		var vResult = id.hashCode()
		vResult = 31 * vResult + fen.hashCode()
		vResult = 31 * vResult + moves.contentHashCode()
		vResult = 31 * vResult + rating
		vResult = 31 * vResult + themesLow.hashCode()
		vResult = 31 * vResult + themesHigh.hashCode()
		return vResult
	}

	companion object {
		/** Nom de la table. */
		const val kTableName = "puzzle"

		/** Nom de l'index de sélection (cote, thèmes, identifiant). */
		const val kSelectionIndexName = "index_puzzle_selection"

		/**
		 * Crée l'entité correspondant à un problème du modèle.
		 */
		fun fromPuzzle(inPuzzle: Puzzle): PuzzleEntity = PuzzleEntity(
			id = inPuzzle.id,
			fen = inPuzzle.fen,
			moves = MoveCodec.encode(inPuzzle.moves),
			rating = inPuzzle.rating,
			themesLow = inPuzzle.themes.low,
			themesHigh = inPuzzle.themes.high,
		)
	}
}
