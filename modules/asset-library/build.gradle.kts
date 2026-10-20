plugins { `java-library` }

java { toolchain { languageVersion = JavaLanguageVersion.of(17) } }
repositories {
    val offline = providers.gradleProperty("assetOfflineTestRepository")
    if (offline.isPresent) maven {
        name = "ExplicitOfflineTestArtifacts"
        url = uri(offline.get())
        metadataSources { artifact() }
    } else mavenCentral()
}
dependencies {
    testImplementation("junit:junit:4.13.2") { isTransitive = false }
    testRuntimeOnly("org.hamcrest:hamcrest-core:1.3") { isTransitive = false }
}
dependencyLocking { lockAllConfigurations() }

tasks.withType<JavaCompile>().configureEach { options.encoding = "UTF-8" }
tasks.test {
    useJUnit()
    maxParallelForks = 1
    maxHeapSize = "192m"
    jvmArgs("-XX:ActiveProcessorCount=2")
    systemProperty("asset.test.root", rootProject.file(".local/a1-001/tests").absolutePath)
    systemProperty("asset.child.classpath", sourceSets.test.get().runtimeClasspath.asPath)
    testLogging { events("passed", "failed", "skipped"); showStandardStreams = true }
}
