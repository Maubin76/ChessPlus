// Build racine : déclare les plugins sans les appliquer, chaque module choisit les siens.
plugins {
	alias(libs.plugins.android.application) apply false
	alias(libs.plugins.android.kmp.library) apply false
	alias(libs.plugins.kotlin.multiplatform) apply false
	alias(libs.plugins.kotlin.jvm) apply false
	alias(libs.plugins.kotlin.compose) apply false
	alias(libs.plugins.ksp) apply false
	alias(libs.plugins.room) apply false
}
