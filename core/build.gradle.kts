import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType

// Module :core : modèle, codec, règles des échecs et logique des sessions.
// Aucune dépendance Android ni Room ; le code qui utilise une bibliothèque Java vit dans jvmShared.
plugins {
	alias(libs.plugins.kotlin.multiplatform)
	alias(libs.plugins.android.kmp.library)
}

kotlin {
	jvmToolchain(21)

	android {
		namespace = "com.chessplus.core"
		compileSdk = libs.versions.android.compileSdk.get().toInt()
		minSdk = libs.versions.android.minSdk.get().toInt()
		compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
		withHostTest {}
	}

	jvm {
		compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
	}

	// Source set intermédiaire jvmShared, partagé par Android et la JVM, pour le code qui utilise chesslib.
	@OptIn(ExperimentalKotlinGradlePluginApi::class)
	applyDefaultHierarchyTemplate {
		common {
			group("jvmShared") {
				// withAndroidTarget() ne reconnaît pas la cible du plugin com.android.kotlin.multiplatform.library.
				withCompilations {
					it.platformType == KotlinPlatformType.jvm || it.platformType == KotlinPlatformType.androidJvm
				}
			}
		}
	}

	sourceSets {
		commonTest.dependencies {
			implementation(libs.kotlin.test)
		}
		getByName("jvmSharedMain").dependencies {
			implementation(libs.chesslib)
		}
	}
}
