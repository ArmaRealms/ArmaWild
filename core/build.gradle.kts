import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import xyz.jpenilla.runpaper.task.RunServer

plugins {
    java
    alias(libs.plugins.shadow)
    alias(libs.plugins.runpaper)
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
    destinationDirectory.set(rootProject.layout.projectDirectory.dir("out"))
    relocate("io.papermc", "biz.donvi.jakesRTP.libs.io.papermc")
}

tasks.named("build") {
    dependsOn(tasks.named("shadowJar"))
}

tasks.named<RunServer>("runServer") {
        minecraftVersion("1.20.4")
        jvmArguments.add("-Dcom.mojang.eula.agree=true")
        jvmArguments.add("-Dnet.kyori.ansi.colorLevel=truecolor")
        jvmArguments.add("-Dfile.encoding=UTF8")
        systemProperty("terminal.jline", false)
        systemProperty("terminal.ansi", true)
}



