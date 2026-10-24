plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}
kotlin { jvmToolchain(17) }
dependencies {
    implementation(project(":asset-library"))
    implementation(project(":image-authority"))
    implementation("net.java.dev.jna:jna:5.18.1")
    implementation("net.java.dev.jna:jna-platform:5.18.1")
    implementation("org.jetbrains.skiko:skiko-awt:0.9.37.4")
    runtimeOnly("org.jetbrains.skiko:skiko-awt-runtime-windows-x64:0.9.37.4")
    testImplementation(kotlin("test-junit"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
}
dependencyLocking { lockAllConfigurations() }
tasks.test {
    useJUnit(); maxParallelForks = 1; maxHeapSize = "256m"
    jvmArgs("-XX:ActiveProcessorCount=2")
    val isolated = providers.gradleProperty("previewTestRoot").orNull
    systemProperty("preview.test.root", isolated?.let { "$it/core" } ?: rootProject.file(".local/windows-preview/tests").absolutePath)
    isolated?.let { systemProperty("java.io.tmpdir", "$it/core-tmp") }
    testLogging { events("passed", "failed", "skipped") }
}
tasks.register<JavaExec>("liveProbe") {
    group = "verification"
    dependsOn("testClasses")
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("io.github.misakapiano.ccbstudio.core.LiveProbe")
    maxHeapSize = "256m"
    jvmArgs("-XX:ActiveProcessorCount=2")
    args(providers.gradleProperty("liveService").getOrElse("CREDENTIAL_CHECK"), rootProject.file(".local/windows-preview/live-validation").absolutePath)
}
