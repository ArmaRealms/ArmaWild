plugins {
    java
    id(libs.plugins.shadow)
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(libs.versions.jvm.get()))
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
    compileOnly("io.papermc.paper:paper-api:${libs.versions.paper}")
    compileOnly("us.dynmap:dynmap-api:${libs.versions.dynmap}")
    compileOnly("org.popcraft:chunky-common:${libs.versions.chunkyCommon}")
    compileOnly("org.popcraft:chunkyborder-common:${libs.versions.chunkyBorderCommon}")
    compileOnly("org.popcraft:chunkyborder-bukkit:${libs.versions.chunkyBorderBukkit}")
    compileOnly("com.github.Brettflan:WorldBorder:${libs.versions.worldBorder}")
    compileOnly("com.github.MilkBowl:VaultAPI:${libs.versions.vaultAPI}")
    compileOnly("com.github.TechFortress:GriefPrevention:${libs.versions.griefPrevention}")
    compileOnly("com.sk89q.worldguard:worldguard-bukkit:${libs.versions.worldguard}")
    compileOnly("com.github.WiIIiam278:HuskTowns:${libs.versions.huskTowns}")
    compileOnly("com.github.angeschossen:LandsAPI:${libs.versions.landsAPI}")
}

processResources {
    filesMatching("plugin.yml") {
        expand("version" to project.version)
    }
}

// ShadowJar config
shadowJar {
    archiveBaseName.set("JakesRTP-v")
    archiveClassifier.set("")
    archiveVersion.set(project.version.toString())
    // Remove paperlib relocation
}

artifacts {
    archives(shadowJar)
}



