import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Module :data : entités, DAO et RoomDatabase, partagés par l'application et le builder.
plugins {
	alias(libs.plugins.kotlin.multiplatform)
	alias(libs.plugins.android.kmp.library)
	alias(libs.plugins.ksp)
	alias(libs.plugins.room)
}

kotlin {
	jvmToolchain(21)
	// Room KMP déclare ses constructeurs de base en expect object.
	compilerOptions { freeCompilerArgs.add("-Xexpect-actual-classes") }

	android {
		namespace = "com.chessplus.data"
		compileSdk = libs.versions.android.compileSdk.get().toInt()
		minSdk = libs.versions.android.minSdk.get().toInt()
		compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
		withHostTest {}
	}

	jvm {
		compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
	}

	sourceSets {
		commonMain.dependencies {
			api(project(":core"))
			api(libs.room.runtime)
			implementation(libs.sqlite.bundled)
			implementation(libs.kotlinx.coroutines.core)
		}
		commonTest.dependencies {
			implementation(libs.kotlin.test)
		}
	}
}

room {
	schemaDirectory("$projectDir/schemas")
}

dependencies {
	add("kspAndroid", libs.room.compiler)
	add("kspJvm", libs.room.compiler)
}
