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
    implementation("com.microsoft.onnxruntime:onnxruntime:1.31.1")
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
            packageVersion = providers.gradleProperty("studioPackageVersion").orElse("0.2.0").get()
            description = "CCB Studio Suite Windows Preview"
            vendor = "MisakaPiano"
            windows { iconFile.set(project.file("src/main/resources/brand/ccb-blue.ico")) }
            modules("java.desktop", "java.logging", "java.naming", "java.net.http", "java.sql", "jdk.unsupported", "jdk.crypto.ec")
        }
    }
}

tasks.test {
    useJUnit(); maxParallelForks=1; maxHeapSize="256m"; jvmArgs("-XX:ActiveProcessorCount=2")
    val isolated = providers.gradleProperty("previewTestRoot").orNull
    systemProperty("preview.test.root", isolated?.let { "$it/desktop" } ?: rootProject.file(".local/windows-preview/desktop-tests").absolutePath)
    isolated?.let { systemProperty("java.io.tmpdir", "$it/desktop-tmp") }
    providers.gradleProperty("updateFixtureRoot").orNull?.let { systemProperty("preview.update.fixture", it) }
    testLogging { events("passed","failed","skipped") }
}

// Separate test-only driver for an isolated native A process; never in the shipped JAR.
tasks.register<Jar>("reviewUpdateProbeJar") {
    dependsOn("testClasses")
    from(sourceSets.test.get().output)
    archiveClassifier.set("review-update-probe")
}

// Test-only native measurement host. No installer or distributable is created.
tasks.register("nativePerformanceInputs") {
    dependsOn("testClasses")
    doLast {
        val launcher=javaToolchains.launcherFor {languageVersion.set(JavaLanguageVersion.of(17))}.get()
        val target=layout.buildDirectory.file("native-performance-20261025/inputs.txt").get().asFile
        target.parentFile.mkdirs()
        target.writeText(launcher.executablePath.asFile.absolutePath+"\n"+sourceSets.test.get().runtimeClasspath.asPath+"\n")
    }
}
