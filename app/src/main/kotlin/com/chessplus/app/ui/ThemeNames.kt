package com.chessplus.app.ui

import androidx.annotation.StringRes
import com.chessplus.app.R
import com.chessplus.core.model.Theme

/**
 * Famille de thèmes, pour les regroupements des filtres (docs/DESIGN.md).
 *
 * @property label nom français de la famille.
 */
enum class ThemeFamily(@StringRes val label: Int) {
	Tactics(R.string.theme_family_tactics),
	Mates(R.string.theme_family_mates),
	Endgames(R.string.theme_family_endgames),
	Phase(R.string.theme_family_phase),
	Length(R.string.theme_family_length),
	Evaluation(R.string.theme_family_evaluation);
}

/**
 * Retourne le nom français d'un thème.
 */
@StringRes
fun themeLabel(inTheme: Theme): Int = when (inTheme) {
	Theme.Fork -> R.string.theme_fork
	Theme.Pin -> R.string.theme_pin
	Theme.Skewer -> R.string.theme_skewer
	Theme.DiscoveredAttack -> R.string.theme_discovered_attack
	Theme.DiscoveredCheck -> R.string.theme_discovered_check
	Theme.DoubleCheck -> R.string.theme_double_check
	Theme.Deflection -> R.string.theme_deflection
	Theme.Attraction -> R.string.theme_attraction
	Theme.Sacrifice -> R.string.theme_sacrifice
	Theme.HangingPiece -> R.string.theme_hanging_piece
	Theme.Intermezzo -> R.string.theme_intermezzo
	Theme.CapturingDefender -> R.string.theme_capturing_defender
	Theme.Clearance -> R.string.theme_clearance
	Theme.Interference -> R.string.theme_interference
	Theme.XRayAttack -> R.string.theme_x_ray_attack
	Theme.TrappedPiece -> R.string.theme_trapped_piece
	Theme.QuietMove -> R.string.theme_quiet_move
	Theme.DefensiveMove -> R.string.theme_defensive_move
	Theme.Zugzwang -> R.string.theme_zugzwang
	Theme.ExposedKing -> R.string.theme_exposed_king
	Theme.KingsideAttack -> R.string.theme_kingside_attack
	Theme.QueensideAttack -> R.string.theme_queenside_attack
	Theme.AttackingF2F7 -> R.string.theme_attacking_f2_f7
	Theme.AdvancedPawn -> R.string.theme_advanced_pawn
	Theme.Promotion -> R.string.theme_promotion
	Theme.UnderPromotion -> R.string.theme_under_promotion
	Theme.Castling -> R.string.theme_castling
	Theme.EnPassant -> R.string.theme_en_passant
	Theme.CollinearMove -> R.string.theme_collinear_move
	Theme.Mate -> R.string.theme_mate
	Theme.MateIn1 -> R.string.theme_mate_in1
	Theme.MateIn2 -> R.string.theme_mate_in2
	Theme.MateIn3 -> R.string.theme_mate_in3
	Theme.MateIn4 -> R.string.theme_mate_in4
	Theme.MateIn5 -> R.string.theme_mate_in5
	Theme.BackRankMate -> R.string.theme_back_rank_mate
	Theme.SmotheredMate -> R.string.theme_smothered_mate
	Theme.AnastasiaMate -> R.string.theme_anastasia_mate
	Theme.ArabianMate -> R.string.theme_arabian_mate
	Theme.BalestraMate -> R.string.theme_balestra_mate
	Theme.BlindSwineMate -> R.string.theme_blind_swine_mate
	Theme.BodenMate -> R.string.theme_boden_mate
	Theme.CornerMate -> R.string.theme_corner_mate
	Theme.DoubleBishopMate -> R.string.theme_double_bishop_mate
	Theme.DovetailMate -> R.string.theme_dovetail_mate
	Theme.EpauletteMate -> R.string.theme_epaulette_mate
	Theme.HookMate -> R.string.theme_hook_mate
	Theme.KillBoxMate -> R.string.theme_kill_box_mate
	Theme.MorphysMate -> R.string.theme_morphys_mate
	Theme.OperaMate -> R.string.theme_opera_mate
	Theme.PillsburysMate -> R.string.theme_pillsburys_mate
	Theme.SwallowstailMate -> R.string.theme_swallowstail_mate
	Theme.TriangleMate -> R.string.theme_triangle_mate
	Theme.VukovicMate -> R.string.theme_vukovic_mate
	Theme.PawnEndgame -> R.string.theme_pawn_endgame
	Theme.RookEndgame -> R.string.theme_rook_endgame
	Theme.BishopEndgame -> R.string.theme_bishop_endgame
	Theme.KnightEndgame -> R.string.theme_knight_endgame
	Theme.QueenEndgame -> R.string.theme_queen_endgame
	Theme.QueenRookEndgame -> R.string.theme_queen_rook_endgame
	Theme.Opening -> R.string.theme_opening
	Theme.Middlegame -> R.string.theme_middlegame
	Theme.Endgame -> R.string.theme_endgame
	Theme.OneMove -> R.string.theme_one_move
	Theme.Short -> R.string.theme_short
	Theme.Long -> R.string.theme_long
	Theme.VeryLong -> R.string.theme_very_long
	Theme.Advantage -> R.string.theme_advantage
	Theme.Crushing -> R.string.theme_crushing
	Theme.Equality -> R.string.theme_equality
	Theme.Master -> R.string.theme_master
	Theme.MasterVsMaster -> R.string.theme_master_vs_master
	Theme.SuperGM -> R.string.theme_super_gm
}

