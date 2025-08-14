pluginManagement {
	repositories {
		gradlePluginPortal()
		mavenCentral()
	}
}

dependencyResolutionManagement {
	repositoriesMode.set(RepositoriesMode.PREFER_PROJECT)
	versionCatalogs {
		create("libs")
	}
}

rootProject.name = "JakesRTP"
include("API", "Core")
