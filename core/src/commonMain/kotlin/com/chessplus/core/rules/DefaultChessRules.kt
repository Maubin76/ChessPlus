package com.chessplus.core.rules

/**
 * Retourne l'implémentation des règles des échecs de la plateforme.
 *
 * En v1, Android et la JVM partagent l'implémentation chesslib du source set `jvmShared`.
 */
expect fun defaultChessRules(): ChessRules