/**
 * Retourne la famille d'un thème.
 */
fun themeFamily(inTheme: Theme): ThemeFamily = when (inTheme) {
	Theme.Fork, Theme.Pin, Theme.Skewer, Theme.DiscoveredAttack, Theme.DiscoveredCheck, Theme.DoubleCheck, Theme.Deflection, Theme.Attraction, Theme.Sacrifice, Theme.HangingPiece, Theme.Intermezzo, Theme.CapturingDefender, Theme.Clearance, Theme.Interference, Theme.XRayAttack, Theme.TrappedPiece, Theme.QuietMove, Theme.DefensiveMove, Theme.Zugzwang, Theme.ExposedKing, Theme.KingsideAttack, Theme.QueensideAttack, Theme.AttackingF2F7, Theme.AdvancedPawn, Theme.Promotion, Theme.UnderPromotion, Theme.Castling, Theme.EnPassant, Theme.CollinearMove -> ThemeFamily.Tactics
	Theme.Mate, Theme.MateIn1, Theme.MateIn2, Theme.MateIn3, Theme.MateIn4, Theme.MateIn5, Theme.BackRankMate, Theme.SmotheredMate, Theme.AnastasiaMate, Theme.ArabianMate, Theme.BalestraMate, Theme.BlindSwineMate, Theme.BodenMate, Theme.CornerMate, Theme.DoubleBishopMate, Theme.DovetailMate, Theme.EpauletteMate, Theme.HookMate, Theme.KillBoxMate, Theme.MorphysMate, Theme.OperaMate, Theme.PillsburysMate, Theme.SwallowstailMate, Theme.TriangleMate, Theme.VukovicMate -> ThemeFamily.Mates
	Theme.PawnEndgame, Theme.RookEndgame, Theme.BishopEndgame, Theme.KnightEndgame, Theme.QueenEndgame, Theme.QueenRookEndgame -> ThemeFamily.Endgames
	Theme.Opening, Theme.Middlegame, Theme.Endgame -> ThemeFamily.Phase
	Theme.OneMove, Theme.Short, Theme.Long, Theme.VeryLong -> ThemeFamily.Length
	Theme.Advantage, Theme.Crushing, Theme.Equality, Theme.Master, Theme.MasterVsMaster, Theme.SuperGM -> ThemeFamily.Evaluation
}
