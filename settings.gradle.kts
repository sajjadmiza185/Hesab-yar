pluginManagement {
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "com.google.gms.google-services") {
                useModule("com.google.gms:google-services:${requested.version}")
            }

            if (requested.id.id == "com.google.devtools.ksp") {
                useModule(
                    "com.google.devtools.ksp:symbol-processing-gradle-plugin:${requested.version}"
                )
            }
        }
    }

    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)

    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "باربرگ"

include(":app")
