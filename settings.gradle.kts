pluginManagement {
    repositories {
        maven { url = uri(rootDir.resolve(".local/windows-preview/maven")) }
        gradlePluginPortal()
        mavenCentral()
    }
    resolutionStrategy.eachPlugin {
        when (requested.id.id) {
            "org.jetbrains.kotlin.jvm" -> useModule("org.jetbrains.kotlin:kotlin-gradle-plugin:2.3.20")
            "org.jetbrains.kotlin.plugin.serialization" -> useModule("org.jetbrains.kotlin:kotlin-serialization:2.3.20")
            "org.jetbrains.kotlin.plugin.compose" -> useModule("org.jetbrains.kotlin:compose-compiler-gradle-plugin:2.3.20")
            "org.jetbrains.compose" -> useModule("org.jetbrains.compose:compose-gradle-plugin:1.10.3")
        }
    }
}
dependencyResolutionManagement {
    repositories {
        maven { url = uri(rootDir.resolve(".local/windows-preview/maven")) }
        mavenCentral()
        google()
    }
}
rootProject.name = "CCB-Studio-Suite"
include(":asset-library")
project(":asset-library").projectDir = file("modules/asset-library")
include(":image-authority")
project(":image-authority").projectDir = file("modules/image-authority")
include(":studio-core")
project(":studio-core").projectDir = file("modules/studio-core")
include(":studio-desktop")
project(":studio-desktop").projectDir = file("modules/studio-desktop")
