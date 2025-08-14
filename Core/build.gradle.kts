import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    java
    alias(libs.plugins.shadow)
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(libs.versions.jvm.get().toInt()))
}

repositories {
    mavenCentral()
    maven("https://papermc.io/repo/repository/maven-public/")
    maven("https://repo.codemc.io/repository/maven-public/")
    maven("https://jitpack.io")
    maven("https://maven.enginehub.org/repo/")
    maven("https://repo.mikeprimm.com/")
}

dependencies {
    compileOnly(libs.paper)
    compileOnly(libs.dynmap)
    compileOnly(libs.chunkyCommon)
    compileOnly(libs.chunkyBorderCommon)
    compileOnly(libs.chunkyBorderBukkit)
    compileOnly(libs.worldBorder)
    compileOnly(libs.vaultAPI)
    compileOnly(libs.griefPrevention)
    compileOnly(libs.worldguard)
    compileOnly(libs.huskTowns)
    compileOnly(libs.landsAPI)
    implementation(libs.paperlib)
}

tasks.processResources {
    filesMatching("plugin.yml") {
        expand("version" to project.version)
    }
}

// ShadowJar config
tasks.named<ShadowJar>("shadowJar") {
    archiveBaseName.set("JakesRTP")
    archiveClassifier.set("")
    archiveVersion.set(project.version.toString())
    relocate("io.papermc", "biz.donvi.jakesRTP.libs.io.papermc")
}

// Shadow plugin already adds the shadowJar artifact by default



