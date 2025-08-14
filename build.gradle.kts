defaultTasks("build")

subprojects {
    apply(plugin = "java-library")

    group = "biz.donvi"
    version = "0.14.9"

    // Configure Java toolchain for all subprojects using the version catalog
    extensions.configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(libs.versions.jvm.get().toInt()))
        }
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
    }

    repositories {
        mavenLocal()
        maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
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
    }

    tasks.withType<Copy> {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
    }
}
