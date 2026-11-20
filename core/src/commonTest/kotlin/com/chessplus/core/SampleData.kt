package com.chessplus.core

import com.chessplus.core.puzzle.LichessCsv
import com.chessplus.core.puzzle.Puzzle

/**
 * Données de test : extrait réel du CSV Lichess donné dans la spécification.
 */
object SampleData {
	/** En-tête du fichier. */
	const val kHeader = "PuzzleId,FEN,Moves,Rating,RatingDeviation,Popularity,NbPlays,Themes,GameUrl,OpeningTags,DailyDate"

	/** Mat en 2 : les noirs jouent e8d7, le joueur (blancs) doit trouver a2e6 puis f7f8. */
	const val kLine00sHx = "00sHx,q3k1nr/1pp1nQpp/3p4/1P2p3/4P3/B1PP1b2/B5PP/5K2 b k - 0 17,e8d7 a2e6 d7d8 f7f8,1760,80,83,72,mate mateIn2 middlegame short,https://lichess.org/yyznGmXs/black#34,Italian_Game Italian_Game_Classical_Variation,"

	/** Problème long avec un horodatage DailyDate renseigné. */
	const val kLine00sJ9 = "00sJ9,r3r1k1/p4ppp/2p2n2/1p6/3P1qb1/2NQR3/PPB2PP1/R1B3K1 w - - 5 18,e3g3 e8e1 g1h2 e1c1 a1c1 f4h6 h2g1 h6c1,2671,105,87,325,advantage attraction fork middlegame sacrifice veryLong,https://lichess.org/gyFeQsOE#35,French_Defense French_Defense_Exchange_Variation,1607774862751"

	/** Problème long avec fourchette. */
	const val kLine00sJb = "00sJb,Q1b2r1k/p2np2p/5bp1/q7/5P2/4B3/PPP3PP/2KR1B1R w - - 1 17,d1d7 a5e1 d7d1 e1e3 c1b1 e3b6,2235,76,97,64,advantage fork long,https://lichess.org/kiuvTFoE#33,Sicilian_Defense Sicilian_Defense_Dragon_Variation,"

	/** Ligne terminée par deux virgules (OpeningTags et DailyDate vides). */
	const val kLine00sO1 = "00sO1,1k1r4/pp3pp1/2p1p3/4b3/P3n1P1/8/KPP2PN1/3rBR1R b - - 2 31,b8c7 e1a5 b7b6 f1d1,998,85,94,293,advantage discoveredAttack master middlegame short,https://lichess.org/vsfFkG0s/black#62,,"

	/**
	 * Problème réel (hors extrait de la spécification) avec un mat alternatif au dernier coup :
	 * la solution finit par d8g8#, mais d8h8# est aussi mat.
	 */
	const val kLine00Mgf = "00Mgf,2RQ4/p4pp1/4p1kp/8/6PP/q3PPK1/1r6/8 b - - 0 35,a3e3 h4h5 g6h7 d8g8,1124,89,90,254,endgame mate mateIn2 short,https://lichess.org/game/Phr30rbU/black#70,,"

	/** Les quatre lignes de l'extrait. */
	val kLines = listOf(kLine00sHx, kLine00sJ9, kLine00sJb, kLine00sO1)

	/** Les quatre problèmes décodés. */
	val kPuzzles: List<Puzzle> by lazy { kLines.map { LichessCsv.parseLine(it).toPuzzle() } }

	/**
	 * Retourne le problème d'identifiant donné parmi les quatre de l'extrait.
	 */
	fun puzzle(inId: String): Puzzle = kPuzzles.first { it.id == inId }
}
