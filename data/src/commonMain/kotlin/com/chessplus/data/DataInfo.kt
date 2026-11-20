package com.chessplus.data

import com.chessplus.core.model.Theme

/**
 * Informations générales sur le module :data.
 *
 * Classe provisoire de la phase 1, remplacée par les entités et la base Room en phase 3.
 */
object DataInfo {
	/** Nom du module, utilisé par les tests de mise en place. */
	const val kModuleName = "data"

	/**
	 * Retourne le nombre de thèmes connus de :core, pour vérifier le câblage vers :core.
	 */
	fun coreThemeCount(): Int = Theme.entries.size
}
