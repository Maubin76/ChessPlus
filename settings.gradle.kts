// Configuration des modules et des dépôts de ChessPlus.
pluginManagement {
	repositories {
		google()
		mavenCentral()
		gradlePluginPortal()
	}
}

dependencyResolutionManagement {
	repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
	repositories {
		google()
		mavenCentral()
		// chesslib n'est publié que sur JitPack : le dépôt est limité à ce seul groupe.
		exclusiveContent {
			forRepository { maven("https://jitpack.io") }
			filter { includeGroup("com.github.bhlangonijr") }
		}
	}
}

rootProject.name = "ChessPlus"

include(":core")
include(":data")
include(":puzzle-builder")
include(":app")
