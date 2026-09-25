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
    implementation(project(":api"))
    compileOnly(libs.paper)
    compileOnly(libs.dynmap) {
        exclude(group = "org.bukkit")
        exclude(group = "org.spigotmc")
    }
    compileOnly(libs.chunkyCommon)
    compileOnly(libs.chunkyBorderCommon)
    compileOnly(libs.chunkyBorderBukkit)
    compileOnly(libs.worldBorder) {
        exclude(group = "org.bukkit")
        exclude(group = "org.spigotmc")
    }
    compileOnly(libs.vaultAPI) {
        exclude(group = "org.bukkit")
        exclude(group = "org.spigotmc")
    }
    compileOnly(libs.griefPrevention)
    compileOnly(libs.worldguard) {
        exclude(group = "org.bukkit")
        exclude(group = "org.spigotmc")
    }
    compileOnly(libs.huskTowns)
    compileOnly(libs.landsAPI)

    testImplementation(libs.paper)
    testImplementation(libs.vaultAPI)
    testImplementation("org.junit.jupiter:junit-jupiter:5.13.4")
    testImplementation("org.mockito:mockito-core:5.20.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
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
}

tasks.named("build") {
    dependsOn(tasks.named("shadowJar"))
}

tasks.named<RunServer>("runServer") {
    minecraftVersion("1.21.11")
    jvmArguments.add("-Dcom.mojang.eula.agree=true")
    jvmArguments.add("-Dnet.kyori.ansi.colorLevel=truecolor")
    jvmArguments.add("-Dfile.encoding=UTF8")
    systemProperty("terminal.jline", false)
    systemProperty("terminal.ansi", true)
}


