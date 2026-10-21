plugins {
    kotlin("jvm")
    kotlin("plugin.compose")
    id("org.jetbrains.compose")
}
kotlin { jvmToolchain(17) }
dependencies {
    implementation(project(":studio-core"))
    implementation("org.xerial:sqlite-jdbc:3.53.4.0")
    implementation("com.composables:icons-lucide-cmp-jvm:2.2.1")
    implementation("net.java.dev.jna:jna:5.18.1")
    implementation("net.java.dev.jna:jna-platform:5.18.1")
    testImplementation(kotlin("test-junit"))
    implementation(project(":asset-library"))
    implementation(project(":image-authority"))
    implementation(compose.desktop.currentOs)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")
}
dependencyLocking { lockAllConfigurations() }
compose.desktop {
    application {
        mainClass = "io.github.misakapiano.ccbstudio.desktop.MainKt"
        jvmArgs("-Xmx512m", "-XX:ActiveProcessorCount=2")
        nativeDistributions {
            packageName = "CCBStudioSuitePreview"
            packageVersion = "0.1.0"
            description = "CCB Studio Suite - synthetic workspace Preview"
            vendor = "MisakaPiano"
            windows { iconFile.set(project.file("src/main/resources/brand/ccb-blue.ico")) }
            modules("java.desktop", "java.logging", "java.naming", "java.net.http", "java.sql", "jdk.unsupported", "jdk.crypto.ec")
        }
    }
}

tasks.test {
    useJUnit(); maxParallelForks=1; maxHeapSize="256m"; jvmArgs("-XX:ActiveProcessorCount=2")
    systemProperty("preview.test.root", rootProject.file(".local/windows-preview/desktop-tests").absolutePath)
    testLogging { events("passed","failed","skipped") }
}
