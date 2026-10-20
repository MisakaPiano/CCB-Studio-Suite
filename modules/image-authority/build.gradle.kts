plugins {
    kotlin("jvm") version "2.3.20"
    kotlin("plugin.serialization") version "2.3.20"
    `java-library`
}
kotlin { jvmToolchain(17) }
dependencies {
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")
    api("com.squareup.okhttp3:okhttp:4.12.0")
    api("org.msgpack:msgpack-core:0.9.12")
    testImplementation(kotlin("test-junit"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
}
dependencyLocking { lockAllConfigurations() }
tasks.test {
    useJUnit()
    maxParallelForks = 1
    maxHeapSize = "256m"
    jvmArgs("-XX:ActiveProcessorCount=2")
    testLogging { events("passed", "failed", "skipped") }
}
