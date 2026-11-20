package com.chessplus.data

import com.chessplus.core.CoreInfo

/**
 * Informations générales sur le module :data.
 *
 * Classe provisoire de la phase 1, remplacée par les entités et la base Room en phase 3.
 */
object DataInfo {
	/** Nom du module, utilisé par les tests de mise en place. */
	const val kModuleName = "data"

	/**
	 * Retourne le nom du module dont :data dépend, pour vérifier le câblage vers :core.
	 */
	fun coreModuleName(): String = CoreInfo.kModuleName
}
