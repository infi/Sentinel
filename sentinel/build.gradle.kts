import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm")
    application
}

repositories {
    mavenCentral()
}

val coroutines_version: String by project
val logback_version: String by project

dependencies {
    implementation(kotlin("stdlib"))
    implementation(project(":lookout"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutines_version")
    implementation("ch.qos.logback:logback-classic:$logback_version")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

kotlin {
    compilerOptions.jvmTarget.set(JvmTarget.JVM_17)
}

application {
    mainClass.set("chat.revolt.sentinel.SentinelKt")
}