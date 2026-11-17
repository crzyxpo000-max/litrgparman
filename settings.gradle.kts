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
    }
}

rootProject.name = "yukselis-protokolu"
include(":engine", ":app", ":tools:simulator")

// Ayrı uygulama: webnovel → EPUB indirici (oyundan bağımsız; bkz. scraper/README.md).
include(":scraper:core", ":scraper:app")

// Ayrı uygulama: Reddit beğenilerini indirici (oyundan bağımsız; bkz. reddit/README.md).
include(":reddit:core", ":reddit:app", ":reddit:desktop")
