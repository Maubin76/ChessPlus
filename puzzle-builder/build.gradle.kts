import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Module :puzzle-builder : génère app/src/main/assets/puzzles.db à partir du CSV Lichess.
// Lancement manuel uniquement : ./gradlew :puzzle-builder:run --args="<fichier.csv.zst> [options]".
// Aucune tâche du build normal ne doit dépendre de ce module.
plugins {
	alias(libs.plugins.kotlin.jvm)
	application
}

kotlin {
	jvmToolchain(21)
	compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
}

java {
	targetCompatibility = JavaVersion.VERSION_17
}

application {
	mainClass.set("com.chessplus.builder.MainKt")
}

dependencies {
	implementation(project(":core"))
	implementation(project(":data"))
	implementation(libs.zstd.jni)
	implementation(libs.kotlinx.coroutines.core)

	testImplementation(libs.kotlin.test)
	testImplementation(libs.junit)
}

tasks.named<JavaExec>("run") {
	// Les chemins relatifs passés en argument sont résolus depuis la racine du dépôt.
	workingDir = rootProject.projectDir
}
