import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

// Module :app : application Android (écrans, navigation, ViewModels).
// AGP 9 intègre le support Kotlin : le plugin org.jetbrains.kotlin.android n'est pas appliqué.
plugins {
	alias(libs.plugins.android.application)
	alias(libs.plugins.kotlin.compose)
	alias(libs.plugins.compose.screenshot)
}

// Clé de signature de la version release : lue dans le gradle.properties de l'utilisateur ou dans
// local.properties (jamais versionnés), comme pour PatrimoinePlus. Absente : la release est signée
// avec la clé de debug, ce qui suffit pour l'installer sur son téléphone.
val kLocal = Properties().apply { rootProject.file("local.properties").takeIf { it.exists() }?.reader()?.use { load(it) } }
val kSigning = { inName: String -> (findProperty("chessplus.$inName") ?: kLocal.getProperty("chessplus.$inName"))?.toString() }
val kKeystore = kSigning("keystore")?.let { file(it) }?.takeIf { it.exists() }

android {
	namespace = "com.chessplus.app"
	compileSdk = libs.versions.android.compileSdk.get().toInt()

	defaultConfig {
		applicationId = "com.chessplus.app"
		minSdk = libs.versions.android.minSdk.get().toInt()
		targetSdk = libs.versions.android.targetSdk.get().toInt()
		versionCode = property("chessplus.versionCode").toString().toInt()
		versionName = property("chessplus.version").toString()
	}

	signingConfigs {
		if (kKeystore != null) {
			create("publication") {
				storeFile = kKeystore
				storePassword = kSigning("keyPassword")
				keyAlias = kSigning("keyAlias")
				keyPassword = kSigning("keyPassword")
			}
		}
	}

	buildTypes {
		release {
			isMinifyEnabled = true
			isShrinkResources = true
			signingConfig = signingConfigs.findByName("publication") ?: signingConfigs.getByName("debug")
			proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
			// Bibliothèques natives (SQLite intégré, Compose) pour les téléphones 64 bits ARM seulement :
			// tous les téléphones récents, dont le Galaxy S24. -Pchessplus.allAbis garde toutes les
			// architectures (test de l'APK release sur un émulateur x86_64 en CI).
			if (findProperty("chessplus.allAbis") == null) {
				ndk { abiFilters += "arm64-v8a" }
			}
		}
	}

	compileOptions {
		sourceCompatibility = JavaVersion.VERSION_17
		targetCompatibility = JavaVersion.VERSION_17
	}

	buildFeatures {
		compose = true
	}

	// Captures des maquettes : ./gradlew :app:updateDebugScreenshotTest
	experimentalProperties["android.experimental.enableScreenshotTest"] = true

	testOptions {
		// Exigé par le rendu des @Preview (ressources compilées disponibles hors appareil).
		unitTests.isIncludeAndroidResources = true
	}
}

kotlin {
	jvmToolchain(21)
	compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
}

dependencies {
	implementation(project(":core"))
	implementation(project(":data"))

	implementation(libs.androidx.core.ktx)
	implementation(libs.androidx.activity.compose)
	implementation(libs.androidx.lifecycle.runtime.compose)
	implementation(libs.androidx.lifecycle.viewmodel.compose)
	implementation(platform(libs.compose.bom))
	implementation(libs.compose.ui)
	implementation(libs.compose.ui.graphics)
	implementation(libs.compose.ui.tooling.preview)
	implementation(libs.compose.material3)
	debugImplementation(libs.compose.ui.tooling)

	screenshotTestImplementation(libs.screenshot.validation.api)
	screenshotTestImplementation(libs.compose.ui.tooling)

	testImplementation(libs.kotlin.test.junit)
	testImplementation(libs.junit)
}

// Le rendu des @Preview (plugin com.android.compose.screenshot) exige des tests unitaires sur chaque variante.
androidComponents {
	beforeVariants(selector().withBuildType("release")) { inVariant ->
		inVariant.hostTests[com.android.build.api.variant.HostTestBuilder.UNIT_TEST_TYPE]?.enable = true
	}
}
