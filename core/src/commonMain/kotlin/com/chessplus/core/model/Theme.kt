package com.chessplus.core.model

/**
 * Thème de problème Lichess.
 *
 * Chaque thème occupe un bit fixe du masque stocké en base : **ne jamais changer le bit
 * d'un thème existant**, seulement en ajouter. Les 73 thèmes recensés dans le CSV du
 * 2026-11-20 dépassent 64, d'où un masque sur deux entiers 64 bits (voir [ThemeSet]).
 *
 * @property bit position du thème dans le masque, de 0 à 127.
 * @property key identifiant du thème dans le CSV Lichess.
 */
enum class Theme(val bit: Int, val key: String) {
	AdvancedPawn(0, "advancedPawn"),
	Advantage(1, "advantage"),
	AnastasiaMate(2, "anastasiaMate"),
	ArabianMate(3, "arabianMate"),
	AttackingF2F7(4, "attackingF2F7"),
	Attraction(5, "attraction"),
	BackRankMate(6, "backRankMate"),
	BalestraMate(7, "balestraMate"),
	BishopEndgame(8, "bishopEndgame"),
	BlindSwineMate(9, "blindSwineMate"),
	BodenMate(10, "bodenMate"),
	CapturingDefender(11, "capturingDefender"),
	Castling(12, "castling"),
	Clearance(13, "clearance"),
	CollinearMove(14, "collinearMove"),
	CornerMate(15, "cornerMate"),
	Crushing(16, "crushing"),
	DefensiveMove(17, "defensiveMove"),
	Deflection(18, "deflection"),
	DiscoveredAttack(19, "discoveredAttack"),
	DiscoveredCheck(20, "discoveredCheck"),
	DoubleBishopMate(21, "doubleBishopMate"),
	DoubleCheck(22, "doubleCheck"),
	DovetailMate(23, "dovetailMate"),
	EnPassant(24, "enPassant"),
	Endgame(25, "endgame"),
	EpauletteMate(26, "epauletteMate"),
	Equality(27, "equality"),
	ExposedKing(28, "exposedKing"),
	Fork(29, "fork"),
	HangingPiece(30, "hangingPiece"),
	HookMate(31, "hookMate"),
	Interference(32, "interference"),
	Intermezzo(33, "intermezzo"),
	KillBoxMate(34, "killBoxMate"),
	KingsideAttack(35, "kingsideAttack"),
	KnightEndgame(36, "knightEndgame"),
	Long(37, "long"),
	Master(38, "master"),
	MasterVsMaster(39, "masterVsMaster"),
	Mate(40, "mate"),
	MateIn1(41, "mateIn1"),
	MateIn2(42, "mateIn2"),
	MateIn3(43, "mateIn3"),
	MateIn4(44, "mateIn4"),
	MateIn5(45, "mateIn5"),
	Middlegame(46, "middlegame"),
	MorphysMate(47, "morphysMate"),
	OneMove(48, "oneMove"),
	Opening(49, "opening"),
	OperaMate(50, "operaMate"),
	PawnEndgame(51, "pawnEndgame"),
	PillsburysMate(52, "pillsburysMate"),
	Pin(53, "pin"),
	Promotion(54, "promotion"),
	QueenEndgame(55, "queenEndgame"),
	QueenRookEndgame(56, "queenRookEndgame"),
	QueensideAttack(57, "queensideAttack"),
	QuietMove(58, "quietMove"),
	RookEndgame(59, "rookEndgame"),
	Sacrifice(60, "sacrifice"),
	Short(61, "short"),
	Skewer(62, "skewer"),
	SmotheredMate(63, "smotheredMate"),
	SuperGM(64, "superGM"),
	SwallowstailMate(65, "swallowstailMate"),
	TrappedPiece(66, "trappedPiece"),
	TriangleMate(67, "triangleMate"),
	UnderPromotion(68, "underPromotion"),
	VeryLong(69, "veryLong"),
	VukovicMate(70, "vukovicMate"),
	XRayAttack(71, "xRayAttack"),
	Zugzwang(72, "zugzwang");

	companion object {
		/** Nombre maximal de bits du masque (deux entiers de 64 bits). */
		const val kMaxBits = 128

		/** Index des thèmes par identifiant Lichess. */
		private val kByKey: Map<String, Theme> = entries.associateBy { it.key }

		/**
		 * Retourne le thème correspondant à un identifiant Lichess, ou null s'il est inconnu.
		 */
		fun fromKey(inKey: String): Theme? = kByKey[inKey]
	}
}
